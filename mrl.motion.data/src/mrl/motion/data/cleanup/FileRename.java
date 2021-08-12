package mrl.motion.data.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import mrl.util.FileUtil;
import mrl.util.Utils;

public class FileRename {
	
	
	public static void applyPostfix(){
		String[] postfix = {
				"jump",
				"jump",
				"high jump",
				"high jump",
				"forward jump",
				"forward jump",
				"forward jump",
				"run/jog, sudden stop",
				"forward jump",
				"forward jump",
				"walk, veer left",
				"walk, veer left",
				"walk, veer right",
				"walk, veer right",
				"walk",
				"walk",
				"walk, 90-degree left turn",
				"walk, 90-degree left turn",
				"walk, 90-degree right turn",
				"walk, 90-degree right turn",
				"walk",
				"walk",
				"walk, veer left",
				"walk, veer left",
				"walk, veer right",
				"walk, veer right",
				"walk, 90-degree left turn",
				"walk, 90-degree left turn",
				"walk, 90-degree right turn",
				"walk, 90-degree right turn",
				"walk",
				"walk",
				"slow walk, stop",
				"slow walk, stop",
				"run/jog",
				"run/jog",
				"run/jog, veer left",
				"run/jog, veer left",
				"run/jog, veer right",
				"run/jog, veer right",
				"run/jog, 90-degree left turn",
				"run/jog, 90-degree left turn",
				"run/jog, 90-degree right turn",
				"run/jog, 90-degree right turn",
				"run/jog",
				"run/jog",
				"walk",
				"run, veer left",
				"run, veer right",
				"run, veer right",
				"run, 90-degree left turn",
				"run, 90-degree left turn",
				"run, 90-degree right turn",
				"run, 90-degree right turn",
				"run",
				"run/jog",
				"run/jog, sudden stop",
				"walk",
		};
		
		File folder = new File("D:\\data\\AniCourse\\cmu");
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			String name = f.getName();
			name = name.replace(".bvh", "");
			String post = postfix[i];
			post = post.replace("\t", "");
			post = post.replace("/", "&");
//			post = post.replace(",", " ");
			System.out.println(i + " : " + post);
			f.renameTo(new File(folder.getAbsolutePath() + "\\" + name + "_" + post + ".bvh"));
		}
	}
	
	public static void collectBVH(File dataFolder, String targetFolder) {
		new File(targetFolder).mkdirs();
		for (File f : dataFolder.listFiles()) {
			if (f.isDirectory()) {
				collectBVH(f, targetFolder);
				continue;
			} 
			String name = f.getName();
			if (!name.toLowerCase().endsWith(".bvh")) continue;
			System.out.println("copy : " + name);
			File dest = new File(targetFolder + "\\" + name);
			Utils.copyFile(f, dest);
		}
	}

	
	public static void main(String[] args) {
		collectBVH(new File("C:\\data\\모션캡처\\모션캡처\\사람\\남자"), "C:\\data\\모션캡처\\남자_All");
		System.exit(0);
		
		
		File folder = new File("D:\\data\\basketMotion\\c3d");
		for (File f : folder.listFiles()){
			String name = f.getName();
			name = name.replace("Session ", "session");
			f.renameTo(new File(folder.getAbsolutePath() + "\\" + name));
		}
//		File folder = new File("D:\\data\\basketMotion\\c3d");
//		for (File subFolder : folder.listFiles()){
//			if (!subFolder.isDirectory()) continue;
//			for (File f : subFolder.listFiles()){
//				f.renameTo(new File(folder.getAbsolutePath() + "\\" + subFolder.getName() + "_" + f.getName()));
//			}
//		}
	}
}
