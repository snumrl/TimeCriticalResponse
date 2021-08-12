package mrl.motion.neural.agility;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.MDatabase;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.ActionDistDP;
import mrl.motion.neural.agility.match.MotionMatching;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class StuntModel extends PokeModel{

	static double[][] actionMarginList = {
			{ 4, 4 }, // i
			{ 7.837, 5.558 }, // p
			{ 10.844, 9.188 }, // k
			{ 11.250, 11.750 }, // bs
			{ 19.000, 10.333 }, // jk
			{ 20.000, 13.333 }, // jbs
			{ 18.500, 16.000 }, // jd
			{ 9.500, 6.500 }, // th
			{ 9.818, 9.091 }, // lk
			{ 13.500, 12.000 }, // knee
			{ 9.000, 8.000 }, // d
			{ 7.000, 4.462 }, // g
			{ 0.000, 0.000 }, // n
			
//			{ 4, 4 }, // i
//			{ 7.377, 5.164 }, // p
//			{ 9.500, 6.500 }, // th
//			{ 10.243, 8.514 }, // k
//			{ 11.250, 11.750 }, // bs
//			{ 16.750, 10.000 }, // jk
//			{ 19.400, 14.000 }, // jbs
//			{ 18.500, 16.000 }, // jd
//			{ 9.818, 9.091 }, // lk
//			{ 13.500, 12.000 }, // knee
//			{ 9.000, 8.000 }, // d
//			{ 7.000, 4.462 }, // g
//			{ 0.000, 0.000 }, // n
//			{ 2.667, 2.667 }, // nd
//			{ 10.000, 6.250 }, // nk
//			{ 3.333, 2.000 }, // nn
	};
	
	private PokeGoal lastGoal;
	public StuntModel(String dpLabel) {
		super(dpLabel);
		FootContactDetection.footDistPointOffset = new int[]{
				0, 20, 0
		};
		actionTypes = new String[] {
				"i",
				"p",
				"k",
				"bs",
				"jk",
				"jbs",
				"jd",
				
//				"th",
//				"lk",
//				"knee",
//				"d",
//				"g",
//				"n",
		};
		
		sampleIdleGoal();
	}
	
	public String[] getActionTypes() {
		return actionTypes;
	}
	public String[] getFullActionTypes() {
		String[] fullActions = {
				"i",
				"p",
				"k",
				"bs",
				"jk",
				"jbs",
				"jd",
				"th",
				"lk",
				"knee",
				"d",
				"g",
				"n",
		};
		return fullActions;
	}
	
	public int getContinuousLabelSize() {
		return 1;
	}
	
	public PokeGoal sampleRandomGoal(int action) {
		double timeLength = 0;
		timeLength += 6; // base margin
		timeLength += actionMarginList[lastGoal.actionType][1];
		timeLength += actionMarginList[action][0];
		int adjustedTime = MathUtil.round(timeLength/20d*GOAL_TIME_LIMIT);
		lastGoal = new PokeGoal(action, adjustedTime);
		return lastGoal;
	}
	
	@Override
	public PokeGoal sampleRandomGoal(Pose2d currentPose) {
		int action = MathUtil.random.nextInt(actionTypes.length);
		return sampleRandomGoal(action);
	}

	@Override
	public PokeGoal sampleIdleGoal() {
		lastGoal = new PokeGoal(0, GOAL_TIME_LIMIT);
		return lastGoal;
	}

	@Override
	public MotionMatching makeMotionMatching(MDatabase database) {
		throw new RuntimeException();
	}
}
