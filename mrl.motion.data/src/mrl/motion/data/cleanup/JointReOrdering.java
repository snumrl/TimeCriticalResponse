package mrl.motion.data.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;

import mrl.motion.data.SkeletonData.Joint;
import mrl.motion.data.parser.BVHParser;
import mrl.util.Utils;

public class JointReOrdering {


	static String[] origin = {
			"SHE_Hips",
			"SHE_Spine",
			"SHE_Spine1",
			"SHE_Spine2",
			"SHE_Neck",
			"SHE_Head",
			"SHE_LeftShoulder",
			"SHE_LeftArm",
			"SHE_LeftForeArm",
			"SHE_LeftHand",
			"SHE_RightShoulder",
			"SHE_RightArm",
			"SHE_RightForeArm",
			"SHE_RightHand",
			"SHE_LeftUpLeg",
			"SHE_LeftLeg",
			"SHE_LeftFoot",
			"SHE_LeftFootMiddle1",
			"SHE_RightUpLeg",
			"SHE_RightLeg",
			"SHE_RightFoot",
			"SHE_RightFootMiddle1",
		};
	
	static String[] toChange = {
			"SHE_Hips",
			
			"SHE_RightUpLeg",
			"SHE_RightLeg",
			"SHE_RightFoot",
			"SHE_RightFootMiddle1",
			
			"SHE_LeftUpLeg",
			"SHE_LeftLeg",
			"SHE_LeftFoot",
			"SHE_LeftFootMiddle1",
			
			"SHE_Spine",
			"SHE_Spine1",
			"SHE_Spine2",
			
			"SHE_RightShoulder",
			"SHE_RightArm",
			"SHE_RightForeArm",
			"SHE_RightHand",
			
			"SHE_LeftShoulder",
			"SHE_LeftArm",
			"SHE_LeftForeArm",
			"SHE_LeftHand",
			
			"SHE_Neck",
			"SHE_Head",
	};
	
	private static int[] getMapping(){
		int[] mapping = new int[origin.length];
		for (int i = 0; i < mapping.length; i++) {
			mapping[i] = Utils.indexOf(origin, toChange[i]);
//			mapping[i] = Utils.indexOf(toChange, origin[i]);
		}
		System.out.println(Arrays.toString(mapping));
		return mapping;
	}
	
	public static BufferedWriter writeHeader(File newFile, File baseFile) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(baseFile)));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile)));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("Frames:")) break;
			bw.write(line + "\r\n");
		}
		br.close();
		return bw;
	}
	
	
	public static void change(File file, File baseFile) throws IOException{
		System.out.println("file : " + file.getName());
		File tempFile = new File(file.getAbsoluteFile().getParent() + "\\" + "_" + file.getName());
		BufferedWriter bw = writeHeader(tempFile, baseFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line;
		while ((line = br.readLine()) != null) {
			line = line.replace('\t', ' ').trim();
			if (line.startsWith("Frames:")) break;
		}
		// frames
		bw.write(line + "\r\n");
		
		// frame time
		line = br.readLine();
		bw.write(line + "\r\n");
		
		int[] mapping = getMapping();
		while ((line = br.readLine()) != null) {
			line = line.replace('\t', ' ').trim();
			if (line.trim().length() == 0) continue;
			
			String[] tokens = line.split(" ");
			String[] mapped = new String[tokens.length];
			System.arraycopy(tokens, 0, mapped, 0, 3);
			for (int i = 0; i < mapping.length; i++) {
				System.arraycopy(tokens, mapping[i]*3+3, mapped, i*3 + 3, 3);
			}
			line = Utils.toString((Object[])mapped).replace("\t", " ");
			bw.write(line + "\r\n");
		}
		bw.close();
		br.close();
		
		file.delete();
		tempFile.renameTo(file);

	}
	
	
	static void changeAll(String baseFile, String targetFolder) {
		try{
			File folder = new File("C:\\data\\FightMotion_fei\\converted");
			for (File file : new File(targetFolder).listFiles()){
				if (file.isDirectory()) continue;
				if (file.getName().startsWith("_")) continue;
				if (!file.getName().endsWith("bvh")) continue;
//				scale(file, 100);
//				scale(file, 6);
				
//				File rename = new File(file.getAbsolutePath().replace(".bvh", "") + "_1.bvh");
//				file.renameTo(rename);
				change(file, new File(baseFile));
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
}
