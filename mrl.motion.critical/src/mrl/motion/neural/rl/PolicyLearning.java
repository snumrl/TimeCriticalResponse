package mrl.motion.neural.rl;

import static mrl.util.Configuration.MOTION_TRANSITON_MARGIN;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.dp.DynamicProgrammingMulti.GoalDimensionInfo;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.gmm.GMMConfig;
import mrl.motion.neural.gmm.GMMConfig.GMMGoal;
import mrl.motion.neural.gmm.GMMConfig.GMMGoalGenerator;
import mrl.motion.neural.gmm.GMMJumpConfig;
import mrl.motion.neural.gmm.GMMLocoActionConfig;
import mrl.motion.neural.gmm.GMMLocoConfig;
import mrl.motion.neural.gmm.GMMStuntLocoConfig;
import mrl.motion.neural.gmm.GMMStuntOnlyConfig;
import mrl.motion.neural.rl.MFeatureMatching.MatchResult;
import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;
import mrl.motion.neural.rl.PolicyEvaluation.MatchingPath;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.util.AdaptiveWeight;
import mrl.util.IterativeRunnable;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.MeanCounter;
import mrl.util.Pair;
import mrl.util.TextUtil;
import mrl.util.TimeChecker;
import mrl.util.Utils;

public class PolicyLearning {
	
	public static int BATCH_SIZE = 256;
	public static int TIME_LIMIT_BASE = 32;
	public static boolean useTiming = true;
	public static boolean useRawReward = true;
	public static boolean useDP = false;
	public static boolean useDPFilter = true;
	public static boolean useProceedNearActions = true;
	public static boolean useAdaptiveWeightOnFiltering = false;
	public static boolean enableSequentialMove = false;
	public static boolean findMatchByDP = false;
	
	
	public GMMConfig config;
	public String policyName;
	public TransitionData tData;
	public MFeatureMatching matching;
	public RLPyhtonModel python;
	
	private String folder;
	
	public PolicyLearning(GMMConfig config, boolean useCPU) {
		this.config = config;
		this.policyName = config.getDataFolder() + "_" + config.name;
		tData = config.getTransitionData();
		matching = new MFeatureMatching(tData.database);
		int fSize = matching.pivot.data.length;
		System.out.println("feature dimesion size :: " + fSize);
		int stateSize = fSize + config.getControlParameterSize() + (useTiming ? 3 : 0);
		int actionSize = fSize + (useDP ? 1 : 0);
//		int actionSize = fSize + (useTiming ? 1 : 0);
		python = new RLPyhtonModel(config.getDataFolder(), stateSize, actionSize, useCPU);
		
		folder = "train_rl/" + this.policyName;
		new File(folder).mkdirs();
		if (new File(folder).listFiles().length > 0) {
			System.out.println("restore RL");
			python.restore(folder + "/ckpt");
		}
		
//		tData.preCalculateTransitionDistances();
	}
	
	public static boolean isTest = false;
	public ArrayList<GMMQueryLog> queryLogList;
	public ArrayList<Object> testEpisodes(int maxIter) {
		isTest = true;
		prepareStatistics();
		RLAgent agent = new RLAgent(tData, matching);
		
		TimeBasedList<Object> controlParameters = new TimeBasedList<Object>();
		ArrayList<Motion> motionList = new ArrayList<Motion>();
		queryLogList = new ArrayList<GMMQueryLog>();
		Matrix2d startTransform = Matrix2d.identity();
		for (int i = 0; i < maxIter; i++) {
			RL_State state = agent.prepareState();
			int frame = motionList.size() + agent.path.time;
			int remainTime = agent.goal.timeLimit - agent.path.time;
			MotionQuery action = python.getMeanAction(state);
			agent.proceedStep(state, action);
			agent.qLog.frame = frame;
			agent.qLog.goal = agent.goal;
			agent.qLog.remainTime =  remainTime;
			queryLogList.add(agent.qLog);
			if (agent.isFinished()) {
				System.out.println("frame : " + motionList.size() + " : " + agent.goal + " : r" +  agent.path.rotation + " : " + Math.toDegrees(Math.sqrt(agent._rawFinalError)));
				int start = motionList.size() == 0 ? 0 : 1;
				motionList.addAll(Utils.cut(agent.path.motionList, start, -1));
				for (int j = start; j < agent.path.transformList.size(); j++) {
					Matrix2d t = new Matrix2d(agent.path.transformList.get(j));
					t.mul(startTransform, t);
					controlParameters.add(agent.goal.getParameterObject(startTransform, t));
				}
				Matrix2d currentTransform = new Matrix2d(agent.path.transform);
				currentTransform.mul(startTransform, currentTransform);
				startTransform = currentTransform;
			}
		}
		int[][] path = Motion.getPath(motionList);
		System.out.println("--------- path");
//		for (int[] p : path) {
//			System.out.println(Arrays.toString(p));
//		}
		MotionSegment segment = MotionSegment.getPathMotion(tData.database.getMotionList(), path);
		MotionData mData = new MotionData(segment.getMotionList());
		FootSlipCleanup.clean(mData);
		return Utils.toList(new Object[] { mData, controlParameters });
	}
	
