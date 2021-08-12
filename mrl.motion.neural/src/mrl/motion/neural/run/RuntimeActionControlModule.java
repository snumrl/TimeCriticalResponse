package mrl.motion.neural.run;

import java.util.LinkedList;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
//import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.AgilityControlParameterGenerator;
import mrl.motion.neural.agility.PokeModel;
import mrl.motion.neural.agility.StuntModel;
import mrl.motion.neural.data.DirectionJumpControl;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.motion.viewer.ogre.OgreJNI;
import mrl.motion.viewer.ogre.OgreJNI.OgreStatus;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class RuntimeActionControlModule extends Module{
	
	private boolean useAgility = false;
	
	private PythonRuntimeController c;
	private LinkedList<Integer> actionQueue = null;
	private LinkedList<Integer> actionStartFrames = null;
	private double agility = 25;
	
	private int activatedCount = 0;

	@Override
	protected void initializeImpl() {
		final MainViewerModule viewer = getModule(MainViewerModule.class);
		viewer.getPickPoint();
		getModule(ItemListModule.class).addSingleItem("Origin", Pose2d.BASE, new ItemDescription(new Vector3d(1, 0, 0)));
		
		c = new PythonRuntimeController() {
			@Override
			public double[] getControlParameter() {
				if (actionQueue == null) return null;
				
				int action = 0;
				if (actionQueue.size() > 0) {
					action = actionQueue.getFirst();
					int timePassed = frame - actionStartFrames.getFirst();
					boolean isFinished = Utils.last(prevOutput) > 0.5;
//					boolean isFinished = false;
//					if (Utils.last(prevOutput) > 0.5) {
//						activatedCount++;
//						if (activatedCount > 2) {
//							isFinished = true;
//							activatedCount = 0;
//						}
//					} else {
//						activatedCount = 0;
//					}
					
					if (timePassed > 5 && isFinished) {
						int prevAction = actionQueue.removeFirst();
						actionStartFrames.removeFirst();
						System.out.println("action finish : " + actionQueue.size() + " : " +  prevAction + " : " + frame + " : " + timePassed + " : " + Utils.last(prevOutput));
						
						if (actionQueue.size() > 0) {
//							action = actionQueue.getFirst();
							actionStartFrames.removeFirst();
							actionStartFrames.addFirst(frame);
//							System.out.println("Set action : " + action);
						} else {
//							action = 0;
						}
					}
				}
				System.out.println("activation : " + agility + " : " + actionQueue.size() + " : " + action + " : " + frame + " : " + Utils.last(prevOutput));
				double[] control = AgilityControlParameterGenerator.getActionType(actionSize(), action);
//				double[] control = AgilityControlParameterGenerator.getActionType(PokeModel.actionTypes.length, action);
				if (useAgility) {
					control = MathUtil.concatenate(control, new double[] { agility });
				}
				return control;
			}
		};
		
		viewer.getMainViewer().getCanvas().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				Point3d mouseP = viewer.getPickPoint();
				if (mouseP == null) return;
				Point2d mp2d = Pose2d.to2d(mouseP);
				Point2d cPos = c.g.pose.position;
				if ((e.keyCode >= '1') && (e.keyCode <= '9')){
					int action = e.keyCode - '1';
					if (action >= actionSize()) return;
					int actionStartFrame = c.frame;
					if (actionQueue == null) {
						actionQueue = new LinkedList<Integer>();
						actionStartFrames = new LinkedList<Integer>();
					}
					actionQueue.add(action);
					actionStartFrames.add(actionStartFrame);
					System.out.println("add action : " + actionQueue);
					if (action == 0) {
						actionQueue.clear();
						actionStartFrames.clear();
					}
				}
				if (e.keyCode == 'q') agility = 15;
				if (e.keyCode == 'w') agility = 20;
				if (e.keyCode == 'e') agility = 25;
				if (e.keyCode == 'r') agility = 30;
				if (e.keyCode == 't') agility = 35;
			}
		});
		
//		1	"i",
//		2	"p",
//		3	"k",
//		4	"bs",
//		5	"jk",
//		6	"jbs",
//		7	"jd",
		
		c.cameraTracking = true;
		MotionDataConverter.setAllJoints();
//		c.init("walk_dir_rg");
//		c.init("walk_dc");
//		c.init("walk_dc_turn");
		Configuration.BASE_MOTION_FILE = "t_pose_sue.bvh";
//		Configuration.BASE_MOTION_FILE = "t_pose_actor.bvh";
		MotionDataConverter.setUseOrientation();
		useAgility = true;
		c.init("stunt_retargeted_ue_dy_ag_300000");
//		c.init("dc_stunt_dy");
//		c.init("poke_bvh_ver2_ni_dy");
//		c.init("poke_bvh_ver2_v2_25");
		c.startRuntimeModule(app());
	}

	private int actionSize() {
		int actionSize = c.normal.xMeanAndStd[0].length - 1 - (useAgility ? 1 : 0);
		return actionSize;
	}
	
	public static void main(String[] args) {
		MainApplication.run(new RuntimeActionControlModule());
		OgreJNI.close();
	}
}
