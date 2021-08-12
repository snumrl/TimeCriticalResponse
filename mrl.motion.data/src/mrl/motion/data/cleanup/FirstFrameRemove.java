package mrl.motion.data.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FirstFrameRemove {
	
	public static void firstFrameRemove(File file){
		try {
			File tempFile = new File(file.getAbsoluteFile().getParent() + "\\" + "_" + file.getName());
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
			System.out.println("file : " + file.getName());
			String line;
			boolean isFirst = false;
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0) continue;
				if (line.startsWith("Frames:")){
					int frames = Integer.parseInt(line.substring("Frames:".length() + 1));
					line = "Frames:	" + (frames-1);
				}
				
				if (isFirst){
					isFirst = false;
//					bw.write(line + "\r\n");
				} else {
					if (line.startsWith("Frame Time:")){
						isFirst = true;
					}
					bw.write(line + "\r\n");
				}
			}
			bw.close();
			br.close();
			
			file.delete();
			tempFile.renameTo(file);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		File folder = new File("D:\\data\\AniCourse\\cmu");
		for (File file : folder.listFiles()){
			if (file.isDirectory()) continue;
			if (file.getName().startsWith("_")) continue;
			if (!file.getName().endsWith("bvh")) continue;
			firstFrameRemove(file);
		}
	}

}
