package mrl.motion.neural.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import mrl.util.FileUtil;
import mrl.util.Pair;

public class DataExtractor {
	
	public static double STD_LIMIT = -1;
	public static double MIN_STD = 0.01;
//	public static double STD_LIMIT = 0.0001;
//	public static double MIN_STD = 0.01;
	public static int POSE_LENGTH = 999;
	
	public static void writeData(String file, ArrayList<double[]> dataList){
		try {
			DataOutputStream stream = FileUtil.dataOutputStream(file);
			int dataLen = dataList.get(0).length;
			int dataSize = dataList.size();
			stream.writeInt(dataLen);
			stream.writeInt(dataSize);
			System.out.println(String.format("write data : %s : len=%d, size=%d", file, dataLen, dataSize));
			
			for (double[] data : dataList){
				for (double v : data){
					stream.writeDouble(v);
				}
			}
			
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public static void writeListData(String file, ArrayList<ArrayList<double[]>> dataList){
		try {
			DataOutputStream stream = FileUtil.dataOutputStream(file);
			int dataLen = dataList.get(0).get(0).length;
			int dataSize = dataList.size();
			stream.writeInt(dataLen);
			stream.writeInt(dataSize);
			int sum = 0;
			for (ArrayList<double[]> list : dataList){
				sum += list.size();
			}
			System.out.println(String.format("write list data : %s : len=%d, size=%d, total=%d", file, dataLen, dataSize, sum));
			
			for (ArrayList<double[]> list : dataList){
				stream.writeInt(list.size());
				for (double[] data : list){
					for (double v : data){
						stream.writeDouble(v);
					}
				}
			}
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static double[][] writeNormalizeInfo(String file, ArrayList<double[]> dataList){
		return writeNormalizeInfo(file, dataList, null);
	}
	
	public static double[][] writeNormalizeInfo(String file, ArrayList<double[]> dataList, boolean[] markingList){
		double markMean = 0;
		double markStd = 1;
		
		int dataLen = dataList.get(0).length;
		double[] mean = new double[dataLen];
		int[] countList = new int[dataLen];
		for (double[] data : dataList){
			for (int i = 0; i < mean.length; i++) {
				if (Double.isNaN(data[i])) continue;
				mean[i] += data[i];
				countList[i]++;
			}
		}
		for (int i = 0; i < mean.length; i++) {
			mean[i] /= countList[i];
			if (markingList != null && markingList.length > i && markingList[i]){
				mean[i] = markMean;
			}
		}
		double[] std = new double[dataLen];
		for (double[] data : dataList){
			for (int i = 0; i < mean.length; i++) {
				if (Double.isNaN(data[i])) continue;
				double diff = mean[i] - data[i];
				std[i] += diff*diff;
			}
		}
		for (int i = 0; i < std.length; i++) {
			std[i] = Math.sqrt(std[i]/countList[i]);
			if (std[i] < STD_LIMIT){
				std[i] = STD_LIMIT;
			} else if (i > std.length - POSE_LENGTH && std[i] < MIN_STD){
				std[i] = MIN_STD;
			}
			if (markingList != null && markingList.length > i && markingList[i]){
				std[i] = markStd;
			}
		}
		writeNormalizeInfo(file, mean, std);
		return new double[][]{ mean, std };
	}
	
	public static void writeNormalizeInfo(String file, double[] mean, double[] std) {
		try {
			DataOutputStream normal = FileUtil.dataOutputStream(file);
			normal.writeInt(mean.length);
			for (int i = 0; i < mean.length; i++) {
				normal.writeDouble(mean[i]);
			}
			for (int i = 0; i < std.length; i++) {
				normal.writeDouble(std[i]);
			}
			normal.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static double[][] readNormalizeInfo(String normalFile){
		try {
			DataInputStream is = FileUtil.dataInputStream(normalFile);
			int dataLen = is.readInt();
			double[] mean = new double[dataLen];
			for (int i = 0; i < mean.length; i++) {
				mean[i] = is.readDouble();
			}
			double[] std = new double[dataLen];
			for (int i = 0; i < std.length; i++) {
				std[i] = is.readDouble();
			}
			is.close();
			return new double[][]{ mean, std };
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static void writeDataWithNormalize(String file, ArrayList<double[]> dataList, String normalFile){
		try {
			DataOutputStream stream = FileUtil.dataOutputStream(file);
			int dataSize = dataList.size();
			int dataLen = dataList.get(0).length;
			
			double[][] meanAndStd = readNormalizeInfo(normalFile);
			double[] mean = meanAndStd[0];
			double[] std = meanAndStd[1];
			Pair<boolean[], Integer> valid = checkStdValid(std);
			boolean[] isValid = valid.first;
			dataLen = valid.second;
			ArrayList<Integer> removed = new ArrayList<Integer>();
			for (int i = 0; i < isValid.length; i++) {
				if (!isValid[i]){
					removed.add(i);
				}
			}
			
			stream.writeInt(dataLen);
			stream.writeInt(dataSize);
			System.out.println(String.format("write data : %s : len=%d, size=%d, originLen=%d, removed=%s", file, dataLen, dataSize, mean.length, Arrays.toString(removed.toArray(new Integer[0]))));
			for (double[] data : dataList){
				for (int i = 0; i < data.length; i++) {
					if (isValid[i]){
						double v = Double.isNaN(data[i]) ? mean[i] : data[i];
						stream.writeDouble((v - mean[i])/std[i]);
					}
				}
			}
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Pair<boolean[], Integer> checkStdValid(double[] std){
		boolean[] isValid = new boolean[std.length];
		int validCount = 0;
		for (int i = 0; i < isValid.length; i++) {
			if (std[i] > STD_LIMIT){
				validCount++;
				isValid[i] = true;
			} else {
				isValid[i] = false;
			}
		}
		return new Pair<boolean[], Integer>(isValid, validCount);
	}
	
	public static ArrayList<ArrayList<double[]>> readListData(String file){
		try {
			DataInputStream stream = FileUtil.dataInputStream(file);
			int dataLen = stream.readInt();
			int dataSize = stream.readInt();
			
			ArrayList<ArrayList<double[]>> totalList = new ArrayList<ArrayList<double[]>>();
			
			for (int dIdx = 0; dIdx < dataSize; dIdx++) {
				ArrayList<double[]> dataList = new ArrayList<double[]>();
				int len = stream.readInt();
				for (int sIdx = 0; sIdx < len; sIdx++) {
					double[] data = new double[dataLen];
					for (int j = 0; j < data.length; j++) {
						data[j] = stream.readDouble();
					}
					dataList.add(data);
				}
				totalList.add(dataList);
			}
			return totalList;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static ArrayList<double[]> readData(String file){
		try {
			DataInputStream stream = FileUtil.dataInputStream(file);
			int dataLen = stream.readInt();
			int dataSize = stream.readInt();
			ArrayList<double[]> dataList = new ArrayList<double[]>();
			for (int dIdx = 0; dIdx < dataSize; dIdx++) {
				double[] data = new double[dataLen];
				for (int j = 0; j < data.length; j++) {
					data[j] = stream.readDouble();
				}
				dataList.add(data);
			}
			return dataList;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static ArrayList<double[]> getNormalizedData(ArrayList<double[]> inputData, double[][] meanAndStd){
		ArrayList<double[]> result = new ArrayList<double[]>();
		for (double[] v : inputData){
			result.add(getNormalizedData(v, meanAndStd));
		}
		return result;
	}
	public static double[] getNormalizedData(double[] inputData, double[][] meanAndStd){
		if (inputData == null) return null;
		double[] mean = meanAndStd[0];
		double[] std = meanAndStd[1];
		Pair<boolean[], Integer> valid = DataExtractor.checkStdValid(std);
		boolean[] isValid = valid.first;
		int len = valid.second;
		if (inputData.length != mean.length){
			throw new RuntimeException("length not match :: " + inputData.length + " / " +  mean.length);
		}
		double[] data = new double[len];
		int idx = 0;
		for (int i = 0; i < inputData.length; i++) {
			if (isValid[i]){
				double v = Double.isNaN(inputData[i]) ? mean[i] : inputData[i];
				data[idx] = (v - mean[i])/std[i];
				idx++;
			}
		}
		return data;
	}
	
	public static ArrayList<double[]> getUnnormalizedOutput(ArrayList<double[]> values, double[][] meanAndStd){
		ArrayList<double[]> result = new ArrayList<double[]>();
		for (double[] v : values){
			result.add(getUnnormalizedOutput(v, meanAndStd));
		}
		return result;
	}
	
	public static double[] getUnnormalizedOutput(double[] values, double[][] meanAndStd){
		double[] mean = meanAndStd[0];
		double[] std = meanAndStd[1];
		Pair<boolean[], Integer> valid = DataExtractor.checkStdValid(std);
		boolean[] isValid = valid.first;
		
		double[] data = new double[mean.length];
		int idx = 0;
		for (int i = 0; i < data.length; i++) {
			if (isValid[i]){
				data[i] = values[idx]*std[i] + mean[i];
				idx++;
			} else {
				data[i] = mean[i];
			}
		}
		return data;
	}
	public static double[] getUnnormalizedOutput(double[] values, double[][] meanAndStd, int offset){
		double[] mean = meanAndStd[0];
		double[] std = meanAndStd[1];
		double[] data = new double[values.length];
		for (int i = 0; i < data.length; i++) {
			data[i] = values[i]*std[offset + i] + mean[offset + i];
		}
		return data;
	}
}
