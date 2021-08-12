package mrl.motion.viewer;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.sun.opengl.util.GLUT;

public interface GLDrawListener {

	public void onDraw(GL gl, GLU glu, GLUT glut);
}
