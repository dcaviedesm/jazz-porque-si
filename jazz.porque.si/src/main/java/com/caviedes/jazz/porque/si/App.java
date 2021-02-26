package com.caviedes.jazz.porque.si;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.MediaFile;
import org.blinkenlights.jid3.v2.ID3V2Tag;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;

import com.caviedes.jazz.porque.si.json.pojos.Item;
import com.caviedes.jazz.porque.si.json.pojos.Quality;
import com.caviedes.jazz.porque.si.json.pojos.Root;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {

	private static final Logger log = LogManager.getLogger(App.class);

	private static final String mainFolderPath = "C:\\Temp\\Jazz porque si";
	private static final String audiosExtension = ".mp3";
	private static final String jsonUrl = "http://www.rtve.es/api/programas/1999/audios.json";
	private static final String jsonPath = "E:\\Descargas\\Jazz_porque_si_audios.json";
	private static final String dateInvertedPattern = "yyMMdd";
	private static final String originalDatePattern = "dd-MM-yyyy HH:mm:ss";
	private static final int audiosPerPage = 20;

	public static void main(String[] args) {

		log.info("Loading information from JSON to mp3's...");

		int offset = 0;
		for (File currentFolder : getPagesFolders(mainFolderPath)) {

			log.info("Processing page folder " + currentFolder.getName());
			
			offset = getCurrentOffset(currentFolder);
			
			Root jsonMainObject = getJsonBean(offset);

			if (jsonMainObject != null) {

				for (File currentAudio : getAudios(currentFolder)) {

					log.info("Processing audio " + currentAudio.getName());

					processAudio(currentAudio, jsonMainObject);
				}
				
			} else {

				log.error("It is necessary a JSON to process");
			}
		}
	}

	/**
	 * @param currentFolder
	 * @return current offset based on folder name, that must have the same format: "XXX YY", where "YY" represents the page number of audios
	 */
	private static int getCurrentOffset(File currentFolder) {

		int ret = 0;
		
		if (currentFolder != null) {
			
			String[] folderNameSplitted = currentFolder.getName().split(StringUtils.SPACE);
			boolean isFolderNameSplitted = (folderNameSplitted.length == 2);
			if (isFolderNameSplitted) {
				
				try {
					
					Integer pageNumber = Integer.valueOf(folderNameSplitted[1]);
					ret = (pageNumber*audiosPerPage) - audiosPerPage;
					
				} catch (NumberFormatException e) {
					log.error("Folder name must contains page number: " + folderNameSplitted[1]);
				}
			}
			
		}else {
			
			log.error("Current folder cannot be null to calculate offset");
		}
		
		return ret;
	}

	/**
	 * @return java POJO representing JSON
	 */
	private static Root getJsonBean(int offset) {

		Root ret = null;

		try {

			ObjectMapper objectMapper = new ObjectMapper();

			ret = objectMapper.readValue(getJsonStringFromUrl(jsonUrl + "?offset=" + offset), Root.class);

		} catch (JsonMappingException e) {
			log.error("Error trying to map JSON " + jsonPath + " to bean: " + e.getMessage());
		} catch (JsonProcessingException e) {
			log.error("Error trying to map JSON " + jsonPath + " to bean: " + e.getMessage());
		} catch (IOException e) {
			log.error("Error trying to map JSON " + jsonPath + " to bean: " + e.getMessage());
		} finally {

			return ret;
		}
	}

	/**
	 * @param currentAudio
	 * @param jsonMainObject
	 * 
	 *                       Renames audio and fills it based on information
	 *                       contained into a defined JSON
	 */
	private static void processAudio(File currentAudio, Root jsonMainObject) {

		String[] audioNameSplitted = currentAudio.getName().split(audiosExtension);
		boolean audioNameObtained = audioNameSplitted != null;
		if (audioNameObtained) {

			String audioId = audioNameSplitted[0];
			Item item = getMatchItem(audioId, jsonMainObject);
			if (item != null) {
				
				String newAudioName = getFriendlyAudioName(item);
				
				File newFile = new File(currentAudio.getParent(), newAudioName);
				currentAudio.renameTo(newFile);
				
				MediaFile audioFile = new MP3File(newFile);
			    try {
			    	
			    	ID3V2Tag tag = audioFile.getID3V2Tag();
			    	if (tag != null) {

			    		setId3TagInformation(item, newAudioName, tag, audioFile);
				        
					}else {
						
						log.info("Cannot obtain ID3 information, trying to create it from scratch...");
						tag = new ID3V2_3_0Tag();
						setId3TagInformation(item, newAudioName, tag, audioFile);
						
					}

			      } catch (ID3Exception e) {
			        log.error("Error trying to get MP3 metadata: " + e.getMessage());
			      }
			}else {
				log.error("Matched item not found: " + audioId);
			}

		} else {

			log.error("Cannot obtain audio ID of file " + currentAudio.getName());
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
			log.error("Error trying to get MP3 metadata: " + e.getMessage());
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
		forItem:for (Item item : itemList) {

			for (Quality currentQuality : item.getQualities()) {
				
				if (StringUtils.contains(currentQuality.getFilePath(), audioId + audiosExtension)) {

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
	private static String getFriendlyAudioName(Item item) {

		String ret = StringUtils.EMPTY;

		if (item != null) {

			StringBuilder sb = new StringBuilder(getInvertedDate(item.getDateOfEmission()));
			
			sb.append("_");
			
			// Prevent problems with slash characters
			sb.append(StringUtils.replace(item.getShortTitle(), "/", "-"));
			
			sb.append(audiosExtension);
			
			ret = sb.toString();

			log.info("Friendly audio name obtained: " + ret);
		} else {

			log.error("Item cannot be null");
		}

		return ret;
	}

	/**
	 * @param dateOfEmission
	 * @return date of emission in inverted way
	 */
	private static String getInvertedDate(String dateOfEmission) {

		String ret = null;

		SimpleDateFormat originalFormat = new SimpleDateFormat(originalDatePattern);
		SimpleDateFormat invertedFormat = new SimpleDateFormat(dateInvertedPattern);

		try {

			Date dateOriginal = originalFormat.parse(dateOfEmission);
			ret = invertedFormat.format(dateOriginal);

		} catch (ParseException e) {
			log.error("Error parsing date of emission " + dateOfEmission);
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
		return IOUtils.toString(is, "UTF-8");
	}

	/**
	 * @param currentFolder
	 * @return array with files that represents audios of a defined extension
	 */
	private static File[] getAudios(File currentFolder) {

		File[] ret = null;

		if (currentFolder != null) {

			ret = currentFolder.listFiles(new FilenameFilter() {
				public boolean accept(File current, String name) {
					return name.toLowerCase().endsWith(audiosExtension);
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
			if (mainFolder != null) {

				ret = mainFolder.listFiles(new FilenameFilter() {
					public boolean accept(File current, String name) {
						return new File(current, name).isDirectory();
					}
				});
			}

		} else {

			log.error("Current folder where to find pages folders cannot be null");
		}

		return ret;
	}
}
