package mrl.motion.graph;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MDatabase.FrameType;
import mrl.motion.data.MDatabase.MotionInfo;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.MotionVector;
import mrl.util.Configuration;
import mrl.util.IterativeRunnable;
import mrl.util.Logger;
import mrl.util.MathUtil;
import mrl.util.TimeChecker;
import mrl.util.Utils;

public class MGraphGenerator {
	
//	private MotionTransform transform;
	
	private MGGNode[] nodeList;
	private Motion[] nodeMotionList;
	private MGGEdge[][] edgeList;
	private FrameType[] typeList;
	private double[] weightList;
	
	private boolean[] isProcessed;
	private int seedingIndex;
	private int finishedIndex;
	private double[][] distanceMap;
	
	private int localMinimumMargin = 5;

	private MotionDistByPoints dist;

	public MGraphGenerator(MDatabase database) {
		TimeChecker t = new TimeChecker();
		t.state("Motion Graph Construction");
		
//		transform = new MotionTransform();
		dist = database.getDist();
		
		Motion[] motionList = database.getMotionList();
		MotionInfo[] infoList = database.getMotionInfoList();
		typeList = database.getTypeList();
		weightList = database.getWeightList();
		
		
		ArrayList<MGGNode> _nodeList = new ArrayList<MGGNode>();
		for (int i = 0; i < typeList.length; i++) {
			if (typeList[i] == null) continue;
			if (!isValidMotion(motionList[i])) continue;
			_nodeList.add(new MGGNode(/*transform, */_nodeList.size(), motionList[i], infoList[i]));
		}
		nodeList = _nodeList.toArray(new MGGNode[_nodeList.size()]);
		System.out.println("filtered node size : " + nodeList.length + " / " + typeList.length);
		
		nodeMotionList = new Motion[nodeList.length];
		for (int i = 0; i < nodeList.length; i++) {
			nodeMotionList[i] = nodeList[i].motion;
		}
		
		edgeList = generateEdgeList();
		
		
		
		MStrongComponent sc = new MStrongComponent();
		sc.find(nodeList, edgeList);
		
		this.nodeList = sc.nodeList;
		this.edgeList = sc.edgeList;
		
		
//		t.state("Calcuate Edge Kinetic Energy");
//		calcKineticEnergy();
		
		t.state("Calcuate Edge Transform");
		calcEdgeTransform();
		t.state("end");
		Logger.line(t.toString());
		int transitions = 0;
		for (MGGEdge[] edges : sc.edgeList){
			for (MGGEdge edge : edges){
				if (edge.transition != 0){
					transitions++;
				}
			}
		}
		System.out.println("graph size :: " + nodeList.length + " : " + transitions + " : " + sc.edgeCount);
	}
	
	protected boolean isValidMotion(Motion m){
		Motion prev = m;
		for (int i = 0; i < 10; i++) {
			prev = prev.prev;
			if (prev == null) return false;
		}
		Motion next = m;
		for (int i = 0; i < 10; i++) {
			next = next.next;
			if (next == null) return false;
		}
		return true;
	}
	
	public void saveResult(){
		saveNodeList(new File(Configuration.MGRAPH_NODE_CACHE_FILE), nodeList);
		saveEdgeList(new File(Configuration.MGRAPH_EDGE_CACHE_FILE), edgeList);
	}
	
	
	private MGGEdge[][] generateEdgeList(){
		
		isProcessed = new boolean[nodeList.length];
		distanceMap = new double[nodeList.length][];
		seedingIndex = 0;
		
		
		int threadSize = 16;
		for (int i = 0; i < threadSize; i++) {
			addNewThread();
		}
		
		MGGEdge[][] edgeList = new MGGEdge[nodeList.length][0];
		long totalCount = 0;
		long addedCount = 0;
		
		
		long t = System.currentTimeMillis();
		for (int i = 0; i < nodeList.length; i++) {
			while (!isProcessed(i)){
			}
			
			if (i > 0 && (i % 1000) == 0){
				System.out.println("process number : " + i);
				System.out.println(addedCount + " / " + totalCount + " : " + (addedCount/i) + " : " + i + " / " + nodeList.length + " : " + (nodeList.length*(long)nodeList.length) + " : time " + (System.currentTimeMillis()-t)/1000);
			}
			
			MGGNode source = nodeList[i];
			if (i >= localMinimumMargin*2){
				int index = i - localMinimumMargin;
				ArrayList<MGGEdge> edges = new ArrayList<MGGEdge>();
				source = nodeList[index];
				if (typeList[source.motion.motionIndex] != null){
					
					for (int j = localMinimumMargin; j < nodeList.length - localMinimumMargin; j++) {
						double weight = distanceMap[index][j];
						MGGNode target = nodeList[j];
						if (typeList[target.motion.motionIndex] != null){
							if (source.motion.next == target.motion){
								edges.add(new MGGEdge(source, target, weight, 0));
								addedCount++;
							} else if (weight < getEdgeWeightLimit(source, target)){
								if (isMinimum(distanceMap, index, j, localMinimumMargin)){
									edges.add(new MGGEdge(source, target, weight, 1));
									addedCount++;
								}
							}
							totalCount++;
							
						}
					}
					
				}
				edgeList[i - localMinimumMargin] = edges.toArray(new MGGEdge[edges.size()]);
				
				synchronized (distanceMap) {
					distanceMap[i - localMinimumMargin*2] = null;
				}
			}
			
			synchronized (this) {
				finishedIndex = i;
			}
		}
		return edgeList;
	}
	
