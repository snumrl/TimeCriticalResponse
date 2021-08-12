package mrl.util;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

public class Matrix2dTest {

	public static void main(String[] args) {
//		Matrix2d m = new Matrix2d();
//		m.set(Math.PI/2, 1, -1);
//		
//		Point2d p = new Point2d(1, 0);
//		m.transform(p);
//		System.out.println(p);
//		
//		
//		Matrix2d m2 = new Matrix2d(m);
//		m2.invert();
//		
//		m2.mul(m2, m);
//		
//		p = new Point2d(3, 0);
//		m2.transform(p);
//		System.out.println(p);
//		System.out.println(m2);
		
		{
			Matrix2d m1 = new Matrix2d(Math.PI/6, 1, 0);
			Matrix2d m2 = new Matrix2d(Math.PI/6, 0, 1);
//			m2.invert();
			Matrix2d m3 = new Matrix2d();
			m3.mul(m2, m1);
			System.out.println(m3.getTranslation());
			System.out.println(Math.toDegrees(m3.getAngle()));
		}
		{
			Matrix4d m1 = new Matrix4d();
			m1.rotZ(Math.PI/6);
			m1.setTranslation(new Vector3d(1, 0, 0));
			Matrix4d m2 = new Matrix4d();
			m2.rotZ(Math.PI/6);
			m2.setTranslation(new Vector3d(0, 1, 0));
//			m2.invert();
			Matrix4d m3 = new Matrix4d();
			m3.mul(m2, m1);
			
			System.out.println(MathUtil.getTranslation(m3));
			AxisAngle4d a = new AxisAngle4d();
			a.set(m3);
			System.out.println(a.angle);
		}
		
		
	}
}
