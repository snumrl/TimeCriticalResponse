package mrl.motion.dp;

import java.util.ArrayList;
import java.util.List;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MDatabase.FrameType;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.IterativeRunnable;
import mrl.util.MathUtil;
import mrl.util.Matrix2d.RotationMatrix2d;
import mrl.util.Utils;

public class TransitionData {
	
	public static int MAX_SEARCH_TIME = 150;
	public static int NO_BACKWARD_TRANSITION_MARGIN = 8;
	public static int NO_TRANSITION_BEFORE_ACTION_MARGIN = 8;
	public static int NO_TRANSITION_AFTER_ACTION_MARGIN = 4;
	
	public static int STRAIGHT_MARGIN = -1;
	public static double[] STRAIGHT_LIMIT_PRESET = null;

	public MDatabase database;
	public String[] actionLabels;
	public int continuousLabelSize;
	public Motion[] mList;
	private MotionDistByPoints dist;
	
	
	public int[] motionActionTypes;
	public int[] originMotionActionTypes;
	public int[][] actionDirectTime;
	public int[][] actionDirectAfterTime;
	
	public int[] transitionAfterMIndex;
	public boolean[] isNearAction;
	public int[] motionNearActionTypes;
	
	public TransitionNode[] tNodeList;
	public TransitionNode[] motionToNodeMap;
	
	/**
	 * (mIndex-1)에서 mIndex 모션으로 이동하는 transform
	 */
	public RotationMatrix2d[] adjRootTransform;
	public RotationMatrix2d[] tAfterRootTransform;
	
	public boolean[] isStraightMotion;
	
//	public ArrayList<Integer> transitionableIndices;
//	public boolean[] isTransitionableMotion;
//	public boolean[] isContainedMotion;
	
	protected double[][] distCache;
	
	public TransitionData(MDatabase database, String[] actionLabels, int continuousLabelSize) {
		this.database = database;
		this.actionLabels = actionLabels;
		this.continuousLabelSize = continuousLabelSize;
		mList = database.getMotionList();
		transitionAfterMIndex = database.getTransitionAfterMIndex();
		distCache = new double[mList.length][];
		dist = database.getDist();
		
		calcLabelingInfo();
		if (STRAIGHT_MARGIN > 0) {
			calcStraightInfo();
		}
		calcActionReachTime();
		calcTransitionInfo();
		calcTransformInfo();
	}
	
	protected void calcStraightInfo() {
		int margin = STRAIGHT_MARGIN;
		isStraightMotion = new boolean[mList.length];
		if (margin <= 0) {
			for (int i = 0; i < isStraightMotion.length; i++) {
				isStraightMotion[i] = true;
			}
		} else {
			for (MotionData mData : database.getMotionDataList()) {
				for (int i = 0; i < mData.motionList.size() - margin; i++) {
					boolean isStraight = false;
					if (i > margin) {
						double weight = 20;
						if (STRAIGHT_LIMIT_PRESET != null) {
							int action = motionActionTypes[mData.motionList.get(i).motionIndex];
							if (action >= 0 && action < STRAIGHT_LIMIT_PRESET.length) {
								weight = STRAIGHT_LIMIT_PRESET[action];
							}
						}
						
						isStraight = isStraight(Utils.cut(mData.motionList, i-(margin), i + (margin)), weight);
					}
					isStraightMotion[mData.motionList.get(i).motionIndex] = isStraight;
				}
			}
			boolean[] straight = new boolean[isStraightMotion.length];
			for (int i = 0; i < straight.length; i++) {
				boolean isAllStraight = true;
				for (int j = 0; j <= margin*2; j++) {
					int idx = Math.min(i + j, straight.length-1);
					if (mList[i].motionData != mList[idx].motionData) break;;
					if (!isStraightMotion[idx]) {
						isAllStraight = false;
						break;
					}
				}
				straight[i] = isAllStraight;
			}
			isStraightMotion = straight;
		}
		
		for (int i = 0; i < isStraightMotion.length; i++) {
			int action = motionActionTypes[i];
			if (action >= 0 && action < continuousLabelSize) {
				if (!isStraightMotion[i]) {
					motionActionTypes[i] = -1;
				}
			}
		}
	}
	
