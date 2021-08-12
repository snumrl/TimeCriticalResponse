package mrl.motion.neural.agility;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.util.Configuration;

public class TrainingJumpGenerator {
	
	public static void main(String[] args) {
		String folder;
		AgilityModel model;
		int dataGenSize;
		
		dataGenSize = 200000;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		
		
//		folder= "dc_jump_jog";
//		String tPoseFile = "t_pose_actor.bvh";
		
		folder = "jump_jog_ret_ue4";
		String tPoseFile = "t_pose_jue.bvh";
		
		AgilityModel.GOAL_TIME_LIMIT = 20; 
		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
		JumpModel.USE_STRAIGHT_SAMPLING = true;
		JumpModel.FIX_LOCOMOTION_AFTER_ACTION = false;
		TransitionData.STRAIGHT_MARGIN = 3;
		model = new JumpModel();
		model = new DynamicAgilityModel(model, 15, 35);
		MotionDataConverter.useMatrixForAll = true;
		
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		MotionMatchingSampling3 sampling = new MotionMatchingSampling3(database, model);
//		GoalBasedSampling sampling = new GoalBasedSampling(graph, model);
		ArrayList<Motion> mList = sampling.sample(dataGenSize);
			
		String label = folder + "_new_dy_ag";
//		String label = folder + "_new_" + RotationModel.GOAL_TIME_LIMIT;
		RNNDataGenerator.prepareTrainingFolder(label);
		RNNDataGenerator.generate(mList, new AgilityControlParameterGenerator(model, sampling.controlParameters));
	}
}
