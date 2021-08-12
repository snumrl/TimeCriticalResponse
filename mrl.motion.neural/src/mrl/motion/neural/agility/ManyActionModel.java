package mrl.motion.neural.agility;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.StuntLocoModel2.StuntLocoGoal2;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class ManyActionModel extends AgilityModel {
	
	public static double LOCOMOTION_RATIO = 0.8;
	public static int LOCO_ACTION_SIZE = 4;
	
	public static String[] fullActionTypes = {
			"i",
			"walk",
			"jog",
			"run",
			
			"bs",
			"casting",
			"cheer",
			"dance",
			"flip",
			"golf",
			"hot",
			"jbs",
			"jd",
			"jk",
			"jump",
			"jump_moving",
			"jump_one",
			"k",
			"roll",
	};
	public static String[] actionTypes = fullActionTypes;
	
	public static double[][] timeOffset = {
			{ 10.000, 10.000, -1.000, -1.000 }, // i
			{ 10.000, 10.000, -1.000, -1.000 }, // walk
			{ 10.000, 10.000, -1.000, -1.000 }, // jog
			{ 10.000, 10.000, -1.000, -1.000 }, // run
			{ 15.000, 13.000, 38.260, 15.653 }, // bs
			{ 28.000, 19.000, 24.862, 18.476 }, // casting
			{ 17.000, 18.000, 12.943, 24.592 }, // cheer
			{ 23.000, 25.000, 115.299, 102.694 }, // dance
			{ 21.000, 11.000, 227.400, 60.841 }, // flip
			{ 24.000, 29.000, 16.978, 12.324 }, // golf
			{ 24.000, 22.000, 7.969, 12.871 }, // hot
			{ 20.000, 15.000, 76.059, 14.507 }, // jbs
			{ 16.000, 16.000, 82.091, 76.769 }, // jd
			{ 22.000, 12.000, 32.302, 10.041 }, // jk
			{ 17.000, 11.000, 21.599, 17.483 }, // jump
			{ 12.000, 18.000, 161.658, 190.901 }, // jump_moving
			{ 15.000, 14.000, 160.747, 96.635 }, // jump_one
			{ 13.000, 10.000, 53.273, 33.795 }, // k
			{ 16.000, 13.000, 165.260, 83.553 }, // roll
	};
	
	public static boolean USE_STRAIGHT_SAMPLING = false;
	
	private boolean sampleStraight = false;
	private ManyActionGoal lastGoal;
	private int locoCount = 0;
	private int locoType = 0;
	private Pose2d prevPose = new Pose2d(Pose2d.BASE);
	
	public ManyActionModel() {
		lastGoal = sampleIdleGoal();
	}
	
	public String[] getActionTypes() {
		return actionTypes;
	}
	public String[] getFullActionTypes() {
		return fullActionTypes;
	}
	public int getContinuousLabelSize() {
		return LOCO_ACTION_SIZE;
	}
	public boolean useActivation() {
		return true;
	}
	
	@Override
	public ManyActionGoal sampleIdleGoal() {
		lastGoal = new ManyActionGoal(0, 0, GOAL_TIME_LIMIT);
		return lastGoal;
	}
	
	private boolean isActive(int action) {
		return action >= LOCO_ACTION_SIZE;
	}
	
	@Override
	public ManyActionGoal sampleRandomGoal(Pose2d currentPose) {
		int actionType;
		double rotation;
		double lRatio = LOCOMOTION_RATIO;
		
		boolean isLastActive = isActive(lastGoal.actionType);
		if (isLastActive) {
			lRatio = 0.4;
//			lRatio = 0.3;
		}
		
		if (MathUtil.random.nextDouble() < lRatio) {
			actionType = locoType;
			rotation = RotationModel.sampleRotation();
//			if (MathUtil.random.nextDouble() < 0.33) {
//				rotation /= 20;
//			}
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
			if (isLastActive) {
				currentPose = prevPose;
			}
			actionType = MathUtil.random.nextInt(actionTypes.length - LOCO_ACTION_SIZE) + LOCO_ACTION_SIZE;
			rotation = 0;
		}
		double timeLength = 0;
		timeLength += 8; // base margin
		timeLength += timeOffset[lastGoal.actionType][1];
		timeLength += timeOffset[actionType][0];
		int adjustedTime = MathUtil.round(timeLength/20d*GOAL_TIME_LIMIT);
		
//		adjustedTime = GOAL_TIME_LIMIT;
		if (!isActive(actionType)) {
			locoCount++;
			if (actionType == 0) {
				locoCount += 3;
				rotation /= 60;
			}
			if (locoCount > 10) {
				locoCount = 0;
				locoType = MathUtil.random.nextInt(LOCO_ACTION_SIZE);
			}
		}
		
		lastGoal = new ManyActionGoal(rotation, actionType, adjustedTime);
		System.out.println("random goal :: " + locoCount + " : " + locoType + " : " + lastGoal);
		prevPose = currentPose;
		return lastGoal;
	}
	
	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		throw new RuntimeException();
	}
	
	public class ManyActionGoal extends AgilityGoal{
		public double targetRotation;
		
		public ManyActionGoal(double targetRotation, int actionType, int timeLimit) {
			super(actionType, timeLimit);
			this.targetRotation = targetRotation;
		}
		@Override
		public Pose2d getEditingConstraint() {
			if (isActive(actionType) || actionType == 0) {
				return new Pose2d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
			} else {
				Vector2d v = new Vector2d(Pose2d.BASE.direction);
				v = MathUtil.rotate(v, targetRotation);
				Pose2d p = new Pose2d(new Point2d(Double.NaN, Double.NaN), v);
				return p;
			}
		}
		
		
		@Override
		public boolean isGoalFinished(Pose2d startPose, ArrayList<Motion> motionList, ArrayList<Double> activationList) {
			if (motionList.size() < AgilityModel.TIME_EXTENSION_MIN_TIME) return false;
			
			if (isActiveAction()) {
				return Utils.last(activationList) > 0.5;
			} else {
				Vector2d target = MathUtil.rotate(Pose2d.BASE.direction, targetRotation);
				int vCountLimit = 2;
				for (int i = 0; i < vCountLimit; i++) {
					int idx = motionList.size() - vCountLimit + i;
					Pose2d p = PositionMotion.getPose(motionList.get(idx));
					Vector2d direction = startPose.globalToLocal(p.direction);
					double error = direction.angle(target);
					boolean isValid = Math.abs(error) < Math.toRadians(10);
					if (!isValid) return false;
				}
				return true;
			}
		}
		
		@Override
		public double[] getControlParameter(ArrayList<Pose2d> poseList) {
			double[] control = actionData(actionType);
			double remain = Double.NaN;
			if (!isActiveAction()) {
				double rotSum = 0;
				
				for (int i = 0; i < poseList.size()-1; i++) {
					Pose2d p = poseList.get(i);
					Pose2d next = poseList.get(i+1);
					double angle = MathUtil.directionalAngle(p.direction, next.direction);
					rotSum += angle;
				}
				double rotated = rotSum;
				remain = targetRotation - rotated; 
			}
			control = MathUtil.concatenate(control, new double[] { remain });
			return control;
		}
		@Override
		public Object getControlParameterObject(Pose2d startPose, Pose2d currentPose) {
			if (isActive(actionType)) {
				return Pose2d.to3d(startPose.position);
			} else {
				Vector2d v = new Vector2d(Pose2d.BASE.direction);
				v = MathUtil.rotate(v, targetRotation);
				v = startPose.localToGlobal(v);
				return new Pose2d(currentPose.position, v);
			}
		}
		
		@Override
		public double[] getControlParameter(ArrayList<Motion> mList, int currentIndex, int targetIndex) {
			double rotSum = Double.NaN;
			if (!isActive(actionType)) {
				rotSum = 0;
				for (int i = currentIndex; i < targetIndex; i++) {
					Pose2d p = PositionMotion.getPose(mList.get(i));
					Pose2d next = PositionMotion.getPose(mList.get(i + 1));
					double angle = MathUtil.directionalAngle(p.direction, next.direction);
					rotSum += angle;
				}
			}
			double[] control = actionData(actionType);
			control = MathUtil.concatenate(control, new double[] { rotSum });
			return control;
		}
		
		@Override
		public boolean isActiveAction() {
			return isActive(actionType);
		}
		
		public double getSpatialError(Pose2d currentMoved, double currentRotated, Pose2d futurePose, double futureRotation) {
			if (isActive(actionType) || actionType == 0) {
				return 0;
			} else {
				double tr = targetRotation - currentRotated;
				double d = tr - futureRotation;
				d = d * 200;
				return d*d;
			}
		}
		
		public String toString() {
			return "ManyActionGoal:" + Utils.toString(targetRotation, actionType, timeLimit);
		}
	}
	

}
