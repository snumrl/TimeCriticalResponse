package mrl.motion.neural.data;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.data.ParameterBar.AngleBar;
import mrl.motion.neural.data.ParameterBar.LengthBar;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Utils;

public abstract class ControlDataGenerator {
	
	protected ArrayList<Motion> mList;
	protected ArrayList<double[]> mDataList;
	public boolean includeRootMove = false;
	protected ArrayList<Point3d> rootPositions;
	protected ArrayList<double[]> rootMove;
	
	public static boolean includePrediction = false;
	
	public abstract double[] getControl(int index);
	
	public double[] getHasBall(int index){
		return null;
	}
	
	public double[] getPrediction(int index){
		if (!includePrediction) return null;
		int interval = 5;
		int count = 6;
		double[] prediction = new double[count*3];
		for (int i = 0; i < count; i++) {
			int idx1 = index + interval*(i);
			int idx2 = index + interval*(i+1);
			
			Pose2d p = PositionMotion.getPose(mList.get(idx1));
			Pose2d cp = PositionMotion.getPose(mList.get(idx2));
			Pose2d target = Pose2d.relativePose(p, cp);
			
			int pIdx = i*3;
			prediction[pIdx + 0] = target.position.x;
			prediction[pIdx + 1] = target.position.y;
			prediction[pIdx + 2] = Math.atan2(target.direction.y, target.direction.x);
		}
		return prediction;
	}
	
	public void setData(ArrayList<Motion> mList, ArrayList<double[]> mDataList){
		this.mList = mList;
		this.mDataList = mDataList;
		init();
	}
	
	public void init(){
		if (includeRootMove){
			rootPositions = new ArrayList<Point3d>();
			for (int i = 0; i < mList.size(); i++) {
				rootPositions.add(new Point3d(MathUtil.getTranslation(mList.get(i).root())));
			}
			rootMove = new ArrayList<double[]>();
			for (int i = 0; i < mList.size()-1; i++) {
				Vector2d v = Pose2d.to2d(MathUtil.sub(rootPositions.get(i), rootPositions.get(i+1)));
				v.normalize();
				rootMove.add(new double[]{ v.x, v.y });
			}
		}
		initImpl();
	}
	
	protected void initImpl(){
	}
	
	public boolean[] getNormalMarking(){
		return null;
	}
	
	protected boolean[] getTrueList(int size){
		boolean[] list = new boolean[size];
		for (int i = 0; i < list.length; i++) {
			list[i] = true;
		}
		return list;
	}
	
	public static double getMovement(ArrayList<double[]> rootMove, int index){
		int margin = 30;
		int dSize = rootMove.get(0).length;
		double[] mean = new double[dSize];
		int start = Math.max(0, index - margin);
		int end = Math.min(rootMove.size() - 1, index + margin);
		int count = 0;
		for (int i = start; i <= end; i++) {
			for (int j = 0; j < mean.length; j++) {
				mean[j] += rootMove.get(i)[j];
			}
			count++;
		}
		for (int j = 0; j < mean.length; j++) {
			mean[j] /= count;
		}
		
		
		double[] sss = new double[dSize];
		for (int i = start; i <= end; i++) {
			for (int j = 0; j < mean.length; j++) {
				double d = rootMove.get(i)[j] - mean[j];
				sss[j] += d*d;
			}
		}
		
		double movement = 0;
		for (int i = 0; i < sss.length; i++) {
			movement += sss[i] / count;
		}
		movement /= sss.length;
		return movement;
	}
	
	public double getDirectionalMovement(Vector3d direction, int index, int margin){
		int start = Math.max(0, index - margin/3);
		int end = Math.min(rootMove.size() - 2, index + margin);
		double sum = 0;
		int count = 0;
		
		Vector2d d2d = Pose2d.to2d(direction);
		d2d.normalize();
		for (int i = start; i <= end; i++) {
			Point3d p0 = rootPositions.get(i);
			Point3d p1 = rootPositions.get(i+1);
			Vector2d v = Pose2d.to2d(MathUtil.sub(p1, p0));
			v.normalize();
			sum += Math.max(0.1, d2d.dot(v));
			count++;
		}
		return sum/count;
	}
	
	public static class TimeIntervalControl extends ControlDataGenerator{

		private int interval;
		
		public TimeIntervalControl(int interval, boolean includeRootMove) {
			this.interval = interval;
			this.includeRootMove = includeRootMove;
		}

		@Override
		public double[] getControl(int index) {
			int controlIndex = index + interval;
			if (controlIndex >= mList.size()) return null;
			
			Pose2d p = PositionMotion.getPose(mList.get(index));
			Pose2d cp = PositionMotion.getPose(mList.get(controlIndex));
			Pose2d targetPose = Pose2d.relativePose(p, cp);
			
			if (includeRootMove){
				return new double[]{ targetPose.position.x, targetPose.position.y, getMovement(rootMove, index) };
			} else {
				return new double[]{ targetPose.position.x, targetPose.position.y };
			}
		}
	}
	
