package mrl.motion.viewer.module;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;

public abstract class RuntimeMotionController {

	public ArrayList<Motion> totalMotion = new ArrayList<Motion>();
	public boolean cameraTracking = false;
	
	protected int frame;
	protected boolean isStop = false;
	protected Pose2d currentPose = new Pose2d(Pose2d.BASE);
	
	protected abstract Motion step(Point3d mouse, Point2d mouseLocal);
	
	public Pose2d getCurrentPose() {
		return currentPose;
	}

	public void startRuntimeModule(final MainApplication app) {
		final long startTime = System.currentTimeMillis();
		final MainViewerModule viewer = app.getModule(MainViewerModule.class);
		app.dummyParent().getDisplay().timerExec(1, new Runnable() {
			@Override
			public void run() {
				if (app.dummyParent().isDisposed()) return;
				if (isStop) return;
				
				while (true){
					int dt = (int)(System.currentTimeMillis() - startTime);
					int tIndex = dt/33;
					if (frame > tIndex) break;
					frame++;
					
					Point3d mouse = viewer.getPickPoint();
					if (mouse == null) break;
					
					Point2d mouseLocal = Pose2d.to2d(mouse);
					mouseLocal = currentPose.globalToLocal(mouseLocal);
					Motion motion = step(mouse, mouseLocal);
					if (motion == null) break;
					currentPose = PositionMotion.getPose(motion);
					totalMotion.add(motion);
					app.getModule(ItemListModule.class).addSingleItem("Motion", new MotionData(Utils.singleList(motion)));
					if (cameraTracking) {
						viewer.addCameraTracking(MathUtil.getTranslation(motion.root()));
					}
				}
				app.dummyParent().getDisplay().timerExec(1, this);
			}
		});
	}
}
