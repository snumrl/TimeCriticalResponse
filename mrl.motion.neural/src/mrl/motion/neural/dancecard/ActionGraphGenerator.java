package mrl.motion.neural.dancecard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

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
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.motion.graph.MGraphExplorer;
import mrl.motion.graph.MGraphGenerator;
import mrl.motion.graph.MGraphSearch;
import mrl.motion.graph.MGraphSearch.SearchSeed;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agile.ActionSearch;
import mrl.motion.neural.data.ActionControl;
import mrl.motion.neural.data.ActionOnlyWaveControl;
import mrl.motion.neural.data.ActionTarget;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.IterativeRunnable;
import mrl.util.Utils;

public class ActionGraphGenerator {

	public static String[] FightActionTypes = { "punch", "kick" };
	
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
	
	static int[] historgram = new int[100];
	static int sum = 0;
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
		folder = "dc_loco_evade";
//		folder = "dc_merged";
		
		
		init(folder);
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = edgeLimit;
		LevelOfErrorGraph.setConfiguration();
		MDatabase database = MDatabase.load();
		Configuration.BASE_MOTION_FILE = "t_pose_actor.bvh";
		
		
		
		MotionDataConverter.setAllJoints();
		MotionDataConverter.setNoBall();
		MotionDataConverter.setUseOrientation();
		
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
		HashMap<Thread, ActionSearch> searchMap = new HashMap<Thread, ActionSearch>();
		String type = "kick";
		ActionSearch baseSearch = new ActionSearch(graph, type);
		MGraphSearch.MAX_CANDIDATE_SIZE = 40000;
		baseSearch.init(100);
		
		MGraphNode[] nodeList = graph.getNodeList();
		
		Utils.runMultiThread(new IterativeRunnable() {
			@Override
			public void run(int i) {
				Thread thread = Thread.currentThread();
				ActionSearch search = searchMap.get(thread);
				if (search == null) {
					search = new ActionSearch(graph, type);
					search.init(baseSearch.getGraphDP());
					searchMap.put(thread, search);
				}
				
				// TODO Auto-generated method stub
				ArrayList<SearchSeed> candidates = search.getCandidates(nodeList[i], 3, 80);
				int minTime = 99;
				for (SearchSeed s : candidates) {
					if (s.time < minTime) minTime= s.time;
				}
				synchronized (historgram) {
					historgram[minTime]++;
					sum++;
				}
				System.out.println("proc :: " + sum);
			}
		}, nodeList.length);

		System.out.println("sum : " + sum);
		for (int i = 0; i < historgram.length; i++) {
			System.out.println(i + "\t" + historgram[i]);
		}
		System.exit(0);
		
		
		
		
//		final int dataGenSize = 3000;
		final int dataGenSize = 800000;
		MGraphExplorer exp = new MGraphExplorer(graph);
		MGraphExplorer.TRANSITION_LIMIT = 10;
		exp.interactionPickOffset = 200;
		ArrayList<Integer> interactionFrames = new ArrayList<Integer>();
		for (MotionAnnotation ann : database.getEventAnnotations()) {
			if (ann.interactionFrame > 0) {
				System.out.println("interaction :: " + database.getInteractionMotionIndex(ann) + " : " + ann);
				interactionFrames.add(database.getInteractionMotionIndex(ann));
			}
		}
//		exp.setInteractionFrames(interactionFrames, 120);
		exp.setInteractionFrames(interactionFrames, 30);
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
			
//			ActionOnlyControl.USE_DIRECTION_CONTROL = true;
			{
				String label = "";
				RNNDataGenerator.prepareTrainingFolder(label);
				RNNDataGenerator.generate(segment, new ActionControl(new String[] {"evade_l", "evade_r"} , database, targetList));
				System.exit(0);
			}
//			RNNDataGenerator.generate(segment, new ActionOnlyControl(FightActionTypes, database, targetList));
			
			String label = "kickTest_raw_timing";
			
			RNNDataGenerator.prepareTrainingFolder(label);
			if (label.equals("ao_d_wv2_gitp_acti_no_time")) {
				ActionOnlyWaveControl.USE_ACTIVATION = true;
				ActionOnlyWaveControl.USE_GOAL_INTERPOLATION = true;
				ActionOnlyWaveControl.USE_TIMING_PARAMETER = false;
				RNNDataGenerator.generate(segment, new ActionOnlyWaveControl(FightActionTypes, database, targetList));
			} else if (label.equals("ao_d_wv2_gitp_activation")) {
				ActionOnlyWaveControl.USE_ACTIVATION = true;
				ActionOnlyWaveControl.USE_GOAL_INTERPOLATION = true;
				RNNDataGenerator.generate(segment, new ActionOnlyWaveControl(FightActionTypes, database, targetList));
			} else if (label.equals("kickTest_no_interpol")) {
				ActionOnlyWaveControl.USE_ACTIVATION = true;
				ActionOnlyWaveControl.POST_MARGIN = 0;
				RNNDataGenerator.generate(segment, new ActionOnlyWaveControl(FightActionTypes, database, targetList));
			} else if (label.equals("kickTest_raw_timing")) {
				ActionOnlyWaveControl.USE_ACTIVATION = true;
				ActionOnlyWaveControl.POST_MARGIN = 0;
				ActionOnlyWaveControl.USE_RAW_TIMING = true;
				RNNDataGenerator.generate(segment, new ActionOnlyWaveControl(FightActionTypes, database, targetList));
			} else {
				ActionOnlyWaveControl.USE_ACTIVATION = true;
				ActionOnlyWaveControl.USE_GOAL_INTERPOLATION = true;
				RNNDataGenerator.generate(segment, new ActionOnlyWaveControl(FightActionTypes, database, targetList));
			}
		}
	}
}
