package mrl.motion.neural.agility;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import mrl.motion.data.MDatabase;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.data.DataExtractor;
import mrl.motion.neural.data.DirectionJumpControl;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.util.Configuration;
import mrl.util.MathUtil;

public class TrainingJumpGenerator2 {
	static boolean showMotion = false;
	static boolean makeGraph = false;
	static boolean writeAsBVH = false;
	
	public static void main(String[] args) {

		
		String folder;
		AgilityModel model;
		int dataGenSize;
		
		makeGraph = true;
//		showMotion = true;
//		writeAsBVH = true;
		
		dataGenSize = 200000;
		MDatabase.loadEventAnnotations = true;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		
		
//		AgilityModel.TIME_EXTENSION_RATIO = 0.1;
//		AgilityModel.GOAL_TIME_LIMIT = 20;
//		RotationMotionMatching.STRAIGHT_MARGIN = 3;
//		JumpModel.rotErrorRatio = 200;
//		JumpModel.USE_STRAIGHT_SAMPLING = true;
//		JumpModel.FIX_LOCOMOTION_AFTER_ACTION = false;
//		JumpModel.LOCOMOTION_RATIO = 0.75;
//		folder = "dc_jump_jog";
		
		folder = "dc_jog2";
		RotationModel.GOAL_TIME_LIMIT = 25; 
		RotationMotionMatching.rotErrorRatio = 100;
		RotationModel.USE_STRAIGHT_SAMPLING = true;
		model = new RotationModel();
		
		MathUtil.random = new Random(42424);
		MDatabase database = TrainingDataGenerator.loadDatabase(folder);
		MotionMatchingSampling2 sampling = new MotionMatchingSampling2(database, model);
		ArrayList<double[]> xList = new ArrayList<double[]>();
		ArrayList<double[]> yList = new ArrayList<double[]>();
		for (int i = 0; i < 100; i++) {
			boolean add = sampling.sampleSingle(model.sampleRandomGoal(Pose2d.getPose(sampling.currentMotion())));
			if (add) {
				xList.addAll(sampling.generatedXList);
				yList.addAll(sampling.generatedYList);
			}
		}
		
		DirectionJumpControl.USE_POSITION_CONTROL = true;
		DirectionJumpControl.USE_ACTIVATION = true;
		String acti = DirectionJumpControl.USE_ACTIVATION ? "acti" : "nActi";
		String label = folder + "_" + acti + "_test_r" + JumpModel.rotErrorRatio + "_" + RotationModel.GOAL_TIME_LIMIT;
		RNNDataGenerator.prepareTrainingFolder(label);
		new File(RNNDataGenerator.OUTPUT_PATH).mkdirs();
		String path = RNNDataGenerator.OUTPUT_PATH + "\\";
		DataExtractor.writeNormalizeInfo(path + "xNormal.dat", xList, sampling.pg.getNormalMarking());
		DataExtractor.writeDataWithNormalize(path + "xData.dat", xList, path + "xNormal.dat");
		DataExtractor.writeNormalizeInfo(path + "yNormal.dat", yList, MotionDataConverter.getNormalMarking());
		DataExtractor.writeDataWithNormalize(path + "yData.dat", yList, path + "yNormal.dat");
	}
}
