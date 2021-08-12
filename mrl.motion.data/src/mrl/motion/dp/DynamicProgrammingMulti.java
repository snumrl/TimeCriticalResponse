package mrl.motion.dp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MDatabase.FrameType;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.graph.MotionSegment;
import mrl.util.Configuration;
import mrl.util.FileUtil;
import mrl.util.IterativeRunnable;
import mrl.util.MathUtil;
import mrl.util.Utils;

public abstract class DynamicProgrammingMulti {

	public static int MAX_TIME = 60;
	public static int SPARSE_LIMIT = 1000;
			
	protected MDatabase database;
	protected FilteredMotion[] fMotionMap;
	protected Motion[] mList;
	protected FilteredMotion[] nodeList;
	protected Transition[][] sparseTransitions;
	protected double[][] movementMap;
	protected double[][] goalList;
	protected double[][][] distanceTable;
	protected int[][][] traceTable;
	protected int[] goalSize;
	protected GoalDimensionInfo[] goalDimensions;
	
	protected int[] actionPrefix;
	protected double[] heightList;

	public DynamicProgrammingMulti(final MDatabase database) {
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
		heightList = new double[nodeList.length];
		for (int i = 0; i < typeList.length; i++) {
			heightList[i] = MathUtil.getTranslation(nodeList[i].motion.get(SkeletonData.instance.root.name)).y;
		}
		
		goalDimensions = getDimensionInfo();
		goalSize = new int[goalDimensions.length];
		for (int i = 0; i < goalSize.length; i++) {
			goalDimensions[i].init();
			goalSize[i] = goalDimensions[i].division;
		}
		initializeBase();
	}
	
	abstract double[] motionMovement(int nodeIndex);
	abstract GoalDimensionInfo[] getDimensionInfo();
	abstract double getError(int remainTime, int nodeIndex, double[] goal);
	
	
	double[] goalToIndex(double[] goal) {
		double[] indices = new double[goal.length];
		for (int i = 0; i < indices.length; i++) {
			indices[i] = goalDimensions[i].goalToIndex(goal[i]);
		}
		return indices;
	}

	double[] indexToGoal(int[] indices) {
		double[] goals = new double[indices.length];
		for (int i = 0; i < goals.length; i++) {
			goals[i] = goalDimensions[i].indexToGoal(indices[i]);
		}
		return goals;
	}
	
	double interpolatedDistance(int time, int nodeIndex, double[] goal) {
		double[] gIndices = goalToIndex(goal);
		int[] floors = new int[gIndices.length];
		int[] ceils = new int[gIndices.length];
		double[] ratioList = new double[gIndices.length];
		for (int i = 0; i < ceils.length; i++) {
			floors[i] = (int)Math.floor(gIndices[i]);
			ceils[i] = (int)Math.ceil(gIndices[i]);
			ratioList[i] = gIndices[i] - floors[i];
			floors[i] = (floors[i] + goalSize[i])%goalSize[i];
			ceils[i] = (ceils[i] + goalSize[i])%goalSize[i];
		}
		return getInterpolation(time, nodeIndex, 0, new int[goalSize.length], floors, ceils, ratioList);
	}
	
	private double getInterpolation(int time, int nodeIndex, int startIndex, 
			int[] fixedIndices, int[] floors, int[] ceils, double[] ratioList) {
		double ratio = ratioList[startIndex];
		if (startIndex == floors.length - 1) {
			fixedIndices[startIndex] = floors[startIndex];
			double vFloor = distanceTable[time][goalIndex(fixedIndices)][nodeIndex];
			fixedIndices[startIndex] = ceils[startIndex];
			double vCeil = distanceTable[time][goalIndex(fixedIndices)][nodeIndex];
			return (1 - ratio)*vFloor + ratio*vCeil;
		} else {
			fixedIndices[startIndex] = floors[startIndex];
			double vFloor = getInterpolation(time, nodeIndex, startIndex+1, fixedIndices, floors, ceils, ratioList);
			fixedIndices[startIndex] = ceils[startIndex];
			double vCeil = getInterpolation(time, nodeIndex, startIndex+1, fixedIndices, floors, ceils, ratioList);
			return (1 - ratio)*vFloor + ratio*vCeil;
		}
	}
	
	protected int nearestGoalIndex(double[] goal) {
		double[] indices_d = goalToIndex(goal);
		int[] indices = new int[indices_d.length];
		for (int i = 0; i < indices_d.length; i++) {
			int idx = (int)Math.round(indices_d[i]);
			indices[i] = goalDimensions[i].validateIndex(idx);
//			indices[i] = ((int)Math.round(indices_d[i]))%goalSize[0];
		}
		return goalIndex(indices);
	}
	
