package mrl.motion.neural.agility.measure;

import java.util.ArrayList;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.AgilityControlParameterGenerator;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.predict.MotionPredictor;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class AgilityMeasure {

	public AgilityModel model;
	public MotionPredictor predictor;
	
	public TimeBasedList<Object> goalList = new TimeBasedList<Object>();
	public ArrayList<Motion> motionList = new ArrayList<Motion>();
	
	
	public AgilityMeasure(AgilityModel model, MotionPredictor predictor) {
		this.model = model;
		this.predictor = predictor;
	}

	public int checkAgility(AgilityGoal goal, int maxTime) {
		return checkAgility2(goal, maxTime).completionTime;
	}
	public AgilityMeasureResult checkAgility2(AgilityGoal goal, int maxTime) {
		Pose2d startPose = predictor.currentPose();
		int t = 0;
		ArrayList<Motion> mList = new ArrayList<Motion>();
		ArrayList<Double> activationList = new ArrayList<Double>();
		int startFrame = motionList.size();
		ArrayList<Pose2d> poseList = new ArrayList<Pose2d>();
		poseList.add(startPose);
		for (; t < maxTime; t++) {
			double[] controlParameter = goal.getControlParameter(poseList);
			if (AgilityControlParameterGenerator.ADD_TIMING_PARAMETER) {
				controlParameter = MathUtil.concatenate(controlParameter, new double[] { goal.agility });
			}
			Motion motion = predictor.predictMotion(controlParameter);
			mList.add(motion);
			motionList.add(motion);
			poseList.add(predictor.currentPose());
			goalList.add(goal.getControlParameterObject(startPose, predictor.currentPose()));
			if (model.useActivation()) {
				activationList.add(predictor.currentActivation());
			}
			
			if (mList.size() < AgilityModel.TIME_EXTENSION_MIN_TIME) continue;
			if (goal.isGoalFinished(startPose, mList, activationList)) {
				break;
			}
		}
		
		double rotated = 0;
		for (int i = 0; i < poseList.size()-1; i++) {
			Pose2d p = poseList.get(i);
			Pose2d next = poseList.get(i+1);
			double angle = MathUtil.directionalAngle(p.direction, next.direction);
			rotated += angle;
		}
		double spatialError = goal.getSpatialError(startPose.globalToLocal(Utils.last(poseList)), rotated, Pose2d.BASE, 0);
		return new AgilityMeasureResult(goal, t, startFrame, motionList.size(), spatialError);
	}
	
	public static class AgilityMeasureResult{
		public AgilityGoal prevGoal;
		public AgilityGoal goal;
		public int completionTime;
		public int frameStart;
		public int frameEnd;
		public double spatialError;
		
		public AgilityMeasureResult(AgilityGoal goal, int completionTime, int frameStart, int frameEnd, double spatialError) {
			super();
			this.goal = goal;
			this.completionTime = completionTime;
			this.frameStart = frameStart;
			this.frameEnd = frameEnd;
			this.spatialError = spatialError;
		}
	}
	
}
