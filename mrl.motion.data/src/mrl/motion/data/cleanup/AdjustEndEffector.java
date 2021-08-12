package mrl.motion.data.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class AdjustEndEffector {

	private static String ZeroOffset = "OFFSET 0 0 0";
	
	private static String[][] zeroOffsetMap = {
			{ "LeftHand", "8 0 0" },
			{ "RightHand", "-8 0 0" },
			{ "Head", "5.28361 0.364157 0" },
	};
	public static void apply(String folder){
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
//					for (String[] map : mapping){
//						line = line.replace(map[0], map[1]);
//					}
					if (line.contains("JOINT ")) {
						lastJoint = line.replace("JOINT ", "").trim();
					}
					if (line.contains(ZeroOffset)) {
						String zOffset = getMatching(zeroOffsetMap, lastJoint);
						if (zOffset != null) {
							line = line.replace(ZeroOffset, "OFFSET " + zOffset);
						}
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
	
	private static String getMatching(String[][] findMap, String joint) {
		for (String[] map : findMap) {
			if (map[0].equals(joint)) {
				return map[1];
			}
		}
		return null;
	}
	
	public static void main(String[] args) {
		apply("C:\\data\\mocap_data\\man_ue\\split1");
//		apply("C:\\data\\mocap_data\\man_ue\\test");
	}
}
