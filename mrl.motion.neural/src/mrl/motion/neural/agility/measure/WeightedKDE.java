package mrl.motion.neural.agility.measure;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import mrl.util.Gaussian;
import mrl.util.MathUtil;
import mrl.util.TextUtil;
import mrl.util.Utils;

/**
 * Weighted Kernel Density Estimation
 *
 */
public class WeightedKDE {
	
	public static boolean USE_HISTOGRAM = false;
	public static boolean USE_PROBABILITY = false;
	public static boolean USE_POS_PROBABILITY = false;
	public static boolean NO_USE_NORMALIZE = false;
	
	public static double SIGMA_WEIGHT = 1;
	
	private LinkedList<WeightedKDESample> sampleList = new LinkedList<WeightedKDE.WeightedKDESample>();
	private double[] gausianSigma;
	private double[][] posStat;
	private double posDimension = -1;
	
	public int maxSampleSize = 5000;
	public int sigmaUpdateInterval = 200;
	private int addedCount = 0;
	public int sigmaSizeOffset = 0;
	
	public HashMap<Integer, Integer> histogram;

	public WeightedKDE() {
	}
	
	public static WeightedKDE loadFromFile(String file) {
		WeightedKDE kde = new WeightedKDE();
		kde.load(file);
		return kde;
	}
	
	public void updateDataByType() {
		WeightedKDE kde = this;
		if (USE_POS_PROBABILITY) {
			for (WeightedKDESample sample : kde.sampleList) {
				sample.weight = sample.position[0];
			}
			kde.updateSigma();
			for (WeightedKDESample sample : kde.sampleList) {
				sample.weight = kde.getEstimatedWeight(sample.position);
			}
		} else if (USE_HISTOGRAM) {
			histogram = new HashMap<Integer, Integer>();
			for (WeightedKDESample sample : kde.sampleList) {
				sample.position = new double[] { sample.weight };
				int w = (int)sample.weight;
				Integer count = histogram.get(w);
				if (count == null) count = 0;
				histogram.put(w, count + 1);
			}
			kde.updateSigma();
			HashSet<Integer> isProcessed = new HashSet<Integer>();
			LinkedList<WeightedKDESample> filtered = new LinkedList<WeightedKDESample>();
			for (WeightedKDESample sample : kde.sampleList) {
				int w = (int)sample.weight;
				if (isProcessed.contains(w)) continue;
				isProcessed.add(w);
				sample.weight = histogram.get(w);
				filtered.add(sample);
//				sample.weight = kde.getEstimatedWeight(sample.position);
			}
			kde.sampleList = filtered;
			System.out.println("slen : " + kde.sampleList.size());
			kde.updateSigma();
		} else if (USE_PROBABILITY){
			for (WeightedKDESample sample : kde.sampleList) {
				sample.position = new double[] { sample.weight };
			}
			kde.updateSigma();
			for (WeightedKDESample sample : kde.sampleList) {
				sample.weight = kde.getEstimatedWeight(sample.position);
			}
		}
		kde.updateSigma();
	}
	
	public void updateSigma() {
		ArrayList<double[]> posList = new ArrayList<double[]>();
		ArrayList<Double> weightList = new ArrayList<Double>();
		for (WeightedKDESample sample : sampleList) {
			posList.add(sample.position);
			weightList.add(sample.weight);
		}
		
		
		posStat = MathUtil.getStatistics(posList);
		gausianSigma = new double[posStat.length];
		for (int i = 0; i < posStat.length; i++) {
			gausianSigma[i] = SIGMA_WEIGHT*1.06*posStat[i][1]*Math.pow(sampleList.size() + sigmaSizeOffset, -0.2);
			if (Double.isNaN(gausianSigma[i])) {
				double[] pos = posList.get(0);
				gausianSigma[i] = 1;
				posStat[i] = new double[] { pos[i], 1, pos[i], pos[i] };
			}
//			System.out.println("sigma : " + i + " : " + gausianSigma[i] + " : " + posStat[i][1]);
		}
	}
	
	public LinkedList<WeightedKDESample> getSampleList() {
		return sampleList;
	}

	public void addSample(double[] position, double weight) {
		if (posDimension < 0) {
			posDimension = position.length;
		}
		sampleList.add(new WeightedKDESample(position, weight));
		
		if (sampleList.size() > maxSampleSize) {
			for (int i = 0; i < sigmaUpdateInterval; i++) {
				sampleList.removeFirst();
			}
			updateSigma();
		}
		
		addedCount++;
		if (addedCount > sigmaUpdateInterval) {
			addedCount = 0;
			updateSigma();
		}
	}
	
	public double getEstimatedWeight(double[] position) {
		if (gausianSigma == null) {
			updateSigma();
		}
		
		double probSum = 0;
		double weightSum = 0;
		for (WeightedKDESample sample : sampleList) {
			double prob = getPositionPDF(position, sample.position);
			probSum += prob;
			weightSum += prob*sample.weight;
		}
		
		if (USE_PROBABILITY || USE_POS_PROBABILITY) {
			if (NO_USE_NORMALIZE) {
				return Math.min(1, probSum);
			} else {
				return probSum / sampleList.size();
			}
		}
		return weightSum / probSum;
	}
	
	private double getPositionPDF(double[] pivot, double[] sample) {
		double prob = 1;
		for (int i = 0; i < pivot.length; i++) {
			double p = pivot[i] - sample[i];
			double h = gausianSigma[i];
			prob *= Gaussian.pdf(p, 0, h);
		}
		return prob;
	}
	
	public void save(String file) {
		new File(file).getParentFile().mkdirs();
		double[][] values = new double[sampleList.size()][];
		int idx = 0;
		for (WeightedKDESample sample : sampleList) {
			values[idx] = MathUtil.concatenate(sample.position, new double[] { sample.weight });;
			idx++;
		}
		System.out.println("save WeightedKDE : " + file);
		TextUtil.write(file, values);
	}
	
	public void load(String file) {
		double[][] values = TextUtil.readDoubleArray(file);
		for (double[] data : values) {
			sampleList.add(new WeightedKDESample(Utils.cut(data, 0, data.length-2), data[data.length-1]));
		}
	}
	
	public static class WeightedKDESample{
		public double[] position;
		public double weight;
		public WeightedKDESample(double[] position, double weight) {
			this.position = position;
			this.weight = weight;
		}
	}
}
