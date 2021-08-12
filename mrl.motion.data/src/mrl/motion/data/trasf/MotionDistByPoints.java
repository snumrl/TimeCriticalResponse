package mrl.motion.data.trasf;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Contact;
import mrl.motion.data.Motion;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.Joint;
import mrl.util.IterativeRunnable;
import mrl.util.MathUtil;
import mrl.util.Pair;
import mrl.util.Utils;

public class MotionDistByPoints {
	
	public static boolean USE_BALL_CONTACT = false;
	public static boolean USE_VELOCITY = true;
	public static int BASE_POINT_SIZE = 120;
	public static double CONTACT_CONFLICT_ERROR = 50;
	
	public static int BALL_MIN_MARGIN = 6;

	protected SkeletonData skeletonData;
	protected Motion[] mList;
	public MotionPoints[] mPoints;
	public double unitLen;
	
	static protected int[] prevBallMargin;
	static protected int[] postBallMargin;
	static protected int[] ballLeftOffset;
	
	protected static double[] pointWeights;
	

	public MotionDistByPoints(final SkeletonData skeletonData, final Motion[] mList) {
		this.skeletonData = skeletonData;
		this.mList = mList;
		
		double lenSum = 0;
		int offsetSum = 0;
		for (Joint j : skeletonData.values()){
			offsetSum += j.distPointOffset;
			if (j.parent == null) continue;
			lenSum += j.length;
		}
//		unitLen = lenSum/skeletonData.size();
//		unitLen = unitLen/4;
		unitLen = lenSum/(BASE_POINT_SIZE - offsetSum);
		System.out.println("Unit len :: " + unitLen);
		
		long t = System.currentTimeMillis();
		mPoints = new MotionPoints[mList.length];
		
		if (!USE_VELOCITY){
			Utils.runMultiThread(new IterativeRunnable() {
				@Override
				public void run(int i) {
					mPoints[i] = getPoints(skeletonData, mList[i], unitLen);
				}
			}, mList.length);
		} else {
//			final ArrayList<ArrayList<Point3d>> posePointList = new ArrayList<ArrayList<Point3d>>(mList.length);
			@SuppressWarnings("unchecked")
			final ArrayList<Point3d>[] posePointList = new ArrayList[mList.length];
			Utils.runMultiThread(new IterativeRunnable() {
				@Override
				public void run(int i) {
					posePointList[i] = getPosePoints(skeletonData, mList[i], unitLen);
				}
			}, mList.length);
			Utils.runMultiThread(new IterativeRunnable() {
				@Override
				public void run(int i) {
					ArrayList<Point3d> current = posePointList[i];
					ArrayList<Vector3d> velList = getVelocityPoints(skeletonData, posePointList, i);
					mPoints[i] = new MotionPoints(current, velList, mList[i]);
				}
			}, mList.length);
		}
		
		pointWeights = new double[mPoints[0].points.size()];
		for (int i = 0; i < pointWeights.length; i++) {
			pointWeights[i] = 1;
		}
		
		if (USE_BALL_CONTACT) {
			prevBallMargin = new int[mList.length];
			postBallMargin = new int[mList.length];
			ballLeftOffset = new int[mList.length];
			for (int i = 0; i < prevBallMargin.length; i++) {
				Contact contact = mList[i].ballContact;
				int margin = 0;
				for (int m = 1; m <= BALL_MIN_MARGIN; m++) {
					int idx = i - m;
					if (idx < 0) break;
					if (contact.equals(mList[idx].ballContact)){
						margin++;
					}
				}
				prevBallMargin[i] = margin;
				
				margin = 0;
				for (int m = 1; m <= BALL_MIN_MARGIN; m++) {
					int idx = i + m;
					if (idx >= mList.length) break;
					if (contact.equals(mList[idx].ballContact)){
						margin++;
					}
				}
				postBallMargin[i] = margin;
				
				ballLeftOffset[i] = ballLeftOffset(i, 10);
			}
		}
		
		System.out.println("unit len : " + unitLen + " ,  sample point size " + mPoints[0].points.size()
				 + ",  time : " + (System.currentTimeMillis() - t));
	}
	
	private int ballLeftOffset(int index, int margin){
		if (mList[index].ballContact.isNoContact()) return margin;
		for (int i = 1; i < margin; i++) {
			int idx = index + i;
			if (idx < 0 || idx >= mList.length) continue;
			if (mList[idx].ballContact.isNoContact()) return i;
		}
		return margin;
	}
	
	protected ArrayList<Vector3d> getVelocityPoints(SkeletonData skeletonData, ArrayList<Point3d>[] posePointList, int i){
		int post = Math.min(mList.length-1, i + 2);
		ArrayList<Point3d> current = posePointList[i];
		ArrayList<Point3d> next;
		if (mList[i].next == null || mList[i].next.next == null){
			next = posePointList[i];
		} else if (mList[i].next.next != mList[post]){
			next = posePointList[post];
		} else {
			next = getPosePoints(skeletonData, mList[i].next.next, unitLen);
		}
		ArrayList<Vector3d> velList = new ArrayList<Vector3d>();
		for (int j = 0; j < current.size(); j++) {
			Vector3d v = new Vector3d();
			v.sub(next.get(j), current.get(j));
			velList.add(v);
		}
		return velList;
	}
	
