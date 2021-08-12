package mrl.motion.neural.gmm;

import javax.vecmath.Point2d;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.rl.PolicyEvaluation.MatchingPath;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class GMMLocoConfig extends GMMConfig{

	public static String[] ACTION_LABELS = { "jog" };
	
	public GMMLocoConfig(String name) {
		super(name, ACTION_LABELS, ACTION_LABELS, ACTION_LABELS.length);
	}
	
	public GMMGoal randomGoal() {
		return new GMMLocoGoal(baseTimeLimit, Utils.rand1()*Math.PI);
	}
	
	@Override
	public GMMGoalGenerator makeGoalGenerator() {
		return new GMMGoalGenerator() {
			@Override
			public GMMGoal sampleRandomGoal(Pose2d currentPose, Motion currentMotion) {
				return randomGoal();
			}
		};
	}
	

	public class GMMLocoGoal extends GMMGoal{

		public GMMLocoGoal(int timeLimit, double direction) {
			super(0, timeLimit, direction);
		}
		
		@Override
		public double evaluateFinalError(MatchingPath path, double futureRotation, Pose2d futurePose, Motion finalMotion) {
			double d = direction - (path.rotation + futureRotation);
			return d*d;
		}
		
	}
	
}
