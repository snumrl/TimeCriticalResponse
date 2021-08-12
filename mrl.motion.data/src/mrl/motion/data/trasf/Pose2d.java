package mrl.motion.data.trasf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.SkeletonData;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;

public class Pose2d implements Serializable{
	
	private static final long serialVersionUID = 4476320503634896861L;

	public static final Pose2d BASE = new Pose2d(new Point2d(0, 0), new Vector2d(1, 0));
	public static String Hips = "Hips";
	public static String LeftUpLeg = "LeftUpLeg";
	public static String RightUpLeg = "RightUpLeg";
	public static String Spine = "Spine";

	public Point2d position;
	public Vector2d direction;
	
	
	public static Pose2d getPose(Motion motion){
		if (MotionTransform.IS_TPOSE_DEFINED) {
			return MotionTransform.getPose(motion.root());
		}
		return getPoseByPelvis(motion);
	}
	
	public static Pose2d getPoseByPelvis(Motion motion) {
		HashMap<String, Point3d> pointData = Motion.getPointData(SkeletonData.instance, motion);
		
		Vector3d legVector = MathUtil.sub(pointData.get(LeftUpLeg), pointData.get(RightUpLeg));
		Vector3d upVector = new Vector3d(0, 1, 0);
		Vector3d cross = MathUtil.cross(legVector, upVector);
		cross.y = 0;
		cross.normalize();
		Point3d root = pointData.get(Hips);
		return new Pose2d(root, cross);
	}
	
	@Deprecated
	public Pose2d(Matrix4d root){
		double angle = MotionTransform.alignAngle(root);
		Vector3d translation = MathUtil.getTranslation(root);
		
		direction = MathUtil.rotate(new Vector2d(-1, 0), -angle);
		position = new Point2d(translation.x, -translation.z);
	}
	
	public Pose2d(Pose2d copy){
		this(copy.position, copy.direction);
	}
	
	public Pose2d(Point2d position, Vector2d direction) {
		this(position.x, position.y, direction.x, direction.y);
	}
	
	public Pose2d(Point3d position, Vector3d direction) {
		this(to2d(position), to2d(direction));
	}
	
	public Pose2d(double px, double py, double dx, double dy){ 
		this.position = new Point2d(px, py);
		this.direction = new Vector2d(dx, dy);
		this.direction.normalize();
	}
	
	public static Pose2d byBase(Matrix2d transform){
		Pose2d p = new Pose2d(BASE);
		p.transformGlobal(transform);
		return p;
	}
	
	public static Pose2d interpolation(Pose2d p1, Pose2d p2, double ratio){
		Point2d p = new Point2d();
		p.interpolate(p1.position,  p2.position, ratio);
		Vector2d d = new Vector2d();
		d.interpolate(p1.direction,  p2.direction, ratio);
		return new Pose2d(p, d);
	}

	public Point3d position3d(){
		return new Point3d(position.x, 0, -position.y);
	}
	
	public Vector3d direction3d(){
		return new Vector3d(direction.x, 0, -direction.y);
	}
	
	public static Point2d to2d(Point3d p){
		return new Point2d(p.x, -p.z);
	}
	public static Vector2d to2d(Vector3d p){
		return new Vector2d(p.x, -p.z);
	}
	public static Point3d to3d(Point2d p){
		return new Point3d(p.x, 0, -p.y);
	}
	public static Vector3d to3d(Vector2d p){
		return new Vector3d(p.x, 0, -p.y);
	}
	
	
	public static Matrix2d localTransform(Pose2d base, Pose2d target){
		// consider base direction as X axis(=BASE) of the transform.
		Vector2d xAxis = base.direction;
		Vector2d yAxis = new Vector2d(-xAxis.y, xAxis.x);
		Vector2d translation = MathUtil.sub(target.position, base.position);
		
		double x = xAxis.dot(translation);
		double y = yAxis.dot(translation);
		
		double x1 = base.direction.x;
		double y1 = base.direction.y;
		double x2 = target.direction.x;
		double y2 = target.direction.y;
		double angle = Math.atan2(x1 * y2 - y1 * x2, x1 * x2 + y1 * y2);
		
		Matrix2d m = new Matrix2d(angle, x, y);
		return m;
	}
	
	/**
	 * current pose를 target pose로 변환시키도록 하는 global coordinate 상에서의 transform을 구하는 함수
	 * @param current
	 * @param target
	 * @return
	 */
	public static Matrix2d globalTransform(Pose2d current, Pose2d target){
		double x1 = current.direction.x;
		double y1 = current.direction.y;
		double x2 = target.direction.x;
		double y2 = target.direction.y;
		double angle = Math.atan2(x1 * y2 - y1 * x2, x1 * x2 + y1 * y2);
		
		Vector2d currentP = MathUtil.rotate(new Vector2d(current.position), angle);
		Vector2d translation = MathUtil.sub(target.position, currentP);
		return new Matrix2d(angle, translation.x, translation.y);
	}
	
