package mrl.motion.neural.agility;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.agility.match.MotionMatching.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class RotationSpeedModel extends AgilityModel {

	public static double TIMING_ERROR_OFFSET = 90; // 10 frame == 90 degree
	public static boolean USE_STRAIGHT_SAMPLING = false;
	
	private boolean sampleStraight = false;
	private int lastAction = 0;
	private int actionCount = 0;
	
	public static String[] actionTypes = {
			"walk",
			"jog",
			"run",
			"stop"
	};
	
	public RotationSpeedModel() {
	}
	
	public int getActionSize() {
		return actionTypes.length;
	}
	
	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		return new RotationSpeedMotionMatching(database);
	}
	
	@Override
	public RotationSpeedGoal sampleIdleGoal() {
		return new RotationSpeedGoal(0, lastAction, GOAL_TIME_LIMIT);
	}
	
	@Override
	public RotationSpeedGoal sampleRandomGoal(Pose2d currentPose) {
		double rotation = RotationModel.sampleRotation();
		if (USE_STRAIGHT_SAMPLING) {
			if (sampleStraight && (MathUtil.random.nextDouble() < 0.7)) {
				rotation /= 60;
			}
		}
		
		actionCount++;
		int stopIndex = getActionSize() - 1;
		if (actionCount > 5) {
			int action;
			rotation = RotationModel.sampleRotation();
			if (lastAction == stopIndex) {
				action = MathUtil.random.nextInt(stopIndex);
			} else {
				if (MathUtil.random.nextDouble() < 0.7) {
					action = stopIndex;
				} else {
					action = MathUtil.random.nextInt(stopIndex);
					rotation /= 20;
				}
			}
			lastAction = action;
			actionCount = 0;
		} else {
			if (lastAction == stopIndex) {
				actionCount += 3;
				rotation /= 60;
			}
		}
		
		if (Math.abs(rotation) >= Math.PI / 4) {
			sampleStraight = true;
		} else {
			sampleStraight = false;
		}
		int timeLimit = MathUtil.round(GOAL_TIME_LIMIT);
//		System.out.println("random goal :: " + Math.toDegrees(rotation) + " : " + rotation + " : " + sampleStraight);
		return new RotationSpeedGoal(rotation, lastAction, timeLimit);
	}

	public class RotationSpeedGoal extends AgilityGoal{
		
		public double targetRotation;
		
		public RotationSpeedGoal(double targetRotation, int actionType, int timeLimit) {
			super(actionType, timeLimit);
			this.targetRotation = targetRotation;
		}
		
		@Override
		public Pose2d getEditingConstraint() {
			Vector2d v = new Vector2d(Pose2d.BASE.direction);
			v = MathUtil.rotate(v, targetRotation);
			if (actionType == actionTypes.length-1) {
				v.x = v.y = Double.NaN;
			}
			Pose2d p = new Pose2d(new Point2d(Double.NaN, Double.NaN), v);
			return p;
		}

		@Override
		public double[] getControlParameter(ArrayList<Motion> mList, int currentIndex, int targetIndex) {
			double rotSum = 0;
			for (int i = currentIndex; i < targetIndex; i++) {
				Pose2d p = PositionMotion.getPose(mList.get(i));
				Pose2d next = PositionMotion.getPose(mList.get(i + 1));
				double angle = MathUtil.directionalAngle(p.direction, next.direction);
				rotSum += angle;
			}
			double[] control = actionData(actionType);
			control = MathUtil.concatenate(control, new double[] { rotSum });
			return control;
		}
		
		@Override
		public double[] getControlParameter(ArrayList<Pose2d> poseList) {
			double rotSum = 0;
			for (int i = 0; i < poseList.size()-1; i++) {
				Pose2d p = poseList.get(i);
				Pose2d next = poseList.get(i+1);
				double angle = MathUtil.directionalAngle(p.direction, next.direction);
				rotSum += angle;
			}
			double rotated = rotSum;
			double remain = targetRotation - rotated; 
			double[] control = actionData(actionType);
			control = MathUtil.concatenate(control, new double[] { remain });
			return control;
		}
		
		@Override
		public Object getControlParameterObject(Pose2d startPose, Pose2d currentPose) {
			Vector2d v = new Vector2d(Pose2d.BASE.direction);
			v = MathUtil.rotate(v, targetRotation);
			v = startPose.localToGlobal(v);
			return new Pose2d(currentPose.position, v);
		}
		
		@Override
		public boolean isGoalFinished(Pose2d startPose, ArrayList<Motion> motionList, ArrayList<Double> activationList) {
			if (motionList.size() < TIME_EXTENSION_MIN_TIME) return false;
			
			int vCountLimit = 4;
			for (int i = 0; i < vCountLimit; i++) {
				int idx = motionList.size() - vCountLimit + i;
				Pose2d p = PositionMotion.getPose(motionList.get(idx));
				double rotated = MathUtil.directionalAngle(startPose.direction, p.direction);
				boolean isValid = Math.abs(targetRotation - rotated) < Math.toRadians(10);
				if (!isValid) return false;
			}
			return true;
		}
		
		public String toString() {
			return "RotationSpeedGoal:" + Utils.toString(actionType, targetRotation, timeLimit);
		}
	}
	
	
	public static class RotationSpeedMotionMatching extends RotationMotionMatching{
		
		private int[] motionActionTypes;

		public RotationSpeedMotionMatching(MDatabase database) {
			super(database);
			
			motionActionTypes = new int[mList.length];
			for (int i = 0; i < mList.length; i++) {
				motionActionTypes[i] = -1;
			}
			for (MotionAnnotation ann : database.getEventAnnotations()) {
				int action = getActionType(ann.type);
				if (action < 0) continue;
				int start = database.findMotion(ann.file, ann.startFrame).motionIndex;
				int end = database.findMotion(ann.file, ann.endFrame).motionIndex;
				for (int i = start; i <= end; i++) {
					motionActionTypes[i] = action;
				}
			}
			
//			for (int i = 0; i < motionActionTypes.length; i++) {
//				if (/*isContainedMotion[i] && */motionActionTypes[i] >= 0) {
//					System.out.println("mmmtt : " + mList[i] + " : " + motionActionTypes[i] + " : " + isStraightList[i]);
//				}
//			}
//			System.exit(0);
		}
		
		private int getActionType(String type) {
			for (int i = 0; i < actionTypes.length; i++) {
				if (actionTypes[i].equals(type)) return i;
			}
			return -1;
		}

		@Override
		protected boolean isValidEnd(Motion motion, AgilityGoal _goal) {
			if (!isStraightList[motion.motionIndex]) return false;
			
			RotationSpeedGoal goal = (RotationSpeedGoal)_goal;
			if (goal.actionType >= 0) {
				if (motionActionTypes[motion.motionIndex] != goal.actionType) {
					return false;
				}
			}
			return true;
		}
		
		protected void logFinalPath(MatchingPath path, AgilityGoal _goal) {
			RotationSpeedGoal goal = (RotationSpeedGoal)_goal;
			int startType = motionActionTypes[path.motionList.get(0).motionIndex];
			int currentType = motionActionTypes[path.current.motionIndex];
			System.out.println("logFinalPath : " + goal + " : " + startType + "-> " + currentType);
			System.out.println(path.motionList.get(0) + " -> " + path.current);
		}
		
		@Override
		protected double getSpatialError(MatchingPath path, AgilityGoal goal, Motion transitionMotion, int timeOffset, boolean isSequential) {
			double targetRot = ((RotationSpeedGoal)goal).targetRotation - path.rotation;
			double rotOffset = 0;
			if (timeOffset > 0) {
				double[] rotCache = rotateCache[transitionMotion.motionIndex];
				int remainTime = timeOffset;
				if (remainTime >= rotCache.length ) {
					System.out.println(remainTime + " : " + rotCache.length);
				}
				if (Double.isNaN(rotCache[remainTime])) return Integer.MAX_VALUE;
				rotOffset = rotCache[remainTime];
				
				int finalIndex = transitionMotion.motionIndex + timeOffset;
				if (finalIndex >= mList.length || mList[finalIndex].motionData != transitionMotion.motionData) return Integer.MAX_VALUE;
				if (!isStraightList[finalIndex]) return Integer.MAX_VALUE;
			}
			double diff = Math.abs(targetRot - rotOffset);
			double d = diff*rotErrorRatio;
			return d*d;
		}

		@Override
		protected double getActionError(MatchingPath path, AgilityGoal _goal, Motion transitionMotion, int timeOffset, boolean isSequential) {
			if (transitionMotion == null) {
				transitionMotion = path.current;
			}
			
			RotationSpeedGoal goal = (RotationSpeedGoal)_goal;
			int currentType = motionActionTypes[path.current.motionIndex];
			int targetType = motionActionTypes[transitionMotion.motionIndex];
//			if (pickCount >= 23 && pickCount <=24) {
//				if (targetType == goal.action) {
//					log("aerror : " + Utils.toString(currentType, transitionMotion, motionActionTypes[transitionMotion.motionIndex]));
//				}
//			}
			if (currentType == goal.actionType) {
				if (targetType != goal.actionType) {
					return Integer.MAX_VALUE;
				}
			}
			if (timeOffset <= Configuration.MOTION_TRANSITON_MARGIN*3) {
				int finalIndex = transitionMotion.motionIndex + timeOffset;
				if (finalIndex >= mList.length || mList[finalIndex].motionData != transitionMotion.motionData) return Integer.MAX_VALUE;
				int finalType = motionActionTypes[finalIndex];
				if (finalType != goal.actionType) {
					return Integer.MAX_VALUE;
				}
			}
			
			return 0;
		}
	}
	
}
