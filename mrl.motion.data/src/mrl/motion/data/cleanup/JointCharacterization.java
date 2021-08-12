package mrl.motion.data.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class JointCharacterization {
	
	public static String[][] NC_TO_BASE = {
			{"Hips","Hips"},
			{"Chest2","Spine1"},
			{"Chest3","Spine2"},
			{"Chest4","Spine3"},
			{"Chest","Spine"},
			{"Neck","Neck"},
			{"Head","Head"},
			{"RightShoulder","RightArm"},
			{"RightElbow","RightForeArm"},
			{"RightWrist","RightHand"},
			{"RightFinger1","RightInHandIndex"},
			{"RightFinger0","RightInHandTumb"},
			{"RightCollar","RightShoulder"},
			{"LeftShoulder","LeftArm"},
			{"LeftElbow","LeftForeArm"},
			{"LeftWrist","LeftHand"},
			{"LeftFinger1","LeftInHandIndex"},
			{"LeftFinger0","LeftInHandThumb"},
			{"LeftCollar","LeftShoulder"},
			{"RightHip","RightUpLeg"},
			{"RightKnee","RightLeg"},
			{"RightAnkle","RightFoot"},
			{"RightToe","RightToe"},
			{"LeftHip","LeftUpLeg"},
			{"LeftKnee","LeftLeg"},
			{"LeftAnkle","LeftFoot"},
			{"LeftToe","LeftToe"},
	};

	public static String[][] mapping = {
			{"pelvis", "Hips"},
			{"spine_01", "Spine"},
			{"spine_02", "Spine1"},
			{"spine_03", "Spine2"},
			{"clavicle_r", "RightShoulder"},
			{"upperarm_r", "RightArm"},
			{"lowerarm_r", "RightForeArm"},
			{"hand_r", "RightHand"},
			{"clavicle_l", "LeftShoulder"},
			{"upperarm_l", "LeftArm"},
			{"lowerarm_l", "LeftForeArm"},
			{"hand_l", "LeftHand"},
			{"neck_01", "Neck"},
			{"head", "Head"},
			{"thigh_l", "LeftUpLeg"},
			{"calf_l", "LeftLeg"},
			{"foot_l", "LeftFoot"},
			{"toe_l", "LeftToe"},
			{"thigh_r", "RightUpLeg"},
			{"calf_r", "RightLeg"},
			{"foot_r", "RightFoot"},
			{"toe_r", "RightToe"},
			
			{"ball_l", "LeftToe"},
			{"ball_r", "RightToe"},
			
//			{"SHE_", ""},
//			{"FootMiddle1", "Toe"},
			
//			{"pelvis", "Hips"},
//			{"lfemur", "LeftUpLeg"},
//			{"ltibia", "LeftLeg"},
//			{"lfoot", "LeftFoot"},
//			{"rfemur", "RightUpLeg"},
//			{"rtibia", "RightLeg"},
//			{"rfoot", "RightFoot"},
//			{"thorax", "Spine"},
//			{"lhumerus", "LeftArm"},
//			{"lradius", "LeftForeArm"},
//			{"lhand", "LeftHand"},
//			{"rhumerus", "RightArm"},
//			{"rradius", "RightForeArm"},
//			{"rhand", "RightHand"},
	};
	
	private static boolean MakeEndEffector = false;
	private static String ZeroOffset = "OFFSET 0 0 0";
	private static String[][] offsetMap = {
			{ "LeftHand", "5.9751 0 0" },
			{ "RightHand", "-5.9751 0 0" },
			{ "Head", "5.9751 0 0" },
			{ "LeftToe", "6.4538 1.1779 0.0801549" },
			{ "RightToe", "-6.4538 -1.1779 -0.0801549" },
//			{ "LeftToe", "-6.4538 -3.5779 0.0801549" },
//			{ "RightToe", "6.4538 3.5779 -0.0801549" },
	};
	
	public static void apply(String folder, String[][] mapping){
		try {
			for (File file : new File(folder).listFiles()){
				if (!file.getName().toLowerCase().endsWith(".bvh")) continue;
				
				File tempFile = new File(file.getAbsoluteFile().getParent() + "\\" + "_" + file.getName());
				
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
				System.out.println("file : " + file.getName());
				String line;
				String lastJoint = null;
				while ((line = br.readLine()) != null) {
					for (String[] map : mapping){
						line = line.replace(map[0], map[1]);
					}
					if (line.contains("JOINT ")) {
						lastJoint = line.replace("JOINT ", "").trim();
					}
					if (MakeEndEffector && line.contains(ZeroOffset)) {
						line = line.replace(ZeroOffset, getOffset(lastJoint));
					}
					bw.write(line + "\r\n");
				}
				bw.close();
				br.close();
				
				file.delete();
				tempFile.renameTo(file);
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static String getOffset(String joint) {
		for (String[] map : offsetMap) {
			if (map[0].equals(joint)) {
				return "OFFSET " + map[1];
			}
		}
		throw new RuntimeException("invalid joint : " + joint);
	}
	
}
