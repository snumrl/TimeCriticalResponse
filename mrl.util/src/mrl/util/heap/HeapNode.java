package mrl.util.heap;

public abstract class HeapNode {

	public int index;
	
	public double value;
	
	public boolean validate(){
		return true;
	}
	
	public void onRemoveByValidation(){
	}
}
