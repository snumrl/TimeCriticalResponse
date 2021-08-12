package mrl.motion.graph;

import java.util.ArrayList;

import javax.vecmath.Matrix4d;

import mrl.motion.data.Motion;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.position.PositionMotion;
import mrl.util.Utils;
import static mrl.util.Configuration.BLEND_MARGIN;
import static mrl.util.Configuration._ENABLE_BLENDING;
import static mrl.motion.data.trasf.MotionTransform.getAlignTransform;

public class MotionSegment {
	
	public int startIndex;
	public int endIndex;
	private ArrayList<Motion> motionList;
	private ArrayList<Motion> notBlendedMotionList;
	
	public MotionSegment(MotionSegment copy){
		this.startIndex = copy.startIndex;
		this.endIndex = copy.endIndex;
		motionList = new ArrayList<Motion>();
		for (Motion m : copy.motionList){
			motionList.add(new Motion(m));
		}
		notBlendedMotionList = new ArrayList<Motion>();
		for (Motion m : copy.notBlendedMotionList){
			notBlendedMotionList.add(new Motion(m));
		}
	}
	
	public MotionSegment(MotionSegment segment, int startIndex, int endIndex) {
		ArrayList<Motion> mList = segment.getEntireMotion();
		motionList = new ArrayList<Motion>();
		for (Motion m : Utils.cut(mList, startIndex, endIndex + BLEND_MARGIN*2)){
			motionList.add(new Motion(m));
		}
		notBlendedMotionList = new ArrayList<Motion>();
		for (Motion m : motionList){
			notBlendedMotionList.add(new Motion(m));
		}
		this.startIndex = -1;
		this.endIndex = -1;
	}
	public MotionSegment(Motion[] mList, int startIndex, int endIndex) {
		this(mList, startIndex, endIndex, false);
	}
	public MotionSegment(Motion[] mList, int startIndex, int endIndex, boolean byIndex) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		
		motionList = new ArrayList<Motion>();
		if (byIndex){
			for (int i = 0; i < BLEND_MARGIN; i++) {
				int idx = Math.max(0, startIndex - BLEND_MARGIN + i);
				motionList.add(new Motion(mList[idx]));
			}
		} else {
			Motion s = mList[startIndex];
			int count = 0;
			for (int i = 0; i < BLEND_MARGIN; i++) {
				if (s.prev == null) break;
				s = s.prev;
				count++;
			}
			int lackCount = BLEND_MARGIN - count;
			for (int i = 0; i < lackCount; i++) {
				motionList.add(new Motion(s));
			}
			for (int i = 0; i < count; i++) {
				Motion motion = new Motion(s);
				motionList.add(motion);
				if (s.next != null) s = s.next;
			}
		}
		
		for (int i = startIndex; i <= endIndex; i++) {
			Motion motion = new Motion(mList[i]);
			motionList.add(motion);
		}
		
		if (byIndex){
			for (int i = 0; i < BLEND_MARGIN; i++) {
				int idx = Math.min(endIndex + i + 1, mList.length - 1);
				motionList.add(new Motion(mList[idx]));
			}
		} else {
			Motion e = mList[endIndex];
			for (int i = 0; i < BLEND_MARGIN; i++) {
				if (e.next != null) e = e.next;
				Motion motion = new Motion(e);
				motionList.add(motion);
			}
		}
		
