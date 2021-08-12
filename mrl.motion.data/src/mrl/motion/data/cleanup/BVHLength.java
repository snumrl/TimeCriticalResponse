package mrl.motion.data.cleanup;

import java.io.File;

import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.parser.BVHParser;

public class BVHLength {

	public static void main(String[] args) {
//		String folder = "C:\\data\\motionsynth_data\\cmu\\motion";
		String folder = "C:\\data\\모션캡처\\남자_All";
//		String folder = "C:\\data\\200608_MotionCapture\\BVH";
		SkeletonData.USE_SINGLE_SKELETON = false;
		int sum = 0;
		double tSum = 0;
		int count = 0;
		for (File f : new File(folder).listFiles()) {
			if (!f.getName().toLowerCase().endsWith(".bvh")) continue;
			BVHParser parser = new BVHParser();
			parser.parseSkeleton(f);
			int len = parser.frameSize;
			sum += len;
			tSum += len*parser.frameTime;
			count++;
			
//			MotionData mData = new BVHParser().parse(f);
//			System.out.println(f.getName() + "\t" + mData.motionList.size() + "\t" + mData.motionList.size()/(double)mData.framerate + "\t" + mData.framerate);
//			sum += mData.motionList.size();
//			tSum += mData.motionList.size()/(double)mData.framerate;
		}
		System.out.println(sum + " , " + tSum + " , " + count);
	}
}
