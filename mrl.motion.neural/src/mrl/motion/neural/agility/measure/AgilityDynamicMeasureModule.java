package mrl.motion.neural.agility.measure;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MotionData;
import mrl.motion.neural.agility.AgilityControlParameterGenerator;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.RotationSpeedModel;
import mrl.motion.neural.agility.RotationSpeedModel.RotationSpeedGoal;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.agility.predict.RNNPythonPredictor;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Pair;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class AgilityDynamicMeasureModule extends Module{

	private AgilityMeasure measure;
	
	boolean saveFile = true;
	int maxSearchTime = 60;
	String label;
	
	String log = "";
	
	@Override
	protected void initializeImpl() {
		AgilityModel model;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		AgilityModel.TIME_EXTENSION_MIN_TIME = 6;
		
		String folder = "runjogwalk_withstop";
		label = "runjogwalk_withstop_dy_r1000_30";
		String tPoseFile = "t_pose_ue.bvh";
		
		RotationModel.GOAL_TIME_LIMIT = 30; 
		
		RotationMotionMatching.STRAIGHT_MARGIN = 1;
		AgilityModel.TIME_EXTENSION_RATIO = 0.1;
		RotationMotionMatching.rotErrorRatio = 100;
		model = new RotationSpeedModel();
		AgilityControlParameterGenerator.ADD_TIMING_PARAMETER  = true;
		
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		
		getModule(MainViewerModule.class);
		MathUtil.random = new Random(-5768438514678765400l);
//		saveFile = false;
		
		RNNPythonPredictor predictor = new RNNPythonPredictor(label);
		measure = new AgilityMeasure(model, predictor);
		
		saveFile = false;
		maxSearchTime = 60;
		checkAgility(model, 1, 25);
//		checkAgility(model, 0, 35);
		if (!saveFile) return;
		
		int[] actions = {
				0, 1, 2
		};
		int[] agilityList = {
			15, 20, 25, 30, 35, 40	
		};
		for (int action : actions) {
			for (int agility : agilityList) {
				checkAgility(model, action, agility);
			}
		}
		System.out.println("########## results");
		System.out.println(log);
	}
	
	private void checkAgility(AgilityModel model, int actionType, int agility) {
		RotationModel.GOAL_TIME_LIMIT = agility;
		WeightedKDE kde = new WeightedKDE();
		int sampleSize = 500;
		ArrayList<Pair<AgilityGoal, Integer>> segments = new ArrayList<Pair<AgilityGoal,Integer>>();
		int failCount = 0;
		String name = label + "_t" + actionType + "_ag" + RotationModel.GOAL_TIME_LIMIT;
		for (int iter = 0; iter < sampleSize; iter++) {
			System.out.println("iteration :: " + iter);
			AgilityGoal goal = model.sampleRandomGoal(measure.predictor.currentPose());
			
			RotationSpeedGoal g = (RotationSpeedGoal)goal;
			g.actionType = actionType;
			g.targetRotation = RotationModel.sampleRotation();
			g.setTime(RotationModel.GOAL_TIME_LIMIT);
			
			int time = measure.checkAgility(goal, maxSearchTime);
			if (time > goal.timeLimit) {
				failCount++;
			}
			kde.addSample(new double[] { g.targetRotation }, time);
			segments.add(new Pair<AgilityGoal, Integer>(goal, measure.motionList.size()));
			if (time == maxSearchTime) {
				System.out.println("max time--");
				break;
			}
		}
		String outputFolder = "output\\rotDynamicAglity";
		if (saveFile) {
			new File(outputFolder).mkdirs();
			kde.save(outputFolder + "\\" + name + ".txt");
		}
		
		MotionData mData = new MotionData(measure.motionList);
		getModule(ItemListModule.class).addSingleItem("motion", mData);
		getModule(ItemListModule.class).addSingleItem("goalList", measure.goalList);
		log += "fail ratio :: " + name + " : " + failCount + " / " + sampleSize + " : " +  (failCount/(double)sampleSize) + "\r\n";
	}
	
	public static void main(String[] args) {
		MainApplication.run(new AgilityDynamicMeasureModule());
	}
}
