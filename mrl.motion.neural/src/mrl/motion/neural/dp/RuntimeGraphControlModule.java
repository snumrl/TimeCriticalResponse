package mrl.motion.neural.dp;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.LevelOfErrorGraph;
import mrl.motion.dp.MotionGraphDPbyTime;
import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraph.MGraphEdge;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.motion.graph.MGraphSearch;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.motion.viewer.module.RuntimeMotionController;
import mrl.util.IterativeRunnable;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;
import mrl.widget.app.Module;

public class RuntimeGraphControlModule extends Module{

	public static int DEFAULT_TIME = 40;
	
	private MGraph graph;
	private ArrayList<MotionAnnotation> annList;

	private MotionGraphDPbyTime mGraphDP_kick;
	private MotionGraphDPbyTime mGraphDP_punch;
	
	private ArrayList<LocoCandidate> candidates;
	
	@Override
	protected void initializeImpl() {
		getModule(MainViewerModule.class).getPickPoint();
		
		MDatabase database = LevelOfErrorGraph.loadDatabase();
		graph = new MGraph(database);
		annList = new ArrayList<MotionAnnotation>();
		boolean[] isKick = new boolean[graph.getNodeList().length]; 
		boolean[] isPunch = new boolean[isKick.length];
		for (MotionAnnotation ann : database.getEventAnnotations()) {
//		for (MotionAnnotation ann : database.getTransitionAnnotations()) {
			if (ann.interactionFrame > 0) {
				annList.add(ann);
				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
				MGraphNode node = graph.getNodeByMotion(mIndex);
				if (node == null) {
					System.out.println("ann :: " + ann);
					continue;
				}
				if (ann.type.equals("kick")) isKick[graph.getNodeByMotion(mIndex).index] = true;
				if (ann.type.equals("punch")) isPunch[graph.getNodeByMotion(mIndex).index] = true;
			}
		}
		
		mGraphDP_kick = new MotionGraphDPbyTime(graph);
		mGraphDP_kick.calc(isKick, DEFAULT_TIME);
		mGraphDP_punch = new MotionGraphDPbyTime(graph);
		mGraphDP_punch.calc(isPunch, DEFAULT_TIME);
		
	}
	
	private ArrayList<LocoCandidate> expand(MGraphNode node, final int timeLimit, int offset) {
		final long time = System.currentTimeMillis();
		candidates = new ArrayList<LocoCandidate>();
		int minTransMargin = MGraphSearch.TRANSITION_MARGIN;
		
		final ArrayList<Integer> toCheckOffsets = new ArrayList<Integer>();
		final ArrayList<Matrix2d> transformList = new ArrayList<Matrix2d>();
		Matrix2d t = Matrix2d.identity();
		for (int tOffset = 0; tOffset < timeLimit; tOffset++) {
			int mIndex = node.index + tOffset;
			if (!graph.isNextSequential(mIndex)) break; 
			t.mul(graph.getAdjacentEdgeList()[mIndex].transform2d);
			if (tOffset < offset) continue;
			
			toCheckOffsets.add(tOffset);
			transformList.add(new Matrix2d(t));
			candidates.add(new LocoCandidate(Pose2d.byBase(t), tOffset+1, graph.getNodeList()[mIndex+1], null));
		}
		
		Utils.runMultiThread(new IterativeRunnable() {
			@Override
			public void run(int index) {
				int tOffset = toCheckOffsets.get(index);
				Matrix2d t = transformList.get(index);
				int mIndex = node.index + tOffset;
				MGraphEdge[] eList = graph.getEdgeList()[mIndex];
				for (MGraphEdge edge : eList) {
					if (edge.isSequential()) continue;
					int eTarget = edge.target.index;
					int eTargetMoved = eTarget + minTransMargin;
					if (eTargetMoved >= graph.getNodeList().length) continue;
					if (!graph.isConnected(eTarget, eTargetMoved)) continue;
					
					ArrayList<LocoCandidate> list = new ArrayList<LocoCandidate>();
					Matrix2d transform = new Matrix2d(t);
					transform.mul(edge.transform2d);
					list.add(new LocoCandidate(Pose2d.byBase(transform), tOffset + 1, edge.target, edge));
					
					int remainTime = timeLimit - tOffset;
					MGraphNode current = edge.target;
					for (int i = 1; i < remainTime; i++) {
						if (!graph.isNextSequential(current.index)) break;
						MGraphEdge aEdge = graph.getAdjacentEdgeList()[current.index];
						transform.mul(aEdge.transform2d);
						list.add(new LocoCandidate(Pose2d.byBase(transform), tOffset + 1 + i, aEdge.target, edge));
						current = aEdge.target;
					}
					
					synchronized (candidates) {
						candidates.addAll(list);
						if (candidates.size() > 1500) break;
					}
					if (System.currentTimeMillis() - time > 25) break;
				}
			}
		}, toCheckOffsets.size());
		System.out.println("calcTime :: " + (System.currentTimeMillis()-time) + " : " + candidates.size());
		return candidates;
	}
	
