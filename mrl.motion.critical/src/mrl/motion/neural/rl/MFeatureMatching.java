package mrl.motion.neural.rl;

import java.util.ArrayList;
import java.util.Arrays;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionActionDP;
import mrl.motion.dp.TransitionData;
import mrl.motion.dp.TransitionActionDP.TimeCriticalTable;
import mrl.motion.dp.TransitionData.TransitionNode;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.data.DataExtractor;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.neural.gmm.AABBManager;
import mrl.motion.neural.gmm.AABBManager.ValidityChecker;
import mrl.motion.neural.gmm.GMMConfig;
import mrl.motion.neural.gmm.GMMConfig.GMMGoal;
import mrl.motion.neural.rl.PolicyEvaluation.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.MeanCounter;
import mrl.util.Pair;
import mrl.util.Utils;

public class MFeatureMatching {
	
	public static double TRANSITION_ERROR_RATIO = 0.00001;

	MDatabase database;
	ArrayList<PositionMotion> pmList;
	
	public static String[] baseJointForPosition;
	public static String[] baseJointForOrientation;
	public static String[] futureJointForPosition;
	public static String[] futureJointForOrientation;
	public static int[] futurePoseIndices;
	public static int[] futureTrajectoryIndices;
	
	
	public ArrayList<MotionFeature> featureList;
	public boolean[] isTransitionable;
	public Normalizer normal;
	public MotionFeature pivot;
	
	public AABBManager aabb;
	
	public MFeatureMatching(MDatabase database) {
		this.database = database;
		
		setConfigurations_ue();
		
		pmList = new ArrayList<PositionMotion>();
		for (Motion m : database.getMotionList()) {
			pmList.add(new PositionMotion(m));
		}
		
		Motion[] totalMotions = database.getMotionList();
		featureList = new ArrayList<MotionFeature>();
		for (Motion motion : totalMotions) {
			featureList.add(makeFeature(motion));
		}
		
		for (MotionFeature feature : featureList) {
			if (feature != null) {
				pivot = feature;
				break;
			}
		}
		
		isTransitionable = new boolean[featureList.size()];
		int checkSize = Configuration.MOTION_TRANSITON_MARGIN;
		for (int i = 0; i < isTransitionable.length; i++) {
			boolean isValid = true;
			for (int offset = 0; offset <= checkSize; offset++) {
				int idx = i + offset;
				if (idx >= isTransitionable.length) continue;
				if (featureList.get(idx) == null) {
					isValid = false;
					break;
				}
			}
			if (totalMotions[i].frameIndex < Configuration.MOTION_TRANSITON_MARGIN) isValid = false;
			isTransitionable[i] = isValid;
		}
		
		ArrayList<double[]> dataList = new ArrayList<double[]>();
		for (MotionFeature feature : featureList) {
			if (feature == null) continue;
			dataList.add(feature.data);
		}
		double[][] stats = MathUtil.getStatistics(dataList);
		double[][] meanAndStd = new double[2][dataList.get(0).length];
		for (int i = 0; i < stats.length; i++) {
			meanAndStd[0][i] = stats[i][0];
			meanAndStd[1][i] = Math.max(0.0001, stats[i][1]);
		}
		DataExtractor.STD_LIMIT = -1;
		normal = new Normalizer(meanAndStd, meanAndStd, dataList, dataList);
		for (MotionFeature feature : featureList) {
			if (feature == null) continue;
			feature.data = normal.normalizeY(feature.data);
		}
		
		aabb = new AABBManager(featureList);
	}
	
	public MotionFeature getOriginData(int motionIndex, boolean doDenormalize) {
		MotionFeature feature = featureList.get(motionIndex);
		if (doDenormalize) feature = getDenormalizedFeature(feature);
		if (feature == null) {
			System.out.println("null!! : " + motionIndex + database.getMotionList().length);
		}
		return feature;
	}
	public MotionFeature getOriginNextDataSafe(int motionIndex, boolean doDenormalize) {
		MotionFeature feature = featureList.get(motionIndex+1);
		if (feature == null) feature = featureList.get(motionIndex);
		if (doDenormalize) feature = getDenormalizedFeature(feature);
		if (feature == null) {
			System.out.println("null!! : " + motionIndex + database.getMotionList().length);
		}
		return feature;
	}
	
