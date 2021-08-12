package mrl.motion.data;

import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector2d;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;

public class SequentialMotion {

	public Motion motion;
	public double rotation;
	public Vector2d translation;
	

	public SequentialMotion(Motion motion, double rotation, Vector2d translation) {
		this.motion = motion;
		this.rotation = rotation;
		this.translation = translation;
	}
	
	public Matrix2d transform2d(){
		return new Matrix2d(rotation, translation);
	}

	public static ArrayList<SequentialMotion> getSequence(ArrayList<Motion> mList){
		Pose2d[] poseList = new Pose2d[mList.size()];
		for (int i = 0; i < poseList.length; i++) {
			poseList[i] = PositionMotion.getPose(mList.get(i));
		}
		ArrayList<SequentialMotion> motionList = new ArrayList<SequentialMotion>();
//		double max = 0;
//		double sum = 0;
		for (int i = 0; i < poseList.length; i++) {
			Matrix4d transform = Pose2d.globalTransform(poseList[i], Pose2d.BASE).to3d();
			Motion motion = new Motion(mList.get(i));
			motion.root().mul(transform, motion.root());
			
			Pose2d p = poseList[i];
			Pose2d prevP = (i == 0) ? p : poseList[i-1];
			Matrix2d t = Pose2d.localTransform(prevP, p);
			
			double rotation = t.getAngle();
			Vector2d translation = t.getTranslation();
			
//			double v = Math.toDegrees(Math.abs(rotation));
//			double v = translation.length();
//			System.out.println("tLen : " + i + " : " + v);
//			max = Math.max(max, v);
//			sum += v;
			motionList.add(new SequentialMotion(motion, rotation, translation));
		}
//		System.out.println("## max : " + max + " :  mean : " + sum/(poseList.length-1));
		return motionList;
	}
	
	public static ArrayList<Motion> generateMotion(ArrayList<SequentialMotion> sequence){
		ArrayList<Motion> motionList = new ArrayList<Motion>();
		Matrix4d transform = null;
		
		for (SequentialMotion s : sequence){
			if (transform == null){
				transform = new Matrix4d();
				transform.setIdentity();
			} else {
				transform.mul(transform, s.transform2d().to3d());
			}
			
			Motion motion = new Motion(s.motion);
			motion.root().mul(transform, motion.root());
			motionList.add(motion);
		}
		
		return motionList;
	}
	
	public static ArrayList<SequentialMotion> rotateSequence(ArrayList<SequentialMotion> sequence, double angle){
		double[] deltaList = new double[sequence.size()];
		double minMove = 0.3;
		double maxMove = 1;
		for (int i = 0; i < deltaList.length; i++) {
			double move = sequence.get(i).translation.length();
			double a = Math.min(1, Math.max(0, move - minMove)/(maxMove-minMove))*Math.PI/2;
			double ratio = 1 - Math.cos(a);
			deltaList[i] = angle*ratio;
		}
		deltaList = MathUtil.applyGaussianFilter(deltaList, 7);
		
		ArrayList<SequentialMotion> result = new ArrayList<SequentialMotion>();
		for (int i = 0; i < deltaList.length; i++) {
			SequentialMotion origin = sequence.get(i);
			Vector2d t = MathUtil.rotate(origin.translation, deltaList[i]/2);
			SequentialMotion m = new SequentialMotion(origin.motion, origin.rotation + deltaList[i], t);
			result.add(m);
		}
		return result;
	}
}
