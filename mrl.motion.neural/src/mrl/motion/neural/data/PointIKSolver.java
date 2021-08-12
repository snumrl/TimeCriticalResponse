package mrl.motion.neural.data;

import static mrl.util.MathUtil.getTranslation;
import static mrl.util.MathUtil.sub;

import java.util.HashMap;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.Joint;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;

public class PointIKSolver {
	
	public static int Z_AXIS_OFFSET = -1;
	private static final double eps = 0.000001;
	public static String[] AngleJointList = {
		"Head",
		"Hips",
		"LeftArm",
		"LeftFoot",
		"LeftForeArm",
		"LeftHand",
		"LeftLeg",
		"LeftShoulder",
		"LeftToe",
		"LeftUpLeg",
		"Neck",
		"RightArm",
		"RightFoot",
		"RightForeArm",
		"RightHand",
		"RightLeg",
		"RightShoulder",
		"RightToe",
		"RightUpLeg",
		"Spine",
		"Spine1",
		"Spine2",
	};
	
//	KeyJointList = new String[]{
//			"Head_End",
//			"LeftHand",
//			"LeftFoot",
//			"LeftToe",
//			"RightHand",
//			"RightFoot",
//			"RightToe",
//			
//			"LeftArm",
//			"RightArm",
//			
//			"LeftForeArm",
//			"LeftLeg",
//			"RightForeArm",
//			"RightLeg",
//			
//			// added
//			"Spine",
//			"LeftHand_End",
//			"RightHand_End",
//			"Neck",
//			"LeftUpLeg",
//			"RightUpLeg",
//		};
	
	private SkeletonData skeleton;
	private Motion sample;
	
	private Motion motion;
	private HashMap<String, Point3d> map;

	public PointIKSolver(SkeletonData skeleton, Motion sample) {
		this.skeleton = skeleton;
		this.sample = sample;
	}
	
	int count = 0;
	boolean print;
	public Motion solve(HashMap<String, Point3d> map, Pose2d p){
		this.map = map;
		motion = new Motion(sample);
		
		double rootHeight = map.get("Hips").y;
		
		alignAxis("Hips", "LeftUpLeg", vec("Hips", "LeftUpLeg", "RightUpLeg"));
		rotateAxis("Hips", "Spine", null, null);
		motion.root().setTranslation(new Vector3d(0, rootHeight, 0));
		
		alignAxis("Spine", "Neck", null);
		rotateAxis("Neck", "LeftArm", vec("Neck", "LeftArm", "RightArm"), null);
		alignAxis("Head", "Head_End", null);
		
		alignArm("Left");
//		if (count == 62) print = true;
//		print = true;
		alignArm("Right");
//		print = false;
		
		alignLeg("Left");
		alignLeg("Right");
		
		
		Matrix4d globalM = Pose2d.globalTransform(Pose2d.BASE, p).to3d();
		motion.root().mul(globalM, motion.root());
		Vector3d rootTranslation = new Vector3d(Pose2d.to3d(p.position));
		rootTranslation.y = rootHeight;
		motion.root().setTranslation(rootTranslation);
		count++;
		return motion;
	}
	
	public Motion solveFoot(Motion base, HashMap<String, Point3d> map){
		this.map = map;
		motion = PositionMotion.getAlignedMotion(base);
		alignLegWithoutRotation("Left");
		alignLegWithoutRotation("Right");
		motion.root().set(base.root());
		count++;
		return motion;
	}
	
	private void alignArm(String prefix){
		alignAxis(prefix + "Shoulder", prefix + "Arm", null);
		updateJoint(prefix + "Arm");
		bendArm(prefix + "ForeArm", prefix + "Hand", vec(prefix + "Arm", prefix + "Hand"));
		alignAxis(prefix + "Arm", prefix + "Hand", null);
		rotateAxis(prefix + "Arm", prefix + "ForeArm", null, null);
		
		updateJoint(prefix + "Hand");
		alignAxis(prefix + "Hand", prefix + "Hand_End", null);
	}
	private void alignLeg(String prefix){
		updateJoint(prefix + "UpLeg");
		bendArm(prefix + "Leg", prefix + "Foot", vec(prefix + "UpLeg", prefix + "Foot"));
		alignAxis(prefix + "UpLeg", prefix + "Foot", null);
		rotateAxis(prefix + "UpLeg", prefix + "Leg", null, null);
		
		updateJoint(prefix + "Foot");
		alignAxis(prefix + "Foot", prefix + "Toe_End", null);
	}
	
	private void alignLegWithoutRotation(String prefix){
		if (map.get(prefix + "Foot").y > 50) return;
		updateJoint(prefix + "UpLeg");
		bendArm(prefix + "Leg", prefix + "Foot", vec(prefix + "UpLeg", prefix + "Foot"));
		alignAxis(prefix + "UpLeg", prefix + "Foot", null);
//		rotateAxis(prefix + "UpLeg", prefix + "Leg", null, null);
		
		updateJoint(prefix + "Foot");
//		alignAxis(prefix + "Foot", prefix + "Toe_End", null);
	}
	
