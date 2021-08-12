package mrl.motion.neural.agility.adaptive;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.dp.TransitionData.TransitionNode;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.MotionMatchingSampling2;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.agility.match.MMatching;
import mrl.motion.neural.agility.match.MMatching.MatchingPath;
import mrl.motion.neural.agility.measure.AgilityMeasure;
import mrl.motion.neural.agility.measure.WeightedKDE;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.run.RuntimeMotionGenerator;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.NearestBuffer;
import mrl.util.Utils;

public class AdaptiveTrainer3 {

	static int TRAIN_STEP_SIZE = 48;
	static int TRAIN_BATCH_SIZE = 60;

	private AgilityModel model;
	private AgilityMeasure measure;
	private CombinedPythonModel2 predictor;
	private MotionMatchingSampling2 sampling;
	private boolean load;
	
	private WeightedKDE kde;
	
	private AgilityGoal lastGoal;
	private String name;
	
	private int searchTimeOffset = 10;
	private ArrayList<double[]> dataList;
	private MMatching mMatching;
	private Motion[] mList;
	
	public AdaptiveTrainer3(MDatabase database, AgilityModel model, String name, boolean load) {
		this.name = name;
		this.model = model;
		this.load = load;
		dataList = MotionDataConverter.motionToData(database);
		mMatching = model.makeMMatching(database);
		mList = database.getMotionList();
		predictor = new CombinedPythonModel2(name, TRAIN_STEP_SIZE, TRAIN_BATCH_SIZE, load);
		measure = new AgilityMeasure(model, predictor);
		sampling = new MotionMatchingSampling2(database, model);
		kde = new WeightedKDE();
	}
	
	ArrayList<double[]> xDataList = new ArrayList<double[]>();
	ArrayList<double[]> yDataList = new ArrayList<double[]>();
	public void train(int iteration) {
		int gCount = 0;
		while (true) {
			boolean add = sampling.sampleSingle(model.sampleRandomGoal(Pose2d.getPose(sampling.currentMotion())));
			if (add) {
				gCount += MotionMatchingSampling2.UPDATE_DATA_MARGIN;
				predictor.appendOriginalData(Utils.toArray(sampling.generatedXList), Utils.toArray(sampling.generatedYList));
				if (gCount > 1000) break;
//				if (gCount > 10000) break;
			}
		}
		
		if (!load) {
//			for (int iter = 0; iter < 200; iter++) {
			for (int iter = 0; iter < 50; iter++) {
				System.out.println("train iter1 :: " + name + " : " + iter);
				predictor.trainByOriginalData();
				if (iter > 0 && (iter % 50) == 0) {
					predictor.saveNetwork();
				}
			}
			predictor.saveNetwork();
		}
		{
			ArrayList<AgilityTrainingData> resultList = collectAgilityData(500, false);
			for (AgilityTrainingData d : resultList) {
				kde.addSample(d.goal.getControlParameter(), d.completionTime);
			}
			kde.updateSigma();
		}
		
		lastGoal = model.sampleIdleGoal();
		for (int iter = 0; iter < iteration; iter++) {
			System.out.println("train iter2 :: " + name + " : " + iter);
//			for (int i = 0; i < 5; i++) {
//				AgilityGoal goal = sampleGoalByMCMC(Pose2d.getPose(sampling.currentMotion()));
//				if (sampling.sampleSingle(goal)) {
//					predictor.appendOriginalData(Utils.toArray(sampling.generatedXList), Utils.toArray(sampling.generatedYList));
//				}
//			}
			
			predictor.trainByOriginalData();
			predictor.trainByAdaptiveData();
			predictor.trainByAdaptiveData();
			
			if (iter > 0 && (iter % 50) == 0) {
				predictor.saveNetwork();
			}
			if ((iter % 50) == 0) {
				kde.save("output\\kde_log\\" + name + "\\iter_" + iter + ".txt");
				predictor.saveNetwork();
			}
			
			if ((iter % 20) == 0){
				ArrayList<AgilityTrainingData> resultList = collectAgilityData(100, true);
				for (AgilityTrainingData d : resultList) {
					kde.addSample(d.goal.getControlParameter(), d.completionTime);
				}
				
				for (int i = 0; i < 100; i++) {
//					AgilityGoal goal =  model.sampleRandomGoal(predictor.currentPose());
					AgilityGoal goal = sampleGoalByMCMC(Pose2d.getPose(sampling.currentMotion()));
					if (sampling.sampleSingle(goal)) {
						predictor.appendOriginalData(Utils.toArray(sampling.generatedXList), Utils.toArray(sampling.generatedYList));
					}
				}
				
				for (AgilityTrainingData r : resultList) {
					if (r.completionTime > r.goal.timeLimit) {
						makeTrainTuple(r);
					}
				}
			}
			
		}
	}
	
