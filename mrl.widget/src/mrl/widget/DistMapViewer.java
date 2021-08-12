package mrl.widget;

import javax.vecmath.Point2d;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

public class DistMapViewer extends ScalableCanvas2D{
	
	private double[][] distMap;
	private boolean[][] checkMap;
	private boolean[][] checkMap2;
	private boolean[][] checkMap3;
	private double maxDistance;
	
	public DistMapViewer(Composite parent) {
		super(parent);
		
		useAntialias = false;
		
		this.setLayout(new FillLayout());
	}
	

	
	public void setDistMap(double[][] distMap, boolean[][] checkMap, double maxDistance) {
		this.distMap = distMap;
		this.checkMap = checkMap;
		this.maxDistance = maxDistance;
		
		if (distMap != null){
			double min = Integer.MAX_VALUE;
			double max = 0;
			for (double[] list : distMap){
				for (double d : list){
					min = Math.min(min, d);
					max = Math.max(max, d);
				}
			}
			System.out.println("dist stat : " + min + " / " + max);
			if (maxDistance <= 0) this.maxDistance = max/2;
		}
		getDisplay().timerExec(500, new Runnable() {
			@Override
			public void run() {
				fitToScreen();
				redrawCanvas();
			}
		});
		
	}
	
	public void setCheckMap2(boolean[][] checkMap2) {
		this.checkMap2 = checkMap2;
	}
	public void setCheckMap3(boolean[][] checkMap3) {
		this.checkMap3 = checkMap3;
	}


	@Override
	protected void drawContents(final GC gc) {
		if (distMap == null && checkMap == null) return;
		
		int size = distMap != null ? distMap.length : checkMap.length;
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Color rgb = null;
				if (checkMap3 != null && checkMap3[i][j]){
					rgb = new Color(getDisplay(), 255, 255, 0);
				} else if (checkMap2 != null && checkMap2[i][j]){
					rgb = new Color(getDisplay(), 0, 255, 0);
				} else if (checkMap != null && checkMap[i][j]){
					rgb = new Color(getDisplay(), 255, 0, 0);
				} else if (distMap != null){
					double d = distMap[i][j];
					int c =  (int)(Math.min(d/maxDistance, 1) * 255);
					rgb = new Color(getDisplay(), c, c, c);
				} else {
					int c = 255;
					rgb = new Color(getDisplay(), c, c, c);
				}
				gc.setForeground(rgb);
				gc.drawPoint(i, j);
				rgb.dispose();
			}
		}
	}
	
	@Override
	protected boolean onMouseDown(Point2d p, MouseEvent e) {
		if (distMap == null && checkMap == null) return false;
		
		try{
			int x = (int)p.x;
			int y = (int)p.y;
			System.out.println("mousedown : " + x + " , " + y);
			int margin = 4;
			for (int i = -margin; i <=margin; i++) {
				for (int j = -margin; j <=margin; j++) {
					if (checkMap != null && checkMap[x+j][y+i]) System.out.print("*");
					System.out.print(String.format("%.3f", distMap[x + j][y + i]) + "\t");
				}
				System.out.println();
			}
		} catch (RuntimeException ex){
		}
		return false;
	}

	@Override
	protected boolean onMouseMove(Point2d p, MouseEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean onMouseUp(Point2d p, MouseEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean onMouseDoubleClick(Point2d p, MouseEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean onMouseScrolled(Point2d p, MouseEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Rectangle getContentBoundary() {
		if (distMap == null && checkMap == null) return null;
		int size = distMap != null ? distMap.length : checkMap.length;
		return new Rectangle(0, 0, size, size);
	}

	@Override
	protected void onBoundarySelection(Rectangle2d boundary, MouseEvent e) {
		System.out.println("boundary : " + boundary);
	}
}