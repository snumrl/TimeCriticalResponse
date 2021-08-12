package mrl.motion.neural.agility.match;

import static mrl.util.Configuration.MOTION_TRANSITON_MARGIN;
import static mrl.motion.dp.ActionDistDP.processError;

import java.util.ArrayList;
import java.util.Arrays;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MDatabase.FrameType;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.dp.ActionDistDP;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.util.Configuration;
import mrl.util.IterativeRunnable;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.TimeChecker;
import mrl.util.Utils;

public abstract class MotionMatching {

	public static boolean preCalculateDistCache = false;
	
	public static boolean printLog = false;
	public static int printLog_pickCount = 1;
	
	public static int MAX_TIME_INTERVAL = 60;
	public static double TIMING_EARLY_FINISH_WEIGHT = 0.01;
	public static double TIMING_LATE_FINISH_WEIGHT = 2.5;
	
	protected MDatabase database;
	protected Motion[] mList;
	protected MotionDistByPoints dist;
	protected double[][] distCache;
	
	protected Matrix2d[] adjRootTransform2d;
	
	protected ArrayList<Integer> transitionableIndices;
	protected boolean[] isTransitionableMotion;
	protected boolean[] isContainedMotion;
	
	protected MatchingPath bestPath;
	protected double bestPathDiff;
	
	public MotionMatching(MDatabase database) {
		this.database = database;
		this.mList = database.getMotionList();
		dist = database.getDist();
		transitionableIndices = new ArrayList<Integer>();
		
		FrameType[] typeList = database.getTypeList();
		isTransitionableMotion = new boolean[typeList.length];
		isContainedMotion = new boolean[typeList.length];
		int start = -1;
		for (int i = 0; i <= typeList.length; i++) {
			if (i == typeList.length || typeList[i] == null || (i > 0 && mList[i-1].next != mList[i])) {
				if (start >= 0) {
					int end = i;
					for (int idx = start; idx < end - MOTION_TRANSITON_MARGIN*2; idx++) {
						transitionableIndices.add(idx);
						isTransitionableMotion[idx] = true;
					}
					for (int idx = start; idx < end; idx++) {
						isContainedMotion[idx] = true;
					}
					start = -1;
				}
			} else {
				if (start < 0) {
					start = i;
				}
			}
		}
		System.out.println("contained motions :: " + transitionableIndices.size());
		
		adjRootTransform2d = new Matrix2d[mList.length];
		for (int i = 0; i < adjRootTransform2d.length; i++) {
			Motion m = mList[i];
			if (m.next == null) continue;
			double[] trans = MotionTransform.getTransform(m.root(), m.next.root());
			adjRootTransform2d[i] = new Matrix2d(trans[0], trans[1], -trans[2]);
//			Pose2d p1 = PositionMotion.getPose(m);
//			Pose2d p2 = PositionMotion.getPose(m.next);
//			Matrix2d tp = Pose2d.localTransform(p1, p2);
//			System.out.println("--");
//			System.out.println(adjRootTransform2d[i]);
//			System.out.println(tp);
//			if (i > 10) System.exit(0);
		}
		
		distCache = new double[mList.length][];
//		if (preCalculateDistCache) {
//			Utils.runMultiThread(new IterativeRunnable() {
//				int progress;
//				@Override
//				public void run(int index) {
//					if (isContainedMotion[index]) {
//						synchronized (this) {
//							progress++;
//							if ((progress % 100) == 1) {
//								System.out.println("progress :: " + progress + " / " + transitionableIndices.size());
//							}
//						}
//						getDistanceCache(index);
//					}
//				}
//			}, isContainedMotion.length);
//		}
	}
	
	protected abstract double getSpatialError(MatchingPath path, AgilityGoal goal, Motion transitionMotion, int timeOffset, boolean isSequential);
	protected abstract double getActionError(MatchingPath path, AgilityGoal goal, Motion transitionMotion, int timeOffset, boolean isSequential);
	
//	protected abstract double getTransitionGoalDiff(MatchingPath path, Motion target, AgilityGoal goal);
//	protected abstract double getFinalGoalDiff(MatchingPath path, AgilityGoal goal);
	protected abstract boolean isValidEnd(Motion motion, AgilityGoal goal);
	
	public MDatabase getDatabase() {
		return database;
	}
	
	public int pickRandomStartMotion() {
		while (true) {
			Motion m = Utils.pickRandom(mList);
			if (m.next == null) continue;
			if (!isContainedMotion[m.next.motionIndex]) continue;
			if (isContainedMotion[m.motionIndex]) return m.motionIndex;
		}
	}
	
