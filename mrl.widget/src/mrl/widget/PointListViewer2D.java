package mrl.widget;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * 2차원 점들의 목록을 그려주는 클래스
 * 점들을 몇개의 그룹으로 나눠 다른 색으로 표시하고
 * 마우스 조작으로 점 선택 및 zoom, panning이 가능하다
 */
public class PointListViewer2D extends Composite implements PaintListener{
	
	protected Canvas canvas;
	protected ArrayList<ArrayList<Point2d>> pointGroupList = new ArrayList<ArrayList<Point2d>>();
	protected ArrayList<Color> groupColorList = new ArrayList<Color>();
	
	protected Point2d centerPoint = new Point2d();
	protected double zoom = 1;
	
	protected Point canvasSize;
	protected int pointSize = 5;
	protected int halfPointSize = pointSize/2;
	
	protected boolean isMouseDown = false;
	protected Point lastPoint;
	
	protected Point2d selectedOriginPoint = null;
	protected int selectedGroupIndex = -1;
	protected int selectedPointIndex = -1;
	protected ArrayList<Point2d> selectedGroup = null;
	
	protected double selectionDistanceLimit = 10;
	protected boolean groupSelectionHighlighting = true;
	
	protected boolean drawSelectedGroupSame = false;
	
	protected boolean showAxis = true;

	public PointListViewer2D(Composite parent) {
		super(parent, SWT.NONE);
		
		this.setLayout(new FillLayout());
		
		canvas = new Canvas(this, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		canvas.addPaintListener(this);
		canvas.addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				arrangeBoundary();
			}
			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
		
		canvas.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				isMouseDown = false;
			}
			
