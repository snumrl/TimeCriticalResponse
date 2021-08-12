package mrl.motion.neural.agile;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraph.MGraphEdge;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.motion.graph.MGraphExplorer;
import mrl.motion.graph.MGraphSearch;
import mrl.motion.graph.MGraphSearch.SearchSeed;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.position.PositionMotion;
import mrl.motion.position.PositionResultMotion.PositionFrame;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.NearestBuffer;
import mrl.util.Utils;

public class ImproveByGraph {
	
	public static int MAX_SEARCH_TIME = 80;
	public static int MAX_CANDIDATE_SIZE = 10000;

	private MGraph graph;
	private ArrayList<double[]> dataList;
	private ActionSearch search;
	
	public ImproveByGraph(MGraph graph, Normalizer normal) {
		this.graph = graph;
		MGraphSearch.MAX_CANDIDATE_SIZE = MAX_CANDIDATE_SIZE;
		search = new ActionSearch(graph, "kick");
		search.init(MAX_SEARCH_TIME);
		
		ArrayList<double[]> totalDataList = MotionDataConverter.motionToData(graph.getDatabase());
		dataList = new ArrayList<double[]>();
		for (MGraphNode node : graph.getNodeList()) {
			double[] data = totalDataList.get(node.motionIndex);
			data = MathUtil.concatenate(data, new double[] { 0 } );
			dataList.add(normal.normalizeY(data));
		}
	}
	
	private ArrayList<MGraphNode> findNearestNodes(double[] pose, int neighborSize) {
		NearestBuffer<MGraphNode> buffer = new NearestBuffer<MGraphNode>(neighborSize);
		for (int i = 0; i < dataList.size(); i++) {
			double d = MathUtil.distance(pose, dataList.get(i));
			buffer.add(graph.getNodeList()[i], d);
		}
		return buffer.getElements();
	}
	
	public MotionSegment notEditedResult;
	public int selectedTime = -1;
	public Pose2d estimatedPose;
	public ArrayList<Motion> generateMatchingMotion(double[] pose, Pose2d target, int desireTime) {
		ArrayList<MGraphNode> nodeList = findNearestNodes(pose, 3);
		
		MGraphNode pivot = nodeList.get(0);
		ArrayList<SearchSeed> candidates = search.getCandidates(pivot, 3, desireTime + 15);
		NearestBuffer<SearchSeed> buffer = new NearestBuffer<SearchSeed>(6);
		for (SearchSeed c : candidates) {
			c.error = measureError(c, target, desireTime);
			buffer.add(c, c.error);
		}
		SearchSeed c = buffer.sample(0.3, 1);
		estimatedPose = new Pose2d(c.finalPose());
		
		System.out.println("best candidate :: " + Utils.toString(c.error, c.time, c.finalPose(), desireTime, target));
		int[][] path = appendDummyMotion(c.getPath(), c.time + 50);
//		int[][] path = appendDummyMotion(c.getPath(), desireTime + 50);
		notEditedResult = MotionSegment.getPathMotion(graph.getDatabase().getMotionList(), path, 0);
		
		
		Pose2d start = MotionTransform.getPose(notEditedResult.getNotBlendedMotionList().get(0));
		Matrix4d transform = Pose2d.globalTransform(start, Pose2d.BASE).to3d();
		MotionSegment.align(notEditedResult, transform);
		
		Pose2d posConstraint = new Pose2d(target);
		posConstraint.direction = new Vector2d(Double.NaN, Double.NaN);
		selectedTime = Math.min(c.time, desireTime);
		MotionSegment segment = MotionEdit.getEditedSegment(new MotionSegment(notEditedResult), 0, c.time, posConstraint, selectedTime);
		FootSlipCleanup.clean(segment);
		ArrayList<Motion> list =  MotionData.divideByKnot(segment.getMotionList());
		System.out.println("dummy added motion length :: " + notEditedResult.length() + " -> " + Utils.toString(segment.getMotionList().size(), list.size(), c.time, desireTime));
		return list;
	}
	
	private double measureError(SearchSeed c, Pose2d target, int desireTime) {
		Pose2d pose = c.finalPose();
		double posError = positionDiff(pose.position, target.position);
		double timeError = Math.max(0, c.time - desireTime);
		timeError *= 6; // 10 frame == 60cm
		timeError = timeError*timeError;
		return posError + timeError;
	}
	
	public static double positionDiff(Point2d p1, Point2d p2) {
		double lengthDiff = MathUtil.length(p1) - MathUtil.length(p2);
		double angleDiff = Math.toDegrees(MathUtil.directionalAngle(p1, p2));
		angleDiff *= 1.5; // 40 degree = 60cm
		return lengthDiff*lengthDiff + angleDiff*angleDiff;
	}
	
	public ArrayList<double[]> blendWithStartPose(double[] pose, ArrayList<Motion> motionList){
		Motion start = motionList.get(0);
		motionList = Utils.cut(motionList, 1, motionList.size()-1);
		ArrayList<double[]> dataList = MotionDataConverter.motionToData(motionList, start, false);
		if (pose.length == dataList.get(0).length + 1) {
			pose = Utils.cut(pose, 0, pose.length-2);
		}
		int blendInterval = 5;
		for (int i = 0; i < blendInterval; i++) {
			double ratio = (i+1)/(double)(blendInterval+1);
			double[] blended = MathUtil.interpolate(pose, dataList.get(i), ratio);
			for (int j = MotionDataConverter.ROOT_OFFSET; j < blended.length; j++) {
				dataList.get(i)[j] = blended[j];
			}
		}
		return dataList;
	}
	
	private int[][] appendDummyMotion(int[][] path, int desireLen){
		ArrayList<int[]> newPath = new ArrayList<int[]>();
		int totalLen = 0;
		for (int[] p : path) {
			newPath.add(MathUtil.copy(p));
			totalLen += p[1] - p[0] + 1;
		}
		int lastMotion = Utils.last(path)[1];
		MGraphNode lastNode = graph.getNodeByMotion(lastMotion);
		for (int i = 0; i < desireLen - totalLen; i++) {
			MGraphEdge link = graph.getAdjacentEdgeList()[lastNode.index];
			if (link == null) {
				MGraphEdge[] eList = graph.getEdgeList()[lastNode.index];
				link = Utils.pickRandom(eList, MathUtil.random);
				newPath.add(new int[] { link.target.motionIndex, link.target.motionIndex });
			}
			lastNode = link.target;
			Utils.last(newPath)[1] = lastNode.motionIndex;
		}
		
		return Utils.toArray(newPath);
	}
	
}