	private MotionFeature getDenormalizedFeature(MotionFeature feature) {
		double[] data = normal.deNormalizeY(feature.data);
		return new MotionFeature(data);
	}
	
	public int findMatch(MotionQuery query, boolean doNormalize) {
		double minDist = Integer.MAX_VALUE;
		int minIndex = -1;
		double[] data = query.feature.data;
		if (doNormalize) data = normal.normalizeY(data);
		for (int i = 0; i < featureList.size() - Configuration.MOTION_TRANSITON_MARGIN; i++) {
			if (!isTransitionable[i]) continue;
//			if (featureList.get(i) == null) continue;
//			if (featureList.get(i + Configuration.MOTION_TRANSITON_MARGIN) == null) continue;
			double d = featureList.get(i).distance(data);
			if (d < minDist) {
				minDist = d;
				minIndex = i;
			}
		}
//		System.out.println("min dist :: " + minDist + " : " + database.getMotionList()[minIndex]);
		return minIndex;
	}
	

	public Pair<Integer, Double> findMatch(TransitionData tData, int currentMIndex, MotionQuery query, boolean doNormalize) {
//		double minDist = Integer.MAX_VALUE;
//		int minIndex = -1;
//		double[] data = MathUtil.copy(query.feature.data);
//		if (doNormalize) data = normal.normalizeY(data);
//		for (int i = 0; i < featureList.size() - Configuration.MOTION_TRANSITON_MARGIN; i++) {
//			if (!isTransitionable[i]) continue;
////			if (featureList.get(i) == null) continue;
////			if (featureList.get(i + Configuration.MOTION_TRANSITON_MARGIN) == null) continue;
//			double tDist = tData.transitionDistance(currentMIndex, featureList.get(i).motionIndex);
//			if (tDist > 1000000) continue;
//			double d = featureList.get(i).distance(data);
////			d += TRANSITION_ERROR_RATIO * tData.transitionDistance(currentMIndex, featureList.get(i).motionIndex);
//			if (d < minDist) {
//				minDist = d;
//				minIndex = featureList.get(i).motionIndex;
//			}
//		}
//		if (minDist > 10000000) {
//			System.out.println("min dist :: " + minDist + " : " + database.getMotionList()[currentMIndex] + ">" + database.getMotionList()[minIndex]);
//		}
//		return new Pair<Integer, Double>(minIndex, minDist);
		throw new RuntimeException();
	}
	public Pair<Integer, Double> findMatchByAABB(TransitionData tData, GMMGoal goal, MatchingPath path, MotionQuery query) {
		int currentMIndex = path.current.motionIndex;
		boolean[] isValidTransitionExist = new boolean[1];
		int[] checkCallCount = new int[1];
		Pair<MotionFeature, Double> match = aabb.findMatch(query, new ValidityChecker() {
			@Override
			public boolean isValid(int motionIndex) {
				checkCallCount[0] ++;
				if (!isTransitionable[motionIndex]) return false;
				if (!goal.isValidTransition(path, motionIndex)) return false;
				isValidTransitionExist[0] = true;
//				double tDist = tData.transitionDistance(currentMIndex, motionIndex);
//				if (tDist > 10000000) return false;
				return true;
			}
		}, currentMIndex + 1);
		
		int minIndex = match.first.motionIndex;
		double minDist = match.second;
		if (match.second > 100000000 && path.current.toString().startsWith("a_")) {
			synchronized (this) {
				System.out.println("goal : " + goal);
				System.out.println("isvalidtransition exist : " + isValidTransitionExist[0] + " : " + checkCallCount[0]);
				System.out.println("f dist :: " + query.feature.distance(featureList.get(path.current.motionIndex + 1).data));
				System.out.println("query : " + Arrays.toString(query.feature.data));
				System.out.println("feature : " + Arrays.toString(featureList.get(path.current.motionIndex).data));
				System.out.println("remain time :: " + (goal.timeLimit - path.time));
				System.out.println("c action type : " + tData.motionNearActionTypes[path.current.motionIndex]);
				System.out.println("min dist :: " + minDist + " : " + database.getMotionList()[currentMIndex] + ">" + database.getMotionList()[minIndex]);
				System.out.println("trace :: --------------");
				for (Motion p : path.motionList) {
					System.out.println(p + " : " + tData.motionNearActionTypes[p.motionIndex] + " : " + tData.isNearAction[p.motionIndex]);
				}
				System.out.println("---------------");
//				throw new RuntimeException();
//				new Exception().printStackTrace();
//				System.exit(0);
			}
		}
		return new Pair<Integer, Double>(minIndex, minDist);
	}
	
