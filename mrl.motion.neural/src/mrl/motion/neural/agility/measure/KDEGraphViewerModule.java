package mrl.motion.neural.agility.measure;

import java.io.File;
import java.util.ArrayList;

import javax.swing.plaf.TextUI;

import org.eclipse.swt.SWT;

import mrl.util.FileUtil;
import mrl.util.TextUtil;
import mrl.util.Utils;
import mrl.widget.app.MainApplication;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.app.Module;

public class KDEGraphViewerModule extends Module{

	int[] agilityList = {
			15,
			25,
			35,
	};
	int[] typeList = {
			0,
			1,
			2,
	};
	int maxSampleSize = -1;
	
	@Override
	protected void initializeImpl() {
//		{
//			maxSampleSize = 500;
//			WeightedKDE.USE_POS_PROBABILITY = true;
//			KDEGraphViewer viewer = showControlParameterDistribution("dc_jog2_rot_adap_te02_t10c", 500, 9500, 1000);
////			KDEGraphViewer viewer = showControlParameterDistribution("dc_jog2_rot_adap_te02_t15", 900, 1500, 100);
//			viewer.yGuideList.add(10d);
//			if (agilityList != null) return;
//		}
		
		
//		{
//			showOmmAgility();
//			if (dummyParent() != null) return;
//		}
//		
//		{
//			String outputFolder = "output\\rotBaseCTime";
//			String[] data = {
////					"walk",
//					"jog",
////					"run",
////					"base",
//			};
//			
//			KDEGraphViewer viewer = showData("agility", readData(data, data, outputFolder, ""));
//			viewer.positionStat = new double[] { 0, 0, -3.14, 3.14 };
//			viewer.weightStat = new double[] { 0, 0, 0, 31 };
//			if (dummyParent() != null) return;
//		}
//		
		{
			for (int i = 15; i <= 30; i+=5) {
				showRotationAgility(i);
			}
			if (dummyParent() != null) return;
		}
		
		
		String outputFolder = "output\\rotationAgility";
		int agility = 20;
		String[] data = {
//				"dc_jog2_rot_base_15",
				"dc_jog2_rot_base_30",
				"dc_jog2_rot_base_25",
				"dc_jog2_rot_base_20",
				"dc_jog2_rot_base_15",
				"dc_jog2_rot_base_10",
				
//				"dc_jog2_rot_adap_te02_t10c",
//				"dc_jog2_rot_base_10",
//				"dc_jog2_rot_margin_10",
				
//				"dc_jog2_rot_adap_te02_t15",
//				"dc_jog2_rot_base_15",
				
//				"dc_jog2_rot_new_adaptive",
				
//				"dc_jog2_dir_r1000_30",
//				"dc_jog2_dir_r1000_25_ad4",
//				"dc_jog2_dir_r1000_30_ad2",
//				"dc_jog2_dir_mm4_r10000_25",
//				"dc_jog2_dir_mm2_r50_25",
//				"dc_jog2_dir_mm2_r50_25_ad",
//				"dc_jog2_dir_mm3_r50_25",
//				"dc_jog2_dir_mm4_r50_25",
//				"dc_jog2_dir_mm3_r50_25_ad",
		};
		String[] labels = {
				"t_c=30",
				"t_c=25",
				"t_c=20",
				"t_c=15",
				"t_c=10",
		};
//		WeightedKDE.USE_POS_PROBABILITY = true;
		WeightedKDE.USE_PROBABILITY = true;
//		WeightedKDE.SIGMA_WEIGHT = 1.5;
		KDEGraphViewer viewer = showData("agility", readData(data, labels, outputFolder, ""));
//		viewer.weightStat = new double[] { 0, 0, 0, 40 };
		if (!WeightedKDE.USE_PROBABILITY) {
//			viewer.yGuideList.add(15d);
//			for (int i = 15; i <= 30; i+=5) {
//				viewer.yGuideList.add((double)i);
//			}
		} else {
			viewer.positionStat = new double[] { 0, 0, 0, 35 };
//			viewer.xGuideList.add(10d);
		}
		viewer.drawSamplePoints = false;
		viewer.topRightLabelAlign = -1;
//		viewer.drawSamplePoints = true;
//		showData("plausibility", readData(labels, outputFolder, "_plausibility"));
		
//		WeightedKDE.USE_PROBABILITY = true;
//		for (int type : typeList) {
//			showDynamicByAgility(type);
//		}
//		for (int agility : agilityList) {
//			showDynamicByType(agility);
//		}
	}
	
	void showOmmAgility() {
		String outputFolder = "output\\ommAgility";
		int[] weights = {
				5,
				10,
				50,
				100,
				500,
//				1000,
//				5000,
		};
		
		String[] data = new String[weights.length];
		String[] labels = new String[weights.length];
		
		for (int idx = 0; idx < data.length; idx++) {
			int i = data.length - 1 - idx;
			i = idx;
			data[idx] = "omm_nopti_jog_w" + weights[i];
//			data[i] = "omm_jog_w" + weights[i];
			labels[idx] = "w=" + weights[i];
		}
		KDEGraphViewer viewer = showData("omm", readData(data, labels, outputFolder, ""));
		viewer.positionStat = new double[] { 0, 0, -3.14, 3.14 };
		viewer.weightStat = new double[] { 0, 0, 0, 40 };
//		viewer.weightStat = new double[] { 0, 0, 0, 60 };
//		if (!WeightedKDE.USE_PROBABILITY) {
//			viewer.yGuideList.add((double)agility);
//		}
		viewer.drawSamplePoints = false;
		viewer.topRightLabelAlign = -1;
	}
	
