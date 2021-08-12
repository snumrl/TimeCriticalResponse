package mrl.motion.annotation;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MotionAnnotation;
import mrl.widget.ColorModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import static mrl.motion.annotation.MotionAnnotationHelper.isBallContact;

public class MotionAnnotationTimeline extends Composite implements PaintListener, MouseListener{
	
	private Canvas canvas;
	private ArrayList<MotionAnnotation> annotationList;
	private ArrayList<MotionAnnotation> subAnnotationList;
	
	/**
	 * start from 1
	 */
	private int selectedPerson;
	private MotionAnnotation selectedAnnotation;
	private MotionAnnotation onMouseAnnotation;
	
	private int personCount;
	private int motionIndex = -1;
	private int maxFrame;

	private boolean isRedrawCalled = false;
	private GC gc;
	
	private int width;
	private int height;
	
	private ColorModel colorModel = new ColorModel();
	private HashMap<String, Color> colorMap = new HashMap<String, Color>();
	private Color selectionColor;
	
	public MotionAnnotationTimeline(Composite parent) {
		super(parent, SWT.NONE);
		
		this.setLayout(new FillLayout());
		
		canvas = new Canvas(this, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		canvas.addPaintListener(this);
		
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				for (Color color : colorMap.values()){
					color.dispose();
				}
				selectionColor.dispose();
			}
		});
		
		canvas.addMouseListener(this);
		
		Point3d rgb = new Point3d(1, 1, 0);
		colorModel.addColor(rgb, 0);
		selectionColor = new Color(getDisplay(), (int)(rgb.x*255), (int)(rgb.y*255), (int)(rgb.z*255));
	}
	
	private Color getColor(String type){
		Color color = colorMap.get(type);
		if (color == null){
			Point3d rgb = colorModel.getNewColor();
			color = new Color(getDisplay(), (int)(rgb.x*255), (int)(rgb.y*255), (int)(rgb.z*255));
			colorMap.put(type, color);
		}
		return color;
	}
	
	public void selectAnnotation(MotionAnnotation annotation) {
		this.selectedAnnotation = annotation;
		notifyListeners(SWT.Selection, new Event());
		callRedraw();
	}
	
	public MotionAnnotation getOnMouseAnnotation() {
		return onMouseAnnotation == selectedAnnotation ? onMouseAnnotation : null;
	}

	/**
	 * start from 1
	 * @return
	 */
	public int getSelectedPerson() {
		return selectedPerson;
	}
	
	public void setSelectedPerson(int selectedPerson) {
		this.selectedPerson = selectedPerson;
	}

	public MotionAnnotation getSelectedAnnotation() {
		return selectedAnnotation;
	}

	public ArrayList<MotionAnnotation> getAnnotationList() {
		return annotationList;
	}

	public void setAnnotationList(ArrayList<MotionAnnotation> annotationList) {
		this.annotationList = annotationList;
	}

	public void setSubAnnotationList(ArrayList<MotionAnnotation> subAnnotationList) {
		this.subAnnotationList = subAnnotationList;
	}

	public Canvas getCanvas() {
		return canvas;
	}
	
	public int getMotionIndex() {
		return motionIndex;
	}

	public void setMotionIndex(int motionIndex) {
		this.motionIndex = motionIndex;
		callRedraw();
	}
	
	public void setMotionInfo(int personCount, int maxFrame) {
		this.personCount = personCount;
		this.maxFrame = maxFrame;
		this.selectedPerson = 1;
		this.selectAnnotation(null);
		callRedraw();
	}



	public void callRedraw(){
		if (!isRedrawCalled){
			isRedrawCalled = true;
			getDisplay().timerExec(30, new Runnable() {
				@Override
				public void run() {
					if (canvas.isDisposed()) return;
					canvas.redraw();
				}
			});
		}
		this.notifyListeners(SWT.Modify, new Event());
	}


	int baseX;
	int endX;
	double xRatio;

	@Override
	public void paintControl(PaintEvent e) {
		isRedrawCalled = false;
		
		Point size = canvas.getSize();
		width = size.x;
		height = size.y;
		
		gc = e.gc;
		gc.setAdvanced(true);
		gc.setAntialias(SWT.ON);
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_GRAY));
		gc.fillRectangle(0, 0, width, height);
		
		if (motionIndex < 0) return;
		
		int leftMargin = 30;
		int rightMargin = 90;
		
		baseX = leftMargin;
		endX = width - rightMargin;
		xRatio = (endX - baseX)/(double)(maxFrame-1);
		
		gc.setLineWidth(1);
		for (int i = 1; i <= personCount; i++) {
			if (i == selectedPerson){
				gc.setForeground(selectionColor);
			} else {
				gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			}
			int y = getBaseY(i);
			gc.drawLine(baseX, y, endX, y);
			gc.drawLine(baseX, y - 10, baseX, y + 10);
			gc.drawLine(endX, y - 10, endX, y + 10);
		}
		
		gc.setLineWidth(1);
		if (annotationList != null){
//			ArrayList<MotionAnnotation> list = new ArrayList<MotionAnnotation>();
//			list.addAll(annotationList);
//			if (subAnnotationList != null){
//				list.addAll(subAnnotationList);
//			}
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			if (subAnnotationList != null) {
				for (MotionAnnotation annotation : subAnnotationList){
					if (isBallContact(annotation.type)) continue;
					drawAnnotation(gc, annotation, -1);
				}
			}
			
			for (MotionAnnotation annotation : annotationList){
				if (isBallContact(annotation.type)) continue;
				drawAnnotation(gc, annotation, 0);
			}
			
			for (MotionAnnotation annotation : annotationList){
				if (!isBallContact(annotation.type)) continue;
				int yOffset = annotation.type.equals(MotionAnnotationHelper.BALL_CONTACTS[0]) ? -1 : 1;
				drawAnnotation(gc, annotation, yOffset);
			}
		}
		
		
		int currentMotionPos = getXpos(motionIndex);
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.drawLine(currentMotionPos, 10, currentMotionPos, height - 10);
	}
	
	private void drawAnnotation(GC gc, MotionAnnotation annotation, int yOffset){
		int y = getBaseY(annotation.person);
		int startX = getXpos(annotation.startFrame)-1;
		int endX = getXpos(annotation.endFrame)-1;
		if (startX == endX){
			startX--;
			endX++;
		}
		
		if (annotation == selectedAnnotation){
			gc.setBackground(selectionColor);
		} else {
			gc.setBackground(getColor(annotation.type));
		}
		
		if (yOffset == 0){
			gc.fillRectangle(startX, y - 5, endX - startX, 11);
			gc.drawRectangle(startX, y - 5, endX - startX, 11);
		} else if (yOffset == -1){
			gc.fillRectangle(startX, y - 10, endX - startX, 10);
			gc.drawRectangle(startX, y - 10, endX - startX, 10);
		} else if (yOffset == 1){
			gc.fillRectangle(startX, y, endX - startX, 10);
			gc.drawRectangle(startX, y, endX - startX, 10);
		}
		
		if (annotation.interactionFrame >= 0){
			int x = getXpos(annotation.interactionFrame);
			gc.drawLine(x, y-8, x, y-5);
			gc.drawLine(x, y+9, x, y+6);
		}
		if (annotation.oppositePerson > 0){
			int oy = getBaseY(annotation.oppositePerson);
			int drawX = (y < oy) ? startX : endX;
			if (annotation.interactionFrame > 0){
				drawX = getXpos(annotation.interactionFrame)-1;
			}
			if (y < oy){
				gc.drawLine(drawX, y, drawX - 5, (y+oy)/2);
				gc.drawLine(drawX, oy, drawX - 5, (y+oy)/2);
			} else {
				gc.drawLine(drawX, y, drawX + 5, (y+oy)/2);
				gc.drawLine(drawX, oy, drawX + 5, (y+oy)/2);
			}
		}
	}
	
	private int getXpos(int motionIndex){
		return (int)Math.round(baseX + motionIndex*xRatio);
	}
	
	private int getBaseY(int personIndex){
		return (height * (personIndex)) / (personCount+1);
	}

	@Override
	public void mouseDown(MouseEvent e) {
		int person = findSelectedPerson(e.x, e.y);
		if (person > 0) selectedPerson = person;
		onMouseAnnotation = findSelectedAnnotation(person, e.x, e.y);
		if (onMouseAnnotation != null){
			selectAnnotation(onMouseAnnotation);
		} else {
			if (selectedAnnotation != null && selectedAnnotation.person != selectedPerson){
				selectAnnotation(null);
			} else {
				notifyListeners(SWT.Selection, new Event());
			}
		}
		callRedraw();
	}
	
	private MotionAnnotation findSelectedAnnotation(int person, int mouseX, int mouseY){
		if (person < 0) return null;
		int margin = 5;
		MotionAnnotation selected = null;
		int minDistance = Integer.MAX_VALUE;
		for (MotionAnnotation annotation : annotationList){
			if (annotation.person != person) continue;
			int startX = getXpos(annotation.startFrame);
			int endX = getXpos(annotation.endFrame);
			if (mouseX >= startX - margin && mouseX <= endX + margin){
				int distance = Math.abs(mouseX - (startX + endX)/2);
				if (distance < minDistance){
					selected = annotation;
					minDistance = distance;
				}
			}
		}
		return selected;
	}
	private int findSelectedPerson(int mouseX, int mouseY){
		for (int i = 1; i <= personCount; i++) {
			int y = getBaseY(i);
			if (Math.abs(y - mouseY) < 10){
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public void mouseUp(MouseEvent e) {
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent e) {
		this.notifyListeners(SWT.MouseDoubleClick, new Event());
	}
	
	
	public Vector3d getPersonColor(int person, int motionIndex){
		if (annotationList == null) return null;
		for (MotionAnnotation annotation : annotationList){
			if (annotation.person != person) continue;
			if (motionIndex >= annotation.startFrame && motionIndex <= annotation.endFrame){
				Color color = getColor(annotation.type);
				Vector3d c = new Vector3d(color.getRed(), color.getGreen(), color.getBlue());
				c.scale(1d/255);
				return c;
			}
		}
		return null;
	}

}
