package mrl.motion.data.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import mrl.util.MathUtil;
import mrl.util.TextUtil;

public class FPSReducer {
	
	public static int DEFAULT_SAMPLING_SIZE = 4;
	
	public static void fpsReduce(File file){
		fpsReduce(file, -1);
	}
	public static void fpsReduce(File file, int targetFrameRate){
		try {
			File tempFile = new File(file.getAbsoluteFile().getParent() + "\\" + "_" + file.getName());
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
			System.out.println("file : " + file.getName());
			
			int samplingSize = DEFAULT_SAMPLING_SIZE;
			int fps = -1;
			if (fpsMap != null) {
				String fName = file.getName();
				int subject = Integer.parseInt(fName.substring(0, fName.indexOf("_")));
				fps = fpsMap.get(subject);
				samplingSize = fps/30;
			}
			
			String line;
			boolean isStarted = false;
			int idx = 0;
			int frames = -1;
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0) continue;
				if (line.startsWith("Frames:")){
					frames = Integer.parseInt(line.substring("Frames:".length() + 1));
					continue;
				}
				
				if (isStarted){
					if ((idx % samplingSize) == 0){
						bw.write(line + "\r\n");
					}
					idx++;
				} else {
					if (line.startsWith("Frame Time:")){
						isStarted = true;
						double frameTime = Double.parseDouble(line.substring("Frame Time:".length() + 1));
						
						if (targetFrameRate > 0) {
							double targetFTime = 1d/targetFrameRate;
							samplingSize = MathUtil.round(targetFTime/frameTime);
						} else {
							if (fps > 0) {
								frameTime = 1d/fps;
							}
						}
						frameTime = frameTime * samplingSize;
						frames = (int)Math.ceil(frames/(double)samplingSize);
						line = "Frames:	" + frames;
						bw.write(line + "\r\n");
						
//						line = "Frame Time:	" + "0.0333333";
						line = "Frame Time:	" + String.format("%.7f", frameTime);
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
	
	
	static HashMap<Integer, Integer> fpsMap;
	static void collectFPS() {
		fpsMap = new HashMap<Integer, Integer>();
		TextUtil.openReader("cmu_motion_list.txt.");
		String line = null;
		while ((line = TextUtil.readLine())!=null) {
			if (line.startsWith("Subject #")) {
				line = line.substring("Subject #".length());
				int idx = line.indexOf(" (");
				
				int subject = Integer.parseInt(line.substring(0, idx));
				line = TextUtil.readLine();
				line = TextUtil.readLine();
				line = TextUtil.readLine();
				String[] tokens = line.split("\t");
				int fps = Integer.parseInt(tokens[tokens.length-2]);
				System.out.println(subject + "\t" + fps);
				fpsMap.put(subject, fps);
			}
		}
	}
	
	public static void main(String[] args) {
//		collectFPS();
//		System.exit(0);
		
		try{
			DEFAULT_SAMPLING_SIZE = 2;
			File folder = new File("C:\\data\\200608_MotionCapture\\retarget");
			
			for (File file : folder.listFiles()){
				if (file.isDirectory()) continue;
				if (file.getName().startsWith("_")) continue;
				if (!file.getName().endsWith("bvh")) continue;
				fpsReduce(file, 30);
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
