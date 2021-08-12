package mrl.widget;

import static mrl.util.MathUtil.round;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;

import mrl.util.MathUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public abstract class ScalableCanvas2D extends Composite implements PaintListener, MouseListener, MouseWheelListener, MouseMoveListener{
	
	protected Canvas canvas;
	protected Point2d center = new Point2d();
	protected double scale = 1;
	protected boolean useAntialias = true;

	private Point lastMousePoint;
	private Point selectionBoxStart;
	private Rectangle selectionBox;
	private int redrawTime = 100; 
	private long lastRedrawedTime = -1;
	
	private int redrawRequestCount = 0;
	private int lastDrawedCount = -1;
	protected int redrawLimitCount = -1;
	protected int drawCounter = 0;

	public ScalableCanvas2D(Composite parent) {
		super(parent, SWT.NONE);
		
		this.setLayout(new FillLayout());
		
		canvas = new Canvas(this, canvasStyle());
		canvas.addPaintListener(this);
		
		canvas.addMouseListener(this);
		canvas.addMouseWheelListener(this);
		canvas.addMouseMoveListener(this);
		
		canvas.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
		
		canvas.addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				onInitialDraw();
				canvas.removeControlListener(this);
			}
			
			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
	}
	
	protected abstract void drawContents(GC gc);
	protected boolean onMouseDown(Point2d p, MouseEvent e){ return false; };
	protected boolean onMouseMove(Point2d p, MouseEvent e){ return false; };
	protected boolean onMouseUp(Point2d p, MouseEvent e){ return false; };
	protected boolean onMouseDoubleClick(Point2d p, MouseEvent e){ return false; };
	protected boolean onMouseScrolled(Point2d p, MouseEvent e){ return false; };
	protected Rectangle getContentBoundary(){ return null; };
	protected void onBoundarySelection(Rectangle2d boundary, MouseEvent e){};
	
	protected void onInitialDraw(){
		fitToScreen();
	}
	
	public void redrawCanvas(){
		if (redrawTime <= 0){
			canvas.redraw();
			return;
		}
		
		redrawRequestCount++;
		final int thisCount = redrawRequestCount;
		getDisplay().timerExec(redrawTime, new Runnable() {
			@Override
			public void run() {
				long dt = System.currentTimeMillis() - lastRedrawedTime;
				if (redrawLimitCount > 0 ){
					if (thisCount != redrawRequestCount && redrawRequestCount - lastDrawedCount <= redrawLimitCount){
						return;
					}
				} else {
					if (dt < redrawTime && thisCount != redrawRequestCount) return;
				}
				
				if (canvas.isDisposed()) return;
				canvas.redraw();
				lastRedrawedTime = System.currentTimeMillis();
				lastDrawedCount = redrawRequestCount;
			}
		});
	}
	
	public int getDrawCounter() {
		return drawCounter;
	}

	public void setRedrawTime(int redrawTime) {
		this.redrawTime = redrawTime;
	}

	public Point2d getCenter() {
		return center;
	}

	public void setCenter(Point2d center) {
		this.center = center;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	protected int canvasStyle(){
		return SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED;
	}
	
	protected Color backgroundColor(){
		return getDisplay().getSystemColor(SWT.COLOR_WHITE);
	}
	
	public static void fillOval(GC gc, double x, double y, double width, double height) {
		gc.fillOval(round(x), round(y), round(width), round(height));
	}
	public static void fillRectangle(GC gc, double x, double y, double width, double height) {
		gc.fillRectangle(round(x), round(y), round(width), round(height));
	}
	public static void drawRectangle(GC gc, double x, double y, double width, double height) {
		gc.drawRectangle(round(x), round(y), round(width), round(height));
	}
	public static void drawOval(GC gc, double x, double y, double width, double height) {
		gc.drawOval(round(x), round(y), round(width), round(height));
	}
	public static void drawLine(GC gc, double x1, double y1, double x2, double y2) {
		gc.drawLine(round(x1), round(y1), round(x2), round(y2));
	}
	
	public static void drawLine(GC gc, double x1, double y1, double x2, double y2, double offset1, double offset2) {
		Vector3d direction = MathUtil.sub(new Vector3d(x2, y2, 0), new Vector3d(x1, y1, 0));
		if (direction.length() > 0){
			direction.normalize();
		}
		x1 += direction.x*offset1;
		y1 += direction.y*offset1;
		x2 -= direction.x*offset2;
		y2 -= direction.y*offset2;
		gc.drawLine(round(x1), round(y1), round(x2), round(y2));
	}
	
	public static void drawString(GC gc, String text, double x, double y, int xAlign, int yAlign) {
		Point tSize = gc.stringExtent(text);
		if (xAlign == 0) {
			x -= tSize.x/2;
		} else if (xAlign > 0) {
			x -= tSize.x;
		}
		if (yAlign == 0) {
			y -= tSize.y/2;
		} else if (yAlign < 0) {
			y -= tSize.y;
		}
		gc.drawString(text, round(x), round(y));
	}
	
	
	public static void drawArrow(GC gc, double x1, double y1, double x2, double y2, double radiusOffset) {
		drawArrow(gc, x1, y1, x2, y2, 0, radiusOffset);
//		Vector3d v0 = MathUtil.sub(new Vector3d(x2, y2, 0), new Vector3d(x1, y1, 0));
//		if (v0.length() > 0){
//			v0.normalize();
//		}
//		v0.scale(-radiusOffset);
//		x2 += v0.x;
//		y2 += v0.y;
//		
//		radiusOffset += gc.getLineWidth();
//		if (v0.length() > 0){
//			v0.normalize();
//			v0.scale(radiusOffset/2);
//		}
//		
//		
//		Vector3d v1 = MathUtil.sub(new Vector3d(x2, y2, 0), new Vector3d(x1, y1, 0));
//		Vector3d v2 = new Vector3d(v1);
//		
//		
//		
//		double headOffset = Math.max(24, gc.getLineWidth()*3);
//		v1.normalize();
//		v2.normalize();
//		v1.scale(-headOffset);
//		v2.scale(-headOffset);
//		
//		Matrix3d m = new Matrix3d();
//		m.rotZ(Math.toRadians(27));
//		m.transform(v1);
//		m.rotZ(Math.toRadians(-27));
//		m.transform(v2);
//		
//		gc.drawLine(round(x1), round(y1), round(x2 + v0.x), round(y2 + v0.y));
//		
//		gc.setBackground(gc.getForeground());
//		gc.fillPolygon(new int[]{
//				round(x2), round(y2), 
//				round(x2 + v1.x), round(y2 + v1.y),
//				round(x2 + v2.x), round(y2 + v2.y),
//		});
	}
	
	public static void drawArrow(GC gc, double x1, double y1, double x2, double y2, double offset1, double offset2) {
		drawArrow(gc, x1, y1, x2, y2, offset1, offset2, Math.max(24, gc.getLineWidth()*3));
	}
	public static void drawArrow(GC gc, double x1, double y1, double x2, double y2, double offset1, double offset2, double headSize) {
		Vector3d direction = MathUtil.sub(new Vector3d(x2, y2, 0), new Vector3d(x1, y1, 0));
		if (direction.length() > 0){
			direction.normalize();
		}
		x1 += direction.x*offset1;
		y1 += direction.y*offset1;
		x2 -= direction.x*offset2;
		y2 -= direction.y*offset2;
		
		Vector3d v1 = MathUtil.sub(new Vector3d(x2, y2, 0), new Vector3d(x1, y1, 0));
		Vector3d v2 = new Vector3d(v1);
		v1.normalize();
		v2.normalize();
		v1.scale(-headSize);
		v2.scale(-headSize);
		
		Matrix3d m = new Matrix3d();
		m.rotZ(Math.toRadians(27));
		m.transform(v1);
		m.rotZ(Math.toRadians(-27));
		m.transform(v2);
		
		gc.drawLine(round(x1), round(y1), round(x2 - direction.x*headSize/2), round(y2 - direction.y*headSize/2));
		
		gc.setBackground(gc.getForeground());
		gc.fillPolygon(new int[]{
				round(x2), round(y2), 
				round(x2 + v1.x), round(y2 + v1.y),
				round(x2 + v2.x), round(y2 + v2.y),
		});
	}
	
	protected Rectangle2d viewToReal(Rectangle rectangle){
		Point2d p1 = viewToReal(rectangle.x, rectangle.y);
		Point2d p2 = viewToReal(rectangle.x + rectangle.width, rectangle.y + rectangle.height);
		return new Rectangle2d(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
	}
	protected Point2d viewToReal(MouseEvent e){
		return viewToReal(e.x, e.y);
	}
	protected Point2d viewToReal(int x, int y){
		Point size = canvas.getSize();
		double midX = size.x/2d;
		double midY = size.y/2d;
		
		Point2d p = new Point2d();
		p.x = center.x + (x - midX)/scale;
		p.y = center.y + (y - midY)/scale;
		return p;
	}
	
	public void paintControl(GC gc, Point2d center, double scale, Point size){
		gc.setBackground(backgroundColor());
		gc.fillRectangle(0, 0, size.x, size.y);
		
		Transform transform = new Transform(gc.getDevice());
		transform.translate(size.x/2f, size.y/2f);
		transform.scale((float)scale, (float)scale);
		transform.translate((float)(-center.x), (float)(-center.y));
		
		gc.setTransform(transform);
		
		gc.setAdvanced(true);
		if (useAntialias){
			gc.setAntialias(SWT.ON);
		} else {
			gc.setAntialias(SWT.OFF);
		}
		
		drawContents(gc);
		
		if (selectionBox != null){
			Rectangle2d rectangle = viewToReal(selectionBox);
			gc.setAlpha(100);
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLUE));
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			gc.fillRectangle(round(rectangle.x), round(rectangle.y), round(rectangle.width), round(rectangle.height));
			gc.drawRectangle(round(rectangle.x), round(rectangle.y), round(rectangle.width), round(rectangle.height));
		}
		
		transform.dispose();
	}

	public void paintControl(GC gc) {
		Point size = canvas.getSize();
		paintControl(gc, center, scale, size);
	}
	
	@Override
	public void paintControl(PaintEvent e) {
		drawCounter++;
		paintControl(e.gc);
	}

	int scrolCount = 0;
	@Override
	public void mouseScrolled(MouseEvent e) {
		Point2d p = viewToReal(e);
		if (onMouseScrolled(p, e)){
			redrawCanvas();
			return;
		}
		
		if (e.count > 0){
			scale *= 1.1;
		} else {
			scale /= 1.1;
		}
		redrawCanvas();
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		Point2d p = viewToReal(e);
		if (onMouseDoubleClick(p, e)){
			redrawCanvas();
			return;
		}
		
		fitToScreen();
		redrawCanvas();
	}
	
	public void fitToScreen(){
		Rectangle boundary = getContentBoundary();
		if (boundary != null){
			center = new Point2d(boundary.x + boundary.width/2d, boundary.y + boundary.height/2d);
			Point size = canvas.getSize();
			int margin = 80;
			size.x -= margin;
			size.y -= margin;
			scale = Math.min(size.x/(double)boundary.width, size.y/(double)boundary.height);
//			System.out.println("fit to screen : " + scale + " : " + size + " : " + boundary);
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {
		Point2d p = viewToReal(e);
		if (onMouseDown(p, e)){
			redrawCanvas();
			Event event = new Event();
			event.stateMask = e.stateMask;
			this.notifyListeners(SWT.Selection, event);
			return;
		}
		
		lastMousePoint = new Point(e.x, e.y);
		if ((e.stateMask & SWT.SHIFT) > 0){
			selectionBoxStart = new Point(e.x, e.y);
			selectionBox = new Rectangle(e.x, e.y, 1, 1);
		}
	}
	
	@Override
	public void mouseMove(MouseEvent e) {
		Point2d p = viewToReal(e);
		if (onMouseMove(p, e)){
			redrawCanvas();
			return;
		}
		
		if (lastMousePoint == null) return;
		
		if (selectionBox != null){
			selectionBox = new Rectangle(
					Math.min(selectionBoxStart.x, e.x),
					Math.min(selectionBoxStart.y, e.y),
					Math.abs(selectionBoxStart.x - e.x),
					Math.abs(selectionBoxStart.y - e.y));
		} else {
			Point2d diff = new Point2d(e.x - lastMousePoint.x, e.y - lastMousePoint.y);
			diff.scale(1/scale);
			center.sub(diff);
		}
		lastMousePoint = new Point(e.x, e.y);
		
		redrawCanvas();
	}

	@Override
	public void mouseUp(MouseEvent e) {
		Point2d p = viewToReal(e);
		if (onMouseUp(p, e)){
			redrawCanvas();
			return;
		}
		if (selectionBox != null){
			onBoundarySelection(viewToReal(selectionBox), e);
			selectionBox = null;
			selectionBoxStart = null;
			redrawCanvas();
		}
		
		lastMousePoint = null;
		
	}

}
