package mrl.motion.data.clip;

import mrl.motion.data.MDatabase;
import mrl.motion.data.clip.MotionClip.ClipFrame;

public class ClipDatabase {

	public MDatabase database;
	
	private ClipFrame[] motionList;
	

	public ClipDatabase(MDatabase database) {
		this.database = database;
	}
	
	public ClipFrame[] getMotionList() {
		return motionList;
	}
}
