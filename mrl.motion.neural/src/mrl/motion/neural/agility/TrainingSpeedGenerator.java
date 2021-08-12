package mrl.motion.neural.agility;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.parser.BVHWriter;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraphExplorer;
import mrl.motion.graph.MGraphGenerator;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.data.DirectionControl;
import mrl.motion.neural.data.DirectionJumpControl;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class TrainingSpeedGenerator {
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
		
		folder = "runjogwalk";
		RotationMotionMatching.STRAIGHT_MARGIN = 1;
		AgilityModel.TIME_EXTENSION_RATIO = 0.1;
		RotationModel.GOAL_TIME_LIMIT = 30; 
		RotationMotionMatching.rotErrorRatio = 100;
		RotationModel.USE_STRAIGHT_SAMPLING = true;
		MotionMatching.preCalculateDistCache = true;
		model = new RotationSpeedModel();
		
		MDatabase database = TrainingDataGenerator.loadDatabase(folder);
		MotionMatchingSampling sampling = new MotionMatchingSampling(database, model);
//		GoalBasedSampling sampling = new GoalBasedSampling(graph, model);
		ArrayList<Motion> mList = sampling.sample(showMotion ? 3000 : dataGenSize);
		if (writeAsBVH) {
			ArrayList<Motion> motionList = mList;
			motionList.get(0).knot = Double.NaN;
			BVHWriter bw = new BVHWriter(new File(Configuration.BASE_MOTION_FILE));
			bw.write(new File("augmented.bvh"), new MotionData(motionList));
		} else if (showMotion){
			// show generated motion
			MotionData mData = new MotionData(mList);
			MainViewerModule.run(mData);
		} else {
//			int interval = 20;
//			String label = folder + "_dir_gbc_s_te_tw90_fixed_" + interval;
//			RNNDataGenerator.prepareTrainingFolder(label);
//			RNNDataGenerator.generate(segment, new DirectionControl(interval));
			
			String label = folder + "_r" + RotationMotionMatching.rotErrorRatio + "_" + RotationModel.GOAL_TIME_LIMIT;
			RNNDataGenerator.prepareTrainingFolder(label);
			RNNDataGenerator.generate(mList, new AgilityControlParameterGenerator(model, sampling.controlParameters));
		}
	}
}
