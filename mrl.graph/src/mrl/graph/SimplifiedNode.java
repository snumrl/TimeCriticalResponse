package mrl.graph;

import java.util.ArrayList;

public class SimplifiedNode extends DefaultNode {

	private ArrayList<DefaultNode> mergedNodes = new ArrayList<DefaultNode>();

	public ArrayList<DefaultNode> mergedNodes() {
		return mergedNodes;
	}
	
	void addNode(DefaultNode node){
		mergedNodes.add(node);
	}
	
}