	protected double getEdgeWeightLimit(MGGNode source, MGGNode target){
		return Configuration.MGRAPH_EDGE_WEIGHT_LIMIT;
	}
	
	
	private int getSeed(){
		while (true){
			synchronized (this) {
				if (seedingIndex < finishedIndex + 1000) break;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		synchronized (this) {
			int seed = seedingIndex;
			if (seed >= nodeList.length) return -1;
			seedingIndex++;
			if ((seedingIndex % 1000) == 0){
				System.out.println("seed number : " + seedingIndex);
			}
			return seed;
		}
	}
	
	private boolean isProcessed(int index){
		synchronized (isProcessed) {
			return isProcessed[index];
		}
	}
	
	private void setProcessed(int index){
		synchronized (isProcessed) {
			isProcessed[index] = true;
		}
	}
	
	protected boolean isValid(MGGNode source, MGGNode target){
		if (source == target) return false;
//		if (source.isAir || target.isAir) return false;
//		
//		FrameType sourceType = typeList[source.motion.motionIndex];
//		FrameType targetType = typeList[target.motion.motionIndex];
//		if (sourceType == null || typeList[target.motion.motionIndex] == null) return false;
//		
//		if (sourceType == FrameType.Event || sourceType == FrameType.Prefix) return false;
//		if (targetType == FrameType.Event || targetType == FrameType.Postfix) return false;
		return true;
	}
	
	private void addNewThread(){
		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				while (true){
					int seed = getSeed();
					if (seed < 0) break;
					
					MGGNode source = nodeList[seed];
					double[] distanceList = new double[nodeList.length];
					for (int j = 0; j < nodeList.length; j++) {
						MGGNode target = nodeList[j];
						double weight;
						if (isValid(source, target)){
							weight = getEdgeWeight(source, target);
						} else {
							weight = Integer.MAX_VALUE;
						}
						weight = weight * Math.min(weightList[source.motion.motionIndex], weightList[target.motion.motionIndex]);
						distanceList[j] = weight;
					}
					synchronized (distanceMap) {
						distanceMap[seed] = distanceList;
					}
					
					setProcessed(seed);
				}
			}
		};
		Thread thread = new Thread(runnable);
		thread.start();
	}
	
	private boolean isMinimum(double[][] array, int x, int y, int margin){
		double min = array[x][y];
		for (int i = 0; i <= margin; i++) {
			for (int j = 0; j <= margin; j++) {
				if (i == 0 && j == 0) continue;
				
				if (array[x + i][y + j] < min) return false;
				if (array[x - i][y + j] < min) return false;
				if (array[x + i][y - j] < min) return false;
				if (array[x - i][y - j] < min) return false;
			}
		}
		return true;
	}
	
	private double getEdgeWeight(MGGNode source, MGGNode target){
		if (source.info.fileName.equals(target.info.fileName)){
			if (target.info.frameIndex == source.info.frameIndex + 1){
				double weight = getDiff(source, target);
				if (weight > Configuration.MGRAPH_EDGE_WEIGHT_LIMIT){
				}
				return weight;
			}
			if (Math.abs(target.info.frameIndex - source.info.frameIndex) < Configuration.MGRAPH_CONNECTION_MARGIN) return Integer.MAX_VALUE;
		}
//		if (source.isAir || target.isAir) return Integer.MAX_VALUE;
		double weight = getDiff(source, target);
		return weight;
	}
	
	public double getDiff(MGGNode source, MGGNode target){
		double offset = getDistanceOffset(source, target);
		return offset * dist.getDistance(source.motion.motionIndex, target.motion.motionIndex);
//		if (!MotionVector.isFootContactEqual(source.pose, target.pose)) return Integer.MAX_VALUE;
//		
//		double dSum = 0;
//		dSum += MotionVector.getMotionDistance(source.pose, target.pose, transform, dSum, Configuration.MGRAPH_EDGE_WEIGHT_LIMIT);
//		dSum += MotionVector.getVectorDistance(source.velocity, target.velocity, transform, dSum, Configuration.MGRAPH_EDGE_WEIGHT_LIMIT);
//		return dSum;
	}
	
