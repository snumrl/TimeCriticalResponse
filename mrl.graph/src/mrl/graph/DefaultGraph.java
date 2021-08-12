package mrl.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

import mrl.util.Utils;

public class DefaultGraph<N extends DefaultNode, L extends DefaultLink<N>> {
	
	protected ArrayList<N> nodeList;
	protected ArrayList<L> linkList;
	
	public DefaultGraph(){
		nodeList = new ArrayList<>();
		linkList = new ArrayList<>();
	}

	public ArrayList<N> nodeList() {
		return nodeList;
	}

	public ArrayList<L> linkList() {
		return linkList;
	}
	
	public void clear(){
		for (N node : nodeList){
			node.index = -1;
		}
		nodeList.clear();
		linkList.clear();
	}
	
	public N addNode(N node){
		node.index = nodeList.size();
		nodeList.add(node);
		return node;
	}
	
	public L addLink(L link){
		linkList.add(link);
		return link;
	}
	
	public void removeNode(N node){
		if (!nodeList.remove(node)){
			throw new RuntimeException();
		}
	}
	
	public void removeNodeAndRelatedLinks(N node){
		if (!nodeList.remove(node)){
			throw new RuntimeException();
		}
		ArrayList<L> links = getInLinks(node);
		links.addAll(getOutLinks(node));
		for (L link : links){
			linkList.remove(link);
		}
	}
	
	public void removeLink(L link){
		if (!linkList.remove(link)){
			throw new RuntimeException();
		}
	}
	
	public void removeLinkWithoutCheck(L link){
		linkList.remove(link);
	}
	
	public void updateNodeIndices(){
		for (int i = 0; i < nodeList.size(); i++) {
			nodeList.get(i).index = i;
		}
	}
	
	public ArrayList<ArrayList<L>> getDuplicatedLinks(){
		SparseMatrix<ArrayList<L>> matrix  = new SparseMatrix<ArrayList<L>>(nodeList().size());
		for (L link : linkList()){
			int x = link.source().index();
			int y = link.target().index();
			ArrayList<L> v = matrix.get(x, y);
			if (v == null){
				v = new ArrayList<L>();
				matrix.put(x, y, v);
			}
			v.add(link);
		}
		
		ArrayList<ArrayList<L>> result = new ArrayList<ArrayList<L>>();
		for (ArrayList<L> list : matrix.values()){
			if (list.size() == 1) continue;
			result.add(list);
		}
		return result;
	}
	
	public DefaultGraph<N, L> subgraphByLink(LinkFilter<L> filter){
		DefaultGraph<N, L> subGraph = new DefaultGraph<N, L>();
		
		HashSet<N> validNodeSet = new HashSet<N>();
		
		for (L link : linkList){
			if (filter.isValid(link)){
				subGraph.linkList.add(link);
				validNodeSet.add((N)link.source);
				validNodeSet.add((N)link.target);
			}
		}
		
		subGraph.nodeList.addAll(sortNodeList(validNodeSet));
		return subGraph;
	}
	
	public L[][] getLinksBySource(){
		ArrayList<ArrayList<L>> linkMap = new ArrayList<ArrayList<L>>();
		for (int i = 0; i < nodeList.size(); i++) {
			linkMap.add(new ArrayList<L>());
		}
		@SuppressWarnings("unchecked")
		Class<L> c = (Class<L>)linkList.get(0).getClass();
		for (L link : linkList){
			linkMap.get(link.source.index).add(link);
		}
		ArrayList<L[]> arrayList = new ArrayList<L[]>();
		for (int i = 0; i < linkMap.size(); i++) {
			arrayList.add(Utils.toArray(linkMap.get(i), c));
		}
		return Utils.toArray(arrayList);
	}
	
