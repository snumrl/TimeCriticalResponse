package mrl.motion.neural.agility.measure;

import java.io.File;
import java.util.ArrayList;

import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.adaptive.AdaptiveTrainer;
import mrl.motion.neural.agility.adaptive.AdaptiveTrainer4;
import mrl.motion.neural.agility.measure.WeightedKDE.WeightedKDESample;
import mrl.util.Logger;
import mrl.util.TextUtil;
import mrl.util.Utils;
import mrl.widget.app.MainApplication;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.app.Module;

public class KDEAdaptiveLearningViewerModule extends Module{

	@Override
	protected void initializeImpl() {
		String outputFolder = "output\\kde_log2\\";
		String name = "dc_jog_ue_rot_test_rMargin10_15_2_adap";
//		String name = "dc_jog_ue_rot_test_rMargin10_15_2_adap";
		
		int samplingSize = 1000;
		int timeLimit = 15;
		WeightedKDE finalKDE = null;
		ArrayList<WeightedKDE> kdeList = new ArrayList<WeightedKDE>();
		for (int iter = 0; iter < 20; iter++) {
			String file = outputFolder + name + "\\iter_" + iter + ".txt";
			if (new File(file).exists() == false) break;
//			WeightedKDE.NO_USE_NORMALIZE = false;
			WeightedKDE kde = WeightedKDE.loadFromFile(file);
			ArrayList<WeightedKDESample> remain = new ArrayList<WeightedKDESample>();
			for (WeightedKDESample sample : kde.getSampleList()) {
				if (sample.weight > timeLimit) {
					remain.add(sample);
				}
			}
			if (remain.size() == 0) {
				finalKDE = kde;
				break;
			}
			System.out.println("fail ratio : " + iter + " : " + + remain.size() + " / " +  kde.getSampleList().size());
			kde.sigmaSizeOffset = kde.getSampleList().size() - remain.size();
			kde.getSampleList().clear();
			kde.getSampleList().addAll(remain);
			WeightedKDE.USE_POS_PROBABILITY = true;
//			WeightedKDE.NO_USE_NORMALIZE = true;
			kde.updateDataByType();
			kdeList.add(kde);
			
			KDEGraphViewer2 viewer = showData("iter" + iter, kdeList);
			viewer.drawSamplePoints = false;
			viewer.weightStat = new double[] { 0, 0, 0, 4 };
		}
//		WeightedKDE.USE_POS_PROBABILITY = false;
//		showFinalViewer("final", finalKDE, kdeList);
	}
	
	private KDEGraphViewer showFinalViewer(String label, WeightedKDE finalKDE, ArrayList<WeightedKDE> kdeList) {
		KDEGraphViewer viewer = new KDEGraphViewer(dummyParent()) {
			public double calcLimitOffset(double[] pos) {
				double weight = 0;
				for (int kIdx = 0; kIdx < kdeList.size(); kIdx++) {
					double p = kdeList.get(kIdx).getEstimatedWeight(pos);
					if (Double.isNaN(p)) {
						System.out.println("nan : " + kIdx + " : " + pos[0]);
						continue;
					}
					p = AdaptiveTrainer4.adjustPDF(p);
					weight += p;
				}
				System.out.println("pos : " + pos[0] +" : " + weight);
				return -weight;
			}
		};
		viewer = app().addWindow(viewer, label, WindowPosition.Main);
		viewer.setKde(new String[] { label}, new WeightedKDE[] { finalKDE });
		viewer.drawSamplePoints = true;
		viewer.weightStat = new double[] {0, 0, 0, 30};
		viewer.yGuideList.add(15d);
		return viewer;
	}
	
	private KDEGraphViewer2 showData(String label, ArrayList<WeightedKDE> kdeList) {
		KDEGraphViewer2 viewer = app().addWindow(new KDEGraphViewer2(dummyParent()), label, WindowPosition.Main);
		viewer.setKde(label, Utils.toArray(kdeList));
		return viewer;
	}
	
	

	public static void main(String[] args) {
//		String folder = "output\\rotDynamicAglity";
//		for (File file : new File(folder).listFiles()) {
//			trimData(folder + "\\" + file.getName());
//		}
//		System.exit(0);
		
		MainApplication.run(new KDEAdaptiveLearningViewerModule());
	}
}
