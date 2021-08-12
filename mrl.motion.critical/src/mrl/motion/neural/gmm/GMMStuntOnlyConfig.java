package mrl.motion.neural.gmm;

import java.util.ArrayList;
import java.util.LinkedList;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.rl.PolicyEvaluation.MatchingPath;
import mrl.util.MathUtil;

public class GMMStuntOnlyConfig extends GMMConfig{
	
	public static double LOCOMOTION_RATIO = 0.8;
	public static int LOCO_ACTION_SIZE = 1;
	public static String[] actionTypes = {
			"i",
			"p",
			"k",
			"bs",
			"jk",
			"jbs",
			"jd",
			
//			"th",
//			"lk",
//			"knee",
//			"d",
//			"g",
//			"n",
	};
	public static String[] fullActionTypes = {
			"i",
			"p",
			"k",
			"bs",
			"jk",
			"jbs",
			"jd",
			
			"th",
			"lk",
			"knee",
			"d",
			"g",
			"n",
	};
	
	public static double[][] timeOffset = {
			{ 4, 4 }, // i
			{ 7.837, 5.558 }, // p
			{ 10.844, 9.188 }, // k
			{ 11.250, 11.750 }, // bs
			{ 19.000, 10.333 }, // jk
			{ 20.000, 13.333 }, // jbs
			{ 18.500, 16.000 }, // jd
			{ 9.500, 6.500 }, // th
			{ 9.818, 9.091 }, // lk
			{ 13.500, 12.000 }, // knee
			{ 9.000, 8.000 }, // d
			{ 7.000, 4.462 }, // g
			{ 0.000, 0.000 }, // n
			
//			{ 4, 4 }, // i
//			{ 7.377, 5.164 }, // p
//			{ 9.500, 6.500 }, // th
//			{ 10.243, 8.514 }, // k
//			{ 11.250, 11.750 }, // bs
//			{ 16.750, 10.000 }, // jk
//			{ 19.400, 14.000 }, // jbs
//			{ 18.500, 16.000 }, // jd
//			{ 9.818, 9.091 }, // lk
//			{ 13.500, 12.000 }, // knee
//			{ 9.000, 8.000 }, // d
//			{ 7.000, 4.462 }, // g
//			{ 0.000, 0.000 }, // n
//			{ 2.667, 2.667 }, // nd
//			{ 10.000, 6.250 }, // nk
//			{ 3.333, 2.000 }, // nn
	};
	
	private LinkedList<Integer> actionPreset = null;

	public GMMStuntOnlyConfig(String name) {
		super(name, fullActionTypes, actionTypes, LOCO_ACTION_SIZE);
	}
	
	public void setActionPreset(LinkedList<Integer> actionPreset) {
		this.actionPreset = actionPreset;
	}

	@Override
	public GMMGoalGenerator makeGoalGenerator() {
		return new StuntOnlyGoalGenerator();
	}
	
	public static boolean USE_STRAIGHT_SAMPLING = true;
	private class StuntOnlyGoalGenerator extends GMMGoalGenerator{
		
		private GMMStuntOnlyGoal lastGoal;
//		private Motion prevMotion;
		
		public StuntOnlyGoalGenerator() {
			lastGoal = new GMMStuntOnlyGoal(sampleAction(), 20, sampleRotation());
		}

		@Override
		public GMMGoal sampleRandomGoal(Pose2d currentPose, Motion currentMotion) {
			double rotation = sampleRotation();
			if (CONTROL_ACTION_DIRECTION) {
				rotation = 0.33*RotationModel.sampleRotation();
				if (lastGoal.isActiveAction() && currentMotion != null) {
					rotation += lastGoal.getDirectionOffset(currentMotion);
				}
			}
			
			int action = sampleAction();
			double timeLength = 0;
			timeLength += 10; // base margin
			timeLength += timeOffset[lastGoal.actionType][1];
			timeLength += timeOffset[action][0];
			int adjustedTime = MathUtil.round(timeLength*TIME_RATIO);
			lastGoal = new GMMStuntOnlyGoal(action, adjustedTime, rotation);
//			System.out.println("##sampleRandomGoal:Sample NEW GOAL ::: " + lastGoal);
//			prevMotion = currentMotion;
			return lastGoal;
		}
		
		private int sampleAction() {
			if (actionPreset != null) {
				int action = actionPreset.removeFirst();
				if (actionPreset.size() == 0) actionPreset = null;
				return action;
			}
			return 1 + MathUtil.random.nextInt(actionTypes.length - 1); 
		}
		
		private double sampleRotation() {
			return 0.33*RotationModel.sampleRotation();
		}
	}

	public class GMMStuntOnlyGoal extends GMMGoal{

		public GMMStuntOnlyGoal(int actionType, int timeLimit, double direction) {
			super(actionType, timeLimit, direction);
		}
		
		@Override
		public double evaluateFinalError(MatchingPath path, double futureRotation, Pose2d futurePose, Motion finalMotion) {
			if (!CONTROL_ACTION_DIRECTION) return 0;
			
			double rot = path.rotation + futureRotation;
			double d = MathUtil.trimAngle(direction - getDirectionOffset(path.current) - rot);
			return d*d;
		}
		
		@Override
		public double getDirectionOffset(Motion m) {
			String type = actionTypes[actionType];
			return GMMStuntLocoConfig.getDirectionOffsetByType(m, type);
		}

		@Override
		public boolean isDirectionControl() {
			if (!isActiveAction()) return false;
			if (CONTROL_ACTION_DIRECTION) return true;
			return false;
		}
		
	}
}
