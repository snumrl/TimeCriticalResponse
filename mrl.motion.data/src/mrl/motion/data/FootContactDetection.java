package mrl.motion.data;

import java.lang.reflect.Field;
import java.util.HashMap;

import javax.vecmath.Point3d;

import mrl.motion.data.parser.BVHParser;

public class FootContactDetection {
	
	public static int CONTACT_MARGIN = 0;
	
	public static int[] footDistPointOffset = {
			0, 20, 0
	};
	public static String[] headJoints = {
		"Head", "Head"+BVHParser.END_POSTFIX
	};
	public static String[] leftFootJoints = {
		"LeftFoot", "LeftToe", "LeftToe_End"
	};
	public static String[] rightFootJoints = {
		"RightFoot", "RightToe", "RightToe_End"
	};
	public static double[] heightLimit = {
//		12, 6, 6
		16, 6, 6
//		16, 11, 9
	};
	public static double[] velocityLimit = {
		2, 2, 2
//		3, 3, 3
	};
	
	public static boolean isHead(String joint){
		return contains(joint, headJoints);
	}
	public static boolean isLeftFoot(String joint){
		return contains(joint, leftFootJoints);
	}
	public static boolean isRightFoot(String joint){
		return contains(joint, rightFootJoints);
	}
	
	private static boolean contains(String check, String[] list){
		for (String j : list) {
			if (j.equals(check)) return true;
		}
		return false;
	}
	
	private static boolean isInitialized = false;
	
	private static void initialize(MotionData motionData){
		isInitialized = true;
		SkeletonData skeleton = motionData.skeletonData;
		if (!skeleton.containsKey(leftFootJoints[2])){
			leftFootJoints[2] = "LeftToe" + BVHParser.END_POSTFIX;
			rightFootJoints[2] = "RightToe" + BVHParser.END_POSTFIX;
		}
	}
	
	private static double distance(Point3d p1, Point3d p2){
		double dx, dz;
	    dx = p2.x-p1.x;
	    dz = p2.z-p1.z;
	    return Math.sqrt(dx*dx+dz*dz);
	}
	
