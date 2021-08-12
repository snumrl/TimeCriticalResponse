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

public class JumpSpeedModel extends AgilityModel {
	
	public static double LOCOMOTION_RATIO = 0.8;
	public static boolean FIX_LOCOMOTION_AFTER_ACTION = true;
	
	public static int LOCO_ACTION_SIZE = 4;
	public static String[] actionTypes = {
			"stop",
			"walk",
			"jog",
			"run",
			"jump_both",
			"jump_one",
			"jump_moving",
			"flip"
	};
	
	public static double[][] timeOffset = {
			{ 8, 8, -1 }, // stop
			{ 6, 6, -1 }, // walk
			{ 6, 6, -1 }, // jog
			{ 8, 8, -1 }, // run
			{ 17.000, 10.000, 219.266 }, // jump_both
			{ 15.000, 14.000, 159.721 }, // jump_one
			{ 12.500, 14.500, 160.327 }, // jump_moving
			{ 21.500, 10.000, 213.017 }, // flip
	};
	
	public static double NOISE_LENGTH = 90;
	
	public static boolean USE_STRAIGHT_SAMPLING = false;
	
	private boolean sampleStraight = false;
	private JumpSpeedGoal lastGoal;
	private Pose2d prevPose = new Pose2d(Pose2d.BASE);

	private int locoCount = 0;
	private int locoType = 0;
	
	public JumpSpeedModel() {
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
		return 4;
	}
	public boolean useActivation() {
		return true;
	}
	
	
	@Override
	public JumpSpeedGoal sampleIdleGoal() {
		lastGoal = new JumpSpeedGoal(0, new Point2d(), 0, GOAL_TIME_LIMIT);
		return lastGoal;
	}
	
	private boolean isActive(int action) {
		return action >= LOCO_ACTION_SIZE;
	}
	
	@Override
	public JumpSpeedGoal sampleRandomGoal(Pose2d currentPose) {
		int actionType;
		double rotation;
		Point2d point = new Point2d();
		double lRatio = LOCOMOTION_RATIO;
		
		boolean isLastActive = isActive(lastGoal.actionType);
		if (isLastActive) {
			lRatio = 0.5;
		}
		
		if ((FIX_LOCOMOTION_AFTER_ACTION && (isLastActive)) || MathUtil.random.nextDouble() < lRatio) {
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
			point = new Point2d(Pose2d.BASE.direction);
			if (isLastActive) {
				point.set(currentPose.globalToLocal(prevPose.direction));
				currentPose = prevPose;
			}
			actionType = MathUtil.random.nextInt(actionTypes.length - LOCO_ACTION_SIZE) + LOCO_ACTION_SIZE;
			Vector2d delta = MathUtil.rotate(point, Math.PI/2);
			delta.scale(20*Utils.rand1());
			double lengthOffset = 10;
			if (actionType == 2) {
				lengthOffset = 40;
			}
			if (actionType == 3) {
				lengthOffset = 100;
			}
			point.scale(timeOffset[actionType][2] + Utils.rand1()*NOISE_LENGTH + MathUtil.random.nextDouble()*lengthOffset);
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
		
		lastGoal = new JumpSpeedGoal(rotation, point, actionType, adjustedTime);
		
		System.out.println("random goal :: " + locoCount + " : " + locoType + " : " + lastGoal);
		prevPose = currentPose;
		return lastGoal;
	}
	int count = 0;

	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		throw new RuntimeException();
	}
	
	public class JumpSpeedGoal extends AgilityGoal{
		public double targetRotation;
		public Point2d targetPosition;
		
		
		public JumpSpeedGoal(double targetRotation, Point2d targetPosition, int actionType, int timeLimit) {
			super(actionType, timeLimit);
			this.targetRotation = targetRotation;
			this.targetPosition = targetPosition;
		}
		@Override
		public Pose2d getEditingConstraint() {
			if (isActive(actionType)) {
				return new Pose2d(targetPosition.x, targetPosition.y, Double.NaN, Double.NaN);
			} else {
				Vector2d v = new Vector2d(Pose2d.BASE.direction);
				v = MathUtil.rotate(v, targetRotation);
				Pose2d p = new Pose2d(new Point2d(Double.NaN, Double.NaN), v);
				return p;
			}
		}
		
		
		@Override
		public boolean isGoalFinished(Pose2d startPose, ArrayList<Motion> motionList, ArrayList<Double> activationList) {
//			Pose2d pose = PositionMotion.getPose(Utils.last(motionList));
//			double d = startPose.globalToLocal(pose.position).distance(target);
//			return d < 30;
			throw new RuntimeException();
		}
		@Override
		public double[] getControlParameter(ArrayList<Pose2d> poseList) {
//			Point2d global = startPose.localToGlobal(target);
//			Point2d p = currentPose.globalToLocal(global);
//			return new double[] { p.x, p.y };
			throw new RuntimeException();
		}
		@Override
		public Object getControlParameterObject(Pose2d startPose, Pose2d currentPose) {
			if (isActive(actionType)) {
				return Pose2d.to3d(startPose.localToGlobal(targetPosition));
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
			Point2d position = new Point2d(Double.NaN, Double.NaN);
			if (!isActive(actionType)) {
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
			return isActive(actionType);
		}
		
		public double getSpatialError(Pose2d currentMoved, double currentRotated, Pose2d futurePose, double futureRotation) {
			if (!isActive(actionType)) {
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
