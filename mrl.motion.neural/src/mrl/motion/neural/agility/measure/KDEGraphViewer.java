package mrl.motion.neural.agility.measure;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.vecmath.Point3d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import mrl.motion.neural.agility.measure.WeightedKDE.WeightedKDESample;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.ColorMapping;
import mrl.widget.ScalableCanvas2D;

public class KDEGraphViewer extends ScalableCanvas2D{

	private String[] labels;
	private WeightedKDE[] kdeList;
	public double[] positionStat;
	public double[] weightStat;
	private ColorMapping cm;
	public boolean drawSamplePoints = false;
	public boolean showLegend = true;
	public ArrayList<Double> xGuideList = new ArrayList<Double>();
	public ArrayList<Double> yGuideList = new ArrayList<Double>();
	public int topRightLabelAlign = 1;
	
	public KDEGraphViewer(Composite parent) {
		super(parent);
		cm = new ColorMapping(getDisplay());
		cm.getColorModel().addColor(new Point3d(1, 1, 1), 0.3);
	}
	
	public void setKde(String[] labels, WeightedKDE[] kdeList) {
		this.labels = labels;
		this.kdeList = kdeList;
		ArrayList<Double> posList = new ArrayList<Double>();
		ArrayList<Double> weightList = new ArrayList<Double>();
		for (WeightedKDE kde : kdeList) {
			LinkedList<WeightedKDESample> sampleList = kde.getSampleList();
			for (int i = 0; i < sampleList.size(); i++) {
				posList.add(sampleList.get(i).position[0]);
				weightList.add(sampleList.get(i).weight);
			}
		}
		
		
		positionStat = MathUtil.getStatistics(Utils.toDoubleArray(posList));
		weightStat = MathUtil.getStatistics(Utils.toDoubleArray(weightList));
		if (WeightedKDE.USE_PROBABILITY || WeightedKDE.USE_POS_PROBABILITY) {
			weightStat[2] = 0;
		}
		System.out.println("data stats :: " + Utils.toString(positionStat, weightStat));
	}
	
	public double calcLimitOffset(double[] pos) {
		return 0;
	}
	
	protected Rectangle getContentBoundary(){ 
		return new Rectangle(-100, 0, xRange + 300, yRange + 20*fontScale); 
	};