	public int size(){
		return mList.length;
	}
	
	public double[][] calcDistAll(final int margin){
		long t = System.currentTimeMillis();
		final double[][] distMap = new double[mList.length][mList.length];
		Utils.runMultiThread(new IterativeRunnable(){
			@Override
			public void run(int i) {
				MotionPoints source = mPoints[i];
				for (int j = i+1; j < mList.length; j++) {
					MotionPoints target = mPoints[j];
					double d = getDistance(source, target);
					distMap[i][j] = distMap[j][i] = d;
				}
			}
		}, mList.length);
		System.out.println("dist time : " + (System.currentTimeMillis() - t));
		
		if (margin == 0) return distMap;
		
		final double[][] seqDistMap = new double[mList.length][mList.length];
		Utils.runMultiThread(new IterativeRunnable(){
			@Override
			public void run(int i) {
				for (int j = 0; j < mList.length; j++) {
					seqDistMap[i][j] = calcSeqDist(distMap, i, j, margin, 0.125);
				}
			}
		}, mList.length);
		return seqDistMap;
	}
	
	public static double calcSeqDist(double[][] distMap, int x, int y, int margin, double damp){
//		if (x == y) return 0;
		double dSum = distMap[x][y];
		double weight = 1;
		
		int p1 = x;
		int p2 = y;
		for (int i = 1; i <= margin; i++) {
			if (p1 > 0) p1--;
			if (p2 > 0) p2--;
			weight += (1-damp*i);
			dSum += distMap[p1][p2]*(1-damp*i);
		}
		
		int n1 = x;
		int n2 = y;
		for (int i = 1; i <= margin; i++) {
			if (n1 < distMap.length-1) n1++;
			if (n2 < distMap.length-1) n2++;
			weight += (1-damp*i);
			dSum += distMap[n1][n2]*(1-damp*i);
		}
		double poseDiff = dSum / weight;
		return poseDiff;
	}
	
	public void updatePointWeightByJointWeight() {
		SkeletonData skeletonData = SkeletonData.instance;
//		HashMap<String, Point3d> jointPoints = Motion.getPointData(skeletonData, m);
		pointWeights = new double[pointWeights.length];
		int idx = 0;
		for (Joint j : skeletonData.values()){
//			Point3d p = jointPoints.get(j.name);
			double w = j.weight;
//			System.out.println("joint weight : " + j.name + " : " + j.weight + " : " + j.distPointOffset);
			pointWeights[idx++] = w;
			
			if (j.parent == null) continue;
			
			int count = (int)Math.round(j.length/unitLen) + j.distPointOffset;
			if (count > 1){
				for (int i = 1; i < count; i++) {
					pointWeights[idx++] = w;
				}
			}
		}
		double sum = 0;
		for (int i = 0; i < pointWeights.length; i++) {
			sum += pointWeights[i];
		}
		sum /= pointWeights.length;
		for (int i = 0; i < pointWeights.length; i++) {
			pointWeights[i] /= sum;
		}
//		System.out.println("plen :: " + idx + " : " + pointWeights.length);
	}
	
	public void updateMeanPositions() {
		Utils.runMultiThread(new IterativeRunnable() {
			@Override
			public void run(int index) {
				mPoints[index].updateMeanPoint();
			}
		}, mPoints.length);
	}
		
	
	public double getDistance(int i, int j){
		return getDistance(mPoints[i], mPoints[j]);
	}
	
	public static double getDistance(MotionPoints source, MotionPoints target){
		return getDistanceAndTransform(source, target).first;
	}
	public static Pair<Double, Matrix4d> getDistanceAndTransform(MotionPoints source, MotionPoints target){
		boolean leftConflict = source.isLeftFootContact != target.isLeftFootContact;
		boolean rightConflict = source.isRightFootContact != target.isRightFootContact;
		
		Matrix4d alignTransform = getAlignTransform(source, target);
		double sum = 0;
		for (int i = 0; i < source.points.size(); i++) {
			Point3d p1 = source.points.get(i);
			Point3d p2 = new Point3d(target.points.get(i));
			alignTransform.transform(p2);
			double d = p1.distance(p2);
			sum += d*d * pointWeights[i];
//			sum += p1.distance(p2);
			
			if (source.velocitys != null){
				Vector3d v1 = source.velocitys.get(i);
				Vector3d v2 = new Vector3d(target.velocitys.get(i));
				alignTransform.transform(v2);
				d = MathUtil.distance(v1, v2);
				sum += d*d*2  * pointWeights[i];
			}
		}
		double d = sum/(source.points.size()*3);
		if (leftConflict && rightConflict) {
			d += CONTACT_CONFLICT_ERROR*5;
		} else if (leftConflict || rightConflict) {
			d += CONTACT_CONFLICT_ERROR;
		}
		
		if (USE_BALL_CONTACT) {
			if (isBallError(source, target)) {
				d += CONTACT_CONFLICT_ERROR*10;
			} else if (!source.motion.ballContact.equals(target.motion.ballContact)){
				d += CONTACT_CONFLICT_ERROR*5;
			}
		}
		return new Pair<Double, Matrix4d>(d, alignTransform);
	}
	
