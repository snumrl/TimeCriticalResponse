package mrl.motion.dp;

import mrl.graph.SparseMatrix;

public class MotionDistanceMatrix extends SparseMatrix<Double> {
	
	private double maxDist;
	
	public MotionDistanceMatrix(int size, double maxDist) {
		super(size);
		this.maxDist = maxDist;
	}
	
	public Double put(int x, int y, Double e){
		if (e >= maxDist) return null;
		return super.put(x, y, e);
	}
	
	public Double get(int x, int y){
		Double v = super.get(x, y);
		if (v == null) return maxDist;
		return v;
	}
	
}
