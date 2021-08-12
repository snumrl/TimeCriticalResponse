package mrl.motion.neural.gmm;

import java.util.ArrayList;

import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.util.MathUtil;

public class AABB {

	public ArrayList<MotionFeature> featureList = new ArrayList<MotionFeature>();
	
	private double[] min; 
	private double[] max; 
	
	public AABB() {
	}
	
	public void add(MotionFeature feature) {
		if (min == null) {
			min = MathUtil.copy(feature.data);
			max = MathUtil.copy(feature.data);
		} else {
			for (int i = 0; i < min.length; i++) {
				min[i] = Math.min(min[i], feature.data[i]);
				max[i] = Math.max(max[i], feature.data[i]);
			}
		}
		featureList.add(feature);
	}
	
	public double distance(MotionFeature feature, double minDist) {
		double[] data = feature.data;
		double dSum = 0;
		for (int i = 0; i < data.length; i++) {
			double d;
			if (data[i] < min[i]) {
				d = min[i] - data[i];
			} else if (data[i] > max[i]) {
				d = data[i] - max[i];
			} else {
				continue;
			}
//			double dMin = Math.abs(data[i] - min[i]);
//			double dMax = Math.abs(data[i] - max[i]);
//			double d = Math.min(dMin, dMax);
			
			dSum += d*d;
			if (dSum > minDist) return -1;
		}
		return dSum;
	}
	
}
