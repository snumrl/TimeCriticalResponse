package mrl.motion.data.trasf;

import static mrl.motion.data.FootContactDetection.leftFootJoints;
import static mrl.motion.data.FootContactDetection.rightFootJoints;
import static mrl.util.MathUtil.getTranslation;
import static mrl.util.MathUtil.sub;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.SingularMatrixException;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.Joint;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.data.parser.BVHWriter;
import mrl.motion.graph.MotionSegment;
import mrl.util.MathUtil;
import mrl.util.Pair;
import mrl.util.Utils;

public class FootSlipCleanup {

	public static int MARGIN = 0;
	private SkeletonData skeleton;
	
	private Joint[] leftJoints;
	private Joint[] rightJoints;
	
	private ArrayList<Motion> motionList;

	private FootSlipCleanup(MotionData motionData){
		skeleton = motionData.skeletonData;
//		leftJoints = new Joint[leftFootJoints.length];
//		for (int i = 0; i < leftJoints.length; i++) {
//			leftJoints[i] = skeleton.get(leftFootJoints[i]);
//		}
//		rightJoints = new Joint[rightFootJoints.length];
//		for (int i = 0; i < rightJoints.length; i++) {
//			rightJoints[i] = skeleton.get(rightFootJoints[i]);
//		}
		if (leftFootJoints.length == 1) {
			leftJoints = new Joint[]{ skeleton.get(leftFootJoints[0]) };
			rightJoints = new Joint[]{ skeleton.get(rightFootJoints[0]) };
		} else {
			leftJoints = new Joint[]{ skeleton.get(leftFootJoints[0]), skeleton.get(leftFootJoints[2]) };
			rightJoints = new Joint[]{ skeleton.get(rightFootJoints[0]), skeleton.get(rightFootJoints[2]) };
		}
		
		
		motionList = motionData.motionList;
		cleanFootSlip(true);
		cleanFootSlip(false);
	}
	
	public static void clean(MotionData motionData){
		new FootSlipCleanup(motionData);
	}
	public static void clean(MotionSegment segment){
		clean(new MotionData(segment.getEntireMotion()));
	}
	
	public void cleanFootSlip(boolean isLeft){
		boolean[] contacts = getFootContact(motionList, isLeft);
		int sduration = 5;	
		int eduration = 5;

		int frameStart = 0;
		int mid = 0;

		for(int i=1; i<= motionList.size(); i++)
		{			
			boolean conBefore = contacts[i-1];
			boolean conCurrent = (i < motionList.size()) && contacts[i];
			
			if(conBefore && !conCurrent)
			{
				int mot_size = motionList.size();
				int frameEnd = i - 1;
				mid = (frameStart + frameEnd) / 2;
				int start = Math.min(mid, frameStart + MARGIN);
				int end = Math.max(mid, frameEnd - MARGIN);
				glue_foot(mid, start, end, isLeft);
				smooth_before(start, mid, Math.min(start, sduration), isLeft);
				smooth_after(end, mid, Math.min(mot_size-1 - end, eduration), isLeft);
				
//				if (frameStart < sduration) {
//					mid = (frameStart + i) / 2;
//					//mid = frameStart;
//					glue_foot(mid, 0, i, isLeft);
//					if (eduration + i <= mot_size) {
//						smooth_after(i, mid, eduration, isLeft);
//					}
//				} else if (i >= mot_size - eduration) {
//					mid = (frameStart + i) / 2;
//					glue_foot( mid, frameStart, mot_size, isLeft);
//					if (frameStart - sduration >= 0) {
//						smooth_before(frameStart, mid, sduration, isLeft);
//					}
//				} else {
//					mid = (frameStart + i) / 2;
//					glue_foot(mid, frameStart, i, isLeft);
//					smooth_before(frameStart, mid, sduration, isLeft);
//					smooth_after(i, mid, eduration, isLeft);
//				}
			}
			else if(!conBefore && conCurrent)
			{
				frameStart = i;
			}
		}
	}
	
	void smooth_before(int sP, int mid, int d, boolean isLeft)
	{
		Joint[] joints = isLeft ? leftJoints : rightJoints;
		Point3d[] positions = new Point3d[joints.length];
		
		HashMap<String, Point3d> jointPositions = Motion.getPointData(skeleton, motionList.get(mid));
		for (int i = 0; i < positions.length; i++) {
			positions[i] = jointPositions.get(joints[i].name);
		}

		for (int i=1; i<d; ++i) {
			Motion origin = motionList.get(sP-d+i);

			double t = 0.5*Math.cos(Math.PI*i/d)+0.5;

			Motion temp = new Motion(origin);
			for (int j = 0; j < joints.length; j++) {
				IkLimb(temp, joints[j], positions[j]);
			}
			Motion edited = Motion.interpolateMotion(temp, origin, t);
			motionList.set(sP-d+i, edited);
//			origin.interpolate(t, temp, origin);
		}
	}
	
