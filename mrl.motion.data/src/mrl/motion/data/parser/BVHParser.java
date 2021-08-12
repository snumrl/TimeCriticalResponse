package mrl.motion.data.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.DOF;
import mrl.motion.data.SkeletonData.Joint;
import mrl.util.MathUtil;

public class BVHParser {
	
	public static final boolean IS_BVH_ROTATE_FIRST = false;
	public static final String END_POSTFIX = "_End";
	public static int MAX_FRAME = -1;
	
	private BufferedReader br;
	private String lastLine;
	private boolean unread = false;
	private ArrayList<Joint> jointList = new ArrayList<Joint>();
	public int frameSize = -1;
	public double frameTime;

	public BVHParser(){
	}
	
	private String readLine() {
		if (unread) {
			unread = false;
			return lastLine;
		}
		try {
			lastLine = br.readLine();
			while (lastLine != null && lastLine.trim().length() == 0){
				lastLine = br.readLine();
			}
			
			if (lastLine != null){
				lastLine = lastLine.replace('\t', ' ');
				lastLine = lastLine.trim();
				while (lastLine.contains("  ")){
					lastLine = lastLine.replace("  ", " ");
				}
			}
			return lastLine;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ArrayList<Joint> getJointList() {
		return jointList;
	}

	private Joint readJoint(){
		String line = readLine();
		String[] tokens = line.split(" ");
		Joint joint = new Joint(tokens[1]);
		jointList.add(joint);
		
		line = readLine();
		if (!line.equals("{")) throw new RuntimeException();
		
		while ((line = readLine()) != null){
			if (line.equals("}")) break;
			
			tokens = line.split(" ");
			if (tokens[0].equals("OFFSET")){
				Vector3d vector = new Vector3d();
				vector.x = Double.parseDouble(tokens[1]);
				vector.y = Double.parseDouble(tokens[2]);
				vector.z = Double.parseDouble(tokens[3]);
				joint.transition = vector;
				joint.length = vector.length();
			} else if (tokens[0].equals("CHANNELS")){
				int len = Integer.parseInt(tokens[1]);
				joint.dof = new DOF[len];
				for (int i = 0; i < len; i++) {
					String token = tokens[i+2];
					String type = token.substring(1).equals("position") ? "T" : "R";
					joint.dof[i] = DOF.valueOf(type + token.substring(0, 1));
				}
			} else if (tokens[0].equals("JOINT")){
				unread = true;
				Joint child = readJoint();
				joint.addChild(child);
			} else if (tokens[0].equals("End")){
				Joint child = new Joint(joint.name + END_POSTFIX);
				readLine();
				line = readLine();
				tokens = line.split(" ");
				Vector3d vector = new Vector3d();
				vector.x = Double.parseDouble(tokens[1]);
				vector.y = Double.parseDouble(tokens[2]);
				vector.z = Double.parseDouble(tokens[3]);
				child.transition = vector;
				child.length = vector.length();
				joint.addChild(child);
				readLine();
			}
		}
		return joint;	
	}
	
	private Motion readMotion(SkeletonData skeletonData){
		String line = readLine();
		Motion motion = new Motion(IS_BVH_ROTATE_FIRST);
		String[] tokens = line.split(" ");
		
		int idx = 0;
		for (Joint joint : jointList){
			DOF[] dof = joint.dof;
			Vector3d translation = new Vector3d();
			Matrix4d matrix = new Matrix4d();
			matrix.setIdentity();
			
			for (int i = 0; i < dof.length; i++) {
				Matrix4d m = new Matrix4d();
				m.setIdentity();
				double value = Double.parseDouble(tokens[idx]);
				
				
				idx++;
				if (Double.isNaN(value)) continue;
				switch (dof[i]) {
					case RX: m.rotX(Math.toRadians(value)); break;
					case RY: m.rotY(Math.toRadians(value)); break;
					case RZ: m.rotZ(Math.toRadians(value)); break;
					case TX: translation.x = value; break;
					case TY: translation.y = value; break;
					case TZ: translation.z = value; break;
				}
				switch (dof[i]) {
					case RX: case RY: case RZ:
						matrix.mul(m);
						break;
					default:
				}
			}
			matrix.setTranslation(translation);
			motion.put(joint.name, matrix);
		}
		if (idx != tokens.length){
			System.out.println("invv :: " + idx + " : " + tokens.length);
		}
		return motion;
	}
	
	public int jointIndex(String name){
		for (int i = 0; i < jointList.size(); i++) {
			if (jointList.get(i).name.equals(name)) return i;
		}
		return -1;
	}
	
	public MotionData parse(String file) {
		return parse(new File(file));
	}
	
	public SkeletonData parseSkeleton(File file){
		return parse(file, true).skeletonData;
	}
	
	public MotionData parse(File file){
		return parse(file, false);
	}
	private MotionData parse(File file, boolean skeletonOnly){
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			
			SkeletonData skeletonData = null;
			ArrayList<Motion> motionList = new ArrayList<Motion>();
			frameTime = 1/120d;
			while (true) {
				String line = readLine();
				if (line == null) break;
				if (line.startsWith(("HIERARCHY"))) {
					skeletonData = new SkeletonData(readJoint());
				} else if (line.startsWith(("MOTION"))) {
					line = readLine();
					line = line.substring("Frames:".length()).trim();
					frameSize = Integer.parseInt(line);
					line = readLine();
					line = line.substring("Frame Time:".length()).trim();
					frameTime = Double.parseDouble(line);
					if (skeletonOnly) break;
					if (MAX_FRAME >= 0 && frameSize > MAX_FRAME){
						frameSize = MAX_FRAME;
					}
					for (int i = 0; i < frameSize; i++) {
						motionList.add(readMotion(skeletonData));
					}
					if (frameSize == MAX_FRAME) break;
				} else {
					System.out.println("bvh parse ??? : " + file.getName() + " : " + line);
				}
			}
			skeletonData.jointListByFileOrder = jointList;

			br.close();
			MotionData motionData = new MotionData(skeletonData, motionList);
			motionData.file = file;
			motionData.framerate = MathUtil.round(1/frameTime);
			for (int i = 0; i < motionList.size(); i++) {
				Motion m1 = motionList.get(i);
				m1.frameIndex = i;
				m1.motionData = motionData;
				if (i < motionList.size() - 1) {
					Motion m2 = motionList.get(i + 1);
					m1.next = m2;
					m2.prev = m1;
				}
			}
			return motionData;
		} catch (Exception e) {
			System.out.println("bvh parser error in : " + file.getName());
//			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
}
