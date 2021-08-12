package mrl.motion.viewer.ogre;

import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.util.MathUtil;
import mrl.util.Pair;
import mrl.util.Utils;

public class OgreJNI extends Thread {

	public static final int CONFIG_IS_TENNIS = 0;
	public static final int CONFIG_IS_WOMAN = 1;
	public static final int CONFIG_USE_ARROW = 2;
	public static final int CONFIG_IS_CMU_MOTION = 3;
	
	private static boolean isLoaded = false;
	public static void load(){
		if (!isLoaded){
			isLoaded = true;
			System.loadLibrary("OgreJNI");
		}
	}
	
	public native void jniInit();
	public native void jniOpen(double[] params);
	public native void jniSetMotion(int persons, int frames, int joints, double[] values);
	public native void jniSetBall(int frames, int joints, double[] values);
	public native void jniSetBall2(int frames, int joints, double[] values);
	public native void jniSetRacket(int persons, int frames, int joints, double[] values);
	public native void jniGetStatus(double[] buffer);
	public native void jniSetStatus(double[] buffer);
	public native void jniCaptureScreen(String path);
	public native void jniSetConfig(int type, double[] values);
	
	public native void jniMessagePump();
	public native int jniIsClosed();
	public native void jniClose();
	
	private static OgreJNI instance;
	

	public static void setConfig(int type, double... values){
//		if (instance == null) return;
//		jniSetConfig(type, values);
		configurations.add(new Pair<Integer, double[]>(type, values));
	}
	
	public static boolean isOpened(){
		return instance != null;
	}
	
	public static boolean isClosed(){
		if (instance == null) return true;
		return instance.jniIsClosed() == 1;
	}
	
	public static boolean isCaptured(){
		return instance.capturePath == null;
	}
	
	public static void doCapture(String path){
		instance.capturePath = path;
	}
	
	public static OgreStatus getStatus(){
		if (instance == null) return null;
		double[] b = new double[11];
		instance.jniGetStatus(b);
		Point3d p = new Point3d(b[0], 0, b[1]);
		int key = (int)b[2];
		OgreStatus status = new OgreStatus();
		status.mouse = p;
		status.key = key;
		status.cameraPosition = new Point3d(b[3], b[4], b[5]);
		status.cameraDirection = new Vector3d(b[6], b[7], b[8]);
		status.cursor = new Point2d(b[9], b[10]);
		return status;
	}
	
	public static void setStatus(double[] status){
		synchronized (instance) {
			instance.status = status;
		}
	}
	
	
	public static void setMotion(Motion[] mList){
		MotionData[] mDataList = new MotionData[mList.length];
		for (int i = 0; i < mDataList.length; i++) {
			mDataList[i] = new MotionData(Utils.singleList(mList[i]));
		}
		setMotion(mDataList);
	}
	public static void setMotion(MotionData[] mDataList){
		if (instance == null) return;
		OgreMotion motion = new OgreMotion(mDataList);
		synchronized (instance) {
			instance.ogreMotion = motion;
		}
	}
	
	public static void setBall(ArrayList<Point3d> trajectory){
		if (instance == null) return;
		BallMotion ballMotion = new BallMotion(trajectory);
		synchronized (instance) {
			instance.ballMotion = ballMotion;
		}
	}
	public static void setBall2(ArrayList<Point3d> trajectory){
		if (instance == null) return;
		BallMotion ballMotion = new BallMotion(trajectory);
		synchronized (instance) {
			instance.ballMotion2 = ballMotion;
		}
	}
	public static void setBall2(ArrayList<Point3d> trajectory, ArrayList<Matrix4d> arrowTrajectory){
		if (instance == null) return;
		BallMotion ballMotion = new BallMotion(trajectory, arrowTrajectory);
		synchronized (instance) {
			instance.ballMotion2 = ballMotion;
		}
	}
	
	
	public static void setRacket(ArrayList<ArrayList<Matrix4d>> racketList){
		if (instance == null) return;
		OgreMotion motion = new OgreMotion(racketList);
		synchronized (instance) {
			instance.racketMotion = motion;
		}
	}
	
	private OgreMotion ogreMotion;
	private BallMotion ballMotion;
	private BallMotion ballMotion2;
	private OgreMotion racketMotion;
	private boolean isClosed = false;
	private double[] initialParameters;
	private double[] status;
	private String capturePath = null;
	private static ArrayList<Pair<Integer, double[]>> configurations = new ArrayList<Pair<Integer,double[]>>();
	
