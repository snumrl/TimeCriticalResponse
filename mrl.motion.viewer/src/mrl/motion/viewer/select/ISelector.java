package mrl.motion.viewer.select;

public interface ISelector {

	public void notifyChange();
	public void addListener(ISelectorListener listener);
	
}
