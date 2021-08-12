package mrl.motion.neural.agility;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.util.Configuration;

public class TrainingStuntLocoGenerator {
	
	public static void main(String[] args) {
		String folder;
		AgilityModel model;
		int dataGenSize;
		
		dataGenSize = 300000;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		
		String tPoseFile = "t_pose_sue.bvh";
		folder = "stunt_loco2";
		
		AgilityModel.GOAL_TIME_LIMIT = 20; 
		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
		StuntLocoModel.LOCOMOTION_RATIO = 0.85;
		StuntLocoModel.USE_STRAIGHT_SAMPLING = true;
		RotationModel.ROTAION_ANGLE_MARGIN = Math.toRadians(15);
		TransitionData.STRAIGHT_MARGIN = 1;
		MotionDataConverter.useMatrixForAll = true;
		model = new StuntLocoModel();
		model = new DynamicAgilityModel(model, 10, 25);
		
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		MotionMatchingSampling3 sampling = new MotionMatchingSampling3(database, model);
//		GoalBasedSampling sampling = new GoalBasedSampling(graph, model);
		ArrayList<Motion> mList = sampling.sample(dataGenSize);
		String label = folder + "_ue_dy_ag2_" + dataGenSize;
		RNNDataGenerator.prepareTrainingFolder(label);
		RNNDataGenerator.generate(mList, new AgilityControlParameterGenerator(model, sampling.controlParameters));
	}
}
