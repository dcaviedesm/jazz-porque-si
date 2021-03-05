package com.caviedes.jazz.porque.si;

import com.caviedes.jazz.porque.si.json.pojos.Item;
import com.caviedes.jazz.porque.si.json.pojos.Root;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.MediaFile;
import org.blinkenlights.jid3.v2.ID3V2Tag;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.caviedes.jazz.porque.si.AppConfig.Parameter.*;

public class App {

    private static final Logger log = LogManager.getLogger(App.class);
    private AppConfig appConfig;
    private SimpleDateFormat originalDateFormat;
    private SimpleDateFormat invertedDateFormat;

    public void execute() {
        execute(null);
    }

    public void execute(AppConfig appConfig) {
        loadConfig(appConfig);

        log.info("Loading information from JSON to mp3's...");


        String mainFolderPath = this.appConfig.getParameter(MAIN_FOLDER_PATH);
        Optional<File[]> pagesFolders = getPagesFolders(mainFolderPath);

        if (pagesFolders.isEmpty()) {
            log.info("Not page folder to process for: {}", mainFolderPath);
            return;
        }

        Arrays.stream(pagesFolders.get())
                .forEachOrdered(currentFolder -> {
                    log.info("Processing page folder {}", currentFolder.getName());

                    int offset = getCurrentOffset(currentFolder).orElse(0);
                    Optional<Root> jsonMainObject = getJsonBean(offset);
                    if (jsonMainObject.isEmpty()) {
                        log.error("It is necessary a JSON to process");
                        return;
                    }

                    Optional<File[]> audioFiles = getAudios(currentFolder);
                    if (audioFiles.isEmpty()) {
                        log.info("No audio to process into {}", currentFolder);
                        return;
                    }
                    Arrays.stream(audioFiles.get())
                            .forEachOrdered(currentAudio -> {
                                log.info("Processing audio {}", currentAudio.getName());
                                processAudio(currentAudio, jsonMainObject.get());
                            });
                });
    }

    private void loadConfig(AppConfig appConfig) {
        this.appConfig = appConfig == null
                ? new AppConfig()
                : appConfig;

        originalDateFormat = new SimpleDateFormat(this.appConfig.getParameter(ORIGINAL_DATE_PATTERN));
        invertedDateFormat = new SimpleDateFormat(this.appConfig.getParameter(DATE_INVERTED_PATTERN));
    }

    /**
     * @param currentFolder
     * @return current offset based on folder name, that must have the same format: "XXX YY", where "YY" represents the page number of audios
     */
    private Optional<Integer> getCurrentOffset(File currentFolder) {
        if (currentFolder == null) {
            log.error("Current folder cannot be null to calculate offset");
            return Optional.empty();
        }

        String[] folderNameSplit = currentFolder.getName().split(StringUtils.SPACE);
        if (folderNameSplit.length == 2) {

            try {
                Integer pageNumber = Integer.valueOf(folderNameSplit[1]);
                return Optional.of((pageNumber * appConfig.getParameterAsInt(AUDIOS_PER_PAGE)) - appConfig.getParameterAsInt(AUDIOS_PER_PAGE));
            } catch (NumberFormatException e) {
                log.error("Folder name must contains page number: {}", folderNameSplit[1]);
            }
        }
        return Optional.empty();
    }

