package mrl.motion.neural.rl;

import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;

public interface MFeatureSelector {

	public MotionQuery getControl(MotionFeature currentState, double[] inputParameter);
}
