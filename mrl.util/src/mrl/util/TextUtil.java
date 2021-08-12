package mrl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class TextUtil {
	
	private static BufferedWriter bw;
	private static BufferedReader br;

	public static void openWriter(String file){
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
		} catch (FileNotFoundException e) {
			throw new RuntimeException();
		}
	}
	public static void write(String text) {
		try {
			bw.write(text);
			bw.flush();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	public static void writeLine(String text) {
		write(text + "\r\n");
	}
	public static void closeWriter() {
		try {
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	public static void openReader(String file){
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public static String readLine() {
		try {
			return br.readLine();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	public static void closeReader() {
		try {
			br.close();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	public static void write(String file, double[][] data) {
		openWriter(file);
		for (double[] v : data) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < v.length; i++) {
				if (i > 0) sb.append("\t");
				sb.append(v[i]);
			}
			writeLine(sb.toString());
		}
		closeWriter();
	}
	
	public static double[][] readDoubleArray(String file){
		openReader(file);
		ArrayList<double[]> dataList = new ArrayList<double[]>();
		String line;
		while ((line = readLine()) != null) {
			String[] tokens = line.trim().split("\t");
			double[] data = new double[tokens.length];
			for (int i = 0; i < data.length; i++) {
				data[i] = Double.parseDouble(tokens[i]);
			}
			dataList.add(data);
		}
		closeReader();
		return Utils.toArray(dataList);
	}
	
	public static void readLines(String file, TextLineReader reader) {
		openReader(file);
		String line;
		while ((line = readLine()) != null) {
			reader.process(line);
		}
		closeReader();
	}
	
	public interface TextLineReader{
		public void process(String line);
	}
	
}