	protected void logFinalPath(MatchingPath path, AgilityGoal goal) {
	}
	
	protected void log(MatchingPath path, NextMotion next, AgilityGoal goal) {
	}
	
	protected void log(String line) {
		if (printLog) System.out.println(line);
	}
	
	protected double adjustDistance(double distance, int sourceMIndex, int targetMIndex) {
		return distance;
	}

	public MatchingPath searchBest(int startMotionIndex, AgilityGoal goal) {
		Motion start = mList[startMotionIndex];
		MatchingPath path = new MatchingPath(start);
		bestPath = null;
		bestPathDiff = Integer.MAX_VALUE;
		
//		TimeChecker.instance.enable = true;
		NextMotion next = null;
		while (true) {
			TimeChecker.instance.state("before");
			if (path.time + MOTION_TRANSITON_MARGIN > goal.maxSearchTime){
				while (path.time < goal.maxSearchTime) {
					path.moveSequential(path.current.next);
					if (updatePath(path, goal)) break;
				}
				break;
			}
			TimeChecker.instance.state("pickMatch");
			next = pickMatch(path, goal);
			
			TimeChecker.instance.state("After");
			log("transition : " + pickCount + " : " + path.time + " : " + goal.timeLimit + " / " + goal.maxSearchTime + " :: " 
						+ next.isSequential + " : " + next.transitionDistance + " : " + next.estimatedFinalError + " : " + path.current + " -> " + next.motion);
			log(Arrays.toString(next.log));
			log(path, next, goal);
			if (next.isSequential) {
				path.moveSequential(next.motion);
			} else  {
				path.moveTransition(next.motion, next.transitionDistance);
				for (int i = 0; i < MOTION_TRANSITON_MARGIN; i++) {
					if (path.current.next == null) {
						System.out.println("null :: " + path.current + " : " + path.current.prev + " : " + i);
						System.out.println("ppp : " + next.transitionDistance + " : " + bestMatch);
						throw new RuntimeException();
					}
					path.moveSequential(path.current.next);
				}
			}
			if (updatePath(path, goal)) break;
		}
		if (bestPath == null) {
			System.out.println("no path :: " + goal);
		}
		System.out.println("last motion : " + bestPath.current + "; " + bestPath.time + " / " + goal.timeLimit + " : " + bestPathDiff);
		System.out.println("log : " + Arrays.toString(next.log));
		logFinalPath(bestPath, goal);
//		System.out.println("is valid end :: " + isValidEnd(bestPath.current, goal));
		TimeChecker.instance.state("other");
		if (TimeChecker.instance.enable) {
			TimeChecker.instance.print();
		}
		return bestPath;
	}
	
	NextMotion bestMatch;
	protected int pickCount = 0;
	protected NextMotion pickMatch(MatchingPath path, AgilityGoal goal) {
		pickCount++;
		log("pickMatch : " + pickCount);
//		System.out.println("pickMatch :: " + path.time + " : " + path.current + " : " + isContainedMotion[path.current.motionIndex] + " : " + isContainedMotion[path.current.motionIndex+1]);
		Motion next = path.current.next;
		if (next == null) {
			System.out.println("next null :: " + path.current);
		}
		TimeChecker.instance.state("getDistanceCache");
		double[] cache = getDistanceCache(path.current.motionIndex);
//		DistCache[] cache = getDistanceCache(next.motionIndex);
		TimeChecker.instance.state("pickMatch_22");
		if (printLog) {
			Configuration.MAX_THREAD = 1;
		}
//		System.out.println("cache :: " + path.current.motionIndex + " : " + path.current.motionIndex + " : " + cache.length);
		NextMotion[] matchList = new NextMotion[cache.length];
		Utils.runMultiThread(new IterativeRunnable() {
			@Override
			public void run(int index) {
				double c = cache[index];
				double mDiff = c;
				int targetMIndex;
				if (index >= transitionableIndices.size()) {
					targetMIndex = next.motionIndex;
				} else {
					targetMIndex = transitionableIndices.get(index);
				}
				boolean isSequential = (next.motionIndex == targetMIndex);
				Motion m = mList[targetMIndex];
				matchList[index] = getBestMatchingError(path, goal, m, mDiff, isSequential);
			}
		}, cache.length);
		TimeChecker.instance.state("pickMatch_33");
		
		bestMatch = null;
		for (NextMotion m : matchList) {
			if (bestMatch == null || m.estimatedFinalError < bestMatch.estimatedFinalError) {
				bestMatch = m;
			}
		}
//		System.out.println("bbbbbb :: " + bestMatch.motion + " : " + bestMatch.isSequential + " : " + bestMatch.transitionDistance + " : " + bestMatch.estimatedFinalError);
//		System.out.println(Arrays.toString(bestMatch.log));
		if (bestMatch.log == null) {
			System.out.println(path.time + " : " + goal.maxSearchTime + " : " + goal.timeLimit);
//			System.out.println("###############");
//			for (NextMotion m : matchList) {
//				System.out.println(m.motion + " : " + Arrays.toString(m.log));
//			}
			System.exit(0);
		}
		return bestMatch;
	}
	
