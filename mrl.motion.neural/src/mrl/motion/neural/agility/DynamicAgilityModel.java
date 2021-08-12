package mrl.motion.neural.agility;

import mrl.motion.data.MDatabase;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.util.MathUtil;

public class DynamicAgilityModel extends AgilityModel{
	
	public static int AGILITY_CHANGE_COUNT = 20;
	private AgilityModel model;
	private int minTimeLimit;
	private int maxTimeLimit;

	private int repeatedCount = 0;
	
	public DynamicAgilityModel(AgilityModel model, int minTimeLimit, int maxTimeLimit) {
		this.model = model;
		this.minTimeLimit = minTimeLimit;
		this.maxTimeLimit = maxTimeLimit;
		GOAL_TIME_LIMIT = (minTimeLimit + maxTimeLimit)/2; 
		
		AgilityControlParameterGenerator.ADD_TIMING_PARAMETER = true;
	}

	@Override
	public AgilityGoal sampleRandomGoal(Pose2d currentPose) {
		AgilityGoal goal = model.sampleRandomGoal(currentPose);
		updateTimeLimit();
		return goal;
	}

	@Override
	public AgilityGoal sampleIdleGoal() {
		AgilityGoal goal = model.sampleIdleGoal();
		updateTimeLimit();
		return goal;
	}
	
	private void updateTimeLimit() {
//		goal.setTime(timeLimit, AgilityModel.TIME_EXTENSION_MIN_TIME, timeLimit + MathUtil.round(timeLimit*AgilityModel.TIME_EXTENSION_RATIO));
		repeatedCount++;
		if (repeatedCount > AGILITY_CHANGE_COUNT) {
			repeatedCount = 0;
			GOAL_TIME_LIMIT = minTimeLimit;
			if (maxTimeLimit > minTimeLimit) {
				GOAL_TIME_LIMIT += MathUtil.random.nextInt(maxTimeLimit - minTimeLimit);
			}
		}
	}

	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		return model.makeMotionMatching(database);
	}
	
	public String[] getActionTypes() {
		return model.getActionTypes();
	}
	
	public String[] getFullActionTypes() {
		return model.getFullActionTypes();
	}
	
	public int getContinuousLabelSize() {
		return model.getContinuousLabelSize();
	}
	
	public boolean useActivation() {
		return model.useActivation();
	}

}
