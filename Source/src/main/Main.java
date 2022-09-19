package main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Main {
	
	// indices that tell the program what launch arguments are
	private static final int URL_INDEX = 0;
	private static final int SAVE_NAME_INDEX = 1;
	private static final int CHECK_TIME_INDEX = 2;
	
	
	// used for various times to check the calendar/date in the program
	private static GregorianCalendar calendar = new GregorianCalendar();
	
	
	// tells the program how to download and save files
	private static String downloadPath = "Downloaded Files/";
	private static File logFile = new File("latest log.txt");
	
	
	
	// main method/entry point of program
	public static void main(String[] args) throws Exception {
		
		log("Welcome to Arrest Downloader.");
		
		// set preset arguments
		URL url = new URL("https://co.jefferson.tx.us/Sheriff/content/documents/inmate/CURRENTARRESTS.PDF");
		String saveName = "Output <date>.PDF";
		int checkTime = 13;
		
		// load launch arguments
		String configFileName = "configuration.txt";
		
		if (args.length > URL_INDEX) url = new URL(args[URL_INDEX]);
		if (args.length > SAVE_NAME_INDEX) saveName = args[SAVE_NAME_INDEX];
		if (args.length > CHECK_TIME_INDEX) checkTime = Integer.parseInt(args[CHECK_TIME_INDEX]);
		
		// load configuration file
		if (Files.exists(Paths.get(configFileName))) {
			
			File configFile = new File(configFileName);
			Scanner configReader = new Scanner(configFile);
			
			while (configReader.hasNextLine()) {
				String line = configReader.nextLine();
				String[] tokens = line.split(" = ");
				
				if (tokens.length != 2) continue;
				
				switch (tokens[0]) {
					case "URL":
						url = new URL(tokens[1]);
						break;
					case "SaveFileName":
						saveName = tokens[1];
						break;
					case "CheckTime":
						checkTime = Integer.parseInt(tokens[1]);
						break;
					default:
						log("Failed to find config named \""+tokens[0]+"\".");
						continue;
				}
				
			}
			
			configReader.close();
		}
		
		// start program
		if (Files.exists(logFile.toPath())) {
			Files.delete(logFile.toPath());
		}
		
		// ask if user wants to manually download the file
		Scanner input = new Scanner(System.in);
		log("Would you like the program to manually download the file now? (type \"y\" or \"n\")");
		String answer = input.nextLine();
		
		while (!answer.equals("y") && !answer.equals("n") ) {
			log("Please enter a valid answer: (\"y\" or \"n\")");
			answer = input.nextLine();
		}
		
		input.close();
		
		if (answer.equals("y")) {
			downloadFile(url, formattedSaveName(saveName));
		} else if (answer.equals("n")) {
			log("File will not be downloaded manually.");
		}
		
		// tell the user the next time a file will be downloaded
		logNextFile(checkTime);
		
		// main loop, breaking this loop will end the program
		while (true) {
			
			// if it is time to check the link
			calendar.setTime(new Date(System.currentTimeMillis()));
			if (calendar.get(GregorianCalendar.HOUR_OF_DAY) == checkTime) {
				
				// check if the file is already downloaded
				boolean fileAlreadyExists = Files.exists(Paths.get(downloadPath+formattedSaveName(saveName)));
				if (!fileAlreadyExists) {
					// download the file if it does not exist
					downloadFile(url,formattedSaveName(saveName));
					logNextFile(checkTime);
				}
			}
			
			// make the main thread sleep
			// lessens the processing power needed to run the program
			Thread.sleep(1000);
		}
	}
	
	
	
	// downloads the PDF at the given URL and saves it with the given saveName to the downloadPath
	private static void downloadFile(URL url, String saveName) throws Exception {
		log("Downloading file at \""+url.toString()+"\".");
		
		try (InputStream in = url.openStream()) {			
			String pathString = downloadPath+saveName;
			Path savePath = Paths.get(pathString);
			
			if (!Files.exists(Paths.get(downloadPath))) {
				Files.createDirectories(savePath);
			}
			
			if (Files.exists(savePath)) {
				Files.delete(savePath);
			}
			
			Files.copy(in, savePath);
			
			log("\""+saveName+"\" saved at \""+downloadPath+"\"");	
		}
	}
	
	
	
	// prints given text to the console/terminal, then saves text in the "latest log.txt" file
	private static void log(String text) throws IOException {
		String logString = getTimeString(System.currentTimeMillis()) + " - " + text;
		
		if (!Files.exists(logFile.toPath())) {
			Files.createFile(logFile.toPath());
		}
		
		Files.write(logFile.toPath(), ("\n"+logString).getBytes(), StandardOpenOption.APPEND);
		
		System.out.println(logString);
	}
	
	
	
	// logs when the next file will download
	private static void logNextFile(int checkTime) throws IOException {
		
		// check if the file will be downloaded later today or some time tomorrow
		calendar.setTime(new Date(System.currentTimeMillis()));
		String todayOrTomorrow = "today";
		if (calendar.get(GregorianCalendar.HOUR_OF_DAY) >= checkTime) {
			todayOrTomorrow = "tomorrow";
		}
		
		// log the result
		log("Next PDF will download "+todayOrTomorrow+" at "+checkTime+".");
	}
	
	
	
	// returns a string containing a date converted from long [time] argument
	private static String getTimeString(long time) {
		calendar.setTime(new Date(time));
		int month = calendar.get(GregorianCalendar.MONTH)+1;
		int day = calendar.get(GregorianCalendar.DAY_OF_MONTH);
		int year = calendar.get(GregorianCalendar.YEAR);
		int hour = calendar.get(GregorianCalendar.HOUR);
		int minute = calendar.get(GregorianCalendar.MINUTE);
		String amPm = "AM";
		if (calendar.get(GregorianCalendar.AM_PM) == 1) amPm = "PM";
		
		String returnString = month+"/"+day+"/"+year+" "+hour+":";
		
		if (minute < 10) returnString = returnString + "0"+minute;
		else returnString = returnString + minute;
		
		returnString = returnString + " " + amPm;
		
		return returnString;
	}
	
	
	
	// formats save name with tags like <date>
	private static String formattedSaveName(String saveName) {
		
		// the <date> tag gets the day the requested file was uploaded by the police station
		if (saveName.contains("<date>")) {
			
			String dateString = "";

			// the following method of getting the formattedSaveName is unreliable
			//    if the following method is used before the file is uploaded to the URL
			//    then the date will be incorrect by one day
			
			calendar.setTime(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)));
			dateString = dateString + (calendar.get(GregorianCalendar.MONTH)+1) + "-";
			dateString = dateString + calendar.get(GregorianCalendar.DAY_OF_MONTH) + "-";
			dateString = dateString + calendar.get(GregorianCalendar.YEAR);
			
			
			saveName = saveName.replaceAll("<date>", dateString);
		}
		
		
		return saveName;
	}
	
}
