package mrl.motion.neural.agility.measure;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MotionData;
import mrl.motion.neural.agility.AgilityControlParameterGenerator;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.DynamicAgilityModel;
import mrl.motion.neural.agility.StuntLocoModel;
import mrl.motion.neural.agility.StuntLocoModel.StuntLocoGoal;
import mrl.motion.neural.agility.StuntLocoModel2;
import mrl.motion.neural.agility.StuntLocoModel2.StuntLocoGoal2;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.measure.AgilityMeasure.AgilityMeasureResult;
import mrl.motion.neural.agility.predict.RNNPythonPredictor;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.ListViewerModule;
import mrl.widget.app.ListViewerModule.ListListener;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class ActionAgilityMeasureModule2 extends Module{

	private AgilityMeasure measure;
	
	boolean measurePlausibility = false;
	boolean saveFile = true;
	
	@Override
	protected void initializeImpl() {
		StuntLocoModel2 model;
		int checkTimeOffset = 20;
		
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		AgilityControlParameterGenerator.ADD_TIMING_PARAMETER = true;
		AgilityModel.TIME_EXTENSION_MIN_TIME = 6;
		MotionDataConverter.useMatrixForAll = true;
		String folder = "stunt_loco";
		String tPoseFile = "t_pose_sue.bvh";
		model = new StuntLocoModel2();
//		model = new StuntLocoModel();
//		model = new StuntModel(folder);
		String label = "stunt_loco_ue_CTime_ef05";
//		String label = "stunt_loco_ue_dyCTime_400000";
//		String label = "stunt_loco_ue_dy_ag_error";
		
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT  = 300;
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		
		getModule(MainViewerModule.class);
		MathUtil.random = new Random(-5768438514678765400l);
//		saveFile = false;
		
		RNNPythonPredictor predictor = new RNNPythonPredictor(label);
		
		int activeAction = 6;
		
		
		int sampleSize = 1000;
		int failCount = 0;
		int checkCount = 0;
		
//		int agility = 50;
		ArrayList<AgilityMeasureResult> resultList = null;
		for (int agility = 15; agility <= 55; agility+=10) {
			agility = 45;
			
			AgilityModel.GOAL_TIME_LIMIT = agility; 
			measure = new AgilityMeasure(model, predictor);
			HistogramData h = new HistogramData();
			AgilityGoal prevGoal = model.sampleIdleGoal();
			resultList = new ArrayList<AgilityMeasureResult>();
			for (int iter = 0; iter < sampleSize; iter++) {
				System.out.println("iteration :: " + iter);
				int action;
				if ((iter % 3) == 0){
					action = activeAction;
				} else {
					action = 3;
				}
				StuntLocoGoal2 goal = model.sampleGoal(action);
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
			
			if (prevGoal != null) break;
			
			String outputFolder = "output\\stuntLocoAgility2";
			if (saveFile) {
				new File(outputFolder).mkdirs();
				h.save(outputFolder + "\\" + label + "_t" + activeAction + "_a" + agility + ".dat");
			}
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
		MainApplication.run(new ActionAgilityMeasureModule2());
	}
}
