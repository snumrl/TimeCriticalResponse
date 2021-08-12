package mrl.widget.dockable;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

/**
 * FloatingDropTarget을 생성 해주는 DragOverListener
 * 
 * @author whcjs
 */
public class FloatingDragOverListener implements IDragOverListener{
	
	public IDropTarget drag(Control currentControl, Object draggedObject,
								Point position, Rectangle dragRectangle) {
		return new FloatingDropTarget(draggedObject, position);
	}

}
