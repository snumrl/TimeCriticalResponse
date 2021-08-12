package mrl.motion.viewer;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import mrl.util.Utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.sun.opengl.util.GLUT;

public abstract class SWTViewableCanvas extends Composite{
	
	public static int SLICE_AND_STACK = 20;
	public static int PLANE_LINE_WIDTH = 3;
	protected static final long ANIMATE_TIME = 600;
	
	protected GL gl; // interface to OpenGL
	protected GLU glu;
	protected GLUT glut;
	protected GLCanvas canvas;
	protected GLContext context;
	
	public Vector3d eye;
	public Vector3d center;
	public Vector3d upVector;
	
	protected double width;
	protected double height;
	protected double radius;

	protected DoubleBuffer model;
	protected DoubleBuffer proj;
	protected IntBuffer view;
	
	protected boolean isDragging = false;
	protected int lastX = -1;
	protected int lastY = -1;
	protected double fovy;
	
	protected boolean doPicking = false;
	
	protected long animateStartTime;
	protected boolean isAnimating;
	protected Vector3d startEye;
	protected Vector3d startCenter;
	protected Vector3d endEye;
	protected Vector3d endCenter;
	
	public boolean doAllwaysPicking = false;
	public Vector3d pickPoint;
	protected int pickX = -1;
	protected int pickY = -1;
	
	protected boolean isShadowMode = false;
	protected boolean enableWheel = true;
	protected boolean enableDoubleClick = true;
	
	protected int redrawTime = 1;
	protected double panningRatio = 1;
	
	public float backgroundColor = 40/255f;
	public float groundColor = 60/255f;
	
	public double scale = 0.25;
	public boolean drawPlane = true;
	public static boolean isXUp = false;
	
	protected ArrayList<GLDrawListener> listeners = new ArrayList<GLDrawListener>();
	protected ArrayList<GLDrawListener> postListeners = new ArrayList<GLDrawListener>();

	protected abstract void drawObjects();
	protected abstract void init();

	public SWTViewableCanvas(Composite parent){
		super(parent, SWT.NONE);
		
		// ?? eye ? center ??? ???.
		eye = new Vector3d(0, 0, 2);
		center = new Vector3d(0, 0, 0);
		upVector = new Vector3d(0, 1, 0);
		fovy = 25;
		
		model = DoubleBuffer.allocate(16);
		proj = DoubleBuffer.allocate(16);
		view = IntBuffer.allocate(4);
		
		
		this.setLayout(new FillLayout());
		GLData data = new GLData ();
		data.doubleBuffer = true;
		canvas = new GLCanvas(this, SWT.NONE, data);

		canvas.setCurrent();
		context = GLDrawableFactory.getFactory().createExternalGLContext();
		applyListeners();
		
		context.makeCurrent();
		makeGL();
		initBasic();
		init();
		enableFog();
		releaseGL();
		
		canvas.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				Rectangle bounds = canvas.getBounds();
				width = bounds.width;
				height = bounds.height;
				radius = Math.sqrt(width*width + height*height)/2;
				makeGL();
				gl.glViewport(0, 0, bounds.width, bounds.height);
				releaseGL();
			}
		});
		
		addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				removeControlListener(this);
