package mrl.motion.neural.data;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;

public class PositionControl extends ControlDataGenerator{
	
	private int interval;
	
	private int constIndex = 0;
	private ArrayList<Integer> constraintMotionIndices;
	
	
	public PositionControl(int interval) {
		this.interval = interval;
	}
	public PositionControl(ArrayList<Integer> constraintMotionIndices) {
		this.constraintMotionIndices = constraintMotionIndices;
	}
	
	@Override
	public double[] getControl(int index) {
		int controlIndex = index + interval;
		if (constraintMotionIndices != null) {
			if (constIndex >= constraintMotionIndices.size()) return null;
			controlIndex = constraintMotionIndices.get(constIndex);
			int inferenceMargin = 3;
			if (index >= controlIndex - inferenceMargin) {
				constIndex++;
				controlIndex = constraintMotionIndices.get(constIndex);
			}
			if (index >= controlIndex - inferenceMargin) {
				throw new RuntimeException();
			}
		}
		
		if (controlIndex >= mList.size() - 30) return null;
		
		Pose2d p = PositionMotion.getPose(mList.get(index));
		Pose2d next = PositionMotion.getPose(mList.get(controlIndex));
		Point2d target = p.globalToLocal(next.position);
		return new double[] { target.x, target.y };
//		Pose2d p = PositionMotion.getPose(mList.get(index));
//		int avgMargin1 = 20;
//		Point2d p1 = getMeanPose(index, avgMargin1);
//		Point2d p2 = getMeanPose(controlIndex, avgMargin1);
//		Vector2d v = MathUtil.sub(p2, p1);
//		Vector2d target = p.globalToLocal(v);
//		return new double[] { target.x, target.y };
	}
	
	private Point2d getMeanPose(int mIdx, int avgMargin1) {
		int start = Math.max(0, mIdx - avgMargin1);
		int end = Math.min(mIdx + avgMargin1, mList.size());
		Point2d pos = new Point2d();
		int count = 0;
		for (int i = start; i < end; i++) {
			count++;
			pos.add(PositionMotion.getPose(mList.get(i)).position);
		}
		pos.scale(1d/count);
		return pos;
	}

}
