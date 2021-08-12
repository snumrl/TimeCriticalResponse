package mrl.motion.data.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.Joint;
import mrl.motion.data.parser.BVHParser;
import mrl.util.MathUtil;

public class BVHMerge {
	
	static String T_POSE_TEXT = "70.6998 89.8849 -23.9736 -5.82128 88.1528 95.2207 3.34024 -0.91851 -1.52529 -2.04886 1.70754e-006 5.12264e-006 -5.12265e-006 1.70755e-006 -8.53773e-006 1.70748e-006 -8.53774e-006 -4.95189e-005 15.7906 0.907602 4.16892 179.59 -5.35096 -94.3879 -4.0507 -25.2464 17.2401 -22.4328 3.18055e-015 -1.59028e-015 22.103 -52.9709 11.851 179.347 7.39849 95.0584 -0.718377 27.1306 -22.0485 -15.6219 -6.36111e-015 3.18055e-015 13.6844 75.6229 -7.49306 1.95523 1.11164 179.087 -0.0815117 -1.98785e-016 -5.70729e-016 0.00935673 -19.6734 1.35701 90 0 -0 -1.9854 -3.60239 179.797 -9.01848 3.97569e-016 4.96962e-017 5.15696 17.9846 6.94956 90 -1.98785e-016 -3.18055e-015";
	
	public static class Person{
		public int index;
		public SkeletonData skeletonData;
		public File seedFile;
		public ArrayList<File> fileList = new ArrayList<File>();
		public int motionCounts = 0;
		public ArrayList<Integer> sizeList = new ArrayList<Integer>();
		
		public Person(int index, SkeletonData skeletonData, File seedFile) {
			this.index = index;
			this.skeletonData = skeletonData;
			this.seedFile = seedFile;
		}
		
		static double getRootHeight(File seedFile){
			MotionData motionData = new BVHParser().parse(seedFile);
			Joint toeEnd = motionData.skeletonData.get("RightToeEnd");
			if (toeEnd == null){
				toeEnd = motionData.skeletonData.get("RightToe" + BVHParser.END_POSTFIX);
			}
			Vector3d translation = new Vector3d();
			while (toeEnd != motionData.skeletonData.root){
				translation.add(toeEnd.transition);
				toeEnd = toeEnd.parent;
			}
			System.out.println("tt : " + translation);
			return Math.abs(translation.y);
//			return Math.abs(translation.z);
		}
		static double getRootHeight2(File seedFile){
			MotionData motionData = new BVHParser().parse(seedFile);
			return MathUtil.getTranslation(motionData.motionList.get(0).get(motionData.skeletonData.root.name)).y;
		}
		
		String getTPose(File seedFile, int seedFrame){
			BVHParser parser = new BVHParser();
			MotionData motionData = parser.parse(seedFile);
			Motion motion = motionData.motionList.get(seedFrame);
//			HashMap<String, Point3d> pointData = Motion.getPointData(motionData.skeletonData, motion);
			String line = "";
			for (int i = 0; i < parser.getJointList().size(); i++) {
				Joint joint = parser.getJointList().get(i);
				Matrix4d m = motion.get(joint.name);
				if (m == null) continue;
				
				line = getTposeJoint(i, joint, m, line);
//				line = getCMUTpose(i, joint, m, line);
			}
			System.out.println("tpose : " + line);
			return line;
		}
		
		private String getCMUTpose(int i, Joint joint, Matrix4d m, String line){
			if (i == 0){
				// Hips
				line += "0 " + MathUtil.getTranslation(m).y + " 0 0 0 0";
//				line += "0 " + MathUtil.getTranslation(m).y + " 0 0 0 0";
			} else {
				if (joint.name.equals("RightUpLeg")){
					line += " 18 0 0";
				} else if (joint.name.equals("LeftUpLeg")){
					line += " -19 0 0";
				} else {
					line += " 0 0 0";
				}
			}
			return line;
		}
		