	public OgreJNI(double[] initialParameters){
		this.initialParameters = initialParameters;
	}
	
	public void run(){
		load();
		
		jniInit();
		
		System.out.println("set configurations");
		for (Pair<Integer, double[]> pair : configurations){
			jniSetConfig(pair.first, pair.second);
		}
		
		System.out.println("before");
		jniOpen(initialParameters);
		System.out.println("after");
		
		
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		while (true){
			if (isClosed) break;
			jniMessagePump();
			OgreMotion motion = null;
			BallMotion ball = null;
			BallMotion ball2 = null;
			OgreMotion racket = null;
			double[] status = null;
			String capturePath = null;
			synchronized (this) {
				if (ogreMotion != null){
					motion = ogreMotion;
					ogreMotion = null;
				}
				if (ballMotion != null){
					ball = ballMotion;
					ballMotion = null;
				}
				if (ballMotion2 != null){
					ball2 = ballMotion2;
					ballMotion2 = null;
				}
				if (racketMotion != null){
					racket = racketMotion;
					racketMotion = null;
				}
				if (this.status != null){
					status = this.status;
					this.status = null;
				}
				if (this.capturePath != null){
					capturePath = this.capturePath;
					this.capturePath = null;
				}
			}
			if (racket != null){
				jniSetRacket(racket.persons, racket.frames, racket.jointLen, racket.data);
			}
			
			if (motion != null){
				jniSetMotion(motion.persons, motion.frames, motion.jointLen, motion.data);
			}
			if (ball2 != null){
				jniSetBall2(ball2.frames, ball2.jointLen, ball2.data);
			}
			if (ball != null){
				jniSetBall(ball.frames, ball.jointLen, ball.data);
			}
			
			if (status != null){
				jniSetStatus(status);
			}
			if (capturePath != null){
				jniCaptureScreen(capturePath);
			}
		}
		jniClose();
	}
	
	public static void close(){
		if (instance == null) return;
		instance.isClosed = true;
	}
	
	
	public static double[] courtParam(){
		double ratio = 3.35;
		double offset = 948.71*ratio/3.7;
		double[] params = { 1, 0, ratio, offset };
		return params;
	}
	public static void open(){
		open(new double[0]);
	}
	public static void open(double[] initialParameters){
		instance = new OgreJNI(initialParameters);
		instance.start();
	}
	
	public static void waitForOgreClose(){
		while (true){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (OgreJNI.isClosed()) break;
		}
		System.exit(0);
	}
	
	public static void setUseCMUMotion(){
		setConfig(CONFIG_IS_CMU_MOTION, 1);
		JointList = new String[] {
				"Hips",
				"LHipJoint",
				"LeftArm",
				"LeftFoot",
				"LeftForeArm",
				"LeftHand",
				"LeftLeg",
				"LeftShoulder",
				"LeftUpLeg",
				"LowerBack",
				"Neck",
				"RHipJoint",
				"RightArm",
				"RightFoot",
				"RightForeArm",
				"RightHand",
				"RightLeg",
				"RightShoulder",
				"RightUpLeg",
				"Spine",
				"Spine1",
			};
	}
	
//	int aa[] = { 0, 3, 4, 5, 6, 7, /*8,*/ 65, 66, 67, 68, 89, 90, 91, 92, 113, 114, 115, 116, 118, 119, 120, 121 };
	public static String[] JointList = {
		"Hips", "Spine", "Spine1", "Spine2",
		"Neck", "Head", 
		"LeftShoulder", "LeftArm", "LeftForeArm", "LeftHand", 
		"RightShoulder", "RightArm", "RightForeArm", "RightHand", 
		"LeftUpLeg", "LeftLeg", "LeftFoot", "LeftToe",
		"RightUpLeg", "RightLeg", "RightFoot", "RightToe", 
	};
	
	private static class OgreMotion{
		int persons;
		int frames;
		int jointLen;
		double[] data;
		
		public OgreMotion(MotionData[] mDataList){
			persons = mDataList.length;
			frames = mDataList[0].motionList.size();
			jointLen = JointList.length*4 + 3;
			
			ArrayList<Double> values = new ArrayList<Double>();
			for (MotionData mData : mDataList){
				for (Motion motion : mData.motionList){
					Vector3d t = MathUtil.getTranslation(motion.root());
					values.add(t.x);
					values.add(t.y);
					values.add(t.z);
					
					for (String key : JointList){
						Quat4d q = new Quat4d();
						q.set(motion.get(key));
						values.add(q.w);
						values.add(q.x);
						values.add(q.y);
						values.add(q.z);
					}
				}
			}
			data = new double[values.size()];
			for (int i = 0; i < data.length; i++) {
				data[i] = values.get(i);
			}
		}
		
