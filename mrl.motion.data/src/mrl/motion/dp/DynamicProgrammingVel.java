package mrl.motion.dp;

import static mrl.motion.data.trasf.MotionTransform.getAlignTransform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.vecmath.Matrix4d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MDatabase.FrameType;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.position.PositionMotion;
import mrl.util.Configuration;
import mrl.util.FileUtil;
import mrl.util.IterativeRunnable;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class DynamicProgrammingVel {

	static int rotDivision = 60;
	static int rotMid = rotDivision/2;
//	static double MAX_ERROR = 0.2;
	static double rotWeight = 0.01 * Math.toDegrees(Math.PI/rotMid)*Math.toDegrees(Math.PI/rotMid);
	static double rotStartWeight = 100;
	
	private MDatabase database;
	private FilteredMotion[] fMotionMap;
	private Motion[] mList;
	private FilteredMotion[] nodeList;
	private double[][] transitionMap;
	private double[] rotationMap;
	private double[][][] distanceTable;
	private int[][][] traceTable;

	public DynamicProgrammingVel(final MDatabase database) {
		this.database = database;
		mList = database.getMotionList();
		
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
		
		calcRotationMap();
		
	}
	
	public ArrayList<Motion> backtrace(int motionIndex, int time, double desireAngle){
		int margin = 10;
		ArrayList<Motion> trace = new ArrayList<Motion>();
		FilteredMotion current = fMotionMap[motionIndex];
		trace.add(new Motion(current.motion));
		Motion prev = current.motion;
		for (int i = 0; i < margin; i++) {
			prev = prev.prev;
			if (prev == null) break;
			trace.add(0, new Motion(prev));
		}
		
		
		double rotIndex = rotMid + (desireAngle/Math.PI)*rotMid;
		System.out.println("Start :: " + Utils.toString(rotIndex, rotMid, desireAngle));
		while (time >= 0) {
			rotIndex -= rotationMap[current.index]; 
			int rIndex = ((int)Math.round(rotIndex))%rotDivision;
			System.out.println("rr :: " + time + " : " + Utils.toString(rotIndex, rotationMap[current.index], distanceTable[time][rIndex][current.index]));
			if (time == 0) break;
//			rotIndex -= rotationMap[current.index]; 
//			int nextIndex = findMinPath(current, time);
			int nextIndex = traceTable[time][rIndex][current.index];
			current = nodeList[nextIndex];
			Motion.stitch(trace, current.motion);
//			trace.add(current.motion);
			time--;
		}
		
		int lastIndex = Utils.last(trace).motionIndex;
		for (int i = 0; i < margin; i++) {
			Motion.stitch(trace, mList[lastIndex + i + 1]);
		}
		
//		System.out.println("trace::");
//		for (Motion m : trace) {
//			System.out.println(m);
//		}
		return MotionSegment.alignToBase(trace, margin);
	}
	
	public void load(String file) {
		try {
			ObjectInputStream oi = FileUtil.inputStream(file);
			int size = oi.readInt();
			distanceTable = new double[size][][];
			traceTable = new int[size][][];
			for (int i = 0; i < size; i++) {
				distanceTable[i] = FileUtil.readDoubleArray2(oi);
				traceTable[i] = FileUtil.readIntArray2(oi);
			}
			oi.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void calcTransitionMap() {
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
	}
	
//	private void calcRotationMap() {
//		ArrayList<Pose2d> _poseList = new ArrayList<Pose2d>();
//		ArrayList<Pose2d> _prevPoseList = new ArrayList<Pose2d>();
//		for (int i = 0; i < nodeList.length; i++) {
//			_poseList.add(PositionMotion.getPose(nodeList[i].motion));
//			_prevPoseList.add(PositionMotion.getPose(nodeList[i].motion.prev));
//		}
//		Pose2d[] poseList = new Pose2d[_poseList.size()];
//		for (int i = 0; i < poseList.length; i++) {
//			poseList[i] = new Pose2d(_poseList.get(i));
//		}
//		Pose2d[] prevPoseList = new Pose2d[_prevPoseList.size()];
//		for (int i = 0; i < prevPoseList.length; i++) {
//			prevPoseList[i] = new Pose2d(_prevPoseList.get(i));
//		}
//	}
	
	private void calcRotationMap() {
		rotationMap = new double[nodeList.length];
		for (int i = 0; i < nodeList.length; i++) {
			Motion target = nodeList[i].motion;
			Motion source = target.prev;
			
			Pose2d sPose = PositionMotion.getPose(source);
			Pose2d tPose = PositionMotion.getPose(target);
			rotationMap[i] = MathUtil.directionalAngle(sPose.direction, tPose.direction);
			
			rotationMap[i] *= rotMid/Math.PI;
		}
	}

	public void calcAndSave(String file) {
		calcTransitionMap();
		
		int maxTime = 60;
		distanceTable = new double[maxTime][rotDivision][nodeList.length];
		traceTable = new int[maxTime][rotDivision][nodeList.length];
		for (int t = 0; t < maxTime; t++) {
			for (int r = 0; r < rotDivision; r++) {
				for (int i = 0; i < nodeList.length; i++) {
					distanceTable[t][r][i] = Integer.MAX_VALUE;
					traceTable[t][r][i] = -1;
				}
			}
		}
		
		for (int r = 0; r < rotDivision; r++) {
			double dr = (r - rotMid);
			dr = dr*dr;
			for (int i = 0; i < nodeList.length; i++) {
				distanceTable[0][r][i] = dr * rotWeight * rotStartWeight;
			}
		}
		
//		for (MotionAnnotation ann : database.getTransitionAnnotations()) {
//			if (ann.type.equals("kick")) {
//				System.out.println(ann);
//				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
//				distanceTable[0][fMotionMap[mIndex].index] = 0;
//			}
//		}
		
//		Configuration.MAX_THREAD = 1;
		for (int time = 1; time < maxTime; time++) {
			System.out.println("time :: " + time);
			final int t = time;
			Utils.runMultiThread(new IterativeRunnable(){
				@Override
				public void run(int next) {
					
					int nextMotionIndex = nodeList[next].nextMotionIndex;
//					System.out.println("next : " + next);
					for (int r = 0; r < rotDivision; r++) {
						double minDistance = Integer.MAX_VALUE;
						int minIndex = -1;
						double dt = 1;
						if (t < 5) {
							dt = ((10 - t)/(double)10)*rotStartWeight;
						}
						
//						System.out.println("r :: " + r);
						for (int target = 0; target < nodeList.length; target++) {
//							System.out.println("target :: " + target);
							double d = transitionMap[next][target];
							int targetMIndex = nodeList[target].motionIndex;
							if (nextMotionIndex == targetMIndex) {
							} else if (targetMIndex >= nextMotionIndex - 5 && targetMIndex < nextMotionIndex) {
								d += 100;
							} else if (nextMotionIndex + 1 == targetMIndex) {
								d += 1;
							} else {
								d += 2;
							}
							
							d = d*d;
							
							double rot = r - rotationMap[target];
							double dr = rot - rotMid;
							dr = dr*dr;
							
							d = getDistance(t-1, rot, target) + d + dr*dt*rotWeight;
							
							
							
	//						d = (distanceTable[t-1][target]*t + d)/(t+1);
	//						d = Math.max(d*d, distanceTable[t-1][target]);
							if (d < minDistance) {
								minIndex = target;
								minDistance = d;
							}
						}
//						if (r == 58 && minDistance < 50) {
//							System.out.println("????? :: " + minDistance + " : " + r + " : " + next + " : " + minIndex + " : " + dt);
//							System.out.println("p : " + getDistance(t-1, r, minIndex));
//							double rot = r - rotationMap[minIndex];
//							System.out.println("pp : " + getDistance(t-1, rot, minIndex));
//							
//						}
						distanceTable[t][r][next] = minDistance;
						traceTable[t][r][next] = minIndex;
					}
				}
			}, nodeList.length);
		}
		
		try {
			ObjectOutputStream os = FileUtil.outputStream(file);
			System.out.println("Save :: " + file + " :: " + distanceTable.length + " , " + distanceTable[0].length);
			os.writeInt(distanceTable.length);
			for (int i = 0; i < distanceTable.length; i++) {
				FileUtil.writeArray(os, distanceTable[i]);
				FileUtil.writeArray(os, traceTable[i]);
			}
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private double getDistance(int time, double rot, int nodeIndex) {
		int idx1 = (int)Math.floor(rot);
		int idx2 = (int)Math.ceil(rot);
		double ratio = rot - idx1;
		idx1 = (idx1 + rotDivision)%rotDivision;
		idx2 = (idx2 + rotDivision)%rotDivision;
		
		double v1 = distanceTable[time][idx1][nodeIndex];
		double v2 = distanceTable[time][idx2][nodeIndex];
		if (ratio < 0) throw new RuntimeException();
		if (ratio > 1) throw new RuntimeException();
		double v = (1 - ratio)*v1 + ratio*v2;
//		if (v < 0.1) {
//			System.out.println("zz :: " + Utils.toString(v, time,rot,nodeIndex));
//		}
		return v;
	}
	
	
	
	public static class FilteredMotion{
		int index;
		Motion motion;
		int motionIndex;
		int nextMotionIndex;
		
		public FilteredMotion(Motion motion) {
			this.motion = motion;
			motionIndex = motion.motionIndex;
			nextMotionIndex = motion.next.motionIndex;
		}
	}
	
	
}
