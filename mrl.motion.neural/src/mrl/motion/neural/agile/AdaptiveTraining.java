package mrl.motion.neural.agile;

import java.util.ArrayList;
import java.util.Random;

import jep.Jep;
import jep.JepConfig;
import jep.JepException;
import jep.NDArray;
import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.LevelOfErrorGraph;
import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraphSearch;
import mrl.motion.neural.agile.AgilityOfflineController.AgilityControlResult;
import mrl.motion.neural.dancecard.DanceCardGraphGenerator;
import mrl.motion.neural.data.ActionOnlyWaveControl;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.neural.data.ActionOnlyWaveControl.ActionParameter;
import mrl.motion.neural.run.RNNPython;
import mrl.motion.neural.run.RuntimeMotionGenerator;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.util.log.RatioLogger;

public class AdaptiveTraining {
	
	static int TRAIN_STEP_SIZE = 48;
	static int TRAIN_BATCH_SIZE = 60;
	
	static int TARGET_TIME = 40;
	static double TARGET_LENGTH = 120;

	private Jep jep;
	private boolean isFirst = true;
	private AgilityOfflineController c;
	
	public AdaptiveTraining(String name) {
//		normal = new Normalizer(name);
		try {
			JepConfig config = new JepConfig();
			config.addIncludePaths("..\\mrl.python.neural");
			jep = new Jep(config);
			jep.eval("import sys");
			jep.eval("sys.argv = ['']");
			
//			# def run_training(folder, load=False, test=False, lr=0.0001):
			jep.set("folder", name);
			jep.set("load", true);
			jep.set("test", false);
			jep.set("lr", 0.0001);
			jep.set("train_step_size", TRAIN_STEP_SIZE);
			jep.set("train_batch_size", TRAIN_BATCH_SIZE);
			jep.runScript("..\\mrl.python.neural\\adaptive_training.py");
			
			Normalizer.NEURALDATA_PREFIX = "adaptiveTraining";
			c = new AgilityOfflineController() {
				public void init(String folder) {
//					python = new RNNPython(folder, false);
					normal = new Normalizer(folder);
					g = new RuntimeMotionGenerator();
					
					double[] initialY = normal.yList.get(3);
					initialY = new double[initialY.length];
//					python.model.setStartMotion(initialY);
					setStartMotion(initialY);
					prevOutput = initialY;
				}
				
				protected double[] predict(double[] x) {
//					return python.model.predict(x);
					return predictFromModel(x);
				}
			};
			
			Configuration.BASE_MOTION_FILE = "t_pose_actor.bvh";
			MDatabase database = LevelOfErrorGraph.loadDatabase();
//			LevelOfErrorGraph.setConfiguration();
//			MDatabase database = MDatabase.load();
			MGraph graph = new MGraph(database);
			MotionDataConverter.setAllJoints();
			MotionDataConverter.setNoBall();
			MotionDataConverter.setUseOrientation();
			ActionOnlyWaveControl.USE_GOAL_INTERPOLATION = true;
			c.init(name);
			
			MathUtil.setRandomSeed();
			ImproveByGraph.MAX_SEARCH_TIME = TARGET_TIME + 30;
			ImproveByGraph ig = new ImproveByGraph(graph, c.normal);
			for (int iter = 0; iter < 5000; iter++) {
				System.out.println("iter :: " + iter);
				jep.eval("run_one_epoch()");
				jep.eval("train_from_adaptive_tuple()");
				if ((iter % 20) == 0) {
					jep.eval("saver.save(sess, \"%s/train/ckpt\"%(folder))");
//					c.python.updateFromCache();
					long t0 = System.currentTimeMillis();
					ArrayList<AgilityControlResult> resultList = collectAgilityData(c, (iter == 0) ? 1000 : 200, (iter == 0) ? 200 : 40);
					System.out.println("collectAgilityData :: " + (System.currentTimeMillis()-t0));
					
					resultList = sampleResults(resultList);
					System.out.println("sample size :: " + resultList.size());
					long t1 = System.currentTimeMillis();
					for (AgilityControlResult r : resultList) {
						makeTrainTuple(ig, r);
					}
					System.out.println("makeTrainTuple :: " + (System.currentTimeMillis()-t1));
				}

			}
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void makeTrainTuple(ImproveByGraph ig, AgilityControlResult r) throws JepException {
		ArrayList<Motion> motionList = ig.generateMatchingMotion(r.startFrame, r.parameter.actionPose, r.parameter.actionTime);
		ArrayList<double[]> dataList = ig.blendWithStartPose(c.normal.deNormalizeY(r.startFrame), motionList);
		RuntimeMotionGenerator g = new RuntimeMotionGenerator();
//		g.update(c.normal.deNormalizeY(lastPose));
		for (double[] data : dataList) {
			g.update(data);
		}
		ArrayList<double[]> targetList = new ArrayList<double[]>();
		int nextIndex = ig.selectedTime + 20 + MathUtil.random.nextInt(15);
		nextIndex = Math.min(nextIndex, g.poseList.size()-1);
		
		ActionParameter prevAction = new ActionParameter("kick", new Pose2d(Pose2d.BASE), 0);
		ActionParameter currentAction = new ActionParameter("kick", new Pose2d(g.poseList.get(ig.selectedTime)), ig.selectedTime);
		for (int t = 0; t < 50; t++) {
			double[] control = ActionOnlyWaveControl.getControlParameter(
					prevAction, currentAction, t, g.poseList.get(t), DanceCardGraphGenerator.FightActionTypes);
			if (t == ig.selectedTime) {
				prevAction = currentAction;
				currentAction = new ActionParameter("kick", new Pose2d(g.poseList.get(nextIndex)), nextIndex);
			}
			targetList.add(control);
			dataList.set(t, MathUtil.concatenate(dataList.get(t), new double[] { ActionOnlyWaveControl.activation }));
		}
		double[][] x = Utils.toArray(Utils.cut(targetList, 0, TRAIN_STEP_SIZE-1));
		double[][] y = Utils.toArray(Utils.cut(dataList, 0, TRAIN_STEP_SIZE-1));
		for (int i = 0; i < x.length; i++) {
			x[i] = c.normal.normalizeX(x[i]);
		}
		for (int i = 0; i < y.length; i++) {
			y[i] = c.normal.normalizeY(y[i]);
		}
		
		jep.eval("a_idx=" + r.index);
		jep.set("j_x", x);
		jep.set("j_y", y);
		
		jep.eval("a_x = np.array(j_x)");
		jep.eval("a_y = np.array(j_y)");
		jep.eval("a_tuple = {'state':state_list[a_idx],'prev_y':current_y_list[a_idx], 'x':a_x, 'y':a_y}");
		jep.eval("adaptive_tuple_list.append(a_tuple)");
		
		// state, prevMotion,  x, y
		
	}
	
	private ArrayList<AgilityControlResult> sampleResults(ArrayList<AgilityControlResult> list){
		ArrayList<AgilityControlResult> filtered = new ArrayList<AgilityControlResult>();
		for (AgilityControlResult r : list) {
			if (!r.isValid) {
				filtered.add(r);
			}
		}
		return filtered;
	}
	
	private boolean isPrinted = false;
	private ArrayList<AgilityControlResult> collectAgilityData(AgilityOfflineController c, int trySize, int maxialSampleSize) throws JepException{
//		jep.eval("state = None");
		jep.eval("state_list = []");
		jep.eval("current_y_list = []");
		// iterate dummy target to success
		c.iterateSingleTarget(20, TARGET_TIME);
		c.clear();
		
		boolean lastSuccessed = true;
		ArrayList<AgilityControlResult> resultList = new ArrayList<AgilityControlResult>();
		RatioLogger log = new RatioLogger();
		for (int i = 0; i < trySize; i++) {
			if (trySize > 900) {
				System.out.println("try : " + i);
			}
			if (!isPrinted) {
				isPrinted = true;
				System.out.println("------state append print----");
				jep.eval("print(state.__class__)"); 
				jep.eval("print(len(state))"); 
				jep.eval("print(state[0].__class__)"); 
				jep.eval("print(len(state[0]))"); 
				jep.eval("print(state[0][0].__class__)"); 
				jep.eval("print(len(state[0][0]))");
				jep.eval("sys.stdout.flush()");
			}
			
			jep.eval("state_list.append(state)");
			jep.eval("current_y_list.append(current_y[0])");
			AgilityControlResult result;
			if (lastSuccessed) {
				result = c.iterateSingleTarget(TARGET_LENGTH, TARGET_TIME);
			} else {
				// iterate dummy target to success
				result = c.iterateSingleTarget(20, TARGET_TIME);
			}
			log.log(result.isValid);
			if (!result.isValid) {
				resultList.add(result);
				if (resultList.size() >= maxialSampleSize) {
					break;
				}
			}
			lastSuccessed = result.isValid;
		}
		log.print("succ ratio");
		return resultList;
	}
	
	public double[] predictFromModel(double[] target) {
		try {
			jep.set("b_z", target);
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
	
	public static void main(String[] args) {
		TARGET_TIME = 28;
		TRAIN_STEP_SIZE = 36;
		MGraphSearch.TRANSITION_MARGIN = 6;
		new AdaptiveTraining("ao_d_wv2_gitp_activation");
	}
}