		public OgreMotion(ArrayList<ArrayList<Matrix4d>> racketList){
			persons = racketList.size();
			frames = racketList.get(0).size();
			jointLen = 4 + 3;
			
			ArrayList<Double> values = new ArrayList<Double>();
			for (ArrayList<Matrix4d> mData : racketList){
				for (Matrix4d motion : mData){
					Vector3d t = MathUtil.getTranslation(motion);
					values.add(t.x);
					values.add(t.y);
					values.add(t.z);
					
					Quat4d q = new Quat4d();
					q.set(motion);
					values.add(q.w);
					values.add(q.x);
					values.add(q.y);
					values.add(q.z);
				}
			}
			data = new double[values.size()];
			for (int i = 0; i < data.length; i++) {
				data[i] = values.get(i);
			}
		}
	}
	
	private static class BallMotion{
		int frames;
		int jointLen;
		double[] data;
		
		public BallMotion(ArrayList<Point3d> trajectory){
			this(trajectory, null);
		}
		public BallMotion(ArrayList<Point3d> trajectory, ArrayList<Matrix4d> arrowTrajectory){
			frames = trajectory.size();
			jointLen = 3 + ((arrowTrajectory != null) ? 7 : 0);
			data = new double[frames*jointLen];
			int idx = 0;
			for (int i = 0; i < trajectory.size(); i++) {
				Point3d p = trajectory.get(i);
				if (p == null){
					p = new Point3d(-1, -1, -1);
					p.scale(10000);
//					System.out.println("BALL NULL!!");
				}
				data[idx++] = p.x;
				data[idx++] = p.y;
				data[idx++] = p.z;
				
				if (arrowTrajectory != null){
					Matrix4d motion = arrowTrajectory.get(i);
					if (motion == null){
						motion = new Matrix4d();
						motion.setIdentity();
						motion.setTranslation(new Vector3d(-10000, -10000, -10000));
					}
					
					Vector3d t = MathUtil.getTranslation(motion);
					data[idx++] = t.x;
					data[idx++] = t.y;
					data[idx++] = t.z;
					
					Quat4d q = new Quat4d();
					q.set(motion);
					data[idx++] = q.w;
					data[idx++] = q.x;
					data[idx++] = q.y;
					data[idx++] = q.z;
				}
			}
		}
	}
	
	public static class OgreStatus{
		public Point3d mouse;
		public int key;
		public Point3d cameraPosition;
		public Vector3d cameraDirection;
		public Point2d cursor;
		public Point2d target = new Point2d(-100000, -100000);
		
		public String toString(){
			Point3d p = cameraPosition;
			Vector3d d = cameraDirection;
			return Utils.toString(mouse.x, mouse.z, key, p.x, p.y, p.z, d.x, d.y, d.z, cursor.x, cursor.y, target.x, target.y);
		}
		
		public void setNull(){
			int v = Integer.MIN_VALUE;
			cameraPosition = new Point3d(v, v, v);
			cameraDirection = new Vector3d(v, v, v);
			cursor = new Point2d(v, v);
		}
		
		public void load(double[] d){
			mouse = new Point3d(d[0], 0, d[1]);
			key = (int)d[2];
			
			cameraPosition = new Point3d(d[3], d[4], d[5]);
			cameraDirection = new Vector3d(d[6], d[7], d[8]);
			cursor = new Point2d(d[9], d[10]);
			
			if (d.length > 11){
				target = new Point2d(d[11], d[12]);
			}
		}
		
		public void setCamera(OgreStatus s){
			cameraPosition = s.cameraPosition;
			cameraDirection = s.cameraDirection;
		}
		
		public double[] toData(int motionIndex){
			double[] status = new double[9];
			status[0] = cameraPosition.x;
			status[1] = cameraPosition.y;
			status[2] = cameraPosition.z;
			status[3] = cameraDirection.x;
			status[4] = cameraDirection.y;
			status[5] = cameraDirection.z;
			status[6] = cursor.x;
			status[7] = cursor.y;
			status[8] = motionIndex;
			return status;
		}
	}
}
