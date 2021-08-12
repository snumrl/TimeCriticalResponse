package mrl.motion.viewer;

import java.util.HashMap;

import javax.media.opengl.GL;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.HumanInertia;
import mrl.motion.data.Motion;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.SkeletonData.Joint;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.viewer.module.Pose2dItemDrawer;

import org.eclipse.swt.widgets.Composite;

import com.sun.opengl.util.GLUT;

public abstract class MotionAnimator extends SWTViewableCanvas{
	
	public static boolean drawAxis = false;
	public static boolean drawDarkBorder = false;
	public static boolean showFootContact = false;
	public static boolean showRootDirection = false;
	
	protected Vector3d skeletonColor = new Vector3d(0.8, 0.8, 0.8);
//	protected Vector3d skeletonColor = new Vector3d(0.9, 0.4, 0.4);
	protected Vector3d footColor = null;
//	protected Vector3d footColor = new Vector3d(0.4, 0.9, 0.4);
	protected Vector3d headColor = null;
	
	protected Point3d extraPoint;
	protected boolean showExtraPoint = true;
	
	protected double boneThickness = 0.65;
	protected Vector3d clearColor = new Vector3d(1, 1, 1);
	
	protected int animationIndex;
	
	protected boolean drawBox = false;
	

	public MotionAnimator(Composite parent) {
		super(parent);
	}
	
	public void setClearColor(Vector3d clearColor) {
		this.clearColor = clearColor;
	}

	public void setDrawBox(boolean drawBox) {
		this.drawBox = drawBox;
	}

	public void setBoneThickness(double boneThickness) {
		this.boneThickness = boneThickness;
	}

	public Vector3d getSkeletonColor() {
		return skeletonColor;
	}

	public void setSkeletonColor(Vector3d skeletonColor) {
		this.skeletonColor = skeletonColor;
	}

	public void setExtraPoint(Point3d extraPoint) {
		this.extraPoint = extraPoint;
	}
	
	public int getAnimationIndex() {
		return animationIndex;
	}

	public void setAnimationIndex(int animationIndex) {
		this.animationIndex = animationIndex;
	}

	@Override
	protected void drawObjects() {
		drawObjectsImpl();
	}
	
	protected abstract void drawObjectsImpl();
	
	
	protected void drawMotionByBox(SkeletonData skeletonData, Motion motion){
		HashMap<String, Matrix4d> transMap = Motion.getTransformData(skeletonData, motion);
		for (Joint joint : skeletonData.values()){
			if (joint.parent == null) continue;
			Matrix4d m = transMap.get(joint.parent.name);
			if (m == null) continue;
			gl.glPushMatrix();
			
			boolean isRoot = joint == skeletonData.root;
			gl.glMultMatrixd(new double[]{
					m.m00, m.m10, m.m20, m.m30,
					m.m01, m.m11, m.m21, m.m31,
					m.m02, m.m12, m.m22, m.m32,
					m.m03, m.m13, m.m23, m.m33,
			}, 0);
			
			boolean pass = (isRoot && joint.length > 0) || (joint.name.equals("pelvis") && joint.length > 0) ;
			pass |= HumanInertia.isPass(joint.name);
			if (!pass && (joint.length != 0)){
				HumanInertia inertia = HumanInertia.get(joint.parent.name);
				if (inertia != null && inertia.mass > 0){
					Point3d t = new Point3d(joint.transition);
					t.scale(0.5);
					Matrix4d rotMatrix = new Matrix4d();
					rotMatrix.set(inertia.rotation);
					
					if (joint.name.equals("Head")){
//						rotMatrix.setIdentity();
//						gl.glTranslated(t.x, t.y, t.z);
						t.scale(0);
//						t.scale(-1);
						rotMatrix.invert();
//						rotMatrix.rotZ(Math.toRadians(90));
					} else if (joint.parent.name.equals("Spine1")){
						t.scale(0.7/0.5);
					}
					gl.glTranslated(t.x, t.y, t.z);
					
					gl.glMultMatrixd(new double[]{
							rotMatrix.m00, rotMatrix.m10, rotMatrix.m20, rotMatrix.m30,
							rotMatrix.m01, rotMatrix.m11, rotMatrix.m21, rotMatrix.m31,
							rotMatrix.m02, rotMatrix.m12, rotMatrix.m22, rotMatrix.m32,
							rotMatrix.m03, rotMatrix.m13, rotMatrix.m23, rotMatrix.m33,
					}, 0);
					Vector3d s = inertia.inertiaSize;
					gl.glScaled(s.x,s.y,s.z);
					glut.glutSolidCube(1);
					
					if (!isShadowMode){
						if (drawDarkBorder){
							gl.glDisable(GL.GL_LIGHTING);
							gl.glDisable(GL.GL_BLEND);
							gl.glLineWidth(0.5f);
							gl.glColor3d(0.2, 0.2, 0.2);
							glut.glutWireCube(1.01f);
							gl.glColor3d(0.85, 0.85, 0.85);
							gl.glEnable(GL.GL_LIGHTING);
							gl.glEnable(GL.GL_BLEND);
						} else {
							gl.glLineWidth(2);
							gl.glDisable(GL.GL_LIGHTING);
							glut.glutWireCube(1.01f);
							gl.glEnable(GL.GL_LIGHTING);
						}
					}
				}
			}
			
			gl.glPopMatrix();
		}
	}
	
