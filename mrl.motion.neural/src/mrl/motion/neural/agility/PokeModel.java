package mrl.motion.neural.agility;

import java.util.ArrayList;

import javax.vecmath.Vector2d;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.ActionDistDP;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class PokeModel extends AgilityModel {

	public static String[] actionTypes = {
			"i",
			"",
			"k",
			"th",
			"b",
			"j",
//			"n", // not use
	};
	
	protected String dpLabel;
	protected int lastAction = 0;
	public PokeModel(String dpLabel) {
		this.dpLabel = dpLabel;
		FootContactDetection.footDistPointOffset = new int[]{
				30, 100, 0
		};
	}
	
	public boolean useActivation() {
		return true;
	}
	
	@Override
	public AgilityGoal sampleRandomGoal(Pose2d currentPose) {
		int action = MathUtil.random.nextInt(actionTypes.length);
//		if (MathUtil.random.nextDouble() < 0.3) {
//			action = 0;
//		}
		return new PokeGoal(action, GOAL_TIME_LIMIT);
	}

	@Override
	public AgilityGoal sampleIdleGoal() {
		return new PokeGoal(0, GOAL_TIME_LIMIT);
	}

	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		ActionDistDP.NO_MARGIN_ACTION_SIZE = 1;
		
		ActionDistDP dp = new ActionDistDP(database, Utils.concatenate(actionTypes, new String[] { "n" }));
		dp.load("..\\mrl.motion.data\\" + dpLabel + ".dat");
		return new PokeMotionMatching(database, dp);
	}
	
	public int getActionSize() {
		return actionTypes.length;
	}
	
	public class PokeGoal extends AgilityGoal{
		
		public PokeGoal(int actionType, int timeLimit) {
			super(actionType, timeLimit);
		}

		@Override
		public Pose2d getEditingConstraint() {
			return new Pose2d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
		}
		
		@Override
		public double[] getControlParameter(ArrayList<Motion> motionSequence, int currentIndex, int targetIndex) {
			return actionData(actionType);
		}
		

		@Override
		public boolean isGoalFinished(Pose2d startPose, ArrayList<Motion> motionList, ArrayList<Double> activationList) {
			return Utils.last(activationList) > 0.5;
		}

		@Override
		public double[] getControlParameter(ArrayList<Pose2d> poseList) {
			return actionData(actionType);
		}


		@Override
		public Object getControlParameterObject(Pose2d startPose, Pose2d currentPose) {
			Pose2d p = new Pose2d(startPose);
			p.direction = MathUtil.rotate(new Vector2d(1, 0), actionType*(Math.PI/4));
			return p;
		}
		
		public String toString() {
			return "PokeGoal:" + actionType + ", " + timeLimit;
		}
		
		public boolean isActiveAction() {
			return actionType > 0;
		}
	}
	
	public class PokeMotionMatching extends MotionMatching{
		
		ActionDistDP dp;
		boolean[] isNearAction;

		public PokeMotionMatching(MDatabase database, ActionDistDP dp) {
			super(database);
			this.dp = dp;
			isNearAction = new boolean[mList.length];
			for (MotionAnnotation ann : database.getEventAnnotations()) {
				if (ann.type.equals(actionTypes[0])) continue;
				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
				int prevMargin = 8;
				int postMargin = 4;
				for (int i = 1; i < prevMargin; i++) {
					int idx = mIndex - i;
					if (idx < 0) break;
					if (mList[idx].next != mList[idx+1]) break;
					isNearAction[idx] = true;
				}
				for (int i = 1; i < postMargin; i++) {
					int idx = mIndex + i;
					if (idx >= mList.length) break;
					if (mList[idx].prev != mList[idx-1]) break;
					isNearAction[idx] = true;
				}
			}
			
//			int action = 1;
//			for (int i = 0; i < dp.nodeList.length; i++) {
//				System.out.println(dp.nodeList[i].motion + " : " + dp.distanceTable[action][0][i]);
//			}
//			System.exit(0);
		}
		
		protected double adjustDistance(double distance, int sourceMIndex, int targetMIndex) {
			if (isNearAction[sourceMIndex]) return 9999;
			if (isNearAction[targetMIndex]) return 9999;
			
			int backwardLimit = ActionDistDP.NO_BACKWARD_TRANSITION_MARGIN;
			int nextMotionIndex = sourceMIndex + 1;
			if (targetMIndex < nextMotionIndex && targetMIndex > nextMotionIndex - backwardLimit
					&& mList[targetMIndex].frameIndex >= backwardLimit) {
				distance += 9000;
			}
			return distance;
		}

		@Override
		protected double getSpatialError(MatchingPath path, AgilityGoal goal, Motion transitionMotion, int timeOffset,
				boolean isSequential) {
			return 0;
		}

		@Override
		protected double getActionError(MatchingPath path, AgilityGoal _goal, Motion transitionMotion, int timeOffset,
				boolean isSequential) {
			if (transitionMotion == null) {
				// already reached action frame
				return 0;
			}
			
			if (!isSequential && isNearAction[transitionMotion.motionIndex]) return Integer.MAX_VALUE;
			
			int remainTime = timeOffset;
			PokeGoal goal = (PokeGoal)_goal;
			if (remainTime < Configuration.MOTION_TRANSITON_MARGIN*2) {
				int time = dp.directTimeDistance(goal.actionType, transitionMotion);
				if (time > remainTime) {
					if (isSequential) {
						log("tttrr1 : " + transitionMotion + " : " + time + " / " + remainTime);
					}
					return Integer.MAX_VALUE;
				}
			}
			for (int i = 0; i < actionTypes.length; i++) {
				if (i == goal.actionType) continue;
				if (dp.actionDirectTime[i][transitionMotion.motionIndex] <= Configuration.MOTION_TRANSITON_MARGIN) {
					if (isSequential) {
						log("no action : " + transitionMotion + " : " + dp.actionDirectTime[i][transitionMotion.motionIndex] + " / " + remainTime);
					}
					return Integer.MAX_VALUE;
				}
			}
			
			int currentType = dp.motionActionTypes[path.current.motionIndex];
			int targetType = dp.motionActionTypes[transitionMotion.motionIndex];
			if (goal.isActiveAction()) {
				if (dp.directTimeDistance(goal.actionType, transitionMotion) < remainTime) {
					if (isSequential) {
						log("tttrr2 : " + transitionMotion + " : " + dp.directTimeDistance(goal.actionType, transitionMotion) + " / " + remainTime);
					}
					return Integer.MAX_VALUE;
				}
			} else {
				if (currentType == goal.actionType) {
					if (targetType != goal.actionType) {
						if (isSequential) {
							log("no goal : " + transitionMotion + " : " + dp.directTimeDistance(goal.actionType, transitionMotion) + " / " + remainTime);
						}
						return Integer.MAX_VALUE;
					}
				}
				if (timeOffset <= Configuration.MOTION_TRANSITON_MARGIN*3) {
					int finalIndex = transitionMotion.motionIndex + timeOffset;
					if (finalIndex >= mList.length || mList[finalIndex].motionData != transitionMotion.motionData) return Integer.MAX_VALUE;
					int finalType = dp.motionActionTypes[finalIndex];
					if (finalType != goal.actionType) {
						if (isSequential) {
							log("tttrr3 : " + transitionMotion + " : " + dp.directTimeDistance(goal.actionType, transitionMotion) + " / " + remainTime);
						}
						return Integer.MAX_VALUE;
					}
				}
			}
			
			
			double diff;
			if (isSequential) {
				int time = dp.directTimeDistance(goal.actionType, transitionMotion);
				diff = dp.getDistance(goal.actionType, remainTime, transitionMotion.motionIndex);
//				System.out.println("Seq diff :: " + path.current + "->" + transitionMotion + " : " + timeOffset + " / " + time + " : " + diff + " : " + goal.actionType + " : " + path.time);
			} else {
				int tAfterIdx = dp.transitionAfterMIndex[transitionMotion.motionIndex];
				diff = dp.getDistance(goal.actionType, remainTime - Configuration.MOTION_TRANSITON_MARGIN, tAfterIdx);
			}
			
			return diff;
		}

		@Override
		protected boolean isValidEnd(Motion motion, AgilityGoal goal) {
			return dp.getMotionActionType(motion) == ((PokeGoal)goal).actionType;
		}
		
	}

}
