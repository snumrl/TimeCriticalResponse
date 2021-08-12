package mrl.motion.neural.agility.measure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

import mrl.motion.neural.agility.StuntLocoModel;
import mrl.motion.neural.agility.StuntModel;
import mrl.motion.neural.agility.measure.HistogramViewer.HData;
import mrl.util.Pair;
import mrl.util.Utils;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;
import mrl.widget.app.MainApplication.WindowPosition;

public class HistogramViewerModule extends Module{

	String outputFolder = "output\\stuntLocoAgility2\\";
	@Override
	protected void initializeImpl() {
		
		int[] actions = {
				6, 7,
				8, 9
		};
		for (int agility = 15; agility <= 55; agility+=10) {
//			int agility = 15;
			System.out.println("double[][] agility_" + agility + " = {");
			for (int action : actions) {
				double[] stat = collect(action, agility);
						
				System.out.print("\t{");
				for (int i = 0; i < stat.length; i++) {
					System.out.print(" " + stat[i] + ",");
				}
				System.out.println("},");
			}
			System.out.println("};");
		}
		System.exit(0);
		
		
//		HistogramViewer viewer = addWindow(new HistogramViewer(dummyParent()), WindowPosition.Main);
//		{
//			HistogramData d2 = new HistogramData();
//			for (int i = 15; i <= 55; i+=10) {
////				System.out.println("dat a: " + i);
//				d2.load(outputFolder + "stunt_loco_ue_CTime_ef05_t8_a" + i + ".dat");
//				collect(d2);
////				viewer.setHistogram(d2, StuntLocoModel.actionTypes);
//				
//			}
//			System.exit(0);
//		}
		
//		HistogramData data = new HistogramData();
//		data.load(outputFolder + "stunt_loco_ue_CTime_ef05_t8_a10.dat");
//		System.exit(0);
//		viewer.setHistogram(data, StuntLocoModel.actionTypes);
//		data.load(file);
	}
	
	double[] collect(int action, int agility) {
		HistogramData histogram = new HistogramData();
		histogram.load(outputFolder + "stunt_loco_ue_CTime_ef05_t" + action + "_a" + agility + ".dat");
		
		ArrayList<HData> dList = new ArrayList<HData>();
		double maxValue = -1;
		for (Entry<Pair<Integer, Integer>, ArrayList<Double>> entry : histogram.histogram.entrySet()) {
			Pair<Integer, Integer> key = entry.getKey();
			if (key.second == 0) continue;
			HData data = new HData();
			data.key = new int[] { key.first, key.second };
			data.limit = histogram.limitMap.get(key);
			maxValue = Math.max(maxValue, data.limit);
			ArrayList<Double> filteted = new ArrayList<Double>();
			for (double v : entry.getValue()) {
				if (v > 60) continue;
				filteted.add(v);
			}
			data.values = Utils.toDoubleArray(filteted);
//			data.values = Utils.toDoubleArray(entry.getValue());
			data.checkFailRatio();
			dList.add(data);
		}
		HData[] dataList = Utils.toArray(dList);
		double[] stat = null;
		for (int i = 0; i < dataList.length; i++) {
			HData data = dataList[i];
			if (data.key[0] == 3 && data.key[1] != 3) {
//				System.out.println(Arrays.toString(data.key) + " : " + data.values.length + " : " + data.failRatio + " : " + data.limit + " : " + Arrays.toString(data.stat));
				stat = data.stat;
			}
		}
		return stat;
	}

	
	public static void main(String[] args) {
		MainApplication.run(new HistogramViewerModule());
	}
}
