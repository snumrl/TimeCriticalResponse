package mrl.motion.data.cleanup;

import java.io.File;
import java.util.ArrayList;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.data.parser.BVHWriter;

public class BVHCrop {
	
	public static void crop(File bvhFile, int startFrame, int endFrame, File output){
		MotionData motionData = new BVHParser().parse(bvhFile);
		ArrayList<Motion> mList = new ArrayList<>();
		for (int i = startFrame; i < endFrame; i++) {
			mList.add(motionData.motionList.get(i));
		}
		MotionData cropData = new MotionData(motionData.skeletonData, mList);
		BVHWriter writer = new BVHWriter(bvhFile);
		writer.write(output, cropData);
	}

	public static void main(String[] args) {
		File bvhFile = new File("D:\\data\\basketMotion\\bvh_retargeted_20141107\\s_010_1_1.bvh");
		File output = new File("C:\\Dev\\workspace-new\\mrl.motion.topology\\data6\\s_010_1_1.bvh");
		
		crop(bvhFile, 0, 1000, output);
	}
}
