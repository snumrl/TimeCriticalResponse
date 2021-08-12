package mrl.motion.viewer.kinect;

import static mrl.motion.viewer.kinect.KinectJNI.JointType_HipLeft;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_HipRight;
import static mrl.motion.viewer.kinect.KinectJNI.JointType_SpineBase;

import java.io.Serializable;
import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.trasf.Pose2d;
import mrl.util.MathUtil;

public class KinectJNI extends Thread{

	
	private static KinectJNI instance;
	private static boolean isLoaded = false;
    private static void load(){
        if (!isLoaded){
            isLoaded = true;
            System.loadLibrary("KinectJNI");
        }
    }
    

    public native void jniStart(double[] params);
    public native void jniStop();
    public native void jniUpdate(double[] buffer);
    
    
    private double[] initialParameters;
    private KinectJoints status;
    private boolean isStop = false;
    
    
    private KinectJNI(double[] initialParameters) {
		this.initialParameters = initialParameters;
	}


	public static void startKinect(double... params) {
    	instance = new KinectJNI(params);
    	instance.start();
    }
	
	public static void stopKinect() {
		if (instance == null) return;
		instance.isStop = true;
	}
	
	public static KinectJoints getStatus() {
		if (instance == null) return null;
		return instance.status;
	}
    
    
    @Override
    public void run() {
    	load();
    	
    	System.out.println("before");
        jniStart(initialParameters);
        System.out.println("after");
        
        try {
	        while (true) {
	        	if (isStop) {
	        		jniStop();
	        		Thread.sleep(100);
	        		break;
	        	}
	        	
	        	status = update();
	        }
        
        } catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    private KinectJoints update() {
    	double[] buffer = new double[1000];
    	jniUpdate(buffer);
    	KinectJoints status = null;
    	int bIdx = 0;
    	while (true) {
    		int personIndex = (int)buffer[bIdx++];
    		if (personIndex < 0) break;
    		
    		status = new KinectJoints();
    		int jointCount = (int)buffer[bIdx++];
    		for (int i = 0; i < jointCount; i++) {
    			Point3d p = new Point3d(buffer[bIdx++], buffer[bIdx++], buffer[bIdx++]);
    			p.scale(100);
    			p.y += 200;
				status.jointPositions.add(p);
				status.jointStatus.add((int)buffer[bIdx++]);
			}
    		status.leftHandState = (int)buffer[bIdx++];
    		status.rightHandState = (int)buffer[bIdx++];
    		break;
    	}
    	return status;
    }
    
    public static class KinectJoints implements Serializable{
		private static final long serialVersionUID = -3149138329317995867L;
		public ArrayList<Point3d> jointPositions = new ArrayList<>();
    	public ArrayList<Integer> jointStatus = new ArrayList<>();
    	public int leftHandState = -1;
    	public int rightHandState = -1;
    	
    	public KinectJoints getAlignedToBase(){
    		KinectJoints data = this;
			Point3d root = data.jointPositions.get(JointType_SpineBase);
			Point3d lHip = data.jointPositions.get(JointType_HipLeft);
			Point3d rHip = data.jointPositions.get(JointType_HipRight);
			Vector3d v = MathUtil.sub(rHip, lHip);
			Vector3d xAxis = new Vector3d(v.z, v.y, -v.x);
			
			Pose2d pose = new Pose2d(Pose2d.to2d(root), Pose2d.to2d(xAxis));
			Matrix4d mm = Pose2d.globalTransform(pose, Pose2d.BASE).to3d();
			
			KinectJoints newData = new KinectJoints();
			newData.jointStatus.addAll(data.jointStatus);
			newData.leftHandState = data.leftHandState;
			newData.rightHandState = data.rightHandState;
			for (Point3d p : data.jointPositions){
				p = new Point3d(p);
				p.y -= 120;
				mm.transform(p);
				newData.jointPositions.add(p);
			}
			return newData;
    	}
    }
    
    public static int JointType_SpineBase	= 0,
            JointType_SpineMid	= 1,
            JointType_Neck	= 2,
            JointType_Head	= 3,
            JointType_ShoulderLeft	= 4,
            JointType_ElbowLeft	= 5,
            JointType_WristLeft	= 6,
            JointType_HandLeft	= 7,
            JointType_ShoulderRight	= 8,
            JointType_ElbowRight	= 9,
            JointType_WristRight	= 10,
            JointType_HandRight	= 11,
            JointType_HipLeft	= 12,
            JointType_KneeLeft	= 13,
            JointType_AnkleLeft	= 14,
            JointType_FootLeft	= 15,
            JointType_HipRight	= 16,
            JointType_KneeRight	= 17,
            JointType_AnkleRight	= 18,
            JointType_FootRight	= 19,
            JointType_SpineShoulder	= 20,
            JointType_HandTipLeft	= 21,
            JointType_ThumbLeft	= 22,
            JointType_HandTipRight	= 23,
            JointType_ThumbRight	= 24;
      public static int JointType_Count	= ( JointType_ThumbRight + 1 );
      
      public static int TrackingState_NotTracked	= 0,
		    	        TrackingState_Inferred	= 1,
		    	        TrackingState_Tracked	= 2;
      
      public static int HandState_Unknown	= 0,
    	        HandState_NotTracked	= 1,
    	        HandState_Open	= 2,
    	        HandState_Closed	= 3,
    	        HandState_Lasso	= 4;
}
