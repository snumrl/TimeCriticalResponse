package mrl.motion.data.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import static java.lang.Math.*;

public class BVHValidator {
	
	public static Vector3d matrixToEulerYZX(Matrix3d m){
		Vector3d euler = new Vector3d();
		euler.x = atan2(-m.m12, m.m11);
		euler.y = atan2(-m.m20, m.m00);
		euler.z = asin(m.m10);
		return euler;
	}
	public static Matrix3d eulerToMatrixYZX(Vector3d euler){
		euler = new Vector3d(euler);
		Matrix3d mx = new Matrix3d();
		mx.rotX(euler.x);
		Matrix3d my = new Matrix3d();
		my.rotY(euler.y);
		Matrix3d mz = new Matrix3d();
		mz.rotZ(euler.z);
		
		Matrix3d m = new Matrix3d();
		m.set(my);
		m.mul(mz);
		m.mul(mx);
		return m;
	}
	
	public static Vector3d matrixToEulerZXY(Matrix3d m){
		Vector3d euler = new Vector3d();
		euler.x = asin(m.m21);
		euler.y = atan2(-m.m20, m.m22);
		euler.z = atan2(-m.m01, m.m11);
		euler.scale(180/Math.PI);
		return euler;
	}
	
	public static Matrix3d eulerToMatrixZXY(Vector3d euler){
		euler = new Vector3d(euler);
		euler.scale(Math.PI/180);
		Matrix3d mx = new Matrix3d();
		mx.rotX(euler.x);
		Matrix3d my = new Matrix3d();
		my.rotY(euler.y);
		Matrix3d mz = new Matrix3d();
		mz.rotZ(euler.z);
		
		Matrix3d m = new Matrix3d();
		m.set(mz);
		m.mul(mx);
		m.mul(my);
		return m;
	}
	
	public static Vector3d matrixToEulerXYZ(Matrix3d m){
		Vector3d euler = new Vector3d();
		
		euler.x = atan2(-m.m12, m.m22);
		euler.y = asin(m.m02);
		euler.z = atan2(-m.m01, m.m00);
		
//		euler.x = asin(m.m21);
//		euler.y = atan2(-m.m20, m.m22);
//		euler.z = atan2(-m.m01, m.m11);
		euler.scale(180/Math.PI);
		return euler;
	}
	
	public static Matrix3d eulerToMatrixXYZ(Vector3d euler){
		euler = new Vector3d(euler);
		euler.scale(Math.PI/180);
		Matrix3d mx = new Matrix3d();
		mx.rotX(euler.x);
		Matrix3d my = new Matrix3d();
		my.rotY(euler.y);
		Matrix3d mz = new Matrix3d();
		mz.rotZ(euler.z);
		
		Matrix3d m = new Matrix3d();
		m.set(mx);
		m.mul(my);
		m.mul(mz);
		return m;
	}
	
	static String[][] SingleTokens = { 
		{"Chest2", "Spine1" }, 
		{"Chest3", "Spine2" },
		{"Chest", "Spine" } 
	};

	static String[][] DoubleTokens = { 
			{"Hip", "UpLeg"},
			{"Knee", "Leg"},
			{"Ankle", "Foot"},
			{"Shoulder", "Arm"},
			{"Collar", "Shoulder"},
			{"Elbow", "ForeArm"},
			{"Wrist", "Hand"},
	};

	static String[] JointList = {
		"RightHip",
		"RightKnee",
		"RightAnkle",
		"RightToe",
		"RightToeEnd",
		"LeftHip",
		"LeftKnee",
		"LeftAnkle",
		"LeftToe",
		"LeftToeEnd",
		"Chest",
		"Chest2",
		"Chest3",
		"LeftCollar",
		"LeftShoulder",
		"LeftElbow",
		"LeftWrist",
		"LeftFinger0",
		"RightCollar",
		"RightShoulder",
		"RightElbow",
		"RightWrist",
		"RightFinger0",
		"Neck",
		"Head",
		"HeadTip",
	};
	
