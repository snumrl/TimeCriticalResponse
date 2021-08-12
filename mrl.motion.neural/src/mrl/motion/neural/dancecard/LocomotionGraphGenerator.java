package mrl.motion.neural.dancecard;

import java.io.File;
import java.util.ArrayList;

import javax.vecmath.Quat4d;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.TrajectoryEdit;
import mrl.motion.data.parser.BVHWriter;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.dp.LevelOfErrorGraph;
import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraphExplorer;
import mrl.motion.graph.MGraphGenerator;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.data.ActionControl;
import mrl.motion.neural.data.ActionTarget;
import mrl.motion.neural.data.ActivationActionControl;
import mrl.motion.neural.data.DirectionControl;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.PositionControl;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.Utils;

public class LocomotionGraphGenerator {

	public static void init(String folder){
		Configuration.setDataFolder("danceCard\\" + folder);
		Configuration.MGRAPH_NODE_CACHE_FILE = "danceCard\\graphs\\" + folder + "_" + Configuration.MGRAPH_NODE_CACHE_FILE;
		Configuration.MGRAPH_EDGE_CACHE_FILE = "danceCard\\graphs\\" + folder + "_" + Configuration.MGRAPH_EDGE_CACHE_FILE;
	}

	static boolean showMotion = false;
	static boolean makeGraph = false;
	static boolean writeAsBVH = false;
	
	public static void main(String[] args) {

		
		String folder;
		double edgeLimit;
		
		
		edgeLimit = 100;
		MGraphExplorer.TRANSITION_LIMIT = 20;
		FootContactDetection.heightLimit = new double[]{ 15, 11, 9 };
		FootContactDetection.velocityLimit = new double[]{3 , 3, 3 };
		
		makeGraph = true;
//		showMotion = true;
//		writeAsBVH = true;
		
		folder = "dc_loco";
//		folder = "dc_jog";
//		folder = "dcFightMerged";
//		folder = "dc_merged";
		
		
		init(folder);
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = edgeLimit;
		MotionDistByPoints.CONTACT_CONFLICT_ERROR = Configuration.MGRAPH_EDGE_WEIGHT_LIMIT/4;
		MDatabase database = MDatabase.load();
		Configuration.BASE_MOTION_FILE = "t_pose_actor.bvh";
		
		
		
		MotionDataConverter.setAllJoints();
		MotionDataConverter.setNoBall();
		MotionDataConverter.setUseOrientation();
		MotionDataConverter.setOrientationJointsByFileOrder();
//		for (String j : MotionDataConverter.OrientationJointList) {
//			System.out.println(j);
//		}
//		System.exit(0);
		MotionDataConverter.useMatrixForAll = true;
		MotionDataConverter.useTPoseForMatrix = false;
		
//		MotionDataConverter.printOutputSpec();
//		System.exit(0);
		int len = database.getMotionList().length;
		int sec = (len/30);
		System.out.println(folder + " : " + sec);
//		System.exit(0);
//		database.addMirroredData();
		if (makeGraph){
			// generate new motion graph and save to files.
			MGraphGenerator g = new MGraphGenerator(database);
			g.saveResult();
		}
		
		// load motion graph from saved files.
		MGraph graph = new MGraph(database);
		final int dataGenSize = 200000;
		MGraphExplorer exp = new MGraphExplorer(graph);
		ArrayList<int[]> segments = exp.explore(showMotion ? 3000 : dataGenSize);
		
		MotionSegment segment = MotionSegment.getPathMotion(database.getMotionList(), Utils.toArray(segments));
		TrajectoryEdit.CUT_MIN = 90;
		TrajectoryEdit.CUT_MAX = 180;
		TrajectoryEdit tEdit = new TrajectoryEdit();
		tEdit.lengthOffset = 0.2;
//		tEdit.timeOffset = 0;
		tEdit.timeOffset = 0.15;
		segment = tEdit.edit(segment);
		FootSlipCleanup.clean(segment);
		ArrayList<Motion> mList = MotionData.divideByKnot(segment.getEntireMotion());
		
		segment = new MotionSegment(Utils.toArray(mList), MotionSegment.BLEND_MARGIN(), mList.size() - MotionSegment.BLEND_MARGIN() - 1, true);
		if (writeAsBVH) {
			ArrayList<Motion> motionList = segment.getEntireMotion();
			motionList.get(0).knot = Double.NaN;
			BVHWriter bw = new BVHWriter(new File(Configuration.BASE_MOTION_FILE));
			bw.write(new File("augmented.bvh"), new MotionData(motionList));
		} else if (showMotion){
			// show generated motion
			MotionData mData = new MotionData(segment.getMotionList());
			MainViewerModule.run(mData);
		} else {
			int interval = 30;
//			String label = folder + "_dir_" + interval;
//			String label = folder + "_dir_v_" + interval;
//			DirectionControl.USE_VELOCITY = true;
//			RNNDataGenerator.prepareTrainingFolder(label);
//			RNNDataGenerator.generate(segment, new DirectionControl(interval));
			
			String label = folder + "_g_v_pos_" + interval;
			RNNDataGenerator.prepareTrainingFolder(label);
			RNNDataGenerator.generate(segment, new PositionControl(interval));
		}
	}
}
