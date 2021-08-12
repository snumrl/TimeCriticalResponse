package mrl.motion.neural.gmm;

import javax.vecmath.Point2d;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.rl.PolicyEvaluation.MatchingPath;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class GMMLocoActionConfig extends GMMConfig{
	
	public static String[] ACTION_LABELS = { "idle", "walk", "jog", "run" };

	public GMMLocoActionConfig(String name) {
		super(name, ACTION_LABELS, ACTION_LABELS, ACTION_LABELS.length);
	}
	
	@Override
	public GMMGoalGenerator makeGoalGenerator() {
		return new LocoActionGoalGenerator();
	}
	
	private static int stopIndex() {
		return 0;
	}
	
	public static boolean USE_STRAIGHT_SAMPLING = true;
	private class LocoActionGoalGenerator extends GMMGoalGenerator{
		
		
		private boolean sampleStraight = false;
		private int lastAction = 0;
		private int actionCount = 0;
		
		public LocoActionGoalGenerator() {
			lastAction = MathUtil.random.nextInt(getActionSize());
		}

		@Override
		public GMMGoal sampleRandomGoal(Pose2d currentPose, Motion currentMotion) {
			double rotation = RotationModel.sampleRotation();
			if (USE_STRAIGHT_SAMPLING) {
				if (sampleStraight && (MathUtil.random.nextDouble() < 0.7)) {
					rotation /= 60;
				}
			}
			
			actionCount++;
			int stopIndex = stopIndex();
			if (actionCount > 5) {
				int action;
				rotation = RotationModel.sampleRotation();
				if (lastAction == stopIndex) {
					action = 1 + MathUtil.random.nextInt(getActionSize() - 1);
				} else {
					if (MathUtil.random.nextDouble() < 0.7) {
						action = stopIndex;
					} else {
						action = MathUtil.random.nextInt(getActionSize() - 1);
						rotation /= 20;
					}
				}
				lastAction = action;
				actionCount = 0;
			} else {
				if (lastAction == stopIndex) {
					actionCount += 3;
					rotation /= 60;
				}
			}
			
			if (Math.abs(rotation) >= Math.PI / 4) {
				sampleStraight = true;
			} else {
				sampleStraight = false;
			}
			int timeLimit = baseTimeLimit;
			return new GMMLocoActionGoal(lastAction, timeLimit, rotation);
		}
		
	}

	public class GMMLocoActionGoal extends GMMGoal{

		public GMMLocoActionGoal(int actionType, int timeLimit, double direction) {
			super(actionType, timeLimit, direction);
			if (actionType == stopIndex()) direction = 0;
			this.direction = direction;
		}
		
		@Override
		public double evaluateFinalError(MatchingPath path, double futureRotation, Pose2d futurePose, Motion finalMotion) {
			if (!isDirectionControl()) return 0;
			double d = direction - (path.rotation + futureRotation);
			return d*d;
		}

		@Override
		public boolean isDirectionControl() {
			return !isActiveAction() && actionType != stopIndex();
		}
		
	}
}