//				getDisplay().asyncExec(new Runnable() {
				getDisplay().timerExec(1, new Runnable() {
					public void run() {
						if (!canvas.isDisposed()) {
							if (canvas.isVisible()){
								makeGL();
								display();
								for (GLDrawListener listener : postListeners){
									listener.onDraw(gl, glu, glut);
								}
								canvas.swapBuffers();
								releaseGL();
							}
							getDisplay().timerExec(redrawTime, this);
						}
					}
				});
			}
			@Override
			public void controlMoved(ControlEvent e) {}
		});
	}
	
	public GLCanvas getCanvas() {
		return canvas;
	}
	
	public double getScale() {
		return scale;
	}
	
	public void setScale(double scale) {
		this.scale = scale;
	}
	
	public void setDrawPlane(boolean drawPlane) {
		this.drawPlane = drawPlane;
	}
	
	public void setEnableWheel(boolean enableWheel) {
		this.enableWheel = enableWheel;
	}
	
	public void setEnableDoubleClick(boolean enableDoubleClick) {
		this.enableDoubleClick = enableDoubleClick;
	}
	
	public void setPanningRatio(double panningRatio) {
		this.panningRatio = panningRatio;
	}
	
	public void setRedrawTime(int redrawTime) {
		this.redrawTime = redrawTime;
	}
	
	protected void makeGL(){
		canvas.setCurrent();
		context.makeCurrent();
		gl = context.getGL ();
		glu = new GLU();
		glut = new GLUT();
	}
	
	protected void releaseGL(){
		try{
			context.release();
		} catch (GLException e){
			System.out.println("SWTViewableCanvas.releaseGL() :: release error");
		}
	}
	
	protected void initBasic(){
//		gl.glEnable(GL.GL_DEPTH_TEST);
//		gl.glEnable(GL.GL_NORMALIZE);
//		gl.glEnable(GL.GL_SMOOTH);
//		gl.glEnable(GL.GL_LIGHTING);
//		float[] ambientLight = { 0.2f, 0.2f, 0.2f, 1.0f };
//		float[] diffuseLight = { 0.3f, 0.3f, 0.3f, 1.0f };
//		float[] specular = { 0.1f, 0.1f, 0.1f, 1 };
//		float[] position = { 15, 15, 15, 1 };
//		gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, ambientLight, 0);
//		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, diffuseLight, 0);
//		gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, specular, 0);
//		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, position, 0);
//		gl.glEnable(GL.GL_LIGHT0);
//		gl.glEnable(GL.GL_COLOR_MATERIAL);
//		gl.glColorMaterial(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE);
//		gl.glMateriali(GL.GL_FRONT, GL.GL_SHININESS, 64);
//		
//		gl.glShadeModel(GL.GL_SMOOTH);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_NORMALIZE);
		gl.glEnable(GL.GL_SMOOTH);
		gl.glEnable(GL.GL_LIGHTING);
		
//		gl.glDisable(GL.GL_DEPTH_TEST);
//		gl.glEnable(GL.GL_BLEND);
//		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
//		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
		
		float ambient = 0.2f;
		float diffuse = 0.6f;
		float spec = 0.7f;
		float[] ambientLight = { ambient, ambient, ambient, 1.0f };
		float[] diffuseLight = { diffuse, diffuse, diffuse, 1.0f };
		float[] specular = { spec, spec, spec, 1 };
		float[] position = { 15, 15, 15, 1 };
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, ambientLight, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, diffuseLight, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, specular, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, position, 0);
		gl.glEnable(GL.GL_LIGHT0);
		gl.glEnable(GL.GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE);
		gl.glMateriali(GL.GL_FRONT, GL.GL_SHININESS, 32);
		gl.glShadeModel(GL.GL_SMOOTH);
		
		eye = new Vector3d(9.520614748512969, 33.17744437502335, -118.75228792662163);
		center = new Vector3d(2.0815837310808516, 17.552613310215943, 0.43831337277316645);
		upVector = new Vector3d(0.030228875954434634, 0.9921753292113596, -0.12113765377807954);
		Vector3d diff = new Vector3d();
		diff.sub(center, eye);
		diff.scale(2.5);
		diff.y = 5;
		eye.add(diff);
	}
	
	protected void enableFog(){
		gl.glEnable(GL.GL_FOG);							// Enables GL.GL_FOG
		gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);        // Fog Mode
		float groundColor = backgroundColor;
		gl.glFogfv(GL.GL_FOG_COLOR, new float[]{groundColor,groundColor,groundColor}, 0);            // Set Fog Color
		gl.glFogf(GL.GL_FOG_DENSITY, 0.0005f);              // How Dense Will The Fog Be
		gl.glHint(GL.GL_FOG_HINT, GL.GL_NICEST);				// Fog Hint Value
		gl.glFogf(GL.GL_FOG_START, 2000.0f);             // Fog Start Depth
		gl.glFogf(GL.GL_FOG_END, 3000.0f);   
