package mrl.motion.data.edit;

import java.io.File;
import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import mrl.jni.motion.Constraint;
import mrl.jni.motion.MotionEditJNI;
import mrl.jni.motion.RootTrajectory;
import mrl.jni.motion.RootTrajectory.RootInfo;
import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.parser.BVHWriter;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.position.PositionMotion;
import mrl.util.Configuration;
import mrl.util.MathUtil;

public class MotionEdit {
	
	public static int TIME_WINDOW = 48;
	public static boolean DO_TIME_EDIT = true;
	
//	private static MotionSegment getEditedSegment(MotionSegment segment, int f1, int f2, double[] target){
//		return getEditedSegment(segment, f1, f2, target[0], target[1], target[2]);
//	}
	
//	private static MotionSegment getEditedSegment(MotionSegment segment, int f1, int f2, double rot, double tx, double ty){
//		return getEditedSegment(segment, f1, f2, Pose2d.byBase(new Matrix2d(rot, tx, ty)));
//	}
	
	public static MotionSegment getEditedSegment(MotionSegment segment, int f1, int f2, Pose2d p2){
		return getEditedSegment(segment, f1, f2, p2, -1);
	}
	public static MotionSegment getEditedSegment(MotionSegment segment, int f1, int f2, Pose2d p2, int timeLen){
		Pose2d p1 = Pose2d.BASE;
		
		ArrayList<Constraint> constraints = new ArrayList<Constraint>();
		Constraint c1 = new Constraint();
		c1.constraintPersons = new int[]{ 0, -1 };
		c1.constraintFrames = new int[]{ f1, (DO_TIME_EDIT && timeLen >= 0) ? 0 : -100000 };
		c1.posConstraint = getPosConstraint(segment, f1, p1);
		constraints.add(c1);
		Constraint c2 = new Constraint();
		c2.constraintPersons = new int[]{ 0, -1 };
		c2.constraintFrames = new int[]{ f2, (DO_TIME_EDIT && timeLen >= 0) ? timeLen : -100000 };
		c2.posConstraint = getPosConstraint(segment, f2, p2);
		constraints.add(c2);
		
		MotionSegment edited = editByRoot(segment, constraints);
		return edited;
	}
	
	private static MotionSegment editByRoot(MotionSegment segment, ArrayList<Constraint> constraints){
		segment = new MotionSegment(segment);
		MDatabase.applyDistortionForEdit(segment.getEntireMotion());
		RootTrajectory rt = new RootTrajectory(segment, true);
		for (Constraint c : constraints){
			c.constraintFrames[0] += Configuration.BLEND_MARGIN;
			c.constraintFrames[1] += Configuration.BLEND_MARGIN;
		}
		RootTrajectory[] editedRT = MotionEditJNI.instance.editRootTrajectory(new RootTrajectory[]{ rt }, constraints);
		ArrayList<Motion> smList = segment.getEntireMotion();
		for (int i = 0; i < smList.size(); i++) {
			RootInfo root = editedRT[0].getMotionList().get(i);
			Matrix4d m = root.root();
			if (Double.isNaN(m.m00)) {
				System.out.println("nan :: " + i + " : " + segment.getEntireMotion().get(i) + " : " + smList.size());
//				BVHWriter bw = new BVHWriter();
//				bw.write(new File("motionEdit_NaN.bvh"), new MotionData(segment.getEntireMotion()));
//				throw new RuntimeException();
				return segment;
			}
			smList.get(i).root().set(m);
			smList.get(i).knot = root.knot - Configuration.BLEND_MARGIN;
		}
		segment.updateNotBlendedAsCurrent();
		return segment;
	}
	
	private static double[] getPosConstraint(MotionSegment s, int index, Pose2d pose){
		
		Point3d translation = pose.position3d();
		Point2d pos;
		if (Double.isNaN(pose.position.x)) {
			pos = new Point2d(Integer.MIN_VALUE, Integer.MIN_VALUE);
		} else {
			pos = new Point2d(translation.x, translation.z);
		}
		Vector2d dir;
		if (Double.isNaN(pose.direction.x)) {
			dir = new Vector2d(Integer.MIN_VALUE, Integer.MIN_VALUE);
		} else {
			ArrayList<Motion> mList = s.getEntireMotion();
			Motion m = mList.get(MotionSegment.BLEND_MARGIN() + index);
			Vector3d tPrev = MathUtil.getTranslation(mList.get(MotionSegment.BLEND_MARGIN() + index-1).root());
			Vector3d tNext = MathUtil.getTranslation(mList.get(MotionSegment.BLEND_MARGIN() + index+1).root());
			Vector3d v = MathUtil.sub(tNext, tPrev);
			Pose2d originPose = PositionMotion.getPose(m);
			Matrix4d t = Pose2d.globalTransform(originPose, pose).to3d();
			t.transform(v);
			dir = new Vector2d(v.x, v.z);
		}
		return new double[]{ pos.x, pos.y, dir.x, dir.y };
	}
}
