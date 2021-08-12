package mrl.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

public class TimeChecker {
	
	public HashMap<String, Long> timeMap = new HashMap<String, Long>();
	
	private long lastTime = -1;
	private String currentState;
	
	public static TimeChecker instance = new TimeChecker(false);
	
	public boolean enable;
	
	public TimeChecker() {
		this(true);
	}
	
	public TimeChecker(boolean enable) {
		this.enable = enable;
	}
	
	public void check(){
		lastTime = time();
	}
	public void check(String name){
		long currentTime = time();
		long elapsed = currentTime - lastTime;
		System.out.println("<time> " + name + " : " + elapsed/1000000);
		lastTime = currentTime;
	}

	public void state(String state){
		if (enable){
			long currentTime = time();
			if (currentState != null){
				long elapsed = currentTime - lastTime;
				Long time = timeMap.get(currentState);
				if (time == null) time = 0l;
				timeMap.put(currentState, time + elapsed);
			}
			
			currentState = state;
			lastTime = currentTime;
		}
	}
	
	public String toString(){
		String[] labels = timeMap.keySet().toArray(new String[timeMap.size()]);
		Arrays.sort(labels, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Long l1 = timeMap.get(o1);
				Long l2 = timeMap.get(o2);
				return l2.compareTo(l1);
			}
		});
		StringBuilder buffer = new StringBuilder();
		buffer.append("----------------------------\r\n");
		for (String label : labels){
			buffer.append(label + "\t" + (timeMap.get(label)/1000000) + "ms\r\n");
		}
		return buffer.toString();
	}
	
	public void print(){
		System.out.println(this);
	}
	
	public static String timeString(long time){
		long hour = time / (3600*1000);
		time -= hour * (3600*1000);
		long minute = time / (60 * 1000);
		time -= minute * (60*1000);
		long second = time / 1000;
		String str = "";
		if (hour > 0) str += hour  + "h ";
		if (minute > 0) str += minute  + "m ";
		str += second + "s";
		return str;
	}

	public static long time(){
		return System.nanoTime();
//		return System.nanoTime()/1000000;
	}
	
	public void merge(TimeChecker c, double weight){
		for (Entry<String, Long> entry : c.timeMap.entrySet()){
			Long t = timeMap.get(entry.getKey());
			if (t == null) t = 0l;
			timeMap.put(entry.getKey(), t + (long)(entry.getValue()*weight));
		}
	}
}
