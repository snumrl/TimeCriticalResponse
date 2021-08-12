package mrl.graph;

import java.util.ArrayList;
import java.util.LinkedList;

import mrl.util.Utils;


public class StrongComponent {

	public int edgeCount;
	
	private int index = 0;
	private LinkedList<Vertex> set = new LinkedList<Vertex>();
	private Vertex[] vList;
	
	private ArrayList<Vertex> maxComponent;
	private int maxComponetSize = 0;
	
	private boolean[] isRemain;
	
	public <N extends DefaultNode, L extends DefaultLink<N>> void find(DefaultGraph<N, L> graph){
		long t = System.currentTimeMillis();
		
		N[] nodeList = Utils.toArray(graph.nodeList());
		L[][] edgeList = graph.getLinksBySource();
		
		int n = nodeList.length;
		vList = new Vertex[n];
		for (int i = 0; i < vList.length; i++) {
			vList[i] = new Vertex(i);
		}
		
		for (Vertex v : vList){
			if (v.index < 0){
				connect(v, edgeList);
			}
		}
		
		
		
		isRemain = new boolean[n];
		for (Vertex v : maxComponent){
			isRemain[v.i] = true;
		}
		
		ArrayList<L> remainLinks = new ArrayList<L>();
		for (int i = 0; i < n; i++) {
			if (isRemain[i]){
				for (L edge : edgeList[i]){
					if (isRemain[edge.target.index]){
						remainLinks.add(edge);
						if (isRemain[edge.source.index] == false){
							System.out.println("????????????????????? : " + edge.source.index + " : " + i);
							System.exit(0);
						}
					}
				}
			}
		}
		
		graph.clear();
		for (int i = 0; i < isRemain.length; i++) {
			if (isRemain[i]){
				graph.addNode(nodeList[i]);
			}
		}
		for (L link : remainLinks){
			graph.addLink(link);
		}
		System.out.println("scc time : " + (System.currentTimeMillis() - t) + " :: " + graph.nodeList().size() + " / " + n);
	}
	
	public boolean[] getIsRemain() {
		return isRemain;
	}

	private <L extends DefaultLink<?>> void connect(Vertex v, L[][] edgeList){
		LinkedList<Vertex> stack = new LinkedList<Vertex>();
		stack.add(v);
		LinkedList<Integer> indexStack = new LinkedList<Integer>();
		indexStack.add(0);
		
		Vertex lastW = null;
		while (!stack.isEmpty()){
			v = stack.getLast();
			
			if (indexStack.getLast() > 0){
				v.lowlink = Math.min(v.lowlink, lastW.lowlink);
			} else {
				v.index = index;
				v.lowlink = index;
				index++;
				set.addLast(v);
				v.isAdded = true;
			}
			boolean continueLoop = false;
			for (int i = indexStack.getLast(); i < edgeList[v.i].length; i++) {
				DefaultLink<?> edge = edgeList[v.i][i];
				Vertex w = vList[edge.target.index];
				if (w.index < 0){
					indexStack.removeLast();
					indexStack.addLast(i + 1);
					
					stack.addLast(w);
					indexStack.addLast(0);
					continueLoop = true;
					break;
//					connect(w);
//					v.lowlink = Math.min(v.lowlink, w.lowlink);
				} else if (w.isAdded){
					v.lowlink = Math.min(v.lowlink, w.lowlink);
				}
			}
			if (continueLoop) continue;
			
			if (v.lowlink == v.index){
				ArrayList<Vertex> component = new ArrayList<Vertex>();
				while (true){
					Vertex w = set.removeLast();
					component.add(w);
					w.isAdded = false;
					if (w == v) break;
				}
				
				if (component.size() > maxComponetSize){
					maxComponetSize = component.size();
					maxComponent = component;
				}
			}
			lastW = v;
			stack.removeLast();
			indexStack.removeLast();
		}
		
	}
	
	private static class Vertex{
		int i;
		int index = -1;
		int lowlink;
		boolean isAdded = false;
		public Vertex(int i) {
			this.i = i;
		}
	}
	
	
}

