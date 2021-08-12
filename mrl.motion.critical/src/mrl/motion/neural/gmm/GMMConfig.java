package mrl.motion.neural.gmm;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionActionDP;
import mrl.motion.dp.TransitionData;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.TransitionDPGenerator;
import mrl.motion.neural.rl.PolicyEvaluation.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public abstract class GMMConfig {
	
	public static double TIME_RATIO = 1.5;
	public static boolean CONTROL_ACTION_DIRECTION = false;
	
	private String dataFolder;
	private String tPoseFile;
	
	public String name;
	public String[] fullActionLabels;
	public String[] actionLabels;
	public int continuousLabelSize;
	
	public int baseTimeLimit = 30;

	public TransitionData tData;
	public TransitionActionDP actionDP;
	public TransitionConstraints tConstraints;
	
	public GMMConfig(String name, String[] fullActionLabels, String[] actionLabels, int continuousLabelSize) {
		this.name = name;
		this.fullActionLabels = fullActionLabels;
		this.actionLabels = actionLabels;
		this.continuousLabelSize = continuousLabelSize;
	}
	
	public GMMConfig setDataFolder(String dataFolder, String tPoseFile) {
		this.dataFolder = dataFolder;
		this.tPoseFile = tPoseFile;
		
		MDatabase database = TrainingDataGenerator.loadDatabase(dataFolder, tPoseFile);
		tData = new TransitionData(database, fullActionLabels, continuousLabelSize);
		actionDP = new TransitionActionDP(tData);
		actionDP.load(TransitionDPGenerator.FILE_PREFIX + database.getDatabaseName() + ".dat");
		tConstraints = new TransitionConstraints(tData);
		return this;
	}
	
	public TransitionData getTransitionData() {
		return tData;
	}
	
	public int getActionSize() {
		return actionLabels.length;
	}
	
	public int getControlParameterSize() {
		/* rotation conrol */
		int directionControlSize = 1; 
		return getActionSize() + directionControlSize;
	}
	
	public String getDataFolder() {
		return dataFolder;
	}
	
	public abstract GMMGoalGenerator makeGoalGenerator();

	
	public static abstract class GMMGoalGenerator{
		public abstract GMMGoal sampleRandomGoal(Pose2d currentPose, Motion currentMotion);
	}
	
	public static double DIRECTION_MATCH_MARGIN = Math.toRadians(5); 
	public abstract class GMMGoal{
		public int actionType;
		public int timeLimit;
		public int maxSearchTime;
		public int minSearchTime;
		public double direction;
		
		public int dynamicTime = -1;
		
		public GMMGoal(int actionType, int timeLimit, double direction) {
			this.actionType = actionType;
			setTime(timeLimit);
			this.direction = direction;
			
			if (!isDirectionControl()) {
				direction = 0;
			}
			this.direction = direction;
		}
		
		public abstract double evaluateFinalError(MatchingPath path, double futureRotation, Pose2d futurePose, Motion finalMotion);
		
		public boolean isFinishable(MatchingPath path) {
			if (!isActiveAction() && path.time < AgilityModel.TIME_EXTENSION_MIN_TIME) return false;
			Motion m = path.current;
			boolean isActionMatch = tData.motionActionTypes[m.motionIndex] == actionType;
			return isActionMatch;
		}
		
		public boolean isFinished(MatchingPath path) {
			if (!isActiveAction() && path.time < AgilityModel.TIME_EXTENSION_MIN_TIME) return false;
			Motion m = path.current;
			boolean isActionMatch = tData.motionActionTypes[m.motionIndex] == actionType;
			boolean isNextActionMatch = tData.motionActionTypes[m.motionIndex+1] == actionType;
			if (isActiveAction()) return isActionMatch;
			
			if (isDirectionControl()) {
				if (Math.abs(path.rotation - direction) < DIRECTION_MATCH_MARGIN) {
					return isActionMatch;
				}
				int remainTime = maxSearchTime - path.time;
				if (remainTime <= (Configuration.MOTION_TRANSITON_MARGIN+1)*2) {
					if (isActionMatch && !isNextActionMatch) {
						return true;
					}
				}
				
				
//				if (remainTime <= 0) {
				if (remainTime <= Configuration.MOTION_TRANSITON_MARGIN+1) {
					return isActionMatch;
				}
				return false;
			}
			return isActionMatch;
		}
		
		public double[] getParameter(MatchingPath path) {
//			double[] dir =  new double[] { direction - path.rotation };
//			return MathUtil.concatenate(actionTypeArray(), dir);
			double[] control = actionTypeArray();
			double remain = 0;
			if (CONTROL_ACTION_DIRECTION && isActiveAction()) {
				remain = direction - getDirectionOffset(path.current) - path.rotation; 
			} else {
				if (isDirectionControl()) {
					remain = direction - path.rotation; 
				}
			}
			control = MathUtil.concatenate(control, new double[] { remain });
			return control;
		}
		
		public double[] getControlParameter(ArrayList<Motion> mList, int currentIndex, int targetIndex) {
			double rotSum;
			if (CONTROL_ACTION_DIRECTION && isActiveAction()) {
				rotSum = getDirectionOffset(mList.get(targetIndex));
//				System.out.println("get directional offset : " + 
//								Utils.toString(currentIndex, Math.toDegrees(rotSum), mList.get(targetIndex)));
				for (int i = currentIndex; i < targetIndex; i++) {
					Pose2d p = PositionMotion.getPose(mList.get(i));
					Pose2d next = PositionMotion.getPose(mList.get(i + 1));
					double angle = MathUtil.directionalAngle(p.direction, next.direction);
					rotSum += angle;
				}
				rotSum = MathUtil.trimAngle(rotSum);
			} else if (!isDirectionControl()) {
				rotSum = Double.NaN;
			} else {
				rotSum = 0;
//				rotSum = mList.get(targetIndex).directionOffset();
				for (int i = currentIndex; i < targetIndex; i++) {
					Pose2d p = PositionMotion.getPose(mList.get(i));
					Pose2d next = PositionMotion.getPose(mList.get(i + 1));
					double angle = MathUtil.directionalAngle(p.direction, next.direction);
					rotSum += angle;
				}
			}
			double[] control = actionTypeArray();
			control = MathUtil.concatenate(control, new double[] { rotSum });
			return control;
		}
		
		public Pose2d getEditingConstraint(MotionSegment segment) {
			if (CONTROL_ACTION_DIRECTION && isActiveAction()) {
				Vector2d v = new Vector2d(Pose2d.BASE.direction);
				v = MathUtil.rotate(v, direction - getDirectionOffset(segment.lastMotion()));
				Pose2d p = new Pose2d(new Point2d(Double.NaN, Double.NaN), v);
				return p;
			} else if (!isDirectionControl()) {
				return new Pose2d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
			} else {
				Vector2d v = new Vector2d(Pose2d.BASE.direction);
				v = MathUtil.rotate(v, direction);
				Pose2d p = new Pose2d(new Point2d(Double.NaN, Double.NaN), v);
				return p;
			}
		}
		
		public Object getParameterObject(Matrix2d startTransform, Matrix2d currentTransform) {
			Pose2d p = Pose2d.byBase(startTransform);
			p.direction = MathUtil.rotate(p.direction, direction);
			p.position = new Point2d(currentTransform.getTranslation());
			return p;
		}
		
		public boolean isValidTransition(MatchingPath currentPath, int targetMIndex) {
			return tConstraints.isValidTransition(currentPath.current.motionIndex, targetMIndex, actionType);
		}
		
		public double[] actionTypeArray() {
			double[] typeArray = new double[getActionSize()];
			typeArray[actionType] = 1;
			return typeArray;
		}
		
		public boolean isActiveAction() {
			return actionType >= continuousLabelSize;
		}
		
		public boolean isDirectionControl() {
			return !isActiveAction();
		}
		
		public double getDirectionOffset(Motion motion) {
			return motion._directionOffset();
		}
		
		public double getActivation(int remainTime) {
			double activation = 0;
			if (isActiveAction()) {
				int activMargin = AgilityModel.ACTIVATION_PEAK + AgilityModel.ACTIVATION_RISE;
				if (remainTime <= activMargin) {
					if (remainTime <= AgilityModel.ACTIVATION_PEAK) {
						activation = 1;
					} else {
						activation = 1 - (remainTime - AgilityModel.ACTIVATION_PEAK)/(double)(AgilityModel.ACTIVATION_RISE + 1);
					}
				}
			}
			return activation;
		}
		
		public String toString() {
			return Utils.toString(getClass().getSimpleName(), actionLabels[actionType], actionType, timeLimit, direction);
		}
		
		public String actionName() {
			return actionLabels[actionType];
		}
		
		public void setTime(int timeLimit) {
			setTime(timeLimit, AgilityModel.TIME_EXTENSION_MIN_TIME, timeLimit + MathUtil.round(timeLimit*AgilityModel.TIME_EXTENSION_RATIO));
		}
				
		protected void setTime(int timeLimit, int minSearchTime, int maxSearchTime) {
			this.timeLimit = timeLimit;
			this.maxSearchTime = Math.min(maxSearchTime, 65);
			this.minSearchTime = minSearchTime;
		}
		
		public void increaseTime(int t) {
			timeLimit += t;
			maxSearchTime += t;
			minSearchTime += t;
		}
	}
}
