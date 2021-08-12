package mrl.motion.neural.data;

import java.util.ArrayList;
import java.util.Arrays;

import mrl.motion.data.MDatabase;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;

public class ActionOnlyWaveControl extends ControlDataGenerator{
	
	public static boolean USE_GOAL_INTERPOLATION = true;
	public static boolean USE_ACTIVATION = false;
	public static boolean USE_TIMING_PARAMETER = true;
	public static boolean USE_RAW_TIMING = false;
	
	public static int MAX_PREV_MARGIN = 60;
	public static int POST_MARGIN = 10;
	public static int MAX_WAVE_LEN = 90;
	
	public static int WAVE_SIZE = 6;
	
	public static int ACTIVATION_RISE = 6;
	public static int ACTIVATION_PEAK = 1;
	
	private MDatabase database;
	private ArrayList<ActionTarget> targetList;
	private ActionTarget target;
	private int tIndex;
	private String[] actionTypes;

	public static double activation;
	
	public ActionOnlyWaveControl(String[] actionTypes, MDatabase database, ArrayList<ActionTarget> targetList) {
		this.actionTypes = actionTypes;
		this.database = database;
		this.targetList = targetList;
		tIndex = 0;
		target = targetList.get(tIndex);
		
		double dtSum = 0;
		for (int i = 0; i < targetList.size() - 1; i++) {
			if (i > 4){
				dtSum += targetList.get(i+1).mIndex - targetList.get(i).mIndex;
			}
		}
		System.out.println("mean interval :: " + dtSum/(targetList.size()-1));
	}
	
	protected String[] getActionTypes() {
		return actionTypes;
	}
	
	@Override
	public double[] getControl(int index) {
		if (index >= target.mIndex){
			tIndex++;
			if (tIndex >= targetList.size() - 3) return null;
			target = targetList.get(tIndex);
		}
		
		ActionParameter prevAction = null;
		if (tIndex > 0) {
			ActionTarget prevT = targetList.get(tIndex - 1);
			prevAction = new ActionParameter(prevT.ann.type, prevT.getInteractionPose(mList, database), prevT.mIndex);
		}
		ActionParameter targetAction = new ActionParameter(target.ann.type, target.getInteractionPose(mList, database), target.mIndex);
		if (target.mIndex > index + MAX_PREV_MARGIN) {
			targetAction.actionTime = index + MAX_PREV_MARGIN;
			targetAction.actionPose = PositionMotion.getPose(mList.get(targetAction.actionTime));
		}
		Pose2d p = PositionMotion.getPose(mList.get(index));
		double[] control = getControlParameter(prevAction, targetAction, index, p, actionTypes);
		return control;
	}
	
	public static double[] getControlParameter(ActionParameter prevAction, 
												ActionParameter targetAction, 
												double currentTime, Pose2d currentPose, 
												String[] actions) {
		double remainTime = targetAction.actionTime - currentTime;
		
		double prevTimingRatio = 0;
		double passedTime = 999999;
		if (prevAction != null) {
			passedTime = currentTime - prevAction.actionTime;
			if (passedTime < POST_MARGIN) {
				prevTimingRatio = 1 - passedTime/(double)POST_MARGIN;
			}
		}
		
		String type = targetAction.actionType;
		
		Pose2d p = currentPose;
		double[] goalInfo;
		Pose2d targetPose  = Pose2d.relativePose(p, targetAction.actionPose);
		goalInfo = new double[] { targetPose.position.x, targetPose.position.y };
		if (ActionOnlyControl.USE_DIRECTION_CONTROL) {
			goalInfo = targetPose.toArray();
		}
		
		double[] tCurrent = getWave(remainTime);
		double[] tPrev = getWave(-passedTime);
		tCurrent = MathUtil.interpolate(tCurrent, tPrev, prevTimingRatio);
		if (USE_GOAL_INTERPOLATION && prevTimingRatio > 0) {
			Pose2d prevPose = Pose2d.relativePose(p, prevAction.actionPose);
			double[] prevGoalInfo =  new double[] { prevPose.position.x, prevPose.position.y };
			if (ActionOnlyControl.USE_DIRECTION_CONTROL) {
				goalInfo = prevPose.toArray();
			}
			goalInfo = MathUtil.interpolate(goalInfo, prevGoalInfo, prevTimingRatio);
		}
		
		int activMargin = ACTIVATION_PEAK + ACTIVATION_RISE;
		activation = 0;
		if (remainTime <= activMargin) {
			if (remainTime <= ACTIVATION_PEAK) {
				activation = 1;
			} else {
				activation = 1 - (remainTime - ACTIVATION_PEAK)/(double)(ACTIVATION_RISE + 1);
			}
		} else if (passedTime <= activMargin) {
			if (passedTime <= ACTIVATION_PEAK) {
				activation = 1;
			} else {
				activation = 1 - (passedTime - ACTIVATION_PEAK)/(double)(ACTIVATION_RISE + 1);
			}
		}
		
		double[] control = getActionType(type, actions);
		if (USE_RAW_TIMING) {
			control = MathUtil.concatenate(control, new double[] { remainTime });
		} else if (USE_TIMING_PARAMETER) {
			control = MathUtil.concatenate(control, tCurrent);
		}
		control = MathUtil.concatenate(control, goalInfo);
		return control;
	}
	
	public static double[] getWave(double time) {
		return getWave(time, WAVE_SIZE, MAX_WAVE_LEN);
//		double[] timingData = new double[WAVE_SIZE * 2];
//		for (int i = 0; i < WAVE_SIZE; i++) {
//			double t = time/Math.pow(MAX_WAVE_LEN/Math.PI, i/(double)(WAVE_SIZE-1));
//			timingData[2*i] = Math.sin(t);
//			timingData[2*i+1] = Math.cos(t);
//		}
//		return timingData;
	}
	public static double[] getWave(double time, int waveSize, int maxWaveLen) {
		double[] timingData = new double[waveSize * 2];
		for (int i = 0; i < waveSize; i++) {
			double t = time/Math.pow(maxWaveLen/Math.PI, i/(double)(waveSize-1));
			timingData[2*i] = Math.sin(t);
			timingData[2*i+1] = Math.cos(t);
		}
		return timingData;
	}
	
	public static double[] getActionType(String action, String[] types){
		double[] data = new double[types.length];
		if (action == null) return data;
		
		boolean isExist = false;
		for (int i = 0; i < data.length; i++) {
			if (action.equals(types[i])){
				data[i] = 1;
				isExist = true;
			}
		}
		if (!isExist){
			System.out.println("no action :: " + action + " : " + Arrays.toString(types));
			throw new RuntimeException();
		}
		return data;
	}
	
	public double[] getActionType(String action){
		return getActionType(action, getActionTypes());
	}
	
	
	public boolean[] getNormalMarking(){
		// action=4,5, overTime, activation
		int size = getActionTypes().length;
		if (USE_TIMING_PARAMETER) {
			size += WAVE_SIZE * 2;
		}
		return getTrueList(size);
	}
	
	public double[] getHasBall(int index){
		if (!USE_ACTIVATION) return null;
		return new double[] { activation };
	}
	
	public static class ActionParameter{
		public String actionType = null;
		public Pose2d actionPose;
		public int actionTime = -9999;
		public ActionParameter(String actionType, Pose2d actionPose, int actionTime) {
			this.actionType = actionType;
			this.actionPose = actionPose;
			this.actionTime = actionTime;
		}
		
		public ActionParameter(ActionParameter copy) {
			this.actionType = copy.actionType;
			this.actionPose = copy.actionPose;
			this.actionTime = copy.actionTime;
		}
	}
	
}
