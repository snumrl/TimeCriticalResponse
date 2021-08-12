package mrl.motion.data;

import java.util.LinkedList;
import java.util.List;

import mrl.motion.data.parser.BVHParser;
import mrl.util.Utils;

public class MotionSelection {

	private static MotionSelection instance = new MotionSelection();
	
	private LinkedList<Motion> motionList = new LinkedList<Motion>();
	private long _startTime = -1;
	
	public static MotionSelection instance() {
		return instance;
	}
	
	public long startTime() {
		if (_startTime < 0) {
			_startTime = System.currentTimeMillis();
		}
		return _startTime;
	}
	
	public long initStartTime() {
		_startTime = System.currentTimeMillis();
		return _startTime;
	}
	
	public void setSingleMotion(Motion m) {
		motionList.clear();
		motionList.add(m);
	}
	
	public void addKnotMotion(Motion m) {
		motionList.add(m);
		if (motionList.size() > 5) {
			motionList.removeFirst();
		}
	}
	
	public Motion getCurrentMotion() {
		if (motionList.isEmpty()) return null;
		if (motionList.size() == 1) {
			return motionList.get(0);
		}
		double knot = getKnot(System.currentTimeMillis() - 70);
		return timeInterpolatedMotion(motionList, knot);
	}
	
	public double getKnot(long time) {
		long dt = time - startTime();
		double knot = (dt/1000d)*30;
		return knot;
	}
	
	private static Motion timeInterpolatedMotion(List<Motion> mList, double knot){
//		System.out.println("t interpol :: " + Utils.last(mList).knot + " : " + knot);
		for (int i = 0; i < mList.size()-1; i++) {
			double knot1 = mList.get(i).knot;
			double knot2 = mList.get(i+1).knot;
			if (knot >= knot1 && knot <= knot2){
				double ratio = (knot - knot1)/(knot2 - knot1);
//				System.out.println("knot  kkk : " + Utils.toString(knot, ratio, knot1, knot2));
				Motion m = Motion.interpolateMotion(mList.get(i), mList.get(i+1), ratio);
				m.knot = knot;
				return m;
			}
		}
		return Utils.last(mList);
	}
	
}
