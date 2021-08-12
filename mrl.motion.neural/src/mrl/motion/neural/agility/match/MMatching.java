package mrl.motion.neural.agility.match;

import static mrl.motion.dp.TransitionData.processError;
import static mrl.util.Configuration.MOTION_TRANSITON_MARGIN;

import java.util.ArrayList;
import java.util.Arrays;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionActionDP;
import mrl.motion.dp.TransitionData;
import mrl.motion.dp.TransitionData.TransitionNode;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.match.MotionMatching.NextMotion;
import mrl.util.Configuration;
import mrl.util.IterativeRunnable;
import mrl.util.Matrix2d;
import mrl.util.TimeChecker;
import mrl.util.Utils;

public class MMatching {
	public static boolean printLog = false;

	public static double TIMING_EARLY_FINISH_WEIGHT = 0.01;
	public static double TIMING_LATE_FINISH_WEIGHT = 2.5;
	
	public TransitionData tData;
	public TransitionActionDP dp;

	protected MatchingPath bestPath;
	protected double bestPathDiff;
	
	public MMatching(TransitionActionDP dp) {
		this.dp = dp;
		this.tData = dp.tData;
	}
	
	protected void logFinalPath(MatchingPath path, AgilityGoal goal) {
	}
	
	protected void log(String line) {
		if (printLog) System.out.println(line);
	}
	
	protected boolean isReachable(int startMotionIndex, AgilityGoal goal) {
		int nIndex = tData.motionToNodeMap[startMotionIndex].index;
		for (int t = goal.minSearchTime; t < goal.maxSearchTime-2; t++) {
			if (dp.distanceTable[goal.actionType][t][nIndex] < Integer.MAX_VALUE) {
				return true;
			}
		}
		return false;
	}
	
	public MatchingPath searchBest(int startMotionIndex, AgilityGoal goal) {
		Motion start = tData.mList[startMotionIndex];
		MatchingPath path = new MatchingPath(start);
		bestPath = null;
		bestPathDiff = Integer.MAX_VALUE;
		
		
		while (!isReachable(startMotionIndex, goal)) {
			goal.maxSearchTime++;
			System.out.println("increase search time : " + goal.maxSearchTime + " : " + tData.mList[startMotionIndex] + " : " + goal.actionType);
		}
		
		while (true) {
			if (path.time + MOTION_TRANSITON_MARGIN > goal.maxSearchTime){
				while (path.time < goal.maxSearchTime) {
					path.moveSequential(path.current.next);
					if (updatePath(path, goal)) break;
				}
				break;
			}
			
			NextMotion next = pickMatch(path, goal);
			
			TimeChecker.instance.state("After");
			if (next.isSequential) {
//				System.out.println("move sequential : " + path.current + "->" + next.motion);
				path.moveSequential(next.motion);
			} else  {
				path.moveTransition(next.motion, next.transitionDistance);
				for (int i = 0; i < MOTION_TRANSITON_MARGIN; i++) {
					if (path.current.next == null) {
						System.out.println("null :: " + path.current + " : " + path.current.prev + " : " + i);
						System.out.println("ppp : " + next.transitionDistance);
						throw new RuntimeException();
					}
					path.moveSequential(path.current.next);
				}
			}
			if (next.isCyclic) path.isCyclic = true;
			if (updatePath(path, goal)) break;
		}
		if (bestPath == null) {
			throw new RuntimeException();
		}
//		System.out.println("last motion : " + bestPath.current + "; " + bestPath.time + " / " + goal.timeLimit + " : " + bestPathDiff);
		logFinalPath(bestPath, goal);
		System.out.println("best path : " + goal + " : " + bestPathDiff + " : " + Arrays.toString(bestPath.log));
		return bestPath;
	}
	
	protected NextMotion pickMatch(MatchingPath path, AgilityGoal goal) {
		int nextMIndex = path.current.next.motionIndex;
		double[] cache = tData.transitionDistance(path.current.motionIndex);
		NextMotion[] matchList = new NextMotion[cache.length];
		Utils.runMultiThread(new IterativeRunnable() {
			@Override
			public void run(int index) {
				double d = cache[index];
				Motion target = tData.tNodeList[index].motion;
				boolean isSequential = (nextMIndex == target.motionIndex);
				matchList[index] = getBestMatchingError(path, goal, target, d, isSequential);
			}
		}, cache.length);
		
		NextMotion bestMatch = null;
		for (NextMotion m : matchList) {
			if (bestMatch == null || m.estimatedFinalError < bestMatch.estimatedFinalError) {
				bestMatch = m;
			}
		}
		if (bestMatch.estimatedFinalError >= Integer.MAX_VALUE) {
			System.out.println("pickMatch : " + path.current + " -> " + bestMatch.motion + " : " + bestMatch.estimatedFinalError);
			System.out.println("pickMatch_log : " + Arrays.toString(bestMatch.log));
		}
		return bestMatch;
	}
	
