package mrl.motion.viewer;

import javax.vecmath.Vector3d;

import mrl.motion.data.MotionData;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class MultiCharacterNavigator extends AnimationPlayer{
	
	public static Vector3d basicColor = new Vector3d(0.8, 0.8, 0.8);
	public static Vector3d selectedColor = new Vector3d(0.9, 0.9, 0);
	
	protected MultiCharacterViewer viewer;

	public MultiCharacterNavigator(Composite parent) {
		this(parent, true);
	}
	public MultiCharacterNavigator(Composite parent, boolean bigButton) {
		super(parent, bigButton);
	}
	
	@Override
	protected MotionAnimator createMotionViewer(Composite parent) {
		viewer = new MultiCharacterViewer(parent);
		setSelectedMotionColor(-1);
		return viewer;
	}
	
	public void setSelectedMotionColor(int selectedIndex){
		Vector3d[] colorList = new Vector3d[Math.max(16, selectedIndex+3)];
		for (int i = 0; i < colorList.length; i++) {
			colorList[i] = basicColor;
		}
		colorList[0] = new Vector3d(1, 0.8, 0.7);
		colorList[1] = new Vector3d(0.7, 1, 0.8);
		colorList[2] = new Vector3d(0.6, 0.6, 1);
		if (selectedIndex >= 0){
			colorList[selectedIndex] = selectedColor;
		}
		viewer.setSkeletonColorList(colorList);
	}

	public MultiCharacterViewer getViewer() {
		return viewer;
	}

	public Vector3d[] getColorList() {
		return viewer.getSkeletonColorList();
	}
	
	public void setMotionDataList(MotionData[] motionDataList){
		int maxFrame = MotionData.maxFrame(motionDataList);
		setMotionDataList(motionDataList, maxFrame);
	}
	
	public void setMotionDataList(MotionData[] motionDataList, int maxFrame){
		setSelectedMotionColor(-1);
		MotionData.adjustKnot(motionDataList);
		setFPS(motionDataList[0].framerate);
		setMaxFrame(maxFrame);
		viewer.setMotionDataList(motionDataList);
	}

	
	public static void openMotionData(MotionData[] motionDataList, double scale){
		final Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(2, false));
		
		final MultiCharacterNavigator navigator = new MultiCharacterNavigator(shell);
		navigator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		navigator.viewer.setScale(scale);
		navigator.viewer.skeletonColor = new Vector3d(0.8, 0.8, 0.8);
		
		navigator.setMotionDataList(motionDataList);
		
		navigator.viewer.eye = new Vector3d(-121.80955051855364, 47.828390134752624, 29.85532196662258);
		navigator.viewer.center = new Vector3d(2.7325483582816688, 20.29166452107147, 11.701930660868829);
		navigator.viewer.upVector = new Vector3d(0.3243748441825436, 0.9452422181149468, 0.036029287457299365);
		
		shell.setText("Motion Navigator");
		shell.setSize(1400, 1000);
		shell.setMaximized(true);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
