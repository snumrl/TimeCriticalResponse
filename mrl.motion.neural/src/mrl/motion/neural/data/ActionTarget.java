package mrl.motion.neural.data;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;

public class ActionTarget{
	public MotionAnnotation ann;
	public int cIndex;
	public int mIndex;
	
	public int prevMargin = -1;
	public int postMargin = -1;
	
	public ActionTarget(MotionAnnotation ann, int cIndex, int mIndex) {
		this.ann = ann;
		this.cIndex = cIndex;
		this.mIndex = mIndex;
	}
	
	public Pose2d getInteractionPose(ArrayList<Motion> generatedMotion, MDatabase database){
		Motion originMotion = database.findMotion(ann.file, ann.interactionFrame);
		Motion targetMotion = database.findMotion(ann.getOppositeFile(), ann.interactionFrame);
		
		Matrix4d t1 = originMotion.root();
		Matrix4d t2 = targetMotion.root();
		
		Vector3d p1 = MathUtil.getTranslation(t1);
		Vector3d p2 = MathUtil.getTranslation(t2);
		
		Vector2d v = new Vector2d(p2.x - p1.x, p2.z - p1.z);
		double angleOffset = Math.atan2(-v.y, v.x);
		if (Double.isNaN(angleOffset)) throw new RuntimeException();
		
		
		Matrix4d m = new Matrix4d();
		m.rotY(angleOffset);
		Vector3d vX = new Vector3d(1, 0, 0);
		m.transform(vX);
		Vector3d interactionDirection = new Vector3d(vX);
		Point3d interactionPosition = new Point3d(MathUtil.getTranslation(originMotion.root()));
		interactionPosition.y = 0;
		if (Double.isNaN(interactionDirection.x)){
			System.out.println("invalid clip :: " + ann);
			throw new RuntimeException();
		}
		Pose2d interactionPose = new Pose2d(interactionPosition, interactionDirection);
		Pose2d iMotionPose = PositionMotion.getPose(originMotion);
		Pose2d relativePose = Pose2d.relativePose(iMotionPose, interactionPose);
		
		Motion gMotion = generatedMotion.get(mIndex);
		if (Math.abs(gMotion.motionIndex - originMotion.motionIndex) >= 2) {
			System.out.println("not match :: " + gMotion + " : " + originMotion);
			throw new RuntimeException();
		}
		return PositionMotion.getPose(gMotion).localToGlobal(relativePose);
	}
	
	private static int maxMatchLength = -1;
	public static int[] maxMatchClip = null;
	public static ArrayList<ActionTarget> getActionTargets(MotionSegment segment, ArrayList<MotionAnnotation> interactionAnns, MDatabase database){
		ArrayList<Motion> mList = segment.getMotionList();
		ArrayList<ActionTarget> targetList = new ArrayList<ActionTarget>();
		
		HashMap<Integer, MotionAnnotation> annMap = new HashMap<Integer, MotionAnnotation>();
		for (MotionAnnotation ann : interactionAnns) {
			if (ann.interactionFrame > 0){
				Motion interactionMotion = database.findMotion(ann.file, ann.interactionFrame);
				annMap.put(interactionMotion.motionIndex, ann);
				annMap.put(interactionMotion.motionIndex+1, ann);
			}
		}
		ArrayList<MotionAnnotation> annList = new ArrayList<MotionAnnotation>();
		for (int i = 0; i < mList.size(); i++) {
			MotionAnnotation ann = annMap.get(mList.get(i).motionIndex);
			if (ann != null) {
//				System.out.println("annlist :: " + i + " : " + ann);
				annList.add(ann);
				i += 4;
			}
		}
		
		int mIndex = 0;
		int aIndex = 0;
		while (true){
			MotionAnnotation interactionAnn = null;
			for (; aIndex < annList.size(); aIndex++) {
				if (annList.get(aIndex).interactionFrame > 0){
					interactionAnn = annList.get(aIndex);
					break;
				}
			}
			if (interactionAnn == null) break;
//			System.out.println(mIndex + " : " + interactionAnn);
			Motion interactionMotion = database.findMotion(interactionAnn.file, interactionAnn.interactionFrame);
			int iMotionIndex = interactionMotion.motionIndex;
			int matchIndex = -1;
			int startMIndex = mIndex;
			for (; mIndex < mList.size(); mIndex++) {
				if (mList.get(mIndex).motionIndex == iMotionIndex || mList.get(mIndex).motionIndex == (iMotionIndex+1)){
					matchIndex = mIndex;
					break;
				}
			}
			if (matchIndex - startMIndex > maxMatchLength) {
				maxMatchLength = matchIndex - startMIndex;
				maxMatchClip = new int[] { startMIndex, matchIndex };
			}
			if (matchIndex < 0){
				System.out.println("no matcing :: " + aIndex+ " : " + annList.get(aIndex) + " : " + iMotionIndex + " : " + startMIndex);
				throw new RuntimeException();
			}
			
			ActionTarget target = new ActionTarget(interactionAnn, aIndex, mIndex);
			targetList.add(target);
			aIndex++;
			mIndex+=4;
		}
		System.out.println("maxMatchLength :: " + maxMatchLength);
		return targetList;
	}
}