package mrl.motion.neural.agility;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.ActionDistDP;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.position.PositionMotion;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class FightModel extends AgilityModel {
	
	public static int STRAIGHT_MARGIN = 3;
	
	public static double POS_ERROR_RATIO = 5;
	
	public static double BASE_TARGET_LENGTH = 150;
	public static boolean USE_STRAIGHT_SAMPLING = false;
	
	public static boolean USE_POSITON_CONTROL = true;
	
	
	public static String[] actionTypes = {
			"kick",
			"punch"
	};
	
	private boolean sampleStraight = false;
	private String dpLabel;
	
	public FightModel(String dpLabel) {
		this.dpLabel = dpLabel;
	}
	
	public FightGoal newGoal(Point2d target, int actionType, int timeLimit) {
		return new FightGoal(target, actionType, timeLimit);
	}
	
	@Override
	public FightGoal sampleIdleGoal() {
		throw new RuntimeException();
	}
	
	@Override
	public FightGoal sampleRandomGoal(Pose2d currentPose) {
		double targetLength = BASE_TARGET_LENGTH;
		if (MathUtil.random.nextDouble() < 0.5) {
			targetLength = targetLength*MathUtil.random.nextDouble();
		} else {
			targetLength = targetLength*(1 + Utils.rand1()*0.25);
		}
		Vector2d v = new Vector2d(targetLength, 0);
		double rotation = MathUtil.random.nextDouble()*Math.PI*2;
		if (USE_STRAIGHT_SAMPLING) {
			if (sampleStraight && MathUtil.random.nextDouble() < 0.8) {
				rotation /= 20;
			}
			sampleStraight = !sampleStraight;
			if (Math.abs(rotation) < Math.PI / 4) {
				sampleStraight = false;
			}
		}
		
		v = MathUtil.rotate(v, rotation);
		int actionType = MathUtil.random.nextInt(actionTypes.length);
		
		int timeLimit = MathUtil.round(GOAL_TIME_LIMIT + Utils.rand1()*1);
		System.out.println("random goal :: " + v + " : " + timeLimit);
		return new FightGoal(new Point2d(v), actionType, timeLimit);
	}

	@Override
	public FightMotionMatching makeMotionMatching(MDatabase database) {
		ActionDistDP dp = new ActionDistDP(database, actionTypes);
		dp.load("..\\mrl.motion.data\\" + dpLabel + ".dat");
		return new FightMotionMatching(database, dp);
	}
	
	public class FightGoal extends AgilityGoal{
		public Point2d target;
		public FightGoal(Point2d target, int actionType, int timeLimit) {
			super(actionType, timeLimit);
			this.target = target;
		}
		@Override
		public Pose2d getEditingConstraint() {
			if (USE_POSITON_CONTROL) {
				return new Pose2d(target.x, target.y, Double.NaN, Double.NaN);
			} else {
				return new Pose2d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
			}
		}
		
		
		@Override
		public boolean isGoalFinished(Pose2d startPose, ArrayList<Motion> motionList, ArrayList<Double> activationList) {
			Pose2d pose = PositionMotion.getPose(Utils.last(motionList));
			double d = startPose.globalToLocal(pose.position).distance(target);
			return d < 30;
		}
		@Override
		public double[] getControlParameter(ArrayList<Pose2d> poseList) {
			Point2d global = poseList.get(0).localToGlobal(target);
			Point2d p = Utils.last(poseList).globalToLocal(global);
			return new double[] { p.x, p.y };
		}
		@Override
		public Object getControlParameterObject(Pose2d startPose, Pose2d currentPose) {
			Point2d global = startPose.localToGlobal(target);
			return Pose2d.to3d(global);
		}
		
		@Override
		public boolean isActiveAction() {
			return true;
		}
		@Override
		public double[] getControlParameter(ArrayList<Motion> motionSequence, int currentIndex, int targetIndex) {
			throw new RuntimeException();
		}
	}
	
	
	public static class FightMotionMatching extends MotionMatching{

		private Matrix2d[][] spatialCache;
		private double[][] translationLengthCache;
		public ActionDistDP dp;
		
		public FightMotionMatching(MDatabase database, ActionDistDP dp) {
			super(database);
			this.dp = dp;
			
			spatialCache = new Matrix2d[mList.length][];
			translationLengthCache = new double[mList.length][];
			for (int i = 0; i < mList.length; i++) {
				if ((i%100) ==0) {
					System.out.println("spatial cache :: " + i);
				}
				if (isContainedMotion[i]) {
					calcSpatialCache(i);
				}
			} 
		}
		
		private void calcSpatialCache(int mIndex) {
			Motion motion = mList[mIndex];
			Matrix2d invalid = null;
			Matrix2d[] cache = new Matrix2d[MAX_TIME_INTERVAL];
			double[] tCache = new double[MAX_TIME_INTERVAL];
			Matrix2d m = Matrix2d.identity();
			cache[0] = new Matrix2d(m);
			double translationSum = m.getTranslation().length();
			tCache[0] = translationSum;
			for (int i = 1; i < MAX_TIME_INTERVAL; i++) {
				if (motion.next == null) {
					cache[i] = invalid;
				} else {
					m.mul(adjRootTransform2d[motion.motionIndex]);
					translationSum += adjRootTransform2d[motion.motionIndex].getTranslation().length();
//					if (isContainedMotion[motion.motionIndex]) {
						cache[i] = new Matrix2d(m);
						tCache[i] = translationSum;
//					} else {
//						cache[i] = invalid;
//					}
					motion = motion.next;
				}
			}
			spatialCache[mIndex] = cache;
			translationLengthCache[mIndex] = tCache;
		}

		@Override
		protected double getSpatialError(MatchingPath path, AgilityGoal _goal, Motion transitionMotion, int timeOffset, boolean isSequential) {
			if (!USE_POSITON_CONTROL) return 0;
			
			FightGoal goal = (FightGoal)_goal;
			Point2d targetPos = goal.target;
			
			Matrix2d t = path.transform;
			double transLen = path.translationLength;
			if (timeOffset > 0) {
				if (timeOffset < ActionDistDP.NO_TRANSITION_BEFORE_ACTION_MARGIN && 
						dp.directTimeDistance(goal.actionType, transitionMotion) < timeOffset) {
					return Integer.MAX_VALUE;
				}
				Matrix2d[] sCache = spatialCache[transitionMotion.motionIndex];
				int remainTime = timeOffset;
				if (sCache[remainTime] == null) return Integer.MAX_VALUE;
				t = new Matrix2d(t);
				t.mul(sCache[remainTime]);
				transLen += translationLengthCache[transitionMotion.motionIndex][remainTime]; 
			}
			double targetLen = Math.max(50, MathUtil.length(targetPos));
			
			double dPos = MathUtil.distance(targetPos, t.getTranslation())/targetLen;
			double dTLen = (transLen - targetLen)/ targetLen;
			if (dTLen > 0) {
				dTLen *= 3;
			}
			double d1 = dPos*POS_ERROR_RATIO;
			double d2 = dTLen*POS_ERROR_RATIO;
			return d1*d1 + d2*d2;
		}
		
		protected void log(MatchingPath path, NextMotion next, AgilityGoal _goal) {
			FightGoal goal = (FightGoal)_goal;
			int dtd = dp.directTimeDistance(goal.actionType, next.motion);
			System.out.println("directTimeDistance : " + path.time + " : " + next.motion + " : " + dtd);
		}
		

		@Override
		protected double getActionError(MatchingPath path, AgilityGoal _goal, Motion transitionMotion, int timeOffset, boolean isSequential) {
			if (transitionMotion == null) {
				// already reached action frame
				return 0;
			}
			int remainTime = timeOffset;
			FightGoal goal = (FightGoal)_goal;
			if (remainTime < Configuration.MOTION_TRANSITON_MARGIN*2) {
				int time = dp.directTimeDistance(goal.actionType, transitionMotion);
				if (time > remainTime) {
					return Integer.MAX_VALUE;
				}
			}
			if (dp.directTimeDistance(goal.actionType, transitionMotion) < remainTime) {
				return Integer.MAX_VALUE;
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
			return dp.getMotionActionType(motion) == ((FightGoal)goal).actionType;
		}
		
	}

}
