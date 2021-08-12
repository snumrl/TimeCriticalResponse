package mrl.widget.graph;

import java.util.HashSet;

import javax.vecmath.Point2d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import static mrl.widget.ScalableCanvas2D.*;

public class DefaultGraphDecorater implements IGraphDecorater{
	
	@Override
	public void beforeDrawLink(GC gc, DrawGraph graph) {
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
	}
	
	@Override
	public void drawLink(GC gc, DrawLink link) {
		Point2d s = link.source.position;
		Point2d t = link.target.position;
		gc.setLineWidth((int)Math.round(link.thickness));
		drawArrow(gc, s.x, s.y, t.x, t.y, link.target.radius);
//		drawLine(gc, s.x, s.y, t.x, t.y);
	}

	@Override
	public void beforeDrawNode(GC gc, DrawGraph graph) {
	}

	@Override
	public void drawNode(GC gc, DrawNode node, HashSet<DrawNode> selectedNodes) {
		Point2d p = node.position;
		if (selectedNodes.contains(node)){
			drawOval(gc, p, SWT.COLOR_YELLOW, node.radius*1.5);
		}
		drawOval(gc, p, node.color, node.radius);
		
		if (node.label != null){
			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
			Point size = gc.textExtent(node.label);
			gc.drawString(node.label,(int)( p.x - size.x/2), (int)(p.y + node.radius+4), true);
		}
	}
	
	protected void drawOval(GC gc, Point2d p, int color, double radius){
		gc.setBackground(gc.getDevice().getSystemColor(color));
		fillOval(gc, p.x - radius, p.y - radius, radius*2, radius*2);
	}

	@Override
	public void afterDrawNode(GC gc, DrawGraph graph) {
	}

	
}
