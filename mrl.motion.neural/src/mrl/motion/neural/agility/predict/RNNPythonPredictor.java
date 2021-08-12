package mrl.motion.neural.agility.predict;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.neural.run.RNNPython;
import mrl.motion.neural.run.RuntimeMotionGenerator;
import mrl.util.Utils;

public class RNNPythonPredictor implements MotionPredictor {

	public RNNPython python;
	public Normalizer normal;
	public RuntimeMotionGenerator g;
	public double[] prevOutput;
	
	public RNNPythonPredictor(String folder) {
		python = new RNNPython(folder, false);
		normal = new Normalizer(folder);
		g = new RuntimeMotionGenerator();
		
		double[] initialY = normal.yList.get(3);
		initialY = new double[initialY.length];
		python.model.setStartMotion(initialY);
		prevOutput = initialY;
	}
	
	public void reset() {
		double[] initialY = normal.yList.get(3);
		initialY = new double[initialY.length];
		python.model.setStartMotion(initialY);
		prevOutput = initialY;
		g.pose = new Pose2d(Pose2d.BASE);
	}
	
	@Override
	public Motion predictMotion(double[] x) {
		if (x == null) return null;
		x = normal.normalizeX(x);
		double[] output = python.model.predict(x);
		prevOutput = output;
		output = normal.deNormalizeY(output);
		g.update(output);
		return g.motion();
	}

	@Override
	public Pose2d currentPose() {
		return new Pose2d(g.pose);
	}

	@Override
	public double currentActivation() {
		return Utils.last(normal.deNormalizeY(prevOutput));
	}
}