	public static class GMMQueryLog{
		public int frame;
		
		public GMMGoal goal;
		public int remainTime;
		
		public MotionFeature originFeature;
		public MotionFeature queryFeature;
		public MotionFeature selectedFeature;
		
		public MotionFeature byQueryFeature;
		public MotionFeature byDPFeature;
		
		public double dpWeight;
		public MatchResult matchResult;
		public MatchResult dpMatchResult;
	}
	
	
	public void runTraining(int maxIter) {
		new File("logs").mkdirs();
		String logFile = "logs\\" + config.name + ".log";
		TextUtil.openWriter(logFile);
		TimeChecker.instance.enable = true;
		
		System.out.println("Start Training :: ");
		int stepSize = 32;
		for (int i = 0; i < maxIter; i++) {
			System.out.println("Training Iter :: " + policyName + " : " + i);
			double meanReward = collectEpisodes(stepSize);
			TextUtil.write(Utils.toString(i, meanReward) + "\r\n");
			System.out.println("mean reward : " + i + " : " + meanReward);
			TimeChecker.instance.state("runTraining");
			python.runTraining();
			if ((i % 50) == 0) {
				TimeChecker.instance.state("save");
				python.save(folder + "/ckpt");
			}
			TimeChecker.instance.state("other");
			System.out.println("sampleState : " + Arrays.toString(sampleState.state.data));
			System.out.println("sampleAction : " + Arrays.toString(sampleAction.feature.data));
			TimeChecker.instance.print();
//			AABBManager.printCounter();
//			python.save("..\\mrl.python.neural\\train_rl");
		}
		TextUtil.closeWriter();
	}
	
	int sampleStartingPoint(MFeatureMatching matching, int actionType) {
//		return tData.database.findMotion("a_002_2_2.bvh", 471).motionIndex;
		
		boolean isContinuous = (actionType < tData.continuousLabelSize);
		ArrayList<Integer> frames = config.tConstraints.getActionFrames(actionType);
		int size = frames.size();
		while (true) {
			int index = frames.get(MathUtil.random.nextInt(size));
//			if (isContinuous && tData.transitionAfterMIndex[index] < 0) continue;
			if (matching.isTransitionable[index]) {
//				System.out.println("starting point :: " + isContinuous + " : " + tData.database.getMotionList()[index] + " :: " + tData.database.getMotionList()[tData.transitionAfterMIndex[index]]);
				return index;
			}
		}
		
	}
	
