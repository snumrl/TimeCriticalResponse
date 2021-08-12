package mrl.motion.neural.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.Joint;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.position.PositionMotion;
import mrl.motion.position.PositionResultMotion;
import mrl.motion.position.PositionResultMotion.PositionFrame;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class MotionDataConverter {

	public static boolean includeBall = true;
	public static boolean includeBallVelocity = true;
//	public static int ROOT_OFFSET = 10;
	public static int ROOT_OFFSET = 13;
	
	public static Motion tposeMotion;
	public static boolean useOrientation = false;
	
	public static boolean useMatrixForAll = true;
	public static boolean useTPoseForMatrix = false;
	
	public static String[] OrientationJointList = {
		"Head",
		"Hips",
		"LeftArm",
		"LeftFoot",
		"LeftForeArm",
		"LeftHand",
		"LeftLeg",
		"LeftShoulder",
		"LeftToe",
		"LeftUpLeg",
		"Neck",
		"RightArm",
		"RightFoot",
		"RightForeArm",
		"RightHand",
		"RightLeg",
		"RightShoulder",
		"RightToe",
		"RightUpLeg",
		"Spine",
		"Spine1",
		"Spine2",
	};
	
	public static void setUseOrientation(Motion baseMotion){
		useOrientation = true;
		tposeMotion = PositionMotion.getAlignedMotion(baseMotion);
		MotionTransform.setTposeMotion(baseMotion);
	}
	public static void setUseOrientation(){
		setUseOrientation(new BVHParser().parse(new File(Configuration.BASE_MOTION_FILE)).motionList.get(0));
	}
	
	public static void setOrientationJointsByFileOrder() {
		SkeletonData s = SkeletonData.instance;
		ArrayList<String> jointNames = new ArrayList<String>();
		for (Joint j : s.jointListByFileOrder) {
			if (tposeMotion.get(j.name) == null) continue;
			jointNames.add(j.name);
		}
		OrientationJointList = Utils.toArray(jointNames);
	}
	
	public static void setNoBall(){
		includeBall = false;
		ROOT_OFFSET = 5;
	}
	
	public static void setNoBallVelocity(){
		includeBallVelocity = false;
		ROOT_OFFSET = 10;
	}
	
	public static PositionResultMotion dataToMotion(ArrayList<double[]> dataList){
		Matrix4d mm = new Matrix4d();
		mm.setIdentity();
		return dataToMotion(dataList, mm);
	}
	
	
	public static PositionResultMotion dataToMotion(ArrayList<double[]> dataList, Matrix4d mm){
		PositionResultMotion motionList = new PositionResultMotion();
		for (int i = 0; i < dataList.size(); i++) {
			double[] input = dataList.get(i);
			PositionFrame pList = dataToPointList(input);
			Matrix4d m = rotMatrix(input[ROOT_OFFSET-3], input[ROOT_OFFSET-2], input[ROOT_OFFSET-1]);
			mm.mul(mm, m);
			
			for (Point3d[] pp : pList){
				for (Point3d p : pp){
					mm.transform(p);
				}
			}
			motionList.add(pList);
		}
		return motionList;
	}
	
	private static Matrix4d rotMatrix(double angle, double tx, double ty){
		Matrix4d m = new Matrix4d();
		m.rotY(angle);
		m.setTranslation(new Vector3d(tx, 0, -ty));
		return m;
	}
	
	public static Motion dataToMotionByOri(double[] data){
		int offset = ROOT_OFFSET;
		Motion motion = new Motion(tposeMotion);
		Vector3d translation = new Vector3d(0, data[offset], 0);
		String[] jointList = OrientationJointList;
		offset = ROOT_OFFSET + 1 + KeyJointList_Origin.length*3;
		for (int j = 0; j < jointList.length; j++) {
			Vector3d v = new Vector3d(data[offset + j*3 + 0], data[offset + j*3 + 1], data[offset + j*3 + 2]);
			Matrix4d matrix = MathUtil.toMatrix(v);
			
			Matrix4d m = new Matrix4d(tposeMotion.get(jointList[j]));
			m.mul(matrix);
			matrix = m;
			
			motion.put(jointList[j], matrix);
		}
		motion.root().setTranslation(translation);
		return motion;
	}
	
	public static Motion dataToMotionByOriMatForAll(double[] data){
		Motion motion = new Motion(tposeMotion);
		Vector3d translation = new Vector3d(0,  data[MotionDataConverter.ROOT_OFFSET], 0);
		String[] jointList = OrientationJointList;
		int offset = ROOT_OFFSET + 1 + KeyJointList_Origin.length*3;
		
		for (int j = 0; j < jointList.length; j++) {
			Matrix4d matrix = new Matrix4d();
	
			Vector3d x = new Vector3d();
			Vector3d y = new Vector3d();
			x.x = data[offset + 0];
			x.y = data[offset + 2];
			x.z = data[offset + 4];
			x.normalize();
	
			y.x = data[offset + 1];
			y.y = data[offset + 3];
			y.z = data[offset + 5];
			y.normalize();
			
			Vector3d y1 = new Vector3d(x);
			y1.scale(y.dot(x));
			y.sub(y, y1);
			y.normalize();

			Vector3d z = new Vector3d();
			z.cross(x, y);

			matrix.setColumn(0, x.x, x.y, x.z, 0);
			matrix.setColumn(1, y.x, y.y, y.z, 0);
			matrix.setColumn(2, z.x, z.y, z.z, 0);
			matrix.setElement(3, 3, 1);
	
			Matrix4d given = new Matrix4d();
			given.setColumn(0, data[offset + 0], data[offset + 2], data[offset+4], 0);
			given.setColumn(1, data[offset + 1], data[offset + 3], data[offset+5], 0);

//			Vector3d root_rot= MathUtil.toVector(matrix);
//			double root_rot_angle= root_rot.length();
//			root_rot.normalize();
			
			offset = offset + 6;

			if (useTPoseForMatrix) {
				Matrix4d m = new Matrix4d(tposeMotion.get(jointList[j]));
				m.mul(matrix);
				matrix = m;
			}
			
			motion.put(jointList[j], new Matrix4d(matrix));
		}
		
		motion.root().setTranslation(translation);

		return motion;
	}
	
	public static HashMap<String, Point3d> dataToPointMap(double[] data){
		if (useMatrixForAll){
			return dataToPointMapByOriMatForAll(data);
		}else if (useOrientation){
			return dataToPointMapByOrientation(data);
		} else {
			return dataToPointMapByPosition(data);
		}
	}
	
	public static HashMap<String, Point3d> dataToPointMapByPosition(double[] data){
		HashMap<String, Point3d> map = new HashMap<String, Point3d>();
		int offset = ROOT_OFFSET;
		map.put(RootJoint, new Point3d(0, data[offset], 0));
		String[] jointList = KeyJointList_Origin;
		offset++;
		for (int j = 0; j < jointList.length; j++) {
			map.put(jointList[j], new Point3d(
					data[offset + j*3+0],
					data[offset + j*3+1],
					data[offset + j*3+2]
				));
		}
		return map;
	}
	
	public static HashMap<String, Point3d> dataToPointMapByOrientation(double[] data){
		int offset = ROOT_OFFSET;
		Motion motion = new Motion(tposeMotion);
		Vector3d translation = new Vector3d(0, data[offset], 0);
		String[] jointList = OrientationJointList;
		offset = ROOT_OFFSET + 1 + KeyJointList_Origin.length*3;
		for (int j = 0; j < jointList.length; j++) {
			Vector3d v = new Vector3d(data[offset + j*3 + 0], data[offset + j*3 + 1], data[offset + j*3 + 2]);
			Matrix4d matrix = MathUtil.toMatrix(v);
			
			Matrix4d m = new Matrix4d(tposeMotion.get(jointList[j]));
			m.mul(matrix);
			matrix = m;
			
			motion.put(jointList[j], matrix);
		}
		motion.root().setTranslation(translation);
		return Motion.getPointData(SkeletonData.instance, motion);
	}
	
	public static HashMap<String, Point3d> dataToPointMapByOriMatForAll(double[] data){
		int offset = ROOT_OFFSET;
		Motion motion = new Motion(tposeMotion);
		Vector3d translation = new Vector3d(0, data[offset], 0);
		String[] jointList = OrientationJointList;
		offset = ROOT_OFFSET + 1 + KeyJointList_Origin.length*3;
		for (int j = 0; j < jointList.length; j++) {
			Vector3d x = new Vector3d(data[offset + j*6 + 0], data[offset + j*6 + 2], data[offset + j*6 + 4]);
			Vector3d y = new Vector3d(data[offset + j*6 + 1], data[offset + j*6 + 3], data[offset + j*6 + 5]);
			x.normalize(); y.normalize();

			Vector3d y1 = new Vector3d(x);
			y1.scale(y.dot(x));
			y.sub(y, y1);
			y.normalize();

			Vector3d z = new Vector3d();
			z.cross(x, y);
			
			Matrix4d matrix= new Matrix4d();
			matrix.setColumn(0, x.x, x.y, x.z, 0);
			matrix.setColumn(1, y.x, y.y, y.z, 0);
			matrix.setColumn(2, z.x, z.y, z.z, 0);
			matrix.setElement(3, 3, 1);
			
			if (useTPoseForMatrix) {
				Matrix4d m = new Matrix4d(tposeMotion.get(jointList[j]));
				m.mul(matrix);
				matrix = m;
			}
			
			motion.put(jointList[j], matrix);
		}
		motion.root().setTranslation(translation);
		return Motion.getPointData(SkeletonData.instance, motion);
	}
	
	public static PositionFrame dataToPointList(double[] data){
		PositionFrame pList =  getJointPositions(dataToPointMapByPosition(data));
//		PositionFrame pList =  getJointPositions(dataToPointMap(data));
		return pList;
	}
	
//	public static ArrayList<double[]> motionToJoint(ArrayList<Motion> mList, Motion tPose){
//		ArrayList<double[]> result = new ArrayList<double[]>();
//		
//		ArrayList<String> jointList = new ArrayList<String>();
//		for (String j : SkeletonData.keyList){
//			if (tPose.get(j) != null){
//				jointList.add(j);
//			}
//		}
//		
//		int dLen = 1 + jointList.size()*3;
//		for (Motion motion : mList){
//			double[] data = new double[dLen];
//			data[0] = MathUtil.getTranslation(motion.root()).y;
//			boolean error = false;
//			for (int i = 0; i < jointList.size(); i++) {
//				String joint = jointList.get(i);
////				Quat4d q = new Quat4d();
////				q.set(motion.get(jointList.get(i)));
//				
//				Matrix4d matrix = motion.get(joint);
//				Matrix4d m = new Matrix4d(tPose.get(joint));
//				m.invert();
//				m.mul(matrix);
//				matrix = m;
//				
//				Vector3d v = MathUtil.toVector(matrix);
//				if (v.length() > Math.PI - 0.1){
//					AxisAngle4d a = new AxisAngle4d();
//					a.set(matrix);
//					System.out.println(motion.motionData.file.getName() + " : " + motion.frameIndex + " : " + result.size() + " : " + joint + "\t\t" + v.length() + " : " + v);
//					error =  true;
////					break;
//					throw new RuntimeException();
//				}
//				data[i*3 + 1] = v.x;
//				data[i*3 + 2] = v.y;
//				data[i*3 + 3] = v.z;
//			}
//			if (error) continue;
//		}
//		return result;
//	}
	
	public static ArrayList<double[]> motionToData(MotionSegment segment){
		ArrayList<Motion> mList = segment.getMotionList();
		Motion first = segment.getEntireMotion().get(Configuration.BLEND_MARGIN-1);
		return motionToData(mList, first, false);
	}
	
	public static ArrayList<double[]> motionToData(MDatabase database){
		ArrayList<double[]> dataList = new ArrayList<double[]>();
		for (MotionData mData : database.getMotionDataList()) {
			ArrayList<double[]> dList = MotionDataConverter.motionToData(mData.motionList, mData.firstMotion(), false);
			dataList.addAll(dList);
		}
		return dataList;
	}
	
	public static ArrayList<double[]> motionToData(ArrayList<Motion> mList, Motion first, boolean mirror){
		ArrayList<PositionMotion> pmList = new ArrayList<PositionMotion>();
		ArrayList<double[]> inputList = new ArrayList<double[]>();
		for (Motion motion : mList){
			if (mirror){
				pmList.add(new PositionMotion(Motion.mirroredMotion(motion)));
			} else {
				pmList.add(new PositionMotion(motion));
			}
		}
		PositionMotion firstMotion;
		if (mirror){
			firstMotion = new PositionMotion(Motion.mirroredMotion(first));
		} else {
			firstMotion = new PositionMotion(first);
		}
		
		
		ArrayList<Point3d> ballTrajectory = null;
		if (includeBall){
			ballTrajectory = new BallTrajectoryGenerator().generate(mList);
		}		
		for (int i = 0; i < pmList.size(); i++) {
			PositionMotion pm = pmList.get(i);
			double root_height = pm.pointData.get(RootJoint).y;
			double[] data;
			if(useMatrixForAll) 
			{
				data = getPositionData(KeyJointList, pm.pointData);
				data = MathUtil.concatenate(data, getOrientationDataWithMatForAll(OrientationJointList, mList.get(i)));		
			}
			else if (useOrientation){
				data = getPositionData(KeyJointList, pm.pointData);
				data = MathUtil.concatenate(data, getOrientationData(OrientationJointList, mList, i));
			} else {
				data = getPositionData(KeyJointList, pm.pointData);
			}
			
			Pose2d pose = pm.pose;
			Pose2d baseP = (i == 0) ? firstMotion.pose : pmList.get(i-1).pose;
			Matrix2d t = Pose2d.localTransform(baseP, pose);
			double angle = t.getAngle();
			Vector2d translation = t.getTranslation();
			double tx = translation.x;
			double ty = translation.y;
			double[] root = new double[]{
					getFootContact(i, true, pmList),
					getFootContact(i, false, pmList),
					angle, tx, ty, 
					root_height
			};
			
			if (ballTrajectory != null){
				Point3d b0 = getRelativeBall(ballTrajectory.get(Math.max(0, i-1)), pose);
				Point3d b1 = getRelativeBall(ballTrajectory.get(i), pose);
				Vector3d bv = MathUtil.sub(b1, b0);
				if (includeBallVelocity){
					root = MathUtil.concatenate(new double[]{ 
								b1.x, b1.y, b1.z,
								bv.x, bv.y, bv.z,
								getBallContact(i, true, pmList),
								getBallContact(i, false, pmList),
							}, root);
				} else {
					root = MathUtil.concatenate(new double[]{ 
							b1.x, b1.y, b1.z,
							getBallContact(i, true, pmList),
							getBallContact(i, false, pmList),
					}, root);
				}
			}
			
			double[] input = MathUtil.concatenate(root, data);
			inputList.add(input);
		}
		return inputList;
	}
	
	public static void printOutputSpec() {
		System.out.println("################");
		ArrayList<String> list = new ArrayList<String>();
//		getFootContact(i, true, pmList),
//		getFootContact(i, false, pmList),
//		angle, tx, ty, 
//		root_height
		list.add("LFootContact");
		list.add("RFootContact");
		list.add("root_rotation");
		list.add("root_tx");
		list.add("root_ty");
		list.add("root_height");
		
		for (String j : KeyJointList) {
			list.add(j + "_pos_x");
			list.add(j + "_pos_y");
			list.add(j + "_pos_z");
		}
		if (useOrientation){
			for (String j : OrientationJointList) {
				list.add(j + "_ori_x");
				list.add(j + "_ori_y");
				list.add(j + "_ori_z");
			}
		}
		for (int i = 0; i < list.size(); i++) {
			System.out.println(i + "\t" + list.get(i));
		}
		System.out.println("################");
	}
	
	private static Point3d getRelativeBall(Point3d b, Pose2d pose){
		Point3d r = Pose2d.to3d(pose.globalToLocal(Pose2d.to2d(b)));
		r.y = b.y;
		return r;
	}
	
	public static boolean[] getNormalMarking(){
		// set contact as std 1
		boolean[] mark = new boolean[ROOT_OFFSET - 3];
		for (int i = 0; i < mark.length; i++) {
			if (i < mark.length-4){
				mark[i] = false;
			} else {
				mark[i] = true;
			}
		}
		return mark;
//		return new boolean[]{ false, false, false, false, false, false, true, true, true, true };
//		return new boolean[]{ false, false, false, true, true, true, true };
//		return new boolean[]{ true, true };
	}
	
	private static double getFootContact(int index, boolean isLeft, ArrayList<PositionMotion> pmList){
		Motion[] mList = {
			Utils.getSafe(pmList, index-1).motion,
			Utils.getSafe(pmList, index).motion,
			Utils.getSafe(pmList, index+1).motion,
		};
		boolean[] contacts = new boolean[mList.length];
		for (int i = 0; i < contacts.length; i++) {
			if (isLeft){
				contacts[i] = mList[i].isLeftFootContact;
			} else {
				contacts[i] = mList[i].isRightFootContact;
			}
		}
		
		if (contacts[1]){
			if (!contacts[0] || !contacts[2]) return 0.66;
			return 1;
		} else {
			if (contacts[0] || contacts[2]) return 0.33;
			return 0;
		}
	}
	
	private static double getBallContact(int index, boolean isLeft, ArrayList<PositionMotion> pmList){
		Motion[] mList = {
				Utils.getSafe(pmList, index-1).motion,
				Utils.getSafe(pmList, index).motion,
				Utils.getSafe(pmList, index+1).motion,
		};
		boolean[] contacts = new boolean[mList.length];
		for (int i = 0; i < contacts.length; i++) {
			if (isLeft){
				contacts[i] = mList[i].ballContact.left;
			} else {
				contacts[i] = mList[i].ballContact.right;
			}
		}
		
		if (contacts[1]){
			if (!contacts[0] || !contacts[2]) return 0.66;
			return 1;
		} else {
			if (contacts[0] || contacts[2]) return 0.33;
			return 0;
		}
	}
	
	public static double[] getPositionData(String[] jointList, HashMap<String, Point3d> pointMap){
		double[] data = new double[jointList.length*3];
		for (int i = 0; i < jointList.length; i++) {
			Point3d p = pointMap.get(jointList[i]);
			data[i*3 + 0] = p.x;
			data[i*3 + 1] = p.y;
			data[i*3 + 2] = p.z;
		}
		return data;
	}
	
	public static double[] getOrientationDataWithMatForAll(String[] jointList, Motion motion)
	{
		double[] data = new double[jointList.length*6];
		motion = PositionMotion.getAlignedMotion(motion);
		for (int i = 0; i < jointList.length; i++) {
			Matrix4d matrix = motion.get(jointList[i]);
			if (useTPoseForMatrix) {
				Matrix4d m = new Matrix4d(tposeMotion.get(jointList[i]));
				m.invert();
				m.mul(matrix);
				matrix = m;
			}
			
			data[i*6+ 0]= matrix.m00; data[i*6+1]= matrix.m01;
			data[i*6+ 2]= matrix.m10; data[i*6+3]= matrix.m11;
			data[i*6+ 4]= matrix.m20; data[i*6+5]= matrix.m21;
		
		}
		return data;
	}
	
	public static double[] getOrientationData(String[] jointList, ArrayList<Motion> mList, int mIndex){
		Motion motion = mList.get(mIndex);
		double[] data = new double[jointList.length*3];
		motion = PositionMotion.getAlignedMotion(motion);
		for (int i = 0; i < jointList.length; i++) {
			Matrix4d matrix = motion.get(jointList[i]);
			Matrix4d m = new Matrix4d(tposeMotion.get(jointList[i]));
			m.invert();
			m.mul(matrix);
			matrix = m;
			
			Vector3d v = MathUtil.toVector(matrix);
			if (v.length() > Math.PI - 0.05 && !jointList[i].equals("LeftHand")){
				AxisAngle4d a = new AxisAngle4d();
				a.set(matrix);
				System.out.println(mIndex + " : " + motion.motionData.file.getName() + " : " + motion.frameIndex + " : " + jointList[i] + "\t\t" + v.length() + " : " + v);
//				throw new RuntimeException();
			}
			data[i*3 + 0] = v.x;
			data[i*3 + 1] = v.y;
			data[i*3 + 2] = v.z;
		}
		return data;
	}
	
	public static PositionFrame getJointPositions(HashMap<String, Point3d> map){
		PositionFrame list = new PositionFrame();
		for (String[] pair : jointPairs){
			for (int i = 0; i < pair.length-1; i++) {
				Point3d p1 = map.get(pair[i]);
				Point3d p2 = map.get(pair[i+1]);
				if (p1 == null || p2 == null) continue;
				list.add(new Point3d[]{
						new Point3d(p1), new Point3d(p2)
				});
			}
		}
		return list;
	}
	
	public static String RootJoint = "Hips";
	
	
	public static String[] KeyJointList = {
		"Head_End",
//		"Hips",
		"LeftHand",
//		"LeftHand_End",
		"LeftFoot",
		"LeftToe",
		"RightHand",
//		"RightHand_End",
		"RightFoot",
		"RightToe",
		
		"LeftArm",
		"RightArm",
		
		"LeftForeArm",
		"LeftLeg",
		"RightForeArm",
		"RightLeg",
	};
	public static String[][] jointPairs = {
			{ "Hips", "LeftLeg", "LeftFoot", "LeftToe" },
			{ "Hips", "LeftArm", "LeftForeArm", "LeftHand", "LeftHand_End" },
			{ "Hips", "RightLeg", "RightFoot", "RightToe" },
			{ "Hips", "RightArm", "RightForeArm", "RightHand", "RightHand_End" },
			{ "Hips", "Head_End" },
	};
	public static String[] KeyJointList_Origin = KeyJointList;
	
//	static{
//		setAllJoints();
//	}
	public static void setAllJoints(){
		KeyJointList = new String[]{
			"Head_End",
			"LeftHand",
			"LeftFoot",
			"LeftToe_End",
			"RightHand",
			"RightFoot",
			"RightToe_End",
			
			"LeftArm",
			"RightArm",
			
			"LeftForeArm",
			"LeftLeg",
			"RightForeArm",
			"RightLeg",
			
			// added
			"Spine",
			"LeftHand_End",
			"RightHand_End",
			"Neck",
			"LeftUpLeg",
			"RightUpLeg",
		};
		jointPairs = new String[][]{
				{ "Hips", "LeftUpLeg", "LeftLeg", "LeftFoot", "LeftToe_End" },
				{ "Neck", "LeftArm", "LeftForeArm", "LeftHand", "LeftHand_End" },
				{ "Hips", "RightUpLeg", "RightLeg", "RightFoot", "RightToe_End" },
				{ "Neck", "RightArm", "RightForeArm", "RightHand", "RightHand_End" },
				{ "Hips", "Spine", "Neck", "Head_End" },
		};
		KeyJointList_Origin = KeyJointList;
	}
	
	public static void setCMUJointSet(){
		setCMUAllJoints();
//		FootContactDetection.CONTACT_MARGIN = 2;
//		FootContactDetection.rightFootJoints = new String[]{ "RightFoot", "RightToeBase", "RightToeBase_End" } ;
//		FootContactDetection.leftFootJoints = new String[]{ "LeftFoot", "LeftToeBase", "LeftToeBase_End" } ;
//		MotionDataConverter.KeyJointList = new String[]{
//				"Head_End",
////				"Hips",
//				
//				"LeftHand",
////				"LeftHandIndex1",
////				"LThumb_End",
//				"LeftFoot",
//				"LeftToeBase",
//				"RightHand",
////				"RightHandIndex1",
////				"RThumb_End",
//				"RightFoot",
//				"RightToeBase",
//				
//				"LeftArm",
//				"RightArm",
//				
//				"LeftForeArm",
//				"LeftLeg",
//				"RightForeArm",
//				"RightLeg",
//			};
	}
	
	public static void setCMUAllJoints(){
		setNoBall();
//		FootContactDetection.CONTACT_MARGIN = 2;
		FootContactDetection.rightFootJoints = new String[]{ "RightFoot", "RightToeBase", "RightToeBase_End" } ;
		FootContactDetection.leftFootJoints = new String[]{ "LeftFoot", "LeftToeBase", "LeftToeBase_End" } ;
		FootContactDetection.heightLimit = new double[]{ 16, 11, 9 };
		
		MotionDataConverter.KeyJointList = new String[]{
			"Head_End",
			"LeftHand",
			"LeftFoot",
			"LeftToeBase",
			"RightHand",
			"RightFoot",
			"RightToeBase",
			
			"LeftArm",
			"RightArm",
			
			"LeftForeArm",
			"LeftLeg",
			"RightForeArm",
			"RightLeg",
			
			// added
			"Spine",
			"LeftHandIndex1",
			"RightHandIndex1",
			"Neck1",
			"LeftUpLeg",
			"RightUpLeg",
		};
		MotionDataConverter.jointPairs = new String[][]{
				{ "Hips", "LeftUpLeg", "LeftLeg", "LeftFoot", "LeftToeBase" },
				{ "Neck", "LeftArm", "LeftForeArm", "LeftHand", "LeftHandIndex1" },
				{ "Hips", "RightUpLeg", "RightLeg", "RightFoot", "RightToeBase" },
				{ "Neck", "RightArm", "RightForeArm", "RightHand", "RightHandIndex1" },
				{ "Hips", "Spine", "Neck1", "Head_End" },
		};
		MotionDataConverter.KeyJointList_Origin = MotionDataConverter.KeyJointList;
		MotionDataConverter.OrientationJointList = new String[]{
				"Head",
				"Hips",
				"LHipJoint",
//				"LThumb",
				"LeftArm",
//				"LeftFingerBase",
				"LeftFoot",
				"LeftForeArm",
				"LeftHand",
//				"LeftHandIndex1",
				"LeftLeg",
				"LeftShoulder",
				"LeftToeBase",
				"LeftUpLeg",
				"LowerBack",
				"Neck",
				"Neck1",
				"RHipJoint",
//				"RThumb",
				"RightArm",
//				"RightFingerBase",
				"RightFoot",
				"RightForeArm",
				"RightHand",
//				"RightHandIndex1",
				"RightLeg",
				"RightShoulder",
				"RightToeBase",
				"RightUpLeg",
				"Spine",
				"Spine1",
		};
	}
	
	public static void setDanceCardAllJoints(){
		setNoBall();
//		FootContactDetection.CONTACT_MARGIN = 2;
		FootContactDetection.rightFootJoints = new String[]{ "RightFoot", "RightToeBase", "RightToeBase_End" } ;
		FootContactDetection.leftFootJoints = new String[]{ "LeftFoot", "LeftToeBase", "LeftToeBase_End" } ;
		FootContactDetection.heightLimit = new double[]{ 16, 11, 9 };
		
		MotionDataConverter.KeyJointList = new String[]{
			"Head_End",
			"LeftHand",
			"LeftFoot",
			"LeftToeBase",
			"RightHand",
			"RightFoot",
			"RightToeBase",
			
			"LeftArm",
			"RightArm",
			
			"LeftForeArm",
			"LeftLeg",
			"RightForeArm",
			"RightLeg",
			
			// added
			"Spine",
			"LeftHandIndex1",
			"RightHandIndex1",
			"Neck1",
			"LeftUpLeg",
			"RightUpLeg",
		};
		MotionDataConverter.jointPairs = new String[][]{
				{ "Hips", "LeftUpLeg", "LeftLeg", "LeftFoot", "LeftToeBase" },
				{ "Neck", "LeftArm", "LeftForeArm", "LeftHand", "LeftHandIndex1" },
				{ "Hips", "RightUpLeg", "RightLeg", "RightFoot", "RightToeBase" },
				{ "Neck", "RightArm", "RightForeArm", "RightHand", "RightHandIndex1" },
				{ "Hips", "Spine", "Neck1", "Head_End" },
		};
		MotionDataConverter.KeyJointList_Origin = MotionDataConverter.KeyJointList;
//		MotionDataConverter.OrientationJointList = new String[]{
//				"Head",
//				"Hips",
//				"LHipJoint",
////				"LThumb",
//				"LeftArm",
////				"LeftFingerBase",
//				"LeftFoot",
//				"LeftForeArm",
//				"LeftHand",
////				"LeftHandIndex1",
//				"LeftLeg",
//				"LeftShoulder",
//				"LeftToeBase",
//				"LeftUpLeg",
//				"LowerBack",
//				"Neck",
//				"Neck1",
//				"RHipJoint",
////				"RThumb",
//				"RightArm",
////				"RightFingerBase",
//				"RightFoot",
//				"RightForeArm",
//				"RightHand",
////				"RightHandIndex1",
//				"RightLeg",
//				"RightShoulder",
//				"RightToeBase",
//				"RightUpLeg",
//				"Spine",
//				"Spine1",
//		};
	}
	
	public static void main(String[] args) {
		setAllJoints();
//		{
//			String[] imp = {
//					"LeftFoot", "LeftToe_End", "LeftHand", "LeftHand_End",
//					"RightFoot", "RightToe_End", "RightHand", "RightHand_End",
//			};
//			String[] notImp = {
//				"LeftUpLeg", "LeftArm", "RightUpLeg", "RightArm", "Spine"
//			};
//			HashSet<String> impSet = new HashSet<String>();
//			impSet.addAll(Utils.toList(imp));
//			HashSet<String> notImpSet = new HashSet<String>();
//			notImpSet.addAll(Utils.toList(notImp));
//			int count = 0;
//			for (String key : KeyJointList){
//				double weight = 1;
//				if (impSet.contains(key)) weight = 5;
//				if (notImpSet.contains(key)) weight = 0.5;
//				for (int i = 0; i < 3; i++) {
//					System.out.print(weight + ",");
//					count++;
//				}
//			}
//			System.out.println();
//			System.out.println(count);
//			System.exit(0);
//		}
		
		{
		ArrayList<String> list = Utils.toList(KeyJointList);
		for (int jIdx = 0; jIdx < jointPairs.length-1; jIdx++) {
			String[] pair = jointPairs[jIdx];
			for (int i = 1; i < pair.length-2; i++) {
				System.out.print("[ " + list.indexOf(pair[i]) + ", " + list.indexOf(pair[i+1]) + " ],");
			}
		}
		System.out.println("##################");
		for (int jIdx = 0; jIdx < jointPairs.length-1; jIdx++) {
			String[] pair = jointPairs[jIdx];
			for (int i = 1; i < pair.length-2; i++) {
				System.out.print("[ " +pair[i] + ", " + pair[i+1] + " ],");
			}
		}
		System.out.println("##################");
		MotionTransform t = new MotionTransform();
		for (int jIdx = 0; jIdx < jointPairs.length-1; jIdx++) {
			String[] pair = jointPairs[jIdx];
			for (int i = 1; i < pair.length-2; i++) {
				System.out.print(t.skeletonData.get(pair[i+1]).length + ",");
			}
		}
		System.out.println();
		System.exit(0);
		}
		
		int[] indices = { 2, 3, 5, 6 };
//		int[] indices = { 14, 15 };
		System.out.println(KeyJointList.length);
		for (int i : indices){
			System.out.println(KeyJointList[i]);
		}
		System.exit(0);
		ArrayList<String> list = Utils.toList(KeyJointList);
		for (String[] pairs : jointPairs){
			System.out.print("[");
			for (String s : pairs) {
				int index = list.indexOf(s);
				if (index < 0 && !"Hips".equals(s)) continue;
				System.out.print((index+1) + ",");
			}
			System.out.println("],");
		}
	}
}
