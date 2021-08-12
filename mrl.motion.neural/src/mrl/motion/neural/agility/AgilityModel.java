package mrl.motion.neural.agility;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionActionDP;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.match.MMatching;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.motion.neural.agility.match.TransitionDPGenerator;
import mrl.util.MathUtil;
import mrl.util.Utils;

public abstract class AgilityModel {
	
	public static int GOAL_TIME_LIMIT = 20;
	public static int TIME_EXTENSION_MIN_TIME = 10;
	public static double TIME_EXTENSION_RATIO = 0.33;
	public static int ACTIVATION_RISE = 3;
	public static int ACTIVATION_PEAK = 1;
	
	public AgilityModel() {
	}
	
	public String[] getActionTypes() {
		throw new RuntimeException();
	}
	public String[] getFullActionTypes() {
		return getActionTypes();
	}
	
	public int getContinuousLabelSize() {
		throw new RuntimeException();
	}
	
	public int getActionSize() {
		return getActionTypes().length;
	}
	public abstract AgilityGoal sampleRandomGoal(Pose2d currentPose);
	public abstract AgilityGoal sampleIdleGoal();
	public abstract MotionMatching makeMotionMatching(MDatabase database);
	
	public MMatching makeMMatching(MDatabase database) {
		TransitionData tData = new TransitionData(database, getFullActionTypes(), getContinuousLabelSize());
		TransitionActionDP dp = new TransitionActionDP(tData);
		dp.load(TransitionDPGenerator.FILE_PREFIX + database.getDatabaseName() + ".dat");
		MMatching matching = new MMatching(dp);
		return matching;
	}
	
	public boolean useActivation() {
		return false;
	}
	
	protected double[] actionData(int actionType) {
		return AgilityControlParameterGenerator.getActionType(getActionSize(), actionType);
	}
	
	// search model?
//	public abstract double[] inferenceControlParameter();
	
	public abstract class AgilityGoal{
		public int actionType;
		public int timeLimit;
		public int maxSearchTime;
		public int minSearchTime;
		public double agility;
		
		public AgilityGoal(int actionType, int timeLimit) {
			this.actionType = actionType;
			setTime(timeLimit);
			
			agility = GOAL_TIME_LIMIT;
		}
		
		public Boolean checkActionValid(Motion currentMotion, int currentMotionAction, Motion targetMotion, int targetMotionAction) {
			return null;
		}
			
		/**
		 * 현재 path까지 motion을 만든 상태에서 transition 후 future pose 및 rotation에 위치한다고 했을때의 error
		 * @param currentMoved
		 * @param futurePose
		 * @param futureRotation
		 * @return
		 */
		public double getSpatialError(Pose2d currentMoved, double currentRotated, Pose2d futurePose, double futureRotation) {
			return 0;
		}
		
		/**
		 * 최종적으로 생성된 motion을 editing 하기 위한 constraint pose
		 * @return
		 */
		public abstract Pose2d getEditingConstraint();
		
		public Pose2d getEditingConstraint(Motion first, Motion last) {
			throw new RuntimeException();
		}
		
		/**
		 * 학습된 network의 Agility를 평가 할때, 현재까지 생성된 motion이 goal을 달성했는지 판단하는 함수
		 * @param startPose
		 * @param motionList
		 * @param activationList TODO
		 * @return
		 */
		public abstract boolean isGoalFinished(Pose2d startPose, ArrayList<Motion> motionList, ArrayList<Double> activationList);
		/**
		 * 학습된 network의 Agility를 평가 할때, 다음 motion을 생성하기 위해 controller에 넘겨줄 인자를 만들어주는 함수
		 * @param poseList
		 * @return
		 */
		public abstract double[] getControlParameter(ArrayList<Pose2d> poseList);
		
		/**
		 * 학습 데이터를 생성할때, 생성된 motion으로부터 그에 맞는 control parameter를 생성하는 함수.
		 * @param motionSequence
		 * @param currentIndex
		 * @param targetIndex
		 * @return
		 */
		public abstract double[] getControlParameter(ArrayList<Motion> motionSequence, int currentIndex, int targetIndex);
		
		/**
		 * 학습된 network의 Agility를 평가 할때, 다음 motion을 생성하기 위해 controller에 넘겨줄 인자를 시각화 하기 위한 object를 반환하는 함수
		 * @param startPose
		 * @param currentPose
		 * @return
		 */
		public abstract Object getControlParameterObject(Pose2d startPose, Pose2d currentPose);
		
		
		/**
		 * Search 과정에서 valid한 frame에 도달했을때 더이상 탐색을 종료하고 해당 path를 최종 path로 확정할지 여부
		 * @param goal
		 * @return
		 */
		public boolean isActiveAction() {
			return false;
		}
		
		public double getActivation(int remainTime) {
			if (!useActivation()) return Double.NaN;
			
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
		
		public double[] getControlParameter() {
			return getControlParameter(Utils.singleList(Pose2d.BASE));
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
