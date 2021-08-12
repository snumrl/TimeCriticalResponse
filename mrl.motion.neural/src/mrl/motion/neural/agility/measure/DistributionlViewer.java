package mrl.motion.neural.agility.measure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

import javax.vecmath.Point3d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import mrl.motion.neural.agility.measure.WeightedKDE.WeightedKDESample;
import mrl.util.MathUtil;
import mrl.util.Pair;
import mrl.util.Utils;
import mrl.widget.ColorMapping;
import mrl.widget.ScalableCanvas2D;

public class DistributionlViewer extends ScalableCanvas2D{

	private double[][] dataList;
	private String[] labels;
	public double maxValue;
	public double limitValue;
	
	public boolean showYAxis = true;
	
	private ColorMapping cm;
	
	public DistributionlViewer(Composite parent) {
		super(parent);
		cm = new ColorMapping(getDisplay());
		cm.getColorModel().addColor(new Point3d(1, 1, 1), 0.3);
	}
	
	public void setDistribution(String[] labels, double[][] dataList) {
		this.labels = labels;
		this.dataList = dataList;
		maxValue = -1;
		for (double[] data : dataList) {
			maxValue = Math.max(maxValue, data[3]);
		}
//		weightBound = new double[] { 0, maxValue };
	}
		

	int xRange = 1000;
	int yRange = 1000;
	@Override
	protected void drawContents(GC gc) {
		if (dataList == null) return;
		
		
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		if (showYAxis) {
			drawLine(gc, 0, 0, 0, yRange);
			drawLine(gc, 0, yRange, xRange, yRange);
		} else {
			drawLine(gc, -500, yRange, xRange + 500, yRange);
		}
		
		
		int columnWidth = 150;
		int maxHeight = yRange - 200;
		double vRatio = maxHeight/maxValue;
		System.out.println("max value : " + maxValue + " : " + vRatio);
		
		double limitY  =  vRatio*limitValue;
		gc.setLineWidth(4);
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
		drawLine(gc, 0-5, yRange-limitY, (xRange+5)*0.75, yRange-limitY);
		
		
		for (int i = 0; i < dataList.length; i++) {
			double[] data = dataList[i];
			int barX = columnWidth*(i+1);
			double barY = vRatio*data[0];
			double barHeight = 21;
			double barWidth = barHeight*2;
			gc.setForeground(cm.getColor(i));
			gc.setLineWidth(2);
			gc.setBackground(cm.getColor(i));
			
			double minY  =  vRatio*data[2];
			double maxY  =  vRatio*data[3];
			drawLine(gc, barX, yRange-minY, barX, yRange-maxY);
			fillOval(gc, barX-barHeight/2, yRange-(barY+barHeight/2), barHeight, barHeight);
			
			gc.setLineWidth(4);
			drawLine(gc, barX-barWidth/2, yRange-maxY, barX+barWidth/2, yRange-maxY);
			fillRectangle(gc, barX-barHeight/2, yRange-(minY+barHeight/2), barHeight, barHeight);
			
			
//			fillRectangle(gc, barX-barWidth/2, yRange-(barY+barHeight/2), barWidth, barHeight);
//			
//			drawLine(gc, barX-barWidth/2, yRange-barY, barX+barWidth/2, yRange-barY);
//			barWidth -= 2;
//			
//			gc.setLineWidth(1);
//			double minY  =  vRatio*data[2];
//			drawLine(gc, barX-barWidth/2, yRange-minY, barX+barWidth/2, yRange-minY);
//			double maxY  =  vRatio*data[3];
//			drawLine(gc, barX-barWidth/2, yRange-maxY, barX+barWidth/2, yRange-maxY);
//			drawLine(gc, barX, yRange-minY, barX, yRange-maxY);
			
//			double limitY  =  vRatio*data[4];
//			gc.setLineWidth(4);
//			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
//			drawLine(gc, barX-barWidth/2, yRange-limitY, barX+barWidth/2, yRange-limitY);
			
		}
		
		
		
		gc.setLineWidth(1);
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.setFont(new Font(getDisplay(), gc.getFont().getFontData()[0].getName(), fontScale*20, SWT.NONE));
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		if (showYAxis) {
			for (int i = 0; i <= 60; i+=15) {
				drawYLabel(gc, String.format("%.1f", i/30d), xRange, yRange, yRange - vRatio*i);
			}
		}
//		for (int i = 0; i < labels.length; i++) {
//			drawXLabel(gc, labels[i], xRange, yRange, columnWidth*(i+1));
//		}
		
		
	}
	int fontScale = 2;
	String format = "%.2f";
	private void drawXLabel(GC gc, String label, int xRange, int yRange, double xPos) {
		drawString(gc, label, xPos, yRange + 10, 0, 1);
		drawLine(gc, xPos, yRange-5, xPos, yRange+1);
	}
	private void drawYLabel(GC gc, String label, int xRange, int yRange, double yPos) {
		drawString(gc, label, -12, yPos, 1, 0);
		drawLine(gc, -5, yPos, 5, yPos);
	}
	
	protected Rectangle getContentBoundary(){ 
		return new Rectangle(-100, 0, xRange + 300, yRange + 20*fontScale); 
	};
}
