package mrl.motion.graph;

import java.util.ArrayList;
import java.util.LinkedList;

import mrl.motion.graph.MGraphGenerator.MGGEdge;
import mrl.motion.graph.MGraphGenerator.MGGNode;

public class MStrongComponent {

	public MGGNode[] nodeList;
	public MGGEdge[][] edgeList;
	public int edgeCount;
	
	private int index = 0;
	private LinkedList<Vertex> set = new LinkedList<Vertex>();
	private Vertex[] vList;
	
	private ArrayList<Vertex> maxComponent;
	private int maxComponetSize = 0;
	
	private boolean[] isRemain;
	
	public void find(MGGNode[] nodeList, MGGEdge[][] edgeList){
		long t = System.currentTimeMillis();
		
		this.nodeList = nodeList;
		this.edgeList = edgeList;
		
		int n = nodeList.length;
		vList = new Vertex[n];
		for (int i = 0; i < vList.length; i++) {
			vList[i] = new Vertex(i);
		}
		
		for (Vertex v : vList){
			if (v.index < 0){
				connect(v);
			}
		}
		
		isRemain = new boolean[n];
		for (Vertex v : maxComponent){
			isRemain[v.i] = true;
		}
		
		MGGNode[] newNodeList = new MGGNode[maxComponetSize];
		MGGEdge[][] newEdgeList = new MGGEdge[maxComponetSize][];
		
		ArrayList<ArrayList<MGGEdge>> remainEdgeList = new ArrayList<ArrayList<MGGEdge>>();
		edgeCount = 0;
		for (int i = 0; i < n; i++) {
			if (isRemain[i]){
				ArrayList<MGGEdge> list = new ArrayList<MGGEdge>();
				for (MGGEdge edge : edgeList[i]){
					if (isRemain[edge.target.index]){
						list.add(edge);
						if (isRemain[edge.source.index] == false){
							System.out.println("????????????????????? : " + edge.source.index + " : " + i);
							System.exit(0);
						}
						edgeCount++;
					}
				}
				remainEdgeList.add(list);
			}
		}
		
		int idx = 0;
		for (int i = 0; i < n; i++) {
			if (isRemain[i]){
				newNodeList[idx] = nodeList[i];
				newNodeList[idx].index = idx;
				newEdgeList[idx] = remainEdgeList.get(idx).toArray(new MGGEdge[remainEdgeList.get(idx).size()]);
				idx++;
			} else {
				nodeList[i].index = -1;
			}
		}
		
		this.nodeList = newNodeList;
		this.edgeList = newEdgeList;
		
		System.out.println("scc time : " + (System.currentTimeMillis() - t));
	}
	
	public boolean[] getIsRemain() {
		return isRemain;
	}

	private void connect(Vertex v){
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
				MGGEdge edge = edgeList[v.i][i];
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
