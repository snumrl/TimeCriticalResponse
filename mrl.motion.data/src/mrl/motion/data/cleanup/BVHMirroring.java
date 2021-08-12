package mrl.motion.data.cleanup;

import java.io.File;

import mrl.motion.data.MotionData;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.data.parser.BVHWriter;
import mrl.motion.data.trasf.MotionMirroring;

public class BVHMirroring {

	public static void mirror(String folder, boolean overwrite) {
		for (File f : new File(folder).listFiles()) {
			if (!f.getName().toLowerCase().endsWith(".bvh")) continue;
			
			String name = f.getName();
			name = name.substring(0, name.length() - ".bvh".length());
			BVHWriter writer = new BVHWriter(f);
			MotionData mData = new BVHParser().parse(f);
			mData = MotionMirroring.mirrorMotion_NC(mData);
			
			if (overwrite) {
				File temp = new File(folder + "\\" + name + "_mirror.bvh");
				writer.write(temp, mData);
				f.delete();
				temp.renameTo(f);
			} else {
				writer.write(new File(folder + "\\mirror_" + name + ".bvh"), mData);
//				f.renameTo(new File(folder + "\\" + name + "_origin.bvh"));
			}
		}
	}
	
}
