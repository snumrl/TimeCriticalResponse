package mrl.motion.neural.agile;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.dancecard.DanceCardGraphGenerator;
import mrl.motion.neural.data.ActionOnlyWaveControl;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.ActionOnlyWaveControl.ActionParameter;
import mrl.motion.neural.run.PythonRuntimeController;
import mrl.motion.neural.run.RuntimeMotionGenerator;
import mrl.motion.position.PositionResultMotion;
import mrl.motion.viewer.module.TimeBasedList;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class AgilityOfflineController extends PythonRuntimeController{
	
	public static int maxTime = 80;
	public static double TIMING_UNIT = 1;
	
	public ActionParameter prevTarget;
	public ActionParameter currentTarget;
	public int index = 0;
	public double timeIndex = 0;
	
	public TimeBasedList<Pose2d> targetList = new TimeBasedList<Pose2d>();
	public TimeBasedList<Pose2d> failedList = new TimeBasedList<Pose2d>();
	public TimeBasedList<Pose2d> successedList = new TimeBasedList<Pose2d>();
	
	
	@Override
	public double[] getControlParameter() {
		double[] control = ActionOnlyWaveControl.getControlParameter(
				prevTarget, currentTarget, timeIndex, g.pose, DanceCardGraphGenerator.FightActionTypes);
		Point2d p = new Point2d(control[control.length-2], control[control.length-1]);
		Pose2d pose = new Pose2d(p, new Vector2d(p));
		targetList.add(g.pose.localToGlobal(pose));
		return control;
	}
	
	public void clear() {
		g = new RuntimeMotionGenerator();
		totalMotion.clear();
		index = 0;
		timeIndex = 0;
		
		targetList.clear();
		failedList.clear();
		successedList.clear();
		prevTarget = null;
		currentTarget = null;
	}
	
	public AgilityControlResult iterateSingleTarget(double targetLength, int time) {
		return iterateSingleTarget(pickNextTarget(targetLength, time));
	}
	
	public AgilityControlResult iterateSingleTarget(ActionParameter param) {
		double[] startFrame = prevOutput;
		currentTarget = new ActionParameter(param);
		currentTarget.actionPose = g.pose.localToGlobal(currentTarget.actionPose);
		currentTarget.actionTime = MathUtil.round(timeIndex) + currentTarget.actionTime;
		
//		for (; timeIndex < currentTarget.actionTime; timeIndex++) {
		for (; timeIndex < currentTarget.actionTime; timeIndex+= TIMING_UNIT) {
			iterateMotion();
		}
//		System.out.println(targetOrigin + "\t\t" + time);
		AgilityControlResult result = new AgilityControlResult(index, startFrame, param);
		double[] o = normal.deNormalizeY(prevOutput);
		result.evaluate(o[o.length-1], g.pose.globalToLocal(currentTarget.actionPose));
//		System.out.println("is successed :: " + timeIndex + " : " + result.isValid);
		if (result.isValid) {
			for (double i = 0; i < param.actionTime; i+=TIMING_UNIT) {
				successedList.add(currentTarget.actionPose);
				failedList.add(null);
			}
		} else {
			for (double i = 0; i < param.actionTime; i+=TIMING_UNIT) {
				successedList.add(null);
				failedList.add(currentTarget.actionPose);
			}
		}
		
		prevTarget = currentTarget;
		index++;
		return result;
	}
	
	public static ActionParameter pickNextTarget(double targetLength, int time) {
		Vector2d v = new Vector2d(targetLength*(1 + Utils.rand1()*0.25), 0);
		double angle = MathUtil.random.nextDouble()*Math.PI*2;
		v = MathUtil.rotate(v, angle);
		Pose2d actionPose = new Pose2d(new Point2d(v), v);
		return new ActionParameter("kick", actionPose, time);
	}

	public static class AgilityControlResult{
		public int index;
		public double[] startFrame;
		public ActionParameter parameter;
		
		public double error;
		public boolean isValid;
		
		public AgilityControlResult(int index, double[] startFrame, ActionParameter parameter) {
			this.index = index;
			this.startFrame = startFrame;
			this.parameter = parameter;
		}
		
		public boolean evaluate(double activation, Pose2d finalPose) {
			double aError = 0;
			double activationCut = 0.85;
			if (activation < activationCut) {
				aError = 5 + activationCut - activation;
			}
			aError = aError*aError;
			double posError = 0;
			double posDist = MathUtil.length(finalPose.position);
			double distanceCut = 30;
			if (posDist > distanceCut) {
				posError = (distanceCut*2 + posDist - distanceCut)/distanceCut;
				posError = posError*posError;
			}
			error = aError + posError;
			isValid = error < 0.0000001;
//			System.out.println("error :: " + isValid+ " : " + error + " :: " + activation + " : " + posDist);
			return isValid;
		}
	}
}
