package mrl.motion.graph;

import java.util.ArrayList;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MGraph.MGraphEdge;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.motion.graph.MGraphSearch.SearchSeed;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public abstract class MGraphGoalSearch<E extends MGraphGoalSearch.SearchGoal> {

	public static int TRANSITION_MARGIN = 6;
	public static int MAX_CANDIDATE_SIZE = Integer.MAX_VALUE;
	
//	public static double TIME_EXTENSION_RATIO = 0;
//	public static double TIME_EXTENSION_MIN = 0;
	public static double TIME_EXTENSION_MIN_TIME = 12;
	public static double TIME_EXTENSION_RATIO = 0.33;
	public static double TIME_EXTENSION_MIN = 4;
	
	protected MGraph graph;
	protected ArrayList<SearchSeed> candidateList;
	protected MGraphEdge[] adjEdges;

	public MGraphGoalSearch(MGraph graph) {
		this.graph = graph;
		adjEdges = graph.getAdjacentEdgeList();
	}
	
	public MGraph getGraph() {
		return graph;
	}

	protected abstract boolean isTransitionReachable(SearchSeed seed, int transitionLimit, E goal);
	protected abstract boolean isFinalizableEnd(SearchSeed seed, E goal);
	public abstract E sampleRandomGoal(MGraphNode current);
	
	@SuppressWarnings("unchecked")
	public ArrayList<SearchSeed> expand(MGraphNode start, int transitionLimit, SearchGoal goal) {
		candidateList = new ArrayList<SearchSeed>();
		
		SearchSeed seed = new SearchSeed(start);
		expand(seed, transitionLimit, (E)goal);
		return candidateList;
	}
	
	protected void expand(SearchSeed seed, int transitionLimit, E goal) {
		if (candidateList.size() >= MAX_CANDIDATE_SIZE) return;
		checkFinalizable(seed, goal);
		int remainTime = goal.maxTime - seed.time;
		for (int t = 0; t < remainTime; t++) {
			if (transitionLimit > 0 && t < remainTime - TRANSITION_MARGIN) {
				for (MGraphEdge edge : graph.getEdgeList()[seed.current.index]) {
					if (edge.isSequential()) continue;
					
					SearchSeed newSeed = new SearchSeed(seed);
					newSeed.edgeList.add(edge);
					newSeed.current = edge.target;
					newSeed.time++;
					newSeed.transform(edge.transform2d);
					if (!isTransitionReachable(newSeed, transitionLimit - 1, goal)) continue;
	//				if (!isReachable(edge.target, transitionLimit - 1, remainTime - t - 1)) continue;
					
					if (progressTransitionMargin(newSeed)) {
						expand(newSeed, transitionLimit - 1, goal);
					}
				}
			}
			
			MGraphEdge aEdge = adjEdges[seed.current.index];
			if (aEdge == null) break;
			seed.current = aEdge.target;
			seed.time++;
			seed.transform(aEdge.transform2d);
			checkFinalizable(seed, goal);
		}
	}
	
	protected boolean progressTransitionMargin(SearchSeed seed) {
		for (int iter = 0; iter < TRANSITION_MARGIN; iter++) {
			MGraphEdge aEdge = adjEdges[seed.current.index];
			if (aEdge == null) {
				return iter > Math.round(TRANSITION_MARGIN*0.6);
			}
			seed.current = aEdge.target;
			seed.time++;
			seed.transform.mul(aEdge.transform2d);
		}
		return true;
	}
	
	protected void checkFinalizable(SearchSeed seed, E goal) {
		if (seed.time < TIME_EXTENSION_MIN_TIME) return;
//		if (seed.time < goal.minTime) return;
		if (isFinalizableEnd(seed, goal)) {
			seed = new SearchSeed(seed);
			seed.end = seed.current;
			candidateList.add(seed);
		}
	}
	
	public static abstract class SearchGoal{
		public int timeLimit;
		
		public int maxTime;
		
		public SearchGoal(int timeLimit) {
			this.timeLimit = timeLimit;
			int timeExtension = MathUtil.round(Math.max(TIME_EXTENSION_MIN, timeLimit*TIME_EXTENSION_RATIO));
			maxTime = timeLimit + timeExtension;
		}
		
		public abstract double measureError(SearchSeed s);
		public abstract Pose2d getPoseConstraint(SearchSeed c);
	}
	
	public static class SearchSeed{
		public MGraphNode current;
		public int time;
		public Matrix2d transform;
		public double rotation = 0;
		public MGraphNode start;
		public MGraphNode end;
		public ArrayList<MGraphEdge> edgeList = new ArrayList<MGraphEdge>();
		
		private Pose2d finalPose;
		
		SearchSeed(MGraphNode start){
			this.current = this.start = start;
			this.time = 0;
			this.transform = Matrix2d.identity();
			this.rotation = 0;
		}
		
		SearchSeed(SearchSeed copy){
			this.current = copy.current;
			this.time = copy.time;
			this.transform = new Matrix2d(copy.transform);
			this.rotation = copy.rotation;
			this.start = copy.start;
			this.end = copy.end;
			this.edgeList = Utils.copy(copy.edgeList);
		}
		
		public void transform(Matrix2d t) {
			transform.mul(t);
			rotation += t.getAngle();
		}
		
		public Pose2d finalPose() {
			if (finalPose != null) return finalPose;
			return finalPose = Pose2d.byBase(transform);
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
