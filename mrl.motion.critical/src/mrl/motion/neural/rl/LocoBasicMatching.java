package mrl.motion.neural.rl;

import java.util.ArrayList;

import javax.vecmath.Vector2d;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class LocoBasicMatching implements MFeatureSelector {

	@Override
	public MotionQuery getControl(MotionFeature currentState, double[] inputParameter) {
		int[] fIndices = MFeatureMatching.futureTrajectoryIndices;
		
		ArrayList<double[]> features = new ArrayList<double[]>();
		double[] prefix = Utils.cut(currentState.data, 0, currentState.data.length - fIndices.length*4 - 1);
		features.add(prefix);
		
		double fMax = fIndices[fIndices.length - 1];
		double targetAngle = inputParameter[0];
		for (int i = 0; i < fIndices.length; i++) {
			double r = fIndices[i]/fMax;
			double angle = targetAngle*r;
			Vector2d direction = MathUtil.rotate(Pose2d.BASE.direction, angle);
			features.add(new Pose2d(Pose2d.BASE.position, direction).toArray());
		}
		return null;
//		MotionQuery mf = new MotionQuery(currentState, MathUtil.concatenate(Utils.toArray(features)));
//		for (int i = 0; i < fIndices.length; i++) {
//			int idx = currentState.data.length - fIndices.length*4 + i*4;
//			mf.weights[idx] = 0;
//			mf.weights[idx+1] = 0;
//			double w = 30;
//			mf.weights[idx+2] = w;
//			mf.weights[idx+3] = w;
//		}
//		return mf;
	}


}