	public ArrayList<GraphComponent<N, L>> getComponents(){
		ArrayList<GraphComponent<N, L>> result = new ArrayList<GraphComponent<N,L>>();
		HashSet<N> assignedNodes = new HashSet<N>();
		for (N base : nodeList){
			if (assignedNodes.contains(base)) continue;
			
			GraphComponent<N, L> component = new GraphComponent<N, L>();
			HashSet<N> detectedNodes = new HashSet<N>();
			
			HashSet<N> toVisitNodes = new HashSet<N>();
			detectedNodes.add(base);
			toVisitNodes.add(base);
			while (!toVisitNodes.isEmpty()){
				N node = toVisitNodes.iterator().next();
				toVisitNodes.remove(node);
				component.nodeList().add(node);
				
				for (N neighbor : neighbors(node)){
					if (detectedNodes.contains(neighbor)) continue;
					detectedNodes.add(neighbor);
					toVisitNodes.add(neighbor);
				}
			}
			
			assignedNodes.addAll(component.nodeList());
			
			if (component.nodeList().size() == 1) continue;
			component.linkList().addAll(filteredLinkByNodes(detectedNodes));
			
			ArrayList<N> sorted = sortNodeList(component.nodeList());
			component.nodeList().clear();
			component.nodeList().addAll(sorted);
			
			
			result.add(component);
		}
		@SuppressWarnings("unchecked")
		GraphComponent<N, L>[] array = result.toArray(new GraphComponent[0]);
		Arrays.sort(array, new Comparator<GraphComponent<N, L>>() {
			@Override
			public int compare(GraphComponent<N, L> o1, GraphComponent<N, L> o2) {
				return Integer.compare(o2.nodeList().size(), o1.nodeList().size());
			}
		});
		result.clear();
		for (GraphComponent<N, L> comp : array){
			result.add(comp);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<N> sortNodeList(Collection<N> nodes){
		Object[] array = nodes.toArray();
		Arrays.sort(array, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				DefaultNode n1 = (DefaultNode)o1;
				DefaultNode n2 = (DefaultNode)o2;
				return Integer.compare(n1.index(), n2.index());
			}
		});
		
		ArrayList<N> list = new ArrayList<N>();
		for (Object node : array){
			list.add((N)node);
		}
		return list;
	}
	
	public ArrayList<L> filteredLinkByNodes(HashSet<N> nodeSet){
		ArrayList<L> list = new ArrayList<L>();
		for (L link : linkList){
			if (nodeSet.contains(link.source) && nodeSet.contains(link.target)){
				list.add(link);
			}
		}
		return list;
	}
	
	public HashSet<N> neighbors(N node){
		HashSet<N> neighbors = new HashSet<N>();
		for (L link : linkList){
			if (link.source == node){
				neighbors.add(link.target);
			}
			if (link.target == node){
				neighbors.add(link.source);
			}
		}
		return neighbors;
	}
	
	public ArrayList<L> getOutLinks(N node){
		ArrayList<L> list = new ArrayList<L>();
		for (L link : linkList){
			if (link.source() == node){
				list.add(link);
			}
		}
		return list;
	}
	
	public ArrayList<L> getInLinks(N node){
		ArrayList<L> list = new ArrayList<L>();
		for (L link : linkList){
			if (link.target() == node){
				list.add(link);
			}
		}
		return list;
	}
	
	public HashSet<N> getInSources(N node){
		HashSet<N> list = new HashSet<N>();
		for (L link : getInLinks(node)){
			list.add(link.source);
		}
		return list;
	}
	public HashSet<N> getOutTargets(N node){
		HashSet<N> list = new HashSet<N>();
		for (L link : getOutLinks(node)){
			list.add(link.target);
		}
		return list;
	}
	
	public boolean isLinked(N source, N target){
		return getLink(source, target) != null;
	}
	
	public L getLink(N source, N target){
		for (L link : linkList()){
			if (link.source() == source && link.target() == target) return link;
		}
		return null;
	}
	
	public double[][] calcDistance(boolean undirected){
		DijkstraDistance<N, L> dijk = new DijkstraDistance<N, L>(this, undirected);
		dijk.caculateAll();
		return dijk.getDistMap();
	}
	
	/**
	 * return array[N][2]. index 0 is out degrees and 1 is in degrees.  
	 * @return
	 */
	public int[][] degrees(){
		int[][] result = new int[nodeList().size()][2];
		for (L link : linkList()){
			result[link.source().index()][0]++;
			result[link.target().index()][1]++;
		}
		return result;
	}
}