	public static class RandomGoalControl extends ControlDataGenerator{
		
		public static int margin = 5;
		private int interval;
		private int std;
		private boolean includeDirection;
		private int goalIndex = -1;
		
		public RandomGoalControl(int interval, int std, boolean includeDirection) {
			this.interval = interval;
			this.std = std;
			this.includeDirection = includeDirection;
		}
		
		@Override
		public double[] getControl(int index) {
			if (goalIndex <= index + margin) {
				goalIndex = (int)(index + interval + std*Utils.rand1());
			}
			if (goalIndex >= mList.size()) return null;
			
			Pose2d p = PositionMotion.getPose(mList.get(index));
			Pose2d cp = PositionMotion.getPose(mList.get(goalIndex));
			Pose2d targetPose = Pose2d.relativePose(p, cp);
			Vector2d direction = new Vector2d(targetPose.direction);
			if (includeDirection) {
				return new double[]{ targetPose.position.x, targetPose.position.y, direction.x, direction.y};
			} else {
				return new double[]{ targetPose.position.x, targetPose.position.y };
			}
		}
	}
	
	public static class TimeIntervalBarControl extends ControlDataGenerator{
		
		private int interval;
		private ParameterBar angleBar = new AngleBar(8, 2);
		private ParameterBar lengthBar = new LengthBar(0, 250, 8, 2);
		
		public TimeIntervalBarControl(int interval, boolean includeRootMove) {
			this.interval = interval;
			this.includeRootMove = includeRootMove;
		}
		
		@Override
		public double[] getControl(int index) {
			int controlIndex = index + interval;
			if (controlIndex >= mList.size()){
				angleBar.printStatistics();
				lengthBar.printStatistics();
				return null;
			}
			
			Pose2d p = PositionMotion.getPose(mList.get(index));
			Pose2d cp = PositionMotion.getPose(mList.get(controlIndex));
			Pose2d targetPose = Pose2d.relativePose(p, cp);
			Point2d pos = targetPose.position;
			double angle = Math.atan2(pos.y, pos.x);
			double length = MathUtil.length(pos);
			if (includeRootMove) throw new RuntimeException();
			
			return MathUtil.concatenate(angleBar.getBar(angle), lengthBar.getBar(length));
		}
		
		public boolean[] getNormalMarking(){
			return getTrueList(16);
		}
	}
	
	public static double[] getAngleBar(double angle, int barSize, double thumbSize){
		double min = -Math.PI;
		double max = Math.PI;
		double interval = max - min;
		double[] bar = new double[barSize];
		
		double thumbLen = thumbSize*interval;
		
		for (int i = 0; i < bar.length; i++) {
			double start = min + interval*i;
			double end = start + interval;
			double mid = (start + end)/2;
			
			double dist = Math.abs(angle - mid);
			double activation = Math.min(Math.max((thumbLen - dist)/interval, 0), 1);
			bar[i] = activation;
		}
		return bar;
	}
	
//	private static double getBarValue()
	
	
	public static class TimeGoalControl extends ControlDataGenerator{
		
		private int interval;
		
		public TimeGoalControl(int interval, boolean includeRootMove){
			this.interval = interval;
			this.includeRootMove = includeRootMove;
		}
		
		@Override
		public double[] getControl(int index) {
			int target = (index + interval) - index%interval;
			if (target >= mList.size()) return null;
			Pose2d p = PositionMotion.getPose(mList.get(index));
			Pose2d cp = PositionMotion.getPose(mList.get(target));
			Pose2d targetPose = Pose2d.relativePose(p, cp);
			
			double max_dist = 100;
			Vector2d normalP = new Vector2d(targetPose.position);
			if (normalP.length() > max_dist){
				normalP.scale(max_dist/normalP.length());
			}
			double remainTime = target - index;
			double t2 = Math.min(40, remainTime);
			
			double movement = getMovement(rootMove, index);
			return new double[]{ targetPose.position.x, targetPose.position.y, normalP.x, normalP.y, remainTime, t2, movement };
		}
	}
	
	public static class TimeIntervalVelocityControl extends ControlDataGenerator{
		
		private int interval;
		private int margin;
		
		
		public TimeIntervalVelocityControl(int interval, int margin) {
			this.interval = interval;
			this.margin = margin;
		}
		
		@Override
		public double[] getControl(int index) {
			if (index + interval + margin + 1 >= mList.size()) return null;
			
			Vector2d v = new Vector2d();
			int count = 0;
			Pose2d p = PositionMotion.getPose(mList.get(index));
			for (int i = -margin; i < margin; i++) {
				Pose2d p1 = PositionMotion.getPose(mList.get(index + interval + margin));
				Pose2d p2 = PositionMotion.getPose(mList.get(index + interval + margin + 1));
				p1 = Pose2d.relativePose(p, p1);
				p2 = Pose2d.relativePose(p, p2);
				v.add(MathUtil.sub(p2.position, p1.position));
				count++;
			}
			
			v.scale(1d/count);
			
			return new double[]{ v.x, v.y };
		}
		
	}
}
