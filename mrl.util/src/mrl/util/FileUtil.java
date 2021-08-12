package mrl.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class FileUtil {
	
	public static void writeSparseArray(ObjectOutputStream os, double[][] array, double limit) throws IOException{
		int sum = 0;
		ArrayList<ArrayList<Integer>> indexList = new ArrayList<ArrayList<Integer>>();
		for (double[] data : array) {
			ArrayList<Integer> index = new ArrayList<Integer>();
			for (int i = 0; i < data.length; i++) {
				if (data[i] <= limit){
					index.add(i);
					sum++;
				}
			}
			indexList.add(index);
		}
		System.out.println("mean : " + sum/array.length);
		
		os.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			ArrayList<Integer> index = indexList.get(i);
			os.writeInt(index.size());
			for (int idx : index) {
				os.writeInt(idx);
			}
		}
		
		os.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			ArrayList<Integer> index = indexList.get(i);
			os.writeInt(index.size());
			for (int idx : index) {
				os.writeDouble(array[i][idx]);
			}
		}
	}
	
	public static double[][] readSparseArray(ObjectInputStream is, double limit) throws IOException{
		int[][] indexList = readIntArray2(is);
		double[][] valueList = readDoubleArray2(is);
		
		int size = indexList.length;
		double[][] result = new double[size][size];
		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < result.length; j++) {
				result[i][j] = limit;
			}
			
			for (int j = 0; j < indexList[i].length; j++) {
				int idx = indexList[i][j];
				result[i][idx] = valueList[i][j];
			}
		}
		return result;
	}
	
	public static void writeArray(ObjectOutputStream os, double[][] array) throws IOException{
		os.writeInt(array.length);
		for (double[] data : array) {
			writeArray(os, data);
		}
	}
	public static void writeArray(ObjectOutputStream os, double[] array) throws IOException{
		os.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			os.writeDouble(array[i]);
		}
	}
	public static double[][] readDoubleArray2(ObjectInputStream is) throws IOException{
		int size = is.readInt();
		double[][] array = new double[size][];
		for (int i = 0; i < array.length; i++) {
			array[i] = readDoubleArray(is);
		}
		return array;
		
	}
	public static double[] readDoubleArray(ObjectInputStream is) throws IOException{
		int size = is.readInt();
		double[] array = new double[size];
		for (int i = 0; i < array.length; i++) {
			array[i] = is.readDouble();
		}
		return array;
	}
	
	
	public static void writeArray(ObjectOutputStream os, boolean[][] array) throws IOException{
		os.writeInt(array.length);
		for (boolean[] data : array) {
			os.writeInt(data.length);
			for (int i = 0; i < data.length; i++) {
				os.writeBoolean(data[i]);
			}
		}
	}
	public static void writeArray(ObjectOutputStream os, boolean[] array) throws IOException{
		os.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			os.writeBoolean(array[i]);
		}
	}
	public static boolean[][] readBooleanArray2(ObjectInputStream is) throws IOException{
		int size = is.readInt();
		boolean[][] array = new boolean[size][];
		for (int i = 0; i < array.length; i++) {
			array[i] = readBooleanArray(is);
		}
		return array;
		
	}
	public static boolean[] readBooleanArray(ObjectInputStream is) throws IOException{
		int size = is.readInt();
		boolean[] array = new boolean[size];
		for (int i = 0; i < array.length; i++) {
			array[i] = is.readBoolean();
		}
		return array;
	}
	
	public static void writeArray(ObjectOutputStream os, int[][] array) throws IOException{
		os.writeInt(array.length);
		for (int[] data : array) {
			os.writeInt(data.length);
			for (int i = 0; i < data.length; i++) {
				os.writeInt(data[i]);
			}
		}
	}
	public static void writeArray(ObjectOutputStream os, int[] array) throws IOException{
		os.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			os.writeInt(array[i]);
		}
	}
	public static int[][] readIntArray2(ObjectInputStream is) throws IOException{
		int size = is.readInt();
		int[][] array = new int[size][];
		for (int i = 0; i < array.length; i++) {
			array[i] = readIntArray(is);
		}
		return array;
		
	}
	public static int[] readIntArray(ObjectInputStream is) throws IOException{
		int size = is.readInt();
		int[] array = new int[size];
		for (int i = 0; i < array.length; i++) {
			array[i] = is.readInt();
		}
		return array;
	}
	
	
	
	public static <E> void saveArray(ObjectOutputStream os, ElementSerializer<E> s, E[] array) throws Exception{
		os.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			s.save(os, array[i]);
		}
	}
	public static <E> void saveArray2(ObjectOutputStream os, ElementSerializer<E> s, E[][] array) throws Exception{
		os.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			saveArray(os, s, array[i]);
		}
	}
	public static <E> E[] loadArray(ObjectInputStream oi, ElementSerializer<E> s) throws Exception{
		int n = oi.readInt();
		@SuppressWarnings("unchecked")
		E[] array = (E[])Array.newInstance(s.elementClass(), n);
		for (int i = 0; i < array.length; i++) {
			array[i] = s.load(oi);
		}
		return array;
	}
	public static <E> E[][] loadArray2(ObjectInputStream oi, ElementSerializer<E> s) throws Exception{
		int n = oi.readInt();
		@SuppressWarnings("unchecked")
		E[][] array = (E[][])Array.newInstance(s.elementClass(), n, 0);
		for (int i = 0; i < array.length; i++) {
			array[i] = loadArray(oi, s);
		}
		return array;
	}
	
	public static ObjectOutputStream outputStream(String file) throws IOException{
		return new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	}
	
	public static ObjectInputStream inputStream(String file) throws IOException{
		return new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
	}
	
	public static DataOutputStream dataOutputStream(String file) throws IOException{
		return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	}
	
	public static DataInputStream dataInputStream(String file) throws IOException{
		return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
	}
	
	public static void writeArray(DataOutputStream os, double[][] array) throws IOException{
		os.writeInt(array.length);
		for (double[] data : array) {
			os.writeInt(data.length);
			for (int i = 0; i < data.length; i++) {
				os.writeDouble(data[i]);
			}
		}
	}
	public static double[][] readDoubleArray2(DataInputStream is) throws IOException{
		int size = is.readInt();
		double[][] array = new double[size][];
		for (int i = 0; i < array.length; i++) {
			array[i] = readDoubleArray(is);
		}
		return array;
		
	}
	public static double[] readDoubleArray(DataInputStream is) throws IOException{
		int size = is.readInt();
		double[] array = new double[size];
		for (int i = 0; i < array.length; i++) {
			array[i] = is.readDouble();
		}
		return array;
	}
	
	public static void writeObject(Object obj, String file){
		try {
			ObjectOutputStream stream = outputStream(file);
			stream.writeObject(obj);
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Object readObject(String file){
		try {
			if (!isFileExist(file)) return null;
			ObjectInputStream stream = inputStream(file);
			Object obj = stream.readObject();
			stream.close();
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void writeAsString(String[] data, String file){
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			for (String line : data){
				bw.write(line + "\r\n");
			}
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	public static String[] readAsString(String file){
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			ArrayList<String> list = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
			br.close();
			return Utils.toArray(list);
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}
	public static ArrayList<double[]> readDoubleFromString(String file){
		String[] lines = readAsString(file);
		ArrayList<double[]> dataList = new ArrayList<double[]>();
		for (String line : lines){
			line = line.trim();
			if (line.length() == 0) continue;
			String[] tokens = line.split("\t");
			double[] data = new double[tokens.length];
			for (int i = 0; i < data.length; i++) {
				data[i] = Double.parseDouble(tokens[i]);
			}
			dataList.add(data);
		}
		return dataList;
	}
	
	public static boolean isFileExist(String file){
		return new File(file).exists();
	}
}
