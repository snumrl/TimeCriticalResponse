package mrl.motion.neural.agility;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.util.Configuration;

public class TrainingDribbleGenerator {

	public static void main(String[] args) {

		String folder;
		String tPoseFile;
		AgilityModel model;
		int dataGenSize;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;

		dataGenSize = 300000;
		
		
		tPoseFile = "t_pose_bue.bvh";
		folder = "basketTest";
		MotionDistByPoints.USE_BALL_CONTACT = true;
		TransitionData.STRAIGHT_MARGIN = 1;
		TransitionData.STRAIGHT_LIMIT_PRESET = new double[] {
				10000, 20, 30
		};
		DribbleModel.GOAL_TIME_LIMIT = 25; 
		DribbleModel.USE_STRAIGHT_SAMPLING = true;
		AgilityModel.TIME_EXTENSION_RATIO = 0.33;
		int rMargin = 20;
		DribbleModel.ROTATION_ERROR_WEIGHT = 500;
		DribbleModel.ROTAION_ANGLE_MARGIN = Math.toRadians(rMargin);
		model = new DribbleModel();
		model = new DynamicAgilityModel(model, 15, 35);
		
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		MotionMatchingSampling3 sampling = new MotionMatchingSampling3(database, model);
		ArrayList<Motion> mList = sampling.sample(dataGenSize);
		
		String label = folder + "_dirAction_2_dy";
		RNNDataGenerator.prepareTrainingFolder(label);
		RNNDataGenerator.generate(mList, new AgilityControlParameterGenerator(model, sampling.controlParameters));
	}
}
