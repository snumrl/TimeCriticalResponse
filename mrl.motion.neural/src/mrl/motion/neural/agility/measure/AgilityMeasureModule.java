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
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.measure.PlausibilityMeasure.PlausibilityMatch;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
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

public class AgilityMeasureModule extends Module{

	private AgilityMeasure measure;
	
	boolean measurePlausibility = false;
	boolean saveFile = true;
	
	@Override
	protected void initializeImpl() {
		AgilityModel model;
		
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		RotationModel.GOAL_TIME_LIMIT = 15; 
		RotationMotionMatching.rotErrorRatio = 100;
		
//		String folder = "dc_jog2";
		String folder = "dc_jog_ue";
		String tPoseFile = "t_pose_ue.bvh";
		AgilityModel.TIME_EXTENSION_MIN_TIME = 4;
		model = new RotationModel();
//		Normalizer.NEURALDATA_PREFIX = "adaptiveTraining";
		
//		String label = "dc_jog_ue_rot_test_rMargin10_15_adap";
		String label = "dc_jog_ue_rot_test_rMargin10_15";
//		String label = "dc_jog2_rot_margin_10";
//		String label = "dc_jog2_rot_base_30";
//		String label = "dc_jog2_rot_margin_10";
//		String label = "dc_jog2_rot_adap_te02_t10c";
//		String label = "dc_jog2_rot_base_10";
//		String label = "dc_jog2_rot_adap_te02_t10";
//		String label = "dc_jog2_dir_mm3_r50_25";
//		String label = "dc_jog2_rot_new_adaptive";
//		String label = "dc_jog2_dir_r1000_30";
//		String label = "dc_jog2_dir_mm4_r10000_25";
//		String label = "dc_jog2_dir_mm3_r50_25_ad";
//		String label = "dc_jog2_dir_mm3_r50_25";
//		String label = "dc_jog2_dir_mm2_r50_25_ad";
//		String label = "dc_jog2_dir_mm2_r50_25";
		
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT  = 300;
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		PlausibilityMeasure pMeasure = new PlausibilityMeasure(database);
		
		getModule(MainViewerModule.class);
		MathUtil.random = new Random(-5768438514678765400l);
//		saveFile = false;
//		String label = "dc_jog2_dir_mm_r90_17";
//		String label = "dc_jog2_dir_mm_r90_22";
//		String label = "dc_jog2_dir_mm_r90_27";
		
		
		RNNPythonPredictor predictor = new RNNPythonPredictor(label);
		measure = new AgilityMeasure(model, predictor);
		
		WeightedKDE kde = new WeightedKDE();
		int sampleSize = 1000;
		ArrayList<Pair<AgilityGoal, Integer>> segments = new ArrayList<Pair<AgilityGoal,Integer>>();
		int failCount = 0;
		for (int iter = 0; iter < sampleSize; iter++) {
			System.out.println("iteration :: " + iter);
			AgilityGoal goal = model.sampleRandomGoal(measure.predictor.currentPose());
			int time = measure.checkAgility(goal, 60);
			if (time < 0) time = 70;
			if (time > goal.timeLimit) {
				failCount++;
			}
			kde.addSample(goal.getControlParameter(), time);
			segments.add(new Pair<AgilityGoal, Integer>(goal, measure.motionList.size()));
		}
		String outputFolder = "output\\rotationAgility";
		if (saveFile) {
			new File(outputFolder).mkdirs();
			kde.save(outputFolder + "\\" + label + ".txt");
		}
		
		MotionData mData = new MotionData(measure.motionList);
		getModule(ItemListModule.class).addSingleItem("motion", mData);
		getModule(ItemListModule.class).addSingleItem("goalList", measure.goalList);
		System.out.println("fail ratio :: " + failCount + " / " + sampleSize + " : " +  (failCount/(double)sampleSize));
		
		if (measurePlausibility) {
			PlausibilityMatch[] plausibility = pMeasure.measure(measure.motionList);
			double[][] pData = new double[plausibility.length][1];
			for (int i = 0; i < pData.length; i++) {
				System.out.println("plausibility checking :: " + i + " / " + pData.length);
				pData[i][0] = plausibility[i].distance;
			}
			getModule(LineGraphViewerModule.class).setDataList(pData, new String[] { "plausibility" });
			
			WeightedKDE plausKDE = new WeightedKDE();
			for (int i = 0; i < segments.size(); i++) {
				Pair<AgilityGoal, Integer> pair = segments.get(i);
				int start = 0;
				if (i > 0) start = segments.get(i-1).second + 1;
				plausKDE.addSample(pair.first.getControlParameter(), getMeanPlausibility(plausibility, start, pair.second));
			}
			if (saveFile) {
				plausKDE.save(outputFolder + "\\" + label + "_plausibility.txt");
			}
		
		
		
			ListViewerModule listMoudle = getModule(ListViewerModule.class);
			listMoudle.setItems(plausibility, new ListListener<PlausibilityMatch>(){
				@Override
				public String[] getColumnHeaders() {
					return new String[] { "error", "frame", "matchMotion"};	
				}
	
				@Override
				public String[] getTableValues(PlausibilityMatch item) {
					return Utils.toStringArrays(item.distance, item.generated.frameIndex, item.origin);
				}
				@Override
				public void onItemSelection(PlausibilityMatch item) {
					System.out.println("selected :: " + item.alignTransform);
					int margin = 4;
					ArrayList<Motion> generated = collectMotion(item.generated, margin);
					ArrayList<Motion> origin = collectMotion(item.origin, margin);
	//				generated = MotionSegment.align(generated, item.alignTransform);
					origin = MotionSegment.align(origin, item.alignTransform);
					
					
					getModule(ItemListModule.class).removeItemByName("goalList");
					getModule(ItemListModule.class).addSingleItem("motion", new MotionData(generated), new ItemDescription(new Vector3d(1, 0, 0)));
					getModule(ItemListModule.class).addSingleItem("origin", new MotionData(origin));
					
	//				ArrayList<Point3d> omp = item.oMp.points;
	//				ArrayList<Point3d> transformed = new ArrayList<Point3d>();
	//				for (Point3d p : omp) {
	//					p = new Point3d(p);
	//					item.alignTransform.transform(p);
	//					transformed.add(p);
	//				}
	//				getModule(ItemListModule.class).addSingleItem("gmp", item.gMp.points, new ItemDescription(new Vector3d(1, 0, 0)));
	//				getModule(ItemListModule.class).addSingleItem("oMp", transformed);
					
					getModule(MainViewerModule.class).replay();
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
		MainApplication.run(new AgilityMeasureModule());
	}
}
