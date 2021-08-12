package mrl.motion.neural.agility.adaptive;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.neural.agility.match.MotionMatching.MatchingPath;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.agility.measure.AgilityMeasure;
import mrl.motion.neural.agility.measure.PlausibilityMeasure;
import mrl.motion.neural.agility.measure.WeightedKDE;
import mrl.motion.neural.agility.predict.RNNPythonPredictor;
import mrl.motion.neural.agility.visualize.MotionTrajectoryViewerModule;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.run.RuntimeMotionGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.NearestBuffer;
import mrl.util.Pair;
import mrl.util.Utils;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;
import mrl.widget.app.Item.ItemDescription;

public class AdpativeDataTestModule extends Module{

	private RNNPythonPredictor predictor;
	private ArrayList<double[]> dataList;
	private MDatabase database;
	private MotionMatching motionMatching;
	private Motion[] mList;
	AgilityModel model;

	@Override
	protected void initializeImpl() {
		
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		RotationModel.GOAL_TIME_LIMIT = 25; 
		RotationMotionMatching.rotErrorRatio = 5;
//		RotationModel.USE_STRAIGHT_SAMPLING = true;
		
		String folder = "dc_jog2";
		AgilityModel.TIME_EXTENSION_MIN_TIME = 4;
		model = new RotationModel();
		String label = "dc_jog2_dir_mm3_r50_25_ad";
//		String label = "dc_jog2_dir_mm3_r50_25";
//		String label = "dc_jog2_dir_mm2_r50_25_ad";
//		String label = "dc_jog2_dir_mm2_r50_25";
		
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT  = 300;
		database = TrainingDataGenerator.loadDatabase(folder);
		PlausibilityMeasure pMeasure = new PlausibilityMeasure(database);
		
		getModule(MainViewerModule.class);
		MathUtil.random = new Random(-5768438514678765400l);
		
//		String label = "dc_jog2_dir_mm_r90_17";
//		String label = "dc_jog2_dir_mm_r90_22";
//		String label = "dc_jog2_dir_mm_r90_27";
		
		
		predictor = new RNNPythonPredictor(label);
		AgilityMeasure measure = new AgilityMeasure(model, predictor);

		dataList = MotionDataConverter.motionToData(database);
		
		motionMatching = model.makeMotionMatching(database);
		mList = database.getMotionList();
		
		WeightedKDE kde = new WeightedKDE();
		int sampleSize = 300;
		ArrayList<Pair<AgilityGoal, Integer>> segments = new ArrayList<Pair<AgilityGoal,Integer>>();
		int failCount = 0;
		AgilityGoal goal;
		MathUtil.setRandomSeed();
		goal = model.sampleIdleGoal();
		measure.checkAgility(goal, goal.maxSearchTime);
		getModule(MainViewerModule.class);
		for (int iter = 0; iter < sampleSize; iter++) {
			System.out.println("iteration :: " + iter);
			double[] startMotion = predictor.prevOutput;
			Motion sMotion = new Motion(Utils.last(predictor.g.motionList));
			goal = model.sampleRandomGoal(measure.predictor.currentPose());
			int time = measure.checkAgility(goal, 60);
			if (time < 0) time = 70;
			if (time > goal.timeLimit) {
				failCount++;
				search(startMotion, goal, sMotion);						
				break;
			}
		}
	}
	
	private void search(double[] sMotion, AgilityGoal goal, Motion originStart) {
		double[] startData = predictor.normal.deNormalizeY(sMotion);
		Motion startMotion = findNearestMotions(startData, 1).get(0);
		
		MatchingPath path = motionMatching.searchBest(startMotion.motionIndex, goal);
		MotionSegment segment = MotionSegment.getPathMotion(mList, path.getPath(), 0);
		int timeConstraint = Math.min(path.time, goal.timeLimit);
		MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, goal.getEditingConstraint(), timeConstraint);
		ArrayList<Integer> gIndices = new ArrayList<Integer>();
		int goalMotionIndex = MathUtil.round(edited.lastMotion().knot);
		gIndices.add(goalMotionIndex);
		AgilityGoal idleGoal = model.sampleIdleGoal();
		
		int TRAIN_STEP_SIZE = 33;
		