//		gl.glFogf(GL.GL_FOG_START, 1000.0f);             // Fog Start Depth
//		gl.glFogf(GL.GL_FOG_END, 1500.0f);   
	}
	
	protected void display() {
		if (!this.isVisible()) return;
		
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		
		if (isAnimating){
			// ?????? ?? eye ? center? ???? ?? ?? ????.
			double ratio = (System.currentTimeMillis() - animateStartTime)/(double)ANIMATE_TIME;
			ratio = Math.min(ratio, 1);
			Vector3d dEye = new Vector3d();
			dEye.sub(endEye, startEye);
			dEye.scale(ratio);
			eye.add(startEye, dEye);
			Vector3d dCenter = new Vector3d();
			dCenter.sub(endCenter, startCenter);
			dCenter.scale(ratio);
			center.add(startCenter, dCenter);
			
			if (ratio >= 1){
				isAnimating = false;
			}
		}
		
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(fovy, width/height, 10,3000);
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		glu.gluLookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, upVector.x, upVector.y, upVector.z);
		
		// ??? gluUnproject ???? ?? ??
		// ?? matrix?? ??? buffer? ?????.
		gl.glGetDoublev(GL.GL_MODELVIEW_MATRIX, model);
		gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, proj);
		gl.glGetIntegerv(GL.GL_VIEWPORT, view);
		
		
		// ?? ???? ???.
		gl.glPushMatrix();
		gl.glClearColor((float)backgroundColor, (float)backgroundColor, (float)backgroundColor, 1);
		if (drawPlane) {
			gl.glPushMatrix();
			if (isXUp) gl.glRotated(90, 1, 0, 0);
			drawPlane();
			gl.glPopMatrix();
		}
		
//		gl.glEnable(GL.GL_COLOR_MATERIAL);
//		gl.glColorMaterial(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE);
		gl.glDepthMask(true);
