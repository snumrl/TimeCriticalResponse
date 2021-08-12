package mrl.motion.neural.agility.match;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.vecmath.Vector2d;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.dp.TransitionData.TransitionNode;
import mrl.motion.neural.agility.predict.MotionPredictor;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class OriginalMotionMatching implements MotionPredictor{
	
	public static boolean USE_BLENDING = false;
	public static boolean USE_OPTIMAL = false;
	public static int SEARCH_TIME_LIMIT = 30;
	public static int TIME_WEIGHT = 2;

	public TransitionData tData;
	public Motion current;
	public Pose2d currentPose;
	public ArrayList<Motion> motionList = new ArrayList<Motion>();
	
	public double controlWeight = 100;
	public double controlPow = 0;
	
	public double nearWeight = 50;
	
	private double[][] ftList;
	private LinkedList<Motion> queue = new LinkedList<Motion>();

	public OriginalMotionMatching(TransitionData tData) {
		this.tData = tData;

		
		int timeMargin = 30;
		Pose2d[] poseList = new Pose2d[tData.mList.length];
		for (int i = 0; i < poseList.length; i++) {
			poseList[i] = Pose2d.getPose(tData.mList[i]);
		}
		ftList = new double[tData.mList.length][];
		for (int mIdx = 0; mIdx < ftList.length; mIdx++) {
			int tAfter = tData.transitionAfterMIndex[mIdx];
			if (tAfter < 0) continue;
			if (tData.motionActionTypes[mIdx] < 0 || tData.motionActionTypes[tAfter] < 0) continue;
			Motion m = tData.mList[mIdx];
			if (m.frameIndex + timeMargin >= m.motionData.motionList.size() - 1) continue;
			double[] rotSum = new double[timeMargin+1];
			for (int i = 0; i < rotSum.length; i++) {
//				double a1 = MotionTransform.alignAngle(tData.mList[mIdx + i - 1].root());
//				double a2 = MotionTransform.alignAngle(tData.mList[mIdx + i - 1].root());
				double r = MathUtil.directionalAngle(poseList[mIdx + i - 1].direction,  poseList[mIdx + i].direction);
				if (i == 0) {
					rotSum[i] = r;
				} else {
					rotSum[i] = rotSum[i-1] + r;
				}
			}
			
			double[] futureTrajectory = {
					rotSum[10], rotSum[20], rotSum[30], 
			};
			if (USE_OPTIMAL) {
				futureTrajectory = rotSum;
//				futureTrajectory = Utils.cut(rotSum, 1, rotSum.length-1);
			}
			ftList[mIdx] = futureTrajectory;
		}
		
	}
	
	public void preCalcDist() {
		for (int i = 0; i < tData.tNodeList.length; i++) {
			if ((i % 10) == 0) {
				System.out.println("precompute :: " + i + " / " + tData.tNodeList.length);
			}
			tData.transitionDistance(tData.tNodeList[i].motionIndex());
		}
	}
	
	
	public void setStartMotion(Motion motion) {
		current = Pose2d.getAlignedMotion(motion);
		motionList.add(current);
		currentPose = Pose2d.getPose(current);
	}
	
	public void update(Vector2d targetDirection) {
		double targetRotation = MathUtil.directionalAngle(currentPose.direction, targetDirection);
		update(targetRotation);
	}
	public void update(double targetRotation) {
		if (queue.isEmpty()) {
			double[] futureTrajectory = new double[3];
			for (int i = 0; i < futureTrajectory.length; i++) {
				futureTrajectory[i] = targetRotation*(i+1)/(double)futureTrajectory.length;
			}
			
			double[] cache = tData.transitionDistance(current.motionIndex);
			double minDist = Integer.MAX_VALUE;
			int minIdx = -1;
			for (int i = 0; i < cache.length; i++) {
				TransitionNode node = tData.tNodeList[i];
				double d = cache[i];
				double[] nft = ftList[node.motionIndex()];
				if (nft == null) continue;
				double fd;
				if (USE_OPTIMAL) {
					fd = optimalDiff(targetRotation, nft);
				} else {
					fd = futureDiff(futureTrajectory, nft);
				}
				d += fd;
				if (d < minDist) {
					minDist = d;
					minIdx = node.motionIndex();
				}
			}
			if (minIdx < 0) {
				throw new RuntimeException();
			}
			
			if (!USE_BLENDING) {
				for (int i = 0; i <= Configuration.BLEND_MARGIN; i++) {
					queue.add(tData.mList[minIdx + i]);
				}
			} else {
				ArrayList<Motion> seq1 = new ArrayList<Motion>();
				seq1.add(current);
				ArrayList<Motion> seq2 = new ArrayList<Motion>();
				seq2.add(current);
				int originMIdx = current.motionIndex+1;
				for (int i = 0; i <= Configuration.BLEND_MARGIN; i++) {
					seq1.add(Motion.stitchMotion(Utils.last(seq1), tData.mList[originMIdx + i]));
					seq2.add(Motion.stitchMotion(Utils.last(seq2), tData.mList[minIdx + i]));
	//				seq1.add(Motion.stitchMotion(Utils.last(seq1), Utils.last(seq1).next));
	//				seq2.add(Motion.stitchMotion(Utils.last(seq2), tData.mList[minIdx + i]));
				}
				for (int i = 1; i < seq2.size(); i++) {
					double r = i/(double)(Configuration.BLEND_MARGIN+1);
	//				double r = 1 - i/(double)Configuration.BLEND_MARGIN;
					Motion m = Motion.interpolateMotion(seq1.get(i), seq2.get(i), r);
					queue.add(m);
	//				queue.add(tData.mList[minIdx + i]);
				}
			}
		}
		
		Motion m = queue.removeFirst();
		if (USE_BLENDING) {
			current = m;
		} else {
			current = Motion.stitchMotion(current, m);
		}
		currentPose = Pose2d.getPose(current);
		motionList.add(current);
	}
	
	double optimalDiff(double target, double[] rotList) {
		double minDiff = Integer.MAX_VALUE;
		
		double finishTime = -1;
		for (int i = Configuration.BLEND_MARGIN; i < SEARCH_TIME_LIMIT; i++) {
			double rSum = rotList[i];
			double rotation = target;
			finishTime = i;
			if ((Math.abs(rSum) > Math.abs(rotation) && (Math.signum(rSum) == Math.signum(rotation)))) {
				break;
			}
//			double d = Math.abs(rotList[i] - target);
////			double tWeight = (SEARCH_TIME_LIMIT + i)/(double)(SEARCH_TIME_LIMIT*2);
////			d *= tWeight;
//			minDiff = Math.min(d, minDiff);
		}
		
		double dSum = 0;
		for (int i = Configuration.BLEND_MARGIN; i < SEARCH_TIME_LIMIT; i++) {
			double d = Math.abs(rotList[SEARCH_TIME_LIMIT] - target);
			dSum += d*d;
		}
		
		dSum /= SEARCH_TIME_LIMIT;
		
		double dd = rotList[Configuration.BLEND_MARGIN] - target;
		dSum += dd*dd*nearWeight;
//		finishTime = 0;
		
		dSum *= 100/(Math.PI);
//		dSum *= 100/(Math.PI)*10;
		finishTime = finishTime*finishTime;
		
		
		return (dSum + finishTime)*controlWeight;
//		
//		if (finishTime >= 0) {
//			minDiff = finishTime;
//		} else {
//			double d = Math.abs(rotList[SEARCH_TIME_LIMIT] - target);
//			minDiff = SEARCH_TIME_LIMIT + d*30/Math.PI;
//		}
//		minDiff *= controlWeight;
////		minDiff *= controlWeight/(Math.PI);
//		return minDiff*minDiff;
	}
	
	double futureDiff(double[] f1, double[] f2) {
		double dSum = 0;
		double pSum = 0;
		for (int i = 0; i < f2.length; i++) {
			double d = f1[i] - f2[i];
			d *= controlWeight/(Math.PI);
			d = d*d;
			double p = Math.pow(i+1, controlPow);
			d *= p;
			pSum += p;
			dSum += d;
		}
		dSum /= pSum;
		return dSum/f1.length;
	}


	@Override
	public Motion predictMotion(double[] x) {
		double rot = x[0];
		update(rot);
		return current;
	}


	@Override
	public Pose2d currentPose() {
		return currentPose;
	}


	@Override
	public double currentActivation() {
		return 0;
	}
	
	
//	double futureDiff(double[] f1, double[] f2) {
//		double dSum = 0;
//		double pSum = 0;
//		for (int i = 0; i < f2.length; i++) {
//			double d = f1[i] - f2[i];
//			d *= controlWeight/(Math.PI);
//			d = d*d;
//			double p = Math.pow(i+1, controlPow);
//			d *= p;
//			pSum += p;
//			dSum += d;
//		}
//		dSum /= pSum;
//		return dSum/f1.length;
//	}
//	
	
}
