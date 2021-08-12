package mrl.jni.motion;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import mrl.jni.motion.RootTrajectory.RootInfo;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.graph.MotionSegment;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class MotionEditJNI {
	
	public static double rigidThreshold = 1; // 2.5;
	public static double tangentFlipThreshold = 0.04; 
	
	public static MotionEditJNI instance;
	
	private static boolean isLoaded = false;
	public static void loadScriptapalooza(){
		if (!isLoaded){
			isLoaded = true;
//			addLibraryPath("..\\\\ThirdPartyLibrary");
			if (new File("PmQmJNI.dll").exists() == false) {
				for (File file : new File("..\\ThirdPartyLibrary").listFiles()) {
					String name = file.getName();
					if (name.toLowerCase().endsWith(".dll")) {
						Utils.copyFile(file, new File(name));
					}
				}
			}
			System.loadLibrary("PmQmJNI");
//			if (new File("scriptapalooza.dll").exists()){
//				System.load(new File("scriptapalooza.dll").getAbsolutePath());
//			} else {
//				System.load(new File("Scriptapalooza\\scriptapalooza.dll").getAbsolutePath());
//			}
		}
	}
	static void addLibraryPath(String path) {
		try {
			String libPath = System.getProperty("java.library.path");
			String newPath;
	
			if (libPath == null || libPath.isEmpty()) {
			    newPath = path;
			} else {
			    newPath = path + File.pathSeparator + libPath;
			}
	
			System.setProperty("java.library.path", newPath);
	
			Field field = ClassLoader.class.getDeclaredField("sys_paths");
			field.setAccessible(true);
			    
			// Create override for sys_paths
			ClassLoader classLoader = ClassLoader.getSystemClassLoader(); 
			List<String> newSysPaths = new ArrayList<>();
			newSysPaths.add(path);  
			newSysPaths.addAll(Arrays.asList((String[])field.get(classLoader)));
			           
			field.set(classLoader, newSysPaths.toArray(new String[newSysPaths.size()]));
		} catch (Exception e) {
			System.out.println("Error in : MotionEditJNI.addLibraryPath");
			e.printStackTrace();
			System.out.flush();
			System.exit(0);
		}
	}
	static {
		loadScriptapalooza();
		instance = new MotionEditJNI();
		instance.jniInit();
	}

	public native void jniInit();

	public native void jniEditMotion(int persons, int[] frames, double[] values, 
			int fixFrame, int constraintFrame1,
			int constraintFrame2, double[] relConstraints,
			double rigidThreshold, double[] originBuffer);
	
	public native void jniEditFullMotion(int persons, int[] frames, double[] values, 
			int constraintSize, int[] constraintFrames, double[] constraintValues, double[] originBuffer, 
			double rigidThreshold, double tangentFlipThreshold, double[] deformBuffer);
	
	public native void jniEditRootTrajectory(int persons, int[] frames, double[] values, 
			int constraintSize, int[] constraintFrames, double[] constraintValues, double[] originBuffer, 
			double rigidThreshold, double tangentFlipThreshold, double[] deformBuffer);

	public native void jniShutdown();
	
	public native void jniGetDistanceMatrix(int persons, int[] frames, double[] values, int type, double[] resultBuffer);
	public native double jniGetKineticEnergey(int persons, int[] frames, double[] values, int startFrame, int endFrame);
	public native void jniGetTransitionDiffernce(double[] values, double[] buffer);
	
	public native void jniGetCollisionNumber(int persons, int[] frames, double[] values, int transitions, int[] transitionFrames, int[] resultBuffer);
	public native void jniGetRank(double[] values, int[] rankBuffer, double[] priorBuffer, double[] weight);
	public native void jniGetFootContact(int persons, int[] frames, double[] values, int[] resultBuffer);
	
	public native void jniCleanFootSlip(int persons, int[] frames, double[] values, int[] footContacts); 
			
	public native int jniTest();

	public static String[] jointList = { "Hips", "Spine", "Spine1", "Spine2",
			"Neck", "Head", "LeftShoulder", "LeftArm", "LeftForeArm",
			"LeftHand", "RightShoulder", "RightArm", "RightForeArm",
			"RightHand", "LeftUpLeg", "LeftLeg", "LeftFoot", "LeftToe",
			"RightUpLeg", "RightLeg", "RightFoot", "RightToe", };

	static class JNIMotion{
		int persons;
		int[] frames;
		double[] values;
		int totalFrame;
		
		public JNIMotion(MotionData[] motionDataList) {
			persons = motionDataList.length;
			frames = new int[persons];
			totalFrame = 0;
			for (int i = 0; i < motionDataList.length; i++) {
				MotionData data = motionDataList[i];
				totalFrame += data.motionList.size();
				frames[i] = data.motionList.size();
			}
			values = new double[(jointList.length * 4 + 3 + 1) * totalFrame];

			int idx = 0;
			for (int dataIdx = 0; dataIdx < motionDataList.length; dataIdx++) {
				ArrayList<Motion> motionList = motionDataList[dataIdx].motionList;
				for (int fIdx = 0; fIdx < motionList.size(); fIdx++) {
					Motion motion = motionList.get(fIdx);
					double knot = motion.knot;
					if (Double.isNaN(knot)){
						knot = -10000000;
					}
					values[idx++] = knot;
							
					for (int jIdx = 0; jIdx < jointList.length; jIdx++) {
						Matrix4d m = motion.get(jointList[jIdx]);
						if (jIdx == 0) {
							Vector3d v = MathUtil.getTranslation(m);
							values[idx++] = v.x;
							values[idx++] = v.y;
							values[idx++] = v.z;
						}
						Quat4d q = MathUtil.quat(m);
						values[idx++] = q.w;
						values[idx++] = q.x;
						values[idx++] = q.y;
						values[idx++] = q.z;
					}
				}
			}
		}
		
		public JNIMotion(RootTrajectory[] motionDataList) {
			persons = motionDataList.length;
			frames = new int[persons];
			totalFrame = 0;
			for (int i = 0; i < motionDataList.length; i++) {
				RootTrajectory data = motionDataList[i];
				totalFrame += data.motionList.size();
				frames[i] = data.motionList.size();
			}
			values = new double[(4 + 3 + 1) * totalFrame];

			int idx = 0;
			for (int dataIdx = 0; dataIdx < motionDataList.length; dataIdx++) {
				ArrayList<RootInfo> motionList = motionDataList[dataIdx].motionList;
				for (int fIdx = 0; fIdx < motionList.size(); fIdx++) {
					RootInfo motion = motionList.get(fIdx);
					double knot = motion.knot;
					if (Double.isNaN(knot)){
						knot = -10000000;
					}
					values[idx++] = knot;
							
					Matrix4d m = motion.transf;
					Vector3d v = MathUtil.getTranslation(m);
					values[idx++] = v.x;
					values[idx++] = v.y;
					values[idx++] = v.z;
					Quat4d q = MathUtil.quat(m);
					values[idx++] = q.w;
					values[idx++] = q.x;
					values[idx++] = q.y;
					values[idx++] = q.z;
				}
			}
		}
	}
	
	public MotionData[] cleanFootSlip(MotionData[] motionDataList){
		JNIMotion jni = new JNIMotion(motionDataList);
		int totalFrames = 0;
		for (MotionData m : motionDataList) totalFrames += m.motionList.size();
		int i = 0;
		int[] footContacts = new int[totalFrames*2];
		int lc = 0;
		int rc = 0;
		for (MotionData mData : motionDataList){
			for (Motion motion : mData.motionList){
				footContacts[i*2 + 0] = motion.isLeftFootContact ? 1 : 0;
				footContacts[i*2 + 1] = motion.isRightFootContact ? 1 : 0;
				i++;
				if (motion.isLeftFootContact) lc++;
				if (motion.isRightFootContact) rc++;
			}
		}
		System.out.println("fcc : " + Utils.toString(lc, rc, i));
		
		jniCleanFootSlip(jni.persons, jni.frames, jni.values, footContacts);
		
		boolean isRotateFirst = motionDataList[0].motionList.get(0).isRotateFirst;
		double[] values = jni.values;
		int idx = 0;
		MotionData[] resultList = new MotionData[motionDataList.length];
		for (int dataIdx = 0; dataIdx < resultList.length; dataIdx++) {
			ArrayList<Motion> motionList = new ArrayList<Motion>();
			for (int fIdx = 0; fIdx < jni.frames[dataIdx]; fIdx++) {
				Motion motion = new Motion(isRotateFirst);
				
				Motion origin = motionDataList[dataIdx].motionList.get(fIdx);
				motion.type = origin.type;
				motion.motionIndex = origin.motionIndex;
				motion.isLeftFootContact = origin.isLeftFootContact;
				motion.isRightFootContact = origin.isRightFootContact;
				
				motion.knot = values[idx++];
				for (int jIdx = 0; jIdx < jointList.length; jIdx++) {
					Matrix4d m = new Matrix4d();
					Vector3d v = null;
					if (jIdx == 0) {
						v = new Vector3d(values[idx++], values[idx++],
								values[idx++]);
					}
					Quat4d q = new Quat4d(values[idx + 1], values[idx + 2],
							values[idx + 3], values[idx + 0]);
					idx += 4;
					m.set(q);
					if (v != null) {
						m.setTranslation(v);
					}
					motion.put(jointList[jIdx], m);
				}
				motionList.add(motion);
			}
			resultList[dataIdx] = new MotionData(motionDataList[0].skeletonData, motionList);
			resultList[dataIdx].framerate = motionDataList[0].framerate;
		}
		return resultList;
	}
	
	public double[][] getDistanceMatrix(MotionData[] motionDataList, int typeIndex){
		int n = motionDataList.length;
		
		double[] buffer = new double[n*n];
		
		JNIMotion jni = new JNIMotion(motionDataList);
		jniGetDistanceMatrix(jni.persons, jni.frames, jni.values, typeIndex, buffer);
		
		double[][] matrix = new double[n][n];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				matrix[i][j] = buffer[i*n + j];
			}
		}
		
		return matrix;
	}
	
	public int[] getCollisionNumber(MotionData[] motionDataList, ArrayList<int[]> transitionFrameList){
		int transitions = transitionFrameList.size();
		if (motionDataList.length == 1) return new int[transitions];
		
		int[] transitionBuffer = new int[transitions * 3];
		for (int i = 0; i < transitions; i++) {
			int[] frames = transitionFrameList.get(i);
			for (int j = 0; j < frames.length; j++) {
				transitionBuffer[i*3 + j] = frames[j];
			}
		}
		int[] resultBuffer = new int[transitions];
		
		JNIMotion jni = new JNIMotion(motionDataList);
		jniGetCollisionNumber(jni.persons, jni.frames, jni.values, transitions, transitionBuffer, resultBuffer);
		return resultBuffer;
	}
	
	public double getKineticEnergey(MotionData motionData){
		JNIMotion jni = new JNIMotion(new MotionData[]{ motionData });
		return jniGetKineticEnergey(jni.persons, jni.frames, jni.values, 0, motionData.motionList.size()-1);
	}
	
	public double getKineticEnergey(MotionData motionData, int startFrame, int endFrame){
		JNIMotion jni = new JNIMotion(new MotionData[]{ motionData });
		return jniGetKineticEnergey(jni.persons, jni.frames, jni.values, startFrame, endFrame);
	}
	
	public double[] getTransitionDiffernce(MotionData motionData){
		return getTransitionDiffernce(motionData, 0, motionData.motionList.size()-1);
	}
	public double[] getTransitionDiffernce(MotionData motionData, int startFrame, int endFrame){
		double[] diffBuffer = new double[4];
		double[] values = getOriginBuffer(new Motion[]{
				motionData.motionList.get(startFrame),
				motionData.motionList.get(endFrame),
		});
		jniGetTransitionDiffernce(values, diffBuffer);
		return diffBuffer;
	}

	public void checkFootContact(MotionData motionData){
		JNIMotion jni = new JNIMotion(new MotionData[]{ motionData });
		int[] buffer = new int[motionData.motionList.size()*2];
		jniGetFootContact(jni.persons, jni.frames, jni.values, buffer);
		
		for (int i = 0; i < motionData.motionList.size(); i++) {
			Motion motion = motionData.motionList.get(i);
			motion.isLeftFootContact = (buffer[i*2 + 0] == 1);
			motion.isRightFootContact = (buffer[i*2 + 1] == 1);
		}
	}
	

	public static Vector2d vec2(Vector3d v) {
		return new Vector2d(v.z, v.x);
	}

	public static Vector2d getTranslation(MotionData motionData, int index) {
		Matrix4d m = motionData.motionList.get(index).get(motionData.skeletonData.root.name);
		return vec2(MathUtil.getTranslation(m));
	}

	public static Vector2d getTranslation(Motion motion) {
		Matrix4d m = motion.root();
		return vec2(MathUtil.getTranslation(m));
	}

	public static Vector2d get_local_axis(Vector2d global_v, Vector2d x_axis,
			Vector2d y_axis) {
		double x = global_v.dot(x_axis) / x_axis.length() / x_axis.length();
		double y = global_v.dot(y_axis) / y_axis.length() / y_axis.length();
		return new Vector2d(x, y);
	}
	
	public static Vector2d[] getRelConstraints(Motion[] motions){
		Vector2d[] relConstraints = new Vector2d[4];
		relConstraints[0] = get_rel_pos(motions, 0, 1);
		relConstraints[1] = get_rel_dir(motions, 0, 1);
		relConstraints[2] = get_rel_pos(motions, 1, 0);
		relConstraints[3] = get_rel_dir(motions, 1, 0);
		return relConstraints;
	}

	public static Vector2d get_rel_pos(Motion[] nodeList, int group1, int group2) {
		Vector2d p1_plus1 = getTranslation(nodeList[group1].next);
		Vector2d p1_minus1 = getTranslation(nodeList[group1].prev);
		Vector2d p1 = getTranslation(nodeList[group1]);
		Vector2d p2 = getTranslation(nodeList[group2]);

		Vector2d x_axis = MathUtil.sub(p1_plus1, p1_minus1);
		Vector2d y_axis = new Vector2d(-x_axis.y, x_axis.x);
		return get_local_axis(MathUtil.sub(p2, p1), x_axis, y_axis);
	}

	public static Vector2d get_rel_dir(Motion[] nodeList, int group1, int group2) {
		Vector2d p1_plus1 = getTranslation(nodeList[group1].next);
		Vector2d p1_minus1 = getTranslation(nodeList[group1].prev);
		Vector2d p2_plus1 = getTranslation(nodeList[group2].next);
		Vector2d p2_minus1 = getTranslation(nodeList[group2].prev);

		Vector2d x_axis = MathUtil.sub(p1_plus1, p1_minus1);
		Vector2d y_axis = new Vector2d(-x_axis.y, x_axis.x);
		return get_local_axis(MathUtil.sub(p2_plus1, p2_minus1), x_axis, y_axis);
	}

	public static Vector2d get_rel_pos(MotionData[] motionDataList, int group1,
			int index1, int group2, int index2) {
		Vector2d p1_plus1 = getTranslation(motionDataList[group1], index1 + 1);
		Vector2d p1_minus1 = getTranslation(motionDataList[group1], index1 - 1);
		Vector2d p1 = getTranslation(motionDataList[group1], index1);
		Vector2d p2 = getTranslation(motionDataList[group2], index2);

		Vector2d x_axis = MathUtil.sub(p1_plus1, p1_minus1);
		Vector2d y_axis = new Vector2d(-x_axis.y, x_axis.x);
		return get_local_axis(MathUtil.sub(p2, p1), x_axis, y_axis);
	}

	public static Vector2d get_rel_dir(MotionData[] motionDataList, int group1,
			int index1, int group2, int index2) {
		Vector2d p1_plus1 = getTranslation(motionDataList[group1], index1 + 1);
		Vector2d p1_minus1 = getTranslation(motionDataList[group1], index1 - 1);
		Vector2d p2_plus1 = getTranslation(motionDataList[group2], index2 + 1);
		Vector2d p2_minus1 = getTranslation(motionDataList[group2], index2 - 1);

		Vector2d x_axis = MathUtil.sub(p1_plus1, p1_minus1);
		Vector2d y_axis = new Vector2d(-x_axis.y, x_axis.x);
		return get_local_axis(MathUtil.sub(p2_plus1, p2_minus1), x_axis, y_axis);
	}
	
	public static double[] getOriginBuffer(Motion[] motionList){
		double[] buffer = new double[7*motionList.length];
		int idx = 0;
		for (int i = 0; i < motionList.length; i++) {
			Matrix4d m = motionList[i].root();
			Vector3d v = MathUtil.getTranslation(m);
			buffer[idx++] = v.x;
			buffer[idx++] = v.y;
			buffer[idx++] = v.z;
			Quat4d q = MathUtil.quat(m);
			buffer[idx++] = q.w;
			buffer[idx++] = q.x;
			buffer[idx++] = q.y;
			buffer[idx++] = q.z;
		}
		return buffer;
	}
	
	public RootTrajectory[] editRootTrajectory(RootTrajectory[] mSegmentList, ArrayList<Constraint> constraints) {
		for(Constraint constraint : constraints) {
			if(constraint.posConstraint != null) {
				System.out.println(constraint.constraintPersons[0] + " : " + constraint.constraintFrames[0]);
				System.out.println(constraint.posConstraint[0]+","+constraint.posConstraint[1]+","+constraint.posConstraint[2]);
			}
		}			
		
		double[] deformBuffer = new double[mSegmentList.length*2];
		
		int[] constraintFrames = new int[constraints.size()*4];
		double[] relConstraintBuffer = new double[constraints.size()*8];
		double[] originBuffer = new double[constraints.size()*14];
		for (int i = 0; i < constraints.size(); i++) {
			Constraint c = constraints.get(i);
			constraintFrames[i*4 + 0] = c.constraintPersons[0];
			constraintFrames[i*4 + 1] = c.constraintFrames[0];
			constraintFrames[i*4 + 2] = c.constraintPersons[1];
			constraintFrames[i*4 + 3] = c.constraintFrames[1];
			
			if (c.posConstraint != null){
				for (int j = 0; j < c.posConstraint.length; j++) {
					relConstraintBuffer[i*8 + j] = c.posConstraint[j];
				}
			} else if (c.originMotions == null){
				relConstraintBuffer[i*8] = -99999999;
			} else {
				Vector2d[] relConstraints = getRelConstraints(c.originMotions);
				for (int j = 0; j < relConstraints.length; j++) {
					relConstraintBuffer[i*8 + j*2] = relConstraints[j].x;
					relConstraintBuffer[i*8 + j*2 + 1] = relConstraints[j].y;
				}
				double[] origins = getOriginBuffer(c.originMotions);
				for (int j = 0; j < origins.length; j++) {
					originBuffer[i*origins.length + j] = origins[j];
				}
			}
		}
		JNIMotion jni = new JNIMotion(mSegmentList);
		double[] values = jni.values;
		jniEditRootTrajectory(jni.persons, jni.frames, jni.values,
				constraints.size(), constraintFrames, relConstraintBuffer, originBuffer, rigidThreshold, tangentFlipThreshold, deformBuffer);
//		jniEditFullMotion(jni.persons, jni.frames, jni.values,
//				constraints.size(), constraintFrames, relConstraintBuffer, originBuffer, rigidThreshold, tangentFlipThreshold, deformBuffer);
		
		int idx = 0;
		RootTrajectory[] resultList = new RootTrajectory[mSegmentList.length];
		for (int dataIdx = 0; dataIdx < resultList.length; dataIdx++) {
			ArrayList<RootInfo> motionList = new ArrayList<RootInfo>();
			for (int fIdx = 0; fIdx < jni.frames[dataIdx]; fIdx++) {
				RootInfo motion = new RootInfo();
				
				RootInfo origin = mSegmentList[dataIdx].motionList.get(fIdx);
				motion.motionIndex = origin.motionIndex;
				motion.knot = values[idx++];
				int jIdx = 0;
				Matrix4d m = new Matrix4d();
				Vector3d v = null;
				if (jIdx == 0) {
					v = new Vector3d(values[idx++], values[idx++],
							values[idx++]);
				}
				Quat4d q = new Quat4d(values[idx + 1], values[idx + 2],
						values[idx + 3], values[idx + 0]);
				idx += 4;
				m.set(q);
				if (v != null) {
					m.setTranslation(v);
				}
				motion.transf = m;
				
				motionList.add(motion);
			}
			resultList[dataIdx] = new RootTrajectory(motionList);
		}
		
//		double maxDiffAngle = 0;
//		double maxDiffTime = 0;
//		for (int i = 0; i < motionDataList.length; i++) {
//			ArrayList<Motion> mList1 = motionDataList[i].motionList;
//			ArrayList<Motion> mList2 = resultList[i].motionList;
//			for (int j = 0; j < mList1.size()-1; j++) {
//				Vector3d v1 = getDirection(mList1.get(j), mList1.get(j+1));
//				Vector3d v2 = getDirection(mList2.get(j), mList2.get(j+1));
//				double angle = Math.toDegrees(v1.angle(v2));
//				double timeDiff = Math.abs(mList2.get(j+1).knot - mList2.get(j).knot - 1);
//				maxDiffAngle = Math.max(maxDiffAngle, angle);
//				maxDiffTime = Math.max(maxDiffTime, timeDiff);
//			}
//		}
//		
//		
//		double[] dBuffer = new double[deformBuffer.length + 2];
//		System.arraycopy(deformBuffer, 0, dBuffer, 0, deformBuffer.length);
//		dBuffer[deformBuffer.length + 0] = maxDiffAngle;
//		dBuffer[deformBuffer.length + 1] = maxDiffTime;
//		
//		double[] errorBuffer = relConstraintBuffer;
//		for (int i = 0; i < constraints.size(); i++) {
//			Constraint c = constraints.get(i);
//			c.diffFromOriginInMotion = new double[]{
//					errorBuffer[i*4 + 0], errorBuffer[i*4 + 1],  
//			};
//			c.diffFromOriginInTime = new double[]{
//					errorBuffer[i*4 + 2], errorBuffer[i*4 + 3],  
//			};
//		}
//		
//		for (int i = 0; i < mSegmentList.length; i++) {
//			ArrayList<Motion> mList1 = mSegmentList[i].getEntireMotion();
//			ArrayList<Motion> mList2 = resultList[i].motionList;
//			for (int j = 0; j < mList1.size(); j++) {
//				mList1.set(j, mList2.get(j));
//			}
//		}
		
		
//		return deformBuffer;
//		return dBuffer;
		return resultList;
	}
	public double[] editFullMotion(MotionSegment[] mSegmentList, ArrayList<Constraint> constraints) {
		for(Constraint constraint : constraints) {
			if(constraint.posConstraint != null) {
				System.out.println(constraint.constraintPersons[0] + " : " + constraint.constraintFrames[0]);
				System.out.println(constraint.posConstraint[0]+","+constraint.posConstraint[1]+","+constraint.posConstraint[2]);
			}
		}			
			
		MotionData[] motionDataList = new MotionData[mSegmentList.length];
		for (int i = 0; i < motionDataList.length; i++) {
			motionDataList[i] = new MotionData(mSegmentList[i].getEntireMotion());
		}
		double[] deformBuffer = new double[motionDataList.length*2];
		
		int[] constraintFrames = new int[constraints.size()*4];
		double[] relConstraintBuffer = new double[constraints.size()*8];
		double[] originBuffer = new double[constraints.size()*14];
		for (int i = 0; i < constraints.size(); i++) {
			Constraint c = constraints.get(i);
			constraintFrames[i*4 + 0] = c.constraintPersons[0];
			constraintFrames[i*4 + 1] = c.constraintFrames[0] + Configuration.BLEND_MARGIN;
			constraintFrames[i*4 + 2] = c.constraintPersons[1];
			constraintFrames[i*4 + 3] = c.constraintFrames[1] + Configuration.BLEND_MARGIN;
			
			if (c.posConstraint != null){
				for (int j = 0; j < c.posConstraint.length; j++) {
					relConstraintBuffer[i*8 + j] = c.posConstraint[j];
				}
			} else if (c.originMotions == null){
				relConstraintBuffer[i*8] = -99999999;
			} else {
				Vector2d[] relConstraints = getRelConstraints(c.originMotions);
				for (int j = 0; j < relConstraints.length; j++) {
					relConstraintBuffer[i*8 + j*2] = relConstraints[j].x;
					relConstraintBuffer[i*8 + j*2 + 1] = relConstraints[j].y;
				}
				double[] origins = getOriginBuffer(c.originMotions);
				for (int j = 0; j < origins.length; j++) {
					originBuffer[i*origins.length + j] = origins[j];
				}
			}
		}
		JNIMotion jni = new JNIMotion(motionDataList);
		double[] values = jni.values;
		jniEditFullMotion(jni.persons, jni.frames, jni.values,
				constraints.size(), constraintFrames, relConstraintBuffer, originBuffer, rigidThreshold, tangentFlipThreshold, deformBuffer);

		SkeletonData skeletonData = motionDataList[0].skeletonData;
		boolean isRotateFirst = motionDataList[0].motionList.get(0).isRotateFirst;
		int frameRate = motionDataList[0].framerate;

		int idx = 0;
		MotionData[] resultList = new MotionData[motionDataList.length];
		for (int dataIdx = 0; dataIdx < resultList.length; dataIdx++) {
			ArrayList<Motion> motionList = new ArrayList<Motion>();
			for (int fIdx = 0; fIdx < jni.frames[dataIdx]; fIdx++) {
				Motion motion = new Motion(isRotateFirst);
				
				Motion origin = motionDataList[dataIdx].motionList.get(fIdx);
				motion.type = origin.type;
				motion.motionIndex = origin.motionIndex;
				motion.isLeftFootContact = origin.isLeftFootContact;
				motion.isRightFootContact = origin.isRightFootContact;
				
				motion.knot = values[idx++];
				for (int jIdx = 0; jIdx < jointList.length; jIdx++) {
					Matrix4d m = new Matrix4d();
					Vector3d v = null;
					if (jIdx == 0) {
						v = new Vector3d(values[idx++], values[idx++],
								values[idx++]);
					}
					Quat4d q = new Quat4d(values[idx + 1], values[idx + 2],
							values[idx + 3], values[idx + 0]);
					idx += 4;
					m.set(q);
					if (v != null) {
						m.setTranslation(v);
					}
					motion.put(jointList[jIdx], m);
				}
				motionList.add(motion);
			}
			resultList[dataIdx] = new MotionData(skeletonData, motionList);
			resultList[dataIdx].framerate = frameRate;
		}
		
		double maxDiffAngle = 0;
		double maxDiffTime = 0;
		try{
			for (int i = 0; i < motionDataList.length; i++) {
				ArrayList<Motion> mList1 = motionDataList[i].motionList;
				ArrayList<Motion> mList2 = resultList[i].motionList;
				for (int j = 0; j < mList1.size()-1; j++) {
					Vector3d v1 = getDirection(mList1.get(j), mList1.get(j+1));
					Vector3d v2 = getDirection(mList2.get(j), mList2.get(j+1));
					double angle = Math.toDegrees(v1.angle(v2));
					double timeDiff = Math.abs(mList2.get(j+1).knot - mList2.get(j).knot - 1);
					maxDiffAngle = Math.max(maxDiffAngle, angle);
					maxDiffTime = Math.max(maxDiffTime, timeDiff);
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
		double[] dBuffer = new double[deformBuffer.length + 2];
		System.arraycopy(deformBuffer, 0, dBuffer, 0, deformBuffer.length);
		dBuffer[deformBuffer.length + 0] = maxDiffAngle;
		dBuffer[deformBuffer.length + 1] = maxDiffTime;
		
		double[] errorBuffer = relConstraintBuffer;
		for (int i = 0; i < constraints.size(); i++) {
			Constraint c = constraints.get(i);
			c.diffFromOriginInMotion = new double[]{
				errorBuffer[i*4 + 0], errorBuffer[i*4 + 1],  
			};
			c.diffFromOriginInTime = new double[]{
					errorBuffer[i*4 + 2], errorBuffer[i*4 + 3],  
			};
		}
		
		for (int i = 0; i < mSegmentList.length; i++) {
			ArrayList<Motion> mList1 = mSegmentList[i].getEntireMotion();
			ArrayList<Motion> mList2 = resultList[i].motionList;
			for (int j = 0; j < mList1.size(); j++) {
				mList1.set(j, mList2.get(j));
			}
		}
		
		
//		return deformBuffer;
		return dBuffer;
	}
	
	private static Vector3d getDirection(Motion motion1, Motion motion2){
		Matrix4d m1 = MotionTransform.getPlaneTransform(motion1.root());
		Matrix4d m2 = MotionTransform.getPlaneTransform(motion2.root());
		m1.invert();
		m1.mul(m2);
		return MathUtil.getTranslation(m1);
	}
	
	
	
	private void checkConstraintDiff(Vector2d[] relConstraints, MotionData[] motionDataList, int conIndex1, int conIndex2){
		Vector2d[] constraintResult = new Vector2d[4];
		constraintResult[0] = get_rel_pos(motionDataList, 0, conIndex1, 1, conIndex2);
		constraintResult[1] = get_rel_dir(motionDataList, 0, conIndex1, 1, conIndex2);
		constraintResult[2] = get_rel_pos(motionDataList, 1, conIndex2, 0, conIndex1);
		constraintResult[3] = get_rel_dir(motionDataList, 1, conIndex2, 0, conIndex1);
		double diffSum = 0;
		System.out.print("diff : " + conIndex1 + ", " + conIndex2 + " : ");
		for (int i = 0; i < constraintResult.length; i++) {
			double d = MathUtil.distance(constraintResult[i], relConstraints[i]);
			System.out.print(d + ", ");
			diffSum += d*d;
		}
		System.out.println();
		System.out.println("diff sum : " + Math.sqrt(diffSum));
		double distance = MathUtil.distance(getTranslation(motionDataList[0], conIndex1), getTranslation(motionDataList[1], conIndex2));
		double d1 = MathUtil.distance(getTranslation(motionDataList[0], conIndex1+1), getTranslation(motionDataList[0], conIndex1-1));
		double d2 = MathUtil.distance(getTranslation(motionDataList[1], conIndex2+1), getTranslation(motionDataList[1], conIndex2-1));
		System.out.println("check distance : " + distance + " : " + distance/d1 + " : " + distance/d2 + " ; " + d1 + " : " + d2);
	}
	
}