	double eSum = 0;
	double teSum = 0;
	int eCount = 0;
	int teCount = 0;
	double rewardSum = 0;
	RL_State sampleState;
	MotionQuery sampleAction;
	ArrayList<MatchingPath> pathList;
	double collectEpisodes(int stepSize) {
		python.clearEpisodes();
		RLAgent[] agents = new RLAgent[BATCH_SIZE];
		for (int i = 0; i < agents.length; i++) {
			agents[i] = new RLAgent(tData, matching);
		}
		
		prepareStatistics();
		eSum = 0;
		eCount = 0;
		teSum = 0;
		teCount = 0;
		rewardSum = 0;
		pathList = new ArrayList<MatchingPath>();
		TimeChecker.instance.state("other");
		for (int step = 0; step < stepSize; step++) {
//			System.out.println("collect step :: " + step);
			RL_State[] states = new RL_State[agents.length];
			for (int i = 0; i < states.length; i++) {
				states[i] = agents[i].prepareState();
			}
			TimeChecker.instance.state("calcAction");
			MotionQuery[] actions = python.calcAction(states);
			TimeChecker.instance.state("proceedStep");
			sampleState = states[0];
			sampleAction = actions[0];
			double[] rewards = new double[states.length];
			double[] finalErrors = new double[states.length];
			double[][] selectedActions = new double[states.length][];
//			Configuration.MAX_THREAD = 1;
			Utils.runMultiThread(new IterativeRunnable() {
				
				@Override
				public void run(int i) {
					Pair<Double, MotionQuery> pair = agents[i].proceedStep(states[i],actions[i]);
					rewards[i] = pair.first;
//					double[] selectedAction = MathUtil.copy(pair.second.data);
//					MathUtil.sub(selectedAction, states[i].state.data);
					double[] selectedAction = MathUtil.copy(pair.second.generated);
					selectedActions[i] = selectedAction;
					finalErrors[i] = agents[i].finalError;
					if (agents[i].isFinished()) {
						synchronized (finalErrors) {
							if (agents[i]._rawFinalError > 0 && Math.abs(agents[i].goal.direction) > Math.toRadians(10) ) {
								eSum += Math.sqrt(agents[i]._rawFinalError);
								eCount++;
							}
							teSum += agents[i].tErrorSum;
							rewardSum += agents[i].rewardSum;
							teCount++;
							pathList.add(agents[i].path);
						}
					}
				}
			}, states.length);
			TimeChecker.instance.state("updateEpisodes");
//			System.out.println("saction :: " + Arrays.toString(selectedActions[0]));
//			System.out.println("origin :: " + Arrays.toString(actions[0].generated));
			python.updateEpisodes(rewards, finalErrors, selectedActions);
		}
		TimeChecker.instance.state("other");
		double tReward = adaptiveWeight.counterList[1].mean();
		double gReward = adaptiveWeight.counterList[2].mean();
//		double rewardSum = statistics[1].mean() + statistics[2].mean();
//		goalRewardWeight = gReward/(tReward + gReward);
//		goalRewardWeight 
		
		double e = (eSum/eCount);
		e = Math.toDegrees(e);
//		e = Math.toDegrees(Math.sqrt(e));
		System.out.println("mean error : " + e + " : " + eCount + " : " + teCount + " : " + (teSum/teCount));
		MatchingPath path = Utils.pickRandom(pathList);
		Motion[] mList = tData.database.getMotionList();
		for (int[] p : path.getPath()) {
			System.out.print(mList[p[0]] + "->" + mList[p[1]] + ":");
		}
		System.out.println();
		System.out.println("tError Sum : " + path.transitionErrorSum);
		for (MeanCounter c : adaptiveWeight.counterList) {
			System.out.println(c.toString());
		}
		System.out.println("dp filter ratio :: " + MFeatureMatching.dpFilterCounter);
		return rewardSum/teCount;
	}
	
	public RL_State getState(MotionFeature motion, double[] controlParameter, double timeLimit) {
//		MotionFeature currentState = matching.getOriginNextDataSafe(motion.motionIndex, false);
////		MotionFeature currentState = matching.getOriginData(path.current.motionIndex, false);
//		RL_State rl_state = new RL_State(currentState, controlParameter, timeLimit);
//		return rl_state;
//		return new RL_State(motion, controlParameter, timeLimit);
		throw new RuntimeException();
	}
	
	public static double rw = (Math.PI * Math.PI / 10000000);
	public static void prepareStatistics() {
		adaptiveWeight.resetCounter();
	}
//	public static MeanCounter[] statistics;
//	static void prepareStatistics() {
//		statistics = new MeanCounter[3];
//		for (int i = 0; i < statistics.length; i++) {
//			statistics[i] = new MeanCounter();
//		}
//	}
//	static double goalRewardWeight = 0.5;
	public static AdaptiveWeight adaptiveWeight = new AdaptiveWeight(new double[] { 2, 2, 0.5 });
	
	
	protected GMMGoal sampleRandomGoal(GMMGoalGenerator generator, Pose2d currentPose, Motion currentMotion) {
		return generator.sampleRandomGoal(currentPose, currentMotion);
	}
	
	//static double rw = (Math.PI / 1000)*(Math.PI / 1000);
	public class RLAgent{
		MFeatureMatching matching;
		public MatchingPath path;
		public GMMGoal goal = null;
		Episode episode = null;
		TransitionData tData;
		Matrix2d totalTransform;
		GMMGoalGenerator goalGenerator;
		double rewardSum = 0;
		public RLAgent(TransitionData tData, MFeatureMatching matching){
			this.tData = tData;
			this.matching = matching;
			goalGenerator = config.makeGoalGenerator();
			
			goal = sampleRandomGoal(goalGenerator, new Pose2d(Pose2d.BASE), null);
			path = new MatchingPath(matching.database.getMotionList()[sampleStartingPoint(matching, goal.actionType)]);
			totalTransform = Matrix2d.identity();
			finalError = 0;
		}
		
		public boolean isFinished() {
			return finalError >= 0;
		}
		
		GMMGoal prevGoal = null;
		MatchingPath prevPath = null;
		
