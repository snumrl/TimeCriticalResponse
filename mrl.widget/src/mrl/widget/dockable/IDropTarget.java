package mrl.widget.dockable;

import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;

/**
 * org.eclipse.ui.internal.dnd.IDropTarget를 복사해온 클래스.
 * 
 * This interface is used to drop objects. It knows how to drop a particular object
 * in a particular location. IDropTargets are typically created by IDragOverListeners, and
 * it is the job of the IDragOverListener to supply the drop target with information about 
 * the object currently being dragged.
 * 
 * @see mrl.widget.ui.internal.dnd.IDragOverListener
 * 
 * @author whcjs
 */
public interface IDropTarget {

    /**
     * Drops the object in this position
     */
    void drop();

    /**
     * Returns a cursor describing this drop operation
     * 
     * @return a cursor describing this drop operation
     */
    Cursor getCursor();

    /**
     * Returns a rectangle (screen coordinates) describing the target location
     * for this drop operation.
     * 
     * @return a snap rectangle or null if this drop target does not have a specific snap
     * location.
     */
    Rectangle getSnapRectangle();
}