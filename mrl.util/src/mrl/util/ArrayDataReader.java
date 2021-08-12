package mrl.util;

public class ArrayDataReader {

	private int index = 0;
	private double[] data;

	public ArrayDataReader(double[] data) {
		this.data = data;
	}
	
	public double read(){
		index++;
		return data[index-1];
	}
}