	private static boolean isBallError(MotionPoints source, MotionPoints target) {
		if (!source.motion.ballContact.equals(target.motion.ballContact)) return true;
		int i = source.motion.motionIndex;
		int j = target.motion.motionIndex;
		if (ballLeftOffset[i] <= 4 && ballLeftOffset[j] >= 8) return true;
		if (ballLeftOffset[j] <= 4 && ballLeftOffset[i] >= 8) return true;
		if (prevBallMargin[i+1] + postBallMargin[j] < BALL_MIN_MARGIN) return true;
		return false;
	}
	
	public static Matrix4d getAlignTransform(MotionPoints source, MotionPoints target){
		double w = 1d/source.points.size();
		double wSum = 1;
		
		double v1, v2, v3, v4;
		v1 = v2 = v3 = v4 = 0;
		for (int i = 0; i < source.points.size(); i++) {
			Point3d s = source.points.get(i);
			Point3d t = target.points.get(i);
			v1 += w*(s.x*t.z - t.x*s.z);
			v2 += w*(s.x*t.x + s.z*t.z);
		}
		v3 = (1/wSum)*(source.mean.x*target.mean.z - target.mean.x*source.mean.z);
		v4 = (1/wSum)*(source.mean.x*target.mean.x + source.mean.z*target.mean.z);
		double theta = Math.atan2(v1 - v3, v2 - v4);
		double sin = Math.sin(theta);
		double cos = Math.cos(theta);
		
		double x0 = (1/wSum)*(source.mean.x - target.mean.x*cos - target.mean.z*sin);
		double z0 = (1/wSum)*(source.mean.z + target.mean.x*sin - target.mean.z*cos);
		
		Matrix4d m = new Matrix4d();
		m.rotY(theta);
		m.setTranslation(new Vector3d(x0, 0, z0));
		return m;
	}
	
	
	public static MotionPoints getPoints(SkeletonData skeletonData, Motion m, double unitLen){
		
		ArrayList<Point3d> points = getPosePoints(skeletonData, m, unitLen);
//		HashMap<String, Point3d> jointPoints = Motion.getPointData(skeletonData, m);
//		ArrayList<Point3d> points = new ArrayList<Point3d>();
//		for (Joint j : skeletonData.values()){
//			Point3d p = jointPoints.get(j.name);
//			points.add(p);
//			
//			if (j.parent == null) continue;
//			
//			int count = (int)Math.round(j.length/unitLen);
//			if (count > 1){
//				Point3d parentP = jointPoints.get(j.parent.name);
//				for (int i = 1; i < count; i++) {
//					Point3d sample = new Point3d();
//					sample.interpolate(p, parentP, i/(double)count);
//					points.add(sample);
//				}
//			}
//		}
		
		return new MotionPoints(points, null, m);
	}
	
	public static ArrayList<Point3d> getPosePoints(SkeletonData skeletonData, Motion m, double unitLen){
		HashMap<String, Point3d> jointPoints = Motion.getPointData(skeletonData, m);
		
		ArrayList<Point3d> points = new ArrayList<Point3d>();
		for (Joint j : skeletonData.values()){
			Point3d p = jointPoints.get(j.name);
			points.add(p);
			
			if (j.parent == null) continue;
			
			int count = (int)Math.round(j.length/unitLen) + j.distPointOffset;
			if (count > 1){
				Point3d parentP = jointPoints.get(j.parent.name);
				for (int i = 1; i < count; i++) {
					Point3d sample = new Point3d();
					sample.interpolate(p, parentP, i/(double)count);
					points.add(sample);
				}
			}
		}
		
		return points;
	}
	
	public static class MotionPoints{
		public ArrayList<Point3d> points;
		public ArrayList<Vector3d> velocitys;
		public Point3d mean;
		public boolean isLeftFootContact;
		public boolean isRightFootContact;
		public Motion motion;
		
		public MotionPoints(ArrayList<Point3d> points,
				ArrayList<Vector3d> velocitys, Motion motion) {
			this.points = points;
			this.velocitys = velocitys;
			this.isLeftFootContact = motion.isLeftFootContact;
			this.isRightFootContact = motion.isRightFootContact;
			this.motion = motion;
			updateMeanPoint();
		}
		
		public void updateMeanPoint() {
			Point3d m = new Point3d();
			for (Point3d p : points) {
				m.add(p);
			}
			m.scale(1d/points.size());
			mean = m;
		}
	}
}
