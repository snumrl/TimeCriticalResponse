package mrl.motion.dp;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData.TransitionNode;
import mrl.util.Configuration;
import mrl.util.FileUtil;
import mrl.util.IterativeRunnable;
import mrl.util.Matrix2d.RotationMatrix2d;
import mrl.util.Utils;

public class TransitionActionDP {

	public TransitionData tData;
	public double[][][] distanceTable;
	public int[][][] finalMIndexTable;
	
	public double[][][] tAfterDistanceTable;
	public double[][][] ctdTable;
	public double[][][] ctdRotationTable;
	public Pose2d[][][] ctdPoseTable;
	public int[][][] ctdFinalMIndexTable;
	
	public double[][][] tAfterDistanceTable2;
	public double[][][] ctdTable2;
	public double[][][] ctdRotationTable2;
	public Pose2d[][][] ctdPoseTable2;
	public int[][][] ctdFinalMIndexTable2;
	
	public Pose2d[][][] poseTable;
	public double[][][] rotationTable;

	public int actionSize;
	public int nodeSize;
	
	public TransitionActionDP(TransitionData tData) {
		this.tData = tData;
		actionSize = tData.actionLabels.length;
		nodeSize = tData.tNodeList.length;
	}
	
	
	public void calcAndSave(String file) {
		int maxTime = TransitionData.MAX_SEARCH_TIME;
		distanceTable = new double[actionSize][maxTime][nodeSize];
		finalMIndexTable = new int[actionSize][maxTime][nodeSize];
		final RotationMatrix2d[][][] transformData = new RotationMatrix2d[actionSize][maxTime][nodeSize];
		for (int a = 0; a < actionSize; a++) {
			for (int t = 0; t < maxTime; t++) {
				for (int i = 0; i < nodeSize; i++) {
					distanceTable[a][t][i] = Integer.MAX_VALUE;
					finalMIndexTable[a][t][i] = -1;
				}
			}
		}
		for (TransitionNode node : tData.tNodeList) {
			int actionIdx = tData.motionActionTypes[node.motionIndex()];
			if (actionIdx < 0) continue;
			distanceTable[actionIdx][0][node.index] = 0;
			finalMIndexTable[actionIdx][0][node.index] = node.motionIndex();
			transformData[actionIdx][0][node.index] = RotationMatrix2d.identity();
		}
		
//		final int ttt = tData.database.findMotion("s_001_3_1.bvh", 176).motionIndex;
		
		final TransitionNode[] nodeList = tData.tNodeList;
		for (int _actionIdx = 0; _actionIdx < actionSize; _actionIdx++) {
			final int actionIdx = _actionIdx;
			for (int time = 1; time < maxTime; time++) {
				System.out.println("time :: " + actionIdx + " : " + tData.actionLabels[actionIdx] + " : " + time);
				final int t = time;
				Utils.runMultiThread(new IterativeRunnable(){
					@Override
					public void run(int nIdx) {
						double minDistance = Integer.MAX_VALUE;
//						minDistance = distanceTable[actionIdx][t-1][nIdx];
						int minIndex = -1;
						
						int currentMotionIndex = nodeList[nIdx].motionIndex();
						int nextMotionIndex = nodeList[nIdx].nextMotionIndex();
						int currentAction = tData.motionNearActionTypes[currentMotionIndex];
						
//						boolean match = tData.mList[currentMotionIndex].toString().equals("s_001_2_1:234");
//						boolean match = tData.mList[currentMotionIndex].toString().equals("s_001_2_1:219");
						
						double[] distCache = tData.transitionDistance(currentMotionIndex);
						for (int target = 0; target < distCache.length; target++) {
							double d = distCache[target];
							if (d >= Integer.MAX_VALUE) continue;
							
							int targetMIndex = nodeList[target].motionIndex();
							int tAfterIdx = tData.transitionAfterMIndex[targetMIndex];
							boolean isSequential = (nextMotionIndex == targetMIndex);
							
							if (!isSequential) {
								if (tAfterIdx < 0) continue;
								if (tData.motionToNodeMap[tAfterIdx] == null) continue;
								if (t <= Configuration.MOTION_TRANSITON_MARGIN) continue;
//								d = tData.adjustDistance(d, currentMotionIndex, targetMIndex);
							}
							
//							d = d*d;
							double dPrev;
							int targetAction = tData.motionNearActionTypes[targetMIndex];
							if (targetAction >= 0 && targetAction != actionIdx) {
								if ((currentAction != targetAction) || !isSequential) continue;
							}
							
							if (isSequential) {
								dPrev = distanceTable[actionIdx][t-1][target];
							} else {
								int tAfterAction = tData.motionNearActionTypes[tAfterIdx];
								if (tAfterAction >= 0 && tAfterAction != actionIdx) {
									continue;
								}
								dPrev = distanceTable[actionIdx][t-1-Configuration.MOTION_TRANSITON_MARGIN][tData.motionToNodeMap[tAfterIdx].index];
							}
//							if (match && actionIdx == 5 && targetMIndex == ttt) {
//								System.out.println("ttt : " + t + " : " + currentAction + " : " + targetAction + " : " + dPrev + " : " + d);
//								System.out.println("tafter : " + tData.mList[targetMIndex] + " -> " + tData.mList[tAfterIdx]);
//							}
//							if (match && isSequential) {
//								if (actionIdx == 5) {
//									System.out.println("ppp : " + t + " : " + currentAction + " : " + targetAction + " : " + dPrev + " : " + d);
//								}
//							}
							d = TransitionData.processError(dPrev, d);
							if (d < minDistance) {
								minDistance = d;
								minIndex = target;
							}
						}
//						if (match || currentMotionIndex == ttt || currentMotionIndex == (ttt+4)) {
//							System.out.println("best :: " + tData.mList[currentMotionIndex] + " : " + minDistance +  " : " + minIndex);
//						}
						distanceTable[actionIdx][t][nIdx] = minDistance;
//						finalMIndexTable[actionIdx][t][nIdx] = finalMIndexTable[actionIdx][t-1][minIndex];
						if (minDistance < Integer.MAX_VALUE) {
							int targetMIndex = nodeList[minIndex].motionIndex();
							// stitch 할때 첫 frame은 source의 transform을 따라 이동한다고 가정
							RotationMatrix2d transform = new RotationMatrix2d(tData.adjRootTransform[targetMIndex]);
							if (nextMotionIndex == targetMIndex) {
								transform.mul(transformData[actionIdx][t-1][minIndex]);
								finalMIndexTable[actionIdx][t][nIdx] = finalMIndexTable[actionIdx][t-1][minIndex];
							} else {
								transform.mul(tData.tAfterRootTransform[targetMIndex]);
								int tAfterIdx = tData.transitionAfterMIndex[targetMIndex];
								transform.mul(transformData[actionIdx][t-1-Configuration.MOTION_TRANSITON_MARGIN][tData.motionToNodeMap[tAfterIdx].index]);
								finalMIndexTable[actionIdx][t][nIdx] = finalMIndexTable[actionIdx][t-1-Configuration.MOTION_TRANSITON_MARGIN][tData.motionToNodeMap[tAfterIdx].index];
//								finalMIndexTable[actionIdx][t][nIdx] = finalMIndexTable[actionIdx][t-1][tData.motionToNodeMap[tAfterIdx].index];
							}
							transformData[actionIdx][t][nIdx] = transform;
						}
					}
				}, nodeList.length);
			}
//			if (actionIdx == 5) System.exit(0);
		}
		
		try {
			new File(file).getParentFile().mkdirs();
			System.out.println("Save :: " + file + " :: " + distanceTable.length + " , " + distanceTable[0].length);
			ObjectOutputStream os = FileUtil.outputStream(file);
			os.writeInt(distanceTable.length);
			for (int i = 0; i < actionSize; i++) {
				FileUtil.writeArray(os, distanceTable[i]);
			}
			for (int i = 0; i < actionSize; i++) {
				FileUtil.writeArray(os, finalMIndexTable[i]);
			}
			for (int i = 0; i < actionSize; i++) {
				for (int j = 0; j < maxTime; j++) {
					for (int k = 0; k < nodeSize; k++) {
						writePose(os, transformData[i][j][k]);
					}
				}
			}
			os.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private double tAfterDistance(int aType, int nodeIndex, int remainTime, double[][][] ctdTable) {
		int tMargin = Configuration.MOTION_TRANSITON_MARGIN;
		
		if (remainTime < tMargin) {
			for (int i = 0; i <= tMargin; i++) {
				if (nodeIndex + i >= ctdTable[0][0].length) break;
				int type = tData.motionActionTypes[nodeIndex + i];
				if (type == aType) return 0;
			}
//			if (tData.actionDirectTime[aType][tData.tNodeList[nodeIndex].motionIndex()] <= tMargin) {
//				return 0;
//			}
			return Integer.MAX_VALUE;
		} else {
			if (nodeIndex + tMargin >= ctdTable[0][0].length) return Integer.MAX_VALUE;
			return ctdTable[aType][remainTime-tMargin][nodeIndex+tMargin];
		}
	}
	
	void calcCriticalTimeTable() {
		System.out.println("calcCriticalTimeTable :: tMargin : " + Configuration.MOTION_TRANSITON_MARGIN);
		int s1 = distanceTable.length;
		int s2 = distanceTable[0].length;
		int s3 = distanceTable[0][0].length;
		ctdTable = new double[s1][s2][s3];
		ctdRotationTable = new double[s1][s2][s3];
		ctdPoseTable = new Pose2d[s1][s2][s3];
		ctdFinalMIndexTable = new int[s1][s2][s3];
		
		for (int i = 0; i < s1; i++) {
			for (int k = 0; k < s3; k++) {
				double min = Integer.MAX_VALUE;
				double minRot = Double.NaN;
				Pose2d minPose = null;
				int minFinalIndex = -1;
				for (int j = 0; j < s2; j++) {
					if (distanceTable[i][j][k] < min) {
						min = distanceTable[i][j][k];
						minRot = rotationTable[i][j][k];
						minPose = poseTable[i][j][k];
						minFinalIndex = finalMIndexTable[i][j][k];
					}
					ctdTable[i][j][k] = min;
					ctdRotationTable[i][j][k] = minRot;
					ctdPoseTable[i][j][k] = minPose;
					ctdFinalMIndexTable[i][j][k] = minFinalIndex;
				}
			}
		}
		tAfterDistanceTable = new double[s1][s2][s3];
		for (int i = 0; i < s1; i++) {
			for (int k = 0; k < s3; k++) {
				for (int j = 0; j < s2; j++) {
					tAfterDistanceTable[i][j][k] = tAfterDistance(i, k, j, ctdTable);
				}
			}
		}
		
		ctdTable2 = new double[s1][s2][s3];
		ctdRotationTable2 = new double[s1][s2][s3];
		ctdPoseTable2 = new Pose2d[s1][s2][s3];
		ctdFinalMIndexTable2 = new int[s1][s2][s3];
		for (int i = 0; i < s1; i++) {
			for (int k = 0; k < s3; k++) {
				double min = Integer.MAX_VALUE;
				double minRot = Double.NaN;
				Pose2d minPose = null;
				int minFinalIndex = -1;
				for (int j = 0; j < s2; j++) {
					if (j >= 10 && distanceTable[i][j][k] < min) {
						min = distanceTable[i][j][k];
						minRot = rotationTable[i][j][k];
						minPose = poseTable[i][j][k];
						minFinalIndex = finalMIndexTable[i][j][k];
					}
					ctdTable2[i][j][k] = min;
					ctdRotationTable2[i][j][k] = minRot;
					ctdPoseTable2[i][j][k] = minPose;
					ctdFinalMIndexTable2[i][j][k] = minFinalIndex;
				}
			}
		}
		
		tAfterDistanceTable2 = new double[s1][s2][s3];
		for (int i = 0; i < s1; i++) {
			for (int k = 0; k < s3; k++) {
				for (int j = 0; j < s2; j++) {
					tAfterDistanceTable2[i][j][k] = tAfterDistance(i, k, j, ctdTable2);
				}
			}
		}
	}
	
	public void load(String file) {
		try {
			ObjectInputStream oi = FileUtil.inputStream(file);
			int aSize = oi.readInt();
			distanceTable = new double[aSize][][];
			for (int i = 0; i < actionSize; i++) {
				distanceTable[i] = FileUtil.readDoubleArray2(oi); 
			}
			finalMIndexTable = new int[aSize][][];
			for (int i = 0; i < actionSize; i++) {
				finalMIndexTable[i] = FileUtil.readIntArray2(oi); 
			}
			
			int maxTime = TransitionData.MAX_SEARCH_TIME;
			rotationTable = new double[actionSize][maxTime][nodeSize];
			poseTable = new Pose2d[actionSize][maxTime][nodeSize];
			for (int i = 0; i < actionSize; i++) {
				for (int j = 0; j < maxTime; j++) {
					for (int k = 0; k < nodeSize; k++) {
						double angle = oi.readDouble();
						rotationTable[i][j][k] = angle;
						if (!Double.isNaN(angle)) {
							Pose2d p = new Pose2d(oi.readDouble(), oi.readDouble(), oi.readDouble(), oi.readDouble());
							poseTable[i][j][k] = p;
						}
					}
				}
			}
			oi.close();
			
			calcCriticalTimeTable();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	protected void writePose(ObjectOutputStream os, RotationMatrix2d t) {
		try {
			if (t == null) {
				os.writeDouble(Double.NaN);
			} else {
				os.writeDouble(t.angle);
				if (Double.isNaN(t.angle)) throw new RuntimeException();
				Pose2d p = Pose2d.byBase(t.m);
				double[] data = p.toArray();
				for (int i = 0; i < data.length; i++) {
					os.writeDouble(data[i]);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public TimeCriticalTable getBaseTCTable() {
		return new TimeCriticalTable(tAfterDistanceTable, ctdTable, ctdRotationTable, ctdPoseTable, ctdFinalMIndexTable);
	}
	
	public TimeCriticalTable getStartingTCTable() {
		return new TimeCriticalTable(tAfterDistanceTable2, ctdTable2, ctdRotationTable2, ctdPoseTable2, ctdFinalMIndexTable2);
	}
	
	public int searchMinReachTime(int mIndex, int action) {
		int nodeIndex = tData.motionToNodeMap[mIndex].index;
		for (int i = 0; i < distanceTable[0].length; i++) {
			if (distanceTable[action][i][nodeIndex] < Integer.MAX_VALUE) {
				return i;
			}
		}
		return -1;
	}
	
	
	public static class TimeCriticalTable{
		public double[][][] tAfterDistanceTable;
		public double[][][] ctdTable;
		public double[][][] ctdRotationTable;
		public Pose2d[][][] ctdPoseTable;
		public int[][][] ctdFinalMIndexTable;
		
		public TimeCriticalTable(double[][][] tAfterDistanceTable, double[][][] ctdTable, double[][][] ctdRotationTable,
				Pose2d[][][] ctdPoseTable, int[][][] ctdFinalMIndexTable) {
			this.tAfterDistanceTable = tAfterDistanceTable;
			this.ctdTable = ctdTable;
			this.ctdRotationTable = ctdRotationTable;
			this.ctdPoseTable = ctdPoseTable;
			this.ctdFinalMIndexTable = ctdFinalMIndexTable;
		}
	}
}
