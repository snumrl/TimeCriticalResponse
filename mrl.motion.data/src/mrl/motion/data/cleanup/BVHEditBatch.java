package mrl.motion.data.cleanup;

import java.io.File;
import java.io.FilenameFilter;

public class BVHEditBatch {

	public static void main(String[] args) {
//		bvhedit.exe xxx.VSK yyy.V --save zzz.BVH
		File dataFolder = new File("C:\\Users\\khlee\\Downloads\\motionData");
		File[] files = dataFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".v");
			}
		});
		for (File file : files){
			String name = file.getName();
			int ii = name.lastIndexOf("_");
			String character = name.substring(ii + 1, name.length()-".V".length());
			String targetName;
			String[] tokens = name.split("_");
			
			int idx1 = name.indexOf("_take") + "_take".length();
			int takeIndex = Integer.parseInt(name.substring(idx1, idx1 + 1));
//			System.out.println(name + "\t" + name.substring(idx1 + 5, idx1 + 7) + "\t" + takeIndex);
			int cIndex = -1;
			int scene;
			String prefix;
			int i1 = name.indexOf("_");
			scene = Integer.parseInt(name.substring("scene".length(), i1));
			if (name.substring(idx1 + 5, idx1 + 7).equals(".V")){
				prefix = "s";
				for (int i = 1; i < tokens.length-2; i++) {
					if (tokens[i].equals(character)){
						cIndex = i;
						break;
					}
				}
				
				
				
				
			} else {
				prefix = "a";
				String cList = tokens[2];
				for (int i = 0; i < cList.length(); i++) {
					if (cList.charAt(i) == character.charAt(0)){
						cIndex = i + 1;
						break;
					}
				}
				
				
			}
			if (cIndex < 0) {
				System.out.println("invalide : " + name);
				throw new RuntimeException();
			}
			targetName = String.format("%s_%03d_%d_%d.bvh", prefix, scene, takeIndex, cIndex);
			System.out.println(String.format("bvhedit.exe %s.vsk %s --save %s", 
					character, name, targetName));
//			
		}
	}
}
