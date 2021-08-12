package mrl.motion.neural.agility.measure;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MotionData;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.DynamicAgilityModel;
import mrl.motion.neural.agility.StuntLocoModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.measure.AgilityMeasure.AgilityMeasureResult;
import mrl.motion.neural.agility.predict.RNNPythonPredictor;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.ListViewerModule;
import mrl.widget.app.ListViewerModule.ListListener;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class ActionAgilityMeasureModule extends Module{

	private AgilityMeasure measure;
	
	boolean measurePlausibility = false;
	boolean saveFile = true;
	
	@Override
	protected void initializeImpl() {
		AgilityModel model;
		int checkTimeOffset = 20;
		
		int agility = 20;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		AgilityModel.GOAL_TIME_LIMIT = agility; 
		AgilityModel.TIME_EXTENSION_MIN_TIME = 6;
		
		String folder = "stunt_loco";
		String tPoseFile = "t_pose_sue.bvh";
		model = new StuntLocoModel();
//		model = new StuntModel(folder);
		model = new DynamicAgilityModel(model, agility, agility);
		String label = "stunt_loco_ue_dy_ag_error";
		
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT  = 300;
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		PlausibilityMeasure pMeasure = new PlausibilityMeasure(database);
		
		getModule(MainViewerModule.class);
		MathUtil.random = new Random(-5768438514678765400l);
//		saveFile = false;
		
		
		RNNPythonPredictor predictor = new RNNPythonPredictor(label);
		measure = new AgilityMeasure(model, predictor);
		
		HistogramData h = new HistogramData();
		int sampleSize = 1000;
		int failCount = 0;
		int checkCount = 0;
		AgilityGoal prevGoal = model.sampleIdleGoal();
		ArrayList<AgilityMeasureResult> resultList = new ArrayList<AgilityMeasureResult>();
		for (int iter = 0; iter < sampleSize; iter++) {
			System.out.println("iteration :: " + iter);
			AgilityGoal goal = model.sampleRandomGoal(measure.predictor.currentPose());
			AgilityMeasureResult result = measure.checkAgility2(goal, goal.maxSearchTime + checkTimeOffset);
			resultList.add(result);
			result.prevGoal = prevGoal;
			int time = result.completionTime;
			if (goal.isActiveAction()) {
				checkCount++;
				if (time > goal.timeLimit) {
					failCount++;
				}
			}
			h.addValue((double)time, prevGoal.actionType, goal.actionType);
			h.setLimitValue((double)goal.timeLimit, prevGoal.actionType, goal.actionType);
			prevGoal = goal;
		}
		String outputFolder = "output\\stuntLocoAgility";
		if (saveFile) {
			new File(outputFolder).mkdirs();
			h.save(outputFolder + "\\" + label + "_a" + agility + ".dat");
		}
		
		MotionData mData = new MotionData(measure.motionList);
		getModule(ItemListModule.class).addSingleItem("motion", mData);
		getModule(ItemListModule.class).addSingleItem("goalList", measure.goalList);
		System.out.println("fail ratio :: " + failCount + " / " + checkCount + " : " +  (failCount/(double)checkCount));
		
		String[] actions = model.getActionTypes();
		ListViewerModule listMoudle = getModule(ListViewerModule.class);
		MainViewerModule mViewer = getModule(MainViewerModule.class);
		listMoudle.setItems(resultList, new ListListener<AgilityMeasureResult>(){
			@Override
			public String[] getColumnHeaders() {
				return new String[] { "prev action", "action", "tOver", "time limit", "c time", "fStart", "fEnd"};	
			}

			@Override
			public String[] getTableValues(AgilityMeasureResult item) {
				return Utils.toStringArrays(
						actions[item.prevGoal.actionType], actions[item.goal.actionType], 
						item.completionTime - item.goal.timeLimit,
						item.goal.timeLimit, item.completionTime,
						item.frameStart, item.frameEnd
						);
			}
			@Override
			public void onItemSelection(AgilityMeasureResult item) {
				Vector3d t = MathUtil.getTranslation(mData.motionList.get(item.frameStart).root());
				mViewer.setCameraCenter(t);
				mViewer.setSelectionBound(item.frameStart - 1, item.frameEnd);
				mViewer.setTimeIndex(item.frameStart - 1);
				mViewer.play();
			}
		});
	}
	
	public static void main(String[] args) {
		MainApplication.run(new ActionAgilityMeasureModule());
	}
}
