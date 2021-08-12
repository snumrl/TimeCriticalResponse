package mrl.motion.viewer;

import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;

import org.eclipse.swt.widgets.Composite;

public class MultiCharacterViewer extends MotionAnimator{
	
	private MotionData[] motionDataList = new MotionData[0];
	private boolean[] isKnotApplied = new boolean[0];
	private Vector3d[] skeletonColorList;
	private Vector3d[] headColorList;
	private boolean drawShadow = false;

	public MultiCharacterViewer(Composite parent) {
		super(parent);
		
		setScale(0.25);
		eye = new Vector3d(182.31335839527955, 85.61673298999482, 55.11714656896301);
		center = new Vector3d(1.5774415015064278, 18.182604593713805, -2.503299201380486);
		upVector = new Vector3d(-0.3500979290029628, 0.9173977024123974, -0.18924295420513096);
	}
	
	public MotionData[] getMotionDataList() {
		return motionDataList;
	}

	public void setMotionDataList(MotionData[] motionDataList) {
		this.motionDataList = motionDataList;
		
		isKnotApplied = new boolean[motionDataList.length];
		for (int i = 0; i < motionDataList.length; i++) {
			for (Motion m : motionDataList[i].motionList) {
				if (m != null){
					isKnotApplied[i] = !Double.isNaN(m.knot);
					break;
				}
			}
		}
	}
	
	public void setSkeletonColorList(Vector3d[] skeletonColorList) {
		this.skeletonColorList = skeletonColorList;
	}
	
	public Vector3d[] getSkeletonColorList() {
		return skeletonColorList;
	}

	public Vector3d[] getHeadColorList() {
		return headColorList;
	}

	public void setHeadColorList(Vector3d[] headColorList) {
		this.headColorList = headColorList;
	}

	public void setDrawShadow(boolean drawShadow) {
		this.drawShadow = drawShadow;
	}

	@Override
	protected void drawObjectsImpl() {
		if (drawShadow){
			setupShadow();
			for (int i = 0; i < motionDataList.length; i++) {
				MotionData motionData = motionDataList[i];
				if (motionData.motionList.size() > 0){
					int index = -1;
					if (isKnotApplied[i]){
						index = MotionData.getFrameIndex(motionData.motionList, animationIndex, -1);
					} else {
						if (animationIndex < motionData.motionList.size()){
							index = animationIndex;
						}
					}
					if (index >= 0){
						if (drawBox){
							drawMotionByBox(motionData.skeletonData, motionData.motionList.get(index));
						} else {
							drawBone(motionData.skeletonData.root, motionData.motionList.get(index), true);
						}
					}
				}
			}
			unsetupShadow();
		}
		
		for (int i = 0; i < motionDataList.length; i++) {
			MotionData motionData = motionDataList[i];
			if (skeletonColorList != null && i < skeletonColorList.length){
				skeletonColor = skeletonColorList[i];
			}
			if (headColorList != null && i < headColorList.length){
				headColor = headColorList[i];
			} else {
				headColor = null;
			}
			
			if (motionData.motionList.size() > 0){
				if (isKnotApplied[i]){
					int index = MotionData.getFrameIndex(motionData.motionList, animationIndex, -1);
					if (index >= 0){
						drawMotion(motionData.skeletonData, motionData.motionList.get(index), false);
					}
				} else {
					if (animationIndex < motionData.motionList.size()){
						drawMotion(motionData.skeletonData, motionData.motionList.get(animationIndex), false);
					}
				}
			}
		}
	}
	

	

}
