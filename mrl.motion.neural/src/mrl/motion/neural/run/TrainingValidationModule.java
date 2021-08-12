package mrl.motion.neural.run;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.data.BallTrajectoryGenerator;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.neural.data.PointIKSolver;
import mrl.motion.position.PositionResultMotion;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.motion.viewer.ogre.OgreJNI;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class TrainingValidationModule extends Module {

	@Override
	protected void initializeImpl() {
//		Configuration.BASE_MOTION_FILE = "t_pose_poke.bvh";
//		Configuration.BASE_MOTION_FILE = "t_pose_sue.bvh";
//		Configuration.BASE_MOTION_FILE = "t_pose_jue.bvh";
//		Configuration.BASE_MOTION_FILE = "t_pose_actor.bvh";
		Configuration.BASE_MOTION_FILE = "t_pose_ue2.bvh";
		
		String label = null;
		
		boolean dirControl = true;
		int cOffset = 2;
		int cOffset2 = 1;
//		int cOffset = 3;
//		int cOffset2 = 1;
		
		MotionDataConverter.setAllJoints();
		MotionDataConverter.setNoBall();
		MotionDataConverter.setUseOrientation();
		MotionDataConverter.setOrientationJointsByFileOrder();
		MotionDataConverter.useMatrixForAll = true;
		MotionDataConverter.useTPoseForMatrix = false;
		
//		label = "dc_loco_g_fo_pos_512_4";
//		label = "jump_jog_ret_ue4_graph";
//		label = "martial_arts_sp_da";
		label = "martial_arts_sp";
//		label = "jump_t_rr_pn_dpf_af_short05_t60";
//		label = "jump_t_rr_pn_dpf_af";
//		label = "stunt_only_t_rr_pn_ad_dpf";
//		label = "merged_ue_npc_test";
//		label = "runjogwalk_r1000_30";
//		label = "dc_jog2_dir_mm2_r50_25";
//		label = "dc_jump_jog_2_ac_r1000_30";
//		label = "dc_loco_pos_r60_17";
//		label = "dc_jog_dir_gbc";
//		label = "jogDir_15";
		
//		Normalizer.NEURALDATA_PREFIX = "output\\adaptiveData";
		Normalizer normal = new Normalizer(label);
		System.out.println(normal.xMeanAndStd[0].length + " , " + normal.yMeanAndStd[0].length + " :: " + normal.xList.size());
		
//		{
//			ArrayList<ArrayList<double[]>> data = DataExtractor.readListData("neuralData\\" + label + "\\data\\yData.dat");
//			int count1 = 0;
//			int count2 = 0;
//			for (ArrayList<double[]> dd : data){
//				count1++;
//				count2+= dd.size();
//			}
//			System.out.println("list size :: " + count1 + " / " + count2);
//		}
		
		System.out.println(Arrays.toString(normal.xMeanAndStd[0]));
		System.out.println(Arrays.toString(normal.xMeanAndStd[1]));
		System.out.println(Arrays.toString(normal.yMeanAndStd[0]));
		System.out.println(Arrays.toString(normal.yMeanAndStd[1]));
//		for (int i = 0; i < normal.yMeanAndStd[1].length; i++) {
//			if (normal.yMeanAndStd[1][i] <= 0.001){
//				int jIndex = (i - 6)/3;
//				int aIndex = (i - 6)%3;
//				String j = MotionDataConverter.OrientationJointList[jIndex];
//				System.out.println(i + " ; " + j + aIndex + " : " + normal.yMeanAndStd[0][i] + "\t" + normal.yMeanAndStd[1][i]);
//			}
//		}
		System.out.println("########");
//		System.exit(0);
		
		{
			double min = Integer.MAX_VALUE;
			double max = -Integer.MAX_VALUE;
			for (double[] l : normal.xList){
				for (double v : l){
					min = Math.min(min, v);
					max = Math.max(max, v);
					if (Double.isNaN(v)){
						System.out.println("VVVVVVVVVVVVVVVVVVV");
						System.exit(0);
					}
				}
			}
			for (double[] l : normal.yList){
				for (double v : l){
					min = Math.min(min, v);
					max = Math.max(max, v);
					if (Double.isNaN(v)){
						System.out.println("VVVVVVVVVVVVVVVVVVV");
						System.exit(0);
					}
				}
			}
			System.out.println("minmax :: " + min + " , " + max);
//			System.exit(0);
		}
		
		int showIndex = -1;
		double ooMax = 0;
		for (int i = 0; i < normal.yList.size(); i++) {
			double[] origin = normal.yList.get(i);
			double oMax = 0;
			for (double v : origin){
				oMax = Math.max(oMax, Math.abs(v));
			}
			ooMax = Math.max(ooMax, oMax);
			if (oMax > 25){
				System.out.println(i + " : :");
				for (int j = -3; j <= 3; j++) {
					int ii = i + j; 
					System.out.println(ii + " : " + Arrays.toString(normal.yList.get(ii)));
					System.out.println(Arrays.toString(normal.deNormalizeX(normal.xList.get(ii))));
				}
//				System.exit(0);
				showIndex = i - 50;
				break;
			}
			double[] y = normal.deNormalizeY(origin);
			double max = 0;
			for (int j = 0; j < 3; j++) {
				max = Math.max(max, Math.abs(y[j]));
			}
			if (max > 1000){
				System.out.println(i + " : :");
				for (int j = -3; j <= 3; j++) {
					int ii = i + j; 
					System.out.println(ii + " : " + Arrays.toString(normal.deNormalizeY(normal.yList.get(ii))));
					System.out.println(Arrays.toString(normal.deNormalizeX(normal.xList.get(ii))));
				}
//				System.exit(0);
				showIndex = i - 50;
				break;
			}
		}
		System.out.println("omax :: " + ooMax);
//		System.exit(0);
		
		RuntimeMotionGenerator g = new RuntimeMotionGenerator();
//		RuntimeMotionGenerator.ikSolver = null;
		PositionResultMotion pm = new PositionResultMotion();
		TimeBasedList<Pose2d> targetList = new TimeBasedList<Pose2d>();
//		int poseStart = 0;
//		int poseStart = 4;
		int poseStart = Math.max(0, normal.xMeanAndStd[0].length - cOffset);
		
//		int len = Math.min(100, normal.yList.size());
//		int offset = showIndex;
		int len = Math.min(2000, normal.yList.size());
		int offset = 0;
//		len = 200;
//		int offset = 200000;
		MotionTransform t = new MotionTransform();
		PointIKSolver solver = new PointIKSolver(t.skeletonData, t.sampleMotion);
		ArrayList<Motion> motionList = new ArrayList<Motion>();
		TimeBasedList<Point3d> ballList = new TimeBasedList<Point3d>();
//		int len = Math.min(10000, normal.yList.size());
		for (int i = offset; i < offset+len; i++) {
			double[] y = normal.yList.get(i);
			y = normal.deNormalizeY(y);
			System.out.println("## " + i + " : " + (i - offset) + " : " + y[y.length-1]);
//			System.out.println(Arrays.toString(y));
			
			double[] x = normal.xList.get(i);
			x = normal.deNormalizeX(x);
			System.out.println(Arrays.toString(x));
			int xLen = x.length - cOffset2;
			if (poseStart < xLen){
				Pose2d p = null;
				int s = poseStart;
				if (poseStart + 4 <= xLen){
					p = g.pose.localToGlobal(new Pose2d(x[s+0], x[s+1], x[s+2], x[s+3]));
				} else if (poseStart + 3 <= xLen){
					Vector2d v = MathUtil.rotate(Pose2d.BASE.direction, x[s]);
					s++;
					p = g.pose.localToGlobal(new Pose2d(x[s+0], x[s+1], v.x, v.y));
					
				} else if (poseStart + 2 <= xLen){
					if (dirControl) {
						p = g.pose.localToGlobal(new Pose2d(0, 0, x[s+0], x[s+1]));
					} else {
						p = g.pose.localToGlobal(new Pose2d(x[s+0], x[s+1], 1, 0));
						p.direction = Pose2d.BASE.direction;
						
					}
//					p = g.pose.localToGlobal(new Pose2d(0, 0, x[s+0], x[s+1]));
//					p.direction = Pose2d.BASE.direction;
				} else {
					Vector2d v = MathUtil.rotate(Pose2d.BASE.direction, x[s]);
					p = new Pose2d(new Point2d(), v);
					p = g.pose.localToGlobal(p);
				}
				targetList.add(p);
			}
			
			
			PositionResultMotion motion = g.update(y);
			pm.add(motion.get(0));
			
//			HashMap<String, Point3d> map = MotionDataConverter.dataToPointMap(y);
//			Motion mm = solver.solve(map, g.pose);
//			mm.ballContact = motion.get(0).ballContact;
//			mm.isLeftFootContact = motion.get(0).footContact.left;
//			mm.isRightFootContact = motion.get(0).footContact.right;
//			motionList.add(mm);
			
			if (MotionDataConverter.includeBall){
				double[] b = new double[3];
				System.arraycopy(y, 0, b, 0, 3);
				Point3d p = new Point3d(b[0], 0, b[2]);
				p = Pose2d.to3d(g.pose.localToGlobal(Pose2d.to2d(p)));
				p.y = b[1];
				ballList.add(p);
				System.out.println("bb : " + p + " : " + y[y.length-1]);
			}
		}
		
		getModule(MainViewerModule.class);
		getModule(ItemListModule.class).addSingleItem("motion", pm);
		getModule(ItemListModule.class).addSingleItem("motion2", new MotionData(g.motionList));
		getModule(ItemListModule.class).addSingleItem("target", targetList);
		getModule(ItemListModule.class).addSingleItem("ball", ballList, new ItemDescription(BallTrajectoryGenerator.BALL_RADIUS));
		
//		getModule(MainViewerModule.class).addTimeListener(new Listener() {
//			@Override
//			public void handleEvent(Event event) {
//				Motion m = motionList.get(getModule(MainViewerModule.class).getTimeIndex());
//				OgreJNI.setMotion(new MotionData[]{ new MotionData(Utils.singleList(m)) });
//			}
//		});
	}
	
	public static void main(String[] args) {
		MainApplication.run(new TrainingValidationModule());
		OgreJNI.close();
	}

}