		public RL_State prepareState() {
			if (isFinished()) {
				prevGoal = goal;
				prevPath = path;
				
				goal = sampleRandomGoal(goalGenerator, Pose2d.byBase(totalTransform), path.current);
				episode = new Episode();
				tErrorSum = 0;
				finalError = -1;
				rewardSum = 0;
				path = new MatchingPath(path.current);
				
				if (findMatchByDP) {
					int minReachTime = config.actionDP.searchMinReachTime(path.current.motionIndex+1, goal.actionType);
					System.out.println("minReachTime :: " + Utils.toString(goal.maxSearchTime, minReachTime, 
							config.actionDP.distanceTable[goal.actionType][minReachTime][tData.motionToNodeMap[path.current.motionIndex].index+1],
							config.actionDP.ctdTable[goal.actionType][minReachTime][tData.motionToNodeMap[path.current.motionIndex].index+1]
							));
					minReachTime += 1;
					if (goal.maxSearchTime < minReachTime) {
						goal.increaseTime(minReachTime - goal.maxSearchTime);
					}
				}
				
				proceedNearActions();
//				if (MathUtil.random.nextDouble() < 0.1) {
//					path = new MatchingPath(matching.database.getMotionList()[sampleStartingPoint(matching)]);
//				} else {
//				}
			}
			
			MotionFeature currentState = matching.getOriginNextDataSafe(path.current.motionIndex, false);
//			MotionFeature currentState = matching.getOriginData(path.current.motionIndex, false);
			double[] timing = new double[] { goal.timeLimit, goal.timeLimit - path.time, goal.maxSearchTime - path.time };
			if (goal.dynamicTime >= 0) {
				timing = new double[] { goal.dynamicTime, goal.maxSearchTime, goal.maxSearchTime - path.time };
			}
			RL_State rl_state = new RL_State(currentState, goal.getParameter(path), timing);
			return rl_state;
		}
		
		boolean proceedNearActions() {
			if (!useProceedNearActions) return false;
			
			while (tData.isNearAction[path.current.motionIndex]) {
//				System.out.println("proceed near action : " + path.current);
				path.moveSequential(path.current.next, tData);
				if (checkFinish()) return true;
			}
			return false;
		}
		
		int currentMaxSearchTime() {
			return goal.maxSearchTime - path.time;
		}
		
