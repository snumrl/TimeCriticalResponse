package mrl.motion.neural.data;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.annotation.MotionAnnotationHelper;
import mrl.motion.data.Contact;
import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionAnnotationManager;
import mrl.motion.data.MotionData;
import mrl.motion.data.SequenceStitch;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.Joint;
import mrl.motion.data.trasf.MotionTransform;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.Item.ItemDescription;

public class BallTrajectoryGenerator {
	
	public static String BALL_ANN_FOLDER = "danceCard\\ballAnnotation";

	public static Point3d GOAL_POS = new Point3d(0, 300, 122);
	public static double BALL_RADIUS = 13;
//	private static double BALL_RADIUS = 24.64/2;
	public static String[] HAND_JOINTS = { "LeftHand", "RightHand" }; 
	private static Matrix4d[] HAND_TRANSFORM_OFFSET = null;
	private static double GRAVITY = 9.8*100; // m  ->  cm
	private static double FPS = 30;
	
	public static ItemDescription ballDescription(){
		ItemDescription des = new ItemDescription(BALL_RADIUS);
		des.color = new Vector3d(0.8, 0.4, 0);
		return des;
	}
	
	public BallTrajectoryGenerator() {
	}
	
	public ArrayList<Point3d> generate(ArrayList<Motion> motionList){
		Contact prevContact = null;
		int startIndex = 0;
		ArrayList<BallInterval> intervalList = new ArrayList<BallInterval>();
		for (int i = 0; i < motionList.size(); i++) {
			Contact c = motionList.get(i).ballContact;
			if (!c.equals(prevContact)){
				 if (prevContact != null){
					 intervalList.add(new BallInterval(startIndex, i-1, prevContact));
				 }
				 prevContact = c;
				 startIndex = i;
			}
		}
		intervalList.add(new BallInterval(startIndex, motionList.size()-1, prevContact));
		
		SequenceStitch<Point3d> stitch = new SequenceStitch<Point3d>(0) {
			@Override
			protected Point3d interpolate(Point3d e1, Point3d e2, double ratio) {
				if (e1 == null) return e2;
				if (e2 == null) return e1;
				Point3d p = new Point3d();
				p.interpolate(e1, e2, ratio);
				return p;
			}
		};
		
		BallInterval prevInterval = null;
		for (BallInterval interval : intervalList) {
			ArrayList<Point3d> ballTrajectory = new ArrayList<Point3d>();
			int margin = 0;
			if (interval.isAir()){
				ballTrajectory.addAll(getBounceTrajectory(motionList, interval.start, interval.end));
			} else {
				for (int i = interval.start; i <= interval.end; i++) {
					ballTrajectory.add(getBallPositionByHand(motionList.get(i)));
				}
				if ((prevInterval != null && prevInterval.isHold()) || interval.isHold()){
					margin = 3;
				}
			}
			
			stitch.append(ballTrajectory, margin);
			prevInterval = interval;
		}
		
		return stitch.getSequence();
	}
	
	private static void calcHandTransformOffset() {
		HAND_TRANSFORM_OFFSET = new Matrix4d[2];
		HashMap<String, Matrix4d> map = Motion.getTransformData(SkeletonData.instance, MotionTransform.T_POSE_MOTION);
		
		
	}
	
	public static boolean isValidBallPos(Point3d ball, Motion motion, double distLen){
		HashMap<String, Matrix4d> transform = Motion.getTransformData(SkeletonData.instance, motion);
		for (String j : HAND_JOINTS){
			Point3d hb = getBallPositionByHand(transform, j);
			if (hb.distance(ball) < distLen) return true;
		}
		return false;
	}
	
