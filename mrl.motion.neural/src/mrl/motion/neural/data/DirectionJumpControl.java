package mrl.motion.neural.data;

import java.util.ArrayList;

import javax.vecmath.Point2d;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.JumpModel;
import mrl.motion.neural.agility.JumpModel.JumpGoal;
import mrl.motion.neural.agility.MotionMatchingSampling.MMControlParameter;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;

public class DirectionJumpControl extends ControlDataGenerator{
	
	public static boolean USE_POSITION_CONTROL = false;
	public static boolean USE_ACTIVATION = true;
	public static int ACTIVATION_RISE = 3;
	public static int ACTIVATION_PEAK = 1;
	public static double activation;
	
	private int constIndex = 0;
	private ArrayList<MMControlParameter> constraintMotionIndices;
	
	public DirectionJumpControl(ArrayList<MMControlParameter> constraintMotionIndices) {
		this.constraintMotionIndices = constraintMotionIndices;
	}
	
	@Override
	public double[] getControl(int index) {
		if (constIndex >= constraintMotionIndices.size()) return null;
		
		MMControlParameter cp = constraintMotionIndices.get(constIndex);
		int inferenceMargin = 0;
		if (index >= cp.frame - inferenceMargin) {
			constIndex++;
			cp = constraintMotionIndices.get(constIndex);
		}
		if (index >= cp.frame - inferenceMargin) {
			throw new RuntimeException();
		}
		
		if (cp.frame >= mList.size() - 5) return null;
		
		double rotSum = 0;
		for (int i = index; i < cp.frame; i++) {
			Pose2d p = PositionMotion.getPose(mList.get(i));
			Pose2d next = PositionMotion.getPose(mList.get(i + 1));
			double angle = MathUtil.directionalAngle(p.direction, next.direction);
			rotSum += angle;
		}
		
		JumpGoal goal = (JumpGoal)cp.goal;
		if (goal.actionType >= 0) {
			rotSum = Double.NaN;
		}
		
		double remainTime = cp.frame - index;
		double passedTime = 999999;
		if (constIndex > 0) {
			passedTime = index - constraintMotionIndices.get(constIndex-1).frame;
		}
		int activMargin = ACTIVATION_PEAK + ACTIVATION_RISE;
		activation = 0;
		if (remainTime <= activMargin) {
			if (remainTime <= ACTIVATION_PEAK) {
				activation = 1;
			} else {
				activation = 1 - (remainTime - ACTIVATION_PEAK)/(double)(ACTIVATION_RISE + 1);
			}
			if (goal.actionType < 0) activation = 0;
		}
//		else if (passedTime <= activMargin) {
//			if (passedTime <= ACTIVATION_PEAK) {
//				activation = 1;
//			} else {
//				activation = 1 - (passedTime - ACTIVATION_PEAK)/(double)(ACTIVATION_RISE + 1);
//			}
//			JumpGoal prevGoal = (JumpGoal)constraintMotionIndices.get(constIndex-1).goal;
//			if (prevGoal.actionType < 0) activation = 0;
//		}
		
		
		double[] control = getActionType(goal.actionType);
		control = MathUtil.concatenate(control, new double[] { rotSum });
		
		if (USE_POSITION_CONTROL) {
			Pose2d p = PositionMotion.getPose(mList.get(index));
			Pose2d next = PositionMotion.getPose(mList.get(cp.frame));
			Point2d target = p.globalToLocal(next.position);
			if (goal.actionType >= 0) {
				control = MathUtil.concatenate(control, new double[] { target.x, target.y });
			} else {
				control = MathUtil.concatenate(control, new double[] { Double.NaN, Double.NaN });
			}
		}
		
		return control;
	}
	
	public double[] getHasBall(int index){
		if (!USE_ACTIVATION) return null;
		return new double[] { activation };
	}
	
	public boolean[] getNormalMarking(){
		return getTrueList(JumpModel.actionTypes.length+1);
	}
	
	public static double[] getActionType(int action){
		double[] typeArray = new double[JumpModel.actionTypes.length+1];
		typeArray[action+1] = 1;
		return typeArray;
	}

}
