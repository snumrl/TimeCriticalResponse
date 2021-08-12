package mrl.motion.data;

import java.io.File;
import java.util.ArrayList;

import mrl.util.Configuration;

public class MotionData {

	public static MotionData selectedData;
	
	public SkeletonData skeletonData;
	public ArrayList<Motion> motionList;
	public int framerate = Configuration.DEFAULT_FPS;
	public File file = null;
	
	public MotionData(SkeletonData skeletonData, ArrayList<Motion> motionList) {
		this.skeletonData = skeletonData;
		this.motionList = motionList;
	}
	
	public MotionData(ArrayList<Motion> motionList){
		this.skeletonData = SkeletonData.instance;
		this.motionList = motionList;
	}
	
	
	public MotionData(MotionData copy){
		this(copy, false);
//		this.framerate = copy.framerate;
//		this.skeletonData = copy.skeletonData;
//		this.motionList = new ArrayList<Motion>();
//		for (Motion m : copy.motionList){
//			motionList.add(new Motion(m));
//		}
	}
	
	public MotionData(MotionData copy, boolean mirror){
		this.framerate = copy.framerate;
		this.skeletonData = copy.skeletonData;
		this.motionList = new ArrayList<Motion>();
		
		if (mirror){
			this.file = new File(copy.file.getAbsolutePath() + "_mirrored");
			for (Motion m : copy.motionList){
				motionList.add(Motion.mirroredMotion(m));
			}
		} else {
			this.file = copy.file;
			for (Motion m : copy.motionList){
				motionList.add(new Motion(m));
			}
		}
		for (int i = 0; i < motionList.size()-1; i++) {
			Motion m1 = motionList.get(i);
			Motion m2 = motionList.get(i+1);
			m1.next = m2;
			m2.prev = m1;
		}
		for (Motion m : motionList){
			m.motionData = this;
		}
	}
	
	public Motion firstMotion(){
		return motionList.get(0);
	}
	
	public Motion lastMotion(){
		return motionList.get(motionList.size()-1);
	}
	
	
	public static int getFrameIndex(ArrayList<Motion> motionList, int time){
		return getFrameIndex(motionList, time, -1);
	}
	public static int getFrameIndex(ArrayList<Motion> motionList, int time, double maxDiff){
		double minDiff = Integer.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i < motionList.size(); i++) {
			if (motionList.get(i) == null) continue;
			double diff = Math.abs(motionList.get(i).knot - time);
			if (diff < minDiff){
				minIndex = i;
				minDiff = diff;
			}
		}
		if (maxDiff >= 0 && minDiff > maxDiff) return -1;
		return minIndex;
	}
	
	public static int maxFrame(MotionData[] motionDataList){
		int maxFrame = -1;
		for (MotionData data : motionDataList) {
			if (data == null) continue;
			for (int i = 0; i < data.motionList.size(); i++) {
				Motion motion = data.motionList.get(i);
				if (motion == null) continue;
				int frame = Double.isNaN(motion.knot) ? i : (int)Math.ceil(motion.knot);
				maxFrame = Math.max(maxFrame, frame+1);
			}
		}
		return maxFrame;
	}
	
	public static MotionData cut(MotionData data, int startFrame, int endFrame){
		MotionData result = new MotionData(data);
		ArrayList<Motion> motionList = new ArrayList<Motion>();
		for (int i = startFrame; i <= endFrame; i++) {
			motionList.add(data.motionList.get(i));
		}
		result.motionList = motionList;
		return result;
	}
	
	public static void adjustKnot(MotionData[] motionDataList){
		double offset = Integer.MAX_VALUE;
		for (MotionData motionData : motionDataList){
			for (Motion motion : motionData.motionList){
				if (motion != null){
					offset = Math.min(offset, motion.knot);
					break;
				}
			}
		}
		if (Double.isNaN(offset)) return;
		
		for (MotionData motionData : motionDataList){
			for (Motion motion : motionData.motionList){
				if (motion != null){
					motion.knot -= offset;
				}
			}
		}
	}
	
	public void initializeKnot(){
		for (int i = 0; i < motionList.size(); i++) {
			motionList.get(i).knot = i;
		}
	}
	
	public int findMotionFrame(int motionIndex, int start, int end){
		int minDistance = 0;
		int minIndex = -1;
		for (int i = start; i <= end; i++) {
			int idx = i;
//			int idx = getFrameIndex(motionList, i, 3);
			if (idx < 0) continue;
			
			int d = Math.abs(motionList.get(idx).motionIndex - motionIndex);
			if (d <= minDistance){
				minDistance = d;
				minIndex = idx; 
			}
		}
		return minIndex;
	}
	
	public static ArrayList<Motion> timeInterpolation(ArrayList<Motion> mList, double[] knotList){
		ArrayList<Motion> list = new ArrayList<Motion>();
		for (double knot : knotList){
			list.add(timeInterpolation(mList, knot));
		}
		return list;
	}
	
	public static Motion timeInterpolation(ArrayList<Motion> mList, double knot){
		for (int i = 0; i < mList.size()-1; i++) {
			double knot1 = mList.get(i).knot;
			double knot2 = mList.get(i+1).knot;
			if (knot >= knot1 && knot <= knot2){
				double ratio = (knot - knot1)/(knot2 - knot1);
				Motion m = Motion.interpolateMotion(mList.get(i), mList.get(i+1), ratio);
				m.knot = knot;
				return m;
			}
		}
		return null;
	}
	
	public static Motion[] linkMotionList(ArrayList<Motion> mList){
		for (int i = 0; i < mList.size()-1; i++) {
			Motion m1 = mList.get(i);
			Motion m2 = mList.get(i+1);
			m1.next = m2;
			m2.prev = m1;
		}
		return mList.toArray(new Motion[mList.size()]);
	}
	
		
	public static ArrayList<Motion> divideByKnot(ArrayList<Motion> list){
		return divideByKnot(list, list.get(0).knot);
	}
	
	public static ArrayList<Motion> divideByKnot(ArrayList<Motion> list, double startKnot){
		ArrayList<Motion> result = new ArrayList<Motion>();
		double knot = startKnot;
		int index = 0;
		
		while (true){
			while (index < list.size()){
				if (list.get(index).knot > knot) break;
				index++;
			}
			if (index >= list.size()) break;
			
			Motion m2 = list.get(index);
			if (index == 0) {
				Motion m = new Motion(m2);
				m.knot = startKnot + index;
				result.add(m);
			} else {
				Motion m1 = list.get(index - 1);
				double ratio = (knot - m1.knot)/(m2.knot - m1.knot);
				Motion m = Motion.interpolateMotion(m1, m2, ratio);
				m.knot = knot;
				result.add(m);
			}
			knot += 1;
		}
		return result;
	}
}
