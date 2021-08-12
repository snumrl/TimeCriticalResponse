package mrl.motion.viewer.kinect;

import static mrl.motion.viewer.kinect.KinectJNI.JointType_AnkleLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_AnkleRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_ElbowLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_ElbowRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_FootLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_FootRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_HandLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_HandRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_HandTipLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_HandTipRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_Head;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_HipLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_HipRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_KneeLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_KneeRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_Neck;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_ShoulderLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_ShoulderRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_SpineBase;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_SpineMid;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_SpineShoulder;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_ThumbLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_ThumbRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_WristLeft;
import static mrl.motion.viewer.kinect.KinectJNI.*;

import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.opengl.util.GLUT;

import mrl.motion.viewer.SWTViewableCanvas;
import mrl.motion.viewer.kinect.KinectJNI.KinectJoints;
import mrl.motion.viewer.module.ItemDrawer;
import mrl.widget.app.Item;
import mrl.widget.app.Item.ItemDescription;

public class KinectJointItemDrawer extends ItemDrawer {
	
	private KinectJoints joints;
	private boolean checkColor = false;

	@Override
	public boolean isDrawable(Object data) {
		return data instanceof KinectJoints;
	}

	@Override
	public void draw(GL gl, GLU glu, GLUT glut, ItemDescription desc, Object data, int timeIndex) {
		joints = (KinectJoints)data;
		
		DrawBone(gl, glut, JointType_Head, JointType_Neck);
	    DrawBone(gl, glut, JointType_Neck, JointType_SpineShoulder);
	    DrawBone(gl, glut, JointType_SpineShoulder, JointType_SpineMid);
	    DrawBone(gl, glut, JointType_SpineMid, JointType_SpineBase);
	    DrawBone(gl, glut, JointType_SpineShoulder, JointType_ShoulderRight);
	    DrawBone(gl, glut, JointType_SpineShoulder, JointType_ShoulderLeft);
	    DrawBone(gl, glut, JointType_SpineBase, JointType_HipRight);
	    DrawBone(gl, glut, JointType_SpineBase, JointType_HipLeft);
	    
	    // Right Arm    
	    DrawBone(gl, glut, JointType_ShoulderRight, JointType_ElbowRight);
	    DrawBone(gl, glut, JointType_ElbowRight, JointType_WristRight);
	    DrawBone(gl, glut, JointType_WristRight, JointType_HandRight);
	    DrawBone(gl, glut, JointType_HandRight, JointType_HandTipRight);
	    DrawBone(gl, glut, JointType_WristRight, JointType_ThumbRight);

	    // Left Arm
	    DrawBone(gl, glut, JointType_ShoulderLeft, JointType_ElbowLeft);
	    DrawBone(gl, glut, JointType_ElbowLeft, JointType_WristLeft);
	    DrawBone(gl, glut, JointType_WristLeft, JointType_HandLeft);
	    DrawBone(gl, glut, JointType_HandLeft, JointType_HandTipLeft);
	    DrawBone(gl, glut, JointType_WristLeft, JointType_ThumbLeft);

	    // Right Leg
	    DrawBone(gl, glut, JointType_HipRight, JointType_KneeRight);
	    DrawBone(gl, glut, JointType_KneeRight, JointType_AnkleRight);
	    DrawBone(gl, glut, JointType_AnkleRight, JointType_FootRight);

	    // Left Leg
	    DrawBone(gl, glut, JointType_HipLeft, JointType_KneeLeft);
	    DrawBone(gl, glut, JointType_KneeLeft, JointType_AnkleLeft);
	    DrawBone(gl, glut, JointType_AnkleLeft, JointType_FootLeft);
	    
		
		for (int i = 0; i < joints.jointPositions.size(); i++) {
			int state = joints.jointStatus.get(i);
			if (state == TrackingState_NotTracked) continue;
			
			Vector3d color = desc.getColor(new Vector3d(0, 1, 0));
			double thickness = desc.getSize(3);
			if (state == TrackingState_Inferred) {
				color = desc.getColor(new Vector3d(1, 1, 0));
			}
			if ((i == JointType_HandLeft && joints.leftHandState == HandState_Closed)
					|| (i == JointType_HandRight && joints.rightHandState == HandState_Closed)){
				color = desc.getColor(new Vector3d(1, 0, 0));
				thickness = 6;
			}
			
			gl.glColor3d(color.x, color.y, color.z);
			SWTViewableCanvas.drawSphere(gl, glut, joints.jointPositions.get(i), thickness);
			
			
		}
		
		
	}

	private void DrawBone(GL gl,GLUT glut, int j0, int j1) {
		ArrayList<Point3d> points = joints.jointPositions;
		ArrayList<Integer> status = joints.jointStatus;
		Point3d p0 = points.get(j0);
		Point3d p1 = points.get(j1);
		int joint0State = status.get(j0);
		int joint1State = status.get(j1);
		
		// If we can't find either of these joints, exit
	    if ((joint0State == TrackingState_NotTracked) || (joint1State == TrackingState_NotTracked))
	    {
	        return;
	    }

	    // Don't draw if both points are inferred
	    if ((joint0State == TrackingState_Inferred) && (joint1State == TrackingState_Inferred))
	    {
	        return;
	    }

	    double thickness;
	    Vector3d color;
	    // We assume all drawn bones are inferred unless BOTH joints are tracked
	    if ((joint0State == TrackingState_Tracked) && (joint1State == TrackingState_Tracked))
	    {
	    	thickness = 2;
	    	color = new Vector3d(0, 1, 0);
	    }
	    else
	    {
	    	thickness = 0.5;
	    	color = new Vector3d(0.5, 0.5, 0.5);
	    }
	    if (checkColor){
	    	color = new Vector3d(1, 1, 0);
	    	thickness = 2;
	    	checkColor = false;
	    }
	    gl.glColor3d(color.x, color.y, color.z);
		SWTViewableCanvas.drawLine(gl, glut, p0, p1, thickness);
	}

}
