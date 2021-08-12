package mrl.motion.neural.agility.measure;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.RotationModel.RotationGoal;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.measure.AgilityMeasure.AgilityMeasureResult;
import mrl.motion.neural.agility.measure.PlausibilityMeasure.PlausibilityMatch;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.match.OriginalMotionMatching;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.agility.PositionModel;
import mrl.motion.neural.agility.predict.RNNPythonPredictor;
import mrl.motion.neural.data.ActionOnlyWaveControl;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.viewer.module.LineGraphViewerModule;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Pair;
import mrl.util.Utils;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.ListViewerModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;
import mrl.widget.app.ListViewerModule.ListListener;

public class OMMAgilityMeasureModule extends Module{

	private AgilityMeasure measure;
	
	boolean measurePlausibility = false;
	boolean saveFile = true;
	
	@Override
	protected void initializeImpl() {
		AgilityModel model;
		
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		RotationModel.GOAL_TIME_LIMIT = 30; 
		RotationMotionMatching.rotErrorRatio = 100;
		
		AgilityModel.TIME_EXTENSION_MIN_TIME = 4;
		model = new RotationModel();
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT  = 300;
		
		getModule(MainViewerModule.class);
		MathUtil.random = new Random(-5768438514678765400l);
//		saveFile = false;
//		String label = "dc_jog2_dir_mm_r90_17";
//		String label = "dc_jog2_dir_mm_r90_22";
//		String label = "dc_jog2_dir_mm_r90_27";
		
		String folder = "omm_jog";
		String tPoseFile = "t_pose_ue.bvh";
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		Configuration.BLEND_MARGIN = 4;
		String label = "jog";
		TransitionData tData = new TransitionData(database, new String[] { label }, 1);
		
		Motion.printRootTrajectory = true;
//		{
//			int iii = database.findMotionDataIndex("PC_W_Curve_Largedegree_Jog-001_C.bvh");
//	//		int iii = database.findMotionDataIndex("s_003_1_1.bvh");
//			MotionData mData = database.getMotionDataList()[iii];
//			for (int i = 240; i < 250; i++) {
//				System.out.println(mData.motionList.get(i));
//			}
//			System.exit(0);
//	//		PC_W_Curve_Normaldegree_Jog-001_C:925
//		}
		
//		RotationModel.ROTAION_ANGLE_MARGIN = -Math.PI*0.9;
		OriginalMotionMatching.USE_BLENDING = true;
		OriginalMotionMatching.USE_OPTIMAL = true;
		OriginalMotionMatching predictor = new OriginalMotionMatching(tData);
		predictor.setStartMotion(database.getMotionList()[384]);
		measure = new AgilityMeasure(model, predictor);
		
		int[] weights = {
				5,
				10,
				50,
				100,
				500,
				1000,
				5000,
//				10000,
//				50000,
//				100000,
		};
//		weights = new int[] { 100 };
		saveFile = true;
		for (int weight : weights) {
			predictor.controlWeight = weight;
			WeightedKDE kde = new WeightedKDE();
			int sampleSize = 1000;
			ArrayList<AgilityMeasureResult> resultList = new ArrayList<AgilityMeasureResult>();
			ArrayList<Pair<AgilityGoal, Integer>> segments = new ArrayList<Pair<AgilityGoal,Integer>>();
			int failCount = 0;
			for (int iter = 0; iter < sampleSize; iter++) {
				System.out.println("iteration :: " + iter);
				AgilityGoal goal = model.sampleRandomGoal(measure.predictor.currentPose());
				AgilityMeasureResult result = measure.checkAgility2(goal, 150);
				resultList.add(result);
				int time = result.completionTime;
				if (time > goal.timeLimit) {
					failCount++;
				}
				kde.addSample(goal.getControlParameter(), time);
				segments.add(new Pair<AgilityGoal, Integer>(goal, measure.motionList.size()));
			}
			String outputFolder = "output\\ommAgility";
			if (saveFile) {
				new File(outputFolder).mkdirs();
				kde.save(outputFolder + "\\omm_nopti_" + label + "_w" + (int)predictor.controlWeight + ".txt");
			}
			
			MotionData mData = new MotionData(measure.motionList);
			getModule(ItemListModule.class).addSingleItem("motion", mData);
			getModule(ItemListModule.class).addSingleItem("goalList", measure.goalList);
			System.out.println("fail ratio :: " + weight + " : " + failCount + " / " + sampleSize + " : " +  (failCount/(double)sampleSize));
			
			
			ListViewerModule listMoudle = getModule(ListViewerModule.class);
			MainViewerModule mViewer = getModule(MainViewerModule.class);
			listMoudle.setItems(resultList, new ListListener<AgilityMeasureResult>(){
				@Override
				public String[] getColumnHeaders() {
					return new String[] { "rot" ,"tOver", "time limit", "c time", "fStart", "fEnd"};	
				}

				@Override
				public String[] getTableValues(AgilityMeasureResult item) {
					return Utils.toStringArrays(
							Math.toDegrees(((RotationGoal)item.goal).targetRotation),
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
	}
	
	private double getMeanPlausibility(PlausibilityMatch[] plausibility, int start, int end) {
		double sum = 0;
		for (int i = start; i < end; i++) {
			sum += plausibility[i].distance;
		}
		return sum/(end - start);
	}
	
	private ArrayList<Motion> collectMotion(Motion motion, int margin){
		LinkedList<Motion> list = new LinkedList<Motion>();
		list.add(new Motion(motion));
		Motion prev = motion;
		Motion next = motion;
		for (int i = 0; i < margin; i++) {
			if (prev.prev != null) prev = prev.prev;
			if (next.next != null) next = next.next;
			list.addFirst(new Motion(prev));
			list.addLast(new Motion(next));
		}
		
		return Utils.copy(list);
//		return MotionSegment.alignToBase(Utils.copy(list), margin);
	}

	public static void main(String[] args) {
		MainApplication.run(new OMMAgilityMeasureModule());
	}
}
