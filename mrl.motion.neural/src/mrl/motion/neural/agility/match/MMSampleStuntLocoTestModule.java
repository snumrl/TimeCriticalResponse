package mrl.motion.neural.agility.match;

import java.util.ArrayList;

import javax.vecmath.Point3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.DribbleModel;
import mrl.motion.neural.agility.JumpModel;
import mrl.motion.neural.agility.JumpSpeedModel;
import mrl.motion.neural.agility.MotionMatchingSampling3;
import mrl.motion.neural.agility.PositionModel;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.StuntLocoModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.data.BallTrajectoryGenerator;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.ItemSerializer;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.util.Configuration;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class MMSampleStuntLocoTestModule extends Module {

	private MDatabase database;

	@Override
	protected void initializeImpl() {
		AgilityModel model;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		String tPoseFile;
		String folder;
		
		tPoseFile = "t_pose_sue.bvh";
		folder = "stunt_loco";
		
		AgilityModel.GOAL_TIME_LIMIT = 20; 
		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
		StuntLocoModel.LOCOMOTION_RATIO = 0.85;
		StuntLocoModel.USE_STRAIGHT_SAMPLING = true;
		TransitionData.STRAIGHT_MARGIN = 1;
		model = new StuntLocoModel();
		
//		AgilityModel.GOAL_TIME_LIMIT = 20; 
//		AgilityModel.TIME_EXTENSION_RATIO = 0.3;
//		TransitionData.STRAIGHT_MARGIN = -1;
//		model = new PositionModel();
//		
//		folder= "jump_speed_ue4";
//		tPoseFile = "t_pose_jue.bvh";
//		AgilityModel.GOAL_TIME_LIMIT = 20; 
//		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
//		JumpSpeedModel.LOCOMOTION_RATIO = 0.85;
//		JumpSpeedModel.USE_STRAIGHT_SAMPLING = true;
//		JumpSpeedModel.FIX_LOCOMOTION_AFTER_ACTION = false;
//		TransitionData.STRAIGHT_MARGIN = 1;
//		model = new JumpSpeedModel();
		
//		folder= "dc_jump_jog";
//		tPoseFile = "t_pose_actor.bvh";
//		AgilityModel.GOAL_TIME_LIMIT = 20; 
//		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
//		JumpModel.USE_STRAIGHT_SAMPLING = true;
//		JumpModel.FIX_LOCOMOTION_AFTER_ACTION = false;
//		TransitionData.STRAIGHT_MARGIN = 3;
//		model = new JumpModel();
		
		
		database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		MotionMatchingSampling3 sampling = new MotionMatchingSampling3(database, model);
		
		ArrayList<Motion> motionList = sampling.sample(1000);
//		System.out.println("################");
//		MotionTransform.print = true;
//		for (int i = 295; i < 299; i++) {
//			System.out.println(i + " : " + MotionTransform.alignAngle(motionList.get(i).root()));
//			System.out.println(RotationMotionMatching.getRotation(motionList.get(i), motionList.get(i+1)));
//		}
//		System.exit(0);
		
		MainViewerModule viewer = getModule(MainViewerModule.class);
		ItemListModule listModule = getModule(ItemListModule.class);
		TimeBasedList<Pose2d> cPoseList = new TimeBasedList<Pose2d>();
		for (Motion m : motionList) {
			cPoseList.add(PositionMotion.getPose(m));
		}
		listModule.addSingleItem("Motion", new MotionData(motionList), ItemDescription.red());
//		listModule.addSingleItem("notEdited", new MotionData(sampling.totalNotEdited));
		listModule.addSingleItem("goal", sampling.controlParameterObjects, ItemDescription.red());
		listModule.addSingleItem("cPoseList", cPoseList);
		
	}
	
	public static void main(String[] args) {
		MainApplication.run(new MMSampleStuntLocoTestModule());
	}
}
