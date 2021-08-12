package mrl.motion.neural.agility;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.ActionDistDP;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.position.PositionMotion;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class JumpModel extends AgilityModel {
	
	public static double rotErrorRatio = 0.3;
	public static double LOCOMOTION_RATIO = 0.8;
	public static boolean FIX_LOCOMOTION_AFTER_ACTION = true;
	
	public static String[] actionTypes = {
			"jog",
			"jump_both",
			"jump_one",
			"jump_moving",
			"flip"
	};
	
	public static double[][] timeOffset = {
			{ 6, 6, 20, 20 }, // jog
			{ 17.000, 10.000, 218.649, 128.207 }, // jump_both
			{ 15.000, 14.000, 160.925, 96.277 }, // jump_one
			{ 12.500, 14.500, 161.906, 161.909 }, // jump_moving
			{ 21.500, 10.000, 211.103, 55.133 }, // flip
	};
	
	public static double NOISE_LENGTH = 50;
	
	public static boolean USE_STRAIGHT_SAMPLING = false;
	
	private boolean sampleStraight = false;
	private JumpGoal lastGoal;
	private Pose2d prevPose = new Pose2d(Pose2d.BASE);
	
	public JumpModel() {
		TransitionData.STRAIGHT_MARGIN = 3;
		lastGoal = sampleIdleGoal();
	}
	
	public String[] getActionTypes() {
		return actionTypes;
	}
	public String[] getFullActionTypes() {
		return actionTypes;
	}
	public int getContinuousLabelSize() {
		return 1;
	}
	public boolean useActivation() {
		return true;
	}
	
	
	@Override
	public JumpGoal sampleIdleGoal() {
		lastGoal = new JumpGoal(0, new Point2d(), 0, GOAL_TIME_LIMIT);
		return lastGoal;
	}
	
	@Override
	public JumpGoal sampleRandomGoal(Pose2d currentPose) {
		int actionType;
		double rotation;
		Point2d point = new Point2d();
		double lRatio = LOCOMOTION_RATIO;
		if (lastGoal.actionType > 0) lRatio = 0.5;
		if ((FIX_LOCOMOTION_AFTER_ACTION && (lastGoal.actionType > 0)) || MathUtil.random.nextDouble() < lRatio) {
			actionType = 0;
			rotation = RotationModel.sampleRotation();
//			if (MathUtil.random.nextDouble() < 0.33) {
//				rotation /= 20;
//			}
			if (lastGoal.actionType > 0) {
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
			point = new Point2d(Pose2d.BASE.direction);
			if (lastGoal.actionType > 0) {
				point.set(currentPose.globalToLocal(prevPose.direction));
				currentPose = prevPose;
			}
			actionType = MathUtil.random.nextInt(actionTypes.length - 1) + 1;
			Vector2d delta = MathUtil.rotate(point, Math.PI/2);
			delta.scale(20*Utils.rand1());
			double targetLen = timeOffset[actionType][2] + Utils.rand1()*NOISE_LENGTH + 10;
			targetLen += timeOffset[lastGoal.actionType][3] *(0.5 + 0.5*MathUtil.random.nextDouble());
			point.scale(targetLen);
			point.add(delta);
			rotation = 0;
//			sampleStraight = true;
		}
		count++;
		double timeLength = 0;
		timeLength += 8; // base margin
		timeLength += timeOffset[lastGoal.actionType][1];
		timeLength += timeOffset[actionType][0];
		int adjustedTime = MathUtil.round(timeLength/20d*GOAL_TIME_LIMIT);
		lastGoal = new JumpGoal(rotation, point, actionType, adjustedTime);
		
		System.out.println("random goal :: " + lastGoal);
		prevPose = currentPose;
		return lastGoal;
	}
	int count = 0;

	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		throw new RuntimeException();
	}
	
	public class JumpGoal extends AgilityGoal{
		public double targetRotation;
		public Point2d targetPosition;
		
		
		public JumpGoal(double targetRotation, Point2d targetPosition, int actionType, int timeLimit) {
			super(actionType, timeLimit);
			this.targetRotation = targetRotation;
			this.targetPosition = targetPosition;
		}
		@Override
		public Pose2d getEditingConstraint() {
			if (actionType == 0) {
				Vector2d v = new Vector2d(Pose2d.BASE.direction);
				v = MathUtil.rotate(v, targetRotation);
				Pose2d p = new Pose2d(new Point2d(Double.NaN, Double.NaN), v);
				return p;
			} else {
				return new Pose2d(targetPosition.x, targetPosition.y, Double.NaN, Double.NaN);
			}
		}
		
		
		@Override
		public boolean isGoalFinished(Pose2d startPose, ArrayList<Motion> motionList, ArrayList<Double> activationList) {
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
			double remain = Double.NaN;
			Point2d position = new Point2d(Double.NaN, Double.NaN);
			if (actionType == 0) {
				double rotSum = 0;
				for (int i = 0; i < poseList.size()-1; i++) {
					Pose2d p = poseList.get(i);
					Pose2d next = poseList.get(i+1);
					double angle = MathUtil.directionalAngle(p.direction, next.direction);
					rotSum += angle;
				}
				double rotated = rotSum;
				remain = targetRotation - rotated; 
			} else {
				Pose2d p = poseList.get(0);
				Pose2d current = Utils.last(poseList);
				position = current.globalToLocal(p.localToGlobal(targetPosition));
			}
			double[] control = actionData(actionType);
			control = MathUtil.concatenate(control, new double[] { remain, position.x, position.y });
			return control;
		}
		@Override
		public Object getControlParameterObject(Pose2d startPose, Pose2d currentPose) {
			if (actionType == 0) {
				Vector2d v = new Vector2d(Pose2d.BASE.direction);
				v = MathUtil.rotate(v, targetRotation);
				v = startPose.localToGlobal(v);
				return new Pose2d(currentPose.position, v);
			} else {
				return Pose2d.to3d(startPose.localToGlobal(targetPosition));
			}
		}
		
		@Override
		public double[] getControlParameter(ArrayList<Motion> mList, int currentIndex, int targetIndex) {
			double rotSum = Double.NaN;
			Point2d position = new Point2d(Double.NaN, Double.NaN);
			if (actionType == 0) {
				rotSum = 0;
				for (int i = currentIndex; i < targetIndex; i++) {
					Pose2d p = PositionMotion.getPose(mList.get(i));
					Pose2d next = PositionMotion.getPose(mList.get(i + 1));
					double angle = MathUtil.directionalAngle(p.direction, next.direction);
					rotSum += angle;
				}
			} else {
				Pose2d p = PositionMotion.getPose(mList.get(currentIndex));
				Pose2d next = PositionMotion.getPose(mList.get(targetIndex));
				position = p.globalToLocal(next.position);
			}
			double[] control = actionData(actionType);
			control = MathUtil.concatenate(control, new double[] { rotSum, position.x, position.y });
			return control;
		}
		
		@Override
		public boolean isActiveAction() {
			return actionType > 0;
		}
		
		public double getSpatialError(Pose2d currentMoved, double currentRotated, Pose2d futurePose, double futureRotation) {
			if (actionType == 0) {
				double tr = targetRotation - currentRotated;
				double d = tr - futureRotation;
				d = d * 200;
				return d*d;
			} else {
				Point2d tp = currentMoved.globalToLocal(targetPosition);
				double d = tp.distance(futurePose.position);
				d = d * 5;
				return d*d;
			}
		}
		
		public String toString() {
			return "JumpGoal:" + Utils.toString(targetRotation, actionType, timeLimit);
		}
	}
	

}