		double _rawFinalError;
		double finalError;
		double tErrorSum = 0;
		GMMQueryLog qLog;
		public Pair<Double, MotionQuery> proceedStep(RL_State rl_state, MotionQuery query) {
			double tErrorWeight = 0.00001;
			if (isTest) {
				qLog = new GMMQueryLog();
				qLog.originFeature = rl_state.state;
				qLog.queryFeature = query.feature;
//				MatchResult m1 = matching.findMatch(config, goal, path, query, tErrorWeight, false, true);
//				MatchResult m2 = matching.findMatch(config, goal, path, query, tErrorWeight, true, false);
//				if (m1.motionIndex >= 0) qLog.byQueryFeature = matching.featureList.get(m1.motionIndex);
//				if (m2.motionIndex >= 0) qLog.byDPFeature = matching.featureList.get(m2.motionIndex);
//				qLog.dpMatchResult = m2;
			}
			
			MatchResult match = null;
			int seqCount = 0;
			ArrayList<Motion> noMatchHistory = new ArrayList<Motion>();
			boolean isFinished = false;
//			TimeChecker.instance.state("findMatchByAABB");
			while (seqCount <= MOTION_TRANSITON_MARGIN) {
				match = matching.findMatch(config, goal, path, query, tErrorWeight);
//				match = matching.findMatchByAABB(tData, goal, path, query);
				if (match.minDist < Integer.MAX_VALUE) break;
				noMatchHistory.add(path.current);
				seqCount++;
				if (isTest) System.out.println("proceedStep::proceed sequenetial:: " + seqCount + " : " + path.current + " : " + goal);
//				break;
				path.moveSequential(path.current.next, tData);
				if (isTest) System.out.println("proceedStep::after transition :: " + path.current);
				if (isFinished = checkFinish()) {
					match.minDist = matching.featureList.get(match.motionIndex).distance(query.feature.data);
					break;
				}
				if (!isFinished && goal.isFinishable(path)) {
					isFinished = true;
					match.minDist = matching.featureList.get(match.motionIndex).distance(query.feature.data);
				}
			}
			
			if (isTest) {
				qLog.selectedFeature = matching.featureList.get(match.motionIndex);
				qLog.dpWeight = query.dpWeight;
				qLog.matchResult = match;
			}
			
//			TimeChecker.instance.state("proceedStep_post");
//			if (!isFinished && match.minDist >= 100000000) {
//				synchronized (matching) {
//					double minDist = match.minDist;
//					MDatabase database = tData.database;
//					
//					System.out.println("goal : " + goal);
//					System.out.println("remain time :: " + (goal.maxSearchTime - path.time));
//					System.out.println("c action type : " + tData.motionNearActionTypes[path.current.motionIndex] + " : " + tData.database.getTypeList()[path.current.motionIndex]
//											+ " : " + matching.isTransitionable[path.current.motionIndex] 
//													+ " : " + (matching.featureList.get(path.current.motionIndex) == null));
//					System.out.println("min dist :: " + minDist + " : " + path.current + ">" + database.getMotionList()[match.motionIndex] + " : " + seqCount);
//					System.out.println("trace :: --------------");
//					for (Motion p : path.motionList) {
//						System.out.println(p + " : " + tData.motionNearActionTypes[p.motionIndex] + " : " + tData.motionActionTypes[p.motionIndex]);
//						for (int i = 0; i < 60; i++) {
//							double dd = config.actionDP.tAfterDistanceTable[goal.actionType][i][config.tData.motionToNodeMap[p.motionIndex].index];
//							if (dd < Integer.MAX_VALUE) {
//								System.out.println("dp time limit : " + i + " : " + dd);
//								break;
//							}
//						}
//					}
//					System.out.println("---------------");
//					System.out.println("prev goal : " + prevGoal);
//					System.out.println("prev path trace :: ");
//					for (Motion p : prevPath.motionList) {
//						System.out.println(p + " : " + tData.motionNearActionTypes[p.motionIndex] + " : " + tData.motionActionTypes[p.motionIndex]);
//					}
//					System.out.println("---------------");
//					System.out.println("noMatchHistory trace :: ");
//					for (Motion p : noMatchHistory) {
//						System.out.println(p + " : " + tData.motionNearActionTypes[p.motionIndex] + " : " + tData.motionActionTypes[p.motionIndex]);
//					}
//					System.out.println("---------------");
//					new Exception().printStackTrace();
//					System.exit(0);
//				}
//			}
//			Pair<Integer, Double> match2 = matching.findMatch(tData, goal, path, query, false);
//			if (!match.first.equals(match2.first) || Math.abs(match.second - match2.second) > 0.00001) {
//				synchronized (tData) {
//					System.out.println("proceedStep::dismatch:: " + Utils.toString(path.current.motionIndex, match.first, match.second, match2.first, match2.second, Math.abs(match.second - match2.second)));
//					System.out.println(match.second + " , " + match2.second + " , " + Math.abs(match.second - match2.second));
//					matching.aabb.test(query, match2.first);
//					new Exception().printStackTrace();
//					System.exit(0);
//				}
//			}
			
			int mIndex = match.motionIndex;
			boolean isSequential = enableSequentialMove && (mIndex == path.current.motionIndex + 1);
//			isSequential = false;
			MotionFeature targetFeature = null;
			double matchError = match.minDist/query.feature.data.length;
			double transitionError = 0;
			double _te = 0;
//			TimeChecker.instance.state("proceedStep_makeTransition");
			if (!isFinished){
//				MotionFeature sourceNext = matching.getOriginNextDataSafe(path.current.motionIndex, false);
				targetFeature = matching.getOriginData(mIndex, false);
//				transitionError = sourceNext.distance(targetFeature.data);
				Motion prev = path.current;
				_te = path.moveTransition(tData.database.getMotionList()[mIndex], tData);
				isFinished = checkFinish();
				transitionError = _te*tErrorWeight;
				if (_te >= Integer.MAX_VALUE) {
//					if (_te > 10000000) {
					System.out.println("goal :: " + goal);
					path.printTrace(config, goal);
					System.out.println("TError :: " + transitionError + " : " + _te + " : " + prev + " : " + tData.database.getMotionList()[mIndex]);
					System.out.println("dd : " + tData.database.getDist().getDistance(prev.motionIndex + 1, mIndex));
					tData.adjustDistanceTest(0, prev.motionIndex, mIndex);
					System.out.println("-----------------");
					new Exception().printStackTrace();
					System.exit(0);
				}
			} else {
				targetFeature = rl_state.state;
			}
//			double transitionError = path.moveTransition(tData.database.getMotionList()[mIndex], tData);
//			if (transitionError > 1000000) {
//				System.out.println("TError :: " + transitionError + " : " + path.current + " : " + tData.database.getMotionList()[mIndex]);
//			}
//			TimeChecker.instance.state("proceedStep_moveSeq");
			tErrorSum += _te;
			
			if (!isSequential) {
				for (int i = 0; i < MOTION_TRANSITON_MARGIN; i++) {
					if (isFinished) break;
					if (path.current.next == null) {
						throw new RuntimeException();
					}
					path.moveSequential(path.current.next, tData);
					isFinished = checkFinish();
				}
			}
			if (!isFinished) {
				isFinished = proceedNearActions();
			}
			
//			double goalError = goal.evaluateFinalError(path);
//			matchError /= 1;
//			transitionError /= 0.01;
//			goalError /= 100;
			synchronized (adaptiveWeight) {
				adaptiveWeight.add(0, matchError);
//				statistics[1].add(transitionError);
//				statistics[2].add(goalError);
			}
			
//			goalRewardWeight = 0.1;
			double reward = adaptiveWeight.getWeight(0)*matchError + adaptiveWeight.getWeight(1)*transitionError;
//			double reward = 0.1*matchError + ((1-goalRewardWeight)*transitionError + goalRewardWeight*goalError);
//			reward *= -0.01;
			reward *= -1;
			if (!useRawReward) {
				reward = Math.exp(reward);
			}
			
//			reward = reward*10;
//			double reward = Math.exp(-matchError) + 100*Math.exp(-transitionError) + 100*Math.exp(-goalError);
			
			if (!isFinished && currentMaxSearchTime() <= 0) {
				System.out.println("time over :: " + currentMaxSearchTime() + " : " + isFinished);
				double minDist = match.minDist;
				MDatabase database = tData.database;
				System.out.println("goal : " + goal);
				System.out.println("remain time :: " + (goal.timeLimit - path.time));
				System.out.println("c action type : " + tData.motionNearActionTypes[path.current.motionIndex]);
				System.out.println("min dist :: " + minDist + " : " + path.current + ">" + database.getMotionList()[match.motionIndex]);
				System.out.println("trace :: --------------");
				for (Motion p : path.motionList) {
					System.out.println(p + " : " + tData.motionNearActionTypes[p.motionIndex] + " : " + tData.motionActionTypes[p.motionIndex]);
					System.out.println("dp :: " + config.actionDP.tAfterDistanceTable[goal.actionType][3][config.tData.motionToNodeMap[p.motionIndex].index]);
				}
				System.out.println("---------------");
				System.out.println("prev goal : " + prevGoal);
				System.out.println("prev path trace :: ");
				for (Motion p : prevPath.motionList) {
					System.out.println(p + " : " + tData.motionNearActionTypes[p.motionIndex] + " : " + tData.motionActionTypes[p.motionIndex]);
				}
				System.out.println("---------------");
				System.out.println("noMatchHistory trace :: ");
				for (Motion p : noMatchHistory) {
					System.out.println(p + " : " + tData.motionNearActionTypes[p.motionIndex] + " : " + tData.motionActionTypes[p.motionIndex]);
				}
				System.out.println("---------------");
				throw new RuntimeException();
//					new Exception().printStackTrace();
//					System.exit(0);
			}
			
//			double reward = -transitionError*rw;
//			double goalError = -goal.evaluateFinalError(path);
//			TimeChecker.instance.state("proceedStep_fninish");
			if (isFinished) {
//				System.out.println("## GOAL FINISHED :: " + path.current + " : " + goal);
				double goalError = goal.evaluateFinalError(path, 0, Pose2d.BASE, path.current);
				_rawFinalError = goalError;
				if (path.time > goal.timeLimit) {
					double ratio = 1 + (path.time - goal.timeLimit)/(double)goal.timeLimit;
					goalError *= ratio;
				}
				double tErrorSum = path.transitionErrorSum*tErrorWeight;
				synchronized (adaptiveWeight) {
					if (goalError > 0 && Math.abs(goal.direction) > Math.toRadians(10)) {
						adaptiveWeight.add(2, goalError);
					}
					adaptiveWeight.add(1, tErrorSum);
				}
				
				if (useRawReward) {
					reward += -(adaptiveWeight.getWeight(2)*goalError);
					finalError = 1;
				} else {
					finalError = Math.exp(-(adaptiveWeight.getWeight(2)*goalError));
				}
//				finalError = Math.exp(-(goalRewardWeight*goalError + (1-goalRewardWeight)*tErrorSum));
				totalTransform.mul(totalTransform, path.transform);
//				System.out.println("Final Error : " + finalError);
//				reward += -finalError;
				
//				goal = null;
//				lastPath = path;
//				path = new MatchingPath(matching.database.getMotionList()[path.current.motionIndex]);
			}
//			TimeChecker.instance.state("proceedStep_post_other");
//			targetFeature = query.feature;
			rewardSum += reward;
			return new Pair<Double, MotionQuery>(reward, query);
		}
		
