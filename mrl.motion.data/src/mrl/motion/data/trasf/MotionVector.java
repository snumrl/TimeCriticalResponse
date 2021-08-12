package mrl.motion.data.trasf;

import static mrl.util.MathUtil.distance;
import static mrl.util.MathUtil.getTranslation;
import static mrl.util.MathUtil.quat;
import static mrl.util.MathUtil.sub;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.util.Configuration;
import mrl.util.MathUtil;


public class MotionVector {
	
	public Vector3d rootTranslation;
	public Quat4d rootOrientation;
	public Quat4d[] jointRotations;
	
	public double offset;
	
	public boolean isLeftFootContact;
	public boolean isRightFootContact;
	
	public Point3d leftFootPosition;
	public Point3d rightFootPosition;
	
	public MotionVector(){
		
	}
	
	public static boolean isFootContactEqual(MotionVector v1, MotionVector v2){
		return (v1.isLeftFootContact == v2.isLeftFootContact) && (v1.isRightFootContact == v2.isRightFootContact); 
	}
	
	public static double getMotionDistanceOnly(MotionVector v1, MotionVector v2, MotionTransform t, double dSum, double weightLimit){
		if (dSum > weightLimit) return dSum;
		if (v2 == null){
			System.out.println("v1 null!!!");
			System.out.flush();
			System.exit(0);
		}
		if (v2.rootTranslation == null){
			System.out.println("v1.rootTranslation null!!!");
			System.out.flush();
			System.exit(0);
		}
		dSum += square((v1.rootTranslation.y - v2.rootTranslation.y) * t.posToOriRatio, t.rootPosWeight);
		if (dSum > weightLimit) return dSum;
		dSum += square(quatDistance(v1.rootOrientation, v2.rootOrientation), t.rootOriWeight);
		if (dSum > weightLimit) return dSum;
		for (int i = 0; i < v1.jointRotations.length; i++) {
			dSum += square(quatDistance(v1.jointRotations[i], v2.jointRotations[i]), t.weightList[i]);
			if (dSum > weightLimit) return dSum;
		}
		return dSum;
	}
	public static double getMotionDistance(MotionVector v1, MotionVector v2, MotionTransform t, double dSum, double weightLimit){
		if (!isFootContactEqual(v1, v2)) return Integer.MAX_VALUE;
		
		if (v1.leftFootPosition.y < t.footYLimit || v2.leftFootPosition.y < t.footYLimit){
			dSum += square(v1.leftFootPosition.distance(v2.leftFootPosition) * t.posToOriRatio, t.rootPosWeight*2);
		}
		if (v1.rightFootPosition.y < t.footYLimit || v2.rightFootPosition.y < t.footYLimit){
			dSum += square(v1.rightFootPosition.distance(v2.rightFootPosition) * t.posToOriRatio, t.rootPosWeight*2);
		}
		
		if (dSum > weightLimit) return dSum;
		dSum += square((v1.rootTranslation.y - v2.rootTranslation.y) * t.posToOriRatio, t.rootPosWeight);
		if (dSum > weightLimit) return dSum;
		dSum += square(quatDistance(v1.rootOrientation, v2.rootOrientation), t.rootOriWeight);
		if (dSum > weightLimit) return dSum;
		for (int i = 0; i < v1.jointRotations.length; i++) {
			dSum += square(quatDistance(v1.jointRotations[i], v2.jointRotations[i]), t.weightList[i]);
			if (dSum > weightLimit) return dSum;
		}
		return dSum;
	}
	