	public MatchResult findMatch(GMMConfig config,GMMGoal goal, MatchingPath path, MotionQuery query, double tErrorWeight) {
		return findMatch(config, goal, path, query, tErrorWeight, false, false);
	}
	
	public static MeanCounter dpFilterCounter = new MeanCounter();
	public MatchResult findMatch(GMMConfig config,GMMGoal goal, MatchingPath path, MotionQuery query, double tErrorWeight, boolean ignoreFeature, boolean ignoreDP) {
		TransitionData tData = config.tData;
		TransitionActionDP actionDP = config.actionDP;
		int tMargin = Configuration.MOTION_TRANSITON_MARGIN;
		int remainTime = goal.timeLimit - path.time;
		int remainMaxTime = goal.maxSearchTime - path.time;
		double minDist = Integer.MAX_VALUE;
		int minIndex = -1;
		double minTDist = -1;
		double[] data = MathUtil.copy(query.feature.data);
//		if (doNormalize) data = normal.normalizeY(data);
		int currentMIndex = path.current.motionIndex;
		boolean isValidTransitionExist = false;
		MatchResult matchResult = null;
		double dpw1 = 1 - query.dpWeight;
		double dpw2 = query.dpWeight;
		int featureSize = query.feature.data.length;
		
		boolean print = PolicyLearning.isTest && !ignoreFeature && !ignoreDP;
		if (tData.motionToNodeMap[currentMIndex] == null) {
			System.out.println("##############");
			path.printTrace(config, goal);
		}
//		print = false;
		int cNodeIndex = tData.motionToNodeMap[currentMIndex].index;
		TimeCriticalTable tcTable = actionDP.getBaseTCTable();
		if (!goal.isActiveAction() && (path.time < AgilityModel.TIME_EXTENSION_MIN_TIME)) {
			tcTable = actionDP.getStartingTCTable();
		}
		double upperBound = tcTable.ctdTable[goal.actionType][remainMaxTime][cNodeIndex];
		double upperBoundOrigin = upperBound;
		
		if (print) System.out.println("before transition :: " + upperBound + " : " + goal + " : rTime=" + remainMaxTime + " : " + path.current);
		try {
			if (print) {
				System.out.println("if proceed seq :: " + tcTable.ctdTable[goal.actionType][remainMaxTime-1][cNodeIndex+1] + " : " 
					+ tcTable.tAfterDistanceTable[goal.actionType][remainMaxTime-1][cNodeIndex+1]
					+ " : " + tData.isNearAction[currentMIndex] + " : " + tData.isNearAction[currentMIndex+1]);
				System.out.println("qwrawr prev : " 
					+ tcTable.ctdTable[goal.actionType][remainMaxTime+Configuration.MOTION_TRANSITON_MARGIN][cNodeIndex-Configuration.MOTION_TRANSITON_MARGIN]
							+ ":" + tcTable.ctdTable[goal.actionType][remainMaxTime][cNodeIndex-1]
									+ ":" + tcTable.ctdTable[goal.actionType][remainMaxTime-1][cNodeIndex]
											+ ":" 	+ tcTable.ctdTable[goal.actionType][remainMaxTime][cNodeIndex+1]
							);
				
			}
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		upperBound = tErrorWeight*upperBound;
		if (print) System.out.println("ubb : " + upperBound);
		if (tcTable.ctdFinalMIndexTable[goal.actionType][remainMaxTime][cNodeIndex] < 0) {
			System.out.println("goal:: " + goal);
			System.out.println(Utils.toString(remainMaxTime, path.time,  tData.database.getMotionList()[currentMIndex]));
			System.out.println("NONONO : " + Utils.toString(tcTable.ctdRotationTable[goal.actionType][remainMaxTime][cNodeIndex], 
					tcTable.ctdPoseTable[goal.actionType][remainMaxTime][cNodeIndex]));
			int margin = 5;
			for (int i = -margin; i <= margin; i++) {
				System.out.println(Utils.toString(i, tData.database.getMotionList()[tData.tNodeList[cNodeIndex+i].motionIndex()], tcTable.ctdFinalMIndexTable[goal.actionType][remainMaxTime][cNodeIndex+i]));
			}
			throw new RuntimeException();
		}
		double spatialError = goal.evaluateFinalError(path, 
				tcTable.ctdRotationTable[goal.actionType][remainMaxTime][cNodeIndex], 
				tcTable.ctdPoseTable[goal.actionType][remainMaxTime][cNodeIndex],
				tData.database.getMotionList()[tcTable.ctdFinalMIndexTable[goal.actionType][remainMaxTime][cNodeIndex]]);
		double w_plausible = 1;
		double w_task = 1;
		if (PolicyLearning.useAdaptiveWeightOnFiltering & goal.isActiveAction()) {
			w_plausible = PolicyLearning.adaptiveWeight.getWeight(1);
			w_task = PolicyLearning.adaptiveWeight.getWeight(2);
		}
		upperBound = w_plausible*upperBound + w_task*spatialError;
//		if (!goal.isActiveAction()) {
//			upperBound += tErrorWeight*200;
//		}
		if (print) System.out.println("ubb2 : " + upperBound + " : " + spatialError + " : " + spatialError/tErrorWeight);
		double tAdapWeight = tErrorWeight;
		int checked = 0;
		int filterByDP = 0;
		double mintTAfterError = Integer.MAX_VALUE;
		double minActionDist2 = Integer.MAX_VALUE;
		StringBuilder errorLog = new StringBuilder();
		
		for (int findIter = 0; findIter < 2; findIter++) {
			
		for (TransitionNode tNode : tData.tNodeList) {
			int targetMIndex = tNode.motionIndex();
			
//			boolean print2 = database.getMotionList()[currentMIndex].toString().equals("s_008_1_2:119");
//								&& targetMIndex == currentMIndex + 1;
//			boolean print2 = false;
//			boolean print3 = database.getMotionList()[currentMIndex].toString().equals("s_019_1_2:310");
//			boolean print3 = false;
//			boolean print3 = database.getMotionList()[currentMIndex].toString().equals("s_009_2_2:176");
//			print3 = false;
			
			boolean isSequential = PolicyLearning.enableSequentialMove && (targetMIndex == currentMIndex + 1);
			double estimatedError;
			double estimatedError2 = -1;
			double tError = 0;
			double actionDist2 = -1;
			if (isSequential) {
				estimatedError = tcTable.ctdTable[goal.actionType][remainMaxTime-1][tNode.index];
				actionDist2 = estimatedError;
			} else {
				tError = config.tData.transitionDistance(currentMIndex, targetMIndex);
				actionDist2 = tcTable.tAfterDistanceTable[goal.actionType][remainMaxTime-1][tNode.index];
				estimatedError = tError + actionDist2; 
				try {
				estimatedError2 = tError + tcTable.ctdTable[goal.actionType][remainMaxTime-1-Configuration.MOTION_TRANSITON_MARGIN][tNode.index+Configuration.MOTION_TRANSITON_MARGIN];
				} catch (ArrayIndexOutOfBoundsException e) {
				}
			}
			mintTAfterError = Math.min(mintTAfterError, estimatedError);
			if (estimatedError <= upperBoundOrigin+400) {
				errorLog.append("\r\n" + "print 33 : " + database.getMotionList()[targetMIndex] + " : " + isSequential + " : " + estimatedError + " : " + upperBoundOrigin + " : " + estimatedError2) ;
				errorLog.append("\r\n" + "isvalid :: " + isTransitionable[targetMIndex] + " : " + goal.isValidTransition(path, targetMIndex));
			}
			
			if (estimatedError >= Integer.MAX_VALUE) {
				continue;
			}
			if (findIter == 0) {
				if (PolicyLearning.useDPFilter && !PolicyLearning.findMatchByDP) {
					if (w_plausible*estimatedError*tAdapWeight > upperBound) {
						continue;
					}
				}
			}
				
			if (!isSequential && !isTransitionable[targetMIndex]) continue;
			if (!goal.isValidTransition(path, targetMIndex)) continue;
			
			isValidTransitionExist = true;
//			if (print2) System.out.println("--print2--");
//			if (goal.isActiveAction()) {
//				int dTime = config.tData.actionDirectTime[goal.actionType][targetMIndex];
//				if (dTime + path.time < goal.minSearchTime) {
////					System.out.println("pass by minSearchTime");
//					continue;
//				}
//			}
//			
//			
//			
//			
////			double actionDist1 = Integer.MAX_VALUE;
////			if (remainTime > 0) {
////				actionDist1 = actionDP.tAfterDistanceTable[goal.actionType][remainTime-1][tNode.index];
////			}
//			
//			if (print3) {
//				if (actionDist2 < Integer.MAX_VALUE) {
//					System.out.println("print 33 : " + database.getMotionList()[targetMIndex] + " : " + actionDist2 + " : " + tError + " : " + upperBoundOrigin + " : " + (tError + actionDist2)) ;
//				}
//			}
//			if (print2) System.out.println("--print2-- 2 : " + Utils.toString(tError, actionDist2, (tError + actionDist2), upperBoundOrigin, actionDP.ctdTable[goal.actionType][remainMaxTime-1][cNodeIndex+1]));
//			if (PolicyLearning.useDPFilter) {
//				if ((tError + actionDist2)*tAdapWeight > upperBound) {
//					if (print2) System.out.println("--print2-- filterByDP : " + Utils.toString((tError + actionDist2)*tAdapWeight, upperBound, 
//							tAdapWeight*upperBoundOrigin,
//							tErrorWeight*upperBoundOrigin
//									));
//					filterByDP++;
//					continue;
//				}
//			}
//			if (actionDist2 >= Integer.MAX_VALUE) {
//				continue;
//			}
//			double dAction = tErrorWeight*(tError + Math.min(actionDist1, actionDist2*(1+AgilityModel.TIME_EXTENSION_RATIO)));
//			if (print2) System.out.println("--print2-- check : " + dAction);
//			if (ignoreFeature || (PolicyLearning.useDP && !ignoreDP)) {
////				d += dAction*query.dpWeight;
//				d += tError + actionDist2;
////				d = dpw1*d + dpw2*dAction;
//			}
			checked++;
//			if (print3) {
//				System.out.println("print 333333 :: " + estimatedError + " : " + minDist);
//			}
			
			double featureDist = featureList.get(targetMIndex).distance(data)/featureSize;
			double d = featureDist;
			if (ignoreFeature) d = 0;
			if (PolicyLearning.findMatchByDP) {
				d = estimatedError;
			}
			if (d < minDist) {
				minDist = d;
				minTDist = tError;
				minActionDist2 = actionDist2;
				minIndex = targetMIndex;
				matchResult = new MatchResult(minIndex, minDist, d, actionDist2, actionDist2, d, tError);
			}
		}
		
		if (minDist < Integer.MAX_VALUE) break;
		}
		if (print) {
			if (minIndex < 0) {
				System.out.println("findMatch::preceed sequential : " + tcTable.ctdTable[goal.actionType][remainMaxTime-1][cNodeIndex+1] + " : mtaftererror=" + mintTAfterError);
				System.out.println("isValidTransitionExist : " + isValidTransitionExist + " , " + PolicyLearning.findMatchByDP);
				System.out.println("##check: " + database.getMotionList()[currentMIndex] + " : " + isTransitionable[currentMIndex+1] + " : " + goal.isValidTransition(path, currentMIndex+1) 
				+ " : isNearAction:" + tData.isNearAction[currentMIndex+1] + " : " + tData.motionActionTypes[currentMIndex+1] + " : " + (featureList.get(currentMIndex+1) == null)
				+ " : " + tData.database.getTypeList()[currentMIndex+1]);
				System.out.println("--check2-- 2 : " + Utils.toString(upperBoundOrigin, tcTable.ctdTable[goal.actionType][remainMaxTime-1][cNodeIndex+1]));
				System.out.println(errorLog);
			} else {
//				System.out.println("after min dist :: " + (actionDP.ctdTable[goal.actionType][remainMaxTime][cNodeIndex] - (minActionDist2 + minTDist)) + " : " + minDist + " : " + minActionDist2 + " : " + minTDist + " : " + path.current + "->" + tData.database.getMotionList()[minIndex]);
//				if (actionDP.ctdTable[goal.actionType][remainMaxTime][cNodeIndex] != (minActionDist2 + minTDist)) {
//					int tNodeIndex = tData.motionToNodeMap[minIndex].index;
//					System.out.println("tafter :: " + actionDP.ctdTable[goal.actionType][remainMaxTime-1][tNodeIndex] + " : " + actionDP.tAfterDistanceTable[goal.actionType][remainMaxTime-1][tNodeIndex]);
//					throw new RuntimeException();
//				}
			}
//			int tNodeIndex = tData.motionToNodeMap[minIndex].index;
//			System.out.println("after min dist2 :: " + actionDP.ctdTable[goal.actionType][remainMaxTime-5][cNodeIndex+5] + " : " + actionDP.tAfterDistanceTable[goal.actionType][remainMaxTime][tNodeIndex]);
//			System.out.println("after min dist2 :: " + actionDP.ctdTable[goal.actionType][remainMaxTime-4][cNodeIndex+4] + " : " + actionDP.tAfterDistanceTable[goal.actionType][remainMaxTime-1][tNodeIndex]);
		}
		if (minIndex < 0) {
			minIndex = path.current.motionIndex+1;
			matchResult = new MatchResult(minIndex, minDist, -1, -1, -1, -1,-1);
		}
		synchronized (dpFilterCounter) {
			if (filterByDP + checked > 0) {
				dpFilterCounter.add(filterByDP/(double)(filterByDP + checked));
			}
		}
		return matchResult;
	}
	
	public MotionFeature makeFeature(Motion motion) {
		if (database.getTypeList()[motion.motionIndex] == null) return null;
		ArrayList<double[]> features = new ArrayList<double[]>();
		double[] basePos = MotionDataConverter.getPositionData(baseJointForPosition, pmList.get(motion.motionIndex).pointData);
		double[] baseOri = MotionDataConverter.getOrientationDataWithMatForAll(baseJointForOrientation, motion);
		features.add(basePos);
		features.add(baseOri);
		for (int offset : futurePoseIndices) {
			int frame = Math.min(motion.frameIndex + offset, motion.motionData.motionList.size() - 1);
			Motion m = motion.motionData.motionList.get(frame);
			features.add(new double[] { MathUtil.getTranslation(m.root()).y });
			features.add(MotionDataConverter.getPositionData(futureJointForPosition, pmList.get(m.motionIndex).pointData));
			features.add(MotionDataConverter.getOrientationDataWithMatForAll(futureJointForOrientation, m));
		}
		Pose2d basePose = pmList.get(motion.motionIndex).pose;
		for (int offset : futureTrajectoryIndices) {
			int frame = Math.min(motion.frameIndex + offset, motion.motionData.motionList.size() - 1);
			Motion m = motion.motionData.motionList.get(frame);
			Pose2d p = pmList.get(m.motionIndex).pose;
			p = basePose.globalToLocal(p);
			features.add(p.toArray());
		}
		MotionFeature mf = new MotionFeature(MathUtil.concatenate(Utils.toArray(features)));
		mf.motionIndex = motion.motionIndex;
		return mf;
	}
	
	private void setConfigurations_ue() {
		String[] sparseJoints = new String[] {
//				"Head",
//				"Hips",
//				"Spine",
//				"Spine1",
//				"Spine2",
				
//				"LeftShoulder",
//				"LeftForeArm",
//				"LeftArm",
				"LeftHand",
				
//				"LeftUpLeg",
//				"LeftLeg",
				"LeftFoot",
//				"LeftToe",
				
//				"RightShoulder",
//				"RightForeArm",
//				"RightArm",
				"RightHand",
				
//				"RightUpLeg",
//				"RightLeg",
				"RightFoot",
//				"RightToe",
		};
		
		String[] denseJoints = new String[] {
				"Head",
				"Hips",
//				"Neck",
//				"Spine",
				"Spine1",
//				"Spine2",
				
//				"LeftShoulder",
				"LeftForeArm",
				"LeftArm",
				"LeftHand",
				
				"LeftUpLeg",
				"LeftLeg",
				"LeftFoot",
				"LeftToe",
				
//				"RightShoulder",
				"RightForeArm",
				"RightArm",
				"RightHand",
				
				"RightUpLeg",
				"RightLeg",
				"RightFoot",
				"RightToe",
		};
//		baseJointForPosition = denseJoints;
//		baseJointForOrientation = baseJointForPosition;
		
		baseJointForPosition = new String[0];
		baseJointForOrientation = new String[0];
		
		futureJointForPosition = sparseJoints;
		futureJointForOrientation = new String[0];
		
		futurePoseIndices = new int[] { 0, 10, 20, 30 };
		futureTrajectoryIndices = new int[] { 10, 20, 30 };
	}
	
	public static class MotionFeature{
		public double[] data;
		public int motionIndex = -1;
		private MotionFeature(double[] data) {
			this.data = data;
		}
		
		public double distance(double[] data) {
			double dSum = 0;
			for (int i = 0; i < data.length; i++) {
				double d = (this.data[i] - data[i]);
				dSum += d*d;
			}
			return dSum;
		}
	}
	
	public static class MotionQuery{
		public MotionFeature feature;
		public double[] generated;
		public double dpWeight;
		
		public MotionQuery(MotionFeature currentFeature, double[] data) {
			generated = data;
			if (PolicyLearning.useDP) {
				dpWeight = Utils.last(data);
				dpWeight = (1 + Math.tanh(dpWeight))/2;
//				if (dpWeight > 0) {
//					dpWeight = 1 + dpWeight;
//				} else {
//					dpWeight = Math.exp(dpWeight);
//				}
				data = Utils.cut(data, 0, data.length-2);
			} else {
				dpWeight = 0;
			}
			data = MathUtil.copy(data);
			MathUtil.add(data, currentFeature.data);
			this.feature = new MotionFeature(data);
		}
		
		public double[] toArray() {
			if (PolicyLearning.useDP) {
				return MathUtil.concatenate(feature.data, new double[] { dpWeight });
			} else {
				return feature.data;
			}
		}
	}
	
	public static class MatchResult{
		public int motionIndex;
		public double minDist;
		public double featureDist;
		public double actionDist1;
		public double actionDist2;
		public double dAction;
		public double transitionDist;
		
		public MatchResult(int motionIndex, double minDist, double featureDist, double actionDist1,
				double actionDist2, double dAction, double transitionDist) {
			this.motionIndex = motionIndex;
			this.minDist = minDist;
			this.featureDist = featureDist;
			this.actionDist1 = actionDist1;
			this.actionDist2 = actionDist2;
			this.dAction = dAction;
			this.transitionDist = transitionDist;
		}
	}
}
