package mrl.widget;

import java.util.ArrayList;

import javax.vecmath.Point3d;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.RGB;

public class ColorMapping {
	
	private Device device;
	private ColorModel colorModel = new ColorModel();
	private Color selectionColor;
	private ArrayList<Color> colorList = new ArrayList<Color>();

	public ColorMapping(Device device){
		this.device = device;
	}
	
	public ColorModel getColorModel() {
		return colorModel;
	}
	
	public void setSelectionColor(Point3d c){
		colorModel.addColor(c, 0);
		selectionColor = new Color(device, rgb(c));
	}
	
	public Color getSelectionColor() {
		return selectionColor;
	}

	public Color newColor(){
		Point3d c = colorModel.getNewColor();
		Color color = new Color(device, rgb(c));
		colorList.add(color);
		return color;
	}
	
	public Color addColor(Point3d c){
		Color color = new Color(device, rgb(c));
		colorList.add(color);
		return color;
	}
	
	public void dispose(){
		for (Color color : colorList){
			color.dispose();
		}
		selectionColor.dispose();
	}
	
	public int size(){
		return colorList.size();
	}
	
	public Color getColor(int index){
		while (index >= colorList.size()){
			newColor();
		}
		return colorList.get(index);
	}
	
	public static RGB rgb(Point3d c){
		return new RGB((int)(c.x*255), (int)(c.y*255), (int)(c.z*255));
	}
	public static Point3d point(RGB rgb){
		return new Point3d(rgb.red/255d, rgb.green/255d, rgb.blue/255d);
	}
}
