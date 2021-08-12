package mrl.motion.graph;

import java.util.ArrayList;
import java.util.Random;

import mrl.motion.data.Motion;
import mrl.motion.graph.MGraph.MGraphEdge;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class MGraphExplorer {
	
	public static int TRANSITION_LIMIT = 15;
	public static int CHECK_MARGIN = 40;
//	public static int TRANSITION_LIMIT = 30;
//	public static int CHECK_MARGIN = 60;

	private MGraph graph;
	private Motion[] totalMotions;
	private MGraphNode[] nodeList;
	public int[] visitCounts;
	private ArrayList<ArrayList<MGraphEdge>> transitionMap;
	private MGraphEdge[] sequentialLinks;
	
	private Random rand = MathUtil.random;
	
	private ArrayList<Integer> interactionFrames;
	private int MAX_NON_INTERACTION_LIMIT = Integer.MAX_VALUE;
	
	public double interactionPickOffset = 0;

	public MGraphExplorer(MGraph graph) {
		this.graph = graph;
		totalMotions = graph.getDatabase().getMotionList();
		nodeList = graph.getNodeList();
		for (int i = 0; i < nodeList.length; i++) {
			if (nodeList[i].index != i) throw new RuntimeException(Utils.toString(i, nodeList[i].index));
		}
		visitCounts = new int[nodeList.length];
		
		MGraphEdge[][] linkMap = graph.getEdgeList();
		sequentialLinks = new MGraphEdge[nodeList.length];
		transitionMap = new ArrayList<ArrayList<MGraphEdge>>();
		for (int i = 0; i < sequentialLinks.length; i++) {
			ArrayList<MGraphEdge> tList = new ArrayList<MGraphEdge>();
			for (MGraphEdge link : linkMap[i]){
				if (link.isSequential()){
					sequentialLinks[i] = link;
					if (link.source.index +1 != link.target.index){
						throw new RuntimeException(Utils.toString(link.source.index, link.target.index, link.source.motionIndex, link.target.motionIndex));
					}
				} else {
					tList.add(link);
				}
			}
			transitionMap.add(tList);
		}
	}
	
	public void setInteractionFrames(ArrayList<Integer> interactionFrames, int interactionLimit){
		this.interactionFrames = interactionFrames;
		MAX_NON_INTERACTION_LIMIT = interactionLimit;
	}
	
	protected void update(int totalLen) {
	}
			
	
	public ArrayList<int[]> explore(int size){
		MGraphNode current = nodeList[rand.nextInt(nodeList.length)];
		ArrayList<int[]> segmentList = new ArrayList<int[]>();
		int totalLen = 0;
		
		int prevStart = current.motionIndex;
		int nonInteractionLen = 0;
		long t = System.currentTimeMillis();
		while (true){
			ArrayList<Candidate> candidates = getCandidates(current);
			Candidate c = pick(candidates, nonInteractionLen);
			int startMIdx = current.motionIndex;
			while (true){
				visitCounts[current.index()]++;
				if (current == c.link.source) break;
				if (sequentialLinks[current.index()] == null){
					System.out.println("ERRR :: " + prevStart + ":: " + startMIdx + " : " + current.motionIndex + " :: " + c.link.source.motionIndex +" // " + c.link.target.motionIndex + " : " + c.link.isSequential());
				}
				current = sequentialLinks[current.index()].target;
				nonInteractionLen++;
				if (isInteraction(current)){
					nonInteractionLen = 0;
				}
			}
			if (!c.link.isSequential()){
				segmentList.add(new int[]{ prevStart, current.motionIndex });
				totalLen += (current.motionIndex - prevStart) + 1;
				if ((segmentList.size() % 10) == 0){
					System.out.println("graph segment generation : " + segmentList.size() + " : " + totalLen + " : " + (System.currentTimeMillis()-t)/1000);
				}
				if (totalLen >= size) break;
				update(totalLen);
				prevStart = c.link.target.motionIndex;
				nonInteractionLen++;
			}
			current = c.link.target;
			if (!c.link.isSequential()){
				for (int i = 0; i < TRANSITION_LIMIT; i++) {
					if (sequentialLinks[current.index()] == null) break;
					visitCounts[current.index()]++;
					current = sequentialLinks[current.index()].target;
					nonInteractionLen++;
					if (isInteraction(current)){
						nonInteractionLen = 0;
					}
				}
			}
			if (isInteraction(c.link)){
				nonInteractionLen = 0;
			}
				
		}
		

		int zeroCount = 0;
		for (int c : visitCounts){
			if (c == 0) zeroCount++;
		}
		System.out.println("zero Count :: " + zeroCount + " / " + visitCounts.length + " :: " + zeroCount/(double)visitCounts.length);
		return segmentList;
	}
	
	private Candidate pick(ArrayList<Candidate> candidates, int nonInteractionCount){
//		return Utils.pickRandom(candidates, MathUtil.random);
		
		double iteractionOffset = interactionPickOffset;
		if (nonInteractionCount > MAX_NON_INTERACTION_LIMIT){
			iteractionOffset = 1000000;
		}
		for (Candidate candidate : candidates){
			MGraphNode target = candidate.link.target;
			candidate.vCount = visitCounts[target.index];
			if (isInteraction(candidate.link)){
				candidate.offset = iteractionOffset;
			}
		}
		double pSum = 0;
		double[] accList = new double[candidates.size()];
		for (int i = 0; i < candidates.size(); i++) {
			double p = 1d/(candidates.get(i).vCount + 0.5);
			p = p*p;
			p += candidates.get(i).offset;
			pSum += p;
			accList[i] = pSum;
		}
		double p = rand.nextDouble()*pSum;
		for (int i = 0; i < accList.length; i++) {
			if (p <= accList[i]){
				return candidates.get(i);
			}
		}
		System.out.println("no candidates :: " + candidates.size());
		throw new RuntimeException();
	
//		for (Candidate candidate : candidates){
//			MGraphNode target = candidate.link.target;
//			ArrayList<Candidate> cc = getCandidates(target);
//			int minCount = getMinimumVCount(target.index());
//			for (Candidate c : cc){
//				minCount = Math.min(minCount, getMinimumVCount(c.link.target.index)); 
//			}
//			candidate.vCount = minCount;
//			if (nonInteractionCount > MAX_NON_INTERACTION_LIMIT){
//				double offset = 1000000;
//				if (isInteraction(candidate.link)){
//					candidate.offset = offset;
//				} else if (isInteractionable(target.index())){
//					candidate.offset = offset/1000;
//				}
//			}
//		}
//		double pSum = 0;
//		double[] accList = new double[candidates.size()];
//		for (int i = 0; i < candidates.size(); i++) {
//			double p = 1d/(candidates.get(i).vCount + 0.5);
//			p = p*p;
//			p += candidates.get(i).offset*10;
//			pSum += p;
//			accList[i] = pSum;
//		}
//		double p = rand.nextDouble()*pSum;
//		for (int i = 0; i < accList.length; i++) {
//			if (p <= accList[i]){
//				return candidates.get(i);
//			}
//		}
//		System.out.println("no candidates :: " + candidates.size());
//		throw new RuntimeException();
	}
	
	private boolean isInteraction(MGraphEdge link){
		return isInteraction(link.target);
	}
	private boolean isInteraction(MGraphNode node){
		if (interactionFrames == null) return false;
		int target = node.motionIndex;
		for (int index : interactionFrames){
			if (target < index && target >= index - TRANSITION_LIMIT*3){
				if (totalMotions[target].motionData == totalMotions[index].motionData) return true;
			}
		}
		return false;
	}
	
	private ArrayList<Candidate> getCandidates(MGraphNode node){
		ArrayList<Candidate> candidates = new ArrayList<Candidate>();
		for (int i = 0; i < CHECK_MARGIN; i++) {
			int idx = node.index()+i;
			if (idx >= nodeList.length) return candidates;
			for (MGraphEdge link : transitionMap.get(idx)){
				if (link.source.index() != idx){
					throw new RuntimeException(Utils.toString(link.source.index, idx, node.index));
				}
				candidates.add(new Candidate(link));
			}
			if (sequentialLinks[idx] == null){
				return candidates;
			}
		}
		int last = node.index + CHECK_MARGIN - 1;
		if (last < nodeList.length && sequentialLinks[last] != null){
			candidates.add(new Candidate(sequentialLinks[last]));
		}
		return candidates;
	}
	
	private int getMinimumVCount(int index){
		int minCount = visitCounts[index];
		for (int i = 0; i < CHECK_MARGIN; i++) {
			int idx = index+i;
			if (idx >= nodeList.length) break;
			minCount = Math.min(minCount, visitCounts[idx]);
			if (i >= TRANSITION_LIMIT){
				for (MGraphEdge link : transitionMap.get(idx)){
					minCount = Math.min(minCount, visitCounts[link.target.index]);
				}
			}
			if (sequentialLinks[idx] == null) break;
		}
		return minCount;
	}
	
	private boolean isInteractionable(int index){
		if (interactionFrames == null) return false;
		for (int i = 0; i < CHECK_MARGIN; i++) {
			int idx = index+i;
			if (idx >= nodeList.length) break;
			if (interactionFrames.contains(nodeList[i].motionIndex)) return true;
			if (i >= TRANSITION_LIMIT){
				for (MGraphEdge link : transitionMap.get(idx)){
					if (isInteraction(link)) return true;
				}
			}
			if (sequentialLinks[idx] == null) break;
		}
		return false;
	}
	
	
	private static class Candidate{
		MGraphEdge link;
		double vCount;
		double offset = 0;
		
		public Candidate(MGraphEdge link) {
			this.link = link;
		}
	}
}
