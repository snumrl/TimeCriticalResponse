package mrl.motion.neural.agility;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class NCLocoModel extends AgilityModel {

	public static double TIMING_ERROR_OFFSET = 90; // 10 frame == 90 degree
	public static boolean USE_STRAIGHT_SAMPLING = true;
	
	private boolean sampleStraight = false;
	private int lastAction = 0;
	private int actionCount = 0;
	
	public static String[] actionTypes = {
			"walk",
			"stop",
	};
	
	public NCLocoModel() {
	}
	
	public int getActionSize() {
		return actionTypes.length;
	}
	
	@Override
	public NCLocoGoal sampleIdleGoal() {
		return new NCLocoGoal(0, lastAction, GOAL_TIME_LIMIT);
	}
	
	@Override
	public NCLocoGoal sampleRandomGoal(Pose2d currentPose) {
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
		return new NCLocoGoal(rotation, lastAction, timeLimit);
	}
	
	public class NCLocoGoal extends AgilityGoal{

		public double targetRotation;
		
		public NCLocoGoal(double targetRotation, int actionType, int timeLimit) {
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
		
		public Pose2d getEditingConstraint(Motion first, Motion last) {
			throw new RuntimeException();
		}

		@Override
		public boolean isGoalFinished(Pose2d startPose, ArrayList<Motion> motionList,
				ArrayList<Double> activationList) {
			return false;
		}

		@Override
		public double[] getControlParameter(ArrayList<Pose2d> poseList) {
			// for measure
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
		public Object getControlParameterObject(Pose2d startPose, Pose2d currentPose) {
			Vector2d v = new Vector2d(Pose2d.BASE.direction);
			v = MathUtil.rotate(v, targetRotation);
			v = startPose.localToGlobal(v);
			return new Pose2d(currentPose.position, v);
		}
		
		public String toString() {
			return "NCLocoGoal:" + Utils.toString(actionType, targetRotation, timeLimit);
		}
		
		@Override
		public double getSpatialError(Pose2d currentMoved, double currentRotated, Pose2d futurePose, double futureRotation) {
			
			return 0;
		}
	}

	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		throw new RuntimeException();
	}

}