	int xRange = 800;
	int yRange = 800;
	int fontScale = 2;
	@Override
	protected void drawContents(GC gc) {
		int pointCount = 100;
		
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		drawLine(gc, 0, 0, 0, yRange);
		drawLine(gc, 0, yRange, xRange, yRange);
		
		if (drawSamplePoints) {
			for (int kIdx = 0; kIdx < kdeList.length; kIdx++) {
				gc.setForeground(cm.getColor(kIdx));
				gc.setBackground(cm.getColor(kIdx));
				for (WeightedKDESample s : kdeList[kIdx].getSampleList()) {
					double pRatio = ratio(s.position[0], positionStat);
					double wRatio = ratio(s.weight, weightStat);
					double x = pRatio*xRange;
					double y = yRange - wRatio*yRange;
					double r = 3;
//					gc.drawPoint(MathUtil.round(x), MathUtil.round(y));
//					fillOval(gc, x-r, y-r, r*2, r*2);
					drawOval(gc, x-r, y-r, r*2, r*2);
				}
			}
		}
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		double[][] pointList = new double[pointCount][];
		for (int kIdx = 0; kIdx < kdeList.length; kIdx++) {
			for (int pIdx = 0; pIdx < pointCount; pIdx++) {
				double pRatio = pIdx/(double)pointCount;
				double position = value(pRatio, positionStat);
				double weight = kdeList[kIdx].getEstimatedWeight(new double[] { position });
				double wRatio = ratio(weight, weightStat);
				
				double x = pRatio*xRange;
				double y = yRange - wRatio*yRange;
				pointList[pIdx] = new double[] { x, y };
			}
			
			gc.setLineWidth(3);
			gc.setForeground(cm.getColor(kIdx));
//			gc.setLineWidth(4);
//			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLUE));
			for (int i = 0; i < pointList.length-1; i++) {
				drawLine(gc, pointList[i][0], pointList[i][1], pointList[i+1][0], pointList[i+1][1]);
			}
			gc.setLineWidth(1);
		}
		
		gc.setFont(new Font(getDisplay(), gc.getFont().getFontData()[0].getName(), fontScale*20, SWT.NONE));
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		
		if (WeightedKDE.USE_PROBABILITY) {
			drawXLabel(gc, "0", xRange, yRange, ratio(0, positionStat), 0);
			drawXLabel(gc, "0.5", xRange, yRange, ratio(15, positionStat), 0);
			drawXLabel(gc, "1", xRange, yRange, ratio(30, positionStat), 0);
		} else {
			drawXLabel(gc, xRange, yRange, 0);
			drawXLabel(gc, xRange, yRange, 1);
			double zRatio = ratio(0, positionStat);
			if (zRatio > 0.2 && zRatio < 0.8) {
				drawXLabel(gc, xRange, yRange, ratio(0, positionStat));
			} else {
				drawXLabel(gc, xRange, yRange, 0.5);
				drawXLabel(gc, xRange, yRange, 0.25);
				drawXLabel(gc, xRange, yRange, 0.75);
			}
		}
		
//		drawYLabel(gc, xRange, yRange, 0);
//		drawYLabel(gc, xRange, yRange, 1);
//		zRatio = ratio(0, weightStat);
//		if (zRatio > 0.2 && zRatio < 0.8) {
//			drawYLabel(gc, xRange, yRange, ratio(0, weightStat));
//		} else {
//			drawYLabel(gc, xRange, yRange, 0.5);
//			drawYLabel(gc, xRange, yRange, 0.25);
//			drawYLabel(gc, xRange, yRange, 0.75);
//		}
		
//		for (int i = 10; i < 35; i+=10) {
//			drawYLabel(gc, xRange, yRange, ratio(i, weightStat));
//		}
		
		drawYLabel(gc, "0", xRange, yRange, ratio(0, weightStat));
		drawYLabel(gc, "0.5", xRange, yRange, ratio(15, weightStat));
		drawYLabel(gc, "1", xRange, yRange, ratio(30, weightStat));
		
		
		
//		for (int i = 15; i < weightStat[3]; i+=15) {
//			drawYLabel(gc, xRange, yRange, ratio(i, weightStat));
//		}
		
		if (showLegend) {
			for (int i = 0; i < labels.length; i++) {
				gc.setForeground(cm.getColor(i));
				drawString(gc, labels[i], xRange, 30 + i*40*fontScale, topRightLabelAlign, -1);
			}
		}
		
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
		for (double xGuideLine : xGuideList) {
			double ratio = ratio(xGuideLine, positionStat);
			drawXLabel(gc, xRange, yRange, ratio, yRange);
		}
		
		gc.setLineWidth(3);
		pointCount = 200;
		for (double yGuideLine : yGuideList) {
			for (int i = 0; i < pointCount-1; i++) {
				double x1;
				double y1;
				{
					double pRatio = i/(double)pointCount;
					double position = value(pRatio, positionStat);
					double weight = yGuideLine;
					weight += calcLimitOffset(new double[] { position });
					double wRatio = ratio(weight, weightStat);
					x1 = pRatio*xRange;
					y1 = yRange - wRatio*yRange;
					
				}
				double x2;
				double y2;
				{
					double pRatio = (i+1)/(double)pointCount;
					double position = value(pRatio, positionStat);
					double weight = yGuideLine;
					weight += calcLimitOffset(new double[] { position });
					double wRatio = ratio(weight, weightStat);
					x2 = pRatio*xRange;
					y2 = yRange - wRatio*yRange;
				}
				
				
				drawLine(gc, x1, y1, x2, y2);
			}
			double ratio = ratio(yGuideLine, weightStat);
			drawYLabel(gc, xRange, yRange, ratio, 0);
//			double ratio = ratio(yGuideLine, weightStat);
//			drawYLabel(gc, xRange, yRange, ratio, xRange);
		}
	}
	
	String format = "%.2f";
	private void drawXLabel(GC gc, int xRange, int yRange, double ratio) {
		drawXLabel(gc, xRange, yRange, ratio, 0);
	}
	private void drawXLabel(GC gc, int xRange, int yRange, double ratio, double lengthOffset) {
		String value = String.format(format, value(ratio, positionStat));
		drawXLabel(gc, value, xRange, yRange, ratio, lengthOffset);
	}
	private void drawXLabel(GC gc, String label, int xRange, int yRange, double ratio, double lengthOffset) {
		int xPos = MathUtil.round(ratio*xRange);
		int xAlign = (ratio == 0) ? -1 : 0;
		drawString(gc, label, xPos, yRange + 10, xAlign, 1);
		drawLine(gc, xPos, yRange-5 - lengthOffset, xPos, yRange+1);
	}
	
	private void drawYLabel(GC gc, String label, int xRange, int yRange, double ratio) {
		drawYLabel(gc, label, xRange, yRange, ratio, 0);
	}
	private void drawYLabel(GC gc, int xRange, int yRange, double ratio) {
		String value = String.format(format, value(ratio, weightStat));
		drawYLabel(gc, value, xRange, yRange, ratio, 0);
	}
	private void drawYLabel(GC gc, int xRange, int yRange, double ratio, double lengthOffset) {	
		String value = String.format(format, value(ratio, weightStat));
		drawYLabel(gc, value, xRange, yRange, ratio, lengthOffset);
	}
	private void drawYLabel(GC gc, String label, int xRange, int yRange, double ratio, double lengthOffset) {
		int yPos = MathUtil.round(yRange - ratio*yRange);
		if (lengthOffset < 100) {
			int yAlign = (ratio == 0) ? -1 : 0;
			drawString(gc, label, -12, yPos, 1, yAlign);
		}
		drawLine(gc, -5, yPos, 5 + lengthOffset, yPos);
	}
	
	private double ratio(double value, double[] stat) {
		return (value - stat[2])/(stat[3] - stat[2]);
	}
	private double value(double ratio, double[] stat) {
		return stat[2] + ratio*(stat[3] - stat[2]);
	}
}
