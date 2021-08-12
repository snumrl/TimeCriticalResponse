package mrl.widget.dockable;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

/**
 * org.eclipse.ui.internal.dnd.IDragOverListener 를 복사해온 클래스
 * 
 * Implementers of this interface will receive notifications when objects are dragged over
 * a particular SWT control.
 * 
 * @author whcjs
 */
public interface IDragOverListener {
	
	public static final String DATA_KEY = "test.swt.docking.IDragOverListener";

	/**
     * Notifies the receiver that the given object has been dragged over
     * the given position. Returns a drop target if the object may be
     * dropped in this position. Returns null otherwise.
     * 
     * @param draggedObject object being dragged over this location
     * @param position location of the cursor
     * @param dragRectangle current drag rectangle (may be an empty rectangle if none)
     * @return a valid drop target or null if none
     */
    IDropTarget drag(Control currentControl, Object draggedObject,
            Point position, Rectangle dragRectangle);
}