	public static Point3d getBallPositionByHand(Motion motion){
		HashMap<String, Matrix4d> transform = Motion.getTransformData(SkeletonData.instance, motion);
		Point3d left = null;
		Point3d right = null;
		if (motion.ballContact.left){
			left = getBallPositionByHand(transform, HAND_JOINTS[0]);
		}
		if (motion.ballContact.right){
			right = getBallPositionByHand(transform, HAND_JOINTS[1]);
		}
		if (left != null && right != null){
			left.interpolate(right, 0.5);
			return left;
		} else if (left != null){
			return left;
		} else if (right != null){
			return right;
		} else {
			// missing
			left = getBallPositionByHand(transform, HAND_JOINTS[0]);
			right = getBallPositionByHand(transform, HAND_JOINTS[1]);
			left.interpolate(right, 0.5);
			return left;
		}
	}
	private static Point3d getBallPositionByHand(HashMap<String, Matrix4d> transform, String jointName){
		Joint hand = SkeletonData.instance.get(jointName);
		Joint finger = hand.children.get(0);
		Matrix4d t = new Matrix4d(transform.get(jointName));
		
//		Matrix4d rotZ = new Matrix4d();
//		rotZ.rotZ(Math.toRadians(40));
//		t.mul(rotZ);
//		
		if (jointName.startsWith("Right")){
			Matrix4d rotX = new Matrix4d();
			rotX.rotX(Math.toRadians(180));
			t.mul(rotX);
		}
		
		
		Point3d ballPos = new Point3d();
		ballPos = new Point3d(finger.transition);
		ballPos.y += BALL_RADIUS;
		t.transform(ballPos);
		return ballPos;
	}
	
	public ArrayList<Point3d> getShootAndFall(Point3d startPos, double heightOffset){
		ArrayList<Point3d> shootTrajectory = getShootTrajectory(startPos, GOAL_POS, 80);
		ArrayList<Point3d> fallTrajectory = getFreeFallTrajectory(startPos, GOAL_POS);
		ArrayList<Point3d> ballShootTrajectory = new ArrayList<Point3d>();
		ballShootTrajectory.addAll(shootTrajectory);
		ballShootTrajectory.addAll(fallTrajectory);
		return ballShootTrajectory;
	}
	
	public ArrayList<Point3d> getShootTrajectory(Point3d startPos, Point3d goalPos, double heightOffset){
		double fallTime = Math.sqrt(2/GRAVITY*heightOffset);
		double upTime = Math.sqrt(2/GRAVITY*(goalPos.y + heightOffset - startPos.y));
		double vy0 = (goalPos.y + heightOffset - startPos.y)/upTime + 0.5*GRAVITY*upTime;
		double time = fallTime + upTime;
		
		Vector3d v_hori = MathUtil.sub(goalPos, startPos);
		v_hori.y = 0;
		v_hori.scale(1d/time);
		
		ArrayList<Point3d> trajectory = new ArrayList<Point3d>();
		int frames = (int)Math.ceil(time*FPS) + 1;
		for (int i = 1; i < frames; i++) {
			double t = i/FPS;
			
			Point3d p = new Point3d();
			p.scaleAdd(t, v_hori, startPos);
			p.y = startPos.y + vy0*t - 0.5*GRAVITY*t*t;
			trajectory.add(p);
		}
		System.out.println("shoot last :: " + Utils.last(trajectory) + " : " + trajectory.get(trajectory.size()-2));
		return trajectory;
	}
	
	public ArrayList<Point3d> getFreeFallTrajectory(Point3d startPos, Point3d goalPos){
		Vector3d v_hori = MathUtil.sub(startPos, goalPos);
		v_hori.x *= -1;
		v_hori.normalize();
		Vector3d a_hori = new Vector3d(v_hori);
		v_hori.scale(200);
		a_hori.scale(30);
		
		int bounceCount = 8;
		double[] bounceTime = new double[bounceCount];
		double vRatio = 0.6;
		double[] vy0List = new double[bounceCount];
		for (int i = 0; i < bounceTime.length; i++) {
			if (i == 0){
				bounceTime[i] = Math.sqrt(2/GRAVITY*(goalPos.y  - BALL_RADIUS));
				vy0List[i] = GRAVITY*bounceTime[i]*vRatio;
			} else {
				double t = 2*vy0List[i-1]/GRAVITY;
				bounceTime[i] = bounceTime[i-1] + t;
				vy0List[i] = vy0List[i-1]*vRatio;
			}
		}
		
		ArrayList<Point3d> trajectory = new ArrayList<Point3d>();
		for (int i = 1; i < 10000; i++) {
			double t = i/FPS;
			
			Point3d p = new Point3d();
			p.scaleAdd(t, v_hori, goalPos);
			p.scaleAdd(-0.5*t*t, a_hori, p);
			p.y = -100;
			for (int bIdx = 0; bIdx < bounceTime.length; bIdx++) {
				if (t < bounceTime[bIdx]){
					if (bIdx == 0){
						p.y = goalPos.y - 0.5*GRAVITY*t*t;
					} else {
						t = t - bounceTime[bIdx-1];
						double v0 = vy0List[bIdx-1];
						p.y = BALL_RADIUS + v0*t - 0.5*GRAVITY*t*t;
					}
					break;
				}
			}
			trajectory.add(p);
			if (p.y < 0){
				p.y = BALL_RADIUS;
				break;
			}
		}
		return trajectory;
	}
	