	private NextMotion getBestMatchingError(MatchingPath path, AgilityGoal goal, Motion target, double currentTransitionError, boolean isSequential) {
		boolean check = (path == null);
//		boolean check = path.current.toString().equals("TL_MC_PC_1023_09_01:549")
//				&& target.toString().equals("TL_MC_PC_1023_09_01:550");
//		MMatching.printLog = check;
		
		if (isSequential) log("Seq error : " + path.time + " : " + path.current + "->" + target + " : " + currentTransitionError);
		NextMotion next = new NextMotion(target, isSequential, currentTransitionError);
		next.estimatedFinalError = Integer.MAX_VALUE;
		if (currentTransitionError >= Integer.MAX_VALUE) {
			if (check) throw new RuntimeException(); 
			return next;
		}
		
		int currentAction = tData.motionNearActionTypes[path.current.motionIndex];
		int targetMIndex = target.motionIndex;
		int tAfterIdx = tData.transitionAfterMIndex[targetMIndex];
		if (!isSequential && ((tAfterIdx < 0) || (tData.motionToNodeMap[tAfterIdx] == null))){
			if (isSequential) log("Seq error2 : " + path.time + " : " + path.current + "->" + target + " : " + tAfterIdx);
			if (check) throw new RuntimeException();
			return next;
		}
		
		int actionIdx = goal.actionType;
		int targetAction = tData.motionNearActionTypes[targetMIndex];
		
		Boolean actionValid = goal.checkActionValid(
						path.current, tData.originMotionActionTypes[path.current.motionIndex], 
						target, tData.originMotionActionTypes[target.motionIndex]);
		if (check) {
			System.out.println("action valid : " + actionValid);
			System.out.println(Utils.toString(path.current, tData.originMotionActionTypes[path.current.motionIndex], 
						target, tData.originMotionActionTypes[target.motionIndex]));
		}
		if (actionValid != null) {
			if (actionValid) {
				next.estimatedFinalError = 0;
				next.isCyclic = true;
				if (check) throw new RuntimeException();
				return next;
			} else {
				if (check) throw new RuntimeException();
				return next;
			}
		}

		if (targetAction >= 0 && targetAction != actionIdx) {
			if ((currentAction != targetAction) || !isSequential) {
				if (isSequential) log("Seq error3 : " + path.time + " : " + path.current + "->" + target + " : " + actionIdx + " : " + currentAction + " : " + targetAction);
				if (check) throw new RuntimeException();
				return next;
			}
		}
		if (!isSequential) {
			int tAfterAction = tData.motionNearActionTypes[tAfterIdx];
			if (tAfterAction >= 0 && tAfterAction != actionIdx) {
				if (isSequential) log("Seq error4 : " + path.time + " : " + path.current + "->" + target + " : " + actionIdx + " : " + tAfterAction);
				if (check) throw new RuntimeException();
				return next;
			}
		}
		
		int targetNodeIndex = tData.motionToNodeMap[targetMIndex].index;
		Matrix2d currentT = new Matrix2d(tData.adjRootTransform[target.motionIndex].m);
		currentT.mul(path.transform, currentT);
		Pose2d currentPose = Pose2d.byBase(currentT);
		
		double eTrans = TransitionData.processError(path.transitionErrorSum, currentTransitionError);
		for (int i = goal.minSearchTime; i < goal.maxSearchTime-1; i++) {
			int remainTime = i - path.time;
			if (remainTime <= 0) continue;
			if ((!isSequential) && (remainTime <= Configuration.MOTION_TRANSITON_MARGIN)) continue;
			
			double eAction;
			Pose2d futurePose;
			double futureRotation;
			if (isSequential) {
				eAction = dp.distanceTable[actionIdx][remainTime-1][targetNodeIndex];
				futurePose = dp.poseTable[actionIdx][remainTime-1][targetNodeIndex];
				futureRotation = dp.rotationTable[actionIdx][remainTime-1][targetNodeIndex];
			} else {
				int tIdx = remainTime-1-Configuration.MOTION_TRANSITON_MARGIN;
				int nIdx = tData.motionToNodeMap[tAfterIdx].index;
				eAction = dp.distanceTable[actionIdx][tIdx][nIdx];
				futurePose = dp.poseTable[actionIdx][tIdx][nIdx];
				futureRotation = dp.rotationTable[actionIdx][tIdx][nIdx];
			}
			if (eAction >= Integer.MAX_VALUE) {
				if (isSequential) log("Seq error5 : " + i + " : " + path.time + " : " + path.current + "->" + target + " : " + actionIdx + " : " + eAction + " : " + remainTime);
				if (check) throw new RuntimeException();
				continue;
			}
			
			double eSpace = goal.getSpatialError(currentPose, path.rotation, futurePose, futureRotation);
			double eTransitionTotal = TransitionData.processError(eTrans, eAction);
			
			double error = estimateFinalError(eSpace, eTransitionTotal, i, goal);
			if (error < next.estimatedFinalError) {
				next.estimatedFinalError = error;
				next.log = new double[] { i, path.transitionErrorSum, currentTransitionError, eAction, eSpace, error, remainTime }; 
			}
		}
		return next;
	}
	
