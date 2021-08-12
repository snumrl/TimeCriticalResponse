package mrl.widget;

import static mrl.util.MathUtil.round;

import javax.vecmath.Point2d;

import mrl.util.Utils;

import org.eclipse.swt.graphics.Rectangle;

public class Rectangle2d {

	public double x;
	public double y;
	public double width;
	public double height;
	
	public Rectangle2d(){
		x = y = width = height = 0;
	}
	public Rectangle2d(Rectangle2d r){
		this(r.x, r.y, r.width, r.height);
	}
	
	public Rectangle2d(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public Rectangle swtRectangle(){
		return new Rectangle(round(x), round(y), round(width), round(height));
	}
	
	public boolean isInside(Point2d p){
		return p.x >= x && p.y >= y && p.x <= x + width && p.y <= y + height;
	}
	
	public boolean contains(Rectangle2d rect){
		return isInside(new Point2d(rect.x, rect.y)) && isInside(new Point2d(rect.x + rect.width, rect.y + rect.height));
	}
	
	public String toString(){
		return Utils.toString(x, y, width, height);
	}
	
	public void union(Rectangle2d r){
		double newX = Math.min(x, r.x);
		double newY = Math.min(y, r.y);
		
		width = Math.max(x + width, r.x + r.width) - newX;
		height = Math.max(y + height, r.y + r.height) - newY;
		
		x = newX;
		y = newY;
	}
	
	public static Rectangle2d union(Rectangle2d r1, Rectangle2d r2){
		if (r1 == null && r2 == null) return null;
		if (r1 == null) return new Rectangle2d(r2);
		if (r2 == null) return new Rectangle2d(r1);
		Rectangle2d r = new Rectangle2d(r1);
		r.union(r2);
		return r;
	}
}