	public static boolean isStraight(List<Motion> motionList, double degreeLimit) {
		ArrayList<Pose2d> poseList = new ArrayList<Pose2d>();
		for (Motion m : motionList) {
			poseList.add(PositionMotion.getPose(m));
		}
		
		double rotSum1 = 0;
		double rotSum2 = 0;
		int count = 0;
		
		double rotLimit = Math.toRadians(degreeLimit);
		for (int i = 1; i < poseList.size(); i++) {
			Pose2d p = poseList.get(0).globalToLocal(poseList.get(i));
			double angle1 = Math.abs(MathUtil.directionalAngle(Pose2d.BASE.direction, p.position));
			if (Math.abs(angle1) > Math.PI/2) {
				if (angle1 > 0) {
					angle1 -= Math.PI;
				} else {
					angle1 += Math.PI;
				}
			}
			double angle2 = Math.abs(MathUtil.directionalAngle(Pose2d.BASE.direction, p.direction));
			rotSum1 += angle1*angle1;
			rotSum2 += angle2*angle2;
			count++;
//			if (angle1 > rotLimit || angle2 > rotLimit) return false;
		}
		rotSum1 /= count;
		rotSum2 /= count;
		rotLimit = rotLimit*rotLimit;
		if (rotSum1 > rotLimit || rotSum2 > rotLimit) return false;
		return true;
	}
	
	protected void calcTransformInfo() {
		adjRootTransform = new RotationMatrix2d[mList.length];
		tAfterRootTransform = new RotationMatrix2d[mList.length];
		for (int i = 0; i < adjRootTransform.length; i++) {
			Motion m = mList[i];
			if (m.prev != null) {
				adjRootTransform[i] = MotionTransform.getTransform2d(m.prev, m);
			}
			int tAfter = transitionAfterMIndex[i];
			if (tAfter < 0) continue;
			tAfterRootTransform[i] = MotionTransform.getTransform2d(m, mList[tAfter]);
		}
	}
	
	public void preCalculateTransitionDistances() {
		long t = System.currentTimeMillis();
		System.out.println("preCalculateTransitionDistances ::");
		for (int i = 0; i < distCache.length; i++) {
			if ((i % 100) == 0) {
				System.out.println("progress :: " + i + " : " + (System.currentTimeMillis()-t)/1000d);
			}
			transitionDistance(i);
			
		}
		System.out.println("calc time : " + (System.currentTimeMillis()-t)/1000d);
	}
	
	public double transitionDistance(final int sourceMIndex, int targetMIndex) {
		double[] cache = transitionDistance(sourceMIndex);
		return cache[motionToNodeMap[targetMIndex].index];
	}
	
	public double[] transitionDistance(final int sourceMIndex) {
		double[] cache = distCache[sourceMIndex];
		if (cache == null) {
			synchronized (this) {
				if (distCache[sourceMIndex] != null) {
					cache = distCache[sourceMIndex];
				} else {
					int len = tNodeList.length;
					final double[] c = new double[len];
					for (int i = 0; i < len; i++) {
						c[i] = Integer.MAX_VALUE;
					}
					Utils.runMultiThread(new IterativeRunnable() {
						@Override
						public void run(int index) {
							int mIdx = tNodeList[index].motionIndex();
							boolean isNext = mList[sourceMIndex].next == mList[mIdx];
							double d = 0;
							if (!isNext) {
								d = dist.getDistance(sourceMIndex+1, mIdx);
								d = adjustDistance(d, sourceMIndex, mIdx);
								if (d < Integer.MAX_VALUE) {
									d = d*d;
								}
							}
							c[index] = d;
						}
					}, len);
					distCache[sourceMIndex] = c;
					cache = c;
				}
			}
		}
		return cache;
	}
	
