package com.caviedes.jazz.porque.si;

import com.caviedes.jazz.porque.si.json.pojos.Item;
import com.caviedes.jazz.porque.si.json.pojos.Quality;
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

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.caviedes.jazz.porque.si.AppConfig.Parameter.*;

public class App {

    private static final Logger log = LogManager.getLogger(App.class);
    private AppConfig appConfig;

    public void execute() {
        execute(null);
    }

    public void execute(AppConfig appConfig) {
        loadConfig
                (appConfig);

        log.info("Loading information from JSON to mp3's...");


        for (File currentFolder : getPagesFolders(appConfig.getParameter(MAIN_FOLDER_PATH))) {

            log.info("Processing page folder {}", currentFolder.getName());

            int offset = getCurrentOffset(currentFolder);

            Root jsonMainObject = getJsonBean(offset);

            if (jsonMainObject != null) {

                for (File currentAudio : getAudios(currentFolder)) {

                    log.info("Processing audio {}", currentAudio.getName());

                    processAudio(currentAudio, jsonMainObject);
                }

            } else {

                log.error("It is necessary a JSON to process");
            }
        }
    }

    private void loadConfig(AppConfig appConfig) {
        this.appConfig = appConfig == null
                ? new AppConfig()
                : appConfig;
    }

    /**
     * @param currentFolder
     * @return current offset based on folder name, that must have the same format: "XXX YY", where "YY" represents the page number of audios
     */
    private int getCurrentOffset(File currentFolder) {

        int ret = 0;

        if (currentFolder != null) {

            String[] folderNameSplit = currentFolder.getName().split(StringUtils.SPACE);
            boolean isFolderNameSplit = (folderNameSplit.length == 2);
            if (isFolderNameSplit) {

                try {

                    Integer pageNumber = Integer.valueOf(folderNameSplit[1]);
                    ret = (pageNumber * appConfig.getParameterAsInt(AUDIOS_PER_PAGE)) - appConfig.getParameterAsInt(AUDIOS_PER_PAGE);

                } catch (NumberFormatException e) {
                    log.error("Folder name must contains page number: {}", folderNameSplit[1]);
                }
            }

        } else {

            log.error("Current folder cannot be null to calculate offset");
        }

        return ret;
    }

    /**
     * @return java POJO representing JSON
     */
    private Root getJsonBean(int offset) {

        Root ret = null;

        try {

            ObjectMapper objectMapper = new ObjectMapper();

            ret = objectMapper.readValue(getJsonStringFromUrl(appConfig.getParameter(JSON_URL) + "?offset=" + offset), Root.class);

        } catch (IOException e) {
            log.error("Error trying to map JSON {} to bean: {}", e.getMessage(), e);
        }

        return ret;
    }

    /**
     * @param currentAudio
     * @param jsonMainObject Renames audio and fills it based on information
     *                       contained into a defined JSON
     */
    private void processAudio(File currentAudio, Root jsonMainObject) {

        String[] audioNameSplit = currentAudio.getName().split(appConfig.getParameter(AUDIOS_EXTENSION));
        boolean audioNameObtained = audioNameSplit.length > 0;
        if (audioNameObtained) {

            String audioId = audioNameSplit[0];
            Item item = getMatchItem(audioId, jsonMainObject);
            if (item != null) {

                String newAudioName = getFriendlyAudioName(item);

                File newFile = new File(currentAudio.getParent(), newAudioName);
                if (currentAudio.renameTo(newFile)) {

                    MediaFile audioFile = new MP3File(newFile);
                    try {

                        ID3V2Tag tag = audioFile.getID3V2Tag();
                        if (tag == null) {

                            log.info("Cannot obtain ID3 information, trying to create it from scratch...");
                            tag = new ID3V2_3_0Tag();

                        }
                        setId3TagInformation(item, newAudioName, tag, audioFile);

                    } catch (ID3Exception e) {
                        log.error("Error trying to rename MP3 newFile: {}", newFile.getAbsolutePath());
                    }

                } else {
                    log.error("Matched item not found: {}", audioId);
                }
            } else {
                log.error("Matched item not found: {}", audioId);
            }

        } else {

            log.error("Cannot obtain audio ID of file {}", currentAudio.getName());
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
    private static Item getMatchItem(String audioId, Root jsonMainObject) {

        Item ret = null;

        List<Item> itemList = jsonMainObject.getPage().getItems();
        forItem:
        for (Item item : itemList) {

            for (Quality currentQuality : item.getQualities()) {

                if (StringUtils.contains(currentQuality.getFilePath(), audioId + AUDIOS_EXTENSION)) {

                    ret = item;
                    break forItem;
                }
            }
        }

        return ret;
    }

    /**
     * @param item
     * @return friendly audio name
     */
    private String getFriendlyAudioName(Item item) {

        String ret = StringUtils.EMPTY;

        if (item != null) {

            String sb = getInvertedDate(item.getDateOfEmission()) + "_" +

                    // Prevent problems with slash characters
                    StringUtils.replace(item.getShortTitle(), "/", "-") +
                    AUDIOS_EXTENSION;
            ret = sb;

            log.info("Friendly audio name obtained: {}", ret);
        } else {

            log.error("Item cannot be null");
        }

        return ret;
    }

    /**
     * @param dateOfEmission
     * @return date of emission in inverted way
     */
    private String getInvertedDate(String dateOfEmission) {

        String ret = null;

        SimpleDateFormat originalFormat = new SimpleDateFormat(appConfig.getParameter(ORIGINAL_DATE_PATTERN));
        SimpleDateFormat invertedFormat = new SimpleDateFormat(appConfig.getParameter(DATE_INVERTED_PATTERN));

        try {

            Date dateOriginal = originalFormat.parse(dateOfEmission);
            ret = invertedFormat.format(dateOriginal);

        } catch (ParseException e) {
            log.error("Error parsing date of emission {}", dateOfEmission);
        }

        return ret;
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

        InputStream is = new FileInputStream(jsonFilePath);
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }

    /**
     * @param currentFolder
     * @return array with files that represents audios of a defined extension
     */
    private File[] getAudios(File currentFolder) {

        File[] ret = null;

        if (currentFolder != null) {

            ret = currentFolder.listFiles(new FilenameFilter() {
                public boolean accept(File current, String name) {
                    return name.toLowerCase().endsWith(appConfig.getParameter(AUDIOS_EXTENSION));
                }
            });

        } else {

            log.error("Current folder where to find audios cannot be null");
        }

        return ret;
    }

    /**
     * @param mainFolderPath
     * @return array with folders that represents pages of audios
     */
    private static File[] getPagesFolders(String mainFolderPath) {

        File[] ret = null;

        if (StringUtils.isNotEmpty(mainFolderPath)) {

            File mainFolder = new File(mainFolderPath);
            if (mainFolder.exists()) {

                ret = mainFolder.listFiles(new FilenameFilter() {
                    public boolean accept(File current, String name) {
                        return new File(current, name).isDirectory();
                    }
                });
            } else {
                log.error("The working folder {} do not exits. Have a look at MAIN_FOLDER_PATH", mainFolder);
            }

        } else {

            log.error("The working folder must be. Have a look at MAIN_FOLDER_PATH");
        }

        return ret;
    }
}
