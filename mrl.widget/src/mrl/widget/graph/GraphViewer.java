package mrl.widget.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.util.MathUtil;
import mrl.widget.Rectangle2d;
import mrl.widget.ScalableCanvas2D;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public class GraphViewer extends ScalableCanvas2D{
	
	protected DrawGraph graph;
	protected HashSet<DrawNode> selectedNodes = new HashSet<DrawNode>();
	protected Point2d lastPoint;
	protected IGraphDecorater decorater;

	public GraphViewer(Composite parent) {
		super(parent);
		setRedrawTime(0);
		decorater = new DefaultGraphDecorater();
		canvas.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == 'l'){
					System.out.println("do layout ");
					graph.doLayout();
					fitToScreen();
					redrawCanvas();
				}
				try {
					String fileName = "GraphViewer.nodePositions";
					if (e.character == 's'){
						BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)));
						for (DrawNode node : graph.nodeList){
							bw.write(node.position.x + "\t" + node.position.y + "\r\n");
						}
						bw.close();
					}
					if (e.character == 'o'){
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
						for (DrawNode node : graph.nodeList){
							String line = br.readLine();
							if (line == null) break;
							String[] tokens = line.split("\t");
							node.position.x = Double.parseDouble(tokens[0]);
							node.position.y = Double.parseDouble(tokens[1]);
						}
						br.close();
						fitToScreen();
						redrawCanvas();
					}
				} catch (Exception e2) {
					throw new RuntimeException(e2);
				}
			}
		});
	}
	
	public IGraphDecorater getDecorater() {
		return decorater;
	}

	public void setDecorater(IGraphDecorater decorater) {
		this.decorater = decorater;
	}

	public DrawGraph getGraph() {
		return graph;
	}

	public void setGraph(DrawGraph graph) {
		this.graph = graph;
		redrawCanvas();
	}
	
	public HashSet<DrawNode> getSelectedNodes() {
		return selectedNodes;
	}
	
	protected void onInitialDraw(){
		fitToScreen();
		redrawCanvas();
	}

	@Override
	protected void drawContents(GC gc) {
		if (graph == null) return;
		
		decorater.beforeDrawLink(gc, graph);
		
		for (DrawLink link : graph.linkList){
			decorater.drawLink(gc, link);
		}
		
		decorater.beforeDrawNode(gc, graph);
		
		for (DrawNode node : graph.nodeList){
			decorater.drawNode(gc, node, selectedNodes);
		}
		
		decorater.afterDrawNode(gc, graph);
	}

	@Override
	protected boolean onMouseDown(Point2d p, MouseEvent e) {
		double minD = Integer.MAX_VALUE;
		DrawNode selected = null;
		for (DrawNode node : graph.nodeList){
			double d = node.position.distance(p);
			if (d < minD){
				minD = d;
				selected = node;
			}
		}
		if (minD < 50){
			if ((e.stateMask & SWT.CTRL) == 0){
				if (!selectedNodes.contains(selected)){
					selectedNodes.clear();
					selectedNodes.add(selected);
				}
			} else {
				if (selectedNodes.contains(selected)){
					selectedNodes.remove(selected);
				} else {
					selectedNodes.add(selected);
					notifySelection(e.stateMask);
				}
			}
			
			
			lastPoint = new Point2d(p);
			return true;
		} else {
			if ((e.stateMask & SWT.CTRL) == 0){
				selectedNodes.clear();
			}
			lastPoint = null;
			redrawCanvas();
			return false;
		}
	}
	
	protected void notifySelection(int stateMask){
		Event event = new Event();
		event.stateMask = stateMask;
		this.notifyListeners(SWT.Selection, event);
	}

	@Override
	protected boolean onMouseMove(Point2d p, MouseEvent e) {
		if (selectedNodes.size() > 0 && lastPoint != null){
			Vector2d diff = MathUtil.sub(lastPoint, p);
			for (DrawNode selectedNode : selectedNodes){
				selectedNode.position.sub(diff);
			}
			lastPoint = new Point2d(p);
			return true;
		}
		return false;
	}

	@Override
	protected boolean onMouseUp(Point2d p, MouseEvent e) {
		lastPoint = null;
		return false;
	}

	@Override
	protected boolean onMouseDoubleClick(Point2d p, MouseEvent e) {
		return false;
	}

	@Override
	protected boolean onMouseScrolled(Point2d p, MouseEvent e) {
		return false;
	}

	@Override
	protected Rectangle getContentBoundary() {
		if (graph == null) return null;
		Rectangle2d r = null;
		for (DrawNode node : graph.nodeList){
//			if (isHidden(node)) continue;
			r = Rectangle2d.union(r, new Rectangle2d(node.position.x, node.position.y, 0, 0));
		}
		if (r == null) return null;
		return r.swtRectangle();
	}

	@Override
	protected void onBoundarySelection(Rectangle2d boundary, MouseEvent e) {
		if ((e.stateMask & SWT.CTRL) == 0){
			selectedNodes.clear();
		}
		for (DrawNode node : graph.nodeList){
			if (boundary.isInside(node.position)){
				selectedNodes.add(node);
			}
		}
	}

}
