package mrl.motion.neural.gmm;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import mrl.motion.data.Motion;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.StuntLocoModel2.StuntLocoGoal2;
import mrl.motion.neural.gmm.GMMConfig.GMMGoal;
import mrl.motion.neural.rl.MFeatureMatching;
import mrl.motion.neural.rl.PolicyEvaluation;
import mrl.motion.neural.rl.PolicyLearning;
import mrl.motion.neural.rl.PolicyEvaluation.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class GMMStuntLocoConfig extends GMMConfig{
	
	public static double LOCOMOTION_RATIO = 0.8;
	public static int LOCO_ACTION_SIZE = 4;
	public static String[] actionTypes = {
			"i",
			"walk",
			"jog",
			"run",
			
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
			"walk",
			"jog",
			"run",
			
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
			{ 10, 10, Double.NaN, Double.NaN }, // i
			{ 10, 10, Double.NaN, Double.NaN }, // walk
			{ 10, 10, Double.NaN, Double.NaN }, // jog
			{ 10, 10, Double.NaN, Double.NaN }, // run
//			{ 4, 4, Double.NaN, Double.NaN }, // i
//			{ 4, 4, Double.NaN, Double.NaN }, // walk
//			{ 6, 6, Double.NaN, Double.NaN }, // jog
//			{ 8, 8, Double.NaN, Double.NaN }, // run
//			action prev margins :: [4, 4, 4, 4, 18, 21, 20, 27, 25, 26, 16, 18, 20, 16, 15, 25]
//			{ 18.786, 4.929, 14.928, 11.322 }, // p
//			{ 21.231, 10.538, 34.398, 19.852 }, // k
//			{ 20.250, 11.750, 34.526, 30.272 }, // bs
//			{ 27.500, 11.500, 32.299, 14.874 }, // jk
//			{ 25.000, 13.333, 86.560, 33.417 }, // jbs
//			{ 26.500, 16.000, 89.288, 92.581 }, // jd
			
			
			{ 7.786, 4.929, 14.928, 11.322 }, // p
			{ 12.231, 10.538, 34.398, 19.852 }, // k
			{ 11.250, 11.750, 34.526, 30.272 }, // bs
			{ 20.500, 11.500, 32.299, 14.874 }, // jk
			{ 20.000, 13.333, 86.560, 33.417 }, // jbs
			{ 18.500, 16.000, 89.288, 92.581 }, // jd
			
//			{ 9.500, 6.500, 6.562, 4.436 }, // th
//			{ 9.818, 9.091, 33.182, 30.967 }, // lk
//			{ 13.500, 12.000, 11.171, 9.806 }, // knee
//			{ 9.000, 8.000, 27.772, 24.140 }, // d
//			{ 7.000, 4.462, 15.051, 10.139 }, // g
//			{ 0.000, 0.000, 0.000, 0.000 }, // n
	};

	public GMMStuntLocoConfig(String name) {
		super(name, fullActionTypes, actionTypes, LOCO_ACTION_SIZE);
	}
	
	@Override
	public GMMGoalGenerator makeGoalGenerator() {
		return new StuntLocoGoalGenerator();
	}
	
	private static int stopIndex() {
		return 0;
	}
	
	public static boolean USE_STRAIGHT_SAMPLING = true;
	private class StuntLocoGoalGenerator extends GMMGoalGenerator{
		
		
		private boolean sampleStraight = false;
		private GMMStuntLocoGoal lastGoal;
		private Pose2d prevPose = new Pose2d(Pose2d.BASE);
		private Motion prevMotion = null;

		private int locoCount = 0;
		private int locoType = 1;
//		private int locoType = 0;
		private int count = 0;
		
		public StuntLocoGoalGenerator() {
			prevPose = new Pose2d(Pose2d.BASE);
			lastGoal = new GMMStuntLocoGoal(MathUtil.random.nextInt(getActionSize()), AgilityModel.GOAL_TIME_LIMIT, 0);
		}

		private boolean isActive(int action) {
			return action >= LOCO_ACTION_SIZE;
		}
		
		@Override
		public GMMGoal sampleRandomGoal(Pose2d currentPose, Motion currentMotion) {
			int actionType;
			double rotation;
			double lRatio = LOCOMOTION_RATIO;
			
			boolean isLastActive = isActive(lastGoal.actionType);
			if (isLastActive) {
				lRatio = 0.3;
			}
			
			if (MathUtil.random.nextDouble() < lRatio) {
				actionType = locoType;
				rotation = RotationModel.sampleRotation();
//				if (MathUtil.random.nextDouble() < 0.33) {
//					rotation /= 20;
//				}
				if (isLastActive) {
					rotation = MathUtil.directionalAngle(currentPose.direction, prevPose.direction);
					sampleStraight = false;
				} else if (USE_STRAIGHT_SAMPLING) {
					if (sampleStraight) {
						rotation /= 20;
					}
					sampleStraight = !sampleStraight;
					if (Math.abs(rotation) < Math.PI / 4) {
						sampleStraight = false;
					}
				}
			} else {
				actionType = MathUtil.random.nextInt(actionTypes.length - LOCO_ACTION_SIZE) + LOCO_ACTION_SIZE;
				if (CONTROL_ACTION_DIRECTION) {
					rotation = 0.33*RotationModel.sampleRotation();
					if (isLastActive && prevMotion != null) {
						rotation += lastGoal.getDirectionOffset(prevMotion);
					}
				} else {
					if (isLastActive) {
						currentPose = prevPose;
					}
					rotation = 0;
				}
			}
			count++;
			double timeLength = 0;
			timeLength += 8; // base margin
			timeLength += timeOffset[lastGoal.actionType][1];
			timeLength += timeOffset[actionType][0];
			if (!isActive(actionType) && isActive(lastGoal.actionType)) {
				timeLength += 10;
			}
//			int adjustedTime = MathUtil.round(timeLength/20d*PolicyLearning.TIME_LIMIT_BASE);
//			adjustedTime = PolicyLearning.TIME_LIMIT_BASE;
			int adjustedTime = MathUtil.round(timeLength*TIME_RATIO);
			
			if (!isActive(actionType)) {
				locoCount++;
				if (actionType == 0) {
					locoCount += 3;
					rotation /= 60;
				}
				if (locoCount > 10) {
					locoCount = 0;
					locoType = 1 + MathUtil.random.nextInt(LOCO_ACTION_SIZE-1);
//					locoType = MathUtil.random.nextInt(LOCO_ACTION_SIZE);
				}
			}
			
			lastGoal = new GMMStuntLocoGoal(actionType, adjustedTime, rotation);
			
//			System.out.println("random goal :: " + locoCount + " : " + locoType + " : " + lastGoal);
			prevPose = currentPose;
			prevMotion = currentMotion;
			return lastGoal;
		}
		
	}

	public class GMMStuntLocoGoal extends GMMGoal{

		public GMMStuntLocoGoal(int actionType, int timeLimit, double direction) {
			super(actionType, timeLimit, direction);
		}
		
		@Override
		public double evaluateFinalError(MatchingPath path, double futureRotation, Pose2d futurePose, Motion finalMotion) {
			double rot = path.rotation + futureRotation;
			if (CONTROL_ACTION_DIRECTION && isActiveAction()) {
				double d = MathUtil.trimAngle(direction - getDirectionOffset(path.current) - rot);
				return d*d;
			}
			if (!isDirectionControl()) {
//				System.out.println("evaluate final error :: " + 0 + " : " + this);
				return 0;
			} else {
				double d = MathUtil.trimAngle(direction - rot);
//				System.out.println("evaluate final error :: " + 0 + " : " + Utils.toString(d, direction, path.rotation));
				return d*d;
			}
		}
		
		public double getDirectionOffset(Motion m) {
			String type = actionTypes[actionType];
			return getDirectionOffsetByType(m, type);
		}
		
		@Override
		public boolean isDirectionControl() {
			if (CONTROL_ACTION_DIRECTION && isActiveAction()) return true;
			return !isActiveAction() && actionType != 0;
		}
		
	}
	
	public static double getDirectionOffsetByType(Motion m, String type) {
		if (type.equals("p")){
			return m._directionOffset("LeftHand", "RightHand");
		} else if (
				type.equals("k") || 
				type.equals("bs") || 
				type.equals("jk") || 
				type.equals("jbs")
				) {
			return m._directionOffset("LeftFoot", "RightFoot");
		} else if (type.equals("jd")){
			return -Math.PI;
		} else {
			return 0;
		}
	}
}
