package mrl.jni.motion;

import mrl.motion.data.Motion;
import mrl.util.MathUtil;

public class Constraint {
	public Motion[] originMotions;
	public int[] constraintFrames;
	public int[] constraintPersons;
	public double[] posConstraint;
	
	public double[] diffFromOriginInMotion;
	public double[] diffFromOriginInTime;
	
	
	public Constraint(){
	}
	
	public Constraint(Constraint copy){
		originMotions = new Motion[copy.originMotions.length];
		for (int i = 0; i < originMotions.length; i++) {
			originMotions[i] = copy.originMotions[i];
		}
		constraintFrames = MathUtil.copy(copy.constraintFrames);
		constraintPersons = MathUtil.copy(copy.constraintPersons);
		posConstraint = MathUtil.copy(copy.posConstraint);
		diffFromOriginInMotion = MathUtil.copy(copy.diffFromOriginInMotion);
		diffFromOriginInTime = MathUtil.copy(copy.diffFromOriginInTime);
	}
}
