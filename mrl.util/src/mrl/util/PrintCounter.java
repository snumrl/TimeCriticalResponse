package mrl.util;

public class PrintCounter {

	private int interval;
	private int printedCount = -1;
	private int currentCount = -1;

	public PrintCounter(int interval) {
		this.interval = interval;
	}
	
	public boolean print(int count, String str){
		boolean print = ((printedCount/interval) != (count/interval));
		if (printedCount < 0) print = true;
		if (print){
			printedCount = (count/interval)*interval;
			System.out.println(printedCount + " :: " + str);
		}
		return print;
	}
	
	public boolean print(String str){
		currentCount++;
		return print(currentCount, str);
	}
}
