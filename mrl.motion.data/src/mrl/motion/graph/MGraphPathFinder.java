package mrl.motion.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.vecmath.Matrix4d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MDatabase.FrameType;
import mrl.motion.data.Motion;
import mrl.motion.graph.MGraph.MGraphEdge;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.util.Configuration;
import mrl.util.Pair;

public class MGraphPathFinder {
	
	private MGraph graph;
	public int timeLimit;
	public int transitionLimit;
	public int maxResultCount;
	
	private int nodeSize;

	private MGraphNode[] nodeList;
	private MGraphEdge[][] edgeList;
	private MGraphEdge[][] transposedEdgeList;
	private MGraphEdge[] adjacentEdgeList;
	private MGraphEdge[] transposedAdjacentEdgeList;
	private FrameType[] typeList;
	
	private int typeChangeLimit;
	
	private int[] sourceClip;
	private int[] targetClip;
	
	public MGraphPathFinder(MGraph graph, int timeLimit, int transitionLimit, int maxResultCount) {
		this.graph = graph;
		this.timeLimit = timeLimit;
		this.transitionLimit = transitionLimit;
		this.maxResultCount = maxResultCount;
		
		nodeList = graph.getNodeList();
		edgeList = graph.getEdgeList();
		transposedEdgeList = graph.getTransposedEdgeList();
		adjacentEdgeList = graph.getAdjacentEdgeList();
		transposedAdjacentEdgeList = graph.getTransposedAdjacentEdgeList();
		nodeSize = nodeList.length;
		
		typeList = graph.getDatabase().getTypeList();
	}
	
	public MGraph getGraph() {
		return graph;
	}

	public MDatabase getDatabase(){
		return graph.getDatabase();
	}

	public ArrayList<Trajectory> findPath(int source, int target, int[] sourceClip, int[] targetClip){
		this.sourceClip = sourceClip;
		this.targetClip = targetClip;
		
		long t = System.currentTimeMillis(); 
		FrameType sType = typeList[source];
		FrameType tType = typeList[target];
		typeChangeLimit = (sType == tType) ? 0 : 1; 
		System.out.println("path type : " + sType + " -> " + tType + " : " + typeChangeLimit);
		
		
		MGraphNode sourceNode = graph.getNodeByMotion(source);
		MGraphNode targetNode = graph.getNodeByMotion(target);
		
		
		ArrayList<Trajectory> pathList = new ArrayList<Trajectory>();
		HashMap<MGraphNode, ArrayList<Trajectory>> sourceReachable = findReachableRange(sourceNode, false);
		HashMap<MGraphNode, ArrayList<Trajectory>> targetReachable = findReachableRange(targetNode, true);
		
		HashSet<Pair<Integer, Integer>> matchedIdSet = new HashSet<Pair<Integer,Integer>>();
		for (Entry<MGraphNode, ArrayList<Trajectory>> entry : sourceReachable.entrySet()){
			MGraphNode node = entry.getKey();
			ArrayList<Trajectory> sList = entry.getValue();
			ArrayList<Trajectory> tList = targetReachable.get(node);
			if (tList == null) continue;
			
			for (Trajectory sourceT : sList){
				for (Trajectory targetT : tList){
					Pair<Integer, Integer> idPair = new Pair<Integer, Integer>(sourceT.pathID, targetT.pathID);
					if (matchedIdSet.contains(idPair)) continue;
					matchedIdSet.add(idPair);
					Trajectory path = getMatchingPath(sourceT, targetT);
					if (path != null){
						pathList.add(path);
					}
				}
			}
		}
		System.out.println("path finding time : " + (System.currentTimeMillis() - t));
		return pathList;
	}
//	public ArrayList<Trajectory> findPath(Motion source, Motion target){
//		FrameType sType = typeList[source.motionIndex];
//		FrameType tType = typeList[target.motionIndex];
//		typeChangeLimit = (sType == tType) ? 0 : 1; 
//		System.out.println("path type : " + sType + " -> " + tType + " : " + typeChangeLimit);
//		
//		
//		MGraphNode sourceNode = graph.getNodeByMotion(source);
//		MGraphNode targetNode = graph.getNodeByMotion(target);
//		
//		
//		ArrayList<Trajectory> pathList = new ArrayList<Trajectory>();
//		HashMap<MGraphNode, ArrayList<Trajectory>> sourceReachable = findReachableRange(sourceNode, false);
//		HashMap<MGraphNode, ArrayList<Trajectory>> targetReachable = findReachableRange(targetNode, true);
//		
//		HashSet<Pair<Integer, Integer>> matchedIdSet = new HashSet<Pair<Integer,Integer>>();
//		for (Entry<MGraphNode, ArrayList<Trajectory>> entry : sourceReachable.entrySet()){
//			MGraphNode node = entry.getKey();
//			ArrayList<Trajectory> sList = entry.getValue();
//			ArrayList<Trajectory> tList = targetReachable.get(node);
//			if (tList == null) continue;
//			
//			for (Trajectory sourceT : sList){
//				for (Trajectory targetT : tList){
//					Pair<Integer, Integer> idPair = new Pair<Integer, Integer>(sourceT.pathID, targetT.pathID);
//					if (matchedIdSet.contains(idPair)) continue;
//					matchedIdSet.add(idPair);
//					Trajectory path = getMatchingPath(sourceT, targetT);
//					if (path != null){
//						pathList.add(path);
//					}
//				}
//			}
//		}
//		return pathList;
//	}
	
	
	private Trajectory getMatchingPath(Trajectory source, Trajectory target){
		if (source.typeChangeCount + target.typeChangeCount > typeChangeLimit) return null;
		if (source.transitionAfterCount + target.transitionAfterCount < Configuration.MOTION_TRANSITON_MARGIN) return null;
		
		Trajectory t = source;
		while (true){
			if (target.edge == null) break;
			t = t.move(target.edge, false);
			target = target.prev;
		}
		return t;
	}
	
	
	private HashMap<MGraphNode, ArrayList<Trajectory>> findReachableRange(MGraphNode startNode, boolean isInverse){
		int pathIDCount = 0;
		Trajectory seed = new Trajectory(startNode);
		seed.pathID = pathIDCount;
		// 시작점에서부터는 언제든지 transition 가능
		seed.transitionAfterCount = Configuration.MOTION_TRANSITON_MARGIN+1;
		
		MGraphEdge[][] eList = isInverse ? transposedEdgeList : edgeList;
		MGraphEdge[] aList = isInverse ? transposedAdjacentEdgeList : adjacentEdgeList;
		
		HashMap<MGraphNode, ArrayList<Trajectory>> reachableMap = new HashMap<>();
		while (isValid(seed)){
			addRechable(reachableMap, seed);
			MGraphEdge adjacentEdge = null;
			for (MGraphEdge edge : eList[seed.currentNode.index]){
				if (edge.transition == 0){
					adjacentEdge = edge;
				} else {
					Trajectory branch = seed.move(edge, isInverse);
					
					pathIDCount++;
					branch.pathID = pathIDCount;
					while (isValid(branch)){
						addRechable(reachableMap, branch);
						MGraphEdge adjEdge = aList[branch.currentNode.index];
						if (adjEdge == null) break;
						branch = branch.move(adjEdge, isInverse);
					}
				}
			}
			if (adjacentEdge == null) break;
			seed = seed.move(adjacentEdge, isInverse);
			if (isInClip(seed.currentNode, isInverse ? targetClip : sourceClip)){
				seed.timeSum = 0;
			}
		}
		
		return reachableMap;
	}
	private void addRechable(HashMap<MGraphNode, ArrayList<Trajectory>> reachableMap, Trajectory t){
		ArrayList<Trajectory> list = reachableMap.get(t.currentNode);
		if (list == null){
			list = new ArrayList<Trajectory>();
			reachableMap.put(t.currentNode, list);
		}
		list.add(t);
	}
	
