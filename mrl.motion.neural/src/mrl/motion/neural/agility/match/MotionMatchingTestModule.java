package mrl.motion.neural.agility.match;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;

import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.PokeModel.PokeGoal;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.StuntModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.MotionMatching.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class MotionMatchingTestModule extends Module {

	private MDatabase database;

	@Override
	protected void initializeImpl() {
		MDatabase.loadEventAnnotations = true;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		AgilityModel model;
		
//		String folder= "dc_loco";
//		String folder= "dc_jump_jog";
//		RotationMotionMatching.STRAIGHT_MARGIN = 3;
//		String folder= "dc_jump_run";
//		RotationMotionMatching.STRAIGHT_MARGIN = 1;
		
//		AgilityModel.GOAL_TIME_LIMIT = 30;
//		JumpModel.rotErrorRatio = 100;
//		JumpModel.USE_STRAIGHT_SAMPLING = true;
//		AgilityGoal.TIME_EXTENSION_RATIO = 0.1;
////		database = TrainingDataGenerator.loadDatabase(folder);
//		
//		FootContactDetection.footDistPointOffset = new int[]{
//				30, 40, 0
//		};
//		
//		AgilityModel model;
////		model = new JumpModel(folder);
//		PositionModel.posErrorRatio = 5;
//		model = new PositionModel();
//		database = TrainingDataGenerator.loadDatabase(folder, "t_pose_poke.bvh");
		
//		String folder= "runjogwalk";
//		String tPoseFile = "t_pose_actor.bvh";
//		RotationModel.GOAL_TIME_LIMIT = 25; 
//		RotationMotionMatching.rotErrorRatio = 1000;
//		RotationModel.USE_STRAIGHT_SAMPLING = true;
//		model = new RotationSpeedModel();
//		
//		String folder= "poke_bvh_ver2";
//		String tPoseFile = "t_pose_poke.bvh";
//		RotationModel.GOAL_TIME_LIMIT = 20; 
//		model = new PokeModel(folder);
		
		String folder= "dc_stunt";
		String tPoseFile = "t_pose_actor.bvh";
		RotationModel.GOAL_TIME_LIMIT = 25; 
		model = new StuntModel(folder);
		
		
		database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		MotionMatching matching = model.makeMotionMatching(database);
//		MotionMatching2 matching = new MotionMatching2(database);
		MainViewerModule viewer = getModule(MainViewerModule.class);
		viewer.getPickPoint();
//		Motion startMotion = database.findMotion("PC_W_RapidTurn_Jog-002_C.bvh", 1361);
//		Motion startMotion = database.findMotion("PC_W_RapidTurn_Jog-002_C.bvh", 433);
		
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
				Vector2d mp2d = new Vector2d(Pose2d.to2d(mouseP));
//				mp2d = new Vector2d(0.9936,0.1131);
				if (e.keyCode >= '1' && e.keyCode <= '9') {
//					double targetRotation = MathUtil.directionalAngle(Pose2d.BASE.direction, mp2d);
//					RotationGoal goal = new RotationGoal(targetRotation, 23);
					AgilityGoal goal = model.sampleRandomGoal(Pose2d.BASE);
					int startMotionIndex = matching.pickRandomStartMotion();
					
					PokeGoal g = (PokeGoal)goal;
					g.actionType = e.keyCode - '1';
					startMotionIndex = matching.database.findMotion("s_001_2_1.bvh", 164).motionIndex;
//					startMotionIndex = matching.database.findMotion("2xAttack_Move_med_halfsword_R_L_1.bvh", 89).motionIndex;
					
					System.out.println("Start :: " + database.getMotionList()[startMotionIndex]);
//					System.out.println("motion : " + matching.database.getMotionList()[startMotionIndex-15]);
//					ActionDistDP dp = ((JumpMotionMatching)matching).dp;
//					double[][][] dt = dp.distanceTable;
//					for (int t = 0; t < 20; t++) {
//						int idx = dp.fMotionMap[startMotionIndex-15].index;
//						System.out.println("dttt : " + t + " : " + dt[3][t][idx]);
//					}
//					System.exit(0);
					
//					startMotionIndex = matching.database.findMotion("BF_running_curve_jump_04.bvh", 49).motionIndex;
//					goal = model.sampleRandomGoal(Pose2d.BASE);
					
					MatchingPath path = matching.searchBest(startMotionIndex, goal);
					int[][] pp = path.getPath();
					int mMargin = 4;
					pp[0][0] -= mMargin;
					pp[pp.length-1][1] += mMargin;
					int timeConstraint = Math.min(path.time, goal.timeLimit);
					MotionSegment segment = MotionSegment.getPathMotion(database.getMotionList(), pp);
					MotionSegment notEdited = segment;
					segment = MotionEdit.getEditedSegment(segment, mMargin, segment.length()-1 - mMargin, goal.getEditingConstraint(), timeConstraint);
					
					FootSlipCleanup.clean(segment);
					ArrayList<Motion> motionList = segment.getMotionList();
					motionList = PositionMotion.getAlignedMotion(motionList, mMargin);
					listModule.addSingleItem("motion", new MotionData(motionList), ItemDescription.red());
					listModule.addSingleItem("notEdited", new MotionData(PositionMotion.getAlignedMotion(notEdited.getMotionList(), mMargin)));
					listModule.addSingleItem("goal", goal.getControlParameterObject(Pose2d.BASE, Pose2d.BASE), ItemDescription.red());
//					listModule.addSingleItem("goal", new Pose2d(new Point2d(), mp2d), ItemDescription.red());
					viewer.replay();
					System.out.println("goal : " + goal);
				}
			}
		});
	}
	
	public static void main(String[] args) {
		MainApplication.run(new MotionMatchingTestModule());
	}
}