		private boolean checkFinish() {
			return goal.isFinished(path);
		}
		
//		Episode proceedStep(RL_State rl_state, MotionQuery query, double logprob) {
//			MotionQuery control = query;
//			int mIndex = matching.findMatch(control);
//			
//			double transitionError = path.moveTransition(tData.database.getMotionList()[mIndex], tData);
//			
//			timeLimit--;
//			for (int i = 0; i < MOTION_TRANSITON_MARGIN; i++) {
//				if (path.current.next == null) {
//					throw new RuntimeException();
//				}
//				timeLimit--;
//				path.moveSequential(path.current.next, tData);
//			}
//			
//			EpisodeTuple tuple = new EpisodeTuple();
//			tuple.state = rl_state.toArray();
//			tuple.action = control.toArray();
//			tuple.reward = -transitionError*rw;
//			tuple.value = getValueFunction(rl_state);
//			tuple.logprob = logprob;
//			episode.tupleList.add(tuple);
//			
//			if (timeLimit <= 0) {
//				double finalError = goal.evaluateFinalError(path);
//				System.out.println("Final Error : " + finalError);
//				tuple.reward += -finalError;
//				
//				goal = null;
//				return episode;
//			} else {
//				return null;
//			}
//		}
	}
	
	public static class RL_State{
		MotionFeature state;
		double[] goal;
		double[] timeData;
		
