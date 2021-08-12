package mrl.motion.data.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class BVHScale {
	
	
	public static void scale(File file, double scale) throws IOException{
		scale(file, scale, null);
	}
	
	public static void scale(File file, double scale, File destFile) throws IOException{
		File tempFile;
		if (destFile == null) {
			tempFile = new File(file.getAbsoluteFile().getParent() + "\\" + "_" + file.getName());
		} else {
			tempFile = destFile;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
		System.out.println("file : " + file.getName());
		String line;
		boolean isStarted = false;
		while ((line = br.readLine()) != null) {
			line = line.replace('\t', ' ');
			String prefix = line.substring(0, line.indexOf(line.trim()));
			line = line.trim();
			if (line.trim().length() == 0) continue;
			if (line.trim().startsWith("OFFSET ")){
				String[] tokens = line.trim().split(" ");
				line = line.substring(0, line.indexOf("OFFSET")) + "OFFSET " + String.format("%.6f %.6f %.6f", 
						Double.parseDouble(tokens[1]) * scale,Double.parseDouble(tokens[2]) * scale,Double.parseDouble(tokens[3]) * scale);
			}
			if (isStarted && line.trim().length() > 0){
				int i1 = line.indexOf(" ");
				int i2 = line.indexOf(" ", i1+1);
				int i3 = line.indexOf(" ", i2+1);
				
				String[] tokens = line.split(" ");
				line = String.format("%.6f %.6f %.6f", 
						Double.parseDouble(tokens[0]) * scale,Double.parseDouble(tokens[1]) * scale,Double.parseDouble(tokens[2]) * scale) + line.substring(i3);
			}
			
			bw.write(prefix + line + "\r\n");
			
			if (line.startsWith("Frame Time:")) isStarted = true;
		}
		bw.close();
		br.close();
		
		if (destFile == null) {
			file.delete();
			tempFile.renameTo(file);
		}
	}
	
	public static void scaleFolder(String folder, double scale, boolean appendPersonNumber) {
		try{
			for (File file : new File(folder).listFiles()){
				if (file.isDirectory()) continue;
				if (file.getName().startsWith("_")) continue;
				if (!file.getName().endsWith("bvh")) continue;
				if (appendPersonNumber) {
					File rename = new File(file.getAbsolutePath().replace(".bvh", "") + "_1.bvh");
					file.renameTo(rename);
					file = rename;
				}
				scale(file, scale);
			}
		} catch (IOException e){
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
//		{
//			Vector3d v = new Vector3d(24,-10,50);
//			Matrix3d m = eulerToMatrixZXY(v);
//			Vector3d e = matrixToEulerZXY(m);
//			System.out.println(v);
//			System.out.println(e);
//			System.exit(0);
//		}
//		
//		
		try{
//			scale(new File("D:\\data\\Tennis\\test\\test.bvh"), 2.66);
//			scale(new File("D:\\data\\Tennis\\tennis_mocapdata.com\\bvh\\test.bvh"), 2.66);
//			System.exit(0);
//			scale(new File("Trial001.bvh"), 100);
//			scale(new File("Trial002.bvh"), 100);
//			scale(new File("Trial004.bvh"), 100);
//			System.exit(0);
//			scale(new File("bvhOutput\\KMS.bvh"), 100);
//			scale(new File("bvhOutput\\SDH.bvh"), 100);
//			scale(new File("bvhOutput\\YDS.bvh"), 100);
//			System.exit(0);
			
//			double scale = 100;
			double scale = 2.54; // inch to cm
			scale*=2;
			scale = 1.1;
			String folder = "C:\\data\\200608_MotionCapture\\merged_orc";
			scaleFolder(folder, scale, false);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