		private String getTposeJoint(int i, Joint joint, Matrix4d m, String line){
			int lToeOffset = 0;
			int rToeOffset = 0;
			int hipOffset = 0;
			int heightOffset = 0;
			if (index == 1 || index == 3){
				hipOffset = 5;
			}
			if (index == 1){
				rToeOffset = lToeOffset = 10;
			}
			if (index == 4){
				rToeOffset = lToeOffset = 15;
			}
			if (index == 3){
				rToeOffset = -1;
				lToeOffset = 1;
				heightOffset = -2;
			}
			
			if (i == 0){
				// Hips
				line += "0 " + (MathUtil.getTranslation(m).y + heightOffset) + " 0 0 " + hipOffset + " 0";
//				line += "0 " + MathUtil.getTranslation(m).y + " 0 0 0 0";
			} else {
//				CHANNELS 3 Zrotation Xrotation Yrotation
				if (joint.name.equals("Head")){
					line += " 0 -30 0";
				} else if (joint.name.equals("LeftArm")){
					line += " 90 0 0";
				} else if (joint.name.equals("RightArm")){
					line += " -90 0 0";
				} else if (joint.name.equals("LeftFoot")){
					line += " 0 " + lToeOffset + " 0";
				} else if (joint.name.equals("RightFoot")){
	//				if (index == 3){
	//					line += " 22 11 25";
	//				} else {
						line += " 0 " + rToeOffset + " 0";
	//				}
					
				} else if (joint.name.endsWith("UpLeg") || joint.name.equals("Spine")){
					line += " 0 -" + hipOffset + " 0";
				} else {
					line += " 0 0 0";
				}
			}
			return line;
		}
		
		static double[] getToeAngles(String prefix, HashMap<String, Point3d> pointData, SkeletonData skeletonData){
			String j0 = prefix + "Foot";
			String j1 = prefix + "Toe";
			String j2 = prefix + "Toe" + BVHParser.END_POSTFIX;
			Point3d p0 = pointData.get(j0);
			Point3d p1 = pointData.get(j1);
			Point3d p2 = pointData.get(j2);
			Vector3d v1 = skeletonData.get(j1).transition;
			
			Vector3d d1 = new Vector3d();
			d1.sub(p2, p1);
			
			Vector3d d0 = new Vector3d();
			d0.sub(p1, p0);
			
			
			Vector3d upVector = new Vector3d(0, 1, 0);
			double angle1 = Math.toDegrees(upVector.angle(v1));
			double desire1 = Math.toDegrees(upVector.angle(d0));
			return new double[] { desire1 - angle1 };
		}
		
		
		
