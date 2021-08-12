package mrl.graph;

import java.util.ArrayList;
import java.util.HashMap;

public class SimplifiedGraph extends DefaultGraph<SimplifiedNode, SimplifiedLink> {

	
	public static <N extends DefaultNode,L extends DefaultLink<N>> 
						SimplifiedGraph simplifiy(DefaultGraph<N, L> input)
	{		
		SimplifiedGraph graph = new SimplifiedGraph();
		
		HashMap<N, SimplifiedNode> nodeMap = new HashMap<N, SimplifiedNode>();
		HashMap<L, SimplifiedLink> linkMap = new HashMap<L, SimplifiedLink>();
		for (N node : input.nodeList()){
			SimplifiedNode sNode = graph.addNode(new SimplifiedNode());
			sNode.addNode(node);
			nodeMap.put(node, sNode);
		}
		for (L link : input.linkList()){
			SimplifiedNode source = nodeMap.get(link.source());
			SimplifiedNode target = nodeMap.get(link.target());
			SimplifiedLink aLink = graph.addLink(new SimplifiedLink(source, target));
			linkMap.put(link, aLink);
		}
		graph.simplify();
		
		return graph;
	}
	
	private int[][] degrees;
	private boolean[] isRemoved;
	
	private SimplifiedGraph(){
		
	}
	
	
	private void simplify(){
		degrees = degrees();
		isRemoved = new boolean[degrees.length];
		SimplifiedNode[] originNodes = nodeList().toArray(new SimplifiedNode[0]);
		for (int i = 0; i < degrees.length; i++) {
			if (isRemoved[i]) continue;
			if (degrees[i][0] == 1 && degrees[i][1] == 1){
				SimplifiedNode node = originNodes[i];
				isRemoved[i] = true;
				mergePrev(node);
				mergePost(node);
			}
		}
		updateNodeIndices();
	}
	
	private void mergePrev(SimplifiedNode node){
		ArrayList<SimplifiedLink> links = getInLinks(node);
		if (links.size() != 1) throw new RuntimeException();
		
		SimplifiedNode prev = links.get(0).source();
		if (isRemoved[prev.index()]) return;
		if (degrees[prev.index()][0] == 1 && degrees[prev.index()][1] == 1){
			ArrayList<SimplifiedLink> inLinks = getInLinks(prev);
			ArrayList<SimplifiedLink> outLinks = getOutLinks(prev);
			if (outLinks.size() != 1 || inLinks.size() != 1) throw new RuntimeException();
			
			removeLink(outLinks.get(0));
			inLinks.get(0).changeTarget(node);
			removeNode(prev);
			isRemoved[prev.index()] = true;
			node.addNode(prev.mergedNodes().get(0));
			mergePrev(node);
		}
	}
	private void mergePost(SimplifiedNode node){
		ArrayList<SimplifiedLink> links = getOutLinks(node);
		if (links.size() != 1) throw new RuntimeException();
		
		SimplifiedNode post = links.get(0).target();
		if (isRemoved[post.index()]) return;
		if (degrees[post.index()][0] == 1 && degrees[post.index()][1] == 1){
			ArrayList<SimplifiedLink> inLinks = getInLinks(post);
			ArrayList<SimplifiedLink> outLinks = getOutLinks(post);
			if (outLinks.size() != 1 || inLinks.size() != 1) throw new RuntimeException();
			
			removeLink(inLinks.get(0));
			outLinks.get(0).changeSource(node);
			removeNode(post);
			isRemoved[post.index()] = true;
			node.addNode(post.mergedNodes().get(0));
			mergePost(node);
		}
	}
}
