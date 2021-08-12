package mrl.motion.neural.agility.match;

import java.util.ArrayList;

import javax.vecmath.Point2d;
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
import mrl.motion.neural.agility.JumpModel;
import mrl.motion.neural.agility.JumpModel.JumpGoal;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.MMatching.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.Utils;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class JumpTestModule extends Module {

	private MDatabase database;

	@Override
	protected void initializeImpl() {
		
		String folder= "dc_jump_jog";
		String tPoseFile = "t_pose_actor.bvh";
		AgilityModel.GOAL_TIME_LIMIT = 25; 
		AgilityModel.TIME_EXTENSION_RATIO = 0.5;
		JumpModel model = new JumpModel();
		
		database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		
		MMatching matching = model.makeMMatching(database);
//		MotionMatching2 matching = new MotionMatching2(database);
		MainViewerModule viewer = getModule(MainViewerModule.class);
		viewer .getPickPoint();
		
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
				if (e.keyCode >= '1' && e.keyCode <= '5') {
					model.sampleIdleGoal();
					JumpGoal goal = model.sampleIdleGoal();
					goal.actionType = e.keyCode - '1';
//					goal.targetPosition = new Point2d(.99, .01);
//					goal.targetPosition.scale(mp2d.length());
					goal.targetPosition.set(mp2d);
//					goal.actionType = 0;
					Motion startMotion = database.getMotionList()[400];
//					startMotion = matching.database().findMotion("PC_W_RapidTurn_Jog-002_C.bvh", 586);
					startMotion = matching.database().findMotion("PC_W_RapidTurn_Jog-002_C.bvh", 211);
					
					MatchingPath path = matching.searchBest(startMotion.motionIndex, goal);
					int[][] pp = path.getPath();
					int mMargin = 0;
					pp[0][0] -= mMargin;
					pp[pp.length-1][1] += mMargin;
					int timeConstraint = Math.min(path.time, goal.timeLimit);
					
					MotionSegment segment = MotionSegment.getPathMotion(database.getMotionList(), pp);
					MotionSegment notEdited = segment;
					segment = MotionEdit.getEditedSegment(segment, mMargin, segment.length()-1 - mMargin, goal.getEditingConstraint(), timeConstraint);
					System.out.println("constraint : " + goal.getEditingConstraint());
					FootSlipCleanup.clean(segment);
					ArrayList<Motion> motionList = segment.getMotionList();
					Pose2d p1 = Pose2d.getPose(motionList.get(mMargin));
					Pose2d p2 = Pose2d.getPose(motionList.get(segment.length()-1 - mMargin));
					System.out.println("pppp : " + p1.globalToLocal(p2));
					motionList = PositionMotion.getAlignedMotion(motionList, mMargin);
					listModule.addSingleItem("motion", new MotionData(motionList), ItemDescription.red());
					listModule.addSingleItem("notEdited", new MotionData(PositionMotion.getAlignedMotion(notEdited.getMotionList(), mMargin)));
					listModule.addSingleItem("goal", goal.getControlParameterObject(Pose2d.BASE, Pose2d.BASE), ItemDescription.red());
					viewer.replay();
				}
			}
		});
	}
	
	public static void main(String[] args) {
		MainApplication.run(new JumpTestModule());
	}
}
