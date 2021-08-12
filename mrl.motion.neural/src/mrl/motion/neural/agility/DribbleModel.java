package mrl.motion.neural.agility;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MGraphGoalSearch;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class DribbleModel extends AgilityModel {

	public static boolean USE_STRAIGHT_SAMPLING = false;
	public static double ROTAION_ANGLE_MARGIN = 0.0;
	public static double ROTATION_ERROR_WEIGHT = 200;
	
	public static String[] actionTypes = {
			"stop",
			"dribble",
			"back",
	};
	
	private boolean sampleStraight = false;
	private int actionCount = 0;
	private DribbleGoal lastGoal;
	
	public DribbleModel() {
		sampleIdleGoal();
	}
	
	public String[] getActionTypes() {
		return actionTypes;
	}
	public String[] getFullActionTypes() {
		return actionTypes;
	}
	
	public int getContinuousLabelSize() {
		return actionTypes.length;
	}
	
	public boolean useActivation() {
		return false;
	}
	
	public static double sampleRotation() {
		return (Math.PI + ROTAION_ANGLE_MARGIN) * Utils.rand1();
	}
	
	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		return new RotationMotionMatching(database);
	}
	
	@Override
	public AgilityGoal sampleIdleGoal() {
		lastGoal = new DribbleGoal(0, 0, GOAL_TIME_LIMIT);
		return lastGoal;
	}
	
	@Override
	public AgilityGoal sampleRandomGoal(Pose2d currentPose) {
		double rotation = sampleRotation();
		if (USE_STRAIGHT_SAMPLING) {
			if (sampleStraight) {
				rotation /= 20;
			}
//			sampleStraight = !sampleStraight;
			if ((Math.abs(rotation) < Math.PI / 4) || (MathUtil.random.nextDouble() < 0.5)) {
				sampleStraight = false;
			} else {
				sampleStraight = true;
			}
		}
		
		actionCount++;
		int stopIndex = 0;
		int action;
		if (actionCount > 12) {
//			rotation = RotationModel.sampleRotation();
			if (lastGoal.actionType == stopIndex) {
				action = sampleAction();
				rotation /= 3;
			} else {
				if (MathUtil.random.nextDouble() < 0.2) {
					action = stopIndex;
					rotation /= 60;
				} else {
					action = sampleAction();
					rotation /= 20;
				}
			}
			actionCount = 0;
		} else {
			action = lastGoal.actionType;
			if (lastGoal.actionType == stopIndex) {
				actionCount += 2;
				rotation /= 60;
			} else if (lastGoal.actionType == 2) {
				rotation /= 3;
			}
		}
		
		int timeLimit = MathUtil.round(GOAL_TIME_LIMIT);
//		System.out.println("random goal :: " + Math.toDegrees(rotation));
		lastGoal = new DribbleGoal(action, rotation, timeLimit);
		return lastGoal;
	}
	
	private int sampleAction() {
		int idx = MathUtil.random.nextInt(3);
		if (idx == 0) return 2;
		return 1;
	}
	
	public class DribbleGoal extends AgilityGoal{
		
		public double targetRotation;
		
		public DribbleGoal(int actionType, double targetRotation, int timeLimit) {
			super(actionType, timeLimit);
			this.targetRotation = targetRotation;
		}
		
		@Override
		public Pose2d getEditingConstraint() {
			if (actionType == 0) {
				return new Pose2d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
			}
			Vector2d v = new Vector2d(Pose2d.BASE.direction);
			v = MathUtil.rotate(v, adjustedTarget());
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
			return new double[] { remain };
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
			if (motionList.size() < AgilityModel.TIME_EXTENSION_MIN_TIME) return false;
			
			Vector2d target = MathUtil.rotate(Pose2d.BASE.direction, adjustedTarget());
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
		
		public String toString() {
			return "DribbleGoal:" + targetRotation + ", " + timeLimit;
		}
		
		@Override
		public boolean isActiveAction() {
			return false;
		}
		
		private double adjustedTarget() {
			double target = targetRotation;
			if (actionType == actionTypes.length-1) {
				target *= -1;
			}
			return target;
		}
		
		public double getSpatialError(Pose2d currentMoved, double currentRotated, Pose2d futurePose, double futureRotation) {
			double tr = adjustedTarget() - currentRotated;
			double d = tr - futureRotation;
			d = d * ROTATION_ERROR_WEIGHT;
			return d*d;
		}
	}
	
}
