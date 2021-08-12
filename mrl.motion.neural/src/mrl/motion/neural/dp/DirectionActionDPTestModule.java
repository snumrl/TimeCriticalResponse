package mrl.motion.neural.dp;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;

import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.DirectionActionDP;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.motion.viewer.module.RuntimeMotionController;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class DirectionActionDPTestModule extends Module{
	
	private DirectionActionDP dp;
	
	private Point2d actionPos;
	private int actionType = -1;
	private int actionTime = -1;
	
	@Override
	protected void initializeImpl() {
		String dataFolder = "data\\FightMotion";
		String cacheFile = "dpCacheDirAction_Fight.dat";
//		String dataFolder = "danceCard\\dc_action";
//		String cacheFile = "dpCacheDirAction.dat";
		
		Configuration.setDataFolder(dataFolder);
		MDatabase database = MDatabase.load();
		dp = new DirectionActionDP(database);
		dp.load("..\\mrl.motion.data\\" + cacheFile);
		
		final RuntimeMotionController c = new RuntimeMotionController() {
			@Override
			protected Motion step(Point3d mouse, Point2d mouseLocal) {
				System.out.println("Action :: " + actionTime + " : " + actionPos);
				if (actionPos == null) {
					double angle = MathUtil.directionalAngle(Pose2d.BASE.direction, mouseLocal);
					return dp.step(20, new double[] { angle, 0 });
				} else {
					Point2d p = getCurrentPose().globalToLocal(actionPos);
					double angle = MathUtil.directionalAngle(Pose2d.BASE.direction, p);
					Motion m = dp.step(actionTime + 1, new double[] { angle, actionType });
					actionTime--;
					if (actionTime < 0) {
						actionPos = null;
						actionType = -1;
						app().getModule(ItemListModule.class).addSingleItem("Target", null);
					}
					return m;
					
				}
			}
		};
		c.cameraTracking = true;
		c.startRuntimeModule(app());
		
		final MainViewerModule viewer = getModule(MainViewerModule.class);
		viewer.getMainViewer().getCanvas().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				Point3d mouseP = viewer.getPickPoint();
				if (mouseP == null) return;
				Point2d mp2d = Pose2d.to2d(mouseP);
				
				if (e.keyCode == '`') {
					// move
					actionPos = mp2d;
					actionTime = 29;
					actionType = 0;
					app().getModule(ItemListModule.class).addSingleItem("Target", mouseP);
				}
				if (e.keyCode == '1') {
					// do kick
					actionPos = mp2d;
					actionTime = 29;
					actionType = 1;
					app().getModule(ItemListModule.class).addSingleItem("Target", mouseP);
				}
				if (e.keyCode == '2') {
					// do punch
					actionPos = mp2d;
					actionTime = 29;
					actionType = 2;
					app().getModule(ItemListModule.class).addSingleItem("Target", mouseP);
				}
			}
		});
	}
	
	
	public static void main(String[] args) {
		MainApplication.run(new DirectionActionDPTestModule());
	}

}
