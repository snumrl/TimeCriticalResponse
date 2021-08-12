package mrl.motion.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import mrl.motion.data.parser.BVHParser;

public class MultiCharacterFolder {
	
	public static boolean DEFAULT_MERGE = true;
	public File folder;
	public ArrayList<MultiCharacterFiles> list;

	public MultiCharacterFolder(File folder, ArrayList<MultiCharacterFiles> list) {
		this.folder = folder;
		this.list = list;
	}
	
	public MultiCharacterFiles getFiles(String filename){
		String prefix = getPrefix(filename);
		for (MultiCharacterFiles files : list){
			if (files.label.startsWith(prefix)){
				return files;
			}
		}
		return null;
	}

	public static class MultiCharacterFiles {
		public String label;
		public ArrayList<File> fileList = new ArrayList<File>();
		
		public MotionData[] loadBVH(){
			MotionData[] dataList = new MotionData[fileList.size()];
			for (int i = 0; i < dataList.length; i++) {
				dataList[i] = new BVHParser().parse(fileList.get(i));
			}
			return dataList;
		}
	}
	
	public static ArrayList<MultiCharacterFolder> loadChildFolders(File root){
		return loadChildFolders(root, DEFAULT_MERGE);
	}
	public static ArrayList<MultiCharacterFolder> loadChildFolders(File root, boolean doMerge){
		ArrayList<MultiCharacterFolder> folderList = new ArrayList<MultiCharacterFolder>();
		MultiCharacterFolder folder = MultiCharacterFolder.loadFolder(root, doMerge);
		if (folder != null) folderList.add(folder);
		for (File f : root.listFiles()){
			if (f.isDirectory()){
				folder = MultiCharacterFolder.loadFolder(f, doMerge);
				if (folder != null) folderList.add(folder);
			}
		}
		return folderList;
	}
	
	public static MultiCharacterFolder loadFolder(File folder){
		return loadFolder(folder, DEFAULT_MERGE);
	}
	public static MultiCharacterFolder loadFolder(File folder, boolean doMerge){
		File[] files = folder.listFiles();
		if (files == null){
			System.out.println("null : " + folder.getAbsolutePath());
			return null;
		}
		
		HashMap<String, MultiCharacterFiles> fileMap = new HashMap<String, MultiCharacterFolder.MultiCharacterFiles>();
		ArrayList<MultiCharacterFiles> fileList = new ArrayList<MultiCharacterFiles>();
		
		for (File file : files) {
			String name = file.getName();
			if (name.startsWith("_")) continue;
			if (file.isDirectory()) continue;
			if (!name.toLowerCase().endsWith(".bvh")) continue;
			
			String prefix = doMerge ? getPrefix(name) : name; 
			
			MultiCharacterFiles mFile = fileMap.get(prefix);
			if (mFile == null){
				mFile = new MultiCharacterFiles();
				mFile.label = prefix;
				fileList.add(mFile);
				fileMap.put(prefix, mFile);
			}
			mFile.fileList.add(file);
		}
		
		if (doMerge){
			for (MultiCharacterFiles file : fileList) {
				file.label += "(" + file.fileList.size() + ")";
			}
		}
		
		
		if (fileList.size() > 0){
			return new MultiCharacterFolder(folder, fileList);
		} else {
			return null;
		}
	}
	
	public static String getPrefix(String name){
		String prefix = name;
		if (name.contains("_")){
			int idx1 = name.lastIndexOf("_") + 1;
			int idx2 = name.toLowerCase().indexOf(".bvh", idx1);
			if (idx1 + 1 == idx2 || idx1 + 2 == idx2){
				try {
					Integer.parseInt(name.substring(idx1, idx2));
					prefix = name.substring(0, idx1 - 1);
				} catch (NumberFormatException e) {
				}
			}
		}
		return prefix;
	}
	
	public static String getTakeName(String name){
		int idx1 = name.lastIndexOf("_") + 1;
		int idx2 = name.toLowerCase().indexOf(".bvh", idx1);
		if (idx1 > 0 && idx1 + 1 == idx2){
			try {
				Integer.parseInt(name.substring(idx1, idx2));
				return name.substring(0, idx1 - 1);
			} catch (NumberFormatException e) {
			}
		}
		return name.substring(0, idx2);
	}
	
	public static int getPersonIndex(String name){
		int idx1 = name.indexOf(".bvh");
		int idx2 = name.lastIndexOf("_");
		if (idx1 < 0 || idx2 < 0) throw new RuntimeException(name);
		if (idx1 != idx2 + 2) throw new RuntimeException(name);
		return Integer.parseInt(name.substring(idx2 + 1, idx1));
	}
	
}