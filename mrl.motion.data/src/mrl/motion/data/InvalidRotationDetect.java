package mrl.motion.data;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import mrl.util.MathUtil;

public class InvalidRotationDetect {

	private MotionData motionData;

	public InvalidRotationDetect(MotionData motionData) {
		this.motionData = motionData;
	}
	
	public boolean[] checkAll(boolean print){
		boolean[] isInvalid = new boolean[motionData.motionList.size()];
		for (int i = 0; i < isInvalid.length; i++) {
			isInvalid[i] = checkLeg("Left", i) || checkLeg("Right", i) || checkShoulder("Left", i) || checkShoulder("Right", i) || checkHead(i);
//			System.out.println(i + "\t" + isInvalid[i]);
		}
		if (print) printInvalid(motionData, isInvalid);
		return isInvalid;
	}
	
	public static void adjust(boolean[] isInvalid, int margin){
		while (true){
			boolean isChanged = false;
			for (int i = margin; i < isInvalid.length-margin; i++) {
				if (isInvalid[i]) continue;
				boolean prev = false;
				boolean next = false;
				for (int j = 1; j <= margin; j++) {
					prev |= isInvalid[i-j];
					next |= isInvalid[i+j];
				}
				
				if (prev && next){
					isInvalid[i] = true;
					isChanged = true;
				}
			}
			if (!isChanged) break;
		}
		
		
		int start = -1;
		for (int i = 0; i < isInvalid.length; i++) {
			if (i < isInvalid.length -1 && isInvalid[i]){
				if (start < 0){
					start = i;
				}
			} else {
				if (start >= 0){
					if (i - start >= margin){
					} else {
						for (int j = start; j < i; j++) {
							isInvalid[j] = false;
						}
					}
					start = -1;
				}
			}
		}
	}
	
	static void printInvalid(MotionData motionData, boolean[] isInvalid){
		int margin = 20;
		adjust(isInvalid, margin);
		
		int start = -1;
		for (int i = 0; i < isInvalid.length; i++) {
			if (i < isInvalid.length -1 && isInvalid[i]){
				if (start < 0){
					start = i;
				}
			} else {
				if (start >= 0){
					if (i - start >= margin){
						System.out.println(motionData.file.getName() + "\t" + (start+1) + "\t" + (i-1+1));
					}
					start = -1;
				}
			}
		}
	}
	
	public boolean checkShoulder(String direction, int frame){
		if (motionData.file.getName().equals("a_007_5_2.bvh")){
            if (frame >= 1530 && frame <= 1650) return false;
		}
		
		Motion motion = motionData.motionList.get(frame);
		
		String[] headers = new String[]{ "Shoulder", "Arm" };
		
		Matrix4d t = new Matrix4d(motion.get(direction+ headers[1]));
		
		Vector3d v1 = new Vector3d(1, 0, 0);
		Vector3d v2 = new Vector3d(1, 0, 0);
		t.transform(v2);
		AxisAngle4d a = new AxisAngle4d();
		a.set(MathUtil.cross(v1, v2), v1.angle(v2));
		Matrix4d t2 = new Matrix4d();
		t2.set(a);
		t2.transform(v1);
		
		t.invert();
		t.mul(t2);
		
		
		AxisAngle4d axisAngle = new AxisAngle4d();
		axisAngle.set(t);
		
		double angle = Math.toDegrees(axisAngle.angle);
		
		return (angle > 100 );
		
	}

	public boolean checkLeg(String direction, int frame){
		Motion motion = motionData.motionList.get(frame);
		
		String[] headers = new String[]{ "Leg", "Foot" };
		
		Matrix4d t = new Matrix4d(motion.get(direction+ headers[1]));
		
		Vector3d v = new Vector3d(1, 0, 0);
		t.transform(v);
		
		
		AxisAngle4d axisAngle = new AxisAngle4d();
		axisAngle.set(t);
		
		double angle = Math.toDegrees(axisAngle.angle);
		
		return (angle > 100 ) || ((angle > 40 && (v.z*100) > 50) && MathUtil.getTranslation(motion.root()).y > 45);
	}
	
	public boolean checkHead(int frame){
		Motion motion = motionData.motionList.get(frame);
		
		String[] headers = new String[]{ "Neck", "Head" };
		
		Matrix4d t = new Matrix4d(motion.get(headers[1]));
		
		Vector3d v1 = new Vector3d(1, 0, 0);
		Vector3d v2 = new Vector3d(1, 0, 0);
		t.transform(v2);
		AxisAngle4d a = new AxisAngle4d();
		a.set(MathUtil.cross(v1, v2), v1.angle(v2));
		Matrix4d t2 = new Matrix4d();
		t2.set(a);
		t2.transform(v1);
		
		t.invert();
		t.mul(t2);
		
		
		AxisAngle4d axisAngle = new AxisAngle4d();
		axisAngle.set(t);
		
		double angle = Math.toDegrees(axisAngle.angle);
		
		return (angle > 90 );
		
	}
	
}