		while (edited.lastMotion().knot <= TRAIN_STEP_SIZE + 2) {
			MatchingPath iPath = motionMatching.searchBest(edited.lastMotion().motionIndex, idleGoal);
			segment = MotionSegment.getPathMotion(mList, iPath.getPath(), 0);
			edited = MotionSegment.stitch(edited, segment, true);
			gIndices.add(MathUtil.round(edited.lastMotion().knot));
		}
		FootSlipCleanup.clean(edited);
		ArrayList<Motion> motionList = MotionData.divideByKnot(edited.getMotionList());
		
		ArrayList<double[]> dataList = blendWithStartPose(startData, motionList);
		// blendWithStartPose이 첫번째 frame을 제거해버리기 때문에, goal index도 거기에 맞춰 줄여준다.
		for (int i = 0; i < gIndices.size(); i++) {
			gIndices.set(i, gIndices.get(i)-1);
		}
		gIndices.set(gIndices.size()-1, dataList.size()-1);
		
		RuntimeMotionGenerator g = new RuntimeMotionGenerator();
		for (double[] data : dataList) {
			g.update(data);
		}
		ArrayList<double[]> targetList = new ArrayList<double[]>();
		int constIndex = 0;
		for (int t = 0; t < TRAIN_STEP_SIZE; t++) {
			double[] control;
			int controlIndex = gIndices.get(constIndex);
			if (t >= controlIndex) {
				constIndex++;
				controlIndex = gIndices.get(constIndex);
			}
			System.out.println("Ct : " + t + " : " + constIndex + " : " + controlIndex);
			control = goal.getControlParameter(g.motionList, t, controlIndex);
			targetList.add(control);
		}
		System.out.println("Searccccc  : " + g.motionList.size() + " / " + dataList.size());
		ArrayList<Motion> mList = Pose2d.getAlignedMotion(g.motionList, 0);
		originStart = Pose2d.getAlignedMotion(originStart);
		
		TimeBasedList<Pose2d> controlList = new TimeBasedList<Pose2d>();
		for (int i = 0; i < targetList.size(); i++) {
			double[] c = targetList.get(i);
			Vector2d direction = MathUtil.rotate(Pose2d.BASE.direction, c[0]);
			Pose2d p = new Pose2d(new Point2d(), direction);
			controlList.add(Pose2d.getPose(mList.get(i)).localToGlobal(p));
		}
		
		getModule(ItemListModule.class).addSingleItem("base", Pose2d.BASE);
		getModule(ItemListModule.class).addSingleItem("motion", new MotionData(mList));
		getModule(ItemListModule.class).addSingleItem("sMotion", new MotionData(Utils.singleList(originStart)), ItemDescription.red());
		getModule(ItemListModule.class).addSingleItem("goal", controlList, ItemDescription.red());
//		getModule(ItemListModule.class).addSingleItem("goal", goal.getControlParameterObject(Pose2d.BASE, Pose2d.BASE), ItemDescription.red());
	}
	
	public ArrayList<double[]> blendWithStartPose(double[] pose, ArrayList<Motion> motionList){
		Motion start = motionList.get(0);
		motionList = Utils.cut(motionList, 1, motionList.size()-1);
		ArrayList<double[]> dataList = MotionDataConverter.motionToData(motionList, start, false);
		if (pose.length == dataList.get(0).length + 1) {
			pose = Utils.cut(pose, 0, pose.length-2);
		}
		int blendInterval = 2;
		for (int i = 0; i < blendInterval; i++) {
			double ratio = (i+1)/(double)(blendInterval+1);
			double[] blended = MathUtil.interpolate(pose, dataList.get(i), ratio);
			for (int j = MotionDataConverter.ROOT_OFFSET; j < blended.length; j++) {
				dataList.get(i)[j] = blended[j];
			}
		}
		return dataList;
	}

	private ArrayList<Motion> findNearestMotions(double[] pose, int neighborSize) {
		NearestBuffer<Motion> buffer = new NearestBuffer<Motion>(neighborSize);
		for (int i = 0; i < dataList.size(); i++) {
			double d = MathUtil.distance(pose, dataList.get(i));
			buffer.add(database.getMotionList()[i], d);
		}
		return buffer.getElements();
	}
	
	public static void main(String[] args) {
		MainApplication.run(new AdpativeDataTestModule());
	}
}