	protected boolean updatePath(MatchingPath path, AgilityGoal goal) {
		TransitionNode node = tData.motionToNodeMap[path.current.motionIndex];
		if (node == null) return true;
		if (path.time < goal.minSearchTime) return false;
		
		double dAction;
		if (path.isCyclic) {
			dAction = 0;
		} else {
			if (dp.distanceTable[goal.actionType][0][node.index] > 0) return false;
			dAction = path.transitionErrorSum;
		}
		
		path = new MatchingPath(path);
		double dSpatial = goal.getSpatialError(path.getCurrentPose(), path.rotation, Pose2d.BASE, 0);
		double diff = estimateFinalError(dSpatial, dAction, path.time, goal);
		if (diff < bestPathDiff) {
			bestPathDiff = diff;
			bestPath = new MatchingPath(path);
			bestPath.log = new double[] { dSpatial, dAction, bestPath.time, goal.maxSearchTime };
//			System.out.println("action type : " + bestPath.time + " : " + goal + " : " + tData.motionActionTypes[path.current.motionIndex] + " : " + path.current);
//			System.out.println("updated is valid : " + isValidEnd(bestPath.current, goal));
		}
		if (goal.isActiveAction()) {
			return true;
		}
		return false;
	}
	
	
		
	protected double estimateFinalError(double dSpatial, double dTransition, int finalTime, AgilityGoal goal) {
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
		
		double diff = (dSpatial + dTransition) * tWeight;
		return diff;
	}
	
	public int pickRandomStartMotion() {
		while (true) {
			Motion m = Utils.pickRandom(tData.mList);
			if (m.next == null) continue;
			if (tData.motionToNodeMap[m.next.motionIndex] == null) continue;
			if (tData.motionToNodeMap[m.motionIndex] != null) return m.motionIndex;
		}
	}
	
	public MDatabase database() {
		return tData.database;
	}
	
	public class MatchingPath{
		public Motion current;
		public int time;
		
		public double transitionErrorSum = 0;
		public double rotation = 0;
		public double translationLength = 0;
		public Matrix2d transform;
		
		public ArrayList<Motion> motionList = new ArrayList<Motion>();
		
		public double[] log;
		private Pose2d currentPose = null;
		public boolean isCyclic = false;
		
		MatchingPath(Motion start){
			this.current = start;
			this.time = 0;
			this.rotation = 0;
			transform = Matrix2d.identity();
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
		
		public void moveTransition(Motion next, double transDist) {
			moveSequential(next);
			transitionErrorSum = processError(transitionErrorSum, transDist);
		}
		public void moveSequential(Motion next) {
			currentPose = null;
			if (next == null) {
				System.out.println("move null : " + current + " : " + time + " : " + motionList.size());
				throw new RuntimeException();
			}
			
			Matrix2d t = tData.adjRootTransform[next.motionIndex].m;
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
		
		public Pose2d getCurrentPose() {
			if (currentPose == null) {
				currentPose = Pose2d.byBase(transform);;
			}
			return currentPose;
		}
	}
}
