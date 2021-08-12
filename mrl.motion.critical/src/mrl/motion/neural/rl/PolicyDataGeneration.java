package mrl.motion.neural.rl;

import java.io.File;
import java.util.ArrayList;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.match.MMatching;
import mrl.motion.neural.data.DataExtractor;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.motion.neural.gmm.GMMConfig;
import mrl.motion.neural.gmm.GMMConfig.GMMGoal;
import mrl.motion.neural.gmm.GMMJumpConfig;
import mrl.motion.neural.gmm.GMMStuntLocoConfig;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class PolicyDataGeneration extends PolicyLearning{

	public static boolean useDynamicAgility = true;
	public static double DYNAMIC_AGILITY_MIN = 0.66;
	public static double DYNAMIC_AGILITY_MAX = 1.33;
	public static int AGILITY_CHANGE_COUNT = 20;

	public PolicyDataGeneration(GMMConfig config) {
		super(config, false);
	}

	
	public void generateData(String namePostfix, int totalLength) {
		prepareStatistics();
		
		MotionSegment totalSegment = null;
		ArrayList<Integer> constraintMotionIndices = new ArrayList<Integer>();
		
		RLAgent agent = new RLAgent(tData, matching);
		
		ArrayList<PDGControlParameter> controlParameters = new ArrayList<PDGControlParameter>();
		int aCount = 0;
		double agility = 1;
		while(true) {
			RL_State state = agent.prepareState();
			MotionQuery action = python.getMeanAction(state);
			agent.proceedStep(state, action);
			if (agent.isFinished()) {
				aCount++;
				if (useDynamicAgility && aCount > AGILITY_CHANGE_COUNT) {
					aCount = 0;
					agility = DYNAMIC_AGILITY_MIN + MathUtil.random.nextDouble()*(DYNAMIC_AGILITY_MAX - DYNAMIC_AGILITY_MIN);
				}
				GMMGoal goal = agent.goal;
				MotionSegment segment = MotionSegment.getPathMotion(config.tData.database.getMotionList(), agent.path.getPath(), 0);
				int timeConstraint = Math.min(agent.path.time, MathUtil.round(goal.timeLimit*agility));
				MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, goal.getEditingConstraint(segment), timeConstraint);
				edited = new MotionSegment(edited, 1, edited.length()-1);
				if (totalSegment == null) {
					totalSegment = edited;
					MotionSegment.alignToBase(totalSegment);
				} else {
					totalSegment = MotionSegment.stitch(totalSegment, edited, true);
				}
				
				int length = MathUtil.round(totalSegment.lastMotion().knot);
				System.out.println("goal : " +  length + " : " + goal);
				System.out.println("time constraint : " + timeConstraint + " : " + goal.timeLimit + " : " + agent.path.time);
				constraintMotionIndices.add(length);
				controlParameters.add(new PDGControlParameter(length, goal, agility));
				if (length > totalLength) break;
			}
		}
		FootSlipCleanup.clean(totalSegment);
		System.out.println("t len : " + totalSegment.length());
		ArrayList<Motion> totalMotions = totalSegment.getMotionList();
		ArrayList<Motion> mList = MotionData.divideByKnot(totalMotions);
		mList = MotionSegment.alignToBase(mList, 0);
		
		String label = config.name + namePostfix;
		RNNDataGenerator.prepareTrainingFolder(label);
		RNNDataGenerator.generate(mList, new PolicyControlParameterGenerator(config, controlParameters));
//		return mList;
	}
	
	public static class PDGControlParameter{
		public int frame;
		public GMMGoal goal;
		public double agility;
		
		public PDGControlParameter(int frame, GMMGoal goal, double agility) {
			this.frame = frame;
			this.goal = goal;
			this.agility = agility;
		}
	}
	
	static void packData(String folder) {
		String origin = Normalizer.NEURALDATA_PREFIX + "\\" + folder;
		String target = Normalizer.NEURALDATA_PREFIX + "\\upload\\" + folder;
		Utils.copyFolder(new File(origin), new File(target));
		Normalizer normal = new Normalizer(folder);
		DataExtractor.writeData(target + "\\data\\xData.dat", Utils.cut(normal.xList, 0, 100));
		DataExtractor.writeData(target + "\\data\\yData.dat", Utils.cut(normal.yList, 0, 100));
		System.exit(0);
	}
	
	public static void main(String[] args) {
//		packData("jump_jog_ret_ue4_graph_dOnly");
		
		String namePostfix = "";
//		GMMConfig config = PolicyLearning.jump();
		
//		GMMConfig config = PolicyLearning.jump(60, "_short05", true);
		
//		DYNAMIC_AGILITY_MIN = 0.7;
//		DYNAMIC_AGILITY_MAX = 1.4;
//		useDynamicAgility = true;
//		GMMConfig config = PolicyLearning.jump(80, "_short05", false);
////		GMMConfig config = PolicyLearning.jump(60, "_short05", false);
//		namePostfix += "_dy";
		
		GMMConfig config = PolicyLearning.jump(100, "_short05", false);
		AgilityModel.TIME_EXTENSION_RATIO = 0.33;
		
//		GMMConfig config = PolicyLearning.jump(40, "");
//		GMMJumpConfig.LOCO_TIME_OFFSET = 10;
//		namePostfix = "_test";
		
		
		
//		DYNAMIC_AGILITY_MIN = 1;
//		DYNAMIC_AGILITY_MAX = 1.3;
//		useDynamicAgility = true;
		
//		GMMConfig config = PolicyLearning.stunt_only(false, "");
//		AgilityModel.TIME_EXTENSION_RATIO = 0.33;
//		GMMConfig.TIME_RATIO = 1;
//		DYNAMIC_AGILITY_MIN = 1;
//		DYNAMIC_AGILITY_MAX = 1.3;
//		useDynamicAgility = true;
//		config.name += "_" + Utils.toString(AgilityModel.TIME_EXTENSION_RATIO, GMMConfig.TIME_RATIO, DYNAMIC_AGILITY_MAX).replace("\t", "_");
//		config.name = config.name.replace("0", "");
				
		
//		Motion m = config.tData.database.findMotion("s_020_1_2:420");
//		double angle = m._directionOffset("LeftFoot", "RightFoot");
//		System.out.println(Math.toDegrees(angle));
//		System.exit(0);
		
		
		
//		String namePostfix = "_4";
		
//		GMMStuntLocoConfig.LOCOMOTION_RATIO = 0.8;
//		AgilityModel.TIME_EXTENSION_RATIO = 0.3;
//		RotationModel.ROTAION_ANGLE_MARGIN = Math.toRadians(0);
		MotionDataConverter.useMatrixForAll = true;
		MotionDataConverter.useTPoseForMatrix = false;
		AgilityModel.GOAL_TIME_LIMIT = 20;
		DataExtractor.STD_LIMIT = -1;
		DataExtractor.POSE_LENGTH = 999;
//		model = new ControlGraphModel(graph);
		MotionDataConverter.setOrientationJointsByFileOrder();
		
		new PolicyDataGeneration(config).generateData(namePostfix, 100000);
	}
}
