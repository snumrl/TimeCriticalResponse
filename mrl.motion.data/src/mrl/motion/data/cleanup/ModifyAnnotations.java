package mrl.motion.data.cleanup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionAnnotationManager;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class ModifyAnnotations {

	static void shortening(String annFolder, double ratio){
		File folder = new File(annFolder);
		for (File file : folder.listFiles()){
			if (file.isDirectory() || !file.getName().endsWith(".lab")) continue;
			ArrayList<MotionAnnotation> list = MotionAnnotation.load(file);
			if (list.size() == 0) continue;
			for (MotionAnnotation ann : list) {
				if (ann.interactionFrame < 0) continue;
				int prev = ann.interactionFrame - ann.startFrame;
				int post = ann.endFrame - ann.interactionFrame;
				
				prev = MathUtil.round(prev*ratio);
				post = MathUtil.round(post*ratio);
				ann.startFrame = ann.interactionFrame - prev;
				ann.endFrame = ann.interactionFrame + post;
			}
			MotionAnnotation.save(list, file);
		}
	}
	
}

