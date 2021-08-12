package mrl.util;

public class MeanCounter {

	private double sum = 0;
	private int count = 0;
	private double max;
	private double min;
	
	public MeanCounter(){
	}
	
	public void add(double v){
		if (count == 0){
			min = max = v;
		} else {
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		sum += v;
		count++;
	}
	
	public double mean(){
		return sum/count;
	}
	
	public String toString(){
		return "MeanCounter :: " + Utils.toString(mean(), min, max);
	}
}

