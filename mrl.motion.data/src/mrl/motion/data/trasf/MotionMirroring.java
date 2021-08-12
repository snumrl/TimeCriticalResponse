package mrl.motion.data.trasf;

import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.util.MathUtil;
import mrl.util.Pair;

public class MotionMirroring {

//	// Basket
//	private static int[] weights = {
//		-1, -1, 1, 
//		1, 1, -1, 
//		-1, -1, 1
//	};
	// CMU
	private static int[] weights = {
		1, -1, -1, 
		-1, 1, 1, 
		1, -1, -1
	};
	
	
	public static MotionData mirrorMotion(MotionData mData){
		ArrayList<Motion> motionList = new ArrayList<Motion>();
		for (Motion m : mData.motionList){
			motionList.add(mirrorMotion(m));
		}
		return new MotionData(motionList);
	}
	
	public static Motion mirrorMotion(Motion m){
		Motion origin = m;
		m = new Motion(m);
		for (Pair<String, Matrix4d> pair : m.entrySet()){
			String key = pair.first;
			Matrix4d matrix = pair.second;
			if (key.equals("Hips")){
				mirrorRoot(matrix);
			}
			else if (key.startsWith("Left")){
				matrix.set(origin.get(key.replace("Left", "Right")));
				mirrorMatrix(matrix);
			} else if (key.startsWith("Right")){
				matrix.set(origin.get(key.replace("Right", "Left")));
				mirrorMatrix(matrix);
			} else {
				mirrorMatrix(matrix);
			}
		}
		return m;
	}
	
	public static MotionData mirrorMotion_NC(MotionData mData){
		ArrayList<Motion> motionList = new ArrayList<Motion>();
		for (Motion m : mData.motionList){
			motionList.add(mirrorMotion_NC(m));
		}
		return new MotionData(motionList);
	}
	
	public static Motion mirrorMotion_NC(Motion origin) {
		Motion mirrored = new Motion(origin);
		for (Pair<String, Matrix4d> entry : mirrored.entrySet()) {
			String key = entry.getKey();
			boolean isRotated = key.contains("UpLeg") || key.contains("Shoulder");
			Matrix4d m = null;
			if (key.contains("Left")) {
				m = origin.get(key.replace("Left", "Right"));
			} else if (key.contains("Right")) {
				m = origin.get(key.replace("Right", "Left"));
			} else {
				m = origin.get(key);
				Quat4d q = new Quat4d();
				q.set(m);
				q.x *= -1;
				q.y *= -1;
				q.z *= 1;
				Matrix4d m2 = new Matrix4d();
				m2.set(q);
				m = m2;
			}
			if (isRotated) {
				Matrix4d rot = new Matrix4d();
				rot.rotZ(Math.PI);
				rot.mul(rot, m);
				m = rot;
			}
			entry.getValue().set(m);
		}
		Vector3d t = MathUtil.getTranslation(origin.root());
		t.x *= 1;
		t.y *= 1;
		t.z *= -1;
		mirrored.root().setTranslation(t);
		return mirrored;
	}
	
	private static void mirrorRoot(Matrix4d m){
		Quat4d q = new Quat4d();
		q.set(m);
		q.x *= weights[0];
		q.y *= weights[1];
		q.z *= weights[2];
		Matrix4d m2 = new Matrix4d();
		m2.set(q);
		
		Vector3d t = MathUtil.getTranslation(m);
		t.x *= weights[3];
		t.y *= weights[4];
		t.z *= weights[5];
		m2.setTranslation(t);
		
		m.set(m2);
	}
	
	private static void mirrorMatrix(Matrix4d m){
		
		
		Quat4d q = new Quat4d();
		q.set(m);
		q.x *= weights[6];
		q.y *= weights[7];
		q.z *= weights[8];
		Matrix4d m2 = new Matrix4d();
		m2.set(q);
		
		Vector3d t = MathUtil.getTranslation(m);
		m2.setTranslation(t);
		
		m.set(m2);
//		point.z =  m20*point.x + m21*point.y + m22*point.z + m23;
//		m.m20 *= -1;
//		m.m21 *= -1;
//		m.m22 *= -1;
//		m.m23 *= -1;
	}
	
	
}