		public RL_State(MotionFeature state, double[] goal, double[] timeData) {
			this.state = state;
			this.goal = goal;
			this.timeData = timeData;
			for (int i = 0; i < timeData.length; i++) {
				timeData[i] /= AgilityModel.GOAL_TIME_LIMIT;
			}
		}
		
		public double[] toArray() {
			if (useTiming) {
				return MathUtil.concatenate(state.data, goal, timeData);
			} else {
				return MathUtil.concatenate(state.data, goal);
			}
		}
	}
	
	static class Episode{
		ArrayList<EpisodeTuple> tupleList = new ArrayList<EpisodeTuple>();
		
		public Episode() {
		}
	}
	
	static class EpisodeTuple{
		//episodes[j].push(states[j], actions[j], rewards[j], values[j], logprobs[j])
		double[] state;
		double[] action;
		double reward;
		double value;
		double logprob;
	}
	
//	public static String base_div_t() {
//		useTiming = true;
//		String name = "base_new_e_div_" + div;
//		if (useTiming) name += "_t";
//		TIME_LIMIT_BASE = 18;
//		return name;
//	}
	
	public static GMMConfig base_div_t_sr() {
		useTiming = true;
		String name = "base_div_t_sr";
		if (useTiming) name += "_t";
		return new GMMLocoConfig(name).setDataFolder("ue_loco_only", "t_pose_ue2.bvh");
	}
	
	public static GMMConfig base_div_t_rMul() {
		useTiming = true;
		String name = "base_div_t_rMul";
		if (useTiming) name += "_t";
		return new GMMLocoConfig(name).setDataFolder("ue_loco_only", "t_pose_ue2.bvh");
	}
	public static GMMConfig base_div_rMul() {
		useTiming = false;
		String name = "base_div_rMul";
		if (useTiming) name += "_t";
		return new GMMLocoConfig(name).setDataFolder("ue_loco_only", "t_pose_ue2.bvh");
	}
	public static GMMConfig loco_action() {
		useTiming = true;
		String name = "loco_action";
		if (useTiming) name += "_t";
		return new GMMLocoActionConfig(name).setDataFolder("rl_dc_loco", "t_pose_ue2.bvh");
	}
	public static GMMConfig stunt_loco() {
		useTiming = true;
		useRawReward = true;
		useDP = true;
		useProceedNearActions = true;
		GMMStuntLocoConfig.LOCOMOTION_RATIO = 0.8;
		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
		
//		GMMConfig.CONTROL_ACTION_DIRECTION = true;
//		GMMStuntLocoConfig.LOCOMOTION_RATIO = 0.8;
		
		useDP = false;
		useDPFilter = true;
		GMMConfig.CONTROL_ACTION_DIRECTION = false;
		
		String name = "stunt_loco";
		if (useTiming) name += "_t";
		if (useRawReward) name += "_rr";
		if (useDP) name += "_dp";
		if (useProceedNearActions) name += "_pn";
		if (GMMConfig.CONTROL_ACTION_DIRECTION) name += "_ad";
		if (useDPFilter) name += "_dpf";
		return new GMMStuntLocoConfig(name).setDataFolder("stunt_loco", "t_pose_ue2.bvh");
	}
	public static GMMConfig stunt_only(boolean directionControl, String dataPostfix) {
		useTiming = true;
		useRawReward = true;
		useProceedNearActions = true;
		AgilityModel.TIME_EXTENSION_RATIO = 0.5;

		useDP = false;
		useDPFilter = true;
		if (directionControl) {
			GMMConfig.CONTROL_ACTION_DIRECTION = true;
			adaptiveWeight = new AdaptiveWeight(new double[] { 10, 2, 1 });
		} else {
			GMMConfig.CONTROL_ACTION_DIRECTION = false;
			adaptiveWeight = new AdaptiveWeight(new double[] { 10, 1, 10 });
			enableSequentialMove = true;
			findMatchByDP = true;
		}
		
		String name = "stunt_only";
		if (useTiming) name += "_t";
		if (useRawReward) name += "_rr";
		if (useDP) name += "_dp";
		if (useProceedNearActions) name += "_pn";
		if (GMMConfig.CONTROL_ACTION_DIRECTION) name += "_ad";
		if (useDPFilter) name += "_dpf";
		if (directionControl) name += "_dc";
		name += dataPostfix;
		return new GMMStuntOnlyConfig(name).setDataFolder("stunt_new_label_sOnly" + dataPostfix, "t_pose_ue2.bvh");
	}
	
