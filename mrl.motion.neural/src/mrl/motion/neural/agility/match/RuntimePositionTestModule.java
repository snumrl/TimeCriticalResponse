package mrl.motion.neural.agility.match;

import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.PositionModel;
import mrl.motion.neural.agility.PositionModel.PositionGoal;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.MotionMatching.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class RuntimePositionTestModule extends Module {

	private MDatabase database;
	private Point2d goalPosition;

	
	long startTime = -1;
	int frame = 0;
	int lastUpdatedFrame = -99999;
	
	int currentIndex = -1;
	MotionSegment currentSegment;
	Motion currentMotion = null;
	private MotionMatching matching;
	ArrayList<Motion> totalList = new ArrayList<Motion>();
	
	PositionModel model;
	
	@Override
	protected void initializeImpl() {
		String folder= "dc_loco";
//		String folder= "dc_jog2";
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
//		RotationMotionMatching.TRANSITION_ERROR_TO_ROT_RATIO = 90;
		database = TrainingDataGenerator.loadDatabase(folder);
		
//		RotationMotionMatching.STRAIGHT_MARGIN = -1;
		model = new PositionModel();
		matching = model.makeMotionMatching(database);
		MainViewerModule viewer = getModule(MainViewerModule.class);
		viewer .getPickPoint();
		currentMotion = database.getMotionList()[600];
//		currentMotion = database.findMotion("PC_W_RapidTurn_Jog-002_C.bvh", 433);
		
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
				if (e.keyCode == '1') {
					goalPosition = new Point2d(Pose2d.to2d(mouseP));
					lastUpdatedFrame = -99999;
				}
			}
		});
		
		dummyParent().getDisplay().timerExec(1, new Runnable() {
			@Override
			public void run() {
				if (dummyParent().isDisposed()) return;
				while (true){
					if (isStop) return;
					if (goalPosition == null) break;
					if (startTime < 0) startTime = System.currentTimeMillis();
					int dt = (int)(System.currentTimeMillis() - startTime);
					int tIndex = dt/33;
					if (frame > tIndex) break;
					frame++;
					if (frame > lastUpdatedFrame + Configuration.MOTION_TRANSITON_MARGIN) {
						long t = System.currentTimeMillis();
						updateMotion();
						System.out.println("dt :: " + (System.currentTimeMillis()-t));
						lastUpdatedFrame = frame;
					}
					currentIndex = frame - lastUpdatedFrame;
					currentMotion = currentSegment.getMotionList().get(currentIndex);
					totalList.add(currentMotion);
					System.out.println(frame + " : " + currentMotion);
					getModule(ItemListModule.class).addSingleItem("Motion", new MotionData(Utils.singleList(currentMotion)));
					getModule(ItemListModule.class).addSingleItem("Goal", new Point3d(Pose2d.to3d(goalPosition)), ItemDescription.red());
					getModule(MainViewerModule.class).addCameraTracking(MathUtil.getTranslation(currentMotion.root()));
				}
				dummyParent().getDisplay().timerExec(1, this);
			}
		});
		
		addMenu("&Menu", "&Test\tCtrl+T", SWT.MOD1 + 'T', new Runnable() {
			@Override
			public void run() {
				isStop = true;
				getModule(ItemListModule.class).addSingleItem("Motion", new MotionData(totalList));
			} 
		});
	}
	boolean isStop = false;
	
	private void updateMotion() {
		Pose2d currentPose = PositionMotion.getPose(currentMotion);
		Point2d targetPos = currentPose.globalToLocal(goalPosition);
		PositionGoal goal = model.sampleIdleGoal();
		goal.target = targetPos;
		goal.setTime(25);
		MatchingPath path = matching.searchBest(currentMotion.motionIndex, goal);
		int[][] pp = path.getPath();
		int mMargin = 0;
		pp[0][0] -= mMargin;
		pp[pp.length-1][1] += mMargin;
		
		MotionSegment segment = MotionSegment.getPathMotion(database.getMotionList(), pp);
		int timeConstraint = Math.min(path.time, goal.timeLimit);
		segment = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, goal.getEditingConstraint(), timeConstraint);
		
		if (currentSegment == null) {
			Pose2d pose = PositionMotion.getPose(segment.getMotionList().get(0));
			Matrix4d mm = Pose2d.globalTransform(pose, Pose2d.BASE).to3d();
			MotionSegment.align(segment, mm);
			currentSegment = segment;
		} else {
//			int cIdx = currentSegment.getMotionList().indexOf(currentMotion);
			currentSegment = new MotionSegment(currentSegment, 0, currentIndex);
			currentSegment = MotionSegment.stitch(currentSegment, segment, true);
			currentSegment = new MotionSegment(currentSegment, currentIndex+1, currentSegment.length()-1);
//			FootSlipCleanup.clean(currentSegment);
		}
	}
	
	public static void main(String[] args) {
		MainApplication.run(new RuntimePositionTestModule());
	}
}
