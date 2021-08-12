package mrl.graph;

import java.util.ArrayList;
import java.util.HashSet;

import mrl.util.IterativeRunnable;
import mrl.util.TimeChecker;
import mrl.util.Utils;

public class DijkstraDistance<N extends DefaultNode, L extends DefaultLink<N>> {
	
	private double[][] linkMap;
	private double[][] distMap;
	private ArrayList<HashSet<Integer>> linkList = new ArrayList<HashSet<Integer>>();

	public DijkstraDistance(DefaultGraph<N, L> graph, boolean undirected) {
		TimeChecker.instance.state("dijk init");
		int size = graph.nodeList().size();
		linkMap = new double[size][size];
		for (int i = 0; i < size; i++) {
			linkList.add(new HashSet<Integer>());
			for (int j = 0; j < size; j++) {
				linkMap[i][j] = Integer.MAX_VALUE;
			}
		}
		
		for (L link : graph.linkList()){
			int i1 = link.source().index();
			int i2 = link.target().index();
			double d = link.distance();
			if (d < linkMap[i1][i2]){
				linkMap[i1][i2] = d;
			}
			linkList.get(i1).add(i2);
		}
		if (undirected){
			for (L link : graph.linkList()){
				int i1 = link.source().index();
				int i2 = link.target().index();
				double d = link.distance();
				if (d < linkMap[i2][i1]){
					linkMap[i2][i1] = d;
				}
				linkList.get(i2).add(i1);
			}
		}
	}
	
	public void caculateAll(){
		final int size = linkMap.length;
		TimeChecker.instance.state("dijk getDist");
		distMap = new double[size][size];
		
		Utils.runMultiThread(new IterativeRunnable() {
			@Override
			public void run(int i) {
				distMap[i] = getDist(i);
			}
		}, size);
	}
	
	public double[][] getDistMap() {
		return distMap;
	}

	public double[] getDist(int source){
		double[] dist = new double[linkMap.length];
		for (int i = 0; i < dist.length; i++) {
			dist[i] = Integer.MAX_VALUE;
		}
		dist[source] = 0;
		
		MinHeap heap = new MinHeap(dist.length);
		heap.addNode(source, 0);
		while (!heap.isEmpty()){
			int minIdx = heap.removeMinimum();
			for (int i : linkList.get(minIdx)){
				if (linkMap[minIdx][i] == Integer.MAX_VALUE) throw new RuntimeException();
				double d = dist[minIdx] + linkMap[minIdx][i];
				if (d < dist[i]){
					dist[i] = d;
					if (heap.contains(i)){
						heap.changeValue(i, d);
					} else {
						heap.addNode(i, d);
					}
				}
			}
		}
		return dist;
	}
}