	void showRotationAgility(int agility) {
		String outputFolder = "output\\rotationAgility";
		String[] data = {
				"dc_jog2_rot_base_" + agility,
		};
		double ratio = agility/25d;
		String rStr = String.format("%.2f", ratio);
		String[] labels = {
				"Agility=" + rStr,
		};
//		WeightedKDE.USE_POS_PROBABILITY = true;
//		WeightedKDE.USE_PROBABILITY = true;
//		WeightedKDE.SIGMA_WEIGHT = 1.5;
		KDEGraphViewer viewer = showData("agility_" + ratio, readData(data, labels, outputFolder, ""));
		viewer.showLegend = false;
		viewer.positionStat = new double[] { 0, 0, -3.14, 3.14 };
		viewer.weightStat = new double[] { 0, 0, 0, 40 };
		if (!WeightedKDE.USE_PROBABILITY) {
			viewer.yGuideList.add((double)agility);
		}
		viewer.drawSamplePoints = true;
	}
	
	KDEGraphViewer showControlParameterDistribution(String name, int start, int end, int interval) {
		String outputFolder = "output\\kde_log\\" + name;
		ArrayList<String> labels = new ArrayList<String>();
		for (int i = start; i <= end; i+= interval) {
			labels.add("iter_" + i);
		}
		KDEGraphViewer viewer = showData("agility", readData(Utils.toArray(labels), outputFolder, ""));
		viewer.drawSamplePoints = false;
		return viewer;
	}
	
	void showDynamicByAgility(int type) {
		String outputFolder = "output\\rotDynamicAglity";
		ArrayList<String> labels = new ArrayList<String>();
		for (int agility : agilityList) {
			labels.add("runjogwalk_withstop_dy_r1000_30_t" + type + "_ag" + agility);
		}
		KDEGraphViewer viewer = showData("ByAgility:type" + type, readData(Utils.toArray(labels), outputFolder, ""));
		if (!WeightedKDE.USE_PROBABILITY) {
			for (int agility : agilityList) {
				viewer.yGuideList.add((double)agility);
			}
		}
	}
	
	void showDynamicByType(int agility) {
		String outputFolder = "output\\rotDynamicAglity";
		ArrayList<String> labels = new ArrayList<String>();
		for (int type : typeList) {
			labels.add("runjogwalk_withstop_dy_r1000_30_t" + type + "_ag" + agility);
		}
		showData("ByType:agility" + agility, readData(Utils.toArray(labels), outputFolder, ""));
	}
	
	private KDEGraphViewer showData(String label, KDEData data) {
		KDEGraphViewer viewer = app().addWindow(new KDEGraphViewer(dummyParent()), label, WindowPosition.Main);
		viewer.setKde(Utils.toArray(data.labels), Utils.toArray(data.kdeList));
		if (!WeightedKDE.USE_PROBABILITY) {
			viewer.drawSamplePoints = true;
		}
		return viewer;
	}
	
	private KDEData readData(String[] data, String outputFolder, String postfix) {
		return readData(data, data, outputFolder, postfix);
	}
	
	private KDEData readData(String[] data, String[] labels, String outputFolder, String postfix) {
		KDEData kde = new KDEData();
		for (int i = 0; i < data.length; i++) {
			String name = data[i];
			String file = outputFolder + "\\" + name + postfix + ".txt";
			kde.labels.add(labels[i]);
//			kde.labels.add(new File(file).getName());
			WeightedKDE kdeData = WeightedKDE.loadFromFile(file);
			if (maxSampleSize > 0 && kdeData.getSampleList().size() > maxSampleSize) {
				while (kdeData.getSampleList().size() > maxSampleSize) {
					kdeData.getSampleList().removeFirst();
				}
			}
			kdeData.updateDataByType();
			kde.kdeList.add(kdeData);
		}
		return kde;
	}
	
	private static class KDEData{
		ArrayList<String> labels = new ArrayList<String>();
		ArrayList<WeightedKDE> kdeList = new ArrayList<WeightedKDE>();
	}
	
	static void trimData(String file) {
		TextUtil.openReader(file);
		ArrayList<String> lines = new ArrayList<String>();
		String line = null;
		while ((line = TextUtil.readLine()) != null) {
			String[] tokens = line.split("\t");
			tokens = Utils.cut(tokens, tokens.length-2, tokens.length-1);
			lines.add(tokens[0] + "\t" + tokens[1]);
		}
		TextUtil.closeReader();
		
		TextUtil.openWriter(file);
		for (String l : lines) {
			TextUtil.writeLine(l);
		}
		TextUtil.closeWriter();
	}

	public static void main(String[] args) {
//		String folder = "output\\rotDynamicAglity";
//		for (File file : new File(folder).listFiles()) {
//			trimData(folder + "\\" + file.getName());
//		}
//		System.exit(0);
		
		MainApplication.run(new KDEGraphViewerModule());
	}
}
