package mrl.util;

public class SortableIndexValue implements Comparable<SortableIndexValue>{
	
	public int index;
	public double value;
	
	public SortableIndexValue(int index, double value) {
		this.index = index;
		this.value = value;
	}

	@Override
	public int compareTo(SortableIndexValue o) {
		return Double.compare(value, o.value);
	}

}