	void smooth_after(int eP, int mid, int d, boolean isLeft)
	{
		Joint[] joints = isLeft ? leftJoints : rightJoints;
		Point3d[] positions = new Point3d[joints.length];
		
		HashMap<String, Point3d> jointPositions = Motion.getPointData(skeleton, motionList.get(mid));
		for (int i = 0; i < positions.length; i++) {
			positions[i] = jointPositions.get(joints[i].name);
		}
		
		for (int i=1; i<d; ++i) {
			Motion origin = motionList.get(eP+i-1);
			
			double t = 0.5 - 0.5*Math.cos(Math.PI*i/d);
			
			Motion temp = new Motion(origin);
			for (int j = 0; j < joints.length; j++) {
				IkLimb(temp, joints[j], positions[j]);
			}
			Motion edited = Motion.interpolateMotion(temp, origin, t);
			motionList.set(eP+i-1, edited);
//			origin.interpolate(t, temp, origin);
		}
	}
	
	void glue_foot(int mid, int s, int e, boolean isLeft) {
		Joint[] joints = isLeft ? leftJoints : rightJoints;
		Point3d[] positions = new Point3d[joints.length];
		
		HashMap<String, Point3d> jointPositions = Motion.getPointData(skeleton, motionList.get(mid));
		for (int i = 0; i < positions.length; i++) {
			positions[i] = jointPositions.get(joints[i].name);
		}
		
		for(int i = s; i < e; ++i) {
			for (int j = 0; j < joints.length; j++) {
				try{
					IkLimb(motionList.get(i), joints[j], positions[j]);
				} catch (RuntimeException ex){
					if (i > motionList.size() - 10) break;
					System.out.println("IK Fail :: " + Utils.toString(mid, i, j, joints[j].name));
					for (Pair<String, Matrix4d> entry : motionList.get(i).entrySet()){
						System.out.println(Utils.toString(entry.getKey(), entry.getValue()));
					}
					throw ex;
				}
			}
		}
	}
	
