package mrl.motion.neural.rl;

import mrl.motion.neural.gmm.GMMConfig;
import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;
import mrl.motion.neural.rl.PolicyLearning.RL_State;

public class PolicyMatching implements MFeatureSelector {
	
	public PolicyLearning learning;
	
	public PolicyMatching(GMMConfig config) {
		PolicyLearning.BATCH_SIZE = 1;
		learning = new PolicyLearning(config, true);
//		learning.python.getMeanAction(state);
		
	}

	@Override
	public MotionQuery getControl(MotionFeature currentState, double[] inputParameter) {
//		System.out.println("paramter :: " + Arrays.toString(inputParameter));
		RL_State state = learning.getState(currentState, inputParameter, 32);
//		System.out.println("state :: " + Arrays.toString(currentState.data));
		MotionQuery action = learning.python.getMeanAction(state);
//		System.out.println("action :: " + Arrays.toString(action.feature.data));
		return action;
	}


}
