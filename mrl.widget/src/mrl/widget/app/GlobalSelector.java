package mrl.widget.app;

import java.util.ArrayList;
import java.util.HashMap;

public class GlobalSelector {

	public static GlobalSelector instance = new GlobalSelector();
	
	private ArrayList<GlobalSelectionListener> listeners = new ArrayList<GlobalSelectionListener>();
	private HashMap<String, Object> selectionData = new HashMap<String, Object>();
	
	private GlobalSelector(){
	}
	
	public void addListener(GlobalSelectionListener listener){
		listeners.add(listener);
	}
	
	public void changeSelection(String type, Object data){
		selectionData.put(type, data);
		for (GlobalSelectionListener listener : listeners){
			listener.onSelection(type);
		}
	}
	
//	public void setSelection(String type, Object data){
//		selectionData.put(type, data);
//	}
	
	public Object getSelection(String type){
		return selectionData.get(type);
	}
}
