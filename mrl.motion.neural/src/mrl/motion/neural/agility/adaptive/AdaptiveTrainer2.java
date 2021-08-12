package mrl.motion.neural.agility.adaptive;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.MotionMatchingSampling2;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.agility.measure.AgilityMeasure;
import mrl.motion.neural.agility.measure.WeightedKDE;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class AdaptiveTrainer2 {

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
	
	public AdaptiveTrainer2(MDatabase database, AgilityModel model, String name, boolean load) {
		this.name = name;
		this.model = model;
		this.load = load;
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
				if (gCount > 10000) break;
			}
		}
		
		if (!load) {
			for (int iter = 0; iter < 200; iter++) {
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
					AgilityGoal goal =  model.sampleRandomGoal(predictor.currentPose());
//					AgilityGoal goal = sampleGoalByMCMC(Pose2d.getPose(sampling.currentMotion()));
					if (sampling.sampleSingle(goal)) {
						predictor.appendOriginalData(Utils.toArray(sampling.generatedXList), Utils.toArray(sampling.generatedYList));
					}
				}
			}
			
		}
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
		AgilityModel.TIME_EXTENSION_RATIO = 0.2;
//		MathUtil.random = new Random(46436312);
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		RotationModel.GOAL_TIME_LIMIT = 10; 
//		TRAIN_STEP_SIZE = RotationModel.GOAL_TIME_LIMIT + 4;
		RotationModel.USE_STRAIGHT_SAMPLING = false;
		RotationMotionMatching.rotErrorRatio = 100;
//		RotationModel.USE_STRAIGHT_SAMPLING = true;
		
		TRAIN_BATCH_SIZE = 120;
		
		String folder = "dc_jog2";
		AgilityModel model = new RotationModel();
		MDatabase database = TrainingDataGenerator.loadDatabase(folder);
		
		String name = "dc_jog2_rot_adap_te02_t10c";
//		String name = "dc_jog2_dir_mm3_r50_25";
		AdaptiveTrainer2 trainer = new AdaptiveTrainer2(database, model, name, false);
		trainer.kde.maxSampleSize = 3000;
		trainer.kde.sigmaUpdateInterval = 50;
		trainer.train(10000);
	}
}
