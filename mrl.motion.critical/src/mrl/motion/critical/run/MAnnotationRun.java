package mrl.motion.critical.run;

import mrl.motion.annotation.MotionAnnotationRun;

public class MAnnotationRun {

	public static void main(String[] args) {
		String dataFolder = "data\\martial_arts_compact";
		
		boolean openTransition = false;
//		openTransition = true;
		MotionAnnotationRun.open(dataFolder, !openTransition, true);
	}
}
