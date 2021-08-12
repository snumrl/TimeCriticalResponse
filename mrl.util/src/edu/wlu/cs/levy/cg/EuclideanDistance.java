// Hamming distance metric class

package edu.wlu.cs.levy.cg;

public class EuclideanDistance implements DistanceMetric {

	public double distance(double[] a, double[] b) {
		return Math.sqrt(squaredDistance(a, b));
	}

	protected static double squaredDistance(double[] a, double[] b) {
		double dist = 0;
		for (int i = 0; i < a.length; ++i) {
			double diff = (a[i] - b[i]);
			dist += diff * diff;
		}
		return dist;
	}
}
