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
import mrl.motion.dp.ActionDistDP;
import mrl.motion.dp.TransitionData;
import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraphExplorer;
import mrl.motion.graph.MGraphGenerator;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.data.BallTrajectoryGenerator;
import mrl.motion.neural.data.DirectionControl;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class TrainingDataGenerator {
	public static int DEFAULT_BLEND_MARGIN = 4;
	
	public static void init(String folder){
		Configuration.setDataFolder("data\\" + folder);
		Configuration.MGRAPH_NODE_CACHE_FILE = "data\\graphs\\" + folder + "_" + Configuration.MGRAPH_NODE_CACHE_FILE;
		Configuration.MGRAPH_EDGE_CACHE_FILE = "data\\graphs\\" + folder + "_" + Configuration.MGRAPH_EDGE_CACHE_FILE;
	}
	
	public static MDatabase loadDatabase(String folder) {
		return loadDatabase(folder, "t_pose_actor.bvh");
	}
	public static MDatabase loadDatabase(String folder, String tPoseFile) {
		MDatabase.loadEventAnnotations = true;
		Configuration.BLEND_MARGIN = DEFAULT_BLEND_MARGIN;
		Configuration.MOTION_TRANSITON_MARGIN = DEFAULT_BLEND_MARGIN;
//		ActionDistDP.NO_TRANSITION_BEFORE_ACTION_MARGIN = 8;
		MotionDistByPoints.CONTACT_CONFLICT_ERROR = 100;
		MGraphExplorer.TRANSITION_LIMIT = 20;
//		FootContactDetection.heightLimit = new double[]{ 16, 6, 6 };
//		FootContactDetection.velocityLimit = new double[]{2 , 2, 2 };
		FootContactDetection.heightLimit = new double[]{ 15, 11, 9 };
		FootContactDetection.velocityLimit = new double[]{3 , 3, 3 };
		init(folder);
//		MotionDistByPoints.CONTACT_CONFLICT_ERROR = Configuration.MGRAPH_EDGE_WEIGHT_LIMIT/4;
		MDatabase database = MDatabase.load();
		Configuration.BASE_MOTION_FILE = tPoseFile;
		
		MotionDataConverter.setAllJoints();
		if (MotionDistByPoints.USE_BALL_CONTACT) {
			BallTrajectoryGenerator.updateBallContacts(database);
		} else {
			MotionDataConverter.setNoBall();
		}
		MotionDataConverter.setUseOrientation();
		
		
		return database;
	}
	
	public static MGraph loadMotionGraph(String folder) {
		MDatabase database = loadDatabase(folder);
		if (makeGraph){
			// generate new motion graph and save to files.
			MGraphGenerator g = new MGraphGenerator(database);
			g.saveResult();
		}
		// load motion graph from saved files.
		MGraph graph = new MGraph(database);
		return graph;
	}
	
	static boolean showMotion = false;
	static boolean makeGraph = false;
	static boolean writeAsBVH = false;
	
	public static void main(String[] args) {

		
		String folder;
		String tPoseFile;
		AgilityModel model;
		double edgeLimit;
		int dataGenSize;
		
		makeGraph = true;
//		showMotion = true;
//		writeAsBVH = true;
		
		edgeLimit = 300;
		dataGenSize = 200000;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = edgeLimit;
		
		folder = "dc_jog_ue";
		tPoseFile = "t_pose_ue.bvh";
		RotationModel.GOAL_TIME_LIMIT = 20; 
		RotationModel.USE_STRAIGHT_SAMPLING = true;
		AgilityModel.TIME_EXTENSION_RATIO = 0.2;
		TransitionData.STRAIGHT_MARGIN = 3;
		int rMargin = 10;
		RotationModel.ROTAION_ANGLE_MARGIN = Math.toRadians(rMargin);
		RotationModel.ROTATION_ERROR_WEIGHT = 200;
		model = new RotationModel();
//		model = new DynamicAgilityModel(model, 15, 35);
		
//		folder = "poke_bvh_ver2";
//		tPoseFile = "t_pose_poke.bvh";
//		RotationModel.GOAL_TIME_LIMIT = 25; 
//		model = new PokeModel(folder);
//		model = new DynamicAgilityModel(model, 13, 45);
		
//		folder = "dc_stunt";
//		tPoseFile = "t_pose_actor.bvh";
//		model = new StuntModel(folder);
//		model = new DynamicAgilityModel(model, 15, 45);
		
		MDatabase database = loadDatabase(folder, tPoseFile);
		MotionMatchingSampling3 sampling = new MotionMatchingSampling3(database, model);
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
			
//			String label = folder + "_rot_dy";
			String label = folder + "_rot_tAdap_rMargin" + rMargin + "_" + RotationModel.GOAL_TIME_LIMIT;
			RNNDataGenerator.prepareTrainingFolder(label);
			RNNDataGenerator.generate(mList, new AgilityControlParameterGenerator(model, sampling.controlParameters));
//			RNNDataGenerator.generate(mList, new DirectionControl(sampling.constraintMotionIndices));
		}
	}
}
