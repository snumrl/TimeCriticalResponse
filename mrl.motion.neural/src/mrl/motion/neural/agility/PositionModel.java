package mrl.motion.neural.agility;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.RotationModel.RotationGoal;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class PositionModel extends AgilityModel {
	
	public static double MAX_TARGET_LENGTH = 150; 
	public static double BASE_VELOCITY_LENGTH = 200; 
	
	public static String[] actionTypes = {
			"idle",
	};
	
	private PositionGoal prevGoal;
	private Pose2d prevPose = new Pose2d(Pose2d.BASE);
	
	public PositionModel() {
		sampleIdleGoal();
	}
	
	@Override
	public PositionGoal sampleIdleGoal() {
		prevGoal =  new PositionGoal(new Point2d(), GOAL_TIME_LIMIT);
		return prevGoal;
	}
	
	@Override
	public PositionGoal sampleRandomGoal(Pose2d currentPose){
//		double velLength =  MAX_TARGET_LENGTH*MathUtil.random.nextDouble();
		
//		if (MathUtil.random.nextDouble() < 0.5) {
//		} else {
//			targetLength = targetLength*(1 + Utils.rand1()*0.25);
//		}
		
		Vector2d prevP = new Vector2d(prevGoal.target);
		prevP = currentPose.globalToLocal(prevPose.localToGlobal(prevP));
		
//		Vector2d v = new Vector2d(velLength, 0);
//		double rotation = MathUtil.random.nextDouble()*Math.PI*2;
//		prevP = MathUtil.rotate(v, rotation);
		while (true) {
			double velLength =  BASE_VELOCITY_LENGTH*MathUtil.random.nextDouble();
			Vector2d v = new Vector2d(velLength, 0);
			double rotation = MathUtil.random.nextDouble()*Math.PI*2;
			Vector2d vv = MathUtil.rotate(v, rotation);
			vv.add(prevP);
			double tLen = MathUtil.length(vv);
			if (tLen <= MAX_TARGET_LENGTH) {
				System.out.println("prev : " + Utils.toString(prevGoal.target, vv, MathUtil.rotate(v, rotation), prevP));
				prevP = vv;
				break;
			}
		}
		
		int timeLimit = MathUtil.round(GOAL_TIME_LIMIT);
		System.out.println("random goal :: " + prevP + " : " + timeLimit);
		prevGoal = new PositionGoal(new Point2d(prevP), timeLimit);
		prevPose = new Pose2d(currentPose);
		return prevGoal;
	}

	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		throw new RuntimeException();
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
	
	
	public class PositionGoal extends AgilityGoal{
		public Point2d target;
		public PositionGoal(Point2d target, int timeLimit) {
			super(0, timeLimit);
			this.target = target;
		}
		
		@Override
		public Pose2d getEditingConstraint() {
			return new Pose2d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
//			return new Pose2d(target.x, target.y, Double.NaN, Double.NaN);
		}
		
		@Override
		public boolean isGoalFinished(Pose2d startPose, ArrayList<Motion> motionList, ArrayList<Double> activationList) {
			Pose2d pose = PositionMotion.getPose(Utils.last(motionList));
			double d = startPose.globalToLocal(pose.position).distance(target);
			return d < 30;
		}
		@Override
		public double[] getControlParameter(ArrayList<Pose2d> poseList) {
			Point2d global = poseList.get(0).localToGlobal(target);
			Point2d p = Utils.last(poseList).globalToLocal(global);
			return new double[] { p.x, p.y };
		}
		@Override
		public Object getControlParameterObject(Pose2d startPose, Pose2d currentPose) {
			Point2d global = startPose.localToGlobal(target);
			return Pose2d.to3d(global);
		}
		@Override
		public double[] getControlParameter(ArrayList<Motion> mList, int currentIndex, int targetIndex) {
			Pose2d p = PositionMotion.getPose(mList.get(currentIndex));
			Pose2d next = PositionMotion.getPose(mList.get(targetIndex));
			Point2d target = p.globalToLocal(next.position);
			return new double[] { target.x, target.y };
		}
		
		public double getSpatialError(Pose2d currentMoved, double currentRotated, Pose2d futurePose, double futureRotation) {
			Point2d p = currentMoved.localToGlobal(futurePose.position);
			double d = p.distance(target);
			d = d * 5;
			return d*d;
		}
		
		public boolean isActiveAction() {
			return false;
		}
		
		public String toString() {
			return "PositionGoal:" + Utils.toString(actionType, timeLimit, target);
		}
	}

}