			@Override
			public void mouseDown(MouseEvent e) {
				onMouseDown(e);
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}
		});
		canvas.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				onMouseMove(e);
			}
		});
		canvas.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				onMouseWheel(e);
			}
		});
	}
	
	public Canvas getCanvas() {
		return canvas;
	}

	public void setPointSize(int size){
		this.pointSize = size;
		this.halfPointSize = size/2;
	}
	
	protected void onMouseWheel(MouseEvent e){
		if (e.count > 0){
			zoomIn();
		} else {
			zoomOut();
		}
	}
	
	protected void onMouseMove(MouseEvent e){
		if (isMouseDown){
			int dx = e.x - lastPoint.x;
			int dy = e.y - lastPoint.y;
			centerPoint.x -= dx/zoom;
			centerPoint.y -= dy/zoom;
			lastPoint = new Point(e.x, e.y);
			canvas.redraw();
		}
	}
	protected void onMouseDown(MouseEvent e){
		isMouseDown = true;
		lastPoint = new Point(e.x, e.y);
		canvasSize = canvas.getSize();
		
		Point2d mousePoint = new Point2d(e.x, e.y);
		clearSelection();
		double nearestDistance = selectionDistanceLimit;
		for (int i = 0; i < pointGroupList.size(); i++) {
			ArrayList<Point2d> pList = pointGroupList.get(i);
			for (int j = 0; j < pList.size(); j++) {
				Point2d originP = pList.get(j);
				Point2d p = transPoint(originP);
				double d = p.distance(mousePoint);
				if (d < nearestDistance){
					selectedOriginPoint = new Point2d(originP);
					selectedGroup = pList;
					selectedGroupIndex = i;
					selectedPointIndex = j;
					nearestDistance = d;
				}
			}
		}
		System.out.println(getClass().getSimpleName() + selectedOriginPoint);
		onSelectionChange(selectedOriginPoint);
		canvas.setFocus();
		canvas.redraw();
	}
	
	public Point2d getSelectedPoint(){
		if (selectedOriginPoint == null) return null;
		return new Point2d(selectedOriginPoint);
	}
	
	public void selectPoint(Point2d point){
		clearSelection();
		double nearestDistance = selectionDistanceLimit;
		for (int i = 0; i < pointGroupList.size(); i++) {
			ArrayList<Point2d> pList = pointGroupList.get(i);
			for (int j = 0; j < pList.size(); j++) {
				Point2d originP = pList.get(j);
				double d = originP.distance(point);
				if (d < nearestDistance){
					selectedOriginPoint = new Point2d(originP);
					selectedGroup = pList;
					selectedGroupIndex = i;
					selectedPointIndex = j;
					nearestDistance = d;
				}
			}
		}
		onSelectionChange(selectedOriginPoint);
		canvas.redraw();
	}
	
	protected void clearSelection(){
		selectedOriginPoint = null;
		selectedGroup = null;
		selectedGroupIndex = selectedPointIndex = -1;
	}
	
	protected void onSelectionChange(Point2d selectedPoint){
		Event event = new Event();
		event.widget = this;
		notifyListeners(SWT.Selection, event);
	}
	
	private void zoomIn(){
		zoom = zoom*1.2;
		canvas.redraw();
	}
	private void zoomOut(){
		zoom = zoom/1.2;
		canvas.redraw();
	}
	
	
	
	public void setZoom(double zoom) {
		this.zoom = zoom;
		canvas.redraw();
	}

	public ArrayList<ArrayList<Point2d>> getPointGroupList() {
		return pointGroupList;
	}

	public void setPointGroupList(ArrayList<ArrayList<Point2d>> pointGroupList) {
		setPointGroupList(pointGroupList, true);
	}
	
	protected void setPointGroupList(ArrayList<ArrayList<Point2d>> pointGroupList, boolean arrangeBoundary) {
		this.pointGroupList = pointGroupList;
		selectedOriginPoint = null;
		selectedGroup = null;
		
		for (Color c : groupColorList) c.dispose();
		groupColorList.clear();
		
		ColorModel colorModel = new ColorModel();
		for (int i = 0; i < pointGroupList.size(); i++) {
			Point3d c = new Point3d(colorModel.getNewColor());
			c.scale(255);
			groupColorList.add(new Color(getDisplay(), (int)c.x, (int)c.y, (int)c.z));
		}
		if (arrangeBoundary) arrangeBoundary();
		canvas.redraw();
	}
	
	protected Point2d minP;
	protected Point2d maxP;
	public void arrangeBoundary(){
		zoom = 1;
				
		if (pointGroupList.size() == 0
				|| (pointGroupList.size() == 1 && pointGroupList.get(0).size() == 0)) return;
		
		calculateBoundary();
		
		double xOffset = Math.max(1, Math.max(centerPoint.x - minP.x, maxP.x - centerPoint.x))*1.1*2;
		double yOffset = Math.max(1, Math.max(centerPoint.y - minP.y, maxP.y - centerPoint.y))*1.1*2;
		Point size = canvas.getSize();
		size.x = Math.max(size.x, 5);
		size.y = Math.max(size.y, 5);
		zoom = Math.min(size.x/xOffset, size.y/yOffset);
	}
	
	protected void calculateBoundary(){
		minP = new Point2d(Integer.MAX_VALUE, Integer.MAX_VALUE);
		maxP = new Point2d(Integer.MIN_VALUE, Integer.MIN_VALUE);
		for (ArrayList<Point2d> pList : pointGroupList){
			calculateBoundary(minP, maxP, pList);
		}
		centerPoint.x = (minP.x + maxP.x)/2;
		centerPoint.y = (minP.y + maxP.y)/2;
	}
	
	protected void calculateBoundary(Point2d minP, Point2d maxP, ArrayList<Point2d> pList){
		for (Point2d p : pList){
			minP.x = Math.min(minP.x, p.x);
			minP.y = Math.min(minP.y, p.y);
			maxP.x = Math.max(maxP.x, p.x);
			maxP.y = Math.max(maxP.y, p.y);
		}
	}

	@Override
	public void paintControl(PaintEvent e) {
		canvasSize = canvas.getSize();
		GC gc = e.gc;
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(0, 0, canvasSize.x, canvasSize.y);
		
		prePaintControl(gc);
		
		gc.setAdvanced(true);
		gc.setAntialias(SWT.ON);
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		if (showAxis) drawAxis(gc);
		mainPaintControl(gc);
		
		postPaintControl(gc);
	}
	
	protected void drawAxis(GC gc){
		int len = 10000;
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		drawLine(gc, new Point2d(-len, 0), new Point2d(len, 0));
		drawLine(gc, new Point2d(0, -len), new Point2d(0, len));
		
		drawLine(gc, new Point2d(-2, 25), new Point2d(2, 25));
		drawLine(gc, new Point2d(-2, -25), new Point2d(2, -25));
		drawLine(gc, new Point2d(25, -2), new Point2d(25, 2));
		drawLine(gc, new Point2d(-25, -2), new Point2d(-25, 2));
	}
	
	protected void mainPaintControl(GC gc){
		boolean isGroupHighlight = !drawSelectedGroupSame && (pointGroupList.size() > 1);
		for (int i = 0; i < pointGroupList.size(); i++) {
			ArrayList<Point2d> pList = pointGroupList.get(i);
			if (isGroupHighlight && groupSelectionHighlighting && pList == selectedGroup){
//				gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
//				for (Point2d p : pList){
//					drawPoint(gc, p, pointSize+15);
//				}
			} else {
				gc.setBackground(groupColorList.get(i));
				for (Point2d p : pList){
					drawPoint(gc, p);
				}
			}
		}
		if (isGroupHighlight && selectedGroup != null ){
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
			for (Point2d p : selectedGroup){
				drawPoint(gc, p, pointSize+2);
				drawPointBorder(gc, p, pointSize+2);
			}
		}
		if (selectedOriginPoint != null){
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
			drawPoint(gc, selectedOriginPoint, 18);
			drawPointBorder(gc, selectedOriginPoint, 18);
		}
	}
	protected void prePaintControl(GC gc){
	}
	protected void postPaintControl(GC gc){
	}
	
	protected int round(double d){
		return (int)Math.round(d);
	}
	
	protected void drawLine(GC gc, Point2d p1, Point2d p2){
		p1 = transPoint(p1);
		p2 = transPoint(p2);
		gc.drawLine(round(p1.x), round(p1.y), round(p2.x), round(p2.y));
	}
	
	protected void drawPoint(GC gc, Point2d p){
		p = transPoint(p);
		gc.fillOval((int)Math.round(p.x - halfPointSize), 
				(int)Math.round(p.y - halfPointSize), pointSize, pointSize);
	}
	
	protected void drawPoint(GC gc, Point2d p, int size){
		p = transPoint(p);
		gc.fillOval((int)Math.round(p.x - size/2d), (int)Math.round(p.y - size/2d), size, size);
	}
	
	protected void drawPointBorder(GC gc, Point2d p, int size){
		p = transPoint(p);
		gc.drawOval((int)Math.round(p.x - size/2d), (int)Math.round(p.y - size/2d), size, size);
	}
	
	protected Point2d transPoint(Point2d p){
		double x = (p.x - centerPoint.x)*zoom;
		double y = (p.y - centerPoint.y)*zoom;
		return new Point2d(canvasSize.x/2+x, canvasSize.y/2+y);
	}
	
	protected Point2d inverseTransPoint(Point2d p){
		double x = (p.x - canvasSize.x/2)/zoom;
		double y = (p.y - canvasSize.y/2)/zoom;
		return new Point2d(centerPoint.x+x, centerPoint.y+y);
	}
}
