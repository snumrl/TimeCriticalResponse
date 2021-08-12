package mrl.widget.app;

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import org.eclipse.swt.widgets.TreeItem;

public class Item {
	
	public static Vector3d baseColor = new Vector3d(0.8, 0.8, 0.8);
	public static Vector3d selectionColor = new Vector3d(0.8, 0.8, 0.2);
	
	protected String label;
	protected Item parent;
	protected ArrayList<Item> children = null;
	protected boolean isVisible = true;
	protected Object data;
	protected ItemDescription description;
	protected boolean selected;

	public Item(Object data){
		this.data = data;
	}
	
	public Object getData(){
		return data;
	}

	public void setParent(Item parent) {
		this.parent = parent;
	}

	public Item getParent(){
		return null;
	}
	
	public void addChild(Item item){
		if (children == null) children = new ArrayList<Item>();
		children.add(item);
	}
	
	public void removeChild(Item item){
		children.remove(item);
	}
	
	
	public ArrayList<Item> getChidren(){
		return children;
	}
	
	public void onDoubleClick(){
		
	}
	
	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel(){
		return label;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}
	
	public void setDescription(ItemDescription description) {
		if (description == null) {
			description = new ItemDescription();
		}
		this.description = description;
		description.item = this;
	}
	
	public ItemDescription getDescription(){
		if (description == null) {
			description = new ItemDescription();
			description.item = this;
		}
		return description;
	}
	
	protected void setSelected(boolean selected){
		this.selected = selected;
	}

	public boolean isSelected(){
		return selected;
	}
	
	public static class ItemDescription{
		public Item item;
		public Vector3d color;
		public double size = -1;
		
		public ItemDescription(){
		}
		
		public Vector3d getColor(){
			return getColor(baseColor);
		}
		public Vector3d getColor(Vector3d defaultColor){
			if (item != null && item.isSelected()) return selectionColor;
			if (color == null) return defaultColor;
			return color;
		}
		public double getSize(double defaultSize){
			if (size < 0) return defaultSize;
			return size;
		}

		public ItemDescription(Vector3d color, double size) {
			this.color = color;
			this.size = size;
		}

		public ItemDescription(Vector3d color) {
			this.color = color;
		}
		public ItemDescription(double size) {
			this.size = size;
		}
		
		public static ItemDescription red() {
			return new ItemDescription(new Vector3d(1, 0, 0));
		}
	}
	
}
