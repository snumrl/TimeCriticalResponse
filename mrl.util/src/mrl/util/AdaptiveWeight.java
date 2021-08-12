package mrl.util;

import java.util.Arrays;

public class AdaptiveWeight {

	public MeanCounter[] counterList;
	public double[] desireWeights;
	public double[] currentWeights;

	public AdaptiveWeight(double[] desireWeights) {
		normalize(desireWeights);
		this.desireWeights = desireWeights;
		counterList = new MeanCounter[desireWeights.length];
		for (int i = 0; i < counterList.length; i++) {
			counterList[i] = new MeanCounter();
		}
		
		currentWeights = new double[desireWeights.length];
		for (int i = 0; i < desireWeights.length; i++) {
			currentWeights[i] = 1/desireWeights[i];
		}
		normalize(currentWeights);
	}
	
	public void resetCounter() {
		updateWeights();
		for (int i = 0; i < counterList.length; i++) {
			counterList[i] = new MeanCounter();
		}
	}
	
	public double getWeight(int index) {
		return currentWeights[index];
	}
	
	public void add(int index, double value) {
		counterList[index].add(value);
	}
	
	static double alpha = 0.1;
	public void updateWeights() {
		double[] vWeights = new double[desireWeights.length];
		for (int i = 0; i < vWeights.length; i++) {
			vWeights[i] = counterList[i].mean();
			if (Double.isNaN(vWeights[i])) return;
		}
		normalize(vWeights);
		
		System.out.println("vWeights : " + Arrays.toString(vWeights) + " : " + Arrays.toString(desireWeights));
		
		for (int i = 0; i < vWeights.length; i++) {
			double adjust = vWeights[i] - desireWeights[i];
			currentWeights[i] += alpha*adjust;
			currentWeights[i] = Math.max(currentWeights[i], 0.01);
		}
		normalize(currentWeights);
		System.out.println("after weights : " + Arrays.toString(currentWeights));
	}
	
	private void normalize(double[] weights) {
		double sum = MathUtil.sum(weights);
		for (int i = 0; i < weights.length; i++) {
			weights[i] /= sum;
		}
	}
}
