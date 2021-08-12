package mrl.motion.data.cleanup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionAnnotationManager;
import mrl.util.Utils;

public class CollectAnnotatedFiles {

	public static void collect(String dataFolder, String outputFolder) {
		HashSet<String> fileSet = new HashSet<String>();
		fileSet.addAll(collectFiles(dataFolder + "\\annotation"));
		fileSet.addAll(collectFiles(dataFolder + "\\transition"));
		
		new File(outputFolder).mkdirs();
		Utils.copyFolder(new File(dataFolder + "\\annotation"), new File(outputFolder + "\\annotation"));
		Utils.copyFolder(new File(dataFolder + "\\transition"), new File(outputFolder + "\\transition"));
		
		new File(outputFolder + "\\motion").mkdirs();
		for (String file : fileSet) {
			if (new File(dataFolder + "\\motion\\" + file).exists() == false) continue;
			Utils.copyFile(dataFolder + "\\motion\\" + file, outputFolder + "\\motion\\" + file);
		}
	}
	
	
	static HashSet<String> collectFiles(String annFolder){
		HashSet<String> fileSet = new HashSet<String>();
		File folder = new File(annFolder);
		for (File file : folder.listFiles()){
			if (file.isDirectory() || !file.getName().endsWith(".lab")) continue;
			ArrayList<MotionAnnotation> list = MotionAnnotation.load(file);
			if (list.size() == 0) continue;
			String[] fNames = MotionAnnotationManager.getFileNames(list.get(0).file, 5);
			for (String f : fNames) {
				fileSet.add(f);
			}
			for (MotionAnnotation ann : list) {
				fileSet.add(ann.file);
			}
		}
		return fileSet;
	}
	
	public static void collectMultiLabel(String outputFolder, String dataFolder, String[] labels) {
		Utils.deleteFile(new File(outputFolder));
		new File(outputFolder).mkdirs();
		new File(outputFolder + "\\motion").mkdirs();
		for (String label : labels) {
			String labelFolder = dataFolder + "\\labels\\" + label;
			HashSet<String> fileSet = new HashSet<String>();
			fileSet.addAll(collectFiles(labelFolder + "\\annotation"));
			fileSet.addAll(collectFiles(labelFolder + "\\transition"));
			
			Utils.copyFolder(new File(labelFolder + "\\annotation"), new File(outputFolder + "\\annotation"));
			Utils.copyFolder(new File(labelFolder + "\\transition"), new File(outputFolder + "\\transition"));
			
			for (String file : fileSet) {
				System.out.println("file :: " + label + " : " + file);
				if (new File(dataFolder + "\\motion\\" + file).exists() == false) continue;
				Utils.copyFile(dataFolder + "\\motion\\" + file, outputFolder + "\\motion\\" + file);
			}
		}
	}
	
}

