package mrl.motion.root;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.util.MathUtil;

public class RootMotionGenerator {

	public RootMotionGenerator() {
	}
	
	public ArrayList<Pose2d> generate(ArrayList<Motion> motionList){
		ArrayList<Pose2d> poseList = new ArrayList<Pose2d>();
		ArrayList<Pose2d> originPoseList = new ArrayList<Pose2d>();
		for (Motion m : motionList) {
			Pose2d p = Pose2d.getPose(m);
			poseList.add(p);
			originPoseList.add(new Pose2d(p));
		}
		ArrayList<Point2d> avgPosList = new ArrayList<Point2d>();
		int avgMargin1 = 20;
		for (int mIdx = 0; mIdx < motionList.size(); mIdx++) {
			int start = Math.max(0, mIdx - avgMargin1);
			int end = Math.min(mIdx + avgMargin1, motionList.size());
			Point2d pos = new Point2d();
			int count = 0;
			for (int i = start; i < end; i++) {
				count++;
				pos.add(poseList.get(i).position);
			}
			pos.scale(1d/count);
			avgPosList.add(pos);
		}
		for (int mIdx = 0; mIdx < motionList.size(); mIdx++) {
			poseList.get(mIdx).position.set(avgPosList.get(mIdx));
		}
//		return poseList;
//		
		ArrayList<Pose2d> rootMotion = new ArrayList<Pose2d>();
		int futureMargin = 15;
		for (int mIdx = 0; mIdx < motionList.size(); mIdx++) {
			int last = Math.min(mIdx + futureMargin, motionList.size());
			Pose2d meanPose = new Pose2d(poseList.get(mIdx));
			int count = 0;
			for (int i = mIdx+1; i < last; i++) {
				meanPose.position.add(poseList.get(i).position);
				meanPose.direction.add(poseList.get(i).direction);
				count++;
			}
			meanPose.position.scale(1d/count);
			meanPose.direction.normalize();
			
			Pose2d lastPose = poseList.get(last-1);
			Vector2d posDirection = MathUtil.sub(lastPose.position, poseList.get(mIdx).position);
			double lenLimit = 15;
			Vector2d rootDirection;
			double pLen = posDirection.length();
			System.out.println("plen : " + mIdx + " : " + pLen);
			if (pLen > lenLimit) {
				posDirection.normalize();
				rootDirection = posDirection; 
			} else {
				if (pLen > 0.0001) {
					posDirection.normalize();
				}
				rootDirection = new Vector2d(meanPose.direction);
				rootDirection.interpolate(meanPose.direction, posDirection, (pLen/lenLimit)*(pLen/lenLimit));
				rootDirection.normalize();
			}
			rootMotion.add(new Pose2d(originPoseList.get(mIdx).position, rootDirection));
		}
		
		int avgMargin2 = 15;
		for (int mIdx = 0; mIdx < motionList.size(); mIdx++) {
			int start = Math.max(0, mIdx - avgMargin2);
			int end = Math.min(mIdx + avgMargin2, motionList.size());
			Vector2d direction = new Vector2d();
			for (int i = start; i < end; i++) {
				direction.add(rootMotion.get(i).direction);
			}
			direction.normalize();
			rootMotion.get(mIdx).direction = direction;
		}
		return rootMotion;
	}
	
}
