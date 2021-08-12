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
import mrl.motion.dp.LevelOfErrorGraph;
import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraphExplorer;
import mrl.motion.graph.MGraphGenerator;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.data.ActionControl;
import mrl.motion.neural.data.ActionTarget;
import mrl.motion.neural.data.ActivationActionControl;
import mrl.motion.neural.data.DataExtractor;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.Utils;

public class DanceCardGraphGenerator {

	public static String[] FightActionTypes = { "punch", "kick" };
	public static String[] JumpActionTypes = {
			"jog",
			"jump_both",
			"jump_one",
			"jump_moving",
			"flip"
	};
	
	public static void init(String folder){
		Configuration.setDataFolder("danceCard\\" + folder);
		Configuration.MGRAPH_NODE_CACHE_FILE = "danceCard\\graphs\\" + folder + "_" + Configuration.MGRAPH_NODE_CACHE_FILE;
		Configuration.MGRAPH_EDGE_CACHE_FILE = "danceCard\\graphs\\" + folder + "_" + Configuration.MGRAPH_EDGE_CACHE_FILE;
	}

//	static boolean showMotion = true;
//	static boolean makeGraph = true;
	static boolean showMotion = false;
	static boolean makeGraph = false;
	static boolean writeAsBVH = false;
	
	public static void main(String[] args) {

		
		String folder;
		double edgeLimit;
		
		
		edgeLimit = 50;
		MGraphExplorer.TRANSITION_LIMIT = 20;
		FootContactDetection.heightLimit = new double[]{ 15, 11, 9 };
		FootContactDetection.velocityLimit = new double[]{3 , 3, 3 };
		
//		makeGraph = true;
//		showMotion = true;
//		writeAsBVH = true;
		
//		folder = "dc_small";
//		folder = "dc_merged";
//		folder = "dcFightMerged";
		folder = "jump_jog_ret_ue4_graph";
		
		
		init(folder);
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = edgeLimit;
		LevelOfErrorGraph.setConfiguration();
		MDatabase database = MDatabase.load();
		Configuration.BASE_MOTION_FILE = "t_pose_ue2.bvh";
//		Configuration.BASE_MOTION_FILE = "t_pose_actor.bvh";
		DataExtractor.STD_LIMIT = -1;
		DataExtractor.POSE_LENGTH = 999;
		MotionDataConverter.setAllJoints();
		MotionDataConverter.setNoBall();
		MotionDataConverter.setUseOrientation();
		MotionDataConverter.setOrientationJointsByFileOrder();
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
			ActionTransitionConnector atc = new ActionTransitionConnector(database);
			// generate new motion graph and save to files.
			MGraphGenerator g = new MGraphGenerator(database) {
				protected double getDistanceOffset(MGGNode source, MGGNode target) {
//					char prefix1 = source.motion.motionData.file.getName().charAt(0);
//					char prefix2 = target.motion.motionData.file.getName().charAt(0);
//					if (prefix1 != prefix2) return 0.5;
////					if (!prefix1.equals(prefix2)) return 0.5;
//					return 1;
					return atc.getEdgeWeight(source.motion.motionIndex, target.motion.motionIndex);
				}
			};
			g.saveResult();
		}
		
		// load motion graph from saved files.
		MGraph graph = new MGraph(database);
		final int dataGenSize = 200000;
		MGraphExplorer exp = new MGraphExplorer(graph) {
			protected void update(int totalLen) {
				if (totalLen < dataGenSize * 0.33) {
					interactionPickOffset = 0;
				} else if (totalLen < dataGenSize * 0.66) {
					interactionPickOffset = 10;
				} else {
					interactionPickOffset = 100;
				}
			}
		};
		ArrayList<Integer> interactionFrames = new ArrayList<Integer>();
		for (MotionAnnotation ann : database.getEventAnnotations()) {
			if (ann.interactionFrame > 0) {
				System.out.println("interaction :: " + database.getInteractionMotionIndex(ann) + " : " + ann);
				interactionFrames.add(database.getInteractionMotionIndex(ann));
			}
		}
		exp.setInteractionFrames(interactionFrames, 120);
//		exp.setInteractionFrames(interactionFrames, 240);
		ArrayList<int[]> segments = exp.explore(showMotion ? 3000 : dataGenSize);
		
		MotionSegment segment = MotionSegment.getPathMotion(database.getMotionList(), Utils.toArray(segments));
		TrajectoryEdit.CUT_MIN = 90;
		TrajectoryEdit.CUT_MAX = 180;
		TrajectoryEdit tEdit = new TrajectoryEdit();
//		tEdit.timeOffset = 0;
		tEdit.timeOffset = 0.15;
		segment = tEdit.edit(segment);
		FootSlipCleanup.clean(segment);
		ArrayList<Motion> mList = MotionData.divideByKnot(segment.getEntireMotion());
//		for (int i = 0; i < 300; i++) {
//			boolean contains = interactionFrames.contains(mList.get(i).motionIndex) || interactionFrames.contains(mList.get(i).motionIndex -1);
//			System.out.println(contains + " : " + mList.get(i).motionIndex + " : " + mList.get(i));
//		}
		
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
			ArrayList<MotionAnnotation> interactionAnns = database.getEventAnnotations();
			ArrayList<ActionTarget> targetList = ActionTarget.getActionTargets(segment, interactionAnns, database);
			
//			System.out.println("-----------------");
//			for (ActionTarget target : targetList) {
//				System.out.println(target.mIndex + " : " + target.cIndex + " : " + target.ann);
//			}
//			System.out.println("-----------------");
//			int[] clip = ActionTarget.maxMatchClip;
//			MotionData mData = new MotionData(Utils.cut(segment.getMotionList(), clip[0], clip[1]));
//			int idx = clip[0];
//			for (Motion motion : mData.motionList) {
//				boolean contains = interactionFrames.contains(motion.motionIndex) || interactionFrames.contains(motion.motionIndex -1);
//				System.out.println(idx + " : " + contains + " : " + motion);
//				idx++;
//			}
//			MainViewerModule.run(mData);
//			System.exit(0);
			
			ActivationActionControl.PREV_MARGIN = 60;
			RNNDataGenerator.prepareTrainingFolder(folder + "_dOnly");
			RNNDataGenerator.generate(segment, new ActivationActionControl(JumpActionTypes, database, targetList));
//			ActionControl.ACTION_MARGIN = 30;
//			ActionControl.POST_MARGIN = 10;
//			RNNDataGenerator.generate(segment, new ActionControl(FightActionTypes, database, targetList));
//			RNNDataGenerator.generate(segment, new PositionControl());
		}
	}
}