	private ArrayList<Motion> findNearestMotions(double[] pose, int neighborSize) {
		NearestBuffer<Motion> buffer = new NearestBuffer<Motion>(neighborSize);
		TransitionNode[] nodeMap = mMatching.tData.motionToNodeMap;
		for (int i = 0; i < dataList.size(); i++) {
			if (nodeMap[i] == null) continue;
			double d = MathUtil.distance(pose, dataList.get(i));
			buffer.add(mList[i], d);
		}
		return buffer.getElements();
	}
	
	public ArrayList<double[]> blendWithStartPose(double[] pose, ArrayList<Motion> motionList){
		Motion start = motionList.get(0);
		motionList = Utils.cut(motionList, 1, motionList.size()-1);
		ArrayList<double[]> dataList = MotionDataConverter.motionToData(motionList, start, false);
		if (pose.length == dataList.get(0).length + 1) {
			pose = Utils.cut(pose, 0, pose.length-2);
		}
		int blendInterval = 3;
		for (int i = 0; i < blendInterval; i++) {
			double ratio = (i+1)/(double)(blendInterval+1);
			double[] blended = MathUtil.interpolate(pose, dataList.get(i), ratio);
			for (int j = MotionDataConverter.ROOT_OFFSET; j < blended.length; j++) {
				dataList.get(i)[j] = blended[j];
			}
		}
		return dataList;
	}
	
	private void makeTrainTuple(AgilityTrainingData r) {
		double[] startData = predictor.normal.deNormalizeY(r.startMotion);
		Motion startMotion = findNearestMotions(startData, 1).get(0);
		r.goal.timeLimit -= 1;
		MatchingPath path = mMatching.searchBest(startMotion.motionIndex, r.goal);
		MotionSegment segment = MotionSegment.getPathMotion(mList, path.getPath(), 0);
		int timeConstraint = Math.min(path.time, r.goal.timeLimit);
		MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, r.goal.getEditingConstraint(), timeConstraint);
		ArrayList<Integer> gIndices = new ArrayList<Integer>();
		int goalMotionIndex = MathUtil.round(edited.lastMotion().knot);
		gIndices.add(goalMotionIndex);
//		AgilityGoal idleGoal = model.sampleIdleGoal();
		while (edited.lastMotion().knot <= TRAIN_STEP_SIZE + 2) {
			AgilityGoal nextGoal = model.sampleRandomGoal(null);
			MatchingPath iPath = mMatching.searchBest(edited.lastMotion().motionIndex, nextGoal);
			segment = MotionSegment.getPathMotion(mList, iPath.getPath(), 0);
			edited = MotionSegment.stitch(edited, segment, true);
			gIndices.add(MathUtil.round(edited.lastMotion().knot));
		}
		FootSlipCleanup.clean(edited);
		ArrayList<double[]> dataList = blendWithStartPose(startData, MotionData.divideByKnot(edited.getMotionList()));
		// blendWithStartPose이 첫번째 frame을 제거해버리기 때문에, goal index도 거기에 맞춰 줄여준다.
//		for (int i = 0; i < gIndices.size(); i++) {
//			gIndices.set(i, gIndices.get(i)-1);
//		}
		gIndices.set(gIndices.size()-1, dataList.size());
		
		RuntimeMotionGenerator g = new RuntimeMotionGenerator();
		for (double[] data : dataList) {
			g.update(data);
		}
		Motion base = new Motion(g.motionList.get(0));
		base.root().setIdentity();
		ArrayList<Motion> motionList = new ArrayList<Motion>();
		motionList.add(base);
		motionList.addAll(g.motionList);
		
		ArrayList<double[]> targetList = new ArrayList<double[]>();
		int constIndex = 0;
		for (int t = 0; t < TRAIN_STEP_SIZE; t++) {
			double[] control;
			double activation;
			
			int controlIndex = gIndices.get(constIndex);
			if (t >= controlIndex) {
				constIndex++;
				controlIndex = gIndices.get(constIndex);
			}
			
			control = r.goal.getControlParameter(motionList, t, controlIndex);
			targetList.add(control);
			if (model.useActivation()) {
				activation = r.goal.getActivation(controlIndex - t);
				dataList.set(t, MathUtil.concatenate(dataList.get(t), new double[] { activation }));
			}
		}
		
