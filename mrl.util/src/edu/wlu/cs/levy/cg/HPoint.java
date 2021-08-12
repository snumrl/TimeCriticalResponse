// Hyper-Point class supporting KDTree class

package edu.wlu.cs.levy.cg;

class HPoint {

	protected double[] coord;

	protected HPoint(int n) {
		coord = new double[n];
	}

	protected HPoint(double[] x) {

		coord = new double[x.length];
		for (int i = 0; i < x.length; ++i)
			coord[i] = x[i];
	}

	protected Object clone() {

		return new HPoint(coord);
	}

	protected boolean equals(HPoint p) {

		// seems faster than java.util.Arrays.equals(), which is not
		// currently supported by Matlab anyway
		for (int i = 0; i < coord.length; ++i)
			if (coord[i] != p.coord[i])
				return false;

		return true;
	}

	public String toString() {
		String s = "";
		for (int i = 0; i < coord.length; ++i) {
			s = s + coord[i] + " ";
		}
		return s;
	}

}
