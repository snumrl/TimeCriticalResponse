package mrl.motion.neural.dancecard;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MotionAnnotation;

public class ActionTransitionConnector {

	private static int ACTION_MARGIN = 10;
	private static int TypeTransition = 0;
	private static int TypeActionPrefix = 1;
	private static int TypeActionPostfix = 2;
	private static int TypeActionMain = 3;
	
	private int[] frameTypes;

	public ActionTransitionConnector(MDatabase database) {
		frameTypes = new int[database.getMotionList().length];
		for (int i = 0; i < frameTypes.length; i++) {
			frameTypes[i] = TypeTransition;
		}
		ArrayList<MotionAnnotation> annList = database.getTransitionAnnotations();
		for (MotionAnnotation ann : annList) {
			if (ann.interactionFrame <= 0) continue;
			
			int start = database.findMotion(ann.file, ann.startFrame).motionIndex;
			int end = start + (ann.endFrame - ann.startFrame);
			int interaction = start + (ann.interactionFrame - ann.startFrame);
			
			int prefixEnd = interaction - ACTION_MARGIN;
			int postfixStart = interaction + ACTION_MARGIN;
			
			mark(start, prefixEnd, TypeActionPrefix);
			mark(prefixEnd, postfixStart, TypeActionMain);
			mark(postfixStart, end+1, TypeActionPostfix);
		}
	}
	
	private void mark(int start, int end, int type) {
		for (int i = start; i < end; i++) {
			frameTypes[i] = type;
		}
	}
	
	public double getEdgeWeight(int source, int target) {
		int s = frameTypes[source];
		int t = frameTypes[target];
		if (s == TypeTransition && t == TypeTransition) {
			return 1;
		}
		if (s == TypeTransition && t == TypeActionPrefix) {
			return 0.33;
		}
		if (s == TypeActionPostfix && t == TypeTransition) {
			return 0.33;
		}
		if (s == TypeActionPostfix && t == TypeActionPrefix) {
			return 1;
		}
		
		if (s == TypeActionPrefix && t == TypeActionPrefix) {
			return 1;
		}
		if (s == TypeActionPostfix && t == TypeActionPostfix) {
			return 1;
		}
		return 9999999;
	}
}
