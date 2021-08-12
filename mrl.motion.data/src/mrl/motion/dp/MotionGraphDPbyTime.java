package mrl.motion.dp;

import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraph.MGraphEdge;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.motion.graph.MGraphSearch;

public class MotionGraphDPbyTime {
	
	public MGraph graph;
	public int[] distanceList;
	public int[] traceList;
	public MGraphNode[] actionMatching;
	
	public MotionGraphDPbyTime(MGraph graph) {
		this.graph = graph;
		MGraphNode[] nodeList = graph.getNodeList();
		distanceList = new int[nodeList.length];
		traceList = new int[nodeList.length];
		for (int i = 0; i < nodeList.length; i++) {
			distanceList[i] = 1000000;
			traceList[i] = -1;
		}
	}
	
	private void marking(int nIndex, int tBase, int tMargin) {
		for (int i = 1; i < tMargin; i++) {
			int nIdx = nIndex - i;
			if (nIdx < 0) break;
			if (!graph.isNextSequential(nIdx)) break;
			if (actionMatching[nIdx] != null) break;
			
			int d = tBase + i;
			if (d < distanceList[nIdx]) {
				distanceList[nIdx] = d;
				traceList[nIdx] = nIdx+1;
			}
		}
	}
	
	
	public void calc(boolean[] isStartPoint, int maxTime) {
		MGraphNode[] nodeList = graph.getNodeList();
		actionMatching = new MGraphNode[distanceList.length];
		for (int i = 0; i < distanceList.length; i++) {
			if (!isStartPoint[i]) continue;
			for (int t = 0; t <= maxTime; t++) {
				int idx = i - t;
				if (idx < 0) break;
				if (!graph.isNextSequential(idx)) break;
				if (distanceList[idx] == 0) break;
				distanceList[idx] = t;
				if (t > 0) traceList[idx] = idx+1;
				actionMatching[idx] = nodeList[i];
			}
		}
		
		MGraphEdge[][] tEdgeList = graph.getTransposedEdgeList();
		for (int i = 0; i < distanceList.length; i++) {
			if (distanceList[i] != 0) continue;
			
			for (int t = 1; t < maxTime; t++) {
				int target = i - t;
				if (!graph.isNextSequential(target)) break;
				if (t < MGraphSearch.TRANSITION_MARGIN) continue;
				if (distanceList[target] == 0) break;
				for (MGraphEdge te : tEdgeList[target]) {
					if (te.target.index != target) throw new RuntimeException();
					int tBase = t + 1;
					int tMargin = maxTime - tBase;
					int sIdx = te.source.index;
					if (actionMatching[sIdx] == null&& tBase < distanceList[sIdx]) {
						if (actionMatching[target] == null) {
							System.out.println("?????????????? : " + te.source.motion + " : " + te.target.motion);
							for (int j = target; j <= i; j++) {
								System.out.println(j + " : " + graph.getNodeList()[j].motion);
							}
							System.out.println("bb : " + graph.getNodeList()[i].motion);
							throw new RuntimeException();
						}
						distanceList[sIdx] = tBase;
						traceList[sIdx] = target;
						marking(te.source.index, tBase+1, tMargin-1);
					}
				}
			}
		}
	}
	
}
