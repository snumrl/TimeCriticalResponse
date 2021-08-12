package mrl.motion.viewer.module;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.trasf.Pose2d;
import mrl.util.MathUtil;
import mrl.widget.ValueSlider;
import mrl.widget.WidgetUtil;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.app.Module;
import mrl.widget.graph.DrawGraph;
import mrl.widget.graph.DrawLink;
import mrl.widget.graph.DrawNode;
import mrl.widget.graph.GraphViewer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class Pose2dEditModule extends Module{
	
	public static WindowPosition defaultPosition = WindowPosition.Right;
	
	public static int MAX_TIME = 200;

	private DrawGraph graph;
	private DrawNode node1;
	private DrawNode node2;
	
	private GraphViewer graphViewer;
	private ValueSlider timeBar;
	private Text positionText;
	
	private ArrayList<ValueSlider> timeBarList;

	@Override
	protected void initializeImpl() {
		Composite composite = addWindow(new Composite(dummyParent(), SWT.NONE), defaultPosition);
		composite.setLayout(new GridLayout(1, true));
		
		graphViewer = new GraphViewer(composite){
			@Override
			protected boolean onMouseMove(Point2d p, MouseEvent e) {
				boolean ret = super.onMouseMove(p, e);
				positionText.setText(getPose().toString());
				return ret;
			}
		};
		graphViewer.setLayoutData(WidgetUtil.gridData(true, true));
		
		graph = new DrawGraph();
		DrawNode base = new DrawNode();
		base.position = new Point2d(0, 0);
		base.radius = 4;
		base.color = SWT.COLOR_BLUE;
		graph.nodeList.add(base);
		
		node1 = new DrawNode();
		node2 = new DrawNode();
		graph.nodeList.add(node1);
		graph.nodeList.add(node2);
		
		DrawLink link = new DrawLink();
		link.source = node1;
		link.target = node2;
		graph.linkList.add(link);
		
		graph.initialize();
		node1.position = new Point2d(100, 0);
		node1.radius = 5;
		node2.position = new Point2d(150, 0);
		node2.radius = 5;
		
		
		
		graphViewer.setGraph(graph);
		
		positionText = new Text(composite, SWT.BORDER);
		positionText.setLayoutData(WidgetUtil.gridData(true, false));
		
		timeBar = new ValueSlider(composite);
		timeBar.setLayoutData(WidgetUtil.gridData(true, false));
		timeBar.setRange(1, MAX_TIME);
		timeBar.setSelection(30*3);
		
		timeBarList = new ArrayList<ValueSlider>();
		timeBarList.add(timeBar);
	}
	
	public void setPoseSize(int size){
		if (timeBarList.size() > size){
			while (timeBarList.size() > size){
				graph.nodeList.remove(graph.nodeList.size()-1);
				graph.nodeList.remove(graph.nodeList.size()-1);
				graph.linkList.remove(graph.linkList.size()-1);
				ValueSlider bar = timeBarList.remove(timeBarList.size());
				bar.dispose();
			}
			return;
		}
		
		for (int i = timeBarList.size(); i < size; i++) {
			DrawNode node1 = new DrawNode();
			DrawNode node2 = new DrawNode();
			graph.nodeList.add(node1);
			graph.nodeList.add(node2);
			
			DrawLink link = new DrawLink();
			link.source = node1;
			link.target = node2;
			graph.linkList.add(link);
			
			node1.position = new Point2d(100*(i+1), 0);
			node1.radius = 5;
			node2.position = new Point2d(100*(i+1) + 50, 0);
			node2.radius = 5;
			
			ValueSlider timeBar = new ValueSlider(graphViewer.getParent());
			timeBar.setLayoutData(WidgetUtil.gridData(true, false));
			timeBar.setRange(1, MAX_TIME);
			timeBar.setSelection(30*3);
			timeBarList.add(timeBar);
		}
		graphViewer.getParent().layout();
	}
	
	public void setPose(Pose2d p){
		node1.position.set(p.position);
		Vector2d direction = new Vector2d(p.direction);
		direction.scale(50);
		direction.add(p.position);
		node2.position.set(direction);
	}
		
	public Pose2d getPose(){
		Vector2d direction = MathUtil.sub(node2.position, node1.position);
		direction.normalize();
		return new Pose2d(node1.position, direction);
	}
	
	public int getTime(){
		return timeBar.getSelection();
	}
	
	public void setTime(int time){
		timeBar.setSelection(time);
	}
	
	public void setTimeList(int[] timeList){
		for (int i = 0; i < timeList.length; i++) {
			timeBarList.get(i).setSelection(timeList[i]);
		}
	}
	public void setTimeList(ArrayList<Integer> timeList){
		for (int i = 0; i < timeList.size(); i++) {
			timeBarList.get(i).setSelection(timeList.get(i));
		}
	}
	
	public void setPoseList(ArrayList<Pose2d> poseList){
		for (int i = 0; i < poseList.size(); i++) {
			Pose2d p = poseList.get(i);
			DrawNode node1 = graph.nodeList.get(1 + i*2);
			DrawNode node2 = graph.nodeList.get(2 + i*2);
			
			node1.position.set(p.position);
			Vector2d direction = new Vector2d(p.direction);
			direction.scale(50);
			direction.add(p.position);
			node2.position.set(direction);
		}
		graphViewer.redrawCanvas();
	}
	
	public ArrayList<Pose2d> getPoseList(){
		ArrayList<Pose2d> list = new ArrayList<Pose2d>();
		for (int i = 1; i < graph.nodeList.size(); i+=2) {
			DrawNode node1 = graph.nodeList.get(i);
			DrawNode node2 = graph.nodeList.get(i+1);
			Vector2d direction = MathUtil.sub(node2.position, node1.position);
			direction.normalize();
			list.add(new Pose2d(node1.position, direction));
		}
		return list;
	}
	
	public ArrayList<Integer> getTimeList(){
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (ValueSlider slider : timeBarList){
			list.add(slider.getSelection());
		}
		return list;
	}

}
