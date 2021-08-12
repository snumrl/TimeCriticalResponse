package mrl.motion.neural.agility.adaptive;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.MotionMatchingSampling2;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.agility.measure.AgilityMeasure;
import mrl.motion.neural.agility.measure.WeightedKDE;
import mrl.motion.neural.agility.measure.WeightedKDE.WeightedKDESample;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.util.Configuration;
import mrl.util.Logger;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class AdaptiveTrainer4 {

	static int TRAIN_STEP_SIZE = 48;
	static int TRAIN_BATCH_SIZE = 60;

	public static double MAX_PDF = 0.3;
	public static double W_WEIGHT = 0.1;
			
	
	private AgilityModel model;
	private AgilityMeasure measure;
	private CombinedPythonModel2 predictor;
	private MotionMatchingSampling2 sampling;
	
	private AgilityGoal lastGoal;
	private String name;
	
	private int searchTimeOffset = 10;
	
	private static double ADAPTIVE_ROT_ANGLE_MARGIN = 0;
	
	ArrayList<WeightedKDE> cTimeOffsetList = new ArrayList<WeightedKDE>();
	
	public AdaptiveTrainer4(MDatabase database, AgilityModel model, String name) {
		this.name = name;
		this.model = model;
		predictor = new CombinedPythonModel2(name, TRAIN_STEP_SIZE, TRAIN_BATCH_SIZE, true);
		measure = new AgilityMeasure(model, predictor);
		sampling = new MotionMatchingSampling2(database, model);
	}
	
	ArrayList<double[]> xDataList = new ArrayList<double[]>();
	ArrayList<double[]> yDataList = new ArrayList<double[]>();
	
	public void train(int iteration) {
		Logger.startLogging();
		for (int iter = 0; iter < iteration; iter++) {
			ArrayList<AgilityTrainingData> resultList = collectAgilityData(1000);
			WeightedKDE.USE_POS_PROBABILITY = false;
//			WeightedKDE.NO_USE_NORMALIZE = false;
			WeightedKDE kde = new WeightedKDE();
			RotationModel.ROTAION_ANGLE_MARGIN = 0;
			for (AgilityTrainingData d : resultList) {
				kde.addSample(d.goal.getControlParameter(), d.completionTime);
			}
			kde.updateSigma();
			kde.save("output\\kde_log2\\" + name + "\\iter_" + iter + ".txt");
			
			ArrayList<WeightedKDESample> remain = new ArrayList<WeightedKDESample>();
			for (WeightedKDESample sample : kde.getSampleList()) {
				if (sample.weight > AgilityModel.GOAL_TIME_LIMIT) {
					remain.add(sample);
				}
			}
			Logger.line("fail ratio : " + iter + " : " + + remain.size() + " / " +  resultList.size());
			if (remain.size() == 0) {
				break;
			}
//			System.out.println("fail ratio :: " + );
			kde.sigmaSizeOffset = kde.getSampleList().size() - remain.size();
			kde.getSampleList().clear();
			kde.getSampleList().addAll(remain);
			WeightedKDE.USE_POS_PROBABILITY = true;
//			WeightedKDE.NO_USE_NORMALIZE = true;
			kde.updateDataByType();
			cTimeOffsetList.add(kde);
			
			int gCount = 0;
			double tIntSum = 0;
			double maxTInt = 0;
			int goalSampleCount = 0;
			RotationModel.ROTAION_ANGLE_MARGIN = ADAPTIVE_ROT_ANGLE_MARGIN;
			while (true) {
				AgilityGoal goal = model.sampleRandomGoal(Pose2d.getPose(sampling.currentMotion()));
				double[] pos = goal.getControlParameter();
				double tOffset = getTimingOffset(pos);
				double tInt = Math.floor(tOffset);
				double tRemain = tOffset - tInt;
				if (MathUtil.random.nextDouble() < tRemain) {
					tInt += 1;
				}
				
				tIntSum += tInt;
				maxTInt = Math.max(tInt, maxTInt);
				goalSampleCount++;
				
				goal.timeLimit -= (int)tInt;
				System.out.println("ggtt : " + Utils.toString(goal.timeLimit, tInt, tOffset, tRemain));
				
				boolean add = sampling.sampleSingle(goal);
				if (add) {
					gCount += MotionMatchingSampling2.UPDATE_DATA_MARGIN;
					predictor.appendOriginalData(Utils.toArray(sampling.generatedXList), Utils.toArray(sampling.generatedYList));
					if (gCount > 5000) break;
				}
			}
			Logger.line("tint stat : " + iter + " : " + maxTInt + " : " + (tIntSum/goalSampleCount));
			
			for (int i = 0; i <= 100; i++) {
				System.out.println("train iter : " + iter + " : " + i);
				predictor.trainByOriginalData();
				if (i > 0 && (i % 50) == 0) {
					predictor.saveNetwork();
				}
			}
		}
	}
	
	private double getTimingOffset(double[] pos) {
		double offset = 0;
		for (WeightedKDE kde : cTimeOffsetList) {
			double p = adjustPDF(kde.getEstimatedWeight(pos));
			offset += p;
		}
		return offset;
//		return offset*0.1;
	}
	
	public static double adjustPDF(double p) {
		return Math.min(MAX_PDF, p)*W_WEIGHT;
	}
	
	private ArrayList<AgilityTrainingData> collectAgilityData(int trySize) {
		predictor.clearStateCache();
		ArrayList<AgilityTrainingData> dataList = new ArrayList<AgilityTrainingData>();
		AgilityGoal prevGoal = model.sampleIdleGoal();
		measure.checkAgility(prevGoal, prevGoal.maxSearchTime);
		//System.out.println("Start motion : " + measure.motionList.size());
		int checkCount = 0;
		for (int i = 0; i < trySize; i++) {
			checkCount++;
			AgilityGoal goal;
			goal = model.sampleRandomGoal(predictor.currentPose());
			double[] startMotion = predictor.prevOutput;
			predictor.saveCurrentState();
			
			int time = measure.checkAgility(goal, goal.maxSearchTime + searchTimeOffset);
			dataList.add(new AgilityTrainingData(i, goal, prevGoal, startMotion, time));
			prevGoal = goal;
		}
//		failRatio = dataList.size()/(double)checkCount;
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
		String folder = "dc_jog_ue";
		String tPoseFile = "t_pose_ue.bvh";
		RotationModel.GOAL_TIME_LIMIT = 15; 
		RotationModel.USE_STRAIGHT_SAMPLING = false;
		AgilityModel.TIME_EXTENSION_RATIO = 0.2;
		TransitionData.STRAIGHT_MARGIN = 3;
		int rMargin = 10;
		ADAPTIVE_ROT_ANGLE_MARGIN = Math.toRadians(rMargin);
		RotationModel.ROTATION_ERROR_WEIGHT = 200;
		AgilityModel model = new RotationModel();
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		
		String name = "dc_jog_ue_rot_test_rMargin10_15_2_adap";
//		String name = "dc_jog2_dir_mm3_r50_25";
		
		TRAIN_BATCH_SIZE = 120;
		
		AdaptiveTrainer4 trainer = new AdaptiveTrainer4(database, model, name);
		trainer.train(10000);
	}
}
