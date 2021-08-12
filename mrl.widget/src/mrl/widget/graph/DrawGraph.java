package mrl.widget.graph;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.graph.DefaultGraph;
import mrl.graph.DefaultLink;
import mrl.graph.DefaultNode;
import mrl.util.MathUtil;

public class DrawGraph {

	public static double MOVE_LIMIT = 500;
	public static double nodeWeight = 10000;
	public static double linkWeight = 0.001;
	
	
	public ArrayList<DrawNode> nodeList = new ArrayList<DrawNode>();
	public ArrayList<DrawLink> linkList = new ArrayList<DrawLink>();
	
	protected double currentLimit;
	protected boolean isInitialized = false;
	
	public DrawGraph(){
		
	}
	
	public static <N extends DefaultNode, L extends DefaultLink<N>> DrawGraph genFromGraph(DefaultGraph<N,L> g){
		DrawGraph graph = new DrawGraph();
		for (int i = 0; i < g.nodeList().size(); i++) {
			DrawNode node = new DrawNode();
			node.data = g.nodeList().get(i);
			graph.nodeList.add(node);
		}
		
		for (L l : g.linkList()){
			DrawLink link = new DrawLink();
			link.data = l;
			link.source = graph.nodeList.get(l.source().index());
			link.target = graph.nodeList.get(l.target().index());
			graph.linkList.add(link);
		}
		graph.doLayout();
		return graph;
	}
	
	public void initialize(){
		isInitialized = true;
		currentLimit = MOVE_LIMIT;
		
		double radius = nodeList.size();
		for (int i = 0; i < nodeList.size(); i++) {
			DrawNode node = nodeList.get(i);
			node.index = i;
			node.relatedNodes.clear();
			node.position = new Point2d(Math.random()*radius, Math.random()*radius);
		}
		
		for (DrawLink link : linkList){
			link.source.relatedNodes.add(link.target);
			link.target.relatedNodes.add(link.source);
		}
	}
	
	
	public void setInitialized(boolean isInitialized) {
		this.isInitialized = isInitialized;
	}

	public void doLayout(){
		if (!isInitialized){
			initialize();
		}
		
		int maxIteration = 10000;
		for (int iter = 0; iter < maxIteration; iter++) {
			double energy = 0; 
			for (DrawNode node : nodeList){
				Vector2d force = new Vector2d();
				ArrayList<DrawNode> pushNodes = nodeList;
				for (DrawNode target : pushNodes){
					if (node == target) continue;
					Vector2d v = MathUtil.sub(node.position, target.position);
					double l = v.length();
					if (l > 500) continue;
					double d = l + 0.0001;
					v.normalize();
					v.scale(nodeWeight/(d*d));
					force.add(v);
				}
				for (DrawNode target : node.relatedNodes){
					if (node == target) continue;
					Vector2d v = MathUtil.sub(target.position, node.position);
					double d = v.length();
					v.normalize();
					v.scale(linkWeight*(d));
//						v.scale(linkWeight*(d*d));
					force.add(v);
				}
				double len = force.length();
				if (len <= 0.0000000001) continue;
				energy += len*len;
				
				force.normalize();
				force.scale(Math.min(len, currentLimit*2));
				node.position.add(force);
			}
			
			currentLimit = Math.min(currentLimit, energy);
		}
		
	}
	
	
}
