package mrl.motion.neural.agility.match;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.agility.RotationModel.RotationGoal;
import mrl.util.Utils;

public class RotationMotionMatching extends MotionMatching{
	
	public static int STRAIGHT_MARGIN = 3;
	
	public static double rotErrorRatio = 5;
	protected boolean[] isStraightList;
	protected double[][] rotateCache;

	public RotationMotionMatching(MDatabase database) {
		super(database);
		
		int margin = STRAIGHT_MARGIN;
		isStraightList = new boolean[mList.length];
		if (margin <= 0) {
			for (int i = 0; i < isStraightList.length; i++) {
				isStraightList[i] = true;
			}
		} else {
			for (MotionData mData : database.getMotionDataList()) {
				for (int i = 0; i < mData.motionList.size() - margin; i++) {
					boolean isStraight = false;
					if (i > margin) {
						isStraight = RotationModel.isStraight(Utils.cut(mData.motionList, i-(margin), i + (margin)), 20);
					}
					isStraightList[mData.motionList.get(i).motionIndex] = isStraight;
				}
			}
			boolean[] straight = new boolean[isStraightList.length];
			for (int i = 0; i < straight.length; i++) {
				boolean isAllStraight = true;
				for (int j = 0; j <= margin*2; j++) {
					int idx = Math.min(i + j, straight.length-1);
					if (mList[i].motionData != mList[idx].motionData) break;;
					if (!isStraightList[idx]) {
						isAllStraight = false;
						break;
					}
				}
				straight[i] = isAllStraight;
			}
			isStraightList = straight;
		}
		
		rotateCache = new double[mList.length][];
		for (int i = 0; i < mList.length; i++) {
			rotateCache[i] = calcRotateCache(mList[i]);
		} 
	}

	protected double[] calcRotateCache(Motion motion) {
		double invalid = 999;
		double rotSum = 0;
		double[] cache = new double[MAX_TIME_INTERVAL];
		for (int i = 0; i < MAX_TIME_INTERVAL; i++) {
			if (motion.next == null) {
				cache[i] = invalid;
			} else {
				rotSum += getRotation(motion, motion.next);
				if (isStraightList[motion.motionIndex] && isContainedMotion[motion.motionIndex]) {
					cache[i] = rotSum;
				} else {
					cache[i] = invalid;
				}
				motion = motion.next;
			}
		}
		
		return cache;
	}
	
	public static double getRotation(Motion source, Motion target) {
		double sAlignAngle = -MotionTransform.alignAngle(source.root());
		double tAlignAngle = -MotionTransform.alignAngle(target.root());
		
		double diff = tAlignAngle - sAlignAngle;
		if (diff > Math.PI){
			diff -= Math.PI*2;
		} else if (diff < -Math.PI){
			diff += Math.PI*2;
		}
		return diff;
	}

	@Override
	protected boolean isValidEnd(Motion motion, AgilityGoal goal) {
		return isStraightList[motion.motionIndex];
	}
	
	@Override
	protected double getSpatialError(MatchingPath path, AgilityGoal goal, Motion transitionMotion, int timeOffset, boolean isSequential) {
		double targetRot = ((RotationGoal)goal).targetRotation - path.rotation;
		double rotOffset = 0;
		if (timeOffset > 0) {
			double[] rotCache = rotateCache[transitionMotion.motionIndex];
			int remainTime = timeOffset;
			if (remainTime >= rotCache.length ) {
				System.out.println(remainTime + " : " + rotCache.length);
			}
			if (Double.isNaN(rotCache[remainTime])) return Integer.MAX_VALUE;
			rotOffset = rotCache[remainTime];
		}
		double diff = Math.abs(targetRot - rotOffset);
		double d = diff*rotErrorRatio;
		return d*d;
	}

	@Override
	protected double getActionError(MatchingPath path, AgilityGoal goal, Motion transitionMotion, int timeOffset, boolean isSequential) {
		return 0;
	}

}
