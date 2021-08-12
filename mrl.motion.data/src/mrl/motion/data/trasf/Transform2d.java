package mrl.motion.data.trasf;

import javax.vecmath.Vector2d;

public class Transform2d {

	public Vector2d translation;
	public double rotateAngle;
	
	public Transform2d(Vector2d translation, double rotateAngle) {
		this.translation = translation;
		this.rotateAngle = rotateAngle;
	}
}
