package mrl.motion.viewer.module;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.sun.opengl.util.GLUT;

import mrl.widget.app.Item;
import mrl.widget.app.Item.ItemDescription;

public abstract class SelfItemDrawer extends ItemDrawer{
	
	@Override
	public boolean isDrawable(Object data) {
		return this == data;
	}

	@Override
	public void draw(GL gl, GLU glu, GLUT glut, ItemDescription desc, Object data, int timeIndex) {
		drawImpl(gl, glu, glut, desc, timeIndex);
	}
	
	public String toString(Item item, Object data, int timeIndex){
		return "SelfItemDrawer:" + this.toString();
	}
	
	protected abstract void drawImpl(GL gl, GLU glu, GLUT glut, ItemDescription desc, int timeIndex);
}