	public double adjustDistance(double distance, int currentMotionIndex, int targetMIndex) {
		if (isNearAction[currentMotionIndex] || isNearAction[targetMIndex]) return Integer.MAX_VALUE;
		if (motionToNodeMap[targetMIndex] == null || !motionToNodeMap[targetMIndex].isTransitionable) return Integer.MAX_VALUE;
		
		int backwardLimit = NO_BACKWARD_TRANSITION_MARGIN;
		int nextMotionIndex = currentMotionIndex + 1;
		if (targetMIndex < nextMotionIndex && targetMIndex > nextMotionIndex - backwardLimit
				&& mList[targetMIndex].frameIndex >= backwardLimit) {
			distance += Integer.MAX_VALUE;
		}
		if (nextMotionIndex + 1 == targetMIndex) {
			distance += 1;
		} else {
			distance += 2;
		}
		return distance;
	}
	
	public double adjustDistanceTest(double distance, int currentMotionIndex, int targetMIndex) {
		if (isNearAction[currentMotionIndex] || isNearAction[targetMIndex]) {
			System.out.println("near action error : " + isNearAction[currentMotionIndex] + " : " + isNearAction[targetMIndex]);
			return Integer.MAX_VALUE;
		}
		if (motionToNodeMap[targetMIndex] == null || !motionToNodeMap[targetMIndex].isTransitionable) {
			System.out.println("transitionable error : ");
			return Integer.MAX_VALUE;
		}
		
		int backwardLimit = NO_BACKWARD_TRANSITION_MARGIN;
		int nextMotionIndex = currentMotionIndex + 1;
		if (targetMIndex < nextMotionIndex && targetMIndex > nextMotionIndex - backwardLimit
				&& mList[targetMIndex].frameIndex >= backwardLimit) {
			System.out.println("backward error");
			distance += Integer.MAX_VALUE;
		}
		if (nextMotionIndex + 1 == targetMIndex) {
			distance += 1;
		} else {
			distance += 2;
		}
		return distance;
	}
	
	protected void calcTransitionInfo() {
		FrameType[] typeList = database.getTypeList();
//		transitionableIndices = new ArrayList<Integer>();
		ArrayList<TransitionNode> _tNodeList = new ArrayList<TransitionNode>();
//		isTransitionableMotion = new boolean[typeList.length];
//		isContainedMotion = new boolean[typeList.length];
		motionToNodeMap = new TransitionNode[typeList.length];
		
		int start = -1;
		for (int i = 0; i <= typeList.length; i++) {
			if (i == typeList.length || typeList[i] == null || (i > 0 && mList[i-1].next != mList[i])) {
				if (start >= 0) {
					int end = i;
//					for (int idx = start; idx < end - MOTION_TRANSITON_MARGIN*2; idx++) {
//						transitionableIndices.add(idx);
//						isTransitionableMotion[idx] = true;
//					}
					for (int idx = start; idx < end; idx++) {
						TransitionNode node = new TransitionNode(_tNodeList.size(), mList[idx]);
						_tNodeList.add(node);
						motionToNodeMap[idx] = node;
//						isContainedMotion[idx] = true;
					}
					start = -1;
				}
			} else {
				if (start < 0) {
					start = i;
				}
			}
		}
		tNodeList = Utils.toArray(_tNodeList);
		for (TransitionNode node : tNodeList) {
			int tIndex = transitionAfterMIndex[node.motionIndex()];
			if (tIndex < 0 || motionToNodeMap[tIndex] == null) continue;
			node.isTransitionable = true;
		}
		System.out.println("contained motions :: " + tNodeList.length);
	}
	
