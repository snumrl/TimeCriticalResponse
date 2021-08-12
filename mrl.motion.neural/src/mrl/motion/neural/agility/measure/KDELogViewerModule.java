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

public class KDELogViewerModule extends Module{

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
		{
			maxSampleSize = 500;
//			WeightedKDE.USE_POS_PROBABILITY = true;
			KDEGraphViewer viewer = showControlParameterDistribution("dc_jog_ue_rot_test_rMargin10_15_adap3", 500, 9500, 1000);
			viewer.topRightLabelAlign = -1;
//			KDEGraphViewer viewer = showControlParameterDistribution("dc_jog2_rot_adap_te02_t15", 900, 1500, 100);
			viewer.yGuideList.add(15d);
		}
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
		
		MainApplication.run(new KDELogViewerModule());
	}
}
