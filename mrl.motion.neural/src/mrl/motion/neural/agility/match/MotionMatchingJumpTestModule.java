package mrl.motion.neural.agility.match;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

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
import mrl.motion.neural.agility.FightModel;
import mrl.motion.neural.agility.FightModel.FightGoal;
import mrl.motion.neural.agility.FightModel.FightMotionMatching;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.MotionMatching.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class MotionMatchingJumpTestModule extends Module {

	private MDatabase database;

	@Override
	protected void initializeImpl() {
		MDatabase.loadEventAnnotations = true;
//		String folder= "jump_flip_loco_data";
		String folder= "dc_jump";
//		String folder= "dc_fight";
//		String folder= "dcFightMerged";
//		String folder= "dc_jog2";
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		database = TrainingDataGenerator.loadDatabase(folder);
		
		FightModel.POS_ERROR_RATIO = 200;
		FightModel.actionTypes = new String[] {
				"jump_both",
				"jump_one",
				"jump_moving",
				"flip"	
		};
		AgilityModel.TIME_EXTENSION_MIN_TIME = 8;
		AgilityModel.GOAL_TIME_LIMIT = 35;
//		FightModel.USE_POSITON_CONTROL = false;
		FightModel model = new FightModel(folder);
		FightMotionMatching matching = model.makeMotionMatching(database);
//		MotionMatching2 matching = new MotionMatching2(database);
		MainViewerModule viewer = getModule(MainViewerModule.class);
		viewer.getPickPoint();
		
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
				Point2d mp2d = new Point2d(Pose2d.to2d(mouseP));
//				mp2d = new Vector2d(0.9936,0.1131);
				if (e.keyCode == '1' || e.keyCode == '2' ||
						e.keyCode == '3' || e.keyCode == '4') {
					double gLen = MathUtil.length(mp2d);
					double tRatio = gLen/100d;
					tRatio = Math.min(1.2, tRatio);
					tRatio = 1;
					int timeLimit = MathUtil.round(tRatio*AgilityModel.GOAL_TIME_LIMIT);
					FightGoal goal =model.newGoal(mp2d, e.keyCode - '1', timeLimit);
					
					MatchingPath path = matching.searchBest(matching.pickRandomStartMotion(), goal);
					System.out.println("last motion :: " + path.current + " : " + path.time + " / " + goal.timeLimit + " : " + AgilityModel.GOAL_TIME_LIMIT);
					int[][] pp = path.getPath();
					int mMargin = 15;
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
					listModule.addSingleItem("goal", Pose2d.to3d(mp2d), ItemDescription.red());
					viewer.replay();
				}
			}
		});
	}
	
	public static void main(String[] args) {
		MainApplication.run(new MotionMatchingJumpTestModule());
	}
}
