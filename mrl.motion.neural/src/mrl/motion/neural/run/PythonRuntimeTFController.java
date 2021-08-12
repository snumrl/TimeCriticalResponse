package mrl.motion.neural.run;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Point3d;

import org.eclipse.swt.SWT;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.MotionSelection;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.data.BallTrajectoryGenerator;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.Normalizer;
import mrl.motion.position.PositionResultMotion;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Utils;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;

public abstract class PythonRuntimeTFController extends RuntimeController{

	public static String MOTION_LABEL = "Motion";
	public static boolean USE_NORMALIZATION = true;
	
	public boolean cameraTracking = false;
	public MainApplication app;
	public TFPython python;
	public PositionResultMotion totalMotion = new PositionResultMotion();
	public ArrayList<Motion> totalMotion2 = new ArrayList<Motion>();
	public ArrayList<double[]> inputList = new ArrayList<double[]>();
	
	public PythonRuntimeTFController() {
		this(false);
	}
	public PythonRuntimeTFController(boolean useBall) {
		MotionDataConverter.setAllJoints();
		if (!useBall) {
			MotionDataConverter.setNoBall();
		} else {
			RuntimeMotionGenerator.ALWAYS_HAS_BALL = true;
		}
	}
	
	public void init(String folder) {
		python = new TFPython(folder, false);
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
		totalMotion = new PositionResultMotion();
		totalMotion2 = new ArrayList<Motion>();
		inputList = new ArrayList<double[]>();
	}
	
	public abstract double[] getControlParameter();
	
	public PositionResultMotion iterateMotion(){
		double[] x = getControlParameter();
		if (x == null) return null;
		inputList.add(x);
		if (USE_NORMALIZATION) x = normal.normalizeX(x);
//		long t = System.currentTimeMillis();
//		System.out.println("xx :: " + Arrays.toString(x));
		double[] output = predict(x);
//		System.out.println("dt : " + (System.currentTimeMillis()-t));
		prevOutput = output;
		if (USE_NORMALIZATION) output = normal.deNormalizeY(output);
//		System.out.println("iterate x :: " + totalMotion.size() + " : " + Arrays.toString(x) + " : " + output[output.length-1]);
		
		PositionResultMotion motion = g.update(output);
		if (motion != null) {
			totalMotion.addAll(motion);
			if (MotionDataConverter.useOrientation) {
				totalMotion2.add(g.motion());
			}
		}
		return motion;
	}
	
	protected double[] predict(double[] x) {
		return python.model.predict(x);
	}
	
	
	public boolean isStop = false;
	Point3d prevBallPos = null;
	
	public double[] prevOutput;
	public void startRuntimeModule(MainApplication app) {
		app.dummyParent().getDisplay().timerExec(1, new Runnable() {
			@Override
			public void run() {
				if (app.dummyParent().isDisposed()) return;
				if (isStop) return;
				
				while (true){
					int dt = (int)(System.currentTimeMillis() - MotionSelection.instance().startTime());
					int tIndex = (int)(dt/(1000/30d));
					if (frame > tIndex) break;
					
					PositionResultMotion motion = iterateMotion();
					if (motion == null) break;
					if (frame == 0) MotionSelection.instance().initStartTime();
					frame++;
					if (MotionDataConverter.useOrientation) {
						Motion m = g.motion();
						m.knot = MotionSelection.instance().getKnot(MotionSelection.instance().startTime() + (int)((1000/30d)*frame));
						app.getModule(ItemListModule.class).addSingleItem(MOTION_LABEL, new MotionData(Utils.singleList(m)));
					} else {
						app.getModule(ItemListModule.class).addSingleItem(MOTION_LABEL, motion);
					}
					if (MotionDataConverter.includeBall) {
						app.getModule(ItemListModule.class).addSingleItem("ball", g.ballPosition(), BallTrajectoryGenerator.ballDescription());
					}
					if (cameraTracking) {
						app.getModule(MainViewerModule.class).addCameraTracking(motion.get(0).get(0)[0]);
					}
				}
				app.dummyParent().getDisplay().timerExec(1, this);
			}
		});
		app.addMenu("&Menu", "&StopAndShowMotion\tCtrl+X", SWT.MOD1 + 'X', new Runnable() {
			@Override
			public void run() {
				isStop = true;
				if (MotionDataConverter.useOrientation) {
					app.getModule(ItemListModule.class).addSingleItem(MOTION_LABEL, new MotionData(totalMotion2));
				} else {
					app.getModule(ItemListModule.class).addSingleItem(MOTION_LABEL, totalMotion);
				}
				onStop();
				app.getModule(MainViewerModule.class).replay();
			} 
		});
	}
	
	protected void onStop() {
		
	}

}
