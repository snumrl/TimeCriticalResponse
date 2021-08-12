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

import static mrl.motion.data.cleanup.BVHValidator.*;

public class BVHRotate {

	static String[] JointList = {
		"LeftUpLeg",
		"LeftLeg",
		"LeftFoot",
		"LeftToe",
		"RightUpLeg",
		"RightLeg",
		"RightFoot",
		"RightToe",
		"Spine",
		"Head",
		"LeftShoulder",
		"LeftArm",
		"LeftForeArm",
		"LeftHand",
		"RightShoulder",
		"RightArm",
		"RightForeArm",
		"RightHand"
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
			line = line.trim();
			if (line.trim().length() == 0) continue;
			if (line.trim().startsWith("ROOT ")){
				line = line.substring(0, line.indexOf("R")) + "ROOT Hips";
			}
			if (line.trim().startsWith("JOINT ")){
				line = line.substring(0, line.indexOf("J")) + "JOINT " + JointList[jointIndex];
				jointIndex++;
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
//					m2.rotZ(Math.PI/2);
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
				Matrix3d m = eulerToMatrixXYZ(new Vector3d(values[3], values[4], values[5]));
//				Matrix3d m = eulerToMatrixZXY(new Vector3d(values[4], values[5], values[3]));
				Matrix3d m2 = new Matrix3d();
//				m2.rotZ(Math.PI/2);
				m2.rotX(-Math.PI/2);
				m2.mul(m);
				m = m2;
				Vector3d e = matrixToEulerXYZ(m);
//				Vector3d e = matrixToEulerZXY(m);
				values[3] = e.x;
				values[4] = e.y;
				values[5] = e.z;
//				values[3] = e.z;
//				values[4] = e.x;
//				values[5] = e.y;
				
				
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
	
	public static void main(String[] args) {
		try {
//			{
//				File file = new File("D:\\data\\basketMotion\\s_010_1_1.bvh");
//				FPSReducer.fpsReduce(file);
//				BVHScale.scale(file, 100);
//				validate(file);
//				System.exit(0);
//			}
			
//			File folder = new File("C:\\Users\\khlee\\Downloads\\basketMotion\\test");
//			File folder = new File("D:\\data\\basketMotion\\bvh_20151107");
			File folder = new File("D:\\data\\Tennis\\test");
			for (File file : folder.listFiles()){
//				FPSReducer.fpsReduce(file);
//				BVHScale.scale(file, 100);
				validate(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
