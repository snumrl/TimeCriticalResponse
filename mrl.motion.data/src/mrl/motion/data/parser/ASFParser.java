package mrl.motion.data.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import javax.vecmath.Vector3d;

import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.DOF;
import mrl.motion.data.SkeletonData.Joint;

public class ASFParser {

	private BufferedReader br;
	private String lastLine;
	private boolean unread = false;
	
	private HashMap<String, Joint> jointMap = new HashMap<String, Joint>();

	public ASFParser() {
	}

	private String readLine() {
		if (unread) {
			unread = false;
			return lastLine;
		}
		try {
			lastLine = br.readLine();
			if (lastLine != null) lastLine = lastLine.trim();
			return lastLine;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Joint readRoot(){
		String line;
		Joint root = new Joint("root");
		while ((line = readLine()) != null){
			if (line.startsWith("order ")){
				root.dof = readDOF(line);
			} else if (line.startsWith("position")){
				root.transition = readVector(line);
			} else if (line.startsWith("orientation")){
				root.axis = readVector(line);
			}
			if (line.startsWith(":bonedata")){
				unread = true;
				break;
			}
		}
		return root;
	}
	
	private Joint readSkeletonData(){
		String line = readLine();
		if (!line.startsWith("begin")){
			unread = true;
			return null;
		}
		Joint joint = new Joint("");
		Vector3d direction = null;
		double length = Double.NaN;
		while ((line = readLine()) != null){
			if (line.startsWith("end")) break;
			if (line.startsWith("name")){
				joint.name = line.split(" ")[1];
			} else if (line.startsWith("direction")){
				direction = readVector(line);
			} else if (line.startsWith("length")){
				length = Double.parseDouble(line.split(" ")[1]);
			} else if (line.startsWith("axis")){
				joint.axis = readVector(line);
			}else if (line.startsWith("dof")){
				joint.dof = readDOF(line);
			}
		}
		if (direction == null || Double.isNaN(length)) throw new RuntimeException(joint.name);
		direction.scale(length);
		joint.transition = direction;
		joint.length = length;
		return joint;
	}
	
	private DOF[] readDOF(String line){
		while (line.contains("  ")){
			line = line.replace("  ", " ");
		}
		String[] tokens = line.split(" ");
		DOF[] dof = new DOF[tokens.length - 1];
		for (int i = 0; i < dof.length; i++) {
			try{
				dof[i] = DOF.valueOf(tokens[i+1].toUpperCase().trim());
			} catch (RuntimeException e){
				System.out.println("ttt : '" + tokens[i+1] + "'");
				throw e;
			}
		}
		return dof;
	}
	
	private Vector3d readVector(String line){
		String[] tokens = line.split(" ");
		Vector3d vector = new Vector3d();
		vector.x = Double.parseDouble(tokens[1]);
		vector.y = Double.parseDouble(tokens[2]);
		vector.z = Double.parseDouble(tokens[3]);
		return vector;
	}

	public SkeletonData parse(File file) {
		try {
			jointMap.clear();
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			Joint root = null;
			while (true) {
				String line = readLine();
				if (line == null) break;
				if (line.startsWith((":root"))) {
					root = readRoot();
					jointMap.put(root.name, root);
				}
				if (line.startsWith((":bonedata"))) {
					while (true){
						Joint joint = readSkeletonData();
						if (joint == null) break;
						jointMap.put(joint.name, joint);
					}
				}
				if (line.startsWith((":hierarchy"))) {
					if (!readLine().equals("begin")){
						throw new RuntimeException();
					}
					while ((line = readLine()) != null){
						if (line.startsWith("end")) break;
						String[] tokens = line.split(" ");
						Joint parent = jointMap.get(tokens[0]);
						for (int i = 1; i < tokens.length; i++) {
							Joint child = jointMap.get(tokens[i]);
							if (child == null){
								System.out.println("non child : " + tokens[i] + " : /// " + tokens[0]);
								for (String jj : jointMap.keySet()){
									System.out.println(jj);
								}
							}
							
							parent.addChild(child);
						}
					}
				}
			}

			br.close();
			return new SkeletonData(root);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
