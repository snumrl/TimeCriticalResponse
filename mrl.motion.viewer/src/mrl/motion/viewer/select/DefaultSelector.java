package mrl.motion.viewer.select;

import java.util.ArrayList;

public class DefaultSelector implements ISelector{
	
	private ArrayList<ISelectorListener> listeners = new ArrayList<ISelectorListener>();

	@Override
	public void notifyChange() {
		for (ISelectorListener listener : listeners){
			listener.onChanged();
		}
	}

	@Override
	public void addListener(ISelectorListener listener) {
		listeners.add(listener);
	}

	
}
