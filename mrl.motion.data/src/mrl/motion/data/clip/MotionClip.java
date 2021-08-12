package mrl.motion.data.clip;

import mrl.motion.data.Motion;

public class MotionClip {
	
	public ClipFrame[] frames;
	
	public MotionClip(Motion[] motions) {
		frames = new ClipFrame[motions.length];
		for (int i = 0; i < motions.length; i++) {
			ClipFrame frame = new ClipFrame();
			frame.clip = this;
			frame.motion = motions[i];
			frames[i] = frame;
			if (i > 0) {
				frame.prev = frames[i - 1];
				frames[i - 1].next = frame;
			}
		}
	}

	public static class ClipFrame{
		public MotionClip clip;
		public Motion motion;
		public int cfIndex;
		public ClipFrame prev;
		public ClipFrame next;
	}
}