	public static void checkFootContact(MotionData motionData){
		if (!isInitialized){
			initialize(motionData);
		}
		
		int n = motionData.motionList.size();
		Point3d[][] leftFootPositions = new Point3d[n][leftFootJoints.length];
		Point3d[][] rightFootPositions = new Point3d[n][rightFootJoints.length];
		for (int i = 0; i < n; i++) {
			if (motionData.motionList.get(i) == null) continue;
			HashMap<String, Point3d> pointData = Motion.getPointData(motionData.skeletonData, motionData.motionList.get(i));
			for (int j = 0; j < leftFootJoints.length; j++) {
				leftFootPositions[i][j] = pointData.get(leftFootJoints[j]);
			}
			for (int j = 0; j < rightFootJoints.length; j++) {
				rightFootPositions[i][j] = pointData.get(rightFootJoints[j]);
			}
		}
		
		for (int i = 0; i < n; i++) {
			Motion motion = motionData.motionList.get(i);
			if (motion == null) continue;
			try{
				double[] leftVelocity = new double[leftFootJoints.length];
				for (int j = 0; j < leftFootJoints.length; j++) {
					if (i == 0){
						leftVelocity[j] = distance(leftFootPositions[i][j], leftFootPositions[i+1][j]);
					} else if (i == n - 1){
						leftVelocity[j] = distance(leftFootPositions[i][j], leftFootPositions[i-1][j]);
					} else {
						leftVelocity[j] = distance(leftFootPositions[i-1][j], leftFootPositions[i+1][j]);
					}
				}
				motion.leftFootContact = new boolean[leftFootJoints.length];
				for (int j = 0; j < leftFootJoints.length; j++) {
					if (leftFootPositions[i][j].y < heightLimit[j] && leftVelocity[j] < velocityLimit[j]){
						motion.isLeftFootContact = true;
						motion.leftFootContact[j] = true;
//						break;
					}
				}
				
				double[] rightVelocity = new double[rightFootJoints.length];
				for (int j = 0; j < rightFootJoints.length; j++) {
					if (i == 0){
						rightVelocity[j] = distance(rightFootPositions[i][j], rightFootPositions[i+1][j]);
					} else if (i == n - 1){
						rightVelocity[j] = distance(rightFootPositions[i][j], rightFootPositions[i-1][j]);
					} else {
						rightVelocity[j] = distance(rightFootPositions[i-1][j], rightFootPositions[i+1][j]);
					}
				}
				motion.rightFootContact = new boolean[rightFootJoints.length];
				for (int j = 0; j < rightFootJoints.length; j++) {
					if (rightFootPositions[i][j].y < heightLimit[j] && rightVelocity[j] < velocityLimit[j]){
						motion.isRightFootContact = true;
						motion.rightFootContact[j] = true;
//						break;
					}
				}
			} catch (NullPointerException e){
				
			}
		}
		
		ContactWrapper[] wrapperList = {
				new ContactWrapper("isLeftFootContact", -1),
				new ContactWrapper("isRightFootContact", -1),
				new ContactWrapper("leftFootContact", 0),
				new ContactWrapper("leftFootContact", 1),
				new ContactWrapper("leftFootContact", 2),
				new ContactWrapper("rightFootContact", 0),
				new ContactWrapper("rightFootContact", 1),
				new ContactWrapper("rightFootContact", 2),
		};
		
		for (ContactWrapper w : wrapperList){
			for (int cIdx = 0; cIdx < CONTACT_MARGIN; cIdx++) {
				for (int i = 1; i < n; i++) {
					Motion prev = motionData.motionList.get(i-1);
					Motion motion = motionData.motionList.get(i);
					if (w.getContact(prev) && !w.getContact(motion)){
						w.setContact(prev, false);
					}
				}
				for (int i = n-1; i > 0; i--) {
					Motion prev = motionData.motionList.get(i-1);
					Motion motion = motionData.motionList.get(i);
					if (!w.getContact(prev) && w.getContact(motion)){
						w.setContact(motion, false);
					}
				}
			}
			
			for (int i = 1; i < n-2; i++) {
				try{
					Motion prev = motionData.motionList.get(i-1);
					Motion motion = motionData.motionList.get(i);
					Motion next = motionData.motionList.get(i+1);
					
					boolean cMotion = w.getContact(motion);
					boolean cPrev = w.getContact(prev);
					boolean cNext = w.getContact(next);
					
					if (cMotion != cPrev && cMotion != cNext){
						w.setContact(motion, cPrev);
					}
					
					Motion nextNext = motionData.motionList.get(i+2);
					if (cMotion != cPrev && cMotion != w.getContact(nextNext)){
						motion.isLeftFootContact = prev.isLeftFootContact;
						w.setContact(motion, cPrev);
						w.setContact(next, cPrev);
					}
				} catch (NullPointerException e){
				}
			}
		}
		
//		if (motionData.file.getName().contains("s_009_1_1")){
//			for (int i = 995-120; i <= 995+20; i++) {
////				Motion motion = motionData.motionList.get(i);
////				System.out.println(i + " : " + motion.isLeftFootContact);
//				for (int j = 0; j < leftFootJoints.length; j++) {
//					double vel = distance(leftFootPositions[i][j], leftFootPositions[i+1][j]);
//					System.out.println(i + " : " + leftFootJoints[j] + " : " + leftFootPositions[i][j].y + " : " + vel);
//				}
////				for (int j = 0; j < rightFootJoints.length; j++) {
////					double vel = distance(rightFootPositions[i][j], rightFootPositions[i+1][j]);
////					System.out.println(i + " : " + rightFootJoints[j] + " : " + rightFootPositions[i][j].y + " : " + vel);
////				}
//			}
//		}
	}
	
	private static class ContactWrapper{
		private String label;
		private int index;
		private Field field;
		
		public ContactWrapper(String label, int index) {
			this.label = label;
			this.index = index;
			
			try {
				field = Motion.class.getField(label);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		public boolean getContact(Motion motion){
			try {
				Object value = field.get(motion);
				if (index < 0) return (Boolean)value;
				if (value == null) return false;
				return ((boolean[])value)[index];
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		public void setContact(Motion motion, boolean c){
			try {
				if (index < 0){
					field.set(motion, c);
				} else {
					boolean[] value = (boolean[])field.get(motion);
					if (value == null) return;
					value[index] = c;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
//	private static interface ContactWrapper{
//		public boolean getContact(Motion motion);
//		public void setContact(Motion motion, boolean c);
//	}
}