    /**
     * @return java POJO representing JSON
     */
    private Optional<Root> getJsonBean(int offset) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return Optional.ofNullable(objectMapper.readValue(getJsonStringFromUrl(appConfig.getParameter(JSON_URL) + "?offset=" + offset), Root.class));
        } catch (IOException e) {
            log.error("Error trying to map JSON {} to bean: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * @param currentAudio
     * @param jsonMainObject Renames audio and fills it based on information
     *                       contained into a defined JSON
     */
    private void processAudio(File currentAudio, Root jsonMainObject) {

        if (currentAudio == null) {
            log.error("Cannot obtain audio ID of null file {}", jsonMainObject);
            return;
        }

        String[] audioNameSplit = currentAudio.getName().split(appConfig.getParameter(AUDIOS_EXTENSION));
        if (audioNameSplit.length == 0) {
            log.error("Cannot obtain audio ID of file {}", currentAudio.getName());
            return;
        }

        String audioId = audioNameSplit[0];
        Optional<Item> item = getMatchItem(audioId, jsonMainObject);
        if (item.isEmpty()) {
            log.error("Matched item not found: {}", audioId);
            return;
        }

        Optional<String> newAudioName = getFriendlyAudioName(item.get());
        if (newAudioName.isEmpty()) {
            log.error("Matched item {} doesn't generate a FriendlyAudioName", item.get());
            return;
        }

        File newAudioFile = new File(currentAudio.getParent(), newAudioName.get());
        if (!currentAudio.renameTo(newAudioFile)) {
            log.error("CurrentAudio {} was not renamed to {}", currentAudio, newAudioFile);
            return;
        }

        try {
            MediaFile audioFile = new MP3File(newAudioFile);
            ID3V2Tag tag = audioFile.getID3V2Tag();

            if (tag == null) {
                log.info("Cannot obtain ID3 information, trying to create it from scratch...");
                tag = new ID3V2_3_0Tag();
            }

            setId3TagInformation(item.get(), newAudioName.get(), tag, audioFile);

        } catch (ID3Exception e) {
            log.error("Error trying to rename MP3 newAudioFile: {}", newAudioFile.getAbsolutePath());
        }
    }

    /**
     * Sets ID3 information and syncs audio file
     *
     * @param item
     * @param newAudioName
     * @param tag
     * @param audioFile
     */
    private static void setId3TagInformation(Item item, String newAudioName, ID3V2Tag tag, MediaFile audioFile) {

        try {

            tag.setComment(StringUtils.defaultString(item.getDescription()));
            tag.setTitle(newAudioName);
            audioFile.setID3Tag(tag);
            audioFile.sync();

        } catch (ID3Exception e) {
            log.error("Error trying to get MP3 metadata: {}", e.getMessage(), e);
        }
    }

    /**
     * @param audioId
     * @param jsonMainObject
     * @return item that contains audio ID
     */
    private static Optional<Item> getMatchItem(String audioId, Root jsonMainObject) {
        List<Item> itemList = jsonMainObject.getPage().getItems();
        String fullFilename = String.format("%s%s", audioId, AUDIOS_EXTENSION.getDefaultValue());
        return !itemList.isEmpty()
                ? itemList.stream()
                .filter(item -> item.getQualities().stream()
                        // If any match
                        .anyMatch(quality -> StringUtils.contains(quality.getFilePath(), fullFilename)))
                // get the first one
                .findFirst()
                : Optional.empty();
    }

    /**
     * @param item
     * @return friendly audio name
     */
    private Optional<String> getFriendlyAudioName(Item item) {
        if (item == null) {
            log.error("Item cannot be null");
            return Optional.empty();
        }

        Optional<String> invertedDate = getInvertedDate(item.getDateOfEmission());
        if (invertedDate.isEmpty()) {
            log.error("Impossible to get inverted date of emission");
            return Optional.empty();
		}
        
        // Prevent problems with slash characters
        String tmpName = String.format("%s_%s%s",
        		invertedDate.get(),
                StringUtils.replace(item.getShortTitle(), "/", "-"),
                AUDIOS_EXTENSION.getDefaultValue()
        );

        log.info("Friendly audio name obtained: {}", tmpName);
        return Optional.of(tmpName);
    }

    /**
     * @param dateOfEmission
     * @return date of emission in inverted way
     */
    private Optional<String> getInvertedDate(String dateOfEmission) {
        try {
            Date dateOriginal = originalDateFormat.parse(dateOfEmission);
            return Optional.of(invertedDateFormat.format(dateOriginal));

        } catch (ParseException e) {
            log.error("Error parsing date of emission {}", dateOfEmission);
        }

        return Optional.empty();
    }

    /**
     * @param url
     * @return Json string
     * @throws IOException
     */
    private static String getJsonStringFromUrl(String url) throws IOException {

        URL jsonUrl = new URL(url);
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readTree(jsonUrl).toString();
    }

    /**
     * @param jsonFilePath
     * @return Json string
     * @throws IOException
     */
    private static String getJsonStringFromFile(String jsonFilePath) throws IOException {
        return IOUtils.toString(new FileInputStream(jsonFilePath), StandardCharsets.UTF_8);
    }

    /**
     * @param currentFolder
     * @return array with files that represents audios of a defined extension
     */
    private Optional<File[]> getAudios(File currentFolder) {
        if (currentFolder == null || !currentFolder.exists()) {
            log.error("Current folder where to find audios must exist");
            return Optional.empty();
        }

        return Optional.ofNullable(currentFolder.listFiles((current, name) -> name.toLowerCase().endsWith(appConfig.getParameter(AUDIOS_EXTENSION))));
    }

    /**
     * @param mainFolderPath
     * @return array with folders that represents pages of audios
     */
    private static Optional<File[]> getPagesFolders(String mainFolderPath) {
        if (StringUtils.isBlank(mainFolderPath)) {
            log.error("The working folder must be. Have a look at MAIN_FOLDER_PATH");
            return Optional.empty();
        }

        File mainFolder = new File(mainFolderPath);
        if (!mainFolder.exists()) {
            log.error("The working folder {} do not exits. Have a look at MAIN_FOLDER_PATH", mainFolder);
            return Optional.empty();
        }

        return Optional.ofNullable(mainFolder.listFiles((current, name) -> new File(current, name).isDirectory()));
    }
}
