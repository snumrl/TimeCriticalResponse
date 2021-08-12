package mrl.motion.neural.rl;

import static mrl.motion.dp.TransitionData.processError;
import static mrl.util.Configuration.MOTION_TRANSITON_MARGIN;

import java.util.ArrayList;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.gmm.GMMConfig;
import mrl.motion.neural.gmm.GMMConfig.GMMGoal;
import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;
import mrl.util.Matrix2d;
import mrl.util.Pair;
import mrl.util.Utils;

public class PolicyEvaluation {

	public TransitionData tData;
	private MFeatureMatching matching;
	private MFeatureSelector policy;
	
	public PolicyEvaluation(TransitionData tData, MFeatureSelector policy) {
		this.tData = tData;
		this.policy = policy;
		
		matching = new MFeatureMatching(tData.database);
	}

	LocoGoal sampleRandomGoal() {
		return new LocoGoal(Utils.rand1()*Math.PI);
	}
	
	public double test() {
		Motion startMotion = tData.tNodeList[0].motion;
		int testSize = 200;
		double errorSum = 0;
		for (int i = 0; i < testSize; i++) {
			LocoGoal goal = sampleRandomGoal();
			Pair<Double, Motion> result = testGoal(startMotion, goal);
			errorSum += result.first;
			System.out.println("eror : " + i + " : " + Math.toDegrees(Math.sqrt(result.first)));
			startMotion = result.second;
		}
		return errorSum/testSize;
	}
	
	private boolean isFirst = true;
	private Pair<Double, Motion> testGoal(Motion startMotion, LocoGoal goal) {
		int time = 0;
		MatchingPath path = new MatchingPath(startMotion);
		
		double error = -1;
		while (true) {
			time++;
			if (time > 60) {
				// check time limit
				// check goal finished
				double w = Math.PI / 100;
				w = 0;
				w = w*w;
				error = w * path.transitionErrorSum + goal.evaluateFinalError(path);
				break;
					// calc final goal reward
			}
			MotionFeature state = matching.getOriginData(path.current.motionIndex, true);
			MotionQuery control = policy.getControl(state, goal.getParameter(path));
			if (isFirst) {
				isFirst = false;
				System.out.println("Feature Size :: " + state.data.length + " : " + control.toArray().length + " : " + goal.getParameter(path).length);
			}
			int mIndex = matching.findMatch(control, true);
			path.moveTransition(tData.database.getMotionList()[mIndex], tData);
			for (int i = 0; i < MOTION_TRANSITON_MARGIN; i++) {
				if (path.current.next == null) {
					throw new RuntimeException();
				}
				time++;
				path.moveSequential(path.current.next, tData);
			}
		}
		return new Pair<Double, Motion>(error, path.current);
	}
	
	public static class MatchingPath{
		public Motion current;
		public int time;
		
		public double transitionErrorSum = 0;
		public double rotation = 0;
		public double translationLength = 0;
		public Matrix2d transform;
		
		public ArrayList<Motion> motionList = new ArrayList<Motion>();
		public ArrayList<Matrix2d> transformList = new ArrayList<Matrix2d>();
		
		public double[] log;
		private Pose2d currentPose = null;
		public boolean isCyclic = false;
		
		MatchingPath(Motion start){
			this.current = start;
			this.time = 0;
			this.rotation = 0;
			transform = Matrix2d.identity();
			motionList.add(start);
			transformList.add(new Matrix2d(transform));
		}
		
		MatchingPath(MatchingPath copy){
			this.current = copy.current;
			this.time = copy.time;
			this.transitionErrorSum = copy.transitionErrorSum;
			this.rotation = copy.rotation;
			this.translationLength = copy.translationLength;
			this.motionList = Utils.copy(copy.motionList);
			this.transformList = Utils.copy(copy.transformList);
			this.transform = new Matrix2d(copy.transform);
		}
		
		public double moveTransition(Motion next, TransitionData tData) {
//			double tError = tData.database.getDist().getDistance(current.motionIndex + 1, next.motionIndex);
//			tError = tError*tError;
			double tError = tData.transitionDistance(current.motionIndex, next.motionIndex);
			moveSequential(next, tData);
//			Motion[] mList = tData.database.getMotionList();
//			System.out.println("tError ::: " + tError + " : " + mList[current.motionIndex] + " -> " + mList[next.motionIndex]);
//			System.out.println("tt : " + tData.transitionDistance(current.motionIndex, next.motionIndex + 1));
//			System.out.println("tt : " + tData.transitionDistance(current.motionIndex,next.motionIndex - 1));
			 
			transitionErrorSum = processError(transitionErrorSum, tError);
			return tError;
		}
		public void moveSequential(Motion next, TransitionData tData) {
			currentPose = null;
			if (next == null) {
				System.out.println("move null : " + current + " : " + time + " : " + motionList.size());
				throw new RuntimeException();
			}
			if (tData.adjRootTransform[next.motionIndex] == null) {
				System.out.println("null :: " + next);
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
			transformList.add(new Matrix2d(transform));
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
		
		public void printTrace(GMMConfig config, GMMGoal goal) {
			System.out.println("MatchingPath::printTrace");
			System.out.println("goal :: " + goal + " : rTime" + (goal.maxSearchTime - time));
			System.out.println("straight :: " + TransitionData.STRAIGHT_MARGIN);
			TransitionData tData = config.tData;
			for (Motion p : motionList) {
				System.out.println(p + " : " + tData.motionNearActionTypes[p.motionIndex] + " : " + tData.motionActionTypes[p.motionIndex]);
//				System.out.println("dp :: " + config.actionDP.tAfterDistanceTable[goal.actionType][3][config.tData.motionToNodeMap[p.motionIndex].index]);
			}
		}
	}
	
	static class LocoGoal{
		public double direction;

		public LocoGoal(double direction) {
			this.direction = direction;
		}
		
		public double[] getParameter(MatchingPath path) {
			return new double[] { direction - path.rotation };
		}
		
		public double evaluateFinalError(MatchingPath path) {
			double d = direction - path.rotation;
			return d*d;
		}
	}
}
