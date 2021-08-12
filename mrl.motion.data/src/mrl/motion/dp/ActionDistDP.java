package mrl.motion.dp;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.MDatabase;
import mrl.motion.data.MDatabase.FrameType;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.data.trasf.MotionTransform;
import mrl.util.Configuration;
import mrl.util.FileUtil;
import mrl.util.IterativeRunnable;
import mrl.util.Utils;

public class ActionDistDP {
	
	public static int NO_BACKWARD_TRANSITION_MARGIN = 8;
	public static int NO_TRANSITION_BEFORE_ACTION_MARGIN = 8;
	public static int NO_TRANSITION_AFTER_ACTION_MARGIN = 4;
	public static int NO_MARGIN_ACTION_SIZE = 0;

	private int maxTime = 80;
	
	public MDatabase database;
	public String[] actionLabels;
	public FilteredMotion[] fMotionMap;
	public Motion[] mList;
	public FilteredMotion[] nodeList;
	
	private double[][] transitionMap;
	
	public double[][][] distanceTable;
	public int[][][] traceTable;
	public int[] motionActionTypes;
	public int[][] actionDirectTime;
	public int[][] actionDirectAfterTime;
	
	public int[] transitionAfterMIndex;
	public boolean[] isActiveAction;
	public boolean[] isNearAction;

	public ActionDistDP(final MDatabase database, String[] actionLabels) {
		this.database = database;
		this.actionLabels = actionLabels;
		mList = database.getMotionList();
		isActiveAction = new boolean[actionLabels.length];
		for (int i = 0; i < actionLabels.length; i++) {
			isActiveAction[i] = true;
		}
		motionActionTypes = new int[mList.length];
		for (int i = 0; i < motionActionTypes.length; i++) {
			motionActionTypes[i] = -1;
		}
		isNearAction = new boolean[mList.length];
		for (MotionAnnotation ann : database.getEventAnnotations()) {
			int actionIdx = actionIndex(ann.type);
			if (actionIdx < 0) continue;
			if (actionIdx < NO_MARGIN_ACTION_SIZE) {
				int sIndex = database.findMotion(ann.file, ann.startFrame).motionIndex;
				int eIndex = database.findMotion(ann.file, ann.endFrame).motionIndex;
				for (int i = sIndex; i <= eIndex; i++) {
					motionActionTypes[i] = actionIdx;
				}
			} else {
				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
				motionActionTypes[mIndex] = actionIdx;
				
				isNearAction[mIndex] = true;
				int prevMargin = NO_TRANSITION_BEFORE_ACTION_MARGIN;
				int postMargin = NO_TRANSITION_AFTER_ACTION_MARGIN;
				for (int i = 1; i < prevMargin; i++) {
					int idx = mIndex - i;
					if (idx < 0) break;
					if (mList[idx].next != mList[idx+1]) break;
					isNearAction[idx] = true;
				}
				for (int i = 1; i < postMargin; i++) {
					int idx = mIndex + i;
					if (idx >= mList.length) break;
					if (mList[idx].prev != mList[idx-1]) break;
					isNearAction[idx] = true;
				}
			}
//			System.out.println("action : " + ann.type + " : " + actionIdx + " : " + mList[mIndex] + " : " + database.getTypeList()[mIndex]);
		}
//		System.exit(0);
		init();
	}
	public ActionDistDP(final MDatabase database, String[] actionLabels, boolean[] isActiveAction, int[] motionActionTypes) {
		this.database = database;
		this.actionLabels = actionLabels;
		this.isActiveAction = isActiveAction;
		this.motionActionTypes = motionActionTypes;
		mList = database.getMotionList();
		init();
	}
	
