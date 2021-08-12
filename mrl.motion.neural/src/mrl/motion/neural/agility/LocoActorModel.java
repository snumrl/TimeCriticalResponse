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

public class LocoActorModel extends AgilityModel {
	
	public static int LOCO_ACTION_SIZE = 4;
	public static String[] actionTypes = {
			"idle",
			"walk",
			"jog",
			"run",
			
			"punch",
			"jump",
			"golf",
			"dance",
			"sit",
	};
	public static String[] fullActionTypes = actionTypes;
	
	public static double[][] timeOffset = {
			{ 15, 15, Double.NaN, Double.NaN }, // idle
			{ 15, 15, Double.NaN, Double.NaN }, // walk
			{ 15, 15, Double.NaN, Double.NaN }, // jog
			{ 15, 15, Double.NaN, Double.NaN }, // run
			{ 11.000, 14.000, 22.418, 7.811 }, // punch
			{ 17.000, 11.000, 22.651, 18.503 }, // jump
			{ 24.000, 29.000, 17.667, 11.814 }, // golf
			{ 23.000, 25.000, 109.442, 102.761 }, // dance
			{ 44.000, 52.000, 18.015, 4.151 }, // sit
	};
	
	private LocoActorGoal lastGoal;
	
	public LocoActorModel() {
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
	public LocoActorGoal sampleIdleGoal() {
		lastGoal = new LocoActorGoal(0, 0, GOAL_TIME_LIMIT);
		return lastGoal;
	}
	
	private boolean isActive(int action) {
		return action >= LOCO_ACTION_SIZE;
	}
	
	@Override
	public LocoActorGoal sampleRandomGoal(Pose2d currentPose) {
		int actionType = MathUtil.random.nextInt(getActionTypes().length);
		double targetRotation = RotationModel.sampleRotation();
		if (isActive(actionType) || actionType == 0) {
			targetRotation = 0;
		}
		double timeLength = 0;
		timeLength += 8; // base margin
		timeLength += timeOffset[lastGoal.actionType][1];
		timeLength += timeOffset[actionType][0];
		int adjustedTime = MathUtil.round(timeLength) + GOAL_TIME_LIMIT;
//		int adjustedTime = MathUtil.round(timeLength/20d*GOAL_TIME_LIMIT);
		
		lastGoal = new LocoActorGoal(targetRotation, actionType, adjustedTime);
		return lastGoal;
	}
	
	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		throw new RuntimeException();
	}
	
	public class LocoActorGoal extends AgilityGoal{
		public double targetRotation;
		
		public LocoActorGoal(double targetRotation, int actionType, int timeLimit) {
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
			throw new RuntimeException();
		}
		
		@Override
		public double[] getControlParameter(ArrayList<Pose2d> poseList) {
			throw new RuntimeException();
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
			return "LocoActor:" + Utils.toString(targetRotation, actionType, timeLimit);
		}
	}
	

}
