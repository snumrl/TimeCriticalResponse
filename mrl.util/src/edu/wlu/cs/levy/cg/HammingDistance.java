// Hamming distance metric class

package edu.wlu.cs.levy.cg;

public class HammingDistance implements DistanceMetric {

	public double distance(double[] a, double[] b) {

		double dist = 0;

		for (int i = 0; i < a.length; ++i) {
			double diff = (a[i] - b[i]);
			dist += Math.abs(diff);
		}

		return dist;
	}
}
