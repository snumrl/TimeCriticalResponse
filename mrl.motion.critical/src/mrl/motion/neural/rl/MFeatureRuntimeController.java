package mrl.motion.neural.rl;

import mrl.motion.data.MDatabase;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.motion.neural.run.RuntimeController;
import mrl.motion.neural.run.RuntimeMotionGenerator;

public class MFeatureRuntimeController extends RuntimeController{

	public MFeatureMatching matching;
	public MotionFeature currentFeature;
	public int currentMIndex = -1;
	
	public MFeatureRuntimeController(String dataFolder, String tPoseFile) {
		this(new MFeatureMatching(TrainingDataGenerator.loadDatabase(dataFolder, tPoseFile)));
	}
	public MFeatureRuntimeController(MFeatureMatching matching) {
		this.matching = matching;
		normal = matching.normal;
		g = new RuntimeMotionGenerator();
		
		int idx = 200;
		while (true) {
			select(idx);
			if (currentFeature != null) break;
			idx++;
		}
		
		System.out.println("start feature :: " + currentFeature.data.length);
	}
	
	public void select(int motionIndex) {
		currentMIndex = motionIndex;
		currentFeature = matching.getOriginData(motionIndex, false);
	}
	
}
