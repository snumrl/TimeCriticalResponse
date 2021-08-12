package mrl.motion.neural.agility.measure;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.plaf.TextUI;

import org.eclipse.swt.SWT;

import mrl.motion.neural.agility.measure.WeightedKDE.WeightedKDESample;
import mrl.util.FileUtil;
import mrl.util.TextUtil;
import mrl.util.Utils;
import mrl.widget.app.MainApplication;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.app.Module;

public class KDEAdaptiveErrorViewerModule extends Module{

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
		String outputFolder = "output\\rotationAgility";
		int agility = 20;
		String[] data = {
				"dc_jog_ue_rot_test_rMargin20_14",
//				"dc_jog_ue_rot_test_rMargin10_15",
		};
		String[] labels = {
				"error",
		};
		WeightedKDE.USE_POS_PROBABILITY = true;
//		WeightedKDE.USE_PROBABILITY = true;
		KDEGraphViewer viewer = showData("agility", readData(data, labels, outputFolder, "", 15d));
//		viewer.weightStat = new double[] { 0, 0, 0, 40 };
		viewer.positionStat = new double[] { 0, 0, -3.14, 3.14 };
		viewer.weightStat = new double[] { 0, 0, 0, 1 };
		if (!WeightedKDE.USE_PROBABILITY) {
//			viewer.yGuideList.add(15d);
//			for (int i = 15; i <= 30; i+=5) {
//				viewer.yGuideList.add((double)i);
//			}
		} else {
			viewer.positionStat = new double[] { 0, 0, 0, 35 };
//			viewer.xGuideList.add(10d);
		}
//		viewer.drawSamplePoints = false;
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
	
	private KDEGraphViewer showData(String label, KDEData data) {
		KDEGraphViewer viewer = app().addWindow(new KDEGraphViewer(dummyParent()), label, WindowPosition.Main);
		viewer.setKde(Utils.toArray(data.labels), Utils.toArray(data.kdeList));
		if (!WeightedKDE.USE_PROBABILITY) {
			viewer.drawSamplePoints = true;
		}
		return viewer;
	}
	
	private KDEData readData(String[] data, String[] labels, String outputFolder, String postfix, double wLimit) {
		KDEData kde = new KDEData();
		for (int i = 0; i < data.length; i++) {
			String name = data[i];
			String file = outputFolder + "\\" + name + postfix + ".txt";
			kde.labels.add(labels[i]);
//			kde.labels.add(new File(file).getName());
			WeightedKDE kdeData = WeightedKDE.loadFromFile(file);
			
//			for (WeightedKDESample sample : kdeData.getSampleList()) {
//				if (sample.weight < wLimit) {
//					sample.weight = wLimit;
//				}
//			}
			
//			kdeData.updateSigma();
//			System.exit(0);
			
			ArrayList<WeightedKDESample> remain = new ArrayList<WeightedKDESample>();
			for (WeightedKDESample sample : kdeData.getSampleList()) {
				if (sample.weight > wLimit) {
					remain.add(sample);
				}
			}
			kdeData.sigmaSizeOffset = kdeData.getSampleList().size() - remain.size();
			kdeData.getSampleList().clear();
			kdeData.getSampleList().addAll(remain);
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
		
		MainApplication.run(new KDEAdaptiveErrorViewerModule());
	}
}
