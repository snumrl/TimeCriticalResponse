package mrl.motion.neural.run;

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionBlender;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.JumpModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.Utils;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class MotionTestModule extends Module{
	
	public static int[][] getPath(MDatabase database, String[] path) {
		ArrayList<int[]> pathList = new ArrayList<int[]>();
		for (int i = 0; i < path.length; i+=2) {
			pathList.add(new int[] {
				findMotion(database, path[i]).motionIndex,
				findMotion(database, path[i+1]).motionIndex,
			});
		}
		return Utils.toArray(pathList);
	}
//	public static MotionSegment getMotion(MDatabase database, String[] path) {
//		ArrayList<int[]> pathList = new ArrayList<int[]>();
//		for (int i = 0; i < path.length; i+=2) {
//			pathList.add(new int[] {
//				findMotion(database, path[i]).motionIndex,
//				findMotion(database, path[i+1]).motionIndex,
//			});
//		}
//		return MotionSegment.getPathMotion(database.getMotionList(), Utils.toArray(pathList));
//	}
	
	private static Motion findMotion(MDatabase database, String mName) {
		int idx = mName.indexOf(":");
		String file = mName.substring(0, idx) + ".bvh";
		int frame = Integer.parseInt(mName.substring(idx+1, mName.length()));
		return database.findMotion(file, frame);
	}

	@Override
	protected void initializeImpl() {
		MDatabase.loadEventAnnotations = true;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		
		JumpModel.rotErrorRatio = 100;
		RotationMotionMatching.STRAIGHT_MARGIN = 3;
//		String folder = "poke_bvh_ver2";
		String folder = "stunt_loco";
//		String folder = "runjogwalk";
		
//		String folder= "dc_jump_run";
//		RotationMotionMatching.STRAIGHT_MARGIN = 1;
		
		AgilityModel.GOAL_TIME_LIMIT = 30;
		JumpModel.USE_STRAIGHT_SAMPLING = true;
		AgilityModel.TIME_EXTENSION_RATIO = 0.1;
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, "t_pose_ue2.bvh");
		String[] path = {
				"s_005_1_1:262", "s_005_1_1:282",
				"s_020_2_2:438", "s_020_2_2:458",
				
//				"PC_W_Clockwise_BigCircle-002_C:244", "PC_W_Clockwise_BigCircle-002_C:250",
//				"PC_W_Straight_Acceleration_Deceleration-001_C:1925", "PC_W_Straight_Acceleration_Deceleration-001_C:1929",
//				"PC_W_Straight_Acceleration_Deceleration-001_C:815", "PC_W_Straight_Acceleration_Deceleration-001_C:819",
//				"PC_W_Straight_Acceleration_Deceleration-001_C:1487", "PC_W_Straight_Acceleration_Deceleration-001_C:1491"
		};
//		{
//			MotionData mData = database.getMotionDataList()[0];
//			Motion m = mData.motionList.get(200);
//			Pose2d p1 = Pose2d.getPoseByPelvis(m);
//			Pose2d p2 = MotionTransform.getPose(m);
//			System.out.println(p1 + " , " + p2);
//			System.exit(0);
//		}
//		
//		ArrayList<Motion> mList = mData.motionList;
//		for (int i = 0; i < mList.size()-1; i++) {
//			Motion m = mList.get(i);
//			Motion next = m.next;
//			if (next == null) continue;
//			Pose2d p = PositionMotion.getPose(mList.get(i));
//			double a = MotionTransform.alignAngle(mList.get(i).root());
//			double a2 = MathUtil.directionalAngle(p.direction, Pose2d.BASE.direction);
////			a2 += Math.PI;
//			if (a2 > Math.PI) {
//				a2 -= Math.PI*2;
//			}
//			System.out.println(i + " : " + a + " : " + a2 + " : "+ p);
////				Pose2d pose = PositionMotion.getPose(mList.get(i));
////				Vector2d v = new Vector2d(Pose2d.BASE.direction);
////				pose.direction = MathUtil.rotate(v, a);
////				pList.add(pose);
//		}
////		System.exit(0);
//			
//		TimeBasedList<Pose2d> pp = new TimeBasedList<Pose2d>();
//		for (Motion m : mData.motionList) {
//			pp.add(Pose2d.getPose(m));
//		}
		
		int[][] pathList = getPath(database, path);
		MotionSegment segment = MotionSegment.getPathMotion(database.getMotionList(), pathList);
		MotionBlender blendeder = MotionBlender.getPathMotion(database, pathList);
		MotionData mData = new MotionData(segment.getMotionList());
//		Motion am = PositionMotion.getAlignedMotion(mData.motionList.get(0));
		
		MainViewerModule viewer = getModule(MainViewerModule.class);
		ItemListModule listModule = getModule(ItemListModule.class);
//		listModule.addSingleItem("motion", mData, ItemDescription.red());
//		listModule.addSingleItem("am", new MotionData(segment.getNotBlendedMotionList()));
		listModule.addSingleItem("blender", new MotionData(blendeder.motionList), new ItemDescription(new Vector3d(0, 1, 0)));
		listModule.addSingleItem("blender not blended", new MotionData(blendeder.notBlendedList));
		listModule.addSingleItem("base", Pose2d.BASE, ItemDescription.red());
//		listModule.addSingleItem("ppp", pp, ItemDescription.red());
		
	}
	public static void main(String[] args) {
		MainApplication.run(new MotionTestModule());
	}

}