		double[][] x = Utils.toArray(Utils.cut(targetList, 0, TRAIN_STEP_SIZE-1));
		double[][] y = Utils.toArray(Utils.cut(dataList, 0, TRAIN_STEP_SIZE-1));
		//xDataList.add(predictor.normal.normalizeX(x[0]));
		//yDataList.add(r.startMotion);
		for (int i = 0; i < x.length; i++) {
			x[i] = predictor.normal.normalizeX(x[i]);
			xDataList.add(x[i]);
		}
		for (int i = 0; i < y.length; i++) {
			y[i] = predictor.normal.normalizeY(y[i]);
			yDataList.add(y[i]);
		}
//		System.out.println("idxxxx : " + r.index);
		predictor.addTrainingTuple(r.index, x, y);
	}
	
	private AgilityGoal sampleGoalByMCMC(Pose2d currentPose) {
		AgilityGoal goal = model.sampleRandomGoal(currentPose);
		double eps = 0.000001;
		double pLast = Math.max(0, kde.getEstimatedWeight(lastGoal.getControlParameter()) - lastGoal.timeLimit) + eps;
		double pCurrent = Math.max(0, kde.getEstimatedWeight(goal.getControlParameter()) - lastGoal.timeLimit) + eps;
		double acceptRatio = pCurrent/pLast;
		double r = MathUtil.random.nextDouble();
		if (r <= acceptRatio) {
			lastGoal = goal;
		}
		return lastGoal;
	}
	
	private ArrayList<AgilityTrainingData> collectAgilityData(int trySize, boolean useMCMC) {
		predictor.clearStateCache();
		ArrayList<AgilityTrainingData> dataList = new ArrayList<AgilityTrainingData>();
		AgilityGoal prevGoal = model.sampleIdleGoal();
		measure.checkAgility(prevGoal, prevGoal.maxSearchTime);
		//System.out.println("Start motion : " + measure.motionList.size());
		int checkCount = 0;
		for (int i = 0; i < trySize; i++) {
			checkCount++;
			AgilityGoal goal;
			if (useMCMC) {
				goal = sampleGoalByMCMC(predictor.currentPose());
			} else {
				goal = model.sampleRandomGoal(predictor.currentPose());
			}
			double[] startMotion = predictor.prevOutput;
			predictor.saveCurrentState();
			
			int time = measure.checkAgility(goal, goal.maxSearchTime + searchTimeOffset);
			dataList.add(new AgilityTrainingData(i, goal, prevGoal, startMotion, time));
			prevGoal = goal;
		}
		return dataList;
	}
	
	private static class AgilityTrainingData{
		int index;
		AgilityGoal goal;
		AgilityGoal prevGoal;
		double[] startMotion;
		int completionTime;
		
		public AgilityTrainingData(int index, AgilityGoal goal, AgilityGoal prevGoal, double[] startMotion, int completionTime) {
			this.index = index;
			this.goal = goal;
			this.prevGoal = prevGoal;
			this.startMotion = startMotion;
			this.completionTime = completionTime;
		}
	}
	
	public static void main(String[] args) {
		String folder = "dc_jog_ue";
		String tPoseFile = "t_pose_ue.bvh";
		RotationModel.GOAL_TIME_LIMIT = 15; 
		RotationModel.USE_STRAIGHT_SAMPLING = true;
		AgilityModel.TIME_EXTENSION_RATIO = 0.2;
		TransitionData.STRAIGHT_MARGIN = 3;
		int rMargin = 10;
		RotationModel.ROTAION_ANGLE_MARGIN = Math.toRadians(rMargin);
		RotationModel.ROTATION_ERROR_WEIGHT = 200;
		AgilityModel model = new RotationModel();
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		
		String name = "dc_jog_ue_rot_test_rMargin10_15_adap4";
//		String name = "dc_jog2_dir_mm3_r50_25";
		AdaptiveTrainer3 trainer = new AdaptiveTrainer3(database, model, name, false);
		TRAIN_BATCH_SIZE = 120;
		trainer.kde.maxSampleSize = 3000;
		trainer.kde.sigmaUpdateInterval = 50;
		trainer.load = true;
		trainer.train(10000);
	}
}
