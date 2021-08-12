package mrl.motion.critical.run;

import mrl.motion.neural.gmm.GMMConfig;
import mrl.motion.neural.rl.PolicyLearning;

public class LearningTeacherPolicy {

	public static GMMConfig martial_arts() {
		String name = "martial_arts";
		return new MartialArtsConfig(name).setDataFolder("martial_arts_compact", "data\\t_pose_ue2.bvh");
	}
	
	public static void main(String[] args) {
		GMMConfig config = martial_arts();
		PolicyLearning learning = new PolicyLearning(config, false);
		learning.runTraining(100000);
	}
}
