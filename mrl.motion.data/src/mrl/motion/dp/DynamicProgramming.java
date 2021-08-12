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
import mrl.util.Configuration;
import mrl.util.FileUtil;
import mrl.util.IterativeRunnable;
import mrl.util.Utils;

public class DynamicProgramming {

	private MDatabase database;
	private FilteredMotion[] fMotionMap;
	private Motion[] mList;
	private FilteredMotion[] nodeList;
	private double[][] transitionMap;
	private double[][] distanceTable;
	private int[][] traceTable;

	public DynamicProgramming(final MDatabase database) {
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
		
		
	}
	
	public ArrayList<Motion> backtrace(int motionIndex, int time){
		ArrayList<Motion> trace = new ArrayList<Motion>();
		FilteredMotion current = fMotionMap[motionIndex];
		trace.add(current.motion);
		Motion prev = current.motion;
		for (int i = 0; i < 10; i++) {
			prev = prev.prev;
			if (prev == null) break;
			trace.add(0, prev);
		}
		
		while (time > 0) {
//			int nextIndex = findMinPath(current, time);
			int nextIndex = traceTable[time][current.index];
			current = nodeList[nextIndex];
			System.out.println("dist :: " + Utils.last(trace) +":" + current.motion + " :: " + 
							database.getDist().getDistance(Utils.last(trace).motionIndex, current.motion.motionIndex));
			Motion.stitch(trace, current.motion);
//			trace.add(current.motion);
			time--;
		}
		
		int lastIndex = Utils.last(trace).motionIndex;
		for (int i = 0; i < 10; i++) {
			Motion.stitch(trace, mList[lastIndex + i + 1]);
		}
		
//		System.out.println("trace::");
//		for (Motion m : trace) {
//			System.out.println(m);
//		}
		return trace;
	}
	
	private int findMinPath(FilteredMotion motion, int time) {
		int minIndex = -1;
		double minDistance = Integer.MAX_VALUE;
		for (int i = 0; i < nodeList.length; i++) {
//			double d = database.getDist().getDistance(motion.motionIndex, j)
			double d = distanceTable[time][i];
			if (d < minDistance) {
				minIndex = i;
				minDistance = d;
			}
		}
		System.out.println("min :: " + time + " : " + minDistance + " :: " + nodeList[minIndex].motion);
		return minIndex;
	}
	
	public void load(String file) {
		try {
			ObjectInputStream oi = FileUtil.inputStream(file);
			distanceTable = FileUtil.readDoubleArray2(oi);
			traceTable = FileUtil.readIntArray2(oi);
			oi.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public double[][] getDistanceTable() {
		return distanceTable;
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
		
		int maxTime = 60;
		distanceTable = new double[maxTime][nodeList.length];
		traceTable = new int[maxTime][nodeList.length];
		for (int t = 0; t < maxTime; t++) {
			for (int i = 0; i < nodeList.length; i++) {
				distanceTable[t][i] = Integer.MAX_VALUE;
				traceTable[t][i] = -1;
			}
		}
		for (MotionAnnotation ann : database.getTransitionAnnotations()) {
			if (ann.type.equals("kick")) {
				System.out.println(ann);
				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
				distanceTable[0][fMotionMap[mIndex].index] = 0;
			}
		}
		
		for (int time = 1; time < maxTime; time++) {
			System.out.println("time :: " + time);
			final int t = time;
			Utils.runMultiThread(new IterativeRunnable(){
				@Override
				public void run(int next) {
					double minDistance = Integer.MAX_VALUE;
					int minIndex = -1;
					int nextMotionIndex = nodeList[next].nextMotionIndex;
					for (int target = 0; target < nodeList.length; target++) {
						double d = transitionMap[next][target];
						int targetMIndex = nodeList[target].motionIndex;
						if (nextMotionIndex == targetMIndex) {
						} else if (nextMotionIndex + 1 == targetMIndex) {
							d += 1;
						} else {
							d += 2;
						}
						
						d = d*d;
						d = distanceTable[t-1][target] + d;
						
						
						
//						d = (distanceTable[t-1][target]*t + d)/(t+1);
//						d = Math.max(d*d, distanceTable[t-1][target]);
						if (d < minDistance) {
							minIndex = target;
							minDistance = d;
						}
					}
					distanceTable[t][next] = minDistance;
					traceTable[t][next] = minIndex;
				}
			}, nodeList.length);
		}
		
		try {
			ObjectOutputStream os = FileUtil.outputStream(file);
			System.out.println("Save :: " + file + " :: " + distanceTable.length + " , " + distanceTable[0].length);
			FileUtil.writeArray(os, distanceTable);
			FileUtil.writeArray(os, traceTable);
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