	protected double getDistanceOffset(MGGNode source, MGGNode target) {
		return 1;
	}
	
	
	private void calcEdgeTransform(){
		Utils.runMultiThread(new IterativeRunnable() {
			@Override
			public void run(int index) {
				int i = index;
				final MGGEdge[] edges = edgeList[i];
				
				for (int j = 0; j < edges.length; j++) {
					MGGEdge e = edges[j];
					
					MGGNode source = e.source;
					MGGNode target = e.target;
					
					Matrix4d tRoot;
					if (e.transition == 0){
						tRoot = target.motion.root();
					} else {
						Matrix4d align = MotionTransform.getAlignTransform(source.motion.root(), target.motion.prev.root());
						tRoot = new Matrix4d(target.motion.root());
						tRoot.mul(align, tRoot);
					}
					
					double[] transform = MotionTransform.getTransform(source.motion.root(), tRoot);
					e.rotY = transform[0];
					e.transX = transform[1];
					e.transZ = transform[2];
				}
			}
		}, edgeList.length);
	}
	
//	private void calcKineticEnergy(){
//		final long t = System.currentTimeMillis();
//		final int[] progress = new int[1];
//		Utils.runMultiThread(new IterativeRunnable() {
//			@Override
//			public void run(int index) {
//				
//				MGGEdge[] edges = edgeList[index];
//				double[] energyList = new double[edges.length];
//				
//				for (int i = 0; i < energyList.length; i++) {
//					MGGEdge edge = edges[i];
//					
//					MGGNode source = edge.source;
//					MGGNode target = edge.target;
//					
//					ArrayList<Motion> mList = new ArrayList<Motion>();
//					mList.add(source.motion.prev);
//					mList.add(source.motion);
//					mList.add(source.motion.next);
//					double energy = MotionEditJNI.instance.getKineticEnergey(new MotionData(mList), 1, 1);
//					{
//						MotionSegment s1 = new MotionSegment(nodeMotionList, source.index, source.index);
//						mList = s1.getEntireMotion();
//						double e1 = MotionEditJNI.instance.getKineticEnergey(new MotionData(mList), 1, Configuration.BLEND_MARGIN);
//						
//						MotionSegment s2 = new MotionSegment(nodeMotionList, target.index, target.index);
//						mList = s2.getEntireMotion();
//						double e2 = MotionEditJNI.instance.getKineticEnergey(new MotionData(mList), Configuration.BLEND_MARGIN, mList.size()-1 -1);
//						
//						s1 = MotionSegment.stitch(s1, s2, true);
//						mList = s1.getEntireMotion();
//						double e = MotionEditJNI.instance.getKineticEnergey(new MotionData(mList), 1, mList.size()-1 -1);
//						
//						e1 *= Configuration.BLEND_MARGIN;
//						e2 *= Configuration.BLEND_MARGIN;
//						e *= 2*Configuration.BLEND_MARGIN;
//						energy +=(e - (e1 + e2));
//					}
//					edge.kineticEnergy = energy;
//				}
//				
//				int p;
//				synchronized (progress) {
//					progress[0] = progress[0] + 1;
//					p = progress[0];
//				}
//				if ((p % 1000) == 0){
//					System.out.println("progress : " + p + " / " + nodeList.length + " : " + (System.currentTimeMillis() - t)/1000);
//				}
//				
//			}
//		}, edgeList.length);
//	}
	
	private static void saveNodeList(File file, MGGNode[] nodeList){
		try {
			ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			os.writeInt(nodeList.length);
			for (int i = 0; i < nodeList.length; i++) {
				os.writeInt(nodeList[i].motion.motionIndex);
			}
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void saveEdgeList(File file, MGGEdge[][] edgeList){
		try {
			ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			os.writeDouble(Configuration.MGRAPH_EDGE_WEIGHT_LIMIT);
			os.writeInt(edgeList.length);
			for (int i = 0; i < edgeList.length; i++) {
				MGGEdge[] edges = edgeList[i];
				os.writeInt(edges.length);
				for (int j = 0; j < edges.length; j++) {
					MGGEdge edge = edges[j];
					os.writeInt(edge.source.index);
					os.writeInt(edge.target.index);
					os.writeDouble(edge.weight);
					os.writeDouble(edge.kineticEnergy);
					os.writeInt(edge.transition);
					os.writeDouble(edge.rotY);
					os.writeDouble(edge.transX);
					os.writeDouble(edge.transZ);
				}
			}
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	protected static class MGGNode{
		public int index;
		public Motion motion;
		public MotionInfo info;
		
//		public MotionVector pose;
//		public MotionVector velocity;
		public boolean isAir;
		
		public MGGNode(int index, Motion motion, MotionInfo info) {
			this.index = index;
			this.motion = motion;
			this.info = info;
			
//			pose = transform.toVector(motion);
			isAir = !motion.isLeftFootContact && !motion.isRightFootContact;
//			velocity = MotionVector.getMotionVelocity(motion, motion.next, pose, transform.toVector(motion.next));
		}
	}
	
	protected static class MGGEdge{
		public MGGNode source;
		public MGGNode target;
		public double weight;
		public int transition;
		public int index = -1;
		public double kineticEnergy = -1;
		
		
		public double rotY;
		public double transX;
		public double transZ;
		
		public MGGEdge(MGGNode source, MGGNode target, double weight, int transition) {
			this.source = source;
			this.target = target;
			this.weight = weight;
			this.transition = transition;
		}
	}
}

