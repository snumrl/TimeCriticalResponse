package mrl.motion.neural.agility.measure;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class VelocityAutoLabeling {
	
	public static int ROTATION_MAINTAIN_MARGIN = 3;
	public static int VELOCITY_MAINTAIN_MARGIN = 3;
	

	private MDatabase database;
	private Motion[] mList;
	
	public boolean[] isRotationMaintain;
	public boolean[] isVelocityMaintain;
	
	public double rotationLimit = 20; // degree
	public double velocityLimit = 1; // cm

	public VelocityAutoLabeling(MDatabase database) {
		this.database = database;
		mList = database.getMotionList();
	}
	
	public void calc() {
		int margin = ROTATION_MAINTAIN_MARGIN;
		isRotationMaintain = new boolean[mList.length];
		for (MotionData mData : database.getMotionDataList()) {
			for (int i = margin; i < mData.motionList.size() - margin; i++) {
				boolean rotMaintain = isStraight(Utils.cut(mData.motionList, i-(margin), i + (margin)), 20);
				isRotationMaintain[mData.motionList.get(i).motionIndex] = rotMaintain;
			}
		}
		isRotationMaintain = checkFutureMaintain(isRotationMaintain, margin*2);
		
		margin = VELOCITY_MAINTAIN_MARGIN;
		isVelocityMaintain = new boolean[mList.length];
		for (MotionData mData : database.getMotionDataList()) {
			ArrayList<Pose2d> velList = getVelocityList(mData.motionList);
//			for (int i = 1; i < velList.size(); i++) {
//				double l1 = MathUtil.length(velList.get(i-1).position);
//				double l2 = MathUtil.length(velList.get(i).position);
//				System.out.println("Vvv : " + mData.motionList.get(i) + " : " + l2 + " : " + (l1 - l2));
//			}
			for (int i = margin; i < mData.motionList.size() - margin - 1; i++) {
//				System.out.println(mData.motionList.get(i) + "\t" + velList.get(i).position + " : " + MathUtil.length(velList.get(i).position));
				boolean vMaintain = isVelocityMaintain(Utils.cut(velList, i-(margin), i + (margin) + 1), velocityLimit);
				vMaintain = MathUtil.length(velList.get(i).position) < 150;
				isVelocityMaintain[mData.motionList.get(i).motionIndex] = vMaintain;
			}
		}
		isVelocityMaintain = checkFutureMaintain(isVelocityMaintain, margin*2);
	}
	
	public boolean[] checkFutureMaintain(boolean[] marked, int margin) {
		boolean[] updated = new boolean[marked.length];
		for (int i = 0; i < updated.length; i++) {
			boolean isAllMarked = true;
			for (int j = 0; j <= margin; j++) {
				int idx = Math.min(i + j, updated.length-1);
				if (mList[i].motionData != mList[idx].motionData) break;;
				if (!marked[idx]) {
					isAllMarked = false;
					break;
				}
			}
			updated[i] = isAllMarked;
		}
		return updated;
	}
	
	
	public static boolean isVelocityMaintain(ArrayList<Pose2d> velList, double diffLimit) {
		double diffSum = 0;
		int count = 0;
		for (int i = 0; i < velList.size(); i++) {
			Pose2d v1 = velList.get(i);
			for (int j = i+1; j < velList.size(); j++) {
				count++;
				Pose2d v2 = velList.get(j);
				
//				double dp = v1.position.distance(v2.position);
				double dp = MathUtil.length(v1.position) - MathUtil.length(v2.position);
				diffSum += dp*dp;
			}
		}
		
		double mean = Math.sqrt(diffSum/count);
		return mean < diffLimit;
		
	}
	
	
	public static ArrayList<Pose2d> smoothing(ArrayList<Pose2d> list , int margin){
		ArrayList<Pose2d> result = new ArrayList<Pose2d>();
		for (int i = 0; i < list.size(); i++) {
			Pose2d sum = null;
			int count = 0;
			for (int offset = -margin; offset <= margin; offset++) {
				int idx = i + offset;
				if (idx < 0) idx = 0;
				if (idx >= list.size()) idx = list.size()-1;
				Pose2d p = list.get(idx);
				if (sum == null) {
					sum = new Pose2d(p);
				} else {
					sum.position.add(p.position);
					sum.direction.add(p.direction);
				}
				count++;
			}
			sum.position.scale(1d/count);
			sum.direction.scale(1d/count);
			sum.direction.normalize();
			result.add(sum);
		}
		return result;
	}
	
	
	public static ArrayList<Pose2d> getVelocityList(List<Motion> motionList) {
		ArrayList<Pose2d> poseList = new ArrayList<Pose2d>();
		for (Motion m : motionList) {
			poseList.add(Pose2d.getPose(m));
		}
		poseList = smoothing(poseList, 5);
		
		int velMargin = 5;
		ArrayList<Pose2d> velList = new ArrayList<Pose2d>();
		for (int i = 0; i < poseList.size(); i++) {
			int prevIdx = Math.max(0, i - velMargin);
			Pose2d prev = poseList.get(prevIdx);
			Pose2d current = poseList.get(i);
			Pose2d vel = prev.globalToLocal(current);
			vel.position.scale(30d/5);
			velList.add(vel);
		}
		return velList;
	}
	
	public static boolean isStraight(List<Motion> motionList, double degreeLimit) {
		ArrayList<Pose2d> poseList = new ArrayList<Pose2d>();
		for (Motion m : motionList) {
			poseList.add(PositionMotion.getPose(m));
		}
		
		double rotSum1 = 0;
		double rotSum2 = 0;
		int count = 0;
		
		double rotLimit = Math.toRadians(degreeLimit);
		for (int i = 1; i < poseList.size(); i++) {
			Pose2d p = poseList.get(0).globalToLocal(poseList.get(i));
			double angle1 = Math.abs(MathUtil.directionalAngle(Pose2d.BASE.direction, p.position));
			double angle2 = Math.abs(MathUtil.directionalAngle(Pose2d.BASE.direction, p.direction));
			rotSum1 += angle1*angle1;
			rotSum2 += angle2*angle2;
			count++;
//			if (angle1 > rotLimit || angle2 > rotLimit) return false;
		}
		rotSum1 /= count;
		rotSum2 /= count;
		rotLimit = rotLimit*rotLimit;
		if (rotSum1 > rotLimit || rotSum2 > rotLimit) return false;
		return true;
	}
}
