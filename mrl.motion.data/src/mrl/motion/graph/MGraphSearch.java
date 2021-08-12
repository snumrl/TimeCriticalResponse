package mrl.motion.graph;

import java.util.ArrayList;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MGraph.MGraphEdge;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public abstract class MGraphSearch {

	public static int TRANSITION_MARGIN = 6;
	public static int MAX_CANDIDATE_SIZE = Integer.MAX_VALUE;
	
	protected MGraph graph;
	protected ArrayList<SearchSeed> candidateList;
	protected MGraphEdge[] adjEdges;

	public MGraphSearch(MGraph graph) {
		this.graph = graph;
		adjEdges = graph.getAdjacentEdgeList();
	}
	
	protected abstract boolean isReachable(MGraphNode node, int transitionLimit, int remainTime);
	protected abstract MGraphNode getFinalizableEnd(SearchSeed seed, int maxTime);
	
	public ArrayList<SearchSeed> expand(MGraphNode start, int transitionLimit, int maxTime) {
		candidateList = new ArrayList<SearchSeed>();
		
		SearchSeed seed = new SearchSeed();
		seed.time = 0;
		seed.current = seed.start = start;
		seed.transform = Matrix2d.identity();
		
		checkFinalizable(seed, maxTime);
		expand(seed, transitionLimit, maxTime);
		return candidateList;
	}
	
	protected void expand(SearchSeed seed, int transitionLimit, int maxTime) {
		if (candidateList.size() >= MAX_CANDIDATE_SIZE) return;
		int remainTime = maxTime - seed.time;
		for (int t = 0; t < remainTime - TRANSITION_MARGIN; t++) {
			for (MGraphEdge edge : graph.getEdgeList()[seed.current.index]) {
				if (edge.isSequential()) continue;
				if (!isReachable(edge.target, transitionLimit - 1, remainTime - t - 1)) continue;
				
				SearchSeed newSeed = new SearchSeed(seed);
				newSeed.edgeList.add(edge);
				newSeed.current = edge.target;
				newSeed.time++;
				newSeed.transform.mul(edge.transform2d);
				checkFinalizable(newSeed, maxTime);
				
				progressTransitionMargin(newSeed);
				expand(newSeed, transitionLimit - 1, maxTime);
			}
			MGraphEdge aEdge = adjEdges[seed.current.index];
			if (aEdge == null) break;
			seed.current = aEdge.target;
			seed.time++;
			seed.transform.mul(aEdge.transform2d);
		}
	}
	
	protected void progressTransitionMargin(SearchSeed seed) {
		for (int iter = 0; iter < TRANSITION_MARGIN; iter++) {
			MGraphEdge aEdge = adjEdges[seed.current.index];
			if (aEdge == null) break;
			seed.current = aEdge.target;
			seed.time++;
			seed.transform.mul(aEdge.transform2d);
		}
	}
	
	protected void checkFinalizable(SearchSeed seed, int maxTime) {
		MGraphNode end = getFinalizableEnd(seed, maxTime);
		if (end == null) return;
		
		seed = new SearchSeed(seed);
		while (seed.current != end) {
			MGraphEdge aEdge = adjEdges[seed.current.index];
			seed.current = aEdge.target;
			seed.time++;
			seed.transform.mul(aEdge.transform2d);
		}
		seed.end = end;
		candidateList.add(seed);
	}
	
	public static class SearchSeed{
		public MGraphNode current;
		public int time;
		public Matrix2d transform;
		public MGraphNode start;
		public MGraphNode end;
		public ArrayList<MGraphEdge> edgeList = new ArrayList<MGraphEdge>();
		
		public double error = 0;
		
		SearchSeed(){
		}
		
		SearchSeed(SearchSeed copy){
			this.current = copy.current;
			this.time = copy.time;
			this.transform = new Matrix2d(copy.transform);
			this.start = copy.start;
			this.end = copy.end;
			this.edgeList = Utils.copy(copy.edgeList);
		}
		
		public Pose2d finalPose() {
			return Pose2d.byBase(transform);
		}
		
		public int[][] getPath(){
			ArrayList<int[]> pathList = new ArrayList<int[]>();
			int last = start.motionIndex;
			for (MGraphEdge edge : edgeList) {
				pathList.add(new int[] { last, edge.source.motionIndex });
				last = edge.target.motionIndex;
			}
			pathList.add(new int[] { last, end.motionIndex } );
			return Utils.toArray(pathList);
		}
	}
}
