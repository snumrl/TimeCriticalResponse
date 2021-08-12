package mrl.jni.motion;

import java.util.ArrayList;

import javax.vecmath.Matrix4d;

import mrl.motion.data.Motion;
import mrl.motion.graph.MotionSegment;

public class RootTrajectory{
	public ArrayList<RootInfo> motionList;
	
	public RootTrajectory(MotionSegment segment, boolean entireMotion) {
		motionList = new ArrayList<RootInfo>();
		ArrayList<Motion> mList = entireMotion ? segment.getEntireMotion() : segment.getMotionList();
		for (Motion motion : mList){
			motionList.add(new RootInfo(motion));
		}
		for (int i = 0; i < motionList.size(); i++) {
			motionList.get(i).knot = i;
		}
	}
	
	public RootTrajectory(ArrayList<RootInfo> motionList) {
		this.motionList = motionList;
	}
	
	public RootTrajectory(RootTrajectory copy){
		motionList = new ArrayList<RootInfo>();
		for (RootInfo info : copy.motionList){
			motionList.add(new RootInfo(info));
		}
	}

	public int length(){
		return motionList.size();
	}
	
	public ArrayList<RootInfo> getMotionList() {
		return motionList;
	}
	
	public static class RootInfo{
		public int motionIndex;
		public Matrix4d transf;
		public double knot = Double.NaN;
		
		public RootInfo(){
		}
		public RootInfo(RootInfo copy){
			this.motionIndex = copy.motionIndex;
			this.transf = new Matrix4d(copy.transf);
			this.knot = copy.knot;
		}
		
		public RootInfo(Motion motion) {
			this.transf = new Matrix4d(motion.root());
			this.motionIndex = motion.motionIndex;
		}
		
		public Matrix4d root(){
			return transf;
		}
	}
}