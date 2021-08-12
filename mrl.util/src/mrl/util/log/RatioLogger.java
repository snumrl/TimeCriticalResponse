package mrl.util.log;

import mrl.util.Utils;

public class RatioLogger {

	public int trueCount = 0;
	public int falseCount = 0;
	
	public void log(boolean value) {
		if (value) {
			trueCount++;
		} else {
			falseCount++;
		}
	}
	
	public void print(String prefix) {
		String str = "";
		if (prefix != null) {
			str += prefix + " : ";
		}
		double sum = (trueCount + falseCount)/100d;
		str += Utils.toString(trueCount/sum, falseCount/sum, trueCount, falseCount);
		System.out.println(str);
	}

}
