package mrl.motion.viewer.module;

import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.opengl.util.GLUT;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.MotionSelection;
import mrl.motion.viewer.MotionAnimator;
import mrl.motion.viewer.SWTViewableCanvas;
import mrl.util.Utils;
import mrl.widget.app.Item;
import mrl.widget.app.Item.ItemDescription;

public class MotionItemDrawer extends ItemDrawer{
	
	public static double BoneThickness = 0.65*4;
	public static boolean drawFootContact = false;

	@Override
	public boolean isDrawable(Object data) {
		return (data instanceof MotionData);
	}

	@Override
	public void draw(GL gl, GLU glu, GLUT glut, ItemDescription desc, Object data, int timeIndex) {
		MotionData mData = (MotionData)data;
		if (timeIndex < 0 || timeIndex >= mData.motionList.size()) return;
		
		Motion motion = mData.motionList.get(timeIndex);
		if (motion == null) return;
		if (Double.isNaN(motion.knot)) {
			MotionSelection.instance().setSingleMotion(motion);
		} else {
			MotionSelection.instance().addKnotMotion(motion);
		}
		
		MotionData.selectedData = new MotionData(mData.skeletonData, Utils.singleList(motion));
		Vector3d skeletonColor = desc.getColor();
		gl.glColor4d(skeletonColor.x, skeletonColor.y, skeletonColor.z, 1);
		MotionAnimator.drawBone(gl, glut, BoneThickness, mData.skeletonData.root, motion, true);
		
		if (drawFootContact){
			MotionAnimator.drawFootContact(gl, glut, mData.skeletonData, motion, BoneThickness);
			
			gl.glColor3d(0.2, 0.8, 0.2);
			HashMap<String, Point3d> pointData = Motion.getPointData(mData.skeletonData, motion);
			if (motion.ballContact.left){
				Point3d p = pointData.get("LeftHand_End");
				SWTViewableCanvas.drawSphere(gl, glut, p, BoneThickness + 2);
			}
			if (motion.ballContact.right){
				Point3d p = pointData.get("RightHand_End");
				SWTViewableCanvas.drawSphere(gl, glut, p, BoneThickness + 2);
			}
		}
	}

	public int getTimeLength(Object data){
		return ((MotionData)data).motionList.size();
	}
	
	public String toString(Item item, Object data, int timeIndex){
		ArrayList<Motion> list = ((MotionData)data).motionList;
		if (timeIndex >= 0 && timeIndex < list.size()) {
			return list.get(timeIndex).toString();
		} else {
			return null;
		}
	}
}
