package mrl.motion.neural.data;

import java.util.ArrayList;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;

public class DirectionControl extends ControlDataGenerator{
	
	public static boolean USE_VELOCITY = false; 
	private int interval;
	
	private int constIndex = 0;
	private ArrayList<Integer> constraintMotionIndices;
	
	
	public DirectionControl(int interval) {
		this.interval = interval;
	}
	public DirectionControl(ArrayList<Integer> constraintMotionIndices) {
		this.constraintMotionIndices = constraintMotionIndices;
	}
	
	@Override
	public double[] getControl(int index) {
		int controlIndex = index + interval;
		if (constraintMotionIndices != null) {
			if (constIndex >= constraintMotionIndices.size()) return null;
			controlIndex = constraintMotionIndices.get(constIndex);
			int inferenceMargin = 0;
			if (index >= controlIndex - inferenceMargin) {
				constIndex++;
				controlIndex = constraintMotionIndices.get(constIndex);
			}
			if (index >= controlIndex - inferenceMargin) {
				throw new RuntimeException();
			}
		}
		
		if (controlIndex >= mList.size() - 5) return null;
		
		double rotSum = 0;
		double velSum = 0;
		for (int i = index; i < controlIndex; i++) {
			Pose2d p = PositionMotion.getPose(mList.get(i));
			Pose2d next = PositionMotion.getPose(mList.get(i + 1));
			double angle = MathUtil.directionalAngle(p.direction, next.direction);
			rotSum += angle;
			velSum += p.position.distance(next.position);
		}
//		ActionOnlyWaveControl.getWave(time)
		if (USE_VELOCITY) {
			return new double[] { rotSum, velSum };
		} else {
			return new double[] { rotSum };
		}
		
//		Pose2d p = PositionMotion.getPose(mList.get(index));
//		Pose2d cp = PositionMotion.getPose(mList.get(controlIndex));
//		Pose2d targetPose = Pose2d.relativePose(p, cp);
		
//		
//		return new double[]{ targetPose.direction.x, targetPose.direction.y };
	}

}
