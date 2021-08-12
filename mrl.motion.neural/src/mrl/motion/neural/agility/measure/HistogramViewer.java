package mrl.motion.neural.agility.measure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import mrl.util.MathUtil;
import mrl.util.Pair;
import mrl.util.Utils;
import mrl.widget.ScalableCanvas2D;

public class HistogramViewer extends ScalableCanvas2D{

	private HData[] dataList;
	private String[] labels;
	private int drawSize;
	private double maxValue;
	
	public HistogramViewer(Composite parent) {
		super(parent);
	}
	
	public void setHistogram(HistogramData histogram, String[] labels) {
		this.labels = labels;
		ArrayList<HData> dList = new ArrayList<HData>();
		maxValue = -1;
		for (Entry<Pair<Integer, Integer>, ArrayList<Double>> entry : histogram.histogram.entrySet()) {
			Pair<Integer, Integer> key = entry.getKey();
			if (key.second == 0) continue;
			HData data = new HData();
			data.key = new int[] { key.first, key.second };
			data.limit = histogram.limitMap.get(key);
			maxValue = Math.max(maxValue, data.limit);
			ArrayList<Double> filteted = new ArrayList<Double>();
			for (double v : entry.getValue()) {
				if (v > 60) continue;
				filteted.add(v);
			}
			data.values = Utils.toDoubleArray(filteted);
//			data.values = Utils.toDoubleArray(entry.getValue());
			data.checkFailRatio();
			dList.add(data);
		}
		dataList = Utils.toArray(dList);
		Arrays.sort(dataList);
		drawSize = Math.min(20, dataList.length);
		for (int i = 0; i < drawSize; i++) {
			HData data = dataList[i];
			System.out.println(Arrays.toString(data.key) + " : " + data.values.length + " : " + data.failRatio + " : " + data.limit + " : " + Arrays.toString(data.stat));
		}
	}
		

	@Override
	protected void drawContents(GC gc) {
		if (dataList == null) return;
		
		
		int barWidth =  1200;
		int barHeight = 50;
		double wRatio = barWidth/maxValue;
		drawLine(gc, 0, 0, 0, barHeight*drawSize);
		
		gc.setFont(new Font(getDisplay(), gc.getFont().getFontData()[0].getName(), 20, SWT.NONE));
		for (int i = 0; i < drawSize; i++) {
			HData data = dataList[i];
			String label = labels[data.key[0]] + "->" + labels[data.key[1]];
			Point tSize = gc.stringExtent(label);
			double minX = data.stat[2]*wRatio;
			double maxX = data.stat[3]*wRatio;
			double midX = data.stat[0]*wRatio;
			double limitX = data.limit*wRatio;
			
//			minX = midX - data.stat[1]*wRatio;
//			maxX = midX + data.stat[1]*wRatio;
			
			drawLine(gc, 0, i*barHeight + barHeight*0.5, limitX, i*barHeight + barHeight*0.5);
			
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_GRAY));
			fillRectangle(gc, minX, i*barHeight, maxX - minX, barHeight - 2);
			
			double bMargin = 2;
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLUE));
			fillRectangle(gc, midX-bMargin, i*barHeight, bMargin*2, barHeight - 2);
			
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
			fillRectangle(gc, limitX-bMargin, i*barHeight, bMargin*2, barHeight - 2);
			
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
			gc.drawString(label, -tSize.x - 10, (int)(i*barHeight + barHeight*0.5 - tSize.y/2));
		}
	}
	
	public static class HData implements Comparable<HData>{
		public int[] key;
		public double limit;
		public double[] values;
		
		public double failRatio;
		public double[] stat;
		public double maxOver;
		
		public void checkFailRatio() {
			int fCount = 0;
			maxOver = -Integer.MAX_VALUE;
			for (double d : values) {
				if (d > limit) {
					fCount++;
				}
				maxOver = Math.max(maxOver, d - limit);
			}
			failRatio = fCount/(double)values.length;
			stat = MathUtil.getStatistics(values);
		}

		@Override
		public int compareTo(HData o) {
			int c =  -Double.compare(failRatio, o.failRatio);
			if (c == 0) {
				return -Double.compare(maxOver, o.maxOver);
			}
			return c;
		}
	}
}
