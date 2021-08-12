package mrl.motion.viewer.module;

import mrl.widget.app.Item.ItemDescription;

public class DescriptedData {

	public Object data;
	public ItemDescription description;
	
	public DescriptedData(Object data, ItemDescription description) {
		this.data = data;
		this.description = description;
	}
}
