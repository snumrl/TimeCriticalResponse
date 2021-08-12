package mrl.widget;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point3d;

public class ColorModel {

	private ArrayList<Point3d> colorList = new ArrayList<Point3d>();
	private ArrayList<Double> weightList = new ArrayList<Double>();
	private Random random = new Random(1561);
	private double currentWeight = 2;
	
	public ColorModel(){
//		addColor(new Point3d(1, 1, 1), 0.3);
//		addColor(new Point3d(0, 0, 0), 0.3);
	}
	
	public void addColor(Point3d color, double weight){
		colorList.add(color);
		weightList.add(weight);
	}
	
	private double dist(Point3d p1, Point3d p2){
		double x = (p1.x - p2.x)*Math.max(Math.max(p1.x, p2.x), 0.3);
		double y = (p1.y - p2.y)*Math.max(Math.max(p1.y, p2.y), 0.3);
		double z = (p1.z - p2.z)*Math.max(Math.max(p1.z, p2.z), 0.3);
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	public Point3d getNewColor(){
		int iteration = 0;
		while (true){
			Point3d c = new Point3d(random.nextDouble(), random.nextDouble(), random.nextDouble());
			boolean isDuplicated = false;
			for (int i = 0; i < colorList.size(); i++) {
				Point3d c2 = colorList.get(i);
				double weight = weightList.get(i);
				weight = Math.max(weight, currentWeight);
				if (dist(c,c2) < weight){
//					if (c2.distance(c) < weight){
					isDuplicated = true;
					break;
				}
			}
			
			iteration++;
			if (!isDuplicated){
				addColor(c, 0);
//				System.out.println(colorList.size() + "\t" + c);
				return c;
			} else {
				if (iteration > 10000){
					currentWeight = currentWeight*0.9;
//					System.out.println("weight : " + currentWeight);
					iteration = 0;
				}
			}
		}
	}
}
