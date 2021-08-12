package mrl.motion.neural.rl;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.parser.BVHWriter;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.gmm.GMMConfig;
import mrl.motion.neural.gmm.GMMConfig.GMMGoal;
import mrl.motion.neural.gmm.GMMStuntOnlyConfig;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;
import mrl.motion.neural.rl.PolicyDataGeneration.PDGControlParameter;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.MathUtil;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class PolicyTeacherStudentExperiment extends PolicyLearning{

	
	private MotionData mData;
	
	public PolicyTeacherStudentExperiment(GMMConfig config) {
		super(config, true);
	}

	public void generateData() {
		prepareStatistics();
		
		LinkedList<Integer> actionPreset = new LinkedList<Integer>();
		
		int[] preset = {
			3,4,5,4,3,2,5,4,3,2,3,4,5	
		};
		GMMConfig.TIME_RATIO = 0.66;
		boolean doEditing = false;
		
		
		if (doEditing) {
			AgilityModel.TIME_EXTENSION_RATIO = 0.33;
		} else {
			AgilityModel.TIME_EXTENSION_RATIO = 0;
		}
		for (int a : preset) {
			actionPreset.add(a);
		}
		((GMMStuntOnlyConfig)config).setActionPreset(actionPreset);
		int totalLength = actionPreset.size();
		
		MotionSegment totalSegment = null;
		ArrayList<Integer> constraintMotionIndices = new ArrayList<Integer>();
		
		RLAgent agent = new RLAgent(tData, matching);
		
		ArrayList<PDGControlParameter> controlParameters = new ArrayList<PDGControlParameter>();
		while(true) {
			RL_State state = agent.prepareState();
			MotionQuery action = python.getMeanAction(state);
			agent.proceedStep(state, action);
			if (agent.isFinished()) {
				GMMGoal goal = agent.goal;
				MotionSegment segment = MotionSegment.getPathMotion(config.tData.database.getMotionList(), agent.path.getPath(), 0);
				int timeConstraint = Math.min(agent.path.time, MathUtil.round(goal.timeLimit));
				MotionSegment edited = segment;
				if (doEditing) {
					edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, goal.getEditingConstraint(segment), timeConstraint);
				}
				edited = new MotionSegment(edited, 1, edited.length()-1);
				if (totalSegment == null) {
					totalSegment = edited;
					MotionSegment.alignToBase(totalSegment);
				} else {
					totalSegment = MotionSegment.stitch(totalSegment, edited, true);
				}
				
				int length = MathUtil.round(totalSegment.lastMotion().knot);
				constraintMotionIndices.add(length);
				controlParameters.add(new PDGControlParameter(length, goal, GMMConfig.TIME_RATIO));
				if (controlParameters.size() >= totalLength) break;
			}
		}
		FootSlipCleanup.clean(totalSegment);
		
		
		System.out.println("t len : " + totalSegment.length());
		ArrayList<Motion> totalMotions = totalSegment.getMotionList();
		ArrayList<Motion> mList = MotionData.divideByKnot(totalMotions);
		mList = MotionSegment.alignToBase(mList, 0);
		mData = new MotionData(mList);
//		BVHWriter writer = new BVHWriter();
//		String postfix = doEditing ? "edited" : "notEdited";
//		writer.write(new File("output\\experiment_" + postfix), mData);
		MainApplication.run(new ResultViewerModule());
	}
	
	
	class ResultViewerModule extends Module{

		@Override
		protected void initializeImpl() {
			getModule(MainViewerModule.class);
			getModule(ItemListModule.class).addSingleItem("motion", mData);
		}
		
	}
	
	public static void main(String[] args) {
		GMMConfig config = PolicyLearning.stunt_only(false, "_short07");
		GMMConfig.TIME_RATIO = 0.5;
		new PolicyTeacherStudentExperiment(config).generateData();
	}
}
