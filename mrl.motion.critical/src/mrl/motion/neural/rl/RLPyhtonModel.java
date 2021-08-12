package mrl.motion.neural.rl;

import java.util.ArrayList;
import java.util.Arrays;

import jep.Jep;
import jep.JepConfig;
import jep.JepException;
import jep.NDArray;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;
import mrl.motion.neural.rl.PolicyLearning.Episode;
import mrl.motion.neural.rl.PolicyLearning.EpisodeTuple;
import mrl.motion.neural.rl.PolicyLearning.RL_State;
import mrl.util.Pair;
import mrl.util.Utils;

public class RLPyhtonModel {

	private Jep jep;

	@SuppressWarnings("deprecation")
	public RLPyhtonModel(String name, int num_state, int num_action, boolean useCPU) {
		try {
			JepConfig config = new JepConfig();
			config.addIncludePaths("..\\mrl.python.neural");
			jep = new Jep(config);
			jep.eval("import sys");
			jep.eval("sys.argv = ['']");
			
			jep.set("folder", name);
			jep.set("num_state", num_state);
			jep.set("num_action", num_action);
			jep.set("useCPU", useCPU);
			jep.runScript("..\\mrl.python.neural\\rl_training.py");
			
//			Normalizer.NEURALDATA_PREFIX = "adaptiveTraining";
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	public MotionQuery[] calcAction(RL_State[] state) {
		try {
			jep.eval("_state = []");
			for (RL_State s : state) {
				jep.set("_s_item_", s.toArray());
				jep.eval("_state.append(_s_item_)");
			}
			jep.eval("_state = np.array(_state)");
			jep.eval("_action = model.actor.GetActionOnly(_state)");
//			jep.eval("_action, _logprob = model.actor.GetAction(_state)");
			
//			jep.eval("model._episodes[i].push()");
			
			double[][] generated = getValues_2d("_action");
			MotionQuery[] query = new MotionQuery[generated.length];
			for (int i = 0; i < query.length; i++) {
//				int mid = (generated[i].length/2);
//				MotionFeature feature = new MotionFeature(Utils.cut(generated[i], 0, mid - 1));
//				double[] weights = Utils.cut(generated[i], mid, generated[i].length-1);
				query[i] = new MotionQuery(state[i].state, generated[i]);
				if (Double.isNaN(query[i].feature.data[0])) {
					for (int j = 0; j < query.length; j++) {
						System.out.println("result NAN state :: " + j + " : " + Arrays.toString(state[j].state.data));
					}
					System.out.println("result NAN :: " + i + " : " + Arrays.toString(generated[i]));
					System.out.println("result NAN :: " + i + " : " + Arrays.toString(generated[i+1]));
					System.exit(0);
				}
			}
			return query;
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	public MotionQuery getMeanAction(RL_State state) {
		try {
			jep.eval("_state = []");
			jep.set("_s_item_", state.toArray());
			jep.eval("_state.append(_s_item_)");
			jep.eval("_state = np.array(_state)");
			jep.eval("_action = model.actor.GetMeanAction(_state)");
			
//			jep.eval("model._episodes[i].push()");
			
			double[] generated = getValues("_action");
//			System.out.println("generated :: " + generated.length);
//			int mid = (generated.length/2);
//			System.out.println("generated :: " + Arrays.toString(feature.data));
//			MotionFeature feature = new MotionFeature(Utils.cut(generated, 0, mid - 1));
//			double[] weights = Utils.cut(generated, mid, generated.length-1);
			MotionQuery query = new MotionQuery(state.state, generated);
			return query;
		} catch (JepException e) {
			System.out.println("JEP EXCEPTION--");
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	public void updateEpisodes(double[] rewards, double[] finalErrors, double[][] selectedActions){
		try {
			jep.eval("_action = []");
			for (double[] a : selectedActions) {
				jep.set("_a_item_", a);
				jep.eval("_action.append(_a_item_)");
			}
			jep.eval("_logprob = model.actor.GetNeglohprob(_state, _action)");
			
//			states, actions, rewards, values, logprobs, saveEpisodes
			jep.set("_reward", rewards);
			jep.set("_finalErrors", finalErrors);
			jep.eval("_reward = np.array(_reward)");
			jep.eval("_finalErrors = np.array(_finalErrors)");
			jep.eval("_value = model.critic.GetValue(_state)");
			
			jep.eval("model.updateEpisodes(_state, _action, _reward, _value, _logprob, _finalErrors)");
		} catch (JepException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	private double[][] getValues_2d(String name) throws JepException{
		Object bbb = jep.getValue(name);
		NDArray<?> array = (NDArray<?>) bbb;
		int[] dimensions = array.getDimensions();
		float[] data = (float[]) array.getData();
		double[][] values = new double[dimensions[0]][dimensions[1]];
		int idx = 0;
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values[0].length; j++) {
				values[i][j] = data[idx];
				idx++;
			}
		}
		return values;
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
	
	public void clearEpisodes() {
		try {
			jep.eval("model.clearEpisodes()");
		} catch (JepException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	public void save(String path) {
		try {
			jep.set("_path", path);
			jep.eval("model.Save(_path)");
		} catch (JepException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	@SuppressWarnings("deprecation")
	public void restore(String path) {
		try {
			jep.set("_path", path);
			jep.eval("model.Restore(_path)");
		} catch (JepException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	public void setEpisodes(ArrayList<Episode> episodes) {
		try {
			int tSum = 0;
//			jep.set("b_z", state);
			jep.eval("model.clearEpisodes()");
			for (Episode ep : episodes) {
				tSum += ep.tupleList.size();
				jep.eval("ep = model.makeEpisode()");
				for (EpisodeTuple tp : ep.tupleList) {
//					episodes[j].push(states[j], actions[j], rewards[j], values[j], logprobs[j])
					jep.set("t_s", tp.state);
					jep.set("t_a", tp.action);
					jep.set("t_r", tp.reward);
					jep.set("t_v", tp.value);
					jep.set("t_lpb", tp.logprob);
					jep.eval("ep = model.push(t_s, t_a, t_r, t_v, t_lpb)");
				}
			}
			jep.eval("self._summary_num_transitions_per_iteration = " + tSum);
			
		} catch (JepException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public void runTraining() {
		try {
			jep.eval("model.Optimize()");
		} catch (JepException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
