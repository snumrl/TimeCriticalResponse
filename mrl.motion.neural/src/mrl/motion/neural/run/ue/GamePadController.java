package mrl.motion.neural.run.ue;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import mrl.motion.neural.run.PythonRuntimeController;

public abstract class GamePadController extends MotionController{
	
	protected ControllerManager cm;
	protected int lastAction = -1;
	protected int actionStartFrame = -1;
	protected ControllerState lastState;
	
	public GamePadController(String tPoseFile, String trainFolder) {
		super(tPoseFile, trainFolder);
		cm = new ControllerManager();
		cm.initSDLGamepad();
	}
	
	@Override
	public double[] getControl() {
		ControllerState s = cm.getState(0);
		if (!s.isConnected) return null;
		if (lastState == null) lastState = s;
		double[] control = getControlImpl(s);
		lastState = s;
		return control;
	}
	
	protected abstract double[] getControlImpl(ControllerState s);
}