	protected void init() {
		ArrayList<FilteredMotion> _nodeList = new ArrayList<FilteredMotion>();
		FrameType[] typeList = database.getTypeList();
		fMotionMap = new FilteredMotion[typeList.length];
		for (int i = 0; i < typeList.length; i++) {
			if (typeList[i] == null) continue;
			if (mList[i].prev == null || mList[i].next == null) continue;
			FilteredMotion m = new FilteredMotion(mList[i]);
			m.index = _nodeList.size();
			_nodeList.add(m);
			fMotionMap[i] = m;
		}
		nodeList = _nodeList.toArray(new FilteredMotion[_nodeList.size()]);
		System.out.println("filtered node size :: " + nodeList.length);
		
		transitionAfterMIndex = database.getTransitionAfterMIndex();
		
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
			motionActionTypes[mIndex] = actionIdx;
			actionDirectTime[actionIdx][mIndex] = 0;
			actionDirectAfterTime[actionIdx][mIndex] = 0;
			for (int i = 1; i < maxTime; i++) {
				int idx = mIndex - i;
				if (idx < 0) break;
				if (mList[idx].next != mList[idx+1]) break;
				if (i < actionDirectTime[actionIdx][idx]) {
					actionDirectTime[actionIdx][idx] = i;
				} else {
					break;
				}
			}
			for (int i = 1; i < maxTime; i++) {
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
		return -1;
	}
	
	public void load(String file) {
		try {
			ObjectInputStream oi = FileUtil.inputStream(file);
			int aSize = oi.readInt();
			distanceTable = new double[aSize][][];
			for (int i = 0; i < actionLabels.length; i++) {
				distanceTable[i] = FileUtil.readDoubleArray2(oi); 
			}
			traceTable = new int[aSize][][];
			for (int i = 0; i < actionLabels.length; i++) {
				traceTable[i] = FileUtil.readIntArray2(oi); 
			}
			
			oi.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getActionSize() {
		return actionLabels.length;
	}
	
	public int directTimeDistance(int action, Motion m) {
		return actionDirectTime[action][m.motionIndex];
	}
	
	public double getDistance(int action, int time, int mIndex) {
		if (fMotionMap[mIndex] == null) return Integer.MAX_VALUE;
		int idx = fMotionMap[mIndex].index;
		return distanceTable[action][time][idx];
	}
	
	public int getMotionActionType(Motion m) {
		return motionActionTypes[m.motionIndex];
	}
	
	public double adjustDistance(double distance, int currentMotionIndex, int targetMIndex) {
//		if (actionIdx >= NO_MARGIN_ACTION_SIZE) {
//			if (actionDirectTime[actionIdx][targetMIndex] < NO_TRANSITION_BEFORE_ACTION_MARGIN) {
//				return Integer.MAX_VALUE;
//			}
//			if (actionDirectAfterTime[actionIdx][currentMotionIndex] < NO_TRANSITION_BEFORE_ACTION_MARGIN) {
//				return Integer.MAX_VALUE;
//			}
//		}
		if (isNearAction[currentMotionIndex] || isNearAction[targetMIndex]) return Integer.MAX_VALUE;
		
		int backwardLimit = NO_BACKWARD_TRANSITION_MARGIN;
		int nextMotionIndex = currentMotionIndex + 1;
		if (targetMIndex < nextMotionIndex && targetMIndex > nextMotionIndex - backwardLimit
				&& mList[targetMIndex].frameIndex >= backwardLimit) {
			distance += 2000;
		}
		if (nextMotionIndex + 1 == targetMIndex) {
			distance += 1;
		} else {
			distance += 2;
		}
		return distance;
	}
	
	public void calcAndSave(String file) {
		transitionMap = new double[nodeList.length][nodeList.length];
		System.out.flush();
		final MotionDistByPoints dist = database.getDist();
		Utils.runMultiThread(new IterativeRunnable(){
			int progress;
			@Override
			public void run(int i) {
				synchronized (this) {
					progress++;
					if ((progress % 100) == 1) {
						System.out.println("progress :: " + progress + " / " + nodeList.length);
					}
				}
				
				for (int j = 0; j < nodeList.length; j++) {
					transitionMap[i][j] = dist.getDistance(nodeList[i].nextMotionIndex, nodeList[j].motionIndex);
				}
			}
		}, nodeList.length);
		
		distanceTable = new double[actionLabels.length][maxTime][nodeList.length];
		traceTable = new int[actionLabels.length][maxTime][nodeList.length];
		for (int a = 0; a < actionLabels.length; a++) {
			for (int t = 0; t < maxTime; t++) {
				for (int i = 0; i < nodeList.length; i++) {
					distanceTable[a][t][i] = Integer.MAX_VALUE;
					traceTable[a][t][i] = -1;
				}
			}
		}
		
		for (int mIndex = 0; mIndex < motionActionTypes.length; mIndex++) {
			int actionIdx = motionActionTypes[mIndex];
			if (actionIdx < 0) continue;
			distanceTable[actionIdx][0][fMotionMap[mIndex].index] = 0;
			traceTable[actionIdx][0][fMotionMap[mIndex].index] = -9999;
		}
		
		for (int _actionIdx = 0; _actionIdx < actionLabels.length; _actionIdx++) {
			final int actionIdx = _actionIdx;
			for (int time = 1; time < maxTime; time++) {
				System.out.println("time :: " + time);
				final int t = time;
				Utils.runMultiThread(new IterativeRunnable(){
					@Override
					public void run(int nIdx) {
						double minDistance = Integer.MAX_VALUE;
						minDistance = distanceTable[actionIdx][t-1][nIdx];
								
						int minIndex = -1;
						int currentMotionIndex = nodeList[nIdx].motionIndex;
						int nextMotionIndex = nodeList[nIdx].nextMotionIndex;
						double[] temp = new double[0];
						for (int target = 0; target < nodeList.length; target++) {
							int tAfterIdx = transitionAfterMIndex[nodeList[target].motionIndex];
							int targetMIndex = nodeList[target].motionIndex;
							boolean isSequential = (nextMotionIndex == targetMIndex);
							
							double d = transitionMap[nIdx][target];
							if (!isSequential) {
								if (tAfterIdx < 0) continue;
								if (fMotionMap[tAfterIdx] == null) continue;
								if (t <= Configuration.MOTION_TRANSITON_MARGIN) continue;
								
								d = adjustDistance(d, currentMotionIndex, targetMIndex);
								if (d >= Integer.MAX_VALUE) continue;
							}
							
							d = d*d;
							double dPrev;
							if (isSequential) {
								dPrev = distanceTable[actionIdx][t-1][target];
							} else {
								dPrev = distanceTable[actionIdx][t-1-Configuration.MOTION_TRANSITON_MARGIN][fMotionMap[tAfterIdx].index];
							}
							if (actionIdx == 3 && t == 11 && nodeList[nIdx].motion.toString().equals("BF_Parkour_F_02:34")) {
//								System.out.println("mdd : " + actionIdx + " : " + t + " : " + minDistance);
								System.out.println("check : " + nodeList[target].motion + " ; " + d + " : " + dPrev);
							}
							
							d = processError(dPrev, d);
	//						d = (distanceTable[t-1][target]*t + d)/(t+1);
	//						d = Math.max(d*d, distanceTable[t-1][target]);
							if (d < minDistance) {
								minDistance = d;
								minIndex = target;
//								temp = new double[] { dPrev, t-1-Configuration.MOTION_TRANSITON_MARGIN, fMotionMap[tAfterIdx].index, isSequential ? 1 : 0};
							}
						}
//						if (t == 19 && nIdx == 234) {
//							System.out.println("dddd : " + actionIdx + " : " + minDistance + " , " + minIndex + " : " + Arrays.toString(temp));
//						}
						distanceTable[actionIdx][t][nIdx] = minDistance;
						traceTable[actionIdx][t][nIdx] = minIndex;
					}
				}, nodeList.length);
			}
		}
		
		try {
			new File(file).getParentFile().mkdirs();
			ObjectOutputStream os = FileUtil.outputStream(file);
			System.out.println("Save :: " + file + " :: " + distanceTable.length + " , " + distanceTable[0].length);
			os.writeInt(distanceTable.length);
			for (int i = 0; i < actionLabels.length; i++) {
				FileUtil.writeArray(os, distanceTable[i]);
			}
			for (int i = 0; i < actionLabels.length; i++) {
				FileUtil.writeArray(os, traceTable[i]);
			}
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static double processError(double ePrev, double eCurrent) {
//		return Math.max(ePrev, eCurrent);
		return ePrev + eCurrent;
	}
	
	public static class FilteredMotion{
		public int index;
		public Motion motion;
		public int motionIndex;
		public int nextMotionIndex;
		
		public FilteredMotion(Motion motion) {
			this.motion = motion;
			motionIndex = motion.motionIndex;
			nextMotionIndex = motion.next.motionIndex;
		}
	}
	
	
	static void make(String dataFolder, String[] actions) {
		MotionTransform.setTposeMotion();
		MDatabase.loadEventAnnotations = true;
		Configuration.setDataFolder(dataFolder);
		MDatabase database = MDatabase.load();
		ActionDistDP dp = new ActionDistDP(database, actions);
		String fName = new File(dataFolder).getName() + ".dat";
		dp.calcAndSave(fName);
	}
	
}
