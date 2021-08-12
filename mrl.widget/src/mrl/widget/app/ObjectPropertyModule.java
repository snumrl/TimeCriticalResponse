package mrl.widget.app;

import mrl.widget.ObjectPropertyPanel;
import mrl.widget.app.MainApplication.WindowPosition;

public class ObjectPropertyModule extends Module{
	
	private ObjectPropertyPanel panel;
	private Class<?> type;
	
	public ObjectPropertyModule(Class<?> type) {
		this.type = type;
	}

	@Override
	protected void initializeImpl() {
		panel = addWindow(new ObjectPropertyPanel(dummyParent(), type), WindowPosition.Right);
	}

	public ObjectPropertyPanel getPanel() {
		return panel;
	}
	
	public void setObject(Object object) {
		panel.setObject(object);
	}
}