		for (int i = 0; i < motionList.size(); i++) {
			motionList.get(i).knot = i - BLEND_MARGIN;
		}
		notBlendedMotionList = new ArrayList<Motion>();
		for (Motion m : motionList){
			notBlendedMotionList.add(new Motion(m));
		}
	}
	
	public static int BLEND_MARGIN(){
		return BLEND_MARGIN;
	}
	
	public void type(int type){
		for (Motion motion : motionList){
			motion.type = type;
		}
	}
	
	public void moveKnot(double knot) {
		for (Motion m : getEntireMotion()) {
			m.knot += knot;
		}
		for (Motion m : notBlendedMotionList) {
			m.knot += knot;
		}
	}
	
	public void alignKnotToZero() {
		moveKnot(-firstMotion().knot);
	}
	
	public int trimByKnot(double minKnot){
		int idx = -1;
		for (int i = BLEND_MARGIN; i < motionList.size() - BLEND_MARGIN; i++) {
			if (motionList.get(i).knot <= minKnot){
				idx = i;
			}
		}
		if (idx < 0) return -1;
		
		ArrayList<Motion> newList = new ArrayList<Motion>();
		for (int i = idx - BLEND_MARGIN; i < motionList.size(); i++) {
			newList.add(motionList.get(i));
		}
		int removed = motionList.size() - newList.size();
		
		motionList = newList;
		startIndex = motionList.get(BLEND_MARGIN).motionIndex;
		
		return removed;
	}
	
	public void set(MotionSegment segment){
		this.startIndex = segment.startIndex;
		this.endIndex = segment.endIndex;
		this.motionList = segment.motionList;
		this.notBlendedMotionList = segment.notBlendedMotionList;
	}
	
	public MotionSegment cut(int startOffset, int endOffset){
		MotionSegment s = new MotionSegment(this);
		s.startIndex = startIndex + startOffset;
		s.endIndex = endIndex - endOffset;
		s.motionList = new ArrayList<Motion>();
		s.notBlendedMotionList = new ArrayList<Motion>();
		for (int i = startOffset; i < motionList.size() - endOffset; i++) {
			s.motionList.add(new Motion(motionList.get(i)));
			s.notBlendedMotionList.add(new Motion(notBlendedMotionList.get(i)));
		}
		return s;
	}
	
	private Motion last(){
		return notBlendedMotionList.get(motionList.size() - 1 - BLEND_MARGIN);
//		return motionList.get(motionList.size() - 1 - BLEND_MARGIN);
	}
	private Motion first(){
		return notBlendedMotionList.get(BLEND_MARGIN - 1);
//		return motionList.get(BLEND_MARGIN - 1);
	}
	
	public Motion firstMotion(){
//		return notBlendedMotionList.get(BLEND_MARGIN);
		return motionList.get(BLEND_MARGIN);
	}
	public Motion lastMotion(){
		return motionList.get(motionList.size() - 1 - BLEND_MARGIN);
	}
	
	public double getStartKnot(){
		return motionList.get(BLEND_MARGIN).knot;
	}
	public double getEndKnot(){
		return motionList.get(motionList.size() - 1 - BLEND_MARGIN).knot;
	}
	
	public int length(){
		return motionList.size() - BLEND_MARGIN*2;
	}
	
	public ArrayList<Motion> getMotionList() {
		return Utils.cut(motionList, BLEND_MARGIN, motionList.size() - 1 - BLEND_MARGIN);
	}
	public ArrayList<Motion> getNotBlendedMotionList() {
		return Utils.cut(notBlendedMotionList, BLEND_MARGIN, notBlendedMotionList.size() - 1 - BLEND_MARGIN);
	}
	
	public ArrayList<Motion> getEntireMotion(){
		return motionList;
	}
	
	public ArrayList<Motion> getEntireNotBlendedMotion(){
		return notBlendedMotionList;
	}
	
	public void updateNotBlendedAsCurrent(){
		notBlendedMotionList = new ArrayList<Motion>();
		for (Motion m : motionList){
			notBlendedMotionList.add(new Motion(m));
		}
	}
	
	public static MotionSegment getPathMotion(Motion[] totalMotionList, int[][] path){
		return getPathMotion(totalMotionList, path, 0);
	}
	
	public static MotionSegment getPathMotion(Motion[] totalMotionList, int[][] path, int offset){
		MotionSegment pathMotion = null;
		for (int i = 0; i < path.length; i++) {
			int start = path[i][0];
			int end = path[i][1];
			if (i == 0) start += offset;
			if (i == path.length - 1) end -= offset;
			if (pathMotion == null){
				pathMotion = new MotionSegment(totalMotionList, start, end);
			} else {
				MotionSegment s = new MotionSegment(totalMotionList, start, end);
				pathMotion = stitch(pathMotion, s, true);
			}
		}
		alignToBase(pathMotion);
		return pathMotion;
	}

	public static MotionSegment stitch(MotionSegment s1, MotionSegment s2, boolean holdFirst){
		return stitch(s1, s2, holdFirst, null);
	}
	public static boolean _print = false;
	public static MotionSegment stitch(MotionSegment s1, MotionSegment s2, boolean holdFirst, MotionSegment related){
		if (s1 == null) {
			s2 = new MotionSegment(s2);
			s2.alignKnotToZero();
			return s2;
		}
		if (s2 == null) {
			s1 = new MotionSegment(s1);
			s1.alignKnotToZero();
			return s1;
		}
		
		int testIdx = s1.motionList.size() - 1 - BLEND_MARGIN;
		double knotDiff;
		Matrix4d t;
		if (holdFirst){
			knotDiff = s1.last().knot - s2.first().knot;
			t = getAlignTransform(s1.last().root(), s2.first().root());
//			t = getAlignTransform(s1.last().root(), s2.first().root());
			if (_print){
				System.out.println("align t : " + t);
				System.out.println(s1.last().root());
				System.out.println(s2.first().root());
				System.out.println(s1.notBlendedMotionList.get(testIdx).root());
				System.out.println("##############");
			}
			for (Motion motion : s2.motionList) {
				Matrix4d root = motion.root();
				root.mul(t, root);
				motion.knot += knotDiff;
			}
			for (Motion motion : s2.notBlendedMotionList) {
				Matrix4d root = motion.root();
				root.mul(t, root);
				motion.knot += knotDiff;
			}
			if (_print){
				System.out.println("############## after align ##########");
				System.out.println(s1.notBlendedMotionList.get(testIdx).root());
				System.out.println(s1.last().root());
				System.out.println(s2.first().root());
			}
		} else {
			knotDiff = s2.first().knot - s1.last().knot;
			t = getAlignTransform(s2.first().root(), s1.last().root());
			for (Motion motion : s1.motionList) {
				Matrix4d root = motion.root();
				root.mul(t, root);
				motion.knot += knotDiff;
			}
			for (Motion motion : s1.notBlendedMotionList) {
				Matrix4d root = motion.root();
				root.mul(t, root);
				motion.knot += knotDiff;
			}
		}
		
		if (related != null){
			for (Motion motion : related.motionList) {
				Matrix4d root = motion.root();
				root.mul(t, root);
				motion.knot += knotDiff;
			}
			for (Motion motion : related.notBlendedMotionList) {
				Matrix4d root = motion.root();
				root.mul(t, root);
				motion.knot += knotDiff;
			}
		}
		
		
		
		// ???? frame ???? interpolation ??? ??? knot ???? interpolation ?? ???
		int i1 = s1.motionList.size() - BLEND_MARGIN*2;
		int i2 = 0;
		double divide = (BLEND_MARGIN*2-1);
		for (int i = 0; i < BLEND_MARGIN*2; i++) {
			double ratio = i/divide;
			if (ratio < 0 || ratio > 1 || Double.isNaN(ratio)){
				throw new RuntimeException("invalid ratio  : "  + ratio + " : " + i + " : " + BLEND_MARGIN);
			}
			if (!_ENABLE_BLENDING){
				ratio = (ratio <= 0.5) ? 0 : 1;
			}
			Motion motion = Motion.interpolateMotion(s1.motionList.get(i1+i), s2.motionList.get(i2+i), ratio);
			if (_print){
				System.out.println("############## interpolation " + i + "##########");
				System.out.println(s1.notBlendedMotionList.get(testIdx).root());
//				System.out.println(s1.notBlendedMotionList.get(i1+i).root());
//				System.out.println(s2.notBlendedMotionList.get(i2+i).root());
				System.out.println(s2.first().root());
			}
			s1.motionList.set(i1+i, motion);
			if (i < BLEND_MARGIN) {
				s1.notBlendedMotionList.set(i1+i, s1.notBlendedMotionList.get(i1+i));
			} else {
				s1.notBlendedMotionList.set(i1+i, s2.notBlendedMotionList.get(i2+i));
			}
		}
		
		for (int i = BLEND_MARGIN*2; i < s2.motionList.size(); i++) {
			s1.motionList.add(s2.motionList.get(i));
			s1.notBlendedMotionList.add(s2.notBlendedMotionList.get(i));
		}
		s1.endIndex = s2.endIndex;
		
		return s1;
	}
	
	public static ArrayList<Motion> stitchByInterpolation(ArrayList<Motion> m1, ArrayList<Motion> m2, boolean transformRoot){
		return stitchByInterpolation(m1, m2, transformRoot, 10, 0);
	}
	public static ArrayList<Motion> stitchByInterpolation(ArrayList<Motion> m1, ArrayList<Motion> m2, boolean transformRoot, int margin, int alignOffset){
		Matrix4d t = getAlignTransform(m1.get(m1.size()-1 - alignOffset).root(), m2.get(0).root());
		if (!transformRoot){
			t.setIdentity();
		}
		Motion target = new Motion(m2.get(0));
		target.root().mul(t, target.root());
		
		ArrayList<Motion> result = new ArrayList<Motion>();
		margin = Math.min(margin, m1.size()/2);
		for (int i = 0; i < m1.size(); i++) {
			Motion m = new Motion(m1.get(i));
			if (i >= m1.size() - margin){
				double ratio = (i - (m1.size() - margin))/(double)(margin-1);
				m = Motion.interpolateMotion(m, target, ratio);
			}
			result.add(m);
		}
		for (int i = 0; i < m2.size(); i++) {
			Motion m = new Motion(m2.get(i));
			m.root().mul(t, m.root());
			if (i < margin){
				double ratio = i/(double)(margin-1);
				m = Motion.interpolateMotion(target, m, ratio);
			}
				
			result.add(m);
		}
		return result;
	}
	
	public static void align(MotionSegment base, MotionSegment toAlign){
		if (base == null) return;
		double knotDiff = base.last().knot - toAlign.first().knot;
		Matrix4d t = getAlignTransform(base.last().root(), toAlign.first().root());
		for (Motion motion : toAlign.motionList) {
			Matrix4d root = motion.root();
			root.mul(t, root);
			motion.knot += knotDiff;
		}
		for (Motion motion : toAlign.notBlendedMotionList) {
			Matrix4d root = motion.root();
			root.mul(t, root);
			motion.knot += knotDiff;
		}
	}
	
	public static void alignToBase(MotionSegment segment){
		Motion base = segment.firstMotion();
		Pose2d pose = PositionMotion.getPose(base);
		Matrix4d mm = Pose2d.globalTransform(pose, Pose2d.BASE).to3d();
		for (Motion m : segment.motionList) {
			Matrix4d root = m.root();
			root.mul(mm, root);
		}
		for (Motion m : segment.notBlendedMotionList) {
			Matrix4d root = m.root();
			root.mul(mm, root);
		}
	}
	
	public static ArrayList<Motion> alignToBase(ArrayList<Motion> toAlign){
		return alignToBase(toAlign, 0);
	}
	public static ArrayList<Motion> alignToBase(ArrayList<Motion> toAlign, int offset){
		return Pose2d.getAlignedMotion(toAlign, offset);
//		Matrix4d base = new Matrix4d();
//		base.set(MotionTransform.baseOrientation);
//		Matrix4d t = getAlignTransform(base, toAlign.get(offset).root());
//		for (Motion motion : toAlign) {
//			Matrix4d root = motion.root();
//			root.mul(t, root);
//		}
//		return toAlign;
	}
	
	public static ArrayList<Motion> align(ArrayList<Motion> toAlign, Motion targetMotion){
		Matrix4d t = getAlignTransform(targetMotion.root(), toAlign.get(0).root());
//		Matrix4d t = getAlignTransform(toAlign.get(0).root(), targetMotion.root());
		return align(toAlign, t);
		
	}
	public static void align(MotionSegment toAlign, Matrix4d alignTransform){
		for (Motion motion : toAlign.motionList) {
			Matrix4d root = motion.root();
			root.mul(alignTransform, root);
		}
		for (Motion motion : toAlign.notBlendedMotionList) {
			Matrix4d root = motion.root();
			root.mul(alignTransform, root);
		}
	}
	
	public static ArrayList<Motion> align(ArrayList<Motion> toAlign, Matrix4d alignTransform){
		for (Motion motion : toAlign) {
			Matrix4d root = motion.root();
			root.mul(alignTransform, root);
		}
		return toAlign;
	}
	
	
}
