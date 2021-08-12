package mrl.motion.neural.agility;

import java.util.ArrayList;

import javax.vecmath.Point2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.MotionMatchingSampling.MMControlParameter;
import mrl.motion.neural.agility.PositionModel.PositionGoal;
import mrl.motion.neural.agility.match.MMatching;
import mrl.motion.neural.agility.match.MMatching.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class MotionMatchingSampling3 {
	
	public static boolean cleanFootSlip = true;

	public AgilityModel model;
	public MMatching matching;
	public ArrayList<Integer> constraintMotionIndices = new ArrayList<Integer>();
	public ArrayList<MMControlParameter> controlParameters = new ArrayList<MMControlParameter>();
	public TimeBasedList<Object> controlParameterObjects = new TimeBasedList<Object>();
	public ArrayList<Motion> totalNotEdited = new ArrayList<Motion>();
	
	public MotionMatchingSampling3(MDatabase database, AgilityModel model) {
		this.model = model;
		matching = model.makeMMatching(database);
	}
	
	public void reset() {
		constraintMotionIndices.clear();
		controlParameters.clear();
		controlParameterObjects.clear();
		totalNotEdited.clear();
	}
	
	private ArrayList<AgilityGoal> predefinedGoals;
	public ArrayList<Motion> samplePredefinedGoal(Motion startMotion, ArrayList<AgilityGoal> goalList){
		predefinedGoals = goalList;
		ArrayList<Motion> motionList = sample(startMotion, -1);
		predefinedGoals = null;
		return motionList;
	}
	
	public ArrayList<Motion> sample(int totalLength) {
		Motion start = matching.database().getMotionList()[matching.pickRandomStartMotion()];
		return sample(start, totalLength);
	}
	
	private ArrayList<Motion> sample(Motion startMotion, int totalLength) {
		reset();
		
		Motion current = startMotion;
		MDatabase db = matching.database();
		
		int length = 0;
		MotionSegment totalSegment = null;
		totalNotEdited = new ArrayList<Motion>();
		int gIndex = 0;
		long t = System.currentTimeMillis();
		while (totalLength < 0 || length < totalLength) {
			Pose2d currentPose = Pose2d.BASE;
			if (totalSegment != null) {
				currentPose = PositionMotion.getPose(totalSegment.lastMotion());
			}
			AgilityGoal goal;
			if (totalLength < 0) {
				if (gIndex >= predefinedGoals.size()) break;
				goal = predefinedGoals.get(gIndex);
				gIndex++;
			} else {
				goal = model.sampleRandomGoal(currentPose);
			}
			Object goalObject = goal.getControlParameterObject(currentPose, currentPose);
			System.out.println("search best : " + current + " :: " + goal);
			MatchingPath c = matching.searchBest(current.motionIndex, goal);
			
			if (c == null) {
				System.out.println("no path :: " + goal.maxSearchTime + " : " + goal.timeLimit + " : " + current);
			}
			MotionSegment segment = MotionSegment.getPathMotion(db.getMotionList(), c.getPath(), 0);
			int timeConstraint = Math.min(c.time, goal.timeLimit);
			
//			Motion[] mList = db.getMotionList();
			System.out.println("pp : " + length + " : " + Utils.toString(c.time, goal));
//			for (int[] p : c.getPath()) {
//				System.out.println(Utils.toString(p[1] - p[0], mList[p[0]], mList[p[1]]));
//			}
			MotionSegment notEdited = new MotionSegment(segment);
			MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, goal.getEditingConstraint(), timeConstraint);
//			MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, goal.getEditingConstraint(segment.firstMotion(), segment.lastMotion()), timeConstraint);
			for (Motion m : edited.getMotionList()) {
				if (Double.isNaN(m.root().m00)) {
					System.out.println("Edit fail :: " + current + " : " + goal.getEditingConstraint());
					throw new RuntimeException();
				}
			}
			
//			int eLength = edited.length();
//			{
//				Pose2d p1 = Pose2d.getPose(edited.firstMotion());
//				Pose2d p2 = Pose2d.getPose(edited.lastMotion());
//				Point2d pp = p1.globalToLocal(p2).position;
//				PositionGoal pGoal = (PositionGoal)goal;
//				System.out.println("constraint : " + goal.getEditingConstraint());
//				System.out.println("goalCmp :: " + pGoal.target + " : " + MathUtil.length(pGoal.target));
//				System.out.println(pp + " : " + MathUtil.length(pp));
//			}
			edited = new MotionSegment(edited, 1, edited.length()-1);
			notEdited = new MotionSegment(notEdited, 1, notEdited.length()-1);
			int eLen = edited.length();
			if (totalSegment == null) {
				totalSegment = edited;
				MotionSegment.alignToBase(totalSegment);
			} else {
				totalSegment = MotionSegment.stitch(totalSegment, edited, true);
			}
			
//			{
//				Pose2d p1 = Pose2d.getPose(totalSegment.getMotionList().get(totalSegment.length()-eLen));
//				Pose2d p2 = Pose2d.getPose(totalSegment.lastMotion());
//				Point2d pp = p1.globalToLocal(p2).position;
//				System.out.println(pp + " : " + MathUtil.length(pp));
//			}
			
			Motion pivot = totalSegment.getMotionList().get(totalSegment.length() - eLen);
			ArrayList<Motion> notEditedList = notEdited.getMotionList();
			notEditedList = MotionSegment.align(notEditedList, pivot);
			totalNotEdited.addAll(notEditedList);
			
			length = MathUtil.round(totalSegment.lastMotion().knot);
			System.out.println("result path ::: " + length + " : " + goal + " : " + c.current +  " : " + c.time + " / " + goal.timeLimit);
//			ArrayList<Motion> mm = totalSegment.getMotionList();
//			for (int i = 6; i >= 0; i--) {
//				Motion m = mm.get(mm.size() - i - 1);
//				System.out.println(c.motionList.get(c.motionList.size() - i - 1) + " : " + m + " : " + m.knot);
//			}
//			System.out.println("###########################");
			for (int i = controlParameterObjects.size(); i < length; i++) {
				controlParameterObjects.add(goalObject);
			}
			constraintMotionIndices.add(length);
			controlParameters.add(new MMControlParameter(length, goal));
			System.out.println("length :: " + length);
			current = c.current;
		}
		if (cleanFootSlip) FootSlipCleanup.clean(totalSegment);
		
		System.out.println("sample time : " + (System.currentTimeMillis()-t));
		System.out.println("t len : " + totalSegment.length());
//		System.out.println("befre knot ::");
//		ArrayList<Motion> mm = totalSegment.getMotionList();
//		for (int i = 280; i < 330; i++) {
//			System.out.println(i + " : " + mm.get(i) + " : " + mm.get(i).knot);
//		}
		ArrayList<Motion> totalMotions = totalSegment.getMotionList();
		for (int i = 0; i < totalMotions.size(); i++) {
			totalNotEdited.get(i).knot = totalMotions.get(i).knot;
		}
		ArrayList<Motion> mList = MotionData.divideByKnot(totalMotions);
		totalNotEdited = MotionData.divideByKnot(totalNotEdited);
		
		mList = MotionSegment.alignToBase(mList, 0);
		totalNotEdited = MotionSegment.alignToBase(totalNotEdited, 0);
		
//		System.out.println("after::");
//		for (int i = 280; i < 330; i++) {
//			System.out.println(i + " : " + mList.get(i) + " : " + mList.get(i).knot);
//		}
		
		return mList;
	}
	
}
