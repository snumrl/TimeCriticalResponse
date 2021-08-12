package mrl.motion.data.edit;

import java.util.ArrayList;
import java.util.Random;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class TrajectoryEdit {
	
	public static int CUT_MIN = 45;
	public static int CUT_MAX = 90;
	
	private Random rand = MathUtil.random;
	public ArrayList<MotionSegment> originList = new ArrayList<MotionSegment>();
	public ArrayList<MotionSegment> editedList = new ArrayList<MotionSegment>();
	public MotionSegment origin;
	public MotionSegment edited;

	public double lengthOffset;
	public double angleOffset;
	public double timeOffset;
	
	public double[][] preOffset;
	
	public TrajectoryEdit(){
		this(0.3, 45, 0.2);
	}
	
	public TrajectoryEdit(double lengthOffset, double angleOffset, double timeOffset) {
		this.lengthOffset = lengthOffset;
		this.angleOffset = angleOffset;
		this.timeOffset = timeOffset;
	}


	public MotionSegment edit(MotionSegment totalTrajectory){
		MDatabase.applyDistortionForEdit(totalTrajectory.getEntireMotion());
		originList.clear();
		editedList.clear();
		
		MotionSegment edited = null;
		int maxLen = totalTrajectory.length();
		
		int current = 0;
		int iter = 0;
		int editFinishMargin = 0;
		while (true){
			int cut = CUT_MIN + rand.nextInt(CUT_MAX - CUT_MIN);
			if (preOffset != null){
//				int div = 15;
				int div = 9;
				cut = cut - cut%div - 1;
				if (cut < CUT_MIN){
					cut += 9;
				}
			}
			int idx2 = current + cut;
			if (idx2 >= maxLen-editFinishMargin) idx2 = maxLen-editFinishMargin-1;
			MotionSegment toAttach = new MotionSegment(totalTrajectory, current, idx2);
			if (preOffset != null) originList.add(new MotionSegment(toAttach));
			toAttach = editUnitSegment(toAttach, iter);
			iter++;
			if (preOffset != null) editedList.add(new MotionSegment(toAttach));
			edited = MotionSegment.stitch(edited, new MotionSegment(toAttach), true);
			current = idx2+1;
			System.out.println("#########  EDIT PROGRESS  ########## " + current + " // " + maxLen);
			if (current >= maxLen - editFinishMargin){
				break;
			}
		}
		if (preOffset != null){
			this.edited = new MotionSegment(edited);
			this.origin = new MotionSegment(totalTrajectory);
		}
		return edited;
	}
	
	
	public MotionSegment editUnitSegment(MotionSegment segment, int iter){
		segment =new MotionSegment(segment);
		Pose2d start = PositionMotion.getPose(segment.firstMotion());
		Pose2d end = PositionMotion.getPose(segment.lastMotion());
		
		Pose2d pose = Pose2d.relativePose(start, end);
		
		double lengthRatio = 1 - lengthOffset + rand.nextDouble()*lengthOffset;
		double rotAngle = Utils.rand1()*Math.toRadians(angleOffset);
		double rTime = Utils.rand1()*timeOffset;
		if (preOffset != null && iter < preOffset.length){
			lengthRatio = preOffset[iter][0];
			rotAngle = Math.toRadians(preOffset[iter][1]);
			rTime = preOffset[iter][2];
		}
		pose.position.scale(lengthRatio);
		pose.transformGlobal(new Matrix2d(rotAngle));
		pose.direction = MathUtil.rotate(pose.direction, rotAngle*0.5);
		
		double time = segment.length() - 1;
		time *= 1 + rTime;
		MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, pose, (int)Math.round(time));
		for (Motion m : edited.getEntireMotion()) {
			if (Double.isNaN(m.root().m00)) return segment;
		}
		return edited;
	}
}
