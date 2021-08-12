package mrl.motion.critical.run;

import java.util.Arrays;
import java.util.LinkedList;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.eclipse.swt.events.KeyEvent;
//import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.AgilityControlParameterGenerator;
import mrl.motion.neural.agility.JumpSpeedModel;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.run.PythonRuntimeController;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class RuntimeMartialArtsControlModule extends Module{
	
	private PythonRuntimeController c;
	private LinkedList<Integer> actionQueue = null;
	private LinkedList<Integer> actionStartFrames = null;
	
	private Vector2d targetDirection;
	
	private double agility = 1;
	private boolean useDynamicAgility = true;
	
	private int locoType = 1;
	
	private boolean isActivated = false;
	private int activatedCount = 0;

	@Override
	protected void initializeImpl() {
		final MainViewerModule viewer = getModule(MainViewerModule.class);
		viewer.getPickPoint();
		getModule(ItemListModule.class).addSingleItem("Origin", Pose2d.BASE, new ItemDescription(new Vector3d(1, 0, 0)));
		
		c = new PythonRuntimeController() {
			@Override
			public double[] getControlParameter() {
				if (targetDirection == null) return null;
				
				int locoSize = JumpSpeedModel.LOCO_ACTION_SIZE;
				ItemDescription desc = new ItemDescription(new Vector3d(1, 1, 0));
				desc.size = 10;
				Pose2d goalPose = new Pose2d(g.pose.position, targetDirection);
				getModule(ItemListModule.class).addSingleItem("Goal", goalPose, desc);
				double targetAngle = MathUtil.directionalAngle(g.pose.direction, targetDirection);
				int action = locoType;
				
				if (actionQueue.size() > 0) {
					action = actionQueue.getFirst();
					int timePassed = frame - actionStartFrames.getFirst();
					if (isActivated) {
						activatedCount++;
						if (activatedCount > 3) {
							isActivated = false;
							activatedCount = -1;
							
							int prevAction = actionQueue.removeFirst();
							actionStartFrames.removeFirst();
							System.out.println("action finish : " + actionQueue.size() + " : " +  prevAction + " : " + frame + " : " + timePassed + " : " + Utils.last(prevOutput));
							
							if (actionQueue.size() > 0) {
								actionStartFrames.removeFirst();
								actionStartFrames.addFirst(frame);
							}
						}
					} else {
						boolean isFinished = Utils.last(prevOutput) > 0.4;
						if (timePassed > 5 && isFinished) {
							isActivated = true;
							activatedCount = 0;
						}
					}
				}
				if (action >= locoSize) {
					getModule(ItemListModule.class).addSingleItem("Goal", Pose2d.to3d(g.pose.position), desc);
					targetAngle = Double.NaN;
				}
				double[] control = AgilityControlParameterGenerator.getActionType(MartialArtsConfig.actionTypes.length, action);
				control = MathUtil.concatenate(control, new double[] { targetAngle });
				if (useDynamicAgility) {
					control = MathUtil.concatenate(control, new double[] { agility });
				}
				System.out.println("activation : " + agility + " : " + actionQueue.size() + " : " + action + " : " + frame + " : " + Utils.toString(Utils.last(prevOutput)));
				System.out.println(Arrays.toString(control));
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
				Vector2d direction = MathUtil.sub(mp2d, cPos);
				
				char locoActionSize = String.valueOf(MartialArtsConfig.LOCO_ACTION_SIZE).charAt(0);
				char fullActionSize = String.valueOf(MartialArtsConfig.actionTypes.length).charAt(0);
				if (e.keyCode >= '1' && e.keyCode <= locoActionSize) {
					targetDirection = direction;
					locoType = e.keyCode - '1';
				} else if ((e.keyCode > locoActionSize) && (e.keyCode <= fullActionSize)){
					int action = e.keyCode - '1';
					int actionStartFrame = c.frame;
					if (actionQueue == null) {
						actionQueue = new LinkedList<Integer>();
						actionStartFrames = new LinkedList<Integer>();
					}
					actionQueue.add(action);
					actionStartFrames.add(actionStartFrame);
					System.out.println("add action : " + actionQueue);
				}
				
				if (e.keyCode == 'q') agility = 0.7;
				if (e.keyCode == 'w') agility = 0.85;
				if (e.keyCode == 'e') agility = 1;
				if (e.keyCode == 'r') agility = 1.1;
				if (e.keyCode == 't') agility = 1.2;
			}
		});
		
		c.cameraTracking = true;
		MotionDataConverter.setAllJoints();
		Configuration.BASE_MOTION_FILE = "data\\t_pose_ue2.bvh";
		MotionDataConverter.setUseOrientation();
		MotionDataConverter.setOrientationJointsByFileOrder();
		
		useDynamicAgility = true;
		c.init("martial_arts_sp_da");
		
		actionQueue = new LinkedList<Integer>();
		actionStartFrames = new LinkedList<Integer>();
		targetDirection = new Vector2d(1, 0);
		locoType = 0;
		
		c.startRuntimeModule(app());
	}

	
	public static void main(String[] args) {
		MainApplication.run(new RuntimeMartialArtsControlModule());
	}
}
