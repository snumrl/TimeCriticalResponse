package mrl.motion.dp;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.Configuration;
import mrl.util.MathUtil;

public class DirectionActionDP extends DynamicProgrammingMulti{
	
	static int BIG_ERROR_MARGIN = 3;
	static double BIG_ERROR_WEIGHT = 100;
	static double rotWeight = 0.1*Math.toDegrees(1)*Math.toDegrees(1);
	
	int[] actionLabel;
	double[] angleOffsetList;
	
	public DirectionActionDP(MDatabase database) {
		super(database);
		
		actionLabel = new int[nodeList.length];
		angleOffsetList = new double[nodeList.length];
		
		for (MotionAnnotation ann : database.getTransitionAnnotations()) {
			int actionType = 0;
			if (ann.type.equals("kick")) actionType = 1;
			if (ann.type.equals("punch")) actionType = 2;
			if (actionType > 0) {
				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
				actionLabel[fMotionMap[mIndex].index] = actionType;
				
				Motion originMotion = database.findMotion(ann.file, ann.interactionFrame);
				Motion targetMotion = database.findMotion(ann.getOppositeFile(), ann.interactionFrame);
				
				Matrix4d t1 = originMotion.root();
				Matrix4d t2 = targetMotion.root();
				
				Vector3d p1 = MathUtil.getTranslation(t1);
				Vector3d p2 = MathUtil.getTranslation(t2);
				
				Vector2d v = new Vector2d(p2.x - p1.x, p2.z - p1.z);
				double angleOffset = Math.atan2(-v.y, v.x);
				if (Double.isNaN(angleOffset)) throw new RuntimeException();
				angleOffsetList[fMotionMap[mIndex].index] = angleOffset;
			}
		}
	}
	
	@Override
	GoalDimensionInfo[] getDimensionInfo() {
		int actionSize = 3; // Locomotion, Kick, Punch
		return new GoalDimensionInfo[] {
				// Direction
				new GoalDimensionInfo(30, -Math.PI, Math.PI, true),
				// Velocity
				new GoalDimensionInfo(actionSize, 0, actionSize-1, false),
		};
	}
	
	@Override
	double[] motionMovement(int nodeIndex) {
		Motion target = nodeList[nodeIndex].motion;
		Motion source = target.prev;
		
		Pose2d sPose = PositionMotion.getPose(source);
		Pose2d tPose = PositionMotion.getPose(target);
		double angle = MathUtil.directionalAngle(sPose.direction, tPose.direction);
		
		return new double[] {angle, 0};
	}

	@Override
	double getError(int remainTime, int nodeIndex, double[] goal) {
		double bWeight = 1;
		if (remainTime < BIG_ERROR_MARGIN) {
			int b2 = BIG_ERROR_MARGIN + BIG_ERROR_MARGIN;
			bWeight = (b2 - remainTime)/(double)b2 * BIG_ERROR_WEIGHT;
		}
		double rotError = goal[0] * goal[0];
		double actionError = 0;
		
		int action = actionLabel[nodeIndex];
		if (remainTime == 0) {
			if (action != (int)goal[1]){
				actionError = Integer.MAX_VALUE/2;
			}
		} else if (goal[1] == 0 && actionPrefix[nodeIndex] != 0) {
			actionError = Integer.MAX_VALUE/2;
		}
		
		
		double error = rotError*rotWeight*bWeight + actionError;
		if (heightList[nodeIndex] < 40) {
			error += 500;
		}
		return error;
	}

}
