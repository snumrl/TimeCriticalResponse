package mrl.motion.position;

import static mrl.motion.data.trasf.Pose2d.Hips;
import static mrl.motion.data.trasf.Pose2d.LeftUpLeg;
import static mrl.motion.data.trasf.Pose2d.RightUpLeg;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.trasf.Pose2d;
import mrl.util.MathUtil;

public class PositionMotion {
	
	public Motion motion;
	public HashMap<String, Point3d> pointData;
	public Pose2d pose;

	public PositionMotion(Motion motion) {
		this.motion = motion;
		
		pointData = Motion.getPointData(SkeletonData.instance, motion);
//		if (mirror){
//			HashMap<String, Point3d> mirrored = new HashMap<String, Point3d>();
//			String[][] leftToRight = {
//					{ "Left", "Right" },
//					{ "LHip", "RHip" },
//					{ "LThumb", "RThumb" }
//			};
//			for (Entry<String, Point3d> entry : pointData.entrySet()){
//				String key = entry.getKey();
//				for (String[] pair :  leftToRight){
//					if (key.contains(pair[0])){
//						key = key.replace(pair[0], pair[1]);
//					} else if (key.contains(pair[1])){
//						key = key.replace(pair[1], pair[0]);
//					}
//				}
//				mirrored.put(key, entry.getValue());
//			}
//			pointData = mirrored;
//		}
//		Vector3d legVector = MathUtil.sub(pointData.get(LeftUpLeg), pointData.get(RightUpLeg));
//		Vector3d upVector = new Vector3d(0, 1, 0);
//		Vector3d cross = MathUtil.cross(legVector, upVector);
//		cross.y = 0;
//		cross.normalize();
//		Point3d root = pointData.get(Hips);
//		pose = new Pose2d(root, cross);
		
		pose = getPose(motion);
		
		Matrix4d transform = Pose2d.globalTransform(pose, Pose2d.BASE).to3d();
		for (Point3d p : pointData.values()){
			transform.transform(p);
		}
//		System.out.println("################ ");
//		for (String key : SkeletonData.keyList){
//			System.out.println(key + "\t" + pointData.get(key));
//		}
	}
	
	public static Pose2d getPose(Motion motion){
		return Pose2d.getPose(motion);
	}
	
	public static Motion getAlignedMotion(Motion motion){
		return Pose2d.getAlignedMotion(motion);
	}
	
	public static ArrayList<Motion> getAlignedMotion(ArrayList<Motion> motionList, int offset){
		return Pose2d.getAlignedMotion(motionList, offset);
	}
	
}