	private ArrayList<LocoCandidate> expandLevel2(MGraphNode start, MGraphNode prevSeed, MGraphEdge prevEdge, int tOffset, int timeLimit, Matrix2d transform) {
		ArrayList<LocoCandidate> list = new ArrayList<LocoCandidate>();
		transform = new Matrix2d(transform);
		transform.mul(prevEdge.transform2d);
		list.add(new LocoCandidate(Pose2d.byBase(transform), tOffset, start, prevEdge));
		
		MGraphEdge[] aEdges = graph.getAdjacentEdgeList();
		int remainTime = timeLimit - tOffset;
		for (int t = 0; t < remainTime; t++) {
			int mIndex = start.index + t;
			if (!graph.isPrevSequential(mIndex)) break;
			if (isAction(mIndex)) continue;
			transform.mul(aEdges[mIndex].transform2d);
			
			
//			MGraphEdge[] eList = graph.getEdgeList()[mIndex];
//			for (MGraphEdge edge : eList) {
//				if (edge.isSequential()) continue;
//				int eTarget = edge.target.index;
//				if (isAction(eTarget)) continue;
//				int totalTime = tOffset + t + mGraphDP.distanceList[eTarget];
//				if (totalTime <= timeLimit) {
//					list.add(new ExpansionCandidate(prevSeed, actionNode, totalTime, prevEdge, edge));
//				}
//			}
		}
		return list;
	}
	
	private boolean isAction(int nodeIndex) {
		if (mGraphDP_kick.actionMatching[nodeIndex] != null) return true;
		if (mGraphDP_punch.actionMatching[nodeIndex] != null) return true;
		return false;
	}
	
	private static class LocoCandidate{
		Pose2d pose;
		int time;
		MGraphNode end;
		MGraphEdge edge;
		
		public LocoCandidate(Pose2d pose, int time, MGraphNode end, MGraphEdge edge) {
			this.pose = pose;
			this.time = time;
			this.end = end;
			this.edge = edge;
		}
	}
	
	private class GraphController extends RuntimeMotionController{
		Motion currentMotion;
		int transitionWait = 0;
		int passTime = 0;
		LocoCandidate prevCandidate;
		
		public GraphController(Motion motion) {
			currentMotion = new Motion(motion);
			Vector3d t = MathUtil.getTranslation(motion.root());
			t.x = t.z = 0;
			currentMotion.root().setTranslation(t);
			currentPose = PositionMotion.getPose(currentMotion);
		}

		@Override
		protected Motion step(Point3d mouse, Point2d mouseLocal) {
			MGraphNode node = graph.getNodeByMotion(currentMotion.motionIndex);
			if (transitionWait > 0) {
				transitionWait--;
				passTime--;
				currentMotion = Motion.stitchMotion(currentMotion, currentMotion.next);
			} else {
				int timeLimit = 25;
				ArrayList<LocoCandidate> candidates = expand(node, timeLimit, transitionWait);
				
				double minDist = Integer.MAX_VALUE;
				
			}
			return currentMotion;
		}
		
		private double measureDistance(Point2d target, Pose2d destPose, int reachTime, int minTime) {
			double dp = target.distance(destPose.position);
			dp = dp*dp;
			double dt = Math.max(reachTime - minTime, 0);
			dt = dt*dt;
			double tRatio = 10/*cm*/ / 4d /*frame*/;
			tRatio = tRatio*tRatio;
			return dp + dt*tRatio;
		}
	}

}