	/**
	 * base가 바라보는 방향을 x축이라고 하고 base의 위치를 원점이라고 할때,
	 * target의 상대적인 위치 및 방향 값을 반환
	 * @param base
	 * @param target
	 * @return
	 */
	public static Pose2d relativePose(Pose2d base, Pose2d target){
		Vector2d xAxis = new Vector2d(base.direction);
		Vector2d yAxis = new Vector2d(-base.direction.y, base.direction.x);
		Vector2d dt = MathUtil.sub(target.position, base.position);
		double px = dt.dot(xAxis);
		double py = dt.dot(yAxis);
		double dx = target.direction.dot(xAxis);
		double dy = target.direction.dot(yAxis);
		return new Pose2d(px, py, dx, dy);
	}
	
	public void transformGlobal(Matrix2d t){
		t.transform(position);
		t.transform(direction);
	}
	
	public void transformGlobal(Matrix4d t){
		Point3d p3d = to3d(position);
		Vector3d v3d = to3d(direction);
		t.transform(p3d);
		t.transform(v3d);
		position = to2d(p3d);
		direction = to2d(v3d);
	}
	
	public void transformLocal(Matrix2d t){
		Vector2d xAxis = new Vector2d(direction);
		Vector2d yAxis = new Vector2d(-direction.y, direction.x);
		Vector2d dt = t.getTranslation();
		xAxis.scale(dt.x);
		yAxis.scale(dt.y);
		position.add(xAxis);
		position.add(yAxis);
//		double px = dt.dot(xAxis);
//		double py = dt.dot(yAxis);
//		position.add(new Point2d(px, py));
		direction = MathUtil.rotate(direction, t.getAngle());
	}
	
	public Pose2d globalToLocal(Pose2d p){
		return relativePose(this, p);
	}
	
	public Point2d globalToLocal(Point2d p){
		Vector2d dp = new Vector2d();
		dp.sub(p, position);
		
		Vector2d xAxis = new Vector2d(direction);
		Vector2d yAxis = new Vector2d(-direction.y, direction.x);
		return new Point2d(xAxis.dot(dp), yAxis.dot(dp));
	}
	public Vector2d globalToLocal(Vector2d p){
		Vector2d dp = p;
		Vector2d xAxis = new Vector2d(direction);
		Vector2d yAxis = new Vector2d(-direction.y, direction.x);
		return new Vector2d(xAxis.dot(dp), yAxis.dot(dp));
	}
	
	public Pose2d localToGlobal(Pose2d p){
		return new Pose2d(localToGlobal(p.position), localToGlobal(p.direction));
	}
	
	public Point2d localToGlobal(Point2d p){
		Point2d p1 = new Point2d();
		p1.set(direction);
		p1.scale(p.x);
		Point2d p2 = new Point2d(-direction.y, direction.x);
		p2.scale(p.y);
		p1.add(p2);
		p1.add(position);
		return p1;
	}
	public Vector2d localToGlobal(Vector2d p){
		Vector2d p1 = new Vector2d();
		p1.set(direction);
		p1.scale(p.x);
		Vector2d p2 = new Vector2d(-direction.y, direction.x);
		p2.scale(p.y);
		p1.add(p2);
		return p1;
	}
	
	public String toString(){
		return String.format("(%.4f,%.4f),(%.4f,%.4f)", position.x, position.y, direction.x, direction.y);
	}
	
	public double[] toArray(){
		return new double[]{ position.x, position.y, direction.x, direction.y };
	}
	
	public static Motion getAlignedMotion(Motion motion){
		motion = new Motion(motion);
		Pose2d pose = getPose(motion);
		Matrix4d mm = Pose2d.globalTransform(pose, Pose2d.BASE).to3d();
		motion.root().mul(mm, motion.root());
		return motion;
	}
	
	public static ArrayList<Motion> getAlignedMotion(ArrayList<Motion> motionList, int offset){
		Motion base = motionList.get(offset);
		Pose2d pose = PositionMotion.getPose(base);
		Matrix4d mm = Pose2d.globalTransform(pose, Pose2d.BASE).to3d();
		ArrayList<Motion> aligned = new ArrayList<Motion>();
		for (Motion m : motionList) {
			m = new Motion(m);
			m.root().mul(mm, m.root());
			aligned.add(m);
		}
		return aligned;
	}
	
	public static void main(String[] args) {
		Pose2d p1 = new Pose2d(0, 0, 0, 1);
		Pose2d p2 = new Pose2d(1, 0, 0, 1);
		Matrix2d t1 = localTransform(p1, p2);
		System.out.println(t1.toString2());
		
		
		Pose2d rp = relativePose(p1, p2);
		System.out.println(rp);
		System.out.println(localTransform(BASE, rp).toString2());
		
		p1.transformLocal(t1);
		System.out.println(p1);
//		Matrix2d t2 = globalTransform(p1, p2);
//		System.out.println(t2.toString2());
//		
//		
//		System.out.println(Math.toDegrees(MathUtil.directionalAngle(new Vector2d(1, 0), new Vector2d(0, -1))));
//		System.out.println(Math.toDegrees(MathUtil.directionalAngle(p1.direction, p2.direction)));
	}
}
