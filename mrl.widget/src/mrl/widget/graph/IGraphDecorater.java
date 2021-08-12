package mrl.widget.graph;

import java.util.HashSet;

import org.eclipse.swt.graphics.GC;

public interface IGraphDecorater {
	
	public void drawLink(GC gc, DrawLink link);
	public void drawNode(GC gc, DrawNode node, HashSet<DrawNode> selectedNodes);
	public void beforeDrawLink(GC gc, DrawGraph graph);
	public void beforeDrawNode(GC gc, DrawGraph graph);
	public void afterDrawNode(GC gc, DrawGraph graph);
}
