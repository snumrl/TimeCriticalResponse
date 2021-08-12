package mrl.motion.neural.run;

import mrl.motion.neural.data.Normalizer;

public abstract class RuntimeController {

	public Normalizer normal;
	public RuntimeMotionGenerator g;
	public int frame = 0;
}