	private NextMotion getBestMatchingError(MatchingPath path, AgilityGoal goal, Motion target, double currentTransitionError, boolean isSequential) {
		NextMotion next = new NextMotion(target, isSequential, currentTransitionError);
		next.estimatedFinalError = Integer.MAX_VALUE;
		int searchCount = 0;
		for (int i = goal.minSearchTime; i < goal.maxSearchTime-1; i++) {
			int remainTime = i - path.time;
			if (remainTime < 0) continue;
			if ((!isSequential) && (remainTime < Configuration.MOTION_TRANSITON_MARGIN)) continue;
			searchCount++;
			double dSpatial = getSpatialError(path, goal, target, remainTime, isSequential);
			double dAction = getActionError(path, goal, target, remainTime, isSequential);
			double diff = estimateFinalError(processError(path.transitionErrorSum, currentTransitionError), dSpatial, dAction, i, goal);
			if (pickCount == printLog_pickCount) {
//				log("bb : " + Utils.toString(path.current, target, remainTime, dSpatial, dAction, path.transitionErrorSum, currentTransitionError, diff, isSequential));
			}
			
			double originDiff = diff;
			if (isSequential && diff >= Integer.MAX_VALUE) {
				diff = Integer.MAX_VALUE/2;
			}
			if (isSequential) {
				log("seq : " + Utils.toString(target, i, dSpatial, dAction, path.transitionErrorSum, currentTransitionError, originDiff));
			}
			if (diff < next.estimatedFinalError) {
				next.estimatedFinalError = diff;
				next.log = new double[] { i, dSpatial, dAction, path.transitionErrorSum, currentTransitionError, originDiff }; 
			}
		}
		next.log = MathUtil.concatenate(next.log, new double[] { searchCount });
		return next;
	}
	
	private double[] getDistanceCache(int motionIndex) {
		double[] cache = distCache[motionIndex];
		if (cache == null) {
			int len = transitionableIndices.size();
			if (!isTransitionableMotion[motionIndex + 1]) {
				len++;
			}
			double[] c = new double[len];
			for (int i = 0; i < c.length; i++) {
				c[i] = Integer.MAX_VALUE;
			}
//			ArrayList<DistCache> cList = new ArrayList<DistCache>();
			Utils.runMultiThread(new IterativeRunnable() {
				@Override
				public void run(int index) {
					int mIdx = transitionableIndices.get(index);
					boolean isNext = mList[motionIndex].next == mList[mIdx];
//					if (isNext) return;
					double d = dist.getDistance(motionIndex+1, mIdx);
					if (mIdx < motionIndex +1 && mIdx > motionIndex +1 - ActionDistDP.NO_BACKWARD_TRANSITION_MARGIN ) {
						if (mList[mIdx].motionData == mList[motionIndex].motionData) return;
					}
//					if (d < Configuration.MGRAPH_EDGE_WEIGHT_LIMIT*2) {
					if (!isNext) {
						d = adjustDistance(d, motionIndex, mIdx);
//						d += Configuration.MGRAPH_EDGE_WEIGHT_LIMIT*0.05;
					}
					if (d < Integer.MAX_VALUE) {
						d = d*d;
					}
//						DistCache cache = new DistCache(mIdx, d);
//						synchronized (cList) {
//							cList.add(cache);
//						}
						c[index] = d;
//					}
				}
			}, transitionableIndices.size());
//			if (isContainedMotion[motionIndex+1]) {
//				cList.add(new DistCache(motionIndex+1, 0));
//			}
//			distCache[motionIndex] = Utils.toArray(cList);
//			cache = distCache[motionIndex];
			distCache[motionIndex] = c;
			cache = c;
		}
		return cache;
	}
	