	private Vector3d vec(String j1, String j2, String j3){
		Vector3d v = vec(j1, j2);
		v.add(vec(j3, j1));
		v.scale(0.5);
		return v;
	}
	private Vector3d vec(String j1, String j2){
		Point3d p1 = map.get(j1);
		Point3d p2 = map.get(j2);
		if (p1 == null) p1 = currentP(j1);
		if (p2 == null) p2 = currentP(j2);
		return sub(p2, p1);
	}
	
	private void updateJoint(String joint){
		map.put(joint, currentP(joint));
	}
	
	private Point3d currentP(String joint){
		return Motion.getPointData(skeleton, motion).get(joint);
	}
	
	private void bendArm(String j1, String j2, Vector3d v){
		double l1 = skeleton.get(j1).length;
		double l2 = skeleton.get(j2).length;
		double L = v.length();
		double angle = ACOS((l1*l1 + l2*l2 - L*L)/(2*l1*l2));
		if (print){
			System.out.println("angle :: " + count + " : " + angle + " : " + Math.toDegrees(angle));
		}
		AxisAngle4d rotZ = new AxisAngle4d(MotionTransform.TRANSFORM_Z_AXIS, Z_AXIS_OFFSET*(Math.PI - angle));
		motion.get(j1).set(rotZ);
//		motion.get(j1).rotZ(Z_AXIS_OFFSET*(Math.PI - angle));
	}
	
	private double ACOS(double x){
		if (x > 1) return 0;
		if (x < -1) return Math.PI;
		return Math.acos(x);
	}
	
	private void rotateAxis(String base, String target, Vector3d v, Vector3d axis){
		if (v == null) v = vec(base, target);
		if (axis == null) axis = prevAxis;
		HashMap<String, Matrix4d> tData = Motion.getTransformData(skeleton, motion);
		
		Matrix4d baseM = tData.get(base);
		Matrix4d targetM = tData.get(target);
		Vector3d currentV = sub(getTranslation(targetM), getTranslation(baseM));
		
//		axis = new Vector3d(axis);
//		axis.normalize();
//		Vector3d axisY = MathUtil.cross(axis, v);
//		axisY.normalize();
//		Vector3d axisX = MathUtil.cross(axisY, axis);
//		axisX.normalize();
//		
//		Vector2d v1 = to2d(v, axisX, axisY);
//		Vector2d v2 = to2d(currentV, axisX, axisY);
//		double angle = MathUtil.directionalAngle(v2, v1);
//		AxisAngle4d rot = new AxisAngle4d(axis, angle);
		
		axis = new Vector3d(axis);
		axis.normalize();
		Vector3d v1 = MathUtil.cross(axis, v);
		Vector3d v2 = MathUtil.cross(axis, currentV);
		if (print){
			System.out.println("vv : " + count + " : " + v1.length() + " , " + v2.length());
		}
		if (v1.length() < 0.5) return;
		if (v2.length() < eps) return;
		Vector3d ax = MathUtil.cross(v1,  v2);
		ax.normalize();
		double angle = v1.angle(v2);
		AxisAngle4d rot = new AxisAngle4d(ax, -angle);
		
		
		Matrix4d rotM = new Matrix4d();
		rotM.set(rot);
		rotM.mul(rotM, baseM);
		Joint bJoint = skeleton.get(base);
		if (bJoint.parent != null){
			Matrix4d pp = new Matrix4d(tData.get(bJoint.parent.name));
			pp.invert();
			motion.get(base).mul(pp, rotM);
		} else {
			motion.get(base).set(rotM);
		}
	}
	
	private Vector3d prevAxis;
	private void alignAxis(String base, String target, Vector3d v){
		if (v == null) v = vec(base, target);
		HashMap<String, Matrix4d> tData = Motion.getTransformData(skeleton, motion);
		
		Matrix4d baseM = tData.get(base);
		Matrix4d targetM = tData.get(target);
		Vector3d currentV = sub(getTranslation(targetM), getTranslation(baseM));
		
		
		Vector3d cross = MathUtil.cross(currentV, v);
		if (cross.length() > eps){
			cross.normalize();
			double angle = currentV.angle(v);
			AxisAngle4d rot = new AxisAngle4d(cross, angle);
			
			Matrix4d rotM = new Matrix4d();
			rotM.set(rot);
			
			rotM.mul(rotM, baseM);
			
			Point3d origin = currentP(target);
			Joint bJoint = skeleton.get(base);
			if (bJoint.parent != null){
				Matrix4d pp = new Matrix4d(tData.get(bJoint.parent.name));
				pp.invert();
				motion.get(base).mul(pp, rotM);
			} else {
				motion.get(base).set(rotM);
			}
		}
		prevAxis = v;
	}
}