//		gl.glEnable(GL.GL_DEPTH_TEST);
//		gl.glDepthFunc(GL.GL_LEQUAL);
		
		
		if (doAllwaysPicking){
			FloatBuffer buffer = FloatBuffer.allocate(4);
			gl.glReadPixels(pickX, view.get(3) - pickY, 1, 1, GL.GL_DEPTH_COMPONENT, GL.GL_FLOAT, buffer);
			
			// depth ?? 1?? ?? ??? ??? ?? ???? picking ?? ???.
			float depth = buffer.get(0);
			if (depth >= 1) return;
			// depth ?? ???? unProject? ??? ?? ???  ?? ??? ????.
			pickPoint = unProject(new Vector3d(pickX, view.get(3) - pickY, depth));
		}
		
		
		// Picking? ???? ?? ????.
		if (doPicking){
			doPicking = false;
			// ??? ?? ??? depth ?? ???.
			FloatBuffer buffer = FloatBuffer.allocate(4);
			gl.glReadPixels(lastX, view.get(3) - lastY, 1, 1, GL.GL_DEPTH_COMPONENT, GL.GL_FLOAT, buffer);
			
			// depth ?? 1?? ?? ??? ??? ?? ???? picking ?? ???.
			float depth = buffer.get(0);
			if (depth >= 1) return;
			// depth ?? ???? unProject? ??? ?? ???  ?? ??? ????.
			Vector3d point = unProject(new Vector3d(lastX, view.get(3) - lastY, depth));
		
			// ?? ??? ???? ?? ??, ?? ??? ??? ???? ??? ???
			// ??? ??????.
			Vector3d diff = new Vector3d();
			diff.sub(point, center);
			
			Vector3d newEye = new Vector3d(eye);
			Vector3d newCenter = new Vector3d(center);
			newEye.add(diff);
			newCenter.add(diff);
			animateTo(newEye, newCenter);
			
			System.out.println("pick : " + newCenter);
		}
	
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glLineWidth(3);
		gl.glScaled(scale, scale, scale);
		
		drawObjects();
		for (GLDrawListener listener : listeners){
			listener.onDraw(gl, glu, glut);
		}
		gl.glPopMatrix();
		
		
		
		
	}
	
	/**
	 * ?? eye? center? ?? ??? ??? ????? ??? ??.
	 * @param newEye
	 * @param newCenter
	 */
	protected void animateTo(Vector3d newEye, Vector3d newCenter){
		startEye = new Vector3d(eye);
		startCenter = new Vector3d(center);
		endEye = new Vector3d(newEye);
		endCenter = new Vector3d(newCenter);
		animateStartTime = System.currentTimeMillis();
		isAnimating = true;
	}
	
	/**
	 * ??? z? ???? ???? vector?
	 * ?? eye ??? ?? ???? ???? ????? ??. 
	 * @param vector
	 * @return
	 */
	protected Vector3d unProjectToEye(Vector3d vector){
		// eye ??? z??? ???. 
		Vector3d zAxis = new Vector3d();
		zAxis.sub(center, eye);
		// eye?? vector? upVector? ??? ??? ? ?? ???.
		Vector3d yAxis = new Vector3d();
		yAxis.cross(zAxis, upVector);
		yAxis = rotate(zAxis, yAxis, Math.PI/2);
		Vector3d xAxis = new Vector3d();
		xAxis = rotate(zAxis, yAxis, Math.PI/2);
		
		// ?? vector? ??? ?? ?? ????.
		Vector3d newVector = new Vector3d();
		xAxis.scale(vector.x);
		newVector.add(xAxis);
		yAxis.scale(vector.y);
		newVector.add(yAxis);
		zAxis.scale(vector.z);
		newVector.add(zAxis);
		return newVector;
	}
	 
	/**
	 * ?? mode, proj, view? ???? ?? 
	 * gluUnproject ??? ??? ???? ?? ??? ?? model?? ??? ??? ??.
	 * @param vector
	 * @return
	 */
	protected Vector3d unProject(Tuple3d vector){
		DoubleBuffer objPos = DoubleBuffer.allocate(3);
		glu.gluUnProject(vector.x, vector.y, vector.z, model, proj, view, objPos);
		return new Vector3d(objPos.get(), objPos.get(), objPos.get());
	}
	
	/**
	 * 2???? ??? ??? ???? ?? 
	 * ??? ???? ???? ??? 3?? ??? ????? ??.
	 * ???? radius R? ??? ??? ??? ???? ?? ???
	 * ???? x,y?? ?? ??? ? ?? ???? ??.
	 * @param mouseX
	 * @param mouseY
	 * @return
	 */
	protected Vector3d getMousePoint(int mouseX, int mouseY){
		double x = mouseX - width/2;
		double y = mouseY - height/2;
		double zs = radius*radius - x*x - y*y;
		if (zs <= 0){
			zs = 0;
		}
		double z = Math.sqrt(zs);
		return new Vector3d(x, y, z);
	}
	
	/**
	 * input vector? rotateVector ???? angle?? ?????.
	 * 
	 * @param input
	 * @param rotateVector
	 * @param angle
	 * @return
	 */
	protected Vector3d rotate(Vector3d input, Vector3d rotateVector, double angle){
		Matrix3d matrix = new Matrix3d();
		Vector3d rotated = new Vector3d(input);
		matrix.set(new AxisAngle4d(rotateVector, angle));
		matrix.transform(rotated);
		return rotated;
	}
	
	protected void showAll(){
		// ?? ??? ??? ?? ??? 
		// ????? ????? ?? ?? ? ?? ? ?? ???.
		Point3d[] boundary = getObjectBoundary();
		Point3d min = boundary[0];
		Point3d max = boundary[1];
		double[][] points = new double[][]{
				{max.x, max.y, max.z}, {max.x, min.y, max.z},
				{min.x, max.y, max.z}, {min.x, min.y, max.z},
				{max.x, max.y, min.z}, {max.x, min.y, min.z},
				{min.x, max.y, min.z}, {min.x, min.y, min.z},
		};
		
		Vector3d dEye = new Vector3d();
		dEye.sub(center, eye);
		double maxLen = -1;
		for (int i = 0; i < points.length; i++) {
			double len = calcFocalLength(new Vector3d(points[i][0],points[i][1],points[i][2]), dEye);
			if (len > maxLen){
				maxLen = len;
			}
		}
		
		if (maxLen > 0){
			dEye.normalize();
			dEye.scale(maxLen);
			Vector3d newEye = new Vector3d();
			newEye.sub(center, dEye);
			animateTo(newEye, center);
		}
	}
	
	protected Point3d[] getObjectBoundary(){
		Point3d[] boundary = new Point3d[2];
		double l = 0.6;
		boundary[0] = new Point3d(-l, -l, -l);
		boundary[1] = new Point3d(l, l, l);
		return boundary;
	}
	
	protected double calcFocalLength(Vector3d point, Vector3d dEye){
		try{
			//       obj
			//    /       \
			// eye ------ center
			//
			// ???? ???.
			// (obj-eye ?? eye-center ? ?? ??  = fovy/2)
			double a = fovy/2/180*Math.PI;
			Vector3d dCenter = new Vector3d();
			dCenter.sub(center, point);
			// ??? ????? ?? ??? center? ?? ???? ??? ???.
			// (eye-center ?? obj-center ? ?? ?? )
			double b = dCenter.angle(dEye);
			// obj-center? ??? ??? ????, ??? ? ??? ???
			// eye-center ??? ??? ???.
			double len = dCenter.length()*(Math.cos(a) + Math.cos(b))/(Math.sin(a)/Math.sin(b));
			return len;
		} catch (RuntimeException e){
			e.printStackTrace();
			return Double.NaN;
		}
	}
	
	protected void draw3DAxis(int length, int count, double t){
		gl.glLineWidth(1);
		
		gl.glColor3d(0, 1, 0);
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glVertex3d(0, -length, 0);
		gl.glVertex3d(0, length, 0);
		gl.glEnd();
		double offset = -length;
		for (int i = 0; i <= count; i++) {
			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3d(-t, offset, 0);
			gl.glVertex3d(t, offset, 0);
			gl.glEnd();
			offset += (length/(double)(count/2));
		}
		
		gl.glColor3d(1, 0, 0);
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glVertex3d(-length, 0, 0);
		gl.glVertex3d(length, 0, 0);
		gl.glEnd();
		offset = -length;
		for (int i = 0; i <= count; i++) {
			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3d(offset, -t, 0);
			gl.glVertex3d(offset, t, 0);
			gl.glEnd();
			offset += (length/(double)(count/2));
		}
		
		gl.glColor3d(0, 0, 1);
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glVertex3d(0, 0, -length);
		gl.glVertex3d(0, 0, length);
		gl.glEnd();
		offset = -length;
		for (int i = 0; i <= count; i++) {
			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3d(0, -t, offset);
			gl.glVertex3d(0, t, offset);
			gl.glEnd();
			offset += (length/(double)(count/2));
		}
	}
	
	protected void drawPlane(){
		setupFloor();
		int count = 150;
		int interval = 30;
		
		gl.glLineWidth(PLANE_LINE_WIDTH);
		
		double lineScale = 0.7f;
		gl.glColor3d(lineScale, lineScale, lineScale);
		
		gl.glNormal3d(0, 1, 0);
		gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_LINE );
		for (int i = 0; i < count; i++) {
			double x = (i - count/2)*interval;
			for (int j = 0; j < count; j++) {
				double y = (j - count/2)*interval;
				drawPlaneRect(x, y, interval, interval);
//				double offset = interval * 0.0125;
//				drawPlaneRect(x, y, offset, interval);
//				drawPlaneRect(x, y, interval, offset);
			}
		}
		
		
		double grayScale = groundColor;
		gl.glColor3d(grayScale, grayScale, grayScale);
		gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_FILL );
		for (int i = 0; i < count; i++) {
			double x = (i - count/2)*interval;
			for (int j = 0; j < count; j++) {
				double y = (j - count/2)*interval;
				drawPlaneRect(x, y, interval, interval);
			}
		}
		
		gl.glLineWidth(1);
		unsetupFloor();
		gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL.GL_FILL );
	}
	
	
	protected void drawPlaneRect(double x, double y, double width, double height){
		gl.glBegin(GL.GL_QUADS);
		gl.glNormal3d(0, 1, 0);
		gl.glVertex3d(x, 0, y);
		gl.glVertex3d(x + width, 0, y);
		gl.glVertex3d(x + width, 0, y + height);
		gl.glVertex3d(x, 0, y + height);
		gl.glEnd();
	}
	
	protected void reset(){
		// ?? ???? ?? ????.
		eye = new Vector3d(0, 0, 2);
		center = new Vector3d(0, 0, 0);
		upVector = new Vector3d(0, 1, 0);
		fovy = 25;
	}
	
	protected void onMouseDown(MouseEvent e){
		
	}
	protected void onMouseUp(MouseEvent e){
		
	}
	protected void onMouseMove(MouseEvent e){
		// ?? ??? ??? ?? ??? ???.
		Vector3d lastP = getMousePoint(lastX, lastY);
		Vector3d currentP = getMousePoint(e.x, e.y);
		
		
		if (isRightButtonDown || (e.stateMask & SWT.SHIFT) != 0){
			// ???? ??? ???? ??? ?? ?? ??? ??? translate ??.
			Vector3d moveVector = new Vector3d(e.x - lastX, e.y - lastY, 0);
			Vector3d originMoveVector = new Vector3d(moveVector);
			moveVector = unProjectToEye(moveVector);
			moveVector.normalize();
			double dx  = e.x - lastX;
			double dy  = e.y - lastY;
			double eyeDistance = new Point3d(eye).distance(new Point3d(center));
			moveVector.scale(Math.sqrt(dx*dx + dy*dy)/2000*eyeDistance*panningRatio);
			translateCenter(moveVector, originMoveVector);
		} else {
			// ? ???? ??? ?? ?? ?? ???.
			Vector3d rotateVector = new Vector3d();
			rotateVector.cross(currentP, lastP);
			// ? ???? ??? ?? ???? ?? ???.
			double angle = -currentP.angle(lastP)*2;
			// ??? ?? ??? ???? z??? ???? ??? ?????,
			// ?? eye? ??? ?? ?????? ???? ????.
			rotateVector = unProjectToEye(rotateVector);
			
			// center? ??? ??, eye? upVector? ?? ? ???? angle ?? ?????.
			Vector3d dEye = new Vector3d();
			dEye.sub(center, eye);
			dEye = rotate(dEye, rotateVector, -angle);
			upVector = rotate(upVector, rotateVector, -angle);
			eye.sub(center, dEye);
		}
	}
	
	protected void translateCenter(Vector3d moveVector, Vector3d originMoveVector){
		center.add(moveVector);
		eye.add(moveVector);
	}
	
	protected boolean isRightButtonDown = false;
	protected void applyListeners(){
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				isDragging = false;
				isRightButtonDown = false;
				onMouseUp(e);
			}
			@Override
			public void mouseDown(MouseEvent e) {
				isDragging = true;
				isRightButtonDown = e.button != 1;
				lastX = e.x;
				lastY = e.y;
				onMouseDown(e);
//				// shift+click?? ???? ? ??? picking? ????.
//				if ((e.stateMask & SWT.CTRL) != 0){
//					doPicking = true;
//				}
			}
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (!enableDoubleClick) return;
				
				lastX = e.x;
				lastY = e.y;
				if ((e.stateMask & SWT.CTRL) != 0) return;
				if ((e.stateMask & SWT.SHIFT) != 0) return;
				doPicking = true;
			}
		});
		canvas.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				pickX = e.x;
				pickY = e.y;
				if (!isDragging) return;
				onMouseMove(e);
				lastX = e.x;
				lastY = e.y;
			}
		});
		
		applyMouseWheelListener();
		
		applyKeyListener();
	}
	
	protected void applyKeyListener(){
		canvas.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == 'p'){
					printStatus();
				}
				
