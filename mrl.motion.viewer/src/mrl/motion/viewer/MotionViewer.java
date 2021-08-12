package mrl.motion.viewer;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData;
import mrl.util.MathUtil;

import org.eclipse.swt.widgets.Composite;


public class MotionViewer extends MotionAnimator{
	
	protected MotionData motionData;
	
	protected SkeletonData skeletonData;
	protected ArrayList<Motion> motionList;
	protected boolean drawShadow = true;
	
	protected boolean followCamera = false;
	protected LinkedList<Vector3d> cameraTracking = new LinkedList<Vector3d>();

	public MotionViewer(Composite parent) {
		super(parent);
	}
	
	public void setMotionData(MotionData motionData){
		this.motionData = motionData;
		this.skeletonData = motionData.skeletonData;
		this.motionList = motionData.motionList;
	}
	

	public MotionData getMotionData() {
		return motionData;
	}

	public void setDrawShadow(boolean drawShadow) {
		this.drawShadow = drawShadow;
	}
	
	public void setFollowCamera(boolean followCamera) {
		this.followCamera = followCamera;
	}

	@Override
	protected void drawObjectsImpl() {
		if (motionData == null) return;
		
		int mIndex = animationIndex;
		if (mIndex < 0) return;
		if (motionList != null){
			drawMotion(skeletonData, motionList.get(mIndex), drawShadow);
			
			if (followCamera){
				Vector3d rootP = MathUtil.getTranslation(motionList.get(mIndex).root());
				rootP.scale(scale);
				cameraTracking.add(rootP);
				if (cameraTracking.size() > 20){
					cameraTracking.removeFirst();
				}
				
				Vector3d mean = new Vector3d();
				for (Vector3d v : cameraTracking){
					mean.add(v);
				}
				mean.scale(1d/cameraTracking.size());
				
				
				
				Vector3d diff = new Vector3d();
				diff.sub(mean, center);
				diff.y = 0;
				center.add(diff);
				eye.add(diff);
			}
		}
		
		
	}

}
