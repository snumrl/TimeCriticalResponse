package mrl.motion.neural.agility.measure;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import mrl.util.FileUtil;
import mrl.util.Pair;

public class HistogramData{
	
	public HashMap<Pair<Integer, Integer>, ArrayList<Double>> histogram = new HashMap<>();
	public HashMap<Pair<Integer, Integer>, Double> limitMap = new HashMap<>();
	
	public HistogramData() {
	}
	
	public void addValue(double value, int aPrev, int aPost) {
		Pair<Integer, Integer> key = new Pair<Integer, Integer>(aPrev, aPost);
		ArrayList<Double> list = histogram.get(key);
		if (list == null) {
			list = new ArrayList<Double>();
			histogram.put(key, list);
		}
		list.add(value);
	}
	
	public void setLimitValue(double limit, int aPrev, int aPost) {
		Pair<Integer, Integer> key = new Pair<Integer, Integer>(aPrev, aPost);
		limitMap.put(key, limit);
	}
	
	public ArrayList<Double> getValueList(int aPrev, int aPost) {
		Pair<Integer, Integer> key = new Pair<Integer, Integer>(aPrev, aPost);
		ArrayList<Double> list = histogram.get(key);
		if (list == null) {
			list = new ArrayList<Double>();
		}
		return list;
	}
	
	public void save(String file) {
		try {
			new File(file).getParentFile().mkdirs();
			DataOutputStream os = FileUtil.dataOutputStream(file);
			os.writeInt(histogram.size());
			for (Entry<Pair<Integer, Integer>, ArrayList<Double>> entry : histogram.entrySet()) {
				Pair<Integer, Integer> key = entry.getKey();
				os.writeInt(key.first);
				os.writeInt(key.second);
				os.writeDouble(limitMap.get(key));
				ArrayList<Double> values = entry.getValue();
				os.writeInt(values.size());
				for (Double v : values) {
					os.writeDouble(v);
				}
			}
			os.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void load(String file) {
		try {
			DataInputStream is = FileUtil.dataInputStream(file);
			int dSize = is.readInt();
			for (int dIdx = 0; dIdx < dSize; dIdx++) {
				Pair<Integer, Integer> key = new Pair<Integer, Integer>(is.readInt(), is.readInt());
				limitMap.put(key, is.readDouble());
				int vSize = is.readInt();
				ArrayList<Double> vList = new ArrayList<Double>();
				for (int i = 0; i < vSize; i++) {
					vList.add(is.readDouble());
				}
				histogram.put(key, vList);
			}
			is.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}