package mrl.motion.viewer;

import javax.vecmath.Vector3d;

import mrl.motion.data.MotionData;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class MotionNavigator extends AnimationPlayer{
	
	protected MotionViewer viewer;

	public MotionNavigator(Composite parent) {
		super(parent, true);
		
		MotionNavigator navigator = this;
		navigator.viewer.setScale(0.25);
		navigator.viewer.skeletonColor = new Vector3d(0.8, 0.8, 0.8);
		navigator.viewer.eye = new Vector3d(-163.03298524678615, 56.943046312881094, 35.86409448882707);
		navigator.viewer.center = new Vector3d(2.7325483582816688, 20.29166452107147, 11.701930660868829);
		navigator.viewer.upVector = new Vector3d(0.3243748441825436, 0.9452422181149468, 0.036029287457299365);
	}
	
	protected MotionAnimator createMotionViewer(Composite parent){
		viewer = new MotionViewer(parent);
		return viewer;
	}
	
	public MotionViewer getViewer(){
		return viewer;
	}
	
	public void setMotionData(MotionData motionData){
		setFPS(motionData.framerate);
		setMaxFrame(motionData.motionList.size());
		getViewer().setMotionData(motionData);
	}
	
	public static void openMotionData(MotionData motionData, int frame, double scale, boolean maximize){
		final Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(2, false));
		
		final MotionNavigator navigator = new MotionNavigator(shell);
		navigator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		navigator.getViewer().setDrawShadow(true);
		navigator.setFPS(motionData.framerate);
		navigator.setMaxFrame(motionData.motionList.size());
		navigator.getViewer().setMotionData(motionData);
		
		navigator.viewer.setScale(scale);
		
		navigator.setMotionIndex(frame);
		
		shell.setText("Motion Navigator");
		shell.setSize(1400, 1000);
		if (maximize) shell.setMaximized(true);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	
}
