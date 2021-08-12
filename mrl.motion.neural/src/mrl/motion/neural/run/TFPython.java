package mrl.motion.neural.run;

import java.util.Arrays;

import jep.Jep;
import jep.JepConfig;
import jep.JepException;
import jep.NDArray;
import mrl.motion.neural.data.Normalizer;

public class TFPython {
	
	public static String FIXED_NAME = null;
	public static boolean NO_SCOPE_NAME = true;
	

	private Jep jep;
	
	public TFModel model;
	public String name;

	public TFPython(String name) {
		this(name, false);
	}
	public TFPython(String name, boolean prefix) {
		try {
			this.name = name;
			JepConfig config = new JepConfig();
			config.addIncludePaths("..\\mrl.python.neural");
			jep = new Jep(config);
			jep.eval("import sys");
			jep.eval("sys.argv = ['']");
			
			
			String code = 
					"import numpy as np\n"+
					"import transformer.TConfigurations as cf\n"+
					"import tensorflow as tf\n"+
					"config = tf.ConfigProto(device_count = {'GPU': 0})\n"+
					"sess = tf.Session(config=config)\n";
//					"sess = tf.Session()\n";
			for (String c : code.split("\n")){
				jep.eval(c);
			}
			
			model = loadModel(name, prefix);
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void eval(String code) throws JepException{
		for (String c : code.split("\n")){
			jep.eval(c);
		}
	}
	
	public TFModel loadModel(String name, boolean prefix){
		try {
			return new TFModel(name, prefix);
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void updateFromCache() {
		try {
			jep.eval("saver.restore(sess, trainedFolder)");
		} catch (JepException e) {
			throw new RuntimeException(e);
		}
	}
	
	public class TFModel{
		public String name;
		private boolean isFirst = true;
		
		private String[] variables = {
				"m",
				"current_y",
			};

		public TFModel(String name, boolean prefix) throws JepException {
			String folderName = name;
			if (FIXED_NAME != null) {
				name = FIXED_NAME;
			}
			this.name = name;
			
			for (String v : variables){
				eval(name + "_" + v + " = None");
			}
			
//			if (USE_NEURALDATA_PREFIX) {
				jep.set("trainedFolder", Normalizer.NEURALDATA_PREFIX + "\\" + folderName + "\\train\\ckpt");
				String base = Normalizer.NEURALDATA_PREFIX.replace("\\", "\\\\");
				eval("c = cf.get_config(\"" + name + "\")\n" +
						"c.load_normal_data(\"" + base + "\\\\" + folderName + "\")\n" +
						"c.dropout_rate = 0\n"+
						(NO_SCOPE_NAME ? "" : "c.use_dummy_scope = False\n") +
						name + "_m = c.model(1, 1)\n"
						);
//			} else {
//				jep.set("trainedFolder", folderName + "\\train\\ckpt");
//				eval("c = cf.get_config(\"" + name + "\")\n" +
//						"c.load_normal_data(\"" + folderName + "\")\n" +
//						"c.INPUT_KEEP_PROB = 1\n"+
//						"c.LAYER_KEEP_PROB = 1\n"+
//						name + "_m = c.model(1, 1)\n"
//						);
//			}
			restore(prefix);
			
			
		}
		
		private void preProcess() throws JepException{
			for (String v : variables){
				eval(v + " = " + name + "_" + v);
			}
		}
		private void postProcess() throws JepException{
			for (String v : variables){
				if (v.equals("m")) continue;
				eval(name + "_" + v + " = " + v);
			}
		}
		
		public double[] predict(double[] target) {
			try {
				preProcess();
				jep.set("b_z", target);
				jep.eval("feed_dict = { m.x:[[b_z]], m.prev_y:current_y }");
				jep.eval("output, current_y = sess.run([m.generated, m.final_y], feed_dict)");
				
//				printTensorNames();
				
				postProcess();
				
				double[] generated = getValues("output");
				return generated;
			} catch (JepException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		
		protected void printTensorNames() throws JepException{
			System.out.println("###############");
			jep.eval("print(m.x.name)");
			jep.eval("print(m.prev_y.name)");
			jep.eval("print(m.prev_state.name)");
			
			jep.eval("print(m.generated.name)");
			jep.eval("print(m.final_state.name)");
			jep.eval("print(m.final_y.name)");
////			jep.eval("print(len(m.final_state))");
////			jep.eval("print(len(m.final_state[0]))");
//			for (int i = 0; i < 4; i++) {
//				jep.eval("print(m.final_state[" + i + "][0].name)");
//				jep.eval("print(m.final_state[" + i + "][1].name)");
//			}
//			for (int i = 0; i < 4; i++) {
//				jep.eval("print(m.initial_state[" + i + "][0].name)");
//				jep.eval("print(m.initial_state[" + i + "][1].name)");
//			}
			System.out.println("###############");
			jep.eval("print(m.x.get_shape())");
			jep.eval("print(m.prev_y.get_shape())");
			jep.eval("print(m.prev_state.get_shape())");
			
			jep.eval("print(m.generated.get_shape())");
			jep.eval("print(m.final_state.get_shape())");
			jep.eval("print(m.final_y.get_shape())");
////			jep.eval("print(len(m.final_state))");
////			jep.eval("print(len(m.final_state[0]))");
//			for (int i = 0; i < 4; i++) {
//				jep.eval("print(m.final_state[" + i + "][0].get_shape())");
//				jep.eval("print(m.final_state[" + i + "][1].get_shape())");
//			}
//			for (int i = 0; i < 4; i++) {
//				jep.eval("print(m.initial_state[" + i + "][0].get_shape())");
//				jep.eval("print(m.initial_state[" + i + "][1].get_shape())");
//			}
//			
			jep.eval("sys.stdout.flush()");
			System.out.flush();
		}
		
		public void setStartMotion(double[] data){
			isFirst = true;
			setStartMotionOnly(data);
		}
		
		public void setStartMotionOnly(double[] data){
			try {
				jep.eval("b_z = np.zeros([1, c.history_size, c.Y_DIMENSION])");
				jep.eval(name + "_current_y = b_z");
			} catch (JepException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		
		public void setStartMotionOnly(double[][] data){
			try {
				jep.eval("b_z = []");
				for (int i = 0; i < data.length; i++) {
					jep.set("dd", data[i]);
					jep.eval("b_z.append(dd)");
				}
//				jep.eval("b_z = np.zeros([1, c.history_size, c.Y_DIMENSION])");
				jep.eval(name + "_current_y = [b_z]");
			} catch (JepException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		
		private void restore(boolean prefix) throws JepException{
			String[] codes = null;
			if (NO_SCOPE_NAME) {
				codes = new String[]{
						"saver = tf.train.Saver()",
						"saver.restore(sess, trainedFolder)",
				};
			} else if (prefix){ 
				codes = new String[]{
						"prefix = c.label",
						"t_variables = tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, scope=prefix)",
						"p_len = len(prefix) + 1",
						"v_dict = {}",
						"for v in t_variables:\n\tv_dict[v.name[p_len:-2]] = v\n",
						"saver = tf.train.Saver(v_dict)",
						"saver.restore(sess, trainedFolder)",
				};
			} else {
				codes = new String[]{
//						"vars_in_checkpoint = tf.train.list_variables(trainedFolder)",
//						"for v in vars_in_checkpoint:\n\tprint(v)\n",
//						"sys.stdout.flush()",
						"prefix = c.label",
						"t_variables = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope=prefix)",
//						"for v in t_variables:\n\tprint(v.get_shape())\n",
//						"sys.stdout.flush()",
						"saver = tf.train.Saver(t_variables)",
						"saver.restore(sess, trainedFolder)",
				};
			}
			for (String c : codes){
				jep.eval(c);
			}
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

	
}
