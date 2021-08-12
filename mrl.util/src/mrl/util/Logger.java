package mrl.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

	
	private static BufferedWriter bw;
	private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
	
	public static void line(){
		line("");
	}
	
	public static void line(String str){
		try{
			str = String.format("[%s]\t%s\r\n", timeFormat.format(new Date()), str);
			if (bw != null){
				bw.write(str);
				bw.flush();
			}
			System.out.print(str);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static void startLogging(){
		startLogging("log.txt");
	}
	public static void startLogging(String logFile){
		try {
			boolean isFileExisted = new File(logFile).exists();
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true)));
			if (isFileExisted){
				bw.write("\r\n\r\n\r\n");
			}
			
			line("------------ Logging start ------------");
			SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			line("- Logging Start at : " + format.format(new Date()));
			line();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
