package mrl.motion.neural.data;

import java.util.ArrayList;
import java.util.Arrays;

import mrl.motion.data.MDatabase;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;

public class ActionControl extends ControlDataGenerator{
	
	public static int ACTION_MARGIN = 60;
	public static int CONTROL_MARGIN = 30;
	public static int POST_MARGIN = 20;
	
	public static boolean useDoubleAction = false;
	
	private MDatabase database;
	private ArrayList<ActionTarget> targetList;
	private ActionTarget target;
	private int tIndex;
	private String[] actionTypes;
	
	public ActionControl(String[] actionTypes, MDatabase database, ArrayList<ActionTarget> targetList) {
		this.actionTypes = actionTypes;
		this.database = database;
		this.targetList = targetList;
		tIndex = 0;
		target = targetList.get(tIndex);
		
		for (int i = 0; i < targetList.size(); i++) {
			ActionTarget t1 = targetList.get(i);
			if (i <= 0) {
				t1.prevMargin = ACTION_MARGIN;
			} else {
				ActionTarget t0 = targetList.get(i-1);
				t1.prevMargin = Math.min((t1.mIndex - (t0.mIndex + t0.postMargin)), ACTION_MARGIN);
			}
			if (i >= targetList.size()-1) {
				t1.postMargin = POST_MARGIN;
			} else {
				ActionTarget t2 = targetList.get(i+1);
				t1.postMargin = Math.min((t2.mIndex - t1.mIndex)/2, POST_MARGIN);
			}
		}
		
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
		if (index >= target.mIndex + target.postMargin){
			tIndex++;
			if (tIndex >= targetList.size() - 2) return null;
			target = targetList.get(tIndex);
		}
		
		int remainTime = target.mIndex - index;
		String type = target.ann.type;
		boolean isOverTime = remainTime >= ACTION_MARGIN;
		
		double activation;
		
		
		Pose2d p = PositionMotion.getPose(mList.get(index));
		double[] goalInfo;
		if (isOverTime){
			isOverTime = true;
			activation = 0;
			Pose2d targetPose = PositionMotion.getPose(mList.get(index + CONTROL_MARGIN));
			targetPose = Pose2d.relativePose(p, targetPose);
			goalInfo = targetPose.toArray();
			// position only
			goalInfo[2] = Double.NaN;
			goalInfo[3] = Double.NaN;
		} else {
			activation = Math.max(0, 1 - Math.abs(remainTime)/(double)ACTION_MARGIN);
			isOverTime = false;
			Pose2d targetPose  = Pose2d.relativePose(p, target.getInteractionPose(mList, database));
			goalInfo = targetPose.toArray();
		}
		double[] action = getActionType(isOverTime ? null : type);
		double[] control = action;
		control = MathUtil.concatenate(control, new double[]{ isOverTime ? 1 : 0, activation });
		control = MathUtil.concatenate(control, goalInfo);
		
		return control;
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
		return getTrueList(size + 2);
	}
}
