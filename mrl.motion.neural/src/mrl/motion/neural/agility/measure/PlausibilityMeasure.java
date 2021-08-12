package mrl.motion.neural.agility.measure;

import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.data.trasf.MotionDistByPoints.MotionPoints;
import mrl.util.IterativeRunnable;
import mrl.util.Pair;
import mrl.util.Utils;

public class PlausibilityMeasure {

	private Motion[] originMotions;
	private SequentialDist originDist;

	public PlausibilityMeasure(MDatabase database) {
		originMotions = database.getMotionList();
		originDist = new SequentialDist(originMotions);
	}
	
	public PlausibilityMatch[] measure(ArrayList<Motion> motionList) {
		for (int i = 0; i < motionList.size() - 1; i++) {
			Motion m1 = motionList.get(i);
			m1.frameIndex = i;
			Motion m2 = motionList.get(i + 1);
			m2.frameIndex = i + 1;
			m1.next = m2;
			m2.prev = m1;
		}
		SequentialDist dist = new SequentialDist(Utils.toArray(motionList));
		PlausibilityMatch[] plausibility = new PlausibilityMatch[motionList.size()];
		Utils.runMultiThread(new IterativeRunnable() {
			@Override
			public void run(int index) {
				plausibility[index] = getBestMatch(motionList.get(index), dist.mPoints[index]);
			}
		}, plausibility.length);
		return plausibility;
	}
	
	private PlausibilityMatch getBestMatch(Motion m, MotionPoints p) {
		double minError = Integer.MAX_VALUE;
		int minIdx = -1;
		Matrix4d alignTransform = null;
		for (int i = 0; i < originDist.mPoints.length; i++) {
			Pair<Double, Matrix4d> pair = MotionDistByPoints.getDistanceAndTransform(p, originDist.mPoints[i]);
			if (pair.first < minError) {
				minError = pair.first;
				minIdx = i;
				alignTransform = pair.second;
			}
		}
		PlausibilityMatch match = new PlausibilityMatch(m, originMotions[minIdx], minError, alignTransform);
		match.gMp = p;
		match.oMp = originDist.mPoints[minIdx];
		return match;
	}
	
	public static class PlausibilityMatch{
		public Motion generated;
		public Motion origin;
		public double distance;
		Matrix4d alignTransform;
		
		public MotionPoints gMp;
		public MotionPoints oMp;
		
		public PlausibilityMatch(Motion generated, Motion origin, double distance, Matrix4d alignTransform) {
			this.generated = generated;
			this.origin = origin;
			this.distance = distance;
			this.alignTransform = alignTransform;
		}
	}
	
	private static class SequentialDist extends MotionDistByPoints{

		public SequentialDist(Motion[] mList) {
			super(SkeletonData.instance, mList);
		}
		
		protected ArrayList<Vector3d> getVelocityPoints(SkeletonData skeletonData, ArrayList<ArrayList<Point3d>> posePointList, int i){
			ArrayList<Point3d> current = posePointList.get(i);
			int prevIdx = Math.max(0, i - 2);
			int post = Math.min(mList.length-1, i + 2);
			
			ArrayList<Point3d> prev;
			if (mList[i].prev == null || mList[i].prev.prev == null){
				prev = posePointList.get(i);
			} else if (mList[i].prev.prev != mList[prevIdx]){
				prev = posePointList.get(prevIdx);
			} else {
				prev = getPosePoints(skeletonData, mList[i].prev.prev, unitLen);
			}
			ArrayList<Point3d> next;
			if (mList[i].next == null || mList[i].next.next == null){
				next = posePointList.get(i);
			} else if (mList[i].next.next != mList[post]){
				next = posePointList.get(post);
			} else {
				next = getPosePoints(skeletonData, mList[i].next.next, unitLen);
			}
			next = Utils.copy(next);
			next.addAll(prev);
			ArrayList<Vector3d> velList = new ArrayList<Vector3d>();
			for (int j = 0; j < current.size(); j++) {
				Vector3d v = new Vector3d();
				v.sub(next.get(j), current.get(j));
				velList.add(v);
			}
			return velList;
		}
	}
	
}
