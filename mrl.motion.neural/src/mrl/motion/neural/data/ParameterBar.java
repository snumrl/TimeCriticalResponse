package mrl.motion.neural.data;

import java.util.ArrayList;
import java.util.Arrays;

import mrl.util.MathUtil;
import mrl.util.Utils;

public abstract class ParameterBar {
	
	public static boolean ZERO_BASE = false;
	
	protected ArrayList<Double> valueList = new ArrayList<Double>();
	
	protected double min;
	protected double max;
	protected int barSize;
	protected double thumbSize;
	
	protected double interval;
	protected double thumbLen;
	
	protected ParameterBar(double min, double max, int barSize, double thumbSize) {
		this.min = min;
		this.max = max;
		this.barSize = barSize;
		this.thumbSize = thumbSize;
		
		interval = (max - min)/barSize;
		thumbLen = (thumbSize + 1)/2*interval;
	}

	public abstract double[] getBar(double value);
	
	public void printStatistics(){
		System.out.println("statistics : " + Arrays.toString(MathUtil.getStatistics(Utils.toDoubleArray(valueList))));
	}

	public static class AngleBar extends ParameterBar{

		public AngleBar(int barSize, double thumbSize) {
			super(-Math.PI, Math.PI, barSize, thumbSize);
		}

		@Override
		public double[] getBar(double value) {
			valueList.add(value);
			
			double[] bar = new double[barSize];
			
			for (int i = 0; i < bar.length; i++) {
				double start = min + interval*i;
				double end = start + interval;
				double mid = (start + end)/2;
				
				double dist = Math.abs(value - mid);
				dist = Math.min(dist, Math.abs(value + Math.PI*2 - mid));
				dist = Math.min(dist, Math.abs(value - Math.PI*2 - mid));
				double activation = Math.min(Math.max((thumbLen - dist)/interval, 0), 1);
				if (!ZERO_BASE) activation = activation*2 - 1;
				bar[i] = activation;
			}
			return bar;
		}
	}
	
	public static class LengthBar extends ParameterBar{

		public LengthBar(double min, double max, int barSize, double thumbSize) {
			super(min, max, barSize, thumbSize);
		}

		@Override
		public double[] getBar(double value) {
			valueList.add(value);
			
			double[] bar = new double[barSize];
			value = Math.min(max, Math.max(min, value));
			
			for (int i = 0; i < bar.length; i++) {
				double start = min + interval*i;
				double end = start + interval;
				double mid = (start + end)/2;
				
				double dist = Math.abs(value - mid);
				double activation = Math.min(Math.max((thumbLen - dist)/interval, 0), 1);
				if (!ZERO_BASE) activation = activation*2 - 1;
				bar[i] = activation;
			}
			return bar;
		}
	}
}