	public static GMMConfig jump(String dataPostfix) {
		useTiming = true;
		useRawReward = true;
		useDP = true;
		useProceedNearActions = true;
		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
		useAdaptiveWeightOnFiltering = true;
		
		enableSequentialMove = true;
		
		useDP = false;
//		useDPFilter = false;
		useDPFilter = true;
		GMMJumpConfig.LOCOMOTION_RATIO = 0.7;
		adaptiveWeight = new AdaptiveWeight(new double[] { 10, 1, 1.5 });
		
		String name = "jump";
		if (useTiming) name += "_t";
		if (useRawReward) name += "_rr";
		if (useDP) name += "_dp";
		if (useProceedNearActions) name += "_pn";
		if (GMMConfig.CONTROL_ACTION_DIRECTION) name += "_ad";
		if (useDPFilter) name += "_dpf";
		if (useAdaptiveWeightOnFiltering) name += "_af";
		name += dataPostfix;
		return new GMMJumpConfig(name).setDataFolder("jump_jog_ret_ue4" + dataPostfix, "t_pose_ue2.bvh");
	}
	
	public static GMMConfig jump(int tScale, String dataPostfix, boolean noEditing) {
		GMMConfig config = jump(dataPostfix);
		config.name += "_t" + tScale;
		GMMConfig.TIME_RATIO = tScale/100d;
		
		GMMJumpConfig.USE_DYNAMIC_TIME = true;
		if (GMMJumpConfig.USE_DYNAMIC_TIME) {
			config.name += "_dyt";
			AgilityModel.TIME_EXTENSION_RATIO = 0.33;
		} else {
			if (tScale <= 60) {
				AgilityModel.TIME_EXTENSION_RATIO = 0.5;
			} else /*if (tScale <= 75) */{
				AgilityModel.TIME_EXTENSION_RATIO = 0.4;
			}
		}
//		else {
//			AgilityModel.TIME_EXTENSION_RATIO = 0.25;
//		}
		if (noEditing) {
			AgilityModel.TIME_EXTENSION_RATIO = 0;
			config.name += "_nmn";
		}
		return config;
	}
	
	public static void main(String[] args) {
		
		//String name = "base6";
//		String name = "base7";
//		TIME_LIMIT_BASE = 18;
//		String name = "base8";
//		TIME_LIMIT_BASE = 30;
		
//		String name = "base_e_1";
//		TIME_LIMIT_BASE = 18;
//		rw = (Math.PI * Math.PI / 10000);
		
//		GMMConfig config = PolicyLearning.jump(60, "_short05", false);
//		GMMConfig config = PolicyLearning.jump(80, "_short05", true);
		GMMConfig config = PolicyLearning.jump(100, "_short05", false);
		AgilityModel.TIME_EXTENSION_RATIO = 0.33;
		
		
//		GMMConfig config = stunt_only(true);
//		GMMConfig config = stunt_loco();
//		GMMConfig config = loco_action();
//		GMMConfig config = base_div_t_sr();
//		GMMConfig config = base_div_rMul();
//		GMMConfig config = base_div_t_rMul();
//		String name = base_div_t();
		
		PolicyLearning learning = new PolicyLearning(config, false);
		learning.runTraining(100000);
	}
}
