package mrl.motion.data.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.DOF;
import mrl.motion.data.SkeletonData.Joint;

public class AMCParser {
	
	public static boolean hold_root = false;
	

	public AMCParser(){
	}
	
	@SuppressWarnings("resource")
	public MotionData parse(SkeletonData skeletonData, File file){
		try {
			ArrayList<Motion> motionList = new ArrayList<Motion>();
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line;
			Motion motion = null;
			while ((line = br.readLine()) != null) {
				line = line.trim();				
				if (line.startsWith(":") || line.startsWith("#")) continue;
				while (line.contains("  ")){
					line = line.replace("  ", " ");
				}
				try{
					Integer.parseInt(line);
					motion = new Motion(true);
					motionList.add(motion);
				} catch (NumberFormatException e){
					String[] tokens = line.split(" ");
					double[] values = new double[tokens.length - 1]; 
					for (int i = 0; i < values.length; i++) {
						if (tokens[i + 1].endsWith(".#IND")) tokens[i + 1] = tokens[i + 1].replace(".#IND", "");
						values[i] = Double.parseDouble(tokens[i + 1]);
					}
					
					Joint joint = skeletonData.get(tokens[0]);
					if (joint == null){
						System.out.println("njj : " + tokens[0]);
						System.out.println("[pp : " + Arrays.toString(skeletonData.keySet().toArray(new String[0])));
					}
					DOF[] dof = joint.dof;
					if (dof.length != values.length) throw new RuntimeException();
					
					
					Vector3d translation = new Vector3d();
					Matrix4d matrix = new Matrix4d();
					matrix.setIdentity();
					for (int i = 0; i < dof.length; i++) {
						Matrix4d m = new Matrix4d();
						m.setIdentity();
						switch (dof[i]) {
							case RX: m.rotX(Math.toRadians(values[i])); break;
							case RY: m.rotY(Math.toRadians(values[i])); break;
							case RZ: m.rotZ(Math.toRadians(values[i])); break;
							case TX: translation.x = values[i]; break;
							case TY: translation.y = values[i]; break;
							case TZ: translation.z = values[i]; break;
						}
						switch (dof[i]) {
							case RX: case RY: case RZ:
								m.mul(matrix);
								matrix.set(m);
								break;
							default:
						}
					}
					matrix.setTranslation(translation);
					
					Matrix4d axisM = joint.getAxisMatrix();
					Matrix4d axisMInv = joint.getAxisMatrixInverse();
					
					Matrix4d finalMatrix = new Matrix4d(axisM);
					finalMatrix.mul(matrix);
					finalMatrix.mul(axisMInv);
					
					motion.put(tokens[0], finalMatrix);
				}
			}
			br.close();
			return new MotionData(skeletonData, motionList);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Matrix3d eulerMatrix(double rx, double ry, double rz){
		Matrix3d mx = new Matrix3d();
		mx.rotX(rx);
		Matrix3d my = new Matrix3d();
		my.rotY(ry);
		Matrix3d mz = new Matrix3d();
		mz.rotZ(rz);
		mz.mul(my);
		mz.mul(mx);
		return mz;
	}
	
	
	public static Vector3d Matrix2EulerAngle(Matrix3d  m) {
		double ry = Math.asin(-m.m20);
		double rx = Math.atan2(m.m21, m.m22);
		double rz = Math.atan2(m.m10, m.m00);
		return new Vector3d(rx, ry, rz);
	}
	
}
