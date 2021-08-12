package mrl.motion.neural.run;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.data.BallTrajectoryGenerator;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.PointIKSolver;
import mrl.motion.position.PositionResultMotion;
import mrl.motion.position.PositionResultMotion.PositionFrame;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class RuntimeMotionGenerator {
	
	public static PointIKSolver ikSolver;
	static{
		if (MotionDataConverter.useOrientation) {
			MotionData mData = new BVHParser().parse(new File(Configuration.BASE_MOTION_FILE));
			ikSolver = new PointIKSolver(mData.skeletonData, mData.motionList.get(0));
		}
	}
	
	public static boolean ALWAYS_HAS_BALL = false;
	

	public Pose2d pose = new Pose2d(Pose2d.BASE);
	public PositionResultMotion motionSequence;
	public ArrayList<Pose2d> poseList = new ArrayList<Pose2d>();
	public ArrayList<Motion> motionList = new ArrayList<Motion>();
	public ArrayList<Point3d> ballList = new ArrayList<Point3d>();
	public ArrayList<double[]> dataList = new ArrayList<double[]>();
	private Boolean prevHasBall = null;
			
	
	public RuntimeMotionGenerator(){
		motionSequence = new PositionResultMotion();
	}
	
	public Motion updateByOri(double[] data){
		Motion motion = MotionDataConverter.dataToMotionByOri(data);
		
		int rIdx = MotionDataConverter.ROOT_OFFSET - 3;
		Matrix2d m = new Matrix2d(data[rIdx + 0], data[rIdx + 1], data[rIdx + 2]);
		pose.transformLocal(m);
		
		Matrix4d transform = Pose2d.globalTransform(Pose2d.BASE, pose).to3d();
		motion.root().mul(transform, motion.root());
		motionList.add(motion);
		
		int fIdx = rIdx - 2;
		motion.isLeftFootContact = data[fIdx] > 0.5;
		motion.isRightFootContact = data[fIdx+1] > 0.5;
		int bIdx = fIdx - 2;
		if (bIdx > 0){
			motion.ballContact.left = data[bIdx] > 0.5;
			motion.ballContact.right = data[bIdx+1] > 0.5;
		}
		if (bIdx > 0){
			double distLimit = BallTrajectoryGenerator.BALL_RADIUS*1.5;
//			double distLimit = BallTrajectoryGenerator.BALL_RADIUS*2;
			
			boolean hasBall = data[data.length-1] > 0.5;
			if (ALWAYS_HAS_BALL) hasBall = true;
			boolean isChanged = (prevHasBall != null && prevHasBall != hasBall);
			if (hasBall){
				double[] b = new double[3];
				System.arraycopy(data, 0, b, 0, 3);
				Point3d p = new Point3d(b[0], 0, b[2]);
				p = Pose2d.to3d(pose.localToGlobal(Pose2d.to2d(p)));
				p.y = b[1];
				
				if (isChanged){
					Motion last = Utils.last(motionList);
					boolean isValid = BallTrajectoryGenerator.isValidBallPos(p, last, distLimit);
					if (!isValid){
						p = null;
						hasBall = false;
					}
				}
				
				ballList.add(p);
			} else {
				if (isChanged){
					int idx = ballList.size() - 1;
					while (true){
						boolean isValid = BallTrajectoryGenerator.isValidBallPos(ballList.get(idx), motionList.get(idx), distLimit);
						if (isValid) break;
						ballList.set(idx, null);
						idx--;
						if (idx < 0) break;
					}
				}
				ballList.add(null);
			}
			prevHasBall = hasBall;
		}
		return motion;
	}
	
	public PositionResultMotion update(double[] data){
		dataList.add(MathUtil.copy(data));
		poseList.add(new Pose2d(pose));
		
		int rIdx = MotionDataConverter.ROOT_OFFSET - 3;
		int fIdx = rIdx - 2;
		int bIdx = fIdx - 2;
		
		
		Matrix2d m = new Matrix2d(data[rIdx + 0], data[rIdx + 1], data[rIdx + 2]);
		pose.transformLocal(m);
		Matrix4d transform = Pose2d.globalTransform(Pose2d.BASE, pose).to3d();
		
		PositionFrame frame = MotionDataConverter.dataToPointList(data);
		for (Point3d[] pp : frame){
			for (Point3d p : pp){
				transform.transform(p);
			}
		}
		
		frame.footContact.left = data[fIdx] > 0.5;
		frame.footContact.right = data[fIdx+1] > 0.5;
		if (bIdx > 0){
			frame.ballContact.left = data[bIdx] > 0.5;
			frame.ballContact.right = data[bIdx+1] > 0.5;
		}
		PositionResultMotion motion = new PositionResultMotion();
		motion.add(frame);
		motionSequence.add(frame);
		
		if (MotionDataConverter.useOrientation){
			Motion mMotion;
			if(MotionDataConverter.useMatrixForAll)  mMotion= MotionDataConverter.dataToMotionByOriMatForAll(data);
			else mMotion= MotionDataConverter.dataToMotionByOri(data);
			
			if (ikSolver != null) {
				HashMap<String, Point3d> map = MotionDataConverter.dataToPointMapByPosition(data);
				mMotion = RuntimeMotionGenerator.ikSolver.solveFoot(mMotion, map);
			}
			
			
			mMotion.root().mul(transform, mMotion.root());
			
			mMotion.isLeftFootContact = data[fIdx] > 0.5;
			mMotion.isRightFootContact = data[fIdx+1] > 0.5;
			if (bIdx > 0){
				mMotion.ballContact.left = data[bIdx] > 0.5;
				mMotion.ballContact.right = data[bIdx+1] > 0.5;
			}
			motionList.add(mMotion);
		} else {
//			motionList.add(solveIK(motion, data));
		}
		
		if (bIdx > 0){
			double distLimit = BallTrajectoryGenerator.BALL_RADIUS*1.5;
//			double distLimit = BallTrajectoryGenerator.BALL_RADIUS*2;
			
			boolean hasBall = data[data.length-1] > 0.5;
			if (ALWAYS_HAS_BALL) hasBall = true;
			boolean isChanged = (prevHasBall != null && prevHasBall != hasBall);
			if (hasBall){
				double[] b = new double[3];
				System.arraycopy(data, 0, b, 0, 3);
				Point3d p = new Point3d(b[0], 0, b[2]);
				p = Pose2d.to3d(pose.localToGlobal(Pose2d.to2d(p)));
				p.y = b[1];
				
				if (isChanged){
					Motion last = Utils.last(motionList);
					boolean isValid = BallTrajectoryGenerator.isValidBallPos(p, last, distLimit);
					if (!isValid){
						p = null;
						hasBall = false;
					}
				}
				
				ballList.add(p);
			} else {
				if (isChanged){
					int idx = ballList.size() - 1;
					while (true){
						boolean isValid = BallTrajectoryGenerator.isValidBallPos(ballList.get(idx), motionList.get(idx), distLimit);
						if (isValid) break;
						ballList.set(idx, null);
						idx--;
						if (idx < 0) break;
					}
				}
				ballList.add(null);
			}
			prevHasBall = hasBall;
		}
		
		return motion;
	}
	
	public boolean isBallInHand(){
		Point3d ball = ballPosition();
		double distLimit = BallTrajectoryGenerator.BALL_RADIUS*1.5;
		if (ball == null) return false;
		return BallTrajectoryGenerator.isValidBallPos(ball, Utils.last(motionList), distLimit);
	}
	
	public Motion motion(){
		if (motionList.size() <= 0) return null;
		return Utils.last(motionList);
	}
	
	public Point3d ballPosition(){
		if (ballList.size() <= 0) return null;
		return Utils.last(ballList);
	}
	
//	private Motion solveIK(PositionResultMotion motion, double[] output){
//		HashMap<String, Point3d> map = MotionDataConverter.dataToPointMap(output);
//		Motion mm = ikSolver.solve(map, this.pose);
//		mm.ballContact = motion.get(0).ballContact;
//		mm.isLeftFootContact = motion.get(0).footContact.left;
//		mm.isRightFootContact = motion.get(0).footContact.right;
//		return mm;
//	}
}
