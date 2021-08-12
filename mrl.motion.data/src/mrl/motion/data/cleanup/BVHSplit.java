package mrl.motion.data.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import javax.vecmath.Vector3d;

import mrl.motion.data.cleanup.BVHMerge.Person;

public class BVHSplit {
	
	private static String baseFile;
	
	private static class BVHRewriter{
		BufferedWriter bw;
		BufferedReader br;
		int totalFrameIndex = 0;
		int frameIndex = 0;
		boolean isEnded = false;
		
		public BVHRewriter(File sourceFile){
			try{
				br = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile)));
				System.out.println("first line : " + readLine() + " : " + sourceFile.getAbsolutePath());
				String line;
				while ((line = br.readLine()) != null) {
					if (line.contains("Frame Time:")) break;
				}
				br.readLine();
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		
		public void setWriteFile(File file, int size){
			try {
				if (bw != null) bw.close();
				frameIndex = 0;
				isEnded = false;
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
				
				File f = new File(baseFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.contains("Frames:")) break;
					bw.write(line + "\r\n");
				}
				br.close();
				
				bw.write("Frames:	" + size + "\r\n");
				bw.write("Frame Time:	0.0333333\r\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public boolean writeNextFrame(){
			if (isEnded){
				System.out.println("frammm : " + frameIndex);
				throw new RuntimeException();
			}
			try{
				frameIndex++;
				totalFrameIndex++;
				if (bw != null){
					String line = readLine();
					if (line == null){
						System.out.println("bvh split ????? : " + frameIndex + " : " +  totalFrameIndex);
					} else {
						bw.write(line +"\r\n");
					}
				}
			} catch (Exception e){
				e.printStackTrace();
			}
			return true;
		}
		
		public String readLine(){
			try {
				return br.readLine();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		public void close(){
			try {
				bw.close();
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static Person findPerson(int index, HashMap<Vector3d, Person> personMap){
		for (Person person : personMap.values()){
			if (person.index == index) return person;
		}
		return null;
	}
	
	static void splitSinglePerson(String originalFolder, String mergedBVH, String outputFolder) {
		HashMap<Vector3d, Person> personMap = BVHMerge.loadPersonMap(originalFolder, true, -1);
		baseFile = mergedBVH;
		File file = new File(mergedBVH);
		new File(outputFolder).mkdirs();
		Person person = findPerson(1, personMap);
		BVHRewriter writer = new BVHRewriter(file);
		int sizeSum = 0;
		System.out.println("################# split start ###############");
		for (int i = 0; i < person.fileList.size(); i++) {
			File f = person.fileList.get(i);
			int size = person.sizeList.get(i);
			sizeSum += size;
			System.out.println("write :: " + f.getName() + " : " + size);
			writer.setWriteFile(new File(outputFolder + "\\" + f.getName().substring(0, f.getName().length()-3) + "bvh"), size);
			for (int j = 0; j < size; j++) {
				writer.writeNextFrame();
			}
		}
		writer.close();
		System.out.println(file.getName() + " : " + sizeSum + " : " + person.fileList.size() + " : " + person.sizeList.get(0) + " : " + person.fileList.get(0).getName());
	}
	
	static void splitMultiPerson(String originalMotionFolder, String mergedMotionFolder, String outputFolder) {
		HashMap<Vector3d, Person> personMap = BVHMerge.loadPersonMap(originalMotionFolder, false, -1);
		File folder = new File(mergedMotionFolder);
		File[] files = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("bvh") && name.startsWith("m_");
			}
		});
		
		baseFile = files[0].getAbsolutePath();
		for (File file : files){
			if (!file.getName().endsWith("bvh")) continue;
			String name = file.getName();
			int index = Integer.parseInt(name.substring(name.length() - 1 - ".bvh".length(), name.length() - ".bvh".length()));
			Person person = findPerson(index, personMap);
			System.out.println("person :: " + person.motionCounts);
			BVHRewriter writer = new BVHRewriter(file);
			int sizeSum = 0;
			for (int i = 0; i < person.fileList.size(); i++) {
				File f = person.fileList.get(i);
				int size = person.sizeList.get(i);
				sizeSum += size;
				writer.setWriteFile(new File(outputFolder + "\\" + f.getName().substring(0, f.getName().length()-3) + "bvh"), size);
				for (int j = 0; j < size; j++) {
					writer.writeNextFrame();
				}
			}
			writer.close();
			System.out.println(file.getName() + " : " + sizeSum + " : " + person.fileList.size() + " : " + person.sizeList.get(0) + " : " + person.fileList.get(0).getName());
		}
	}
	
	public static void main(String[] args) {
//		{
//			int idx = 4;
//			String originalFolder = "C:\\data\\mocap_data\\man_retargeted_split\\split" + idx;
//			String mergedBVH = "C:\\data\\share\\retargeting data\\m_merged_" + idx + ".bvh";
//			String outputFolder = "C:\\data\\mocap_data\\man_ue\\split" + idx;
//			
//			splitSinglePerson(originalFolder, mergedBVH, outputFolder);
//			AdjustEndEffector.apply(outputFolder);
//			System.exit(0);
//		}
		
		{
			String originalFolder = "C:\\data\\200608_MotionCapture\\retarget";
			String mergedBVHFolder = "C:\\data\\share\\retargeting data\\hit";
			String outputFolder = "C:\\data\\200608_MotionCapture\\retargeted";
			splitMultiPerson(originalFolder, mergedBVHFolder, outputFolder);
			AdjustEndEffector.apply(outputFolder);
			System.exit(0);
		}
		
		
		String originalMotionFolder = "C:\\data\\mocap_data\\man_retargeted_split\\split1";
		HashMap<Vector3d, Person> personMap = BVHMerge.loadPersonMap(originalMotionFolder, true, -1);
//		HashMap<Vector3d, Person> personMap = BVHMerge.loadPersonMap("D:\\data\\basketMotion\\bvh_20151107", false);
//		File folder = new File("D:\\data\\basketMotion");
		File folder = new File("C:\\data\\FightMotion_fei");
		
//		File folder = new File("D:\\data\\Tennis\\MotionBuilderIK\\output");
		File[] files = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("bvh") && name.startsWith("m_");
			}
		});
		String outputFolder = files[0].getParent() + "\\";
		baseFile = "C:\\data\\FightMotion_fei\\m_merged_1.bvh";
//		baseFile = "D:\\data\\basketMotion\\t_pose\\t_pose.bvh";
		
		for (File file : files){
			if (!file.getName().endsWith("bvh")) continue;
			String name = file.getName();
			int index = Integer.parseInt(name.substring(name.length() - 1 - ".bvh".length(), name.length() - ".bvh".length()));
			Person person = findPerson(index, personMap);
			BVHRewriter writer = new BVHRewriter(file);
			int sizeSum = 0;
			for (int i = 0; i < person.fileList.size(); i++) {
				File f = person.fileList.get(i);
				int size = person.sizeList.get(i);
				sizeSum += size;
				writer.setWriteFile(new File(outputFolder + f.getName().substring(0, f.getName().length()-3) + "bvh"), size);
				for (int j = 0; j < size; j++) {
					writer.writeNextFrame();
				}
			}
			writer.close();
			
			System.out.println(file.getName() + " : " + sizeSum + " : " + person.fileList.size() + " : " + person.sizeList.get(0) + " : " + person.fileList.get(0).getName());
		}
		
	}
}