	public ArrayList<Point3d> getOneBounceTrajectory(Point3d startPos, Point3d endPos, double maxHeight, int start, int end){
		int bStart = start - 1;
		int bEnd = end + 1;
		int length = bEnd - bStart;
		
		double time = length/FPS;
		Vector3d v_hori = MathUtil.sub(endPos, startPos);
		v_hori.y = 0;
		v_hori.scale(1d/time);
		
		double fallTime = Math.sqrt(2*(maxHeight - BALL_RADIUS)/GRAVITY);
		double upHeight = maxHeight - startPos.y;
		double upTime = Math.sqrt(2*upHeight/GRAVITY);
		double upVel = GRAVITY*upTime;
		
		double t3 = time - fallTime - upTime;
		double upVel2 = ((endPos.y - BALL_RADIUS) + 0.5*GRAVITY*t3*t3)/t3;
		
		ArrayList<Point3d> trajectory = new ArrayList<Point3d>();
		for (int i = start; i <= end; i++) {
			int elapsed = i - bStart;
			double t = elapsed/FPS;
			
			Point3d p = new Point3d();
			p.scaleAdd(t, v_hori, startPos);
			if (t < upTime + fallTime){
				p.y = startPos.y + upVel*t - 0.5*GRAVITY*t*t; 
			} else {
				t -= upTime + fallTime;
				p.y = BALL_RADIUS + upVel2*t - 0.5*GRAVITY*t*t;
			}
			trajectory.add(p);
		}
		return trajectory;
	}
	
	public ArrayList<Point3d> getAirTrajectory(Point3d startPos, Point3d endPos, int start, int end){
		int bStart = start - 1;
		int bEnd = end + 1;
		int length = bEnd - bStart;
		
		double time = length/FPS;
		Vector3d v_hori = MathUtil.sub(endPos, startPos);
		v_hori.y = 0;
		v_hori.scale(1d/time);
		
		double h = endPos.y - startPos.y;
		double v_y = (h + 0.5*GRAVITY*time*time)/time;
		
		ArrayList<Point3d> trajectory = new ArrayList<Point3d>();
		for (int i = start; i <= end; i++) {
			int elapsed = i - bStart;
			double t = elapsed/FPS;
			
			Point3d p = new Point3d();
			p.scaleAdd(t, v_hori, startPos);
			p.y = startPos.y + v_y*t - 0.5*GRAVITY*t*t;
			trajectory.add(p);
		}
		return trajectory;
	}
	
	private ArrayList<Point3d> getBounceTrajectory(ArrayList<Motion> motionList, int start, int end){
		int bStart = Math.max(start-1, 0);
		int bEnd = Math.min(end + 1, motionList.size()-1);
		Point3d startPos = getBallPositionByHand(motionList.get(bStart));
		Point3d endPos = getBallPositionByHand(motionList.get(bEnd));
		return getBounceTrajectory(startPos, endPos, start, end);
	}
	public ArrayList<Point3d> getBounceTrajectory(Point3d startPos, Point3d endPos, int start, int end){
		int bStart = start - 1;
		int bEnd = end + 1;
		int maxLen = 45;
		int length = bEnd - bStart;
		
		double time = Math.min(maxLen, length)/FPS;
		
		double t1 = getFreeFallTime(startPos.y);
		double t2 = getFreeFallTime(endPos.y);
		double tSum = t1 + t2;
		
		t1 = time*t1/tSum;
		t2 = time*t2/tSum;
		double v1 = getBounceVelocity(startPos.y, t1);
		double v2 = getBounceVelocity(endPos.y, t2);
		Vector3d v_hori = MathUtil.sub(endPos, startPos);
		v_hori.y = 0;
		v_hori.scale(1d/time);
		
		ArrayList<Point3d> trajectory = new ArrayList<Point3d>();
		for (int i = start; i <= end; i++) {
			int elapsed = i - bStart;
			int remain = bEnd - i;
			double t = elapsed/FPS;
			if (length > maxLen){
				if (elapsed > maxLen/2 && remain > maxLen/2){
					t = Double.NaN;
				} else if (remain < maxLen/2){
					t = time - remain/FPS; 
				}
			}
			Point3d p = Double.isNaN(t) ? new Point3d(0, -100, 0) : getBouncePoint(startPos, t1, v_hori, v1, v2, t);
//			System.out.println("pp : " + i + " : " + Utils.toString(p, startPos, endPos, v_hori, t));
			trajectory.add(p);
		}
		return trajectory;
	}
	
