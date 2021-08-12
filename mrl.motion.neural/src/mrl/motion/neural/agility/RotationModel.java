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

public class RotationModel extends AgilityModel {

	public static boolean USE_STRAIGHT_SAMPLING = false;
	public static double ROTAION_ANGLE_MARGIN = 0.0;
	public static double ROTATION_ERROR_WEIGHT = 200;
	
	private boolean sampleStraight = false;
	public static String[] actionTypes = {
			"jog"
	};
	
	public RotationModel() {
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
		return new RotationGoal(0, GOAL_TIME_LIMIT);
	}
	
	@Override
	public AgilityGoal sampleRandomGoal(Pose2d currentPose) {
		double rotation = sampleRotation();
		if (USE_STRAIGHT_SAMPLING) {
			if (sampleStraight) {
				rotation /= 20;
			}
			sampleStraight = !sampleStraight;
			if (Math.abs(rotation) < Math.PI / 4) {
				sampleStraight = false;
			}
		}
		
		int timeLimit = MathUtil.round(GOAL_TIME_LIMIT);
//		System.out.println("random goal :: " + Math.toDegrees(rotation));
		return new RotationGoal(rotation, timeLimit);
	}

	public static boolean isStraight(List<Motion> motionList, double degreeLimit) {
		ArrayList<Pose2d> poseList = new ArrayList<Pose2d>();
		for (Motion m : motionList) {
			poseList.add(PositionMotion.getPose(m));
		}
		
		double rotSum1 = 0;
		double rotSum2 = 0;
		int count = 0;
		
		double rotLimit = Math.toRadians(degreeLimit);
		for (int i = 1; i < poseList.size(); i++) {
			Pose2d p = poseList.get(0).globalToLocal(poseList.get(i));
			double angle1 = Math.abs(MathUtil.directionalAngle(Pose2d.BASE.direction, p.position));
			double angle2 = Math.abs(MathUtil.directionalAngle(Pose2d.BASE.direction, p.direction));
			rotSum1 += angle1*angle1;
			rotSum2 += angle2*angle2;
			count++;
//			if (angle1 > rotLimit || angle2 > rotLimit) return false;
		}
		rotSum1 /= count;
		rotSum2 /= count;
		rotLimit = rotLimit*rotLimit;
		if (rotSum1 > rotLimit || rotSum2 > rotLimit) return false;
		return true;
	}
	
//	public static double measureError(RotationGoal goal, int time, double rotation, ArrayList<Double> trasitionError) {
//		double transError = Integer.MAX_VALUE;
//		for (Double d : trasitionError) {
//			transError = Math.min(d, transError);
//		}
//		
//		double rDiff = Math.toDegrees(rotation - goal.targetRotation);
//		double tDiff = Math.max(0, time - goal.timeLimit);
//		
//		tDiff *= TIMING_ERROR_OFFSET/10d;
//		transError *= TIMING_ERROR_OFFSET/200d; // 90 degree == 200
//		
//		return rDiff*rDiff + tDiff*tDiff + transError*transError;
//	}
	
	public class RotationGoal extends AgilityGoal{
		
		public double targetRotation;
		
		public RotationGoal(double targetRotation, int timeLimit) {
			super(0, timeLimit);
			this.targetRotation = targetRotation;
		}
		
		@Override
		public Pose2d getEditingConstraint() {
			Vector2d v = new Vector2d(Pose2d.BASE.direction);
			v = MathUtil.rotate(v, targetRotation);
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
			return new double[] { rotSum };
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
		
		public String toString() {
			return "RotationGoal:" + targetRotation + ", " + timeLimit;
		}
		
		@Override
		public boolean isActiveAction() {
			return false;
		}
		
		public double getSpatialError(Pose2d currentMoved, double currentRotated, Pose2d futurePose, double futureRotation) {
			double tr = targetRotation - currentRotated;
			double d = tr - futureRotation;
			d = d * ROTATION_ERROR_WEIGHT;
			return d*d;
		}
	}
	
}