	private boolean isValid(Trajectory t){
		if (t.timeSum > timeLimit/2) return false;
		if (t.typeChangeCount > typeChangeLimit) return false;
		return true;
	}
	
	private boolean isInClip(MGraphNode node, int[] clip){
		if (clip == null) return false;
		return node.motionIndex >= clip[0] && node.motionIndex <= clip[1];
	}
	
	public class Trajectory{
		public int timeSum;
		public int transitionSum;
		public int typeChangeCount;
		
		public MGraphNode currentNode;
		public MGraphEdge edge;
		public double transitionWeightSum = 0;
		public double transitionWeightMax = 0;
		public int transitionAfterCount;
		
		public int pathID;
		public Trajectory prev;
		
		
		public Trajectory(MGraphNode startNode){
			currentNode = startNode;
			prev = null;
			edge = null;
			
			timeSum = 0;
			transitionSum = 0;
			typeChangeCount = 0;
			transitionAfterCount = 0;
		}
		
		public Trajectory(Trajectory copy){
			timeSum = copy.timeSum;
			transitionSum = copy.transitionSum;
			currentNode = copy.currentNode;
			transitionWeightSum = copy.transitionWeightSum;
			transitionWeightMax = copy.transitionWeightMax;
			typeChangeCount = copy.typeChangeCount;
			prev = copy.prev;
			pathID = copy.pathID;
			transitionAfterCount = copy.transitionAfterCount;
		}
		
		public Trajectory move(MGraphEdge edge, boolean isInverse){
			if (!isInverse && edge.source != currentNode){
				System.out.println("error : " + edge.source.motionIndex + "->" + edge.target.motionIndex + " : " + currentNode.motionIndex + " : " + isInverse);
				throw new RuntimeException();
			}
			if (isInverse && edge.target != currentNode){
				System.out.println("error : " + edge.source.motionIndex + "->" + edge.target.motionIndex + " : " + currentNode.motionIndex + " : " + isInverse);
				throw new RuntimeException();
			}
			
			Trajectory next = new Trajectory(this);
			next.prev = this;
			next.edge = edge;
			
			FrameType sType = typeList[edge.source.motionIndex];
			FrameType tType = typeList[edge.target.motionIndex];
			if (sType != tType){
				next.typeChangeCount++;
			}
			
			next.timeSum++;
			next.transitionSum += edge.transition;
			next.currentNode = isInverse ? edge.source : edge.target;
			
			if (edge.transition != 0){
				next.transitionWeightSum += edge.weight;
				next.transitionWeightMax = Math.max(transitionWeightMax, edge.weight);
				next.transitionAfterCount = 0;
			} else {
				next.transitionAfterCount++;
			}
			
			return next;
		}
		
		public int[][] getPath(){
			LinkedList<int[]> pathList = new LinkedList<int[]>();
			Trajectory t = this;
			int end = t.currentNode.motionIndex; 
			while (true){
				if (t.edge == null || t.edge.transition != 0){
					pathList.addFirst(new int[]{ t.currentNode.motionIndex, end });
					if (t.edge != null) end = t.edge.source.motionIndex;
				}
				if (t.prev == null) break;
				t = t.prev;
			}
			return pathList.toArray(new int[pathList.size()][]);
		}
	}
	
}
