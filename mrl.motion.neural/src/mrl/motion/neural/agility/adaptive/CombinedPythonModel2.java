package mrl.motion.neural.agility.adaptive;

import jep.Jep;
import jep.JepConfig;
import jep.JepException;
import jep.NDArray;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.predict.MotionPredictor;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.neural.run.RuntimeMotionGenerator;
import mrl.util.Utils;

public class CombinedPythonModel2 implements MotionPredictor{

	private Jep jep;
	private boolean isFirst = true;
	
	public Normalizer normal;
	public RuntimeMotionGenerator g;
	public double[] prevOutput;
	
	@SuppressWarnings("deprecation")
	public CombinedPythonModel2(String name, int trainStepSize, int trainBatchSize, boolean load) {
		try {
			JepConfig config = new JepConfig();
			config.addIncludePaths("..\\mrl.python.neural");
			jep = new Jep(config);
			jep.eval("import sys");
			jep.eval("sys.argv = ['']");
			
//			# def run_training(folder, load=False, test=False, lr=0.0001):
			jep.set("folder", name);
//			jep.set("load", false);
			jep.set("load", load);
			jep.set("test", false);
			jep.set("lr", 0.0001);
			jep.set("train_step_size", trainStepSize);
			jep.set("train_batch_size", trainBatchSize);
			jep.runScript("..\\mrl.python.neural\\adaptive_training2.py");
			
			Normalizer.NEURALDATA_PREFIX = "adaptiveTraining";
			normal = new Normalizer(name);
			g = new RuntimeMotionGenerator();
			double[] initialY = normal.yList.get(3);
			initialY = new double[initialY.length];
			setStartMotion(initialY);
			prevOutput = initialY;
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void addTrainingTuple(int index, double[][] x, double[][] y) {
		try {
			jep.eval("a_idx=" + index);
			jep.set("j_x", x);
			jep.set("j_y", y);
			jep.eval("a_x = np.array(j_x)");
			jep.eval("a_y = np.array(j_y)");
			jep.eval("a_tuple = {'state':state_list[a_idx],'prev_y':current_y_list[a_idx], 'x':a_x, 'y':a_y}");
			jep.eval("adaptive_tuple_list.append(a_tuple)");
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public void saveNetwork() {
		eval("saver.save(sess, \"%s/train/ckpt\"%(folder))");
	}
	
	public void trainByOriginalData() {
		eval("run_one_epoch()");
	}
	
	public void appendOriginalData(double[][] x, double[][] y) {
		try {
			for (int i = 0; i < y.length; i++) {
				x[i] = normal.normalizeX(x[i]);
				y[i] = normal.normalizeY(y[i]);
			}
			jep.set("j_x", x);
			jep.set("j_y", y);
			jep.eval("xDataList.extend(np.array(j_x))");
			jep.eval("yDataList.extend(np.array(j_y))");
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void trainByAdaptiveData() {
		eval("train_from_adaptive_tuple()");
	}
	
	public void clearStateCache() {
		eval("state_list = []");
		eval("current_y_list = []");
	}
	
	public void saveCurrentState() {
		eval("state_list.append(state)");
		eval("current_y_list.append(current_y[0])");
	}
	
	public void eval(String code) {
		try {
			jep.eval(code);
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public Motion predictMotion(double[] x) {
		if (x == null) return null;
		x = normal.normalizeX(x);
		double[] output = pythonPredict(x);
		prevOutput = output;
		output = normal.deNormalizeY(output);
		g.update(output);
		return g.motion();
	}
	
	private double[] pythonPredict(double[] x) {
		try {
			jep.set("b_z", x);
			jep.eval("feed_dict = { m.x:[[b_z]], m.prev_y:current_y }");
			if (isFirst){
				isFirst = false;
			} else {
				jep.eval("feed_dict[m.initial_state] = state");
			}
			jep.eval("output, state, current_y = sess.run([m.generated, m.final_state, m.final_y], feed_dict)");
			
			double[] generated = getValues("output");
			return generated;
		} catch (JepException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	public void setStartMotion(double[] data){
		try {
			isFirst = true;
			jep.set("b_z", data);
			jep.eval("current_y = [b_z]");
		} catch (JepException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private double[] getValues(String name) throws JepException{
		Object bbb = jep.getValue(name);
		NDArray<?> array = (NDArray<?>) bbb;
		float[] values = ((float[]) array.getData());
		double[] ret = new double[values.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = values[i];
		}
		return ret;
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
