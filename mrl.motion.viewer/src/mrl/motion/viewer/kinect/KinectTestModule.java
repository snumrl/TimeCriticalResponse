package mrl.motion.viewer.kinect;

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import mrl.motion.viewer.kinect.KinectJNI.KinectJoints;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.motion.viewer.module.MainViewerModule.MainViewer;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.util.FileUtil;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

public class KinectTestModule extends Module{
	
	public static String DATA_FILE = "kinectRecord.dat";
	
	private boolean isStarted = false;
	private boolean isLoaded = false;
	private ArrayList<KinectJoints> recorded = new ArrayList<>();

	@Override
	protected void initializeImpl() {
		getModule(MainViewerModule.class).addDrawer(new KinectJointItemDrawer());
		MainViewer c = getModule(MainViewerModule.class).getMainViewer();
		c.eye = new Vector3d(-9.223131969456782, 84.59546803530124, -95.53660149191845);
		c.center = new Vector3d(-11.28330940063578, 56.964158410292356, 2.912744052099384);
		c.upVector = new Vector3d(-0.051090782195466554, 0.9594957488719845, 0.2770516916953194);
		
		start();
//		
		
		dummyParent().getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (isStarted) KinectJNI.stopKinect();
			}
		});
		
		
		addMenu("&Menu", "&Save\tCtrl+S", SWT.MOD1 + 'S', new Runnable() {
			@Override
			public void run() {
				FileUtil.writeObject(recorded, DATA_FILE);
			}
		});
		
		addMenu("&Menu", "&Load\tCtrl+L", SWT.MOD1 + 'L', new Runnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				isLoaded = true;
				recorded = (ArrayList<KinectJoints>)FileUtil.readObject(DATA_FILE);
				getModule(ItemListModule.class).addSingleItem("joints", new TimeBasedList<>(recorded));
			}
		});
	/*	
		getModule(MainViewerModule.class).addTimeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (!isLoaded) return;
				System.out.println("###############");
				KinectJoints s = recorded.get(getModule(MainViewerModule.class).getTimeIndex());
				for (int i = 0; i < s.jointPositions.size(); i++) {
					System.out.println(i + " ; " + s.jointPositions.get(i));
				}
				
			}
		});*/
	}
	
	private void start() {
		KinectJNI.startKinect(0);
		isStarted = true;
		dummyParent().getDisplay().timerExec(30, new Runnable() {
			@Override
			public void run() {
				if (dummyParent().isDisposed()) return;
				
				KinectJoints status = KinectJNI.getStatus();
				if (status != null) {
					getModule(ItemListModule.class).addSingleItem("joints", status);
					recorded.add(status);
				}
				dummyParent().getDisplay().timerExec(30, this);
			}
		});
	}
	
	
	public static void main(String[] args) {
		MainApplication.run(new KinectTestModule());
	}

}
