package mrl.motion.neural.agile;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.dp.MotionGraphDPbyTime;
import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.motion.graph.MGraphSearch;
import mrl.motion.graph.MGraphSearch.SearchSeed;

public class ActionSearch {

	private MGraph graph;
	private String actionType;
	
	private MotionGraphDPbyTime mGraphDP;
	private MGraphSearch search;
	
	public ActionSearch(MGraph graph, String actionType) {
		this.graph = graph;
		this.actionType = actionType;
	}
	
	public void init(int maxTime) {
		MDatabase database = graph.getDatabase();
		boolean[] isAction = new boolean[graph.getNodeList().length]; 
		for (MotionAnnotation ann : database.getEventAnnotations()) {
			if (ann.interactionFrame > 0) {
				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
				MGraphNode node = graph.getNodeByMotion(mIndex);
				if (node == null) continue;
				if (ann.type.equals(actionType)) isAction[graph.getNodeByMotion(mIndex).index] = true;
			}
		}
		MotionGraphDPbyTime graphDP = new MotionGraphDPbyTime(graph);
		graphDP.calc(isAction, maxTime);
		init(graphDP);
	}
	
	public MotionGraphDPbyTime getGraphDP() {
		return mGraphDP;
	}

	public void init(MotionGraphDPbyTime graphDP) {
		mGraphDP = graphDP;
		search = new MGraphSearch(graph) {
			@Override
			protected boolean isReachable(MGraphNode node, int transitionLimit, int remainTime) {
				if (transitionLimit >= 2) return true;
				if (mGraphDP.distanceList[node.index] > remainTime) return false;
				if (transitionLimit == 0) {
					if (mGraphDP.actionMatching[node.index] == null) return false;
				}
				return true;
			}
			
			@Override
			protected MGraphNode getFinalizableEnd(SearchSeed seed, int maxTime) {
				return mGraphDP.actionMatching[seed.current.index];
			}
		};
	}
	
	public ArrayList<SearchSeed> getCandidates(MGraphNode node, int transitionLimit, int timeLimit) {
		return search.expand(node, transitionLimit, timeLimit);
	}
	
	
}
