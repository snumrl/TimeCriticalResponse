package mrl.motion.neural.agility;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.util.Configuration;

public class TrainingPositionGenerator {

	public static void main(String[] args) {

		String folder;
		AgilityModel model;
		int dataGenSize;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;

		String tPoseFile = "t_pose_actor.bvh";
		dataGenSize = 200000;
		folder = "basketTest";
		PositionModel.actionTypes = new String[] {
				"dribble",
		};
		MotionDistByPoints.USE_BALL_CONTACT = true;
		AgilityModel.GOAL_TIME_LIMIT = 20; 
		AgilityModel.TIME_EXTENSION_RATIO = 0.3;
		TransitionData.STRAIGHT_MARGIN = -1;
		PositionModel.BASE_VELOCITY_LENGTH = 200;
		model = new PositionModel();
		model = new DynamicAgilityModel(model, 15, 35);
		
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		MotionMatchingSampling3 sampling = new MotionMatchingSampling3(database, model);
		ArrayList<Motion> mList = sampling.sample(dataGenSize);
		
		String label = folder + "_pos_" + RotationModel.GOAL_TIME_LIMIT;
		RNNDataGenerator.prepareTrainingFolder(label);
		RNNDataGenerator.generate(mList, new AgilityControlParameterGenerator(model, sampling.controlParameters));
	}
}
