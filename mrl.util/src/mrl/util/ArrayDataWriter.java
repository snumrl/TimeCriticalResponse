package mrl.util;

public class ArrayDataWriter {

	private double[] buffer;
	private int currentIndex;
	
	public ArrayDataWriter(int size){
		if (size < 0) size = 2000;
		
		buffer = new double[size];
	}
	
	public void append(double[] data){
		for (int i = 0; i < data.length; i++) {
			buffer[currentIndex + i] = data[i];
		}
		currentIndex += data.length;
	}
	
	public int getCurrentSize(){
		return currentIndex;
	}
	
	public double[] getData(){
		if (currentIndex == buffer.length){
			return buffer;
		} else {
			double[] temp = new double[currentIndex];
			System.arraycopy(buffer, 0, temp, 0, currentIndex);
			buffer = temp;
			return buffer;
		}
	}
}