	private Point3d getBouncePoint(Point3d startPos, double t1, Vector3d v_hori, double v1, double v2, double t){
		Point3d p = new Point3d();
		p.scaleAdd(t, v_hori, startPos);
		if (t < t1){
			p.y = getHeight(v1, t1 - t) + BALL_RADIUS;
		} else {
			p.y = getHeight(v2, t - t1) + BALL_RADIUS;
		}
		return p;
	}
	
	private double getHeight(double bounceV, double time){
		return bounceV*time - 0.5*GRAVITY*time*time;
	}
	
	private double getBounceVelocity(double y, double time){
		return ((y - BALL_RADIUS) + 0.5*GRAVITY*time*time)/time;
	}
	
	private double getFreeFallTime(double y){
		return Math.sqrt(2*(Math.max(1, y - BALL_RADIUS))*GRAVITY);
	}
	
	
	private static class BallInterval{
		int start;
		int end;
		Contact contact;
		
		public BallInterval(int start, int end, Contact contact) {
			this.start = start;
			this.end = end;
			this.contact = contact;
		}
		
		public boolean isHold(){
			return contact.left && contact.right;
		}
		public boolean isAir(){
			return !contact.left && !contact.right;
		}
	}
	
	public static void updateBallContacts(MotionData motionData, String file){
		MotionAnnotationManager eventAnn = new MotionAnnotationManager(BALL_ANN_FOLDER, true){
			protected boolean isValid(MotionAnnotation ann){
				boolean isValid = ann.type.length() > 0;
				return isValid;
			}
		};
		
		Motion[] mList = Utils.toArray(motionData.motionList);
		for (MotionAnnotation ann : eventAnn.getTotalAnnotations()){
			if (!ann.file.equals(file)) continue;
			int start = ann.startFrame;
			int end = ann.endFrame;
			
			if (ann.type.equals(MotionAnnotationHelper.BALL_CONTACTS[0])){
				for (int i = start; i <= end; i++) {
					mList[i].ballContact.left = true;
				}
			} else if (ann.type.equals(MotionAnnotationHelper.BALL_CONTACTS[1])){
				for (int i = start; i <= end; i++) {
					mList[i].ballContact.right = true;
				}
			} else {
				System.out.println("invalid ann :: '" + ann.type  + "'");
				throw new RuntimeException();
			}
		}
	}
	
	public static void updateBallContacts(MDatabase database){
		if (BALL_ANN_FOLDER == null) return;
		
		MotionAnnotationManager eventAnn = new MotionAnnotationManager(BALL_ANN_FOLDER, true){
			protected boolean isValid(MotionAnnotation ann){
				boolean isValid = ann.type.length() > 0;
				return isValid;
			}
		};
		
		Motion[] mList = database.getMotionList();
		for (MotionAnnotation ann : eventAnn.getTotalAnnotations()){
			int start = database.findMotion(ann.file, ann.startFrame).motionIndex;
			int end = database.findMotion(ann.file, ann.endFrame).motionIndex;
			
			if (ann.type.equals(MotionAnnotationHelper.BALL_CONTACTS[0])){
				for (int i = start; i <= end; i++) {
					mList[i].ballContact.left = true;
				}
			} else if (ann.type.equals(MotionAnnotationHelper.BALL_CONTACTS[1])){
				for (int i = start; i <= end; i++) {
					mList[i].ballContact.right = true;
				}
			} else {
				System.out.println("invalid ann :: '" + ann.type  + "'");
				throw new RuntimeException();
			}
		}
	}

}