	public static double getVectorDistance(MotionVector v1, MotionVector v2, MotionTransform t, double dSum, double weightLimit){
		double velocityWeight = t.frameRate*Configuration.MGRAPH_VELOCITY_WEIGHT;
		if (dSum > weightLimit) return dSum;
		
		dSum += square(v1.leftFootPosition.distance(v2.leftFootPosition) * t.posToOriRatio, t.rootPosWeight*velocityWeight);
		dSum += square(v1.rightFootPosition.distance(v2.rightFootPosition) * t.posToOriRatio, t.rootPosWeight*velocityWeight);
		
		if (dSum > weightLimit) return dSum;
		dSum += square(distance(v1.rootTranslation, v2.rootTranslation) * t.posToOriRatio, t.rootPosWeight*velocityWeight*2);
		if (dSum > weightLimit) return dSum;
		dSum += square(quatDistance(v1.rootOrientation, v2.rootOrientation), t.rootOriWeight*velocityWeight);
		if (dSum > weightLimit) return dSum;
		for (int i = 0; i < v1.jointRotations.length; i++) {
			dSum += square(quatDistance(v1.jointRotations[i], v2.jointRotations[i]), t.weightList[i]*velocityWeight);
			if (dSum > weightLimit) return dSum;
		}
		return dSum;
	}
	
//	public static double getMotionFootDistance(MotionVector v1, MotionVector v2, MotionTransform t){
//		if (!isFootContactEqual(v1, v2)) return Integer.MAX_VALUE;
//		double dSum = 0;
//		if (v1.isLeftFootContact){
//			dSum += square(v1.leftFootPosition.distance(v2.leftFootPosition) * t.posToOriRatio, t.rootWeight*3);
//		}
//		if (v1.isRightFootContact){
//			dSum += square(v1.rightFootPosition.distance(v2.rightFootPosition) * t.posToOriRatio, t.rootWeight*3);
//		}
//		return dSum;
//	}
	
//	public static double getMotionDistanceUnlimit(MotionVector v1, MotionVector v2, MotionTransform t){
//		double dSum = 0;
//		dSum += square((v1.rootTranslation.y - v2.rootTranslation.y)* t.posToOriRatio, t.rootWeight);
//		dSum += square(getRootOriDistance(v1, v2), t.rootWeight);
//		for (int i = 0; i < v1.jointRotations.length; i++) {
//			dSum += square(quatDistance(v1.jointRotations[i], v2.jointRotations[i]), t.weightList[i]);
//		}
//		return dSum;
//	}
	
//	public static double getVectorDistanceUnlimit(MotionVector v1, MotionVector v2, MotionTransform t){
//		double dSum = 0;
//		dSum += square((v1.rootTranslation.y - v2.rootTranslation.y)* t.posToOriRatio, t.rootWeight);
//		dSum += square(quatDistance(v1.rootOrientation, v2.rootOrientation), t.rootWeight);
//		for (int i = 0; i < v1.jointRotations.length; i++) {
//			dSum += square(quatDistance(v1.jointRotations[i], v2.jointRotations[i]), t.weightList[i]);
//		}
//		return dSum;
//	}
	
	public static double square(double d, double weight){
		return d*d * weight;
	}
	
	public static MotionVector getMotionVelocity(Motion m1, Motion m2, MotionVector v1, MotionVector v2){
		MotionVector vector = new MotionVector();
		
		
		Matrix4d t = MotionTransform.getAlignTransform(m1);
		Matrix4d r1 = new Matrix4d(m1.root());
		Matrix4d r2 = new Matrix4d(m2.root());
		r1.mul(t, r1);
		r2.mul(t, r2);
		
		
		vector.rootTranslation = sub(getTranslation(r2), getTranslation(r1));
		vector.rootOrientation = quatDiff(quat(r1), quat(r2));
		
		vector.leftFootPosition = new Point3d();
		vector.leftFootPosition.sub(v2.leftFootPosition, v1.leftFootPosition);
		vector.rightFootPosition = new Point3d();
		vector.rightFootPosition.sub(v2.rightFootPosition, v1.rightFootPosition);
		
		vector.jointRotations = new Quat4d[v1.jointRotations.length];
		for (int i = 0; i < v1.jointRotations.length; i++) {
			vector.jointRotations[i] = quatDiff(v1.jointRotations[i], v2.jointRotations[i]);
		}
		return vector;
	}
	
	public static double quatDistance2(Quat4d q1, Quat4d q2){
		Quat4d _q = new Quat4d();
		_q.mulInverse(q1, q2);
		double angle = MathUtil.getQuatAngle(_q);
		if (angle > Math.PI){
			angle = 2*Math.PI - angle;
		}
		return angle;
	}
	public static Quat4d quatDiff2(Quat4d q1, Quat4d q2){
		Quat4d q = new Quat4d();
		q.mulInverse(q2, q1);
		
		double angle = MathUtil.getQuatAngle(q);
		if (angle > Math.PI){
			Quat4d _q = new Quat4d();
			_q.scale(-1, q2);
			q.mulInverse(_q, q1);
		}
		return q;
	}
	
	
	public static double quatDistance(Quat4d q1, Quat4d q2){
		Quat4d qi = new Quat4d(q1);
		qi.inverse();
		Quat4d _q = new Quat4d(); 
		_q.mul(qi, q2);
		
		double angle = MathUtil.getQuatAngle(_q);
		if (angle > Math.PI){
			angle = 2*Math.PI - angle;
		}
		return angle;
	}
	
	public static Quat4d quatDiff(Quat4d q1, Quat4d q2){
		Quat4d qi = new Quat4d(q1);
		qi.inverse();
		Quat4d q = new Quat4d(); 
		q.mul(qi, q2);
		
		double angle = MathUtil.getQuatAngle(q);
		if (angle > Math.PI){
			Quat4d _q = new Quat4d();
			_q.scale(-1, q2);
			q.mul(qi, _q);
		}
		return q;
	}
	
	static double dot(Quat4d q1, Quat4d q2){
		return q1.w * q2.w + q1.x * q2.x + q1.y * q2.y + q1.z * q2.z; 
	}
	
}
