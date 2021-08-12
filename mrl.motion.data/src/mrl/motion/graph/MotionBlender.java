package mrl.motion.graph;

import java.util.ArrayList;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.MotionTransform;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class MotionBlender {

	public MDatabase database;
	public Motion[] totalMotions;
	public ArrayList<Motion> motionList = new ArrayList<Motion>();
	public ArrayList<Motion> notBlendedList = new ArrayList<Motion>();
	
	public static MotionBlender getPathMotion(MDatabase database, int[][] pathList){
		MotionBlender blender = new MotionBlender(database);
		Motion[] mList = database.getMotionList();
		for (int[] path : pathList) {
			blender.append(Utils.cut(mList, path[0], path[1]));
		}
		return blender;
	}
	
	public MotionBlender(MDatabase database) {
		this.database = database;
		this.totalMotions = database.getMotionList();
	}
	
	
	private AxisAngle4d[] getDisplacemenet(Motion sourceMotion, Motion targetMotion) {
		AxisAngle4d[] displacementList = new AxisAngle4d[sourceMotion.transforms.length];
		for (int i = 0; i < displacementList.length; i++) {
			Matrix4d source = sourceMotion.transforms[i];
			if (source == null) continue;
			Matrix4d target = targetMotion.transforms[i];
			Matrix4d d = new Matrix4d(source);
			d.invert();
			d.mul(d, target);
			AxisAngle4d a = new AxisAngle4d();
			a.set(d);
			displacementList[i] = a;
		}
		return displacementList;
	}
	
	public void append(Motion[] segment) {
		if (segment == null || segment.length == 0) return;
		if (motionList.size() == 0) {
			for (Motion m : segment) {
				motionList.add(m);
				notBlendedList.add(new Motion(m));
			}
//			Pose2d.getAlignedMotion(motionList, 0);
			return;
		}
		
		Motion last = Utils.last(motionList);
		Motion lastNext = new Motion(totalMotions[last.motionIndex].next);
		{
			lastNext.knot = last.knot + 1;
			Matrix4d t = MotionTransform.getAlignTransform(last.root(), totalMotions[last.motionIndex].root());
			Matrix4d root = lastNext.root();
			root.mul(t, root);
		}
		
//		Motion lastNext = Utils.last(motionList).next;
		Motion start = segment[0];
		double knotDiff = lastNext.knot - start.knot;
		Matrix4d t = MotionTransform.getAlignTransform(lastNext.root(), start.root());
		
		int lastSize = motionList.size();
		Motion[] aligned = new Motion[segment.length];
		for (int i = 0; i < aligned.length; i++) {
			Motion m = new Motion(segment[i]);
			m.root().mul(t, m.root());
			m.knot += knotDiff;
			aligned[i] = m;
			notBlendedList.add(new Motion(m));
			motionList.add(m);
		}
		
		Motion mid = Motion.interpolateMotion(motionList.get(lastSize), lastNext, 0.5);
		int margin = Configuration.BLEND_MARGIN - 1;
		Vector3d dv = new Vector3d();
		
		AxisAngle4d[] dPrev = getDisplacemenet(lastNext, mid);
		dv.sub(MathUtil.getTranslation(mid.root()), MathUtil.getTranslation(lastNext.root()));
//		dv.sub(MathUtil.getTranslation(lastNext.root()), MathUtil.getTranslation(motionList.get(lastSize).root()));
		for (int idx = 1; idx <= margin; idx++) {
			double ratio = (idx*2)/(double)(margin*2 + 1);
			Motion m = motionList.get(lastSize - 1 - margin +  idx);
//			System.out.println("blend : " + (blendStart + idx) + " : " + ratio);
			for (int j = 0; j < dPrev.length; j++) {
				if (dPrev[j] == null) continue;
				AxisAngle4d a = new AxisAngle4d(dPrev[j]);
				a.angle *= ratio;
				Matrix4d d = new Matrix4d();
				d.set(a);
				m.transforms[j].mul(d);
			}
			Vector3d translation = new Vector3d(dv);
			translation.scale(ratio);
			translation.add(MathUtil.getTranslation(m.root()));
			m.root().setTranslation(translation);
		}
		
		AxisAngle4d[] dPost = getDisplacemenet(motionList.get(lastSize), mid);
		dv.sub(MathUtil.getTranslation(mid.root()), MathUtil.getTranslation(motionList.get(lastSize).root()));
		for (int idx = 1; idx <= margin; idx++) {
			double ratio = 1 - (idx*2 - 1)/(double)(margin*2 + 1);
			Motion m = motionList.get(lastSize - 1 +  idx);
//			System.out.println("blend : " + (blendStart + idx) + " : " + ratio);
			for (int j = 0; j < dPost.length; j++) {
				if (dPost[j] == null) continue;
				AxisAngle4d a = new AxisAngle4d(dPost[j]);
				a.angle *= ratio;
				Matrix4d d = new Matrix4d();
				d.set(a);
				m.transforms[j].mul(d);
			}
			Vector3d translation = new Vector3d(dv);
			translation.scale(ratio);
			translation.add(MathUtil.getTranslation(m.root()));
			m.root().setTranslation(translation);
		}
		
		System.out.println("ss : " + motionList.size() + " : " + last.root() + " : " + lastNext.root() + " : " + aligned[0].root());
		AxisAngle4d[] displacementList = new AxisAngle4d[start.transforms.length];
		for (int i = 0; i < displacementList.length; i++) {
			Matrix4d source = aligned[0].transforms[i];
			if (source == null) continue;
			Matrix4d target = lastNext.transforms[i];
			Matrix4d d = new Matrix4d(source);
			d.invert();
			d.mul(d, target);
			AxisAngle4d a = new AxisAngle4d();
			a.set(d);
			displacementList[i] = a;
		}
		
//		for (int idx = 1; idx < blendLen; idx++) {
//			double ratio = 1 - (idx/blendLen);
//			Motion m = motionList.get(blendStart + idx);
////			System.out.println("blend : " + (blendStart + idx) + " : " + ratio);
//			for (int j = 0; j < displacementList.length; j++) {
//				if (displacementList[j] == null) continue;
//				AxisAngle4d a = new AxisAngle4d(displacementList[j]);
//				a.angle *= ratio;
//				Matrix4d d = new Matrix4d();
//				d.set(a);
//				m.transforms[j].mul(d);
//			}
//			Vector3d translation = new Vector3d(dv);
//			translation.scale(ratio);
//			translation.add(MathUtil.getTranslation(m.root()));
//			m.root().setTranslation(translation);
//		}
		
//		for (int idx = 0; idx < Configuration.BLEND_MARGIN; idx++) {
//			double ratio = 1 - (idx)/(double)(Configuration.BLEND_MARGIN);
////			Vector3d translation = MathUtil.getTranslation(aligned[idx].root());
//			for (int j = 0; j < displacementList.length; j++) {
//				if (displacementList[j] == null) continue;
//				AxisAngle4d a = new AxisAngle4d(displacementList[j]);
//				a.angle *= ratio;
//				Matrix4d d = new Matrix4d();
//				d.set(a);
//				aligned[idx].transforms[j].mul(d);
//			}
//			Vector3d translation = new Vector3d(dv);
//			translation.scale(ratio);
//			translation.add(MathUtil.getTranslation(aligned[idx].root()));
//			aligned[idx].root().setTranslation(translation);
//		}
	}
}