	public void drawMotion(SkeletonData skeletonData, Motion motion, boolean drawShadow){
		if (motion == null) return;
		
		if (drawBox){
			if (drawShadow){
				setupShadow();
				drawMotionByBox(skeletonData, motion);
				unsetupShadow();
				gl.glColor4d(skeletonColor.x, skeletonColor.y, skeletonColor.z, 1);
				drawMotionByBox(skeletonData, motion);
			} else {
				gl.glColor4d(skeletonColor.x, skeletonColor.y, skeletonColor.z, 1);
				drawMotionByBox(skeletonData, motion);
			}
		} else {
			if (drawShadow){
				setupShadow();
				drawBone(skeletonData.root, motion, true);
				unsetupShadow();
				gl.glColor4d(skeletonColor.x, skeletonColor.y, skeletonColor.z, 1);
				drawBone(skeletonData.root, motion, true);
			} else {
				gl.glColor4d(skeletonColor.x, skeletonColor.y, skeletonColor.z, 1);
				drawBone(skeletonData.root, motion, true);
			}
		}
		
		if (showFootContact) {
			drawFootContact(gl, glut, skeletonData, motion, boneThickness*4);
		}
		if (showRootDirection) {
			Pose2d p = Pose2d.getPose(motion);
			Pose2dItemDrawer.draw(gl, glu, glut, p, 1, new Vector3d(0.2, 0.8, 0.2));
		}
	}
	
	public void drawBone(Joint joint, Motion motion, boolean isRoot){
		drawBone(gl, glut, boneThickness*4, joint, motion, isRoot);
//		gl.glPushMatrix();
//		
//		Matrix4d m = motion.get(joint.name);
//		
//		if (motion.isRotateFirst && m != null){
//			gl.glMultMatrixd(new double[]{
//					m.m00, m.m10, m.m20, m.m30,
//					m.m01, m.m11, m.m21, m.m31,
//					m.m02, m.m12, m.m22, m.m32,
//					m.m03, m.m13, m.m23, m.m33,
//			}, 0);
//		}
//		
//		boolean pass = (isRoot && joint.length > 0) || (joint.name.equals("pelvis") && joint.length > 0) ;
//		if (!pass && (joint.length != 0)){
//			Point3d b0 = new Point3d();
//			Point3d b1 = new Point3d(joint.transition);
//			drawLine(b0, b1, boneThickness);
//			gl.glTranslated(joint.transition.x, joint.transition.y, joint.transition.z);
//			glut.glutSolidSphere((boneThickness + 0.05)/scale, 20, 20);
//		}
//		
//		if (!motion.isRotateFirst && m != null){
//			if (isRoot || joint.name.equals("pelvis")){
//				gl.glMultMatrixd(new double[]{
//						m.m00, m.m10, m.m20, m.m30,
//						m.m01, m.m11, m.m21, m.m31,
//						m.m02, m.m12, m.m22, m.m32,
//						m.m03, m.m13, m.m23, m.m33,
//				}, 0);
//			} else {
//				gl.glMultMatrixd(new double[]{
//						m.m00, m.m10, m.m20, m.m30,
//						m.m01, m.m11, m.m21, m.m31,
//						m.m02, m.m12, m.m22, m.m32,
//						0, 0, 0, m.m33,
//				}, 0);
//			}
//		}
//		
//		for (Joint child : joint.children){
//			drawBone(child, motion, false);
//		}
//		gl.glPopMatrix();
//		
	}
	
