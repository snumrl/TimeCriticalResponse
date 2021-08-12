package mrl.motion.neural.agility;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.match.MMatching;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.util.Configuration;

public class TrainingStuntLocoGenerator2 {
	
	public static void main(String[] args) {
		String folder;
		AgilityModel model;
		int dataGenSize;
		
		dataGenSize = 400000;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		
		String tPoseFile = "t_pose_sue.bvh";
		folder = "stunt_loco";
		
		AgilityModel.GOAL_TIME_LIMIT = 20; 
		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
		MMatching.TIMING_EARLY_FINISH_WEIGHT = 0.5;
		StuntLocoModel.LOCOMOTION_RATIO = 0.85;
		StuntLocoModel.USE_STRAIGHT_SAMPLING = true;
		RotationModel.ROTAION_ANGLE_MARGIN = Math.toRadians(0);
		TransitionData.STRAIGHT_MARGIN = 1;
		MotionDataConverter.useMatrixForAll = true;
		model = new StuntLocoModel2();
		model = new DynamicAgilityModel(model, 15, 55);
		
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		MotionMatchingSampling3 sampling = new MotionMatchingSampling3(database, model);
//		GoalBasedSampling sampling = new GoalBasedSampling(graph, model);
		ArrayList<Motion> mList = sampling.sample(dataGenSize);
		String label = folder + "_ue_CTime_ef" + MMatching.TIMING_EARLY_FINISH_WEIGHT;
		RNNDataGenerator.prepareTrainingFolder(label);
		RNNDataGenerator.generate(mList, new AgilityControlParameterGenerator(model, sampling.controlParameters));
	}
}