	private void alignAxis(Motion motion, Joint joint, Point3d pos){
		Joint lower = joint.parent;
		Joint upper = lower.parent;
		
		String base = upper.name;
		String target = joint.name;
		HashMap<String, Matrix4d> tData = Motion.getTransformData(skeleton, motion);
		
		Vector3d v = MathUtil.sub(pos, MathUtil.getTranslation(tData.get(base)));
		
		Matrix4d baseM = tData.get(base);
		Matrix4d targetM = tData.get(target);
		Vector3d currentV = sub(getTranslation(targetM), getTranslation(baseM));
		
		
		Vector3d cross = MathUtil.cross(currentV, v);
		if (cross.length() > 0.00001){
			cross.normalize();
			double angle = currentV.angle(v);
			AxisAngle4d rot = new AxisAngle4d(cross, angle);
			
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
	}
	
	
	void IkLimb(Motion motion, Joint joint, Point3d pos){
		if (leftJoints.length > 1 && (joint == Utils.last(leftJoints) || joint == Utils.last(rightJoints))){
			alignAxis(motion, joint, pos);
			return;
		}
		HashMap<String, Point3d> p = Motion.getPointData(skeleton, motion);
		Joint lower = joint.parent;
		Joint upper = lower.parent;

		Point3d B = p.get(joint.name);
		Point3d C = p.get(lower.name);
		Point3d A = p.get(upper.name);

		//error 처리
		if (B.distance(pos) < 0.0001)
			return;

		Vector3d L = MathUtil.sub(B, A);
		Vector3d N = MathUtil.sub(B, C);
		Vector3d M = MathUtil.sub(C, A);
		
		if (N.length() < 0.0001) return;

		double l = L.length();
		double n = N.length();
		double m = M.length();
		if (Math.abs(l + n - m) < 0.0001) return;

		double a = ACOS((l*l + n*n - m*m) / (2*l*n));
		double b = ACOS((l*l + m*m - n*n) / (2*l*m));

		Vector3d B_new = new Vector3d(pos);
		Vector3d L_new = MathUtil.sub(B_new, A);

		double l_ = L_new.length();

		double a_ = ACOS((l_*l_ + n*n - m*m) / (2*l_*n));
		double b_ = ACOS((l_*l_ + m*m - n*n) / (2*l_*m));

		//팔이 초기값때문에 반대로 꺾이는 경우가 생기면 이를 반대로 수정
//		if (reverse == true)
//		{
//			a_ *= -1.0;
//			b_ *= -1.0;
//		}

		Vector3d rotV = MathUtil.cross(M, L);
		
		if (rotV.length() > 0.00001){
			rotV.normalize();
			double rotb = b - b_;
			double rota = a_ - a - rotb;
			if (Double.isNaN(rotb)) {
				System.out.println("angle NAN :: " + Utils.toString(B_new, pos, A, l_, b, b_));
			}
			//평면상에서 회전해서 맞추기
			SetGlobalRotation(motion, upper, rotV, rotb);
//			if (joint == Utils.last(leftJoints) || joint == Utils.last(rightJoints)){
//			} else {
				SetGlobalRotation(motion, lower, rotV, rota);
//			}
		}

		////평면밖에서 회전해서 맞추기
		Vector3d rotV2 = MathUtil.cross(L, L_new);
		rotV2.normalize();

		double l_new = L_new.length();
		double l_diff = MathUtil.distance(L_new, L);

		double rot2 = ACOS((l_new * l_new + l * l - l_diff * l_diff) / (2.0 * l_new * l));
		SetGlobalRotation(motion, upper, rotV2, rot2);
	}
	
	private void dumpData(){
		
//		System.out.println("dump error motion to -> FootSlipCleanup_dump.bvh");
//		BVHWriter bw = new BVHWriter();
//		bw.write(new File("FootSlipCleanup_dump.bvh"), new MotionData(motionList));
	}
	
	private void SetGlobalRotation(Motion motion, Joint joint, Vector3d axis, double angle){
		if (Double.isNaN(angle)){
			System.out.println("Nan Angle!! ");
			System.out.println(axis + "," + angle + " , " + joint.name );
			dumpData();
			throw new RuntimeException();
		}
		if (Double.isNaN(axis.x)){
			System.out.println("Nan Axis!! ");
			System.out.println(axis + "," + angle + " , " + joint.name );
			dumpData();
			throw new RuntimeException();
		}
		HashMap<String, Matrix4d> tData = Motion.getTransformData(skeleton, motion);
		
		Matrix4d rot = new Matrix4d();
		rot.set(new AxisAngle4d(axis, angle));
		Matrix4d new_rot = new Matrix4d();
		new_rot.mul(rot, tData.get(joint.name));
		
		Matrix4d parent_global = tData.get(joint.parent.name);
		if (Double.isNaN(parent_global.m00) || Double.isNaN(new_rot.m00)){
			System.out.println("NAN :::: ");
			System.out.println(parent_global);
			System.out.println(new_rot);
			System.out.println(tData.get(joint.name));
			System.out.println(rot);
			System.out.println(axis + " :: " + angle);
			System.out.println("######   check motion #######");
			for (Pair<String, Matrix4d> entry : motion.entrySet()){
				System.out.println(Utils.toString(entry.getKey(), entry.getValue()));
			}
			System.out.println("#############");
			System.exit(0);
		}
		try{
			parent_global.invert();
		} catch (SingularMatrixException e){
			System.out.println(parent_global);
			throw e;
		}
		motion.get(joint.name).mul(parent_global, new_rot);
	}
	
	private double ACOS(double x){
		if (x > 1) return 0;
		if (x < -1) return Math.PI;
		return Math.acos(x);
	}
	
	
	private boolean[] getFootContact(ArrayList<Motion> motionList, boolean isLeft){
		boolean[] list = new boolean[motionList.size()];
		for (int i = 0; i < list.length; i++) {
			Motion m = motionList.get(i);
			list[i] = isLeft ? m.isLeftFootContact : m.isRightFootContact; 
		}
		return list;
	}
	
	
//	private static cleanJerkyPart(boolean[] list, int gap_size, int con_size){
//		int			i, j, k;
//		for( int step=0; step<2; step++)
//		{
//			bool isLeft = (step==0);
//			int footPart = (isLeft) ? PmHuman::LEFT_FOOT : PmHuman::RIGHT_FOOT;
//			int toePart = (isLeft) ? PmHuman::LEFT_TOE : PmHuman::RIGHT_TOE;
//	
//			//
//			//	remove constraints of short periods
//			//
//			int count = (isLeft) ? contact.at(0).first : contact.at(0).second;
//	
//			for( i=1; i<contact.size()-1; i++ )
//			{
//				bool conBefore = (isLeft) ? contact.at(i-1).first : contact.at(i-1).second;
//				bool conCurrent = (isLeft) ? contact.at(i).first : contact.at(i).second;
//	
//				if ( conCurrent )
//				{
//					count++;
//				}
//				else if ( conBefore && !conCurrent )
//				{
//					if ( count <= con_size )
//					{
//						for( k=1; k<=count; k++ )
//						{
//							if(isLeft)
//								contact.at(i-k).first = false;
//							else
//								contact.at(i-k).second = false;
//						}
//					}
//	
//					count = 0;
//				}
//			}
//	
//			//
//			//	fill small gaps
//			//
//			count = 0;
//			for( i=1; i<contact.size()-1; i++ )
//			{
//				bool conBefore = (isLeft) ? contact.at(i-1).first : contact.at(i-1).second;
//				bool conCurrent = (isLeft) ? contact.at(i).first : contact.at(i).second;
//	
//				if ( !conCurrent )
//				{
//					count++;
//				}
//				else if ( !conBefore && conCurrent )
//				{
//					if ( count <= gap_size )
//					{
//						for( k=1; k<=count; k++ )
//						{
//							if(isLeft)
//								contact.at(i-k).first = true;
//							else
//								contact.at(i-k).second = true;
//						}
//					}
//	
//					count = 0;
//				}
//			}
//		}
//	}
}