	double[] moveGoal(double[] currentGoal, double[] movement) {
		currentGoal = MathUtil.copy(currentGoal);
		MathUtil.sub(currentGoal, movement);
		return currentGoal;
	}
	
	protected int totalGoalSize() {
		int sum = 1;
		for (int i = 0; i < goalSize.length; i++) {
			sum *= goalSize[i];
		}
		return sum;
	}
	protected int[] elementWiseGoalIndex(int goalIndex) {
		int[] indices = new int[goalSize.length];
		for (int i = 0; i < goalSize.length; i++) {
			indices[i] = goalIndex % goalSize[i];
			goalIndex /= goalSize[i];
		}
		return indices;
	}
	protected int goalIndex(int... indices) {
		int index = 0;
		for (int i = goalSize.length-1; i >= 0; i--) {
			index = index*goalSize[i] + indices[i]; 
		}
		return index;
	}
	
	Motion lastMotion = null;
	FilteredMotion lastFMotion;
	public Motion step(int time, double[] goal) {
		if (lastFMotion == null) {
			lastFMotion = nodeList[new Random().nextInt(nodeList.length)];
			lastMotion = new Motion(lastFMotion.motion);
		}
//		goal = moveGoal(goal, movementMap[lastFMotion.index]);
		int rIndex = nearestGoalIndex(goal);
		System.out.println("rr :: " + time + " : " + Utils.toString(lastFMotion.motion, goal, distanceTable[time][rIndex][lastFMotion.index]));
//		rotIndex -= rotationMap[current.index]; 
//		int nextIndex = findMinPath(current, time);
		int nextIndex = traceTable[time][rIndex][lastFMotion.index];
		lastFMotion = nodeList[nextIndex];
		lastMotion = Motion.stitchMotion(lastMotion, lastFMotion.motion);
		return lastMotion;
	}
	
