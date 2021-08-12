package mrl.widget.app;

import java.util.ArrayList;

import javax.vecmath.Point2d;

import mrl.widget.PointListViewer2D;
import mrl.widget.app.MainApplication.WindowPosition;

public class Point2dViewerModule extends Module{
	
	public static WindowPosition defaultPosition = WindowPosition.Right;
	private PointListViewer2D viewer;

	@Override
	protected void initializeImpl() {
		viewer = addWindow(new PointListViewer2D(dummyParent()), defaultPosition);
	}
	
	public PointListViewer2D getViewer() {
		return viewer;
	}

	public void setPointList(ArrayList<Point2d> pointList){
		ArrayList<ArrayList<Point2d>> pointGroupList = new ArrayList<ArrayList<Point2d>>();
		pointGroupList.add(pointList);
		setPointGroupList(pointGroupList);
	}

	public void setPointGroupList(ArrayList<ArrayList<Point2d>> pointGroupList){
		viewer.setPointGroupList(pointGroupList);
	}
}
