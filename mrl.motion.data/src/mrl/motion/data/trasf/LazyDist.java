package mrl.motion.data.trasf;

import java.util.HashMap;

import javax.vecmath.Point2i;

public class LazyDist {

	private MotionDistByPoints dist;
	private int seqMargin;
	private HashMap<Point2i, Double> distMap = new HashMap<Point2i, Double>();
	private HashMap<Point2i, Double> seqDistMap = new HashMap<Point2i, Double>();

	public LazyDist(MotionDistByPoints dist, int seqMargin) {
		this.dist = dist;
		this.seqMargin = seqMargin;
	}
	
	public double getDist(int i, int j){
		Point2i key = new Point2i(i, j);
		Double d = distMap.get(key);
		if (d == null){
			d = dist.getDistance(i, j); 
			distMap.put(key, d);
		}
		return d;
	}
	public double getSeqDist(int i, int j){
		Point2i key = new Point2i(i, j);
		Double d = seqDistMap.get(key);
		if (d == null){
			d = calcSeqDist(i, j, seqMargin, 0.125);
			seqDistMap.put(key, d);
		}
		return d;
	}
	
	private double calcSeqDist(int x, int y, int margin, double damp){
//		if (x == y) return 0;
		double dSum = getDist(x, y);
		double weight = 1;
		
		int p1 = x;
		int p2 = y;
		for (int i = 1; i <= margin; i++) {
			if (p1 > 0) p1--;
			if (p2 > 0) p2--;
			weight += (1-damp*i);
			dSum += getDist(p1,p2)*(1-damp*i);
		}
		
		int n1 = x;
		int n2 = y;
		for (int i = 1; i <= margin; i++) {
			if (n1 < dist.size()-1) n1++;
			if (n2 < dist.size()-1) n2++;
			weight += (1-damp*i);
			dSum += getDist(n1, n2)*(1-damp*i);
		}
		double poseDiff = dSum / weight;
		return poseDiff;
	}
}
