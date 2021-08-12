package mrl.motion.neural.agility.adaptive;

import java.util.ArrayList;
import java.util.Arrays;

import jep.JepException;
import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.motion.neural.agile.ImproveByGraph;
import mrl.motion.neural.agile.AgilityOfflineController.AgilityControlResult;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.agility.match.MotionMatching.MatchingPath;
import mrl.motion.neural.agility.measure.AgilityMeasure;
import mrl.motion.neural.dancecard.DanceCardGraphGenerator;
import mrl.motion.neural.data.ActionOnlyWaveControl;
import mrl.motion.neural.data.DataExtractor;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.ActionOnlyWaveControl.ActionParameter;
import mrl.motion.neural.run.RuntimeMotionGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.NearestBuffer;
import mrl.util.Utils;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class AdaptiveTrainer {

	static int TRAIN_STEP_SIZE = 48;
	static int TRAIN_BATCH_SIZE = 60;

	private AgilityModel model;
	private AgilityMeasure measure;
	private CombinedPythonModel predictor;
	private MotionMatching motionMatching;
	
	private Motion[] mList;
	private ArrayList<double[]> dataList;
	
	private double failRatio = -1;
	
	public AdaptiveTrainer(MDatabase database, AgilityModel model, String name) {
		this.model = model;
		predictor = new CombinedPythonModel(name, TRAIN_STEP_SIZE, TRAIN_BATCH_SIZE);
		measure = new AgilityMeasure(model, predictor);
		motionMatching = model.makeMotionMatching(database);
		dataList = MotionDataConverter.motionToData(database);
		mList = database.getMotionList();
	}
	
	ArrayList<double[]> xDataList = new ArrayList<double[]>();
	ArrayList<double[]> yDataList = new ArrayList<double[]>();
	public void train(int iteration) {
		for (int iter = 0; iter < iteration; iter++) {
//			jep.eval("run_one_epoch()");
//			jep.eval("train_from_adaptive_tuple()");
			System.out.println("iteration : " + iter + " : fail ratio : " + failRatio + " : " + TRAIN_STEP_SIZE);
			if ((iter % 10) == 0){
				predictor.trainByOriginalData();
			}
			predictor.trainByAdaptiveData();
			
			if (iter > 0 && (iter % 40) == 0) {
				predictor.saveNetwork();
			}
			if ((iter % 20) == 0) {
				long t0 = System.currentTimeMillis();
				ArrayList<AgilityTrainingData> resultList = collectAgilityData((iter == 0) ? 2000 : 200, (iter == 0) ? 200 : 40);
				System.out.println("collectAgilityData :: " + (System.currentTimeMillis()-t0));
				System.out.println("sample size :: " + resultList.size());
				long t1 = System.currentTimeMillis();
				for (AgilityTrainingData r : resultList) {
					makeTrainTuple(r);
				}
				System.out.println("makeTrainTuple :: " + (System.currentTimeMillis()-t1));
				
				
//				DataExtractor.writeData("output\\adaptiveData\\xData.dat", xDataList);
//				DataExtractor.writeData("output\\adaptiveData\\yData.dat", yDataList);
//				System.exit(0);
			}
//			if (iter > 0 && (iter % 100) == 0) {
////				double[][] xNormal = predictor.normal;
////				double[][] yNormal = DataExtractor.readNormalizeInfo(path + "yNormal.dat");
//				DataExtractor.writeData("output\\adaptiveData\\xData.dat", xDataList);
//				DataExtractor.writeData("output\\adaptiveData\\yData.dat", yDataList);
//			}
		}
	}
	
	private ArrayList<Motion> findNearestMotions(double[] pose, int neighborSize) {
		NearestBuffer<Motion> buffer = new NearestBuffer<Motion>(neighborSize);
		for (int i = 0; i < dataList.size(); i++) {
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
		r.goal.timeLimit -= 4;
		MatchingPath path = motionMatching.searchBest(startMotion.motionIndex, r.goal);
		MotionSegment segment = MotionSegment.getPathMotion(mList, path.getPath(), 0);
		int timeConstraint = Math.min(path.time, r.goal.timeLimit);
		MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, r.goal.getEditingConstraint(), timeConstraint);
		ArrayList<Integer> gIndices = new ArrayList<Integer>();
		int goalMotionIndex = MathUtil.round(edited.lastMotion().knot);
		gIndices.add(goalMotionIndex);
		AgilityGoal idleGoal = model.sampleIdleGoal();
		while (edited.lastMotion().knot <= TRAIN_STEP_SIZE + 2) {
			MatchingPath iPath = motionMatching.searchBest(edited.lastMotion().motionIndex, idleGoal);
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
	
	private ArrayList<AgilityTrainingData> collectAgilityData(int trySize, int maxSampleSize) {
		predictor.clearStateCache();
		ArrayList<AgilityTrainingData> dataList = new ArrayList<AgilityTrainingData>();
		AgilityGoal prevGoal = model.sampleIdleGoal();
		measure.checkAgility(prevGoal, prevGoal.maxSearchTime);
		//System.out.println("Start motion : " + measure.motionList.size());
		int checkCount = 0;
		for (int i = 0; i < trySize; i++) {
			checkCount++;
			AgilityGoal goal = model.sampleRandomGoal(predictor.currentPose());
			double[] startMotion = predictor.prevOutput;
			predictor.saveCurrentState();
			
			int time = measure.checkAgility(goal, goal.maxSearchTime);
//			System.out.println("suc/fail : " + i + " : " + (time>goal.timeLimit) + " : " + time + " : " + goal.timeLimit + " : " + measure.motionList.size());
			if (time > goal.timeLimit) {
				dataList.add(new AgilityTrainingData(i, goal, prevGoal, startMotion, time));
				if (dataList.size() >= maxSampleSize) {
					break;
				}
			}
			prevGoal = goal;
		}
		failRatio = dataList.size()/(double)checkCount;
//		System.out.println("collectAgilityData : " + dataList.size() + " / " + checkCount + " : " + failRatio);
//		for (AgilityTrainingData data : dataList) {
//			System.out.println(Arrays.toString(data.startMotion));
//			System.out.println(data.index  + " : " + data.goal + " : " + data.completionTime);
//		}
//		System.exit(0);
//		predictor.eval("print(len(state_list))");
//		predictor.eval("sys.stdout.flush()");
		
//		MainApplication.run(new Module() {
//			@Override
//			protected void initializeImpl() {
//				getModule(MainViewerModule.class);
//				getModule(ItemListModule.class).addSingleItem("motion", new MotionData(measure.motionList));
//				getModule(ItemListModule.class).addSingleItem("control", measure.goalList);
//			}
//		});
//		System.exit(0);
		
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
//		MathUtil.random = new Random(46436312);
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		RotationModel.GOAL_TIME_LIMIT = 25; 
//		TRAIN_STEP_SIZE = RotationModel.GOAL_TIME_LIMIT + 4;
		TRAIN_STEP_SIZE = 32;
		RotationMotionMatching.rotErrorRatio = 1000;
//		RotationModel.USE_STRAIGHT_SAMPLING = true;
		
		String folder = "dc_jog2";
		AgilityModel model = new RotationModel();
		MDatabase database = TrainingDataGenerator.loadDatabase(folder);
		
		String name = "dc_jog2_dir_r1000_30";
//		String name = "dc_jog2_dir_mm3_r50_25";
		AdaptiveTrainer trainer = new AdaptiveTrainer(database, model, name);
		trainer.train(5000);
	}
}