	public static void drawFootContact(GL gl, GLUT glut, SkeletonData skeletonData, Motion motion, double boneThickness) {
		gl.glColor3d(0.2, 0.8, 0.2);
		HashMap<String, Point3d> pointData = Motion.getPointData(skeletonData, motion);
		if (motion.leftFootContact != null){
			for (int i = 0; i < motion.leftFootContact.length; i++) {
				if (motion.leftFootContact[i]){
					Point3d p = pointData.get(FootContactDetection.leftFootJoints[i]);
					SWTViewableCanvas.drawSphere(gl, glut, p, boneThickness + 2);
				}
			}
			for (int i = 0; i < motion.rightFootContact.length; i++) {
				if (motion.rightFootContact[i]){
					Point3d p = pointData.get(FootContactDetection.rightFootJoints[i]);
					SWTViewableCanvas.drawSphere(gl, glut, p, boneThickness + 2);
				}
			}
		}
	}
	
	public static void drawBone(GL gl, GLUT glut, double boneThickness, Joint joint, Motion motion, boolean isRoot){
		gl.glPushMatrix();
		
		Matrix4d m = motion.get(joint.name);
		
		if (motion.isRotateFirst && m != null){
			gl.glMultMatrixd(new double[]{
					m.m00, m.m10, m.m20, m.m30,
					m.m01, m.m11, m.m21, m.m31,
					m.m02, m.m12, m.m22, m.m32,
					m.m03, m.m13, m.m23, m.m33,
			}, 0);
		}
		
		boolean pass = (isRoot && joint.length > 0) || (joint.name.equals("pelvis") && joint.length > 0) ;
		if (!pass && (joint.length != 0)){
			Point3d b0 = new Point3d();
			Point3d b1 = new Point3d(joint.transition);
			SWTViewableCanvas.drawLine(gl, glut, b0, b1, boneThickness);
			gl.glTranslated(joint.transition.x, joint.transition.y, joint.transition.z);
			glut.glutSolidSphere((boneThickness + 0.05), SLICE_AND_STACK, SLICE_AND_STACK);
			
			if (drawAxis){
				double t = boneThickness/4;
				Vector3d[] vList = new Vector3d[]{
					new Vector3d(1, 0, 0),	
					new Vector3d(0, 1, 0),	
					new Vector3d(0, 0, 1),	
				};
				for (Vector3d v : vList){
					gl.glColor3d(v.x, v.y, v.z);
					v.scale(10);
					SWTViewableCanvas.drawLine(gl, glut, new Point3d(), new Point3d(v), t);
				}
				gl.glColor3d(0.8, 0.8, 0.8);
			}
		}
		
		if (!motion.isRotateFirst && m != null){
			if (isRoot || joint.name.equals("pelvis")){
				gl.glMultMatrixd(new double[]{
						m.m00, m.m10, m.m20, m.m30,
						m.m01, m.m11, m.m21, m.m31,
						m.m02, m.m12, m.m22, m.m32,
						m.m03, m.m13, m.m23, m.m33,
				}, 0);
			} else {
				gl.glMultMatrixd(new double[]{
						m.m00, m.m10, m.m20, m.m30,
						m.m01, m.m11, m.m21, m.m31,
						m.m02, m.m12, m.m22, m.m32,
						0, 0, 0, m.m33,
				}, 0);
			}
		}
		
		for (Joint child : joint.children){
			drawBone(gl, glut, boneThickness, child, motion, false);
		}
		gl.glPopMatrix();
		
	}
	
	protected void glNormal(Tuple3d n){
		gl.glNormal3d(n.x, n.y, n.z);
	}
	protected void glVertex(Tuple3d n, Tuple3d base){
		gl.glVertex3d(n.x - base.x, n.y - base.y, n.z - base.z);
	}
	protected void glNormalInverse(Vector3d normal){
		gl.glNormal3d(-normal.x, -normal.y, -normal.z);
	}
	
	protected void drawLine(Point3d p0, Point3d p1){
		drawLine(p0, p1, 0.45);
	}
	
	protected void drawLine(Point3d p0, Point3d p1, double radius){
		drawLine(gl, glut, p0, p1, radius/scale);
	}
	
	@Override
	protected void init() {
	}
	
}
