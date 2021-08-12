package mrl.motion.dp;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.DynamicProgrammingMulti.GoalDimensionInfo;
import mrl.motion.position.PositionMotion;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class DirectionVelocityDP extends DynamicProgrammingMulti{
	
	public static double MAX_VEL = 30;
	
	
	static int BIG_ERROR_MARGIN = 2;
	static double BIG_ERROR_WEIGHT = 100;
	static double rotWeight = 0.01*Math.toDegrees(1)*Math.toDegrees(1);
	static double velWeight = 0.05;
	
	public DirectionVelocityDP(MDatabase database) {
		super(database);
	}
	
	@Override
	GoalDimensionInfo[] getDimensionInfo() {
		return new GoalDimensionInfo[] {
				// Direction
				new GoalDimensionInfo(20, -Math.PI, Math.PI, true),
				// Velocity
				new GoalDimensionInfo(5, -MAX_VEL, MAX_VEL, false),
		};
	}
	
	@Override
	double[] motionMovement(int nodeIndex) {
		Motion target = nodeList[nodeIndex].motion;
		Motion source = target.prev;
		
		Pose2d sPose = PositionMotion.getPose(source);
		Pose2d tPose = PositionMotion.getPose(target);
		double angle = MathUtil.directionalAngle(sPose.direction, tPose.direction);
		double vel = sPose.globalToLocal(tPose.position).x;
		
		return new double[] {angle, vel};
	}

	@Override
	double getError(int remainTime, int nodeIndex, double[] goal) {
		double bWeight = 1;
		if (remainTime < BIG_ERROR_MARGIN) {
			int b2 = BIG_ERROR_MARGIN + BIG_ERROR_MARGIN;
			bWeight = (b2 - remainTime)/(double)b2 * BIG_ERROR_WEIGHT;
		}
		double rotError = goal[0] * goal[0];
		double velError = goal[1] * goal[1];
		
		double error = rotError*rotWeight + velError*velWeight;
		return error * bWeight;
	}

}
