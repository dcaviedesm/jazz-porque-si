package com.caviedes.jazz.porque.si;

import com.caviedes.jazz.porque.si.json.pojos.Item;
import com.caviedes.jazz.porque.si.json.pojos.Quality;
import com.caviedes.jazz.porque.si.json.pojos.Root;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.caviedes.jazz.porque.si.AppConfig.Parameter.*;

public class App {

    private static final Logger log = LogManager.getLogger(App.class);
    private AppConfig appConfig;
    private SimpleDateFormat originalDateFormat;
    private SimpleDateFormat invertedDateFormat;
    private String mainFolderPath;

    public void execute() {
        execute(null);
    }

    public void execute(AppConfig appConfig) {

        loadConfig(appConfig);

        log.info("Loading information from JSON to mp3's...");

        mainFolderPath = this.appConfig.getParameter(MAIN_FOLDER_PATH);
        File[] pagesFolders = ensurePagesFolders();

        Arrays.stream(pagesFolders)
            .forEachOrdered(currentFolder -> {
                log.info("Processing page folder {}", currentFolder.getName());

                int offset = getCurrentOffset(currentFolder).orElse(0);
                Optional<Root> jsonMainObject = getJsonBean(offset);
                if (jsonMainObject.isEmpty()) {
                    log.error("It is necessary a JSON to process");
                    return;
                }

                Optional<File[]> audioFiles = getAudios(currentFolder, jsonMainObject.get());
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

    private File[] ensurePagesFolders() {
        File[] pagesFolders = getPagesFolders(mainFolderPath).orElse(null);
        Root root = getJsonBean().orElse(null);

        if (isMissingPagesFolders(pagesFolders, root)) {
            createMissingPagesFolders(root);
            pagesFolders = getPagesFolders(mainFolderPath).orElse(null);
        }

        if (pagesFolders == null || pagesFolders.length == 0) {
            log.warn("No pagesFoldes found for: {}", mainFolderPath);
        }

        return pagesFolders;
    }

    /**
     * @param firstPageJsonBean
     */
    private void createMissingPagesFolders(Root firstPageJsonBean) {

        if (firstPageJsonBean != null) {
            int totalPagesFolders = firstPageJsonBean.getPage().getTotalPages();
            IntStream.rangeClosed(1, totalPagesFolders)
                .forEach(this::createPageFolderIfNecessary);

        } else {

            log.error("Cannot create pages folders because it is needed information about pages from API!!");
        }
    }

    /**
     * Creates page folder corresponding to "i" number, if it doesn´t already exists
     *
     * @param i
     */
    private void createPageFolderIfNecessary(Integer currentPageNumber) {

        String currentPageFolderName = String.format("%s%02d", this.appConfig.getParameter(PAGE_FOLDER_PREFIX), currentPageNumber);

        File currentPageFolder = new File(mainFolderPath, currentPageFolderName);
        if (!currentPageFolder.exists() && !currentPageFolder.mkdir()) {
            log.error("Unable to create current page folder: {}", currentPageFolderName);
        }
    }

    /**
     * @param pagesFolders
     * @param root
     * @return true if some pages folders (or all of its) are missing
     */
    private boolean isMissingPagesFolders(File[] pagesFolders, Root root) {

        if (root != null && root.getPage() != null) {
            int totalPagesFolders = root.getPage().getTotalPages();
            boolean isMissingAllPagesFolders = pagesFolders == null;
            boolean isMissingSomePagesFolders = (pagesFolders != null && (pagesFolders.length < totalPagesFolders));

            return isMissingAllPagesFolders || isMissingSomePagesFolders;
        }

        return true;
    }

    /**
     * Loads app configuration from parameter if not null, otherwise it will initialize a default config
     *
     * @param appConfig
     */
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
     * @return JSON bean representing first page of audios
     */
    private Optional<Root> getJsonBean() {
        return getJsonBean(0);
    }

    /**
     * @return java POJO representing JSON
     */
    private Optional<Root> getJsonBean(int offset) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return Optional.ofNullable(objectMapper.readValue(getJsonStringFromUrl(String.format("%s?offset=%d", appConfig.getParameter(JSON_URL), offset)), Root.class));
        } catch (IOException e) {
            log.error("Error trying to map JSON {} to bean: {}", e.getMessage(), e);
            return Optional.empty();
        }
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
             setId3TagInformation(item.get(), newAudioName.get(), new MP3File(newAudioFile));
        } catch (ID3Exception e) {
            log.error("Error trying to rename MP3 newAudioFile: {}", newAudioFile.getAbsolutePath());
        }
    }

