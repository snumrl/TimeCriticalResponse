package mrl.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class GraphShortestPath {
	
	public static <N extends DefaultNode, L extends DefaultLink<N>> 
			ArrayList<N> getShortestPath(DefaultGraph<N, L> graph, N source, N target){

		int size = graph.nodeList().size();
		ArrayList<N> prevList = new ArrayList<N>();
		for (int i = 0; i < size; i++) {
			prevList.add(null);
		}
		boolean[] isVisited = new boolean[size];
		
		HashSet<N> seeds = new HashSet<N>();
		seeds.add(source);
		isVisited[source.index()] = true;
		
		for (int i = 0; i < size; i++) {
			if (seeds.size() == 0) return null;
			
			HashSet<N> newSeeds = new HashSet<N>();
			for (N seed : seeds){
				for (L link : graph.getOutLinks(seed)){
					N lTarget = link.target();
					if (isVisited[lTarget.index()]) continue;
					newSeeds.add(lTarget);
					isVisited[lTarget.index()] = true;
					prevList.set(lTarget.index(), seed);
					
					if (lTarget == target){
						N pivot = target;
						LinkedList<N> list = new LinkedList<N>();
						while (pivot != null){
							list.addFirst(pivot);
							pivot = prevList.get(pivot.index());
						}
						ArrayList<N> aList = new ArrayList<N>();
						aList.addAll(list);
						return aList;
					}
				}
			}
			seeds = newSeeds;
		}
		
		return null;
	}
}
