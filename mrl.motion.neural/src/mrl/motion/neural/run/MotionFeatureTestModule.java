package mrl.motion.neural.run;

import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.DribbleModel;
import mrl.motion.neural.agility.JumpModel;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.neural.agility.measure.VelocityAutoLabeling;
import mrl.motion.neural.data.BallTrajectoryGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.util.Configuration;
import mrl.util.Utils;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class MotionFeatureTestModule extends Module{
	
	private MDatabase database;
	
	public MotionFeatureTestModule(MDatabase database) {
		this.database = database;
	}


	@Override
	protected void initializeImpl() {
		getModule(MainViewerModule.class);
		
		int iii = database.findMotionDataIndex("s_003_3_1.bvh");
//		int iii = database.findMotionDataIndex("s_003_1_1.bvh");
		MotionData mData = database.getMotionDataList()[iii];
		BallTrajectoryGenerator.updateBallContacts(database);
		{
//			for (Motion m : mData.motionList) {
////			Motion m = mData.motionList.get(0);
//				Matrix4d rotZ = new Matrix4d();
//				rotZ.rotZ(Math.toRadians(40));
//				m.get("LeftHand").mul(rotZ);
//				
//				Matrix4d rotX = new Matrix4d();
//				rotX.rotX(Math.toRadians(180));
//				m.get("RightHand").mul(rotZ);
//				m.get("RightHand").mul(rotX);
//			}
			
			BallTrajectoryGenerator bg = new BallTrajectoryGenerator();
			TimeBasedList<Point3d> ball = new TimeBasedList<Point3d>();
			ball.addAll(bg.generate(mData.motionList));
			
			
			getModule(ItemListModule.class).addSingleItem("st", mData, new ItemDescription(new Vector3d(1, 0, 0)));
			getModule(ItemListModule.class).addSingleItem("base", Pose2d.BASE, new ItemDescription(new Vector3d(0, 1, 0)));
			getModule(ItemListModule.class).addSingleItem("ball", ball, BallTrajectoryGenerator.ballDescription());
			if (iii >= 0) return;
		}
	
		
		
		ArrayList<Motion> straightList = new ArrayList<Motion>();
		ArrayList<Motion> nonStraightList = new ArrayList<Motion>();
		VelocityAutoLabeling label = new VelocityAutoLabeling(database);
		VelocityAutoLabeling.VELOCITY_MAINTAIN_MARGIN = 3;
		label.velocityLimit = 10;
		label.calc();
		
		
		TransitionData.STRAIGHT_MARGIN = 1;
		TransitionData.STRAIGHT_LIMIT_PRESET = new double[] {
				10000, 20, 30
		};
		
		TransitionData tData = new TransitionData(database, DribbleModel.actionTypes, DribbleModel.actionTypes.length);
		TimeBasedList<Pose2d> velMaintain = new TimeBasedList<Pose2d>();
		TimeBasedList<Pose2d> velNotMaintain = new TimeBasedList<Pose2d>();
		for (int i = 0; i < mData.motionList.size(); i++) {
			Motion m = mData.motionList.get(i);
			if (tData.isStraightMotion[m.motionIndex]) {
				straightList.add(mData.motionList.get(i));
				nonStraightList.add(null);
			} else {
				straightList.add(null);
				nonStraightList.add(mData.motionList.get(i));
			}
		}
		
		
//		TimeBasedList<Pose2d> velMaintain = new TimeBasedList<Pose2d>();
//		TimeBasedList<Pose2d> velNotMaintain = new TimeBasedList<Pose2d>();
//		for (int i = 0; i < mData.motionList.size(); i++) {
//			Motion m = mData.motionList.get(i);
//			if (label.isRotationMaintain[m.motionIndex]) {
//				straightList.add(mData.motionList.get(i));
//				nonStraightList.add(null);
//			} else {
//				straightList.add(null);
//				nonStraightList.add(mData.motionList.get(i));
//			}
//			Pose2d p = Pose2d.getPose(m);
//			if (label.isVelocityMaintain[m.motionIndex]) {
//				velMaintain.add(p);
//				velNotMaintain.add(null);
//			} else {
//				velMaintain.add(null);
//				velNotMaintain.add(p);
//			}
//		}
		
		
		
		getModule(ItemListModule.class).addSingleItem("st", new MotionData(straightList), new ItemDescription(new Vector3d(1, 0, 0)));
		getModule(ItemListModule.class).addSingleItem("nst", new MotionData(nonStraightList));
		getModule(ItemListModule.class).addSingleItem("vm", velMaintain, new ItemDescription(new Vector3d(1, 0, 0)));
		getModule(ItemListModule.class).addSingleItem("vnm", velNotMaintain);
		getModule(ItemListModule.class).addSingleItem("base", Pose2d.BASE, new ItemDescription(new Vector3d(0, 1, 0)));
	}

	
	public static void run(MDatabase database) {
		MainApplication.run(new MotionFeatureTestModule(database));
		System.exit(0);
	}
	
	public static void main(String[] args) {
		String folder = "";
		String tPoseFile = "";
		tPoseFile = "t_pose_actor.bvh";
		folder = "basketTest";
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, "t_pose_actor.bvh");
		run(database);
	}
}
