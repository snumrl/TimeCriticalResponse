package mrl.motion.critical.run;

import mrl.motion.neural.agility.match.TransitionDPGenerator;

public class DP_Preprocessing {

	public static void main(String[] args) {
		String[] actions = MartialArtsConfig.actionTypes;
		// locomotion action size(cyclic actions)
		int cLabelSize = MartialArtsConfig.LOCO_ACTION_SIZE;
		
		String dataFolder = "martial_arts_compact";
		String tPoseFile = "data\\t_pose_ue2.bvh";
//		TransitionDPGenerator.printDistribution = false;
		TransitionDPGenerator.make(dataFolder, tPoseFile, actions, cLabelSize);
	}
}
