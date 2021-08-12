package mrl.motion.neural.rl;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MDatabase.FrameType;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.TrainingDataGenerator;

public class PolicyTest {

	public static void main(String[] args) {
		String dataFolder = "ue_loco_only";
		String tPoseFile = "t_pose_ue2.bvh";
		MDatabase database = TrainingDataGenerator.loadDatabase(dataFolder, tPoseFile);
		FrameType[] types = database.getTypeList();
		for (int i = 0; i < types.length; i++) {
			types[i] = FrameType.Event;
		}
		LocoBasicMatching selector = new LocoBasicMatching();
		PolicyEvaluation eval = new PolicyEvaluation(new TransitionData(database, new String[0], 0), selector);
		double error = eval.test();
		System.out.println("Error : " + Math.toDegrees(Math.sqrt(error)));
		
	}
}
