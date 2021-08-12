package mrl.motion.neural.agility;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class StuntLocoModel extends AgilityModel {
	
	public static double LOCOMOTION_RATIO = 0.8;
	
	public static int LOCO_ACTION_SIZE = 4;
	public static String[] actionTypes = {
			"i",
			"walk",
			"jog",
			"run",
			"bs",
			"jk",
			"jbs",
			"jd",
			
//			"th",
//			"lk",
//			"p",
//			"k",
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
			"bs",
			"jk",
			"jbs",
			"jd",
			
			"th",
			"lk",
			"p",
			"k",
			"knee",
//			"d",
//			"g",
			"n",
	};
//	public static String[] actionTypes = {
//			"i",
//			"walk",
//			"jog",
//			"run",
//			"p",
//			"k",
//			"bs",
//			"jk",
//			"jbs",
//			"jd",
//			
////			"th",
////			"lk",
////			"knee",
////			"d",
////			"g",
////			"n",
//	};
//	public static String[] fullActionTypes = {
//			"i",
//			"walk",
//			"jog",
//			"run",
//			"p",
//			"k",
//			"bs",
//			"jk",
//			"jbs",
//			"jd",
//			"th",
//			"lk",
//			"knee",
//			"d",
//			"g",
//			"n",
//	};
	
	public static double[][] timeOffset = {
			{ 4, 4, Double.NaN, Double.NaN }, // i
			{ 4, 4, Double.NaN, Double.NaN }, // walk
			{ 6, 6, Double.NaN, Double.NaN }, // jog
			{ 8, 8, Double.NaN, Double.NaN }, // run
//			{ 7.786, 4.929, 14.928, 11.322 }, // p
//			{ 12.231, 10.538, 34.398, 19.852 }, // k
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
	
	public static boolean USE_STRAIGHT_SAMPLING = false;
	
	private boolean sampleStraight = false;
	private StuntLocoGoal lastGoal;
	private Pose2d prevPose = new Pose2d(Pose2d.BASE);

	private int locoCount = 0;
	private int locoType = 0;
	
	public StuntLocoModel() {
		lastGoal = sampleIdleGoal();
	}
	
	public String[] getActionTypes() {
		return actionTypes;
	}
	public String[] getFullActionTypes() {
		return fullActionTypes;
	}
	public int getContinuousLabelSize() {
		return 4;
	}
	public boolean useActivation() {
		return true;
	}
	
	
	@Override
	public StuntLocoGoal sampleIdleGoal() {
		lastGoal = new StuntLocoGoal(0, 0, GOAL_TIME_LIMIT);
		return lastGoal;
	}
	
	private boolean isActive(int action) {
		return action >= LOCO_ACTION_SIZE;
	}
	
	@Override
	public StuntLocoGoal sampleRandomGoal(Pose2d currentPose) {
		int actionType;
		double rotation;
		double lRatio = LOCOMOTION_RATIO;
		
		boolean isLastActive = isActive(lastGoal.actionType);
		if (isLastActive) {
			lRatio = 0.35;
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
		count++;
		double timeLength = 0;
		timeLength += 8; // base margin
		timeLength += timeOffset[lastGoal.actionType][1];
		timeLength += timeOffset[actionType][0];
		int adjustedTime = MathUtil.round(timeLength/20d*GOAL_TIME_LIMIT);
		
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
		
		if (actionType == 1 || actionType == 2) actionType = 3;
		
		lastGoal = new StuntLocoGoal(rotation, actionType, adjustedTime);
		
		System.out.println("random goal :: " + locoCount + " : " + locoType + " : " + lastGoal);
		prevPose = currentPose;
		return lastGoal;
	}
	int count = 0;
	
	public StuntLocoGoal sampleGoal(int actionType) {
		double timeLength = 0;
		timeLength += 8; // base margin
		timeLength += timeOffset[lastGoal.actionType][1];
		timeLength += timeOffset[actionType][0];
		int adjustedTime = MathUtil.round(timeLength/20d*GOAL_TIME_LIMIT);
		double rotation =  RotationModel.sampleRotation();
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
		lastGoal = new StuntLocoGoal(rotation, actionType, adjustedTime);
		return lastGoal;
	}
	
	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		throw new RuntimeException();
	}
	
	public class StuntLocoGoal extends AgilityGoal{
		public double targetRotation;
		
		public StuntLocoGoal(double targetRotation, int actionType, int timeLimit) {
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
			return "StuntLoco:" + Utils.toString(targetRotation, actionType, timeLimit);
		}
	}
	

}