	protected boolean updatePath(MatchingPath path, AgilityGoal goal) {
		if (!isValidEnd(path.current, goal)) return false;
		if (path.time < goal.minSearchTime) return false;
		
		path = new MatchingPath(path);
		path.reverseLastTransform();
		double dSpatial = getSpatialError(path, goal, null, 0, true);
		double dAction = getActionError(path, goal, null, 0, true);
		double diff = estimateFinalError(path.transitionErrorSum, dSpatial, dAction, path.time, goal);
		if (diff < bestPathDiff) {
			bestPathDiff = diff;
			bestPath = new MatchingPath(path);
//			System.out.println("updated is valid : " + isValidEnd(bestPath.current, goal));
		}
		if (goal.isActiveAction()) {
			return true;
		}
		return false;
	}
	
	protected double estimateFinalError(double transitionErrorSum, double dSpatial, double dAction, int finalTime, AgilityGoal goal) {
		double tDiff = finalTime - goal.timeLimit;
		double tWeight;
		if (tDiff < 0) {
			int tLen = goal.timeLimit - goal.minSearchTime;
			tWeight = tDiff/tLen;
			tWeight = 1 + tWeight*TIMING_EARLY_FINISH_WEIGHT;
		} else {
			int tLen = goal.maxSearchTime - goal.timeLimit;
			if (tLen < 0) {
				tWeight = 1;
			} else {
				tWeight = tDiff/tLen;
				tWeight = 1 + tWeight*TIMING_LATE_FINISH_WEIGHT;
			}
		}
		
//		double poseError = processError(dAction, transitionErrorSum);
////		poseError = Math.sqrt(poseError);
//		double ratio = 1d/1000000d;
//		double diff = -Math.exp(-dSpatial*ratio)*Math.exp(-poseError*ratio)*Math.exp(tWeight);
////		System.out.println("dd : " + diff + " : " + Utils.toString(diff, dSpatial, processError(dAction, transitionErrorSum), tWeight));
////		System.out.println("dd2 : " + Utils.toString(diff, -dSpatial/10000d, -processError(dAction, transitionErrorSum)/10000d, tWeight));
////		System.out.println("dd3 : " + Utils.toString(diff, Math.exp(-dSpatial/10000d), Math.exp(-processError(dAction, transitionErrorSum)/10000d), tWeight));
//		return diff;
		
		double diff = (dSpatial + processError(dAction, transitionErrorSum)) * tWeight;
//		double diff = (dSpatial + dAction + transitionErrorSum) * tWeight;
		return diff;
	}
	
	
	protected static class NextMotion{
		public Motion motion;
		public boolean isSequential;
		public double transitionDistance;
		
		public double estimatedFinalError;
		public double[] log;
		public boolean isCyclic = false;
		public NextMotion(Motion motion, boolean isSequential, double transitionDistance) {
			this.motion = motion;
			this.isSequential = isSequential;
			this.transitionDistance = transitionDistance;
		}
	}
	
	
	public class MatchingPath{
		public Motion current;
		public int time;
		public double transitionErrorSum = 0;
		
		public double rotation = 0;
		public double translationLength = 0;
		public Matrix2d transform;
		
		public ArrayList<Motion> motionList = new ArrayList<Motion>();
		
		MatchingPath(Motion start){
			this.current = start;
			this.time = 0;
			this.rotation = 0;
			transform = new Matrix2d(adjRootTransform2d[start.motionIndex]);
			motionList.add(start);
		}
		
		MatchingPath(MatchingPath copy){
			this.current = copy.current;
			this.time = copy.time;
			this.transitionErrorSum = copy.transitionErrorSum;
			this.rotation = copy.rotation;
			this.translationLength = copy.translationLength;
			this.motionList = Utils.copy(copy.motionList);
			this.transform = new Matrix2d(copy.transform);
		}
		
		public void reverseLastTransform() {
			Matrix2d t = adjRootTransform2d[current.motionIndex];
			t.invert();
			rotation += t.getAngle();
			translationLength += t.getTranslation().length();
			transform.mul(t);
		}
		
		public void moveTransition(Motion next, double transDist) {
			moveSequential(next);
			transitionErrorSum = processError(transitionErrorSum, transDist);
		}
		public void moveSequential(Motion next) {
			if (next == null) {
				System.out.println("move null : " + current + " : " + time + " : " + motionList.size());
				throw new RuntimeException();
			}
			
			Matrix2d t = adjRootTransform2d[next.motionIndex];
			if (t == null) {
				System.out.println("no adj : " + current + " -> " + next);
			}
			rotation += t.getAngle();
			translationLength += t.getTranslation().length();
			transform.mul(t);
			
			current = next;
			motionList.add(next);
			time++;
		}
		
		public int[][] getPath(){
			return Motion.getPath(motionList);
		}
	}
}