	protected void calcLabelingInfo() {
		motionActionTypes = new int[mList.length];
		motionNearActionTypes = new int[mList.length];
		for (int i = 0; i < motionActionTypes.length; i++) {
			motionActionTypes[i] = -1;
			motionNearActionTypes[i] = -1;
		}
		isNearAction = new boolean[mList.length];
		for (MotionAnnotation ann : database.getEventAnnotations()) {
			int actionIdx = actionIndex(ann.type);
			if (actionIdx < 0) continue;
			if (actionIdx < continuousLabelSize) {
				int sIndex = database.findMotion(ann.file, ann.startFrame).motionIndex;
				int eIndex = database.findMotion(ann.file, ann.endFrame).motionIndex;
				for (int i = sIndex; i <= eIndex; i++) {
					motionActionTypes[i] = actionIdx;
					// changed 2021-01-16 : for rl update
					motionNearActionTypes[i] = actionIdx; 
				}
			} else {
				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
				motionActionTypes[mIndex] = actionIdx;
				
				isNearAction[mIndex] = true;
				motionNearActionTypes[mIndex] = actionIdx;
				int prevMargin = NO_TRANSITION_BEFORE_ACTION_MARGIN;
				if (ann.startFrame < ann.interactionFrame) {
					prevMargin = ann.interactionFrame - ann.startFrame + 1;
				}
				int postMargin = NO_TRANSITION_AFTER_ACTION_MARGIN;
				if (ann.endFrame > ann.interactionFrame) {
					postMargin = ann.endFrame - ann.interactionFrame + 1;
				}
				for (int i = 1; i < prevMargin; i++) {
					int idx = mIndex - i;
					if (idx < 0) break;
					if (mList[idx].next != mList[idx+1]) break;
					isNearAction[idx] = true;
					motionNearActionTypes[idx] = actionIdx;
				}
				for (int i = 1; i < postMargin; i++) {
					int idx = mIndex + i;
					if (idx >= mList.length) break;
					if (mList[idx].prev != mList[idx-1]) break;
					isNearAction[idx] = true;
					motionNearActionTypes[idx] = actionIdx;
				}
			}
//			System.out.println("action : " + ann.type + " : " + actionIdx + " : " + mList[mIndex] + " : " + database.getTypeList()[mIndex]);
		}
		originMotionActionTypes = MathUtil.copy(motionActionTypes);
	}
	
	protected void calcActionReachTime() {
		actionDirectTime = new int[actionLabels.length][mList.length];
		actionDirectAfterTime = new int[actionLabels.length][mList.length];
		for (int aIdx = 0; aIdx < actionDirectTime.length; aIdx++) {
			for (int i = 0; i < actionDirectTime[aIdx].length; i++) {
				actionDirectTime[aIdx][i] = 99999;
				actionDirectAfterTime[aIdx][i] = 99999;
			}
		}
		for (int mIndex = 0; mIndex < motionActionTypes.length; mIndex++) {
			int actionIdx = motionActionTypes[mIndex];
			if (actionIdx < 0) continue;
			actionDirectTime[actionIdx][mIndex] = 0;
			actionDirectAfterTime[actionIdx][mIndex] = 0;
			for (int i = 1; i < MAX_SEARCH_TIME; i++) {
				int idx = mIndex - i;
				if (idx < 0) break;
				if (mList[idx].next != mList[idx+1]) break;
				if (i < actionDirectTime[actionIdx][idx]) {
					actionDirectTime[actionIdx][idx] = i;
				} else {
					break;
				}
			}
			for (int i = 1; i < MAX_SEARCH_TIME; i++) {
				int idx = mIndex + i;
				if (idx >= mList.length) break;
				if (mList[idx].prev != mList[idx-1]) break;
				actionDirectAfterTime[actionIdx][idx] = i;
			}
		}
	}
	
	public int actionIndex(String type) {
		for (int i = 0; i < actionLabels.length; i++) {
			if (actionLabels[i].equals(type)) return i;
		}
		if (type.startsWith("n")) return actionLabels.length-1;
		return -1;
	}

	public static double processError(double ePrev, double eCurrent) {
		return ePrev + eCurrent;
	}
	
	public static class TransitionNode{
		public int index;
		public Motion motion;
		public boolean isTransitionable;
		
		public TransitionNode(int index, Motion motion) {
			this.index = index;
			this.motion = motion;
		}
		
		public int motionIndex() {
			return motion.motionIndex;
		}
		public int nextMotionIndex() {
			return motion.next.motionIndex;
		}
	}
}
