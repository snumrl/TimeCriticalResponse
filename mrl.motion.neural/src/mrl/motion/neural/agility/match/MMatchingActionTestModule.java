package mrl.motion.neural.agility.match;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.vecmath.Point3d;

import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.JumpModel.JumpGoal;
import mrl.motion.neural.agility.JumpModel;
import mrl.motion.neural.agility.MotionMatchingSampling3;
import mrl.motion.neural.agility.StuntModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.widget.ValueSlider;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;
import mrl.widget.app.SliderModule;

public class MMatchingActionTestModule extends Module {

	private MDatabase database;
	
	private LinkedList<Integer> actionList = new LinkedList<Integer>();
	private MotionMatchingSampling3 sampling;

	private ValueSlider slider;

	@Override
	protected void initializeImpl() {
		MDatabase.loadEventAnnotations = true;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		AgilityModel model;
		
		String folder= "dc_jump_jog";
		String tPoseFile = "t_pose_actor.bvh";
		AgilityModel.GOAL_TIME_LIMIT = 25; 
		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
		model = new JumpModel();
		
		
//		String folder = "stunt-retargeted";
//		String tPoseFile = "t_pose_sue.bvh";
//		AgilityModel.GOAL_TIME_LIMIT = 25; 
//		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
//		model = new StuntModel(folder);
//		
//		
		
		
		database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
//		MotionMatching matching = model.makeMotionMatching(database);
//		MotionMatching2 matching = new MotionMatching2(database);
		MainViewerModule viewer = getModule(MainViewerModule.class);
		viewer.getPickPoint();
//		Motion startMotion = database.findMotion("PC_W_RapidTurn_Jog-002_C.bvh", 1361);
//		Motion startMotion = database.findMotion("PC_W_RapidTurn_Jog-002_C.bvh", 433);
		
		
		sampling = new MotionMatchingSampling3(database, model);
		MMatching matching = sampling.matching;
		
		ItemListModule listModule = getModule(ItemListModule.class);
		listModule.addSingleItem("origin", Pose2d.BASE);
		viewer.getMainViewer().getCanvas().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			@Override
			public void keyPressed(KeyEvent e) {
				Point3d mouseP = viewer.getPickPoint();
				if (mouseP == null) return;
				if (e.keyCode >= '1' && e.keyCode <= '9') {
					int action = e.keyCode - '1';
					actionList.add(action);
					System.out.println("add action : " + action);
				}
				if (e.keyCode == 'q') {
					int action = 9;
					actionList.add(action);
					System.out.println("add action : " + action);
				}
				if (e.keyCode == 'w') {
					int action = 10;
					actionList.add(action);
					System.out.println("add action : " + action);
				}
				if (e.keyCode == 'r') {
					AgilityModel.GOAL_TIME_LIMIT = slider.getSelection();
					ArrayList<AgilityGoal> goalList = new ArrayList<AgilityGoal>();
					for (Integer action : actionList) {
						AgilityGoal goal = model.sampleRandomGoal(Pose2d.BASE);
						goal.actionType = action;
						goalList.add(goal);
						System.out.println("goal : " + goal);
					}
					System.out.println("--------------");
					actionList.clear();
					
//					sampling.sample(totalLength)
					
					int startMotionIndex = matching.pickRandomStartMotion();
					
					startMotionIndex = matching.database().findMotion("PC_W_RapidTurn_Jog-002_C.bvh", 211).motionIndex;
//					startMotionIndex = matching.database().findMotion("s_004_1_2.bvh", 175).motionIndex;
//					startMotionIndex = matching.database().findMotion("s_001_2_1.bvh", 164).motionIndex;
					System.out.println("Start :: " + database.getMotionList()[startMotionIndex]);
					
					ArrayList<Motion> motionList = sampling.samplePredefinedGoal(matching.database().getMotionList()[startMotionIndex], goalList);
					listModule.addSingleItem("motion", new MotionData(motionList), ItemDescription.red());
					viewer.replay();
					System.out.println("##### goal list");
					for (AgilityGoal goal : goalList) {
						int action = goal.actionType;
						System.out.println((action+1) + "\t" + StuntModel.actionTypes[action]);
					}
				}
			}
		});
		
		SliderModule sModule = getModule(SliderModule.class);
		sModule.setDefaultRange(10, 60);
		sModule.setSliderValues(new int[] { AgilityModel.GOAL_TIME_LIMIT });
		slider = sModule.getSliderList().get(0);
	}
	
	public static void main(String[] args) {
		MainApplication.run(new MMatchingActionTestModule());
	}
}