    private static ID3V2Tag ensureGetId3V2Tag(MediaFile audioFile) throws ID3Exception {
        ID3V2Tag tag = audioFile.getID3V2Tag();

        if (tag == null) {
            log.info("Cannot obtain ID3 information, trying to create it from scratch...");
            tag = new ID3V2_3_0Tag();
        }

        return tag;
    }

    /**
     * Sets ID3 information and syncs audio file
     *  @param item
     * @param newAudioName
     * @param mp3File
     */
    private static void setId3TagInformation(Item item, String newAudioName, MediaFile mp3File) throws ID3Exception {
        ID3V2Tag tag = ensureGetId3V2Tag(mp3File);
        try {

            tag.setComment(StringUtils.defaultString(item.getDescription()));
            tag.setTitle(newAudioName);
            mp3File.setID3Tag(tag);
            mp3File.sync();

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

        if (itemList.isEmpty()) {
            return Optional.empty();
        }

        String fullFilename = String.format("%s%s", audioId, AUDIOS_EXTENSION.getDefaultValue());
        return itemList.stream()
            .filter(item -> item.getQualities().stream()
                // If any match
                .anyMatch(quality -> StringUtils.contains(quality.getFilePath(), fullFilename)))
            // get the first one
            .findFirst();
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
     * TODO: Delete is no needed????
     * @param jsonFilePath
     * @return Json string
     * @throws IOException
     */
    private static String getJsonStringFromFile(String jsonFilePath) throws IOException {
        return IOUtils.toString(new FileInputStream(jsonFilePath), StandardCharsets.UTF_8);
    }

    /**
     * @param currentFolder
     * @param jsonMainObject
     * @return array with files that represents audios of a defined extension
     */
    private Optional<File[]> getAudios(File currentFolder, Root jsonMainObject) {
        if (currentFolder == null || !currentFolder.exists()) {
            log.error("Current folder where to find audios must exist");
            return Optional.empty();
        }

        if (jsonMainObject != null) {

            jsonMainObject.getPage().getItems()
                .forEach(ensureDownloadingMissingAudios(currentFolder));
        } else {

            log.error("API information about current page is necessary!!");
            return Optional.empty();
        }

        return Optional.ofNullable(currentFolder.listFiles((current, name) -> name.toLowerCase().endsWith(this.appConfig.getParameter(AUDIOS_EXTENSION))));
    }

    private Consumer<Item> ensureDownloadingMissingAudios(File currentFolder) {
        return currentItem -> {

            log.info("Processing item {}", currentItem.getLongTitle());

            // Get file name
            String fileName = getFileName(currentItem).orElse(null);

            if (fileName == null) {
                // perhaps it worth it to log it?
                return;
            }

            // Try to get already downloaded file
            String localFilePath = String.format("%s%s%s", currentFolder.getPath(), System.getProperty("file.separator"), fileName);

            // If not downloaded, download it
            File file = new File(localFilePath);
            if (isFileValid(file)) {
                Optional<Quality> quality = currentItem.getQualities().stream().findFirst();
                if (quality.isPresent()) {
                    String fileUrl = quality.get().getFilePath();
                    try {
                        FileUtils.copyURLToFile(
                            new URL(fileUrl),
                            file,
                            10000,
                            10000);
                    } catch (MalformedURLException e) {
                        log.error("Problem with URL {} while trying to download it: {}", fileUrl, e);
                    } catch (IOException e) {
                        log.error("I/O problem while trying to download dile {}: {}", fileUrl, e);
                    }
                }
            }
        };
    }

    private boolean isFileValid(File file) {
        return file != null && file.isFile() && FileUtils.sizeOf(file) > 0;
    }

    private Optional<String> getFileName(Item item) {
        Optional<String> ret = Optional.empty();

        Optional<Quality> quality = item.getQualities().stream().findFirst();
        if (quality.isEmpty()) {
            log.warn("No Quality found for Item.title {}", item.getTitle());
            return ret;
        }

        String filePath = quality.get().getFilePath();
        String[] filePathSplit = filePath.split("/");
        if (filePathSplit.length > 1) { // URL obtained
            return Optional.ofNullable(filePathSplit[filePathSplit.length - 1]);
        }

        return ret;
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
