package mrl.motion.viewer.module;

import java.util.ArrayList;
import java.util.List;

public class TimeBasedList<E> extends ArrayList<E>{

	private static final long serialVersionUID = 5694459232069659458L;

	
	public TimeBasedList(){
		
	}
	public TimeBasedList(List<E> list){
		for (E e : list){
			add(e);
		}
	}
}
