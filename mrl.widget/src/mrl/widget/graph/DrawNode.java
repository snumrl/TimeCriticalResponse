package mrl.widget.graph;

import java.util.ArrayList;

import javax.vecmath.Point2d;

import org.eclipse.swt.SWT;

public class DrawNode {

	public Object data;
	
	public int index;
	public Point2d position;
	public ArrayList<DrawNode> relatedNodes = new ArrayList<DrawNode>();
	
	public int color = SWT.COLOR_RED;
	public double radius = 15;
	public String label = null;
	
	public DrawNode(){
	}
	
	
}