//				if (!Character.isUpperCase(e.character)){
//					// ?? ? ??? ??, eye? center ? ? ?????.
//					Vector3d diff = new Vector3d();
//					switch (e.character) {
//					case 'a':	diff.x = 1;break;
//					case 'd':	diff.x = -1;break;
//					case 'w':	diff.y = 1;break;
//					case 'x':	diff.y = -1;break;
//					case 'e':	diff.z = 1;break;
//					case 'c':	diff.z = -1;break;
//					case 'v':	showAll();return;
//					case 'r':	reset();return;
//					case '+':	zoom(true);return;
//					case '-':	zoom(false);return;
//					default:	return;
//					}
//					diff = unProjectToEye(diff);
//					diff.normalize();
//					diff.scale(0.07);
//					
//					eye.add(diff);
//					center.add(diff);
//				} else {
//					// shift ?? ??? ??? ??, eye? ??? ?? center? ????.
//					Vector3d rVector = new Vector3d();
//					switch (Character.toLowerCase(e.character)){
//					case 'a':	rVector.y = 1;break;
//					case 'd':	rVector.y = -1;break;
//					case 'w':	rVector.x = -1;break;
//					case 'x':	rVector.x = 1;break;
//					case 'v':	showAll();return;
//					case 'r':	reset();return;
//					default:	return;
//					}
//					rVector = unProjectToEye(rVector);
//					Vector3d dEye = new Vector3d();
//					dEye.sub(center, eye);
//					dEye = rotate(dEye, rVector, 0.07);
//					center.add(eye, dEye);
//					upVector = rotate(upVector, rVector, 0.07);
//				}
			}
		});
	}
	
	protected void printStatus(){
		System.out.println("eye = new Vector3d" + eye + ";");
		System.out.println("center = new Vector3d" + center + ";");
		System.out.println("upVector = new Vector3d" + upVector + ";");
	}
	
	protected void zoom(boolean isZoomIn){
		// dolly-in/out? ?? center ? eye ??? ??? ?,
		// eye? ???? eye? center ?? ??? ????.
		Vector3d dEye = new Vector3d();
		dEye.sub(center, eye);
		if (isZoomIn){
			dEye.scale(0.9);
		} else {
			dEye.scale(1.1);
		}
		eye.sub(center, dEye);
	}
	
	protected void applyMouseWheelListener(){
		canvas.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				if (!enableWheel) return;
				if ((e.stateMask & SWT.SHIFT) != 0){
//					// shift ? ?? ?? zoom-in/out? ?? fovy? ????.
//					if (e.count > 0){
//						fovy = fovy * 0.9;
//					} else {
//						fovy = fovy * 1.1;
//					}
//					if (fovy <= 6) fovy = 6;
//					if (fovy >= 174) fovy = 174;
				} else {
					// dolly-in/out? ?? center ? eye ??? ??? ?,
					// eye? ???? eye? center ?? ??? ????.
					Vector3d dEye = new Vector3d();
					dEye.sub(center, eye);
					if (e.count > 0){
						dEye.scale(0.9);
					} else {
						dEye.scale(1.1);
					}
					eye.sub(center, dEye);
				}
			}
		});
	}
	
	private static float[][] boxVertices;
	private static final float[][] boxNormals = { { -1.0f, 0.0f, 0.0f },
			{ 0.0f, 1.0f, 0.0f }, { 1.0f, 0.0f, 0.0f }, { 0.0f, -1.0f, 0.0f },
			{ 0.0f, 0.0f, 1.0f }, { 0.0f, 0.0f, -1.0f } };
	private static final int[][] boxFaces = { { 0, 1, 2, 3 }, { 3, 2, 6, 7 },
			{ 7, 6, 5, 4 }, { 4, 5, 1, 0 }, { 5, 6, 2, 1 }, { 7, 4, 0, 3 } };
	protected void drawBox(float x, float y, float z) {
		if (boxVertices == null) {
			float[][] v = new float[8][];
			for (int i = 0; i < 8; i++) {
				v[i] = new float[3];
			}
			v[0][0] = v[1][0] = v[2][0] = v[3][0] = -0.5f;
			v[4][0] = v[5][0] = v[6][0] = v[7][0] = 0.5f;
			v[0][1] = v[1][1] = v[4][1] = v[5][1] = -0.5f;
			v[2][1] = v[3][1] = v[6][1] = v[7][1] = 0.5f;
			v[0][2] = v[3][2] = v[4][2] = v[7][2] = -0.5f;
			v[1][2] = v[2][2] = v[5][2] = v[6][2] = 0.5f;
			boxVertices = v;
		}
		float[][] v = boxVertices;
		float[][] n = boxNormals;
		int[][] faces = boxFaces;
		for (int i = 5; i >= 0; i--) {
			gl.glBegin(GL.GL_QUADS);
			gl.glNormal3fv(n[i], 0);
			float[] vt = v[faces[i][0]];
			gl.glVertex3f(vt[0] * x, vt[1] * y, vt[2] * z);
			vt = v[faces[i][1]];
			gl.glVertex3f(vt[0] * x, vt[1] * y, vt[2] * z);
			vt = v[faces[i][2]];
			gl.glVertex3f(vt[0] * x, vt[1] * y, vt[2] * z);
			vt = v[faces[i][3]];
			gl.glVertex3f(vt[0] * x, vt[1] * y, vt[2] * z);
			gl.glEnd();
		}
	}

	protected void setupShadow(){
		setupShadow(0.2, 0.5, false);
	}
	protected void setupShadow(double color, double alpha, boolean blending)
	{
		gl.glDisable(GL.GL_LIGHTING);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glDisable(GL.GL_CULL_FACE);
		gl.glEnable(GL.GL_BLEND);
		if (blending){
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		} else {
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ZERO);
		}

		gl.glEnable(GL.GL_STENCIL_TEST);
		gl.glStencilFunc(GL.GL_EQUAL,0x1,0x1);
		gl.glStencilOp(GL.GL_KEEP,GL.GL_ZERO,GL.GL_ZERO);
		gl.glStencilMask(0x1);		// only deal with the 1st bit

		gl.glPushMatrix();
		// a matrix that squishes things onto the floor
		//float sm[16] = {1,0,0,0, 0,0,0,0.0, 0,0,1,0, 0,0.0,0,1};
		double light1_x = 10.0;
		double light1_y = -10.0;
		double light1_z = 20.0;

		double[] sm = {1,0,0,0, -(light1_x/light1_z) ,0,-(light1_y/light1_z),0, 0,0,1,0, 0,0,0,1};
		gl.glMultMatrixd(sm, 0);
		// draw in transparent black (to dim the floor)
		gl.glColor4d(color,color,color,alpha);
		
		isShadowMode = true;
	}

	protected void unsetupShadow()
	{
		isShadowMode = false;
		
		gl.glPopMatrix();
//		gl.glEnable(GL.GL_CULL_FACE);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_LIGHTING);
		gl.glDisable(GL.GL_STENCIL_TEST);
		gl.glDisable(GL.GL_BLEND);
	}
	
	protected void setupFloor()
	{
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_STENCIL_TEST);
		gl.glStencilFunc(GL.GL_ALWAYS,0x1,0x1);
		gl.glStencilOp(GL.GL_REPLACE,GL.GL_REPLACE,GL.GL_REPLACE);
		gl.glStencilMask(0x1);		// only deal with the 1st bit
	}
	
	protected void unsetupFloor(){
		gl.glDisable(GL.GL_STENCIL_TEST);
	}
	
	public void addDrawListener(GLDrawListener listener){
		listeners.add(listener);
	}
	public void removeDrawListener(GLDrawListener listener){
		listeners.remove(listener);
	}
	public void addPostDrawListener(GLDrawListener listener){
		postListeners.add(listener);
	}
	public void removePostDrawListener(GLDrawListener listener){
		postListeners.remove(listener);
	}
	
	public static void drawLine(GL gl, GLUT glut, Point3d p0, Point3d p1, double radius){
		gl.glPushMatrix();
		Vector3d v = new Vector3d();
		v.sub(p1, p0);
		double len = v.length();
		Vector3d base = new Vector3d(0, 0, 1);
		Vector3d cross = new Vector3d();
		cross.cross(base, v);
		if (Double.isNaN(cross.x)){
			cross = new Vector3d(0, 1, 0);
		}
		double angle = base.angle(v);
		gl.glTranslated(p0.x, p0.y, p0.z);
		gl.glRotated(Math.toDegrees(angle), cross.x, cross.y, cross.z);
		glut.glutSolidCylinder(radius, len, SLICE_AND_STACK, SLICE_AND_STACK);
		gl.glPopMatrix();
	}
	
	public static void drawSphere(GL gl, GLUT glut, Point3d p, double radius){
		gl.glPushMatrix();
		gl.glTranslated(p.x, p.y, p.z);
		glut.glutSolidSphere(radius, SLICE_AND_STACK, SLICE_AND_STACK);
		gl.glPopMatrix();
	}
}

