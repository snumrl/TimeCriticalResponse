package mrl.motion.neural.agility;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.util.Configuration;

public class TrainingStuntGenerator {
	
	public static void main(String[] args) {
		String folder;
		AgilityModel model;
		int dataGenSize;
		
		dataGenSize = 400000;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
//		folder= "dc_stunt";
//		String tPoseFile = "t_pose_actor.bvh";
		folder = "stunt_new_label";
		String tPoseFile = "t_pose_sue.bvh";
		MotionDataConverter.useMatrixForAll = true;
		RotationModel.GOAL_TIME_LIMIT = 25; 
		model = new StuntModel(folder);
		model = new DynamicAgilityModel(model, 15, 40);
		
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		MotionMatchingSampling3 sampling = new MotionMatchingSampling3(database, model);
//		GoalBasedSampling sampling = new GoalBasedSampling(graph, model);
		ArrayList<Motion> mList = sampling.sample(dataGenSize);
		String label = folder + "_ue_ma_dy_ag_15to40";
		RNNDataGenerator.prepareTrainingFolder(label);
		RNNDataGenerator.generate(mList, new AgilityControlParameterGenerator(model, sampling.controlParameters));
	}
}