	static void validate(File file) throws IOException{
		File tempFile = new File(file.getAbsoluteFile().getParent() + "\\" + "_" + file.getName());
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
		System.out.println("file : " + file.getName());
		String line;
		int jointIndex = 0;
		boolean isStarted = false;
		while ((line = br.readLine()) != null) {
			if (line.trim().length() == 0) continue;
			if (line.trim().startsWith("JOINT ")){
				line = line.substring(0, line.indexOf("J")) + "JOINT " + JointList[jointIndex];
				jointIndex++;
			}
			if (line.trim().startsWith("ROOT ")){
				line = line.substring(0, line.indexOf("R")) + "ROOT Hips";
			}
			for (String[] tokens : SingleTokens) {
				line = line.replace(tokens[0], tokens[1]);
			}
			for (String[] tokens : DoubleTokens) {
				line = line.replace("Left" + tokens[0], "Left" + tokens[1]);
				line = line.replace("Right" + tokens[0], "Right" + tokens[1]);
			}
			if (isStarted){
				String[] tokens = line.split(" ");
				double[] values = new double[tokens.length];
				for (int i = 0; i < values.length; i++) {
					values[i] = Double.parseDouble(tokens[i]);
				}
				{
					Matrix4d m = new Matrix4d();
					m.setTranslation(new Vector3d(values[0], values[1], values[2]));
					Matrix4d m2 = new Matrix4d();
					m2.rotX(-Math.PI/2);
					m2.mul(m);
					m = m2;
//					m.mul(m2);
					
					Vector3d t = new Vector3d(m.m03, m.m13, m.m23);
					values[0] = t.x;
					values[1] = t.y;
					values[2] = t.z;
				}
//				double temp = values[1];
//				values[1] = -values[2];
//				values[2] = temp;
				Matrix3d m = eulerToMatrixZXY(new Vector3d(values[4], values[5], values[3]));
				Matrix3d m2 = new Matrix3d();
				m2.rotX(-Math.PI/2);
				m2.mul(m);
				m = m2;
				Vector3d e = matrixToEulerZXY(m);
				values[3] = e.z;
				values[4] = e.x;
				values[5] = e.y;
				
				
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < values.length; i++) {
					if (i != 0) sb.append(" ");
					sb.append(String.format("%.5f", values[i]));
				}
				line = sb.toString();
			}
			
			bw.write(line + "\r\n");
			
			if (line.startsWith("Frame Time:")) isStarted = true;
		}
		bw.close();
		br.close();
		
		file.delete();
		tempFile.renameTo(file);
	}
	static void validate2(File file) throws IOException{
		File tempFile = new File(file.getAbsoluteFile().getParent() + "\\" + "_" + file.getName());
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
		System.out.println("file : " + file.getName());
		String line;
		while ((line = br.readLine()) != null) {
			if (line.trim().length() == 0) continue;
			if (line.trim().startsWith("ROOT ")){
				line = line.substring(0, line.indexOf("R")) + "ROOT Hips";
			}
			for (String[] tokens : SingleTokens) {
				line = line.replace(tokens[0], tokens[1]);
			}
			for (String[] tokens : DoubleTokens) {
				line = line.replace("Left" + tokens[0], "Left" + tokens[1]);
				line = line.replace("Right" + tokens[0], "Right" + tokens[1]);
			}
			
			bw.write(line + "\r\n");
		}
		bw.close();
		br.close();
		
		file.delete();
		tempFile.renameTo(file);
	}
	
	public static void main(String[] args) {
//		{
//			Vector3d v = new Vector3d(24,-10,50);
//			Matrix3d m = eulerToMatrixZXY(v);
//			Vector3d e = matrixToEulerZXY(m);
//			System.out.println(v);
//			System.out.println(e);
//			System.exit(0);
//		}
		
		
		try{
//			validate(new File("a_001_3_1.bvh"));
//			System.exit(0);
//			validate(new File("SDH.bvh"));
//			validate(new File("YDS.bvh"));
////			validate(new File("t_001_1_1.bvh"));
////			validate(new File("t_001_1_2.bvh"));
////			validate(new File("t_001_1_3.bvh"));
			{
				File file = new File("D:\\data\\basketMotion\\s_010_1_1.bvh");
				validate(file);
//				validate2(file);
				BVHScale.scale(file, 100);
				FPSReducer.fpsReduce(file);
				System.exit(0);
			}
			File folder = new File("D:\\data\\basketMotion\\bvh_20151107");
//			File folder = new File("C:\\Users\\khlee\\Downloads\\motionData\\bvh_stuntman");
			for (File file : folder.listFiles()){
				if (file.isDirectory()) continue;
				if (file.getName().startsWith("_")) continue;
				if (!file.getName().endsWith("bvh")) continue;
//				validate(file);
				validate(file);
				BVHScale.scale(file, 100);
				FPSReducer.fpsReduce(file);
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}
}
