package mrl.motion.neural.run.ue;

import mrl.motion.neural.run.RuntimeController;
import mrl.widget.app.Module;

public abstract class MotionController extends Module{

	public String tPoseFile;
	public String trainFolder;
	public RuntimeController c;
	
	public MotionController(String tPoseFile, String trainFolder) {
		this.tPoseFile = tPoseFile;
		this.trainFolder = trainFolder;
	}
	
	
	public String gettPoseFile() {
		return tPoseFile;
	}

	public String getTrainFolder() {
		return trainFolder;
	}
	
	public RuntimeController getRuntimeController() {
		return c;
	}

	public void setRuntimeController(RuntimeController c) {
		this.c = c;
	}

	public abstract double[] getControl();
	
	public double[] getAdditionalInfo() {
		return null;
	}
	
}
