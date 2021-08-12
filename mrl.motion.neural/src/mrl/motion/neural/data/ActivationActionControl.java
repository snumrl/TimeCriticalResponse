package mrl.motion.neural.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;

public class ActivationActionControl extends ControlDataGenerator{
	
	public static int ACTIVATION_MARGIN = 10;
	
	public static int PREV_MARGIN = 30;
	public static int CONTROL_MARGIN = 30;
	
	public static boolean useDoubleAction = false;
	
	private MDatabase database;
	private ArrayList<ActionTarget> targetList;
	private ActionTarget target;
	private int tIndex;
	private String[] actionTypes;
	
	public ActivationActionControl(String[] actionTypes, MDatabase database, ArrayList<ActionTarget> targetList) {
		this.actionTypes = actionTypes;
		this.database = database;
		this.targetList = targetList;
		tIndex = 0;
		target = targetList.get(tIndex);
		
		for (int i = 0; i < targetList.size(); i++) {
			ActionTarget t1 = targetList.get(i);
			int prevMargin = PREV_MARGIN;
//			int prevMargin = PREV_MARGIN/2 + MathUtil.random.nextInt(PREV_MARGIN);
			if (i <= 0) {
				t1.prevMargin = prevMargin;
			} else {
				ActionTarget t0 = targetList.get(i-1);
				t1.prevMargin = Math.min((t1.mIndex - (t0.mIndex + t0.postMargin)), prevMargin);
			}
			if (i >= targetList.size()-1) {
				t1.postMargin = ACTIVATION_MARGIN;
			} else {
				ActionTarget t2 = targetList.get(i+1);
				t1.postMargin = Math.min((t2.mIndex - t1.mIndex)/2, ACTIVATION_MARGIN);
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
		boolean isOverTime = remainTime >= target.prevMargin;
		
		double activation;
		double[] acti = new double[] { 0, 0, 0 };
		Pose2d p = PositionMotion.getPose(mList.get(index));
		double[] goalInfo;
		if (isOverTime){
			isOverTime = true;
			activation = 0;
			Pose2d targetPose = PositionMotion.getPose(mList.get(index + CONTROL_MARGIN));
			targetPose = Pose2d.relativePose(p, targetPose);
			goalInfo = targetPose.toArray();
			// direction only
			goalInfo[0] = Double.NaN;
			goalInfo[1] = Double.NaN;
//			// position only
//			goalInfo[2] = Double.NaN;
//			goalInfo[3] = Double.NaN;
		} else {
			activation = getActivation(remainTime);
			acti = getAcitvation(remainTime, new double[] { PREV_MARGIN, 30, 15 });
			isOverTime = false;
			
			// for jump only
			Motion targetMotion = mList.get(target.mIndex);
			Pose2d targetPose = PositionMotion.getPose(targetMotion);
			targetPose = p.globalToLocal(targetPose);
			double dirOffset = getJumpDirectionOffset(targetMotion);
			targetPose.direction = MathUtil.rotate(targetPose.direction, dirOffset);
			
//			Pose2d targetPose  = Pose2d.relativePose(p, target.getInteractionPose(mList, database));
//			Pose2d targetPose  = Pose2d.relativePose(p, target.getInteractionPose(mList, database));
			goalInfo = targetPose.toArray();
		}
		double[] action = getActionType(isOverTime ? actionTypes[0] : type);
		double[] control = action;
//		control = MathUtil.concatenate(control, new double[]{ isOverTime ? 1 : 0/*, activation */});
		
		
		
		control = MathUtil.concatenate(control, acti);
		control = MathUtil.concatenate(control, new double[] { goalInfo[2], goalInfo[3] });
//		control = MathUtil.concatenate(control, goalInfo);
		
		return control;
	}
	
	HashMap<Integer, Double> directionOffsetMap = new HashMap<Integer, Double>();
	double getJumpDirectionOffset(Motion m) {
		m = database.getMotionList()[m.motionIndex];
		Double offset = directionOffsetMap.get(m.motionIndex);
		if (offset == null) {
			for (MotionAnnotation ann : database.getEventAnnotations()) {
				if (!m.motionData.file.getName().contains(ann.file)) continue;
				if (m.frameIndex >= ann.startFrame && m.frameIndex <= ann.endFrame) {
					Pose2d current = Pose2d.getPose(m);
					Pose2d start = Pose2d.getPose(m.motionData.motionList.get(ann.startFrame));
					Pose2d end = Pose2d.getPose(m.motionData.motionList.get(ann.endFrame));
					Vector2d dir = MathUtil.sub(end.position, start.position);
					dir = current.globalToLocal(dir);
					offset = MathUtil.directionalAngle(Pose2d.BASE.direction, dir);
					directionOffsetMap.put(m.motionIndex, offset);
				}
			}
		}
		return offset;
	}
	
	public double[] getHasBall(int index){
		int remainTime = target.mIndex - (index);
		return new double[] { getActivation(remainTime) };
	}
	
	public static double[] getAcitvation(int remainTime, double[] margins) {
		double[] acti = new double[margins.length];
		for (int i = 0; i < acti.length; i++) {
			acti[i] = getActivation(remainTime, margins[i]);
		}
		return acti;
	}
	
	public static double getActivation(int remainTime, double margin) {
		return Math.max(0, 1 - Math.abs(remainTime)/(double)margin);
	}
	public static double getActivation(int remainTime) {
		return Math.max(0, 1 - Math.abs(remainTime)/(double)ACTIVATION_MARGIN);
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
		return getTrueList(size + 5); // 20210128
//		return getTrueList(size + 1);
	}
}