		private void write(){
			try {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("merged_" + index + ".bvh")));
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(seedFile)));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("Frames:")){
						line = "Frames: " + (motionCounts + 1);
					}
					bw.write(line + "\r\n");
					
					if (line.startsWith("Frame Time")){
						bw.write(T_POSE_TEXT + "\r\n");
//						bw.write("70.6998 89.8849 -23.9736 -5.82128 88.1528 95.2207 3.34024 -0.91851 -1.52529 -2.04886 1.70754e-006 5.12264e-006 -5.12265e-006 1.70755e-006 -8.53773e-006 1.70748e-006 -8.53774e-006 -4.95189e-005 15.7906 0.907602 4.16892 179.59 -5.35096 -94.3879 -4.0507 -25.2464 17.2401 -22.4328 3.18055e-015 -1.59028e-015 22.103 -52.9709 11.851 179.347 7.39849 95.0584 -0.718377 27.1306 -22.0485 -15.6219 -6.36111e-015 3.18055e-015 13.6844 75.6229 -7.49306 1.95523 1.11164 179.087 -0.0815117 -1.98785e-016 -5.70729e-016 0.00935673 -19.6734 1.35701 90 0 -0 -1.9854 -3.60239 179.797 -9.01848 3.97569e-016 4.96962e-017 5.15696 17.9846 6.94956 90 -1.98785e-016 -3.18055e-015\r\n");
//						bw.write(getTPose(seedFile, 0) + "\r\n");
						break;
					}
				}
				
				int startLen = -1;
				for (File file : fileList) {
					br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
					boolean isStarted = false;
					int thisLen = -1;
					while ((line = br.readLine()) != null) {
						if (line.trim().length() == 0) continue;
						if (isStarted){
							if (startLen < 0){
								startLen = line.split(" ").length;
							}
							if (thisLen < 0){
								thisLen = line.split(" ").length;
								if (startLen != thisLen){
									System.out.println(index + " : " + seedFile.getName() + " : " + file.getName() + " : " +startLen + " : " + thisLen);
								}
							}
							bw.write(line + "\r\n");
						}
						if (line.startsWith("Frame Time")) isStarted = true;
					}
				}
				br.close();
				bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static HashMap<Vector3d, Person> loadPersonMap(String folderName, boolean singlePerson, int targetFrameRate){
		File folder = new File(folderName);
		HashMap<Vector3d, Person> personMap = new HashMap<Vector3d, Person>();
		for (File file : folder.listFiles()) {
			String name = file.getName();
			if (!name.endsWith("bvh")) continue;
			
			BVHParser parser = new BVHParser();
			SkeletonData skeletonData = parser.parseSkeleton(file);
			try{
				Vector3d v;
				if (singlePerson){
					v = new Vector3d();
				} else {
					v = new Vector3d(skeletonData.get("RightLeg").length, skeletonData.get("RightFoot").length, skeletonData.get("RightUpLeg").length);
				}
				Person person = personMap.get(v);
				if (person == null){
					person = new Person(personMap.size() + 1, skeletonData, file);
					personMap.put(v, person);
				}
				person.fileList.add(file);
				double ratio;
				if (targetFrameRate > 0) {
					ratio = targetFrameRate/(1d/parser.frameTime);
					System.out.println("ratio:: " + ratio + " : " + parser.frameTime);
				} else {
					ratio = 1;
				}
				int size = MathUtil.round(parser.frameSize*ratio);
				person.motionCounts += size;
				person.sizeList.add(size);
				System.out.println(file.getName() + "\t" + person.index);
			} catch (Exception e){
				System.out.println("file : " + file.getName());
				e.printStackTrace();
			}
		}
		return personMap;
	}

	public static void main(String[] args) {
//		{
//			Matrix3d m1 = new Matrix3d();
//			m1.rotY(Math.toRadians(28));
//			Matrix3d m2 = new Matrix3d();
//			m2.rotZ(Math.toRadians(25));
//			m1.mul(m1, m2);
//			
//			Vector3d e = BVHValidator.matrixToEulerZXY(m1);
//			System.out.println(e);
//			System.exit(0);
//		}
		
		
//		HashMap<Vector3d, Person> personMap = loadPersonMap("D:\\data\\Tennis\\CMU_run_motions\\motion", true);
		
//		HashMap<Vector3d, Person> personMap = loadPersonMap("C:\\Users\\khlee\\git\\MotionGAN\\mrl.motion.neural\\salsa\\motion", true);
		
//		{
//			for (int i = 1; i <= 4; i++) {
//				HashMap<Vector3d, Person> personMap = loadPersonMap("C:\\data\\mocap_data\\man_retargeted_split\\split" + i, true);
//				T_POSE_TEXT = "0.00000 89.37280 0.00000 -5.39376 2.31071 0.57675 2.48176 -0.00790 -0.38465 1.13862 0.07174 -0.42695 5.53173 0.70235 0.53874 -5.18350 -0.04560 0.69946 8.49333 -0.00000 0.00000 13.12910 -0.00000 0.00000 10.16778 -0.40910 4.04130 6.06521 7.75210 61.33207 -0.00000 10.00000 0.00000 -9.16650 -4.83019 -87.27953 0.00000 0.00000 -0.00000 0.00000 0.00000 -0.00000 9.83690 -0.30630 -6.55920 9.30059 -17.74209 -55.94209 0.00000 -10.00000 0.00000 0.00594 4.53698 88.00021 -0.00000 0.00000 -0.00000 -0.00000 0.00000 -0.00000 13.86321 -5.83600 -2.91660 0.90460 -0.00000 -0.00000 -8.32499 -8.60271 2.85789 0.08420 0.00000 0.00000 13.46988 7.84910 0.40830 -0.19580 -0.00000 0.00000 -6.96195 0.15454 -1.97817 -0.73480 0.00000 -0.00000";
//				for (Person person : personMap.values()){
//					person.index = i;
//					person.write();
//				}
//			}
//		}
		
		SkeletonData.USE_SINGLE_SKELETON = false;
		HashMap<Vector3d, Person> personMap = loadPersonMap("C:\\data\\200608_MotionCapture\\retarget", false, -1);
		T_POSE_TEXT = "0 89.3728 0 0.578819 -5.37023 2.365 -0.385014 2.48176 0.00876987 -0.427034 1.13805 0.0802256 0.541226 5.53809 0.650143 0.70233 -5.18367 0.0178574 9.6642e-17 8.49333 -4.01978e-16 3.84245e-16 13.1291 -8.16481e-16 4.10509 10.1134 -1.13216 61.8832 9.69354 -1.64869 3.78471e-15 0 10 -86.5356 4.32839 -9.41222 -1.73937e-15 1.59028e-15 7.95139e-16 -1.73937e-15 1.59028e-15 7.95139e-16 -6.65681 9.80686 0.830499 -57.0286 19.8557 -2.10355 5.60818e-16 3.18055e-15 -10 87.9944 4.53442 0.152701 -5.96354e-16 0 -0 -5.96354e-16 0 -0 -2.99223 14.1415 -5.11184 -1.98785e-16 0.9046 -0 2.85895 -8.7421 -8.17839 1.2424e-16 0.0842 -0 0.416012 13.5253 7.752 1.98785e-16 -0.1958 -0 -1.99287 -6.96312 -0.0870575 -1.03845e-16 -0.7348 1.59041e-15";
		for (Person person : personMap.values()){
			person.write();
		}
	}
}
