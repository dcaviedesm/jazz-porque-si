# Jazz porque sí

## Summary
Java project used to rename and tag MP3 metadata of all previously downloaded RTVE "Jazz porque sí" radio programs.

Basically the project logic is:
- Go through each audio in each folder, and for each one:
- Get your information from the RTVE API
- Rename the audio according to the format: yyMMdd_TitleFromRtveApi.extension
- Set some MP3 metadata in the audio (initially title and description)

## Details
This Java project (uses Maven) has the class "App.java" as its central class. In order for it to work, several aspects must be taken into account:
* The "Jazz porque sí" programs have to be organized by folders as they are on the RTVE website, where they are organized by pages of 20 audios in this case, therefore, in any directory, there must be a folder for each website of the program "Jazz porque sí" of RTVE
* Said folders must be named according to the following format: "XXX YY", where "XXX" can be anything (in our case we put the string "Pag") and "YY" must be the page number corresponding to the RTVE website (eg "01")
* Each folder will contain the audios corresponding to its equivalent page on the RTVE website
* The initial name of the audios will be the same original name existing when downloading each audio of "Jazz porque sí", which is a number followed by the MP3 extension (eg "1552503521627.mp3")

## Configuration
The above aspects are important as the Java project assumes them:
Within the main class of the project, there are a series of defined constants, some of which need to be customized for each case. We indicate the meaning of each of them:
* mainFolderPath: physical path where the folders that represent the audio pages are located (Page 01, Page 02, etc)
* audiosExtension: extension of the audio files
* jsonUrl: base URL of the RTVE API to obtain the information of the audios of the "Jazz porque sí" program (whose RTVE identifier is "1999")
* jsonPath: physical path in case we had a single JSON with all the information of the audios (obsolete)
* dateInvertedPattern: date pattern that will be used to rename the beginning of the names of each audio
* originalDatePattern: original date / time pattern from the RTVE API
* audiosPerPage: number of audios per page on the RTVE website