	public ArrayList<Motion> backtrace(int motionIndex, int time, double[] goal){
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
		
		
		while (time >= 0) {
			goal = moveGoal(goal, movementMap[current.index]);
			int rIndex = nearestGoalIndex(goal);
			System.out.println("rr :: " + time + " : " + Utils.toString(goal, distanceTable[time][rIndex][current.index]));
			if (time == 0) break;
//			rotIndex -= rotationMap[current.index]; 
//			int nextIndex = findMinPath(current, time);
			int nextIndex = traceTable[time][rIndex][current.index];
			current = nodeList[nextIndex];
			Motion.stitch(trace, current.motion);
//			trace.add(current.motion);
			time--;
		}
		
//		System.out.println("#######");
//		int gSize = totalGoalSize();
//		for (int i = 0; i < gSize; i++) {
//			double[] gg = indexToGoal(elementWiseGoalIndex(i));
//			System.out.println(i + " : "+ Utils.toString(gg, distanceTable[0][i][current.index]));
//		}
//		System.out.println("#######");
		
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
		final int sparseSize = SPARSE_LIMIT;
		
		String fName = new File(Configuration.DATA_FOLDER).getName();
		fName = "transition_" + fName + ".dat";
		
		sparseTransitions = new Transition[nodeList.length][];
		if (new File(fName).exists()) {
			try {
				DataInputStream is = FileUtil.dataInputStream(fName);
				Utils.assertEqual(is.readInt(), nodeList.length);
				Utils.assertEqual(is.readInt(), sparseSize);
				
				for (int i = 0; i < sparseTransitions.length; i++) {
					Transition[] tList = new Transition[sparseSize];
					for (int j = 0; j < tList.length; j++) {
						tList[j] = new Transition(is.readInt(), is.readDouble());
					}
					sparseTransitions[i] = tList;
				}
				is.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
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
					Transition[] tList = new Transition[nodeList.length];
					for (int j = 0; j < nodeList.length; j++) {
						double d = dist.getDistance(nodeList[i].nextMotionIndex, nodeList[j].motionIndex);
						if (actionPrefix[j] != 0) {
							d -= 10000;
						}
						tList[j] = new Transition(j, d);
	//					transitionMap[i][j] = dist.getDistance(nodeList[i].nextMotionIndex, nodeList[j].motionIndex);
					}
					Arrays.sort(tList);
					sparseTransitions[i] = Utils.cut(tList, 0, sparseSize-1);
				}
			}, nodeList.length);
			
			
			try {
				DataOutputStream os = FileUtil.dataOutputStream(fName);
				os.writeInt(nodeList.length);
				os.writeInt(sparseSize);
				for (int i = 0; i < sparseTransitions.length; i++) {
					for (Transition t : sparseTransitions[i]) {
						os.writeInt(t.target);
						os.writeDouble(t.distance);
					}
				}
				os.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private void initializeBase() {
		ArrayList<double[]> movementMap = new ArrayList<double[]>();
		for (int i = 0; i < nodeList.length; i++) {
			movementMap.add(motionMovement(i));
		}
		this.movementMap = Utils.toArray(movementMap);
		
		ArrayList<double[]> goalList = new ArrayList<double[]>();
		for (int i = 0; i < totalGoalSize(); i++) {
			goalList.add(indexToGoal(elementWiseGoalIndex(i)));
		}
		this.goalList = Utils.toArray(goalList);
		
		for (MotionAnnotation ann : database.getTransitionAnnotations()) {
			if (ann.interactionFrame > 0) {
				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
				int margin = 15;
				for (int i = 0; i < margin; i++) {
					int idx = mIndex - i - 1;
					if (idx < 0) break;
					if (fMotionMap[idx] == null) continue;
					actionPrefix[fMotionMap[idx].index] = 1;
				}
			}
		}
	}

	public void calcAndSave(String file) {
		calcTransitionMap();
		
		int maxTime = MAX_TIME;
		final int goalSize = totalGoalSize();
		distanceTable = new double[maxTime][goalSize][nodeList.length];
		traceTable = new int[maxTime][goalSize][nodeList.length];
		for (int t = 0; t < maxTime; t++) {
			for (int r = 0; r < goalSize; r++) {
				for (int i = 0; i < nodeList.length; i++) {
					distanceTable[t][r][i] = Integer.MAX_VALUE;
					traceTable[t][r][i] = -1;
				}
			}
		}
		
		for (int goalIdx = 0; goalIdx < goalSize; goalIdx++) {
			for (int i = 0; i < nodeList.length; i++) {
				distanceTable[0][goalIdx][i] = getError(0, i, goalList[goalIdx]);
			}
		}
		
//		Configuration.MAX_THREAD = 1;
		for (int time = 1; time < maxTime; time++) {
			System.out.println("time :: " + time);
			final int t = time;
			Utils.runMultiThread(new IterativeRunnable(){
				@Override
				public void run(int next) {
					
					int nextMotionIndex = nodeList[next].nextMotionIndex;
//					System.out.println("next : " + next);
					for (int goalIdx = 0; goalIdx < goalSize; goalIdx++) {
						double[] currentGoal = goalList[goalIdx];
						double minDistance = Integer.MAX_VALUE;
						int minIndex = -1;
						
						for (Transition transition : sparseTransitions[next]) {
							int target = transition.target;
							double d = transition.distance;
//						}
//						for (int target = 0; target < nodeList.length; target++) {
//							double d = transitionMap[next][target];
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
							
							double[] movedGoal = moveGoal(currentGoal, movementMap[target]);
							double error = getError(t, target, movedGoal);
							d = interpolatedDistance(t-1, target, movedGoal) + d + error;
							
							if (d < minDistance) {
								minIndex = target;
								minDistance = d;
							}
						}
						distanceTable[t][goalIdx][next] = minDistance;
						traceTable[t][goalIdx][next] = minIndex;
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
	
	private static class Transition implements Comparable<Transition>{
		int target;
		double distance;
		
		public Transition(int target, double distance) {
			this.target = target;
			this.distance = distance;
		}

		@Override
		public int compareTo(Transition o) {
			return Double.compare(distance, o.distance);
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
	
	public static class GoalDimensionInfo{
		int division;
		double min;
		double max;
		boolean isCyclic;
		
		private double g2iRatio;
		private double i2gRatio;
		
		public GoalDimensionInfo(int division, double min, double max, boolean isCyclic) {
			this.division = division;
			this.min = min;
			this.max = max;
			this.isCyclic = isCyclic;
		}
		
		public void init() {
			double width = max - min;
			if (isCyclic) {
				i2gRatio = width/division;
			} else {
				i2gRatio = width/(division-1);
			}
			g2iRatio = 1/i2gRatio;
		}
		
		public double goalToIndex(double goal) {
			return (goal - min) * g2iRatio;
		}
		
		public double indexToGoal(int index) {
			return min + index * i2gRatio;
		}
		
		public int validateIndex(int index) {
			if (isCyclic) {
				return (index + division)%division;
			} else {
				if (index < 0) return 0;
				if (index > division-1) return division -1;
				return index;
			}
		}
	}
}
