package mrl.motion.neural.agility.predict;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;

public interface MotionPredictor {

	public Motion predictMotion(double[] x);
	public Pose2d currentPose();
	public double currentActivation();
}
