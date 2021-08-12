package mrl.graph;

import java.util.ArrayList;

public class GraphComponent<N extends DefaultNode, L extends DefaultLink<N>> {

	private ArrayList<N> nodeList;
	private ArrayList<L> linkList;
	
	public GraphComponent(){
		nodeList = new ArrayList<N>();
		linkList = new ArrayList<L>();
	}

	public ArrayList<N> nodeList() {
		return nodeList;
	}

	public ArrayList<L> linkList() {
		return linkList;
	}
}
