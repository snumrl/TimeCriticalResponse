package mrl.widget.dockable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tracker;

/**
 * Drag 관련 유틸 클래스
 * 
 * org.eclipse.ui.internal.dnd.DragUtil 를 참고하여 구현하였음.
 * 
 * @author whcjs
 */
public class DragUtil {
	
	private static final String DROP_TARGET_ID = DragUtil.class.getName() + ".dropTarget";
	
	public static void setDragTarget(Control control, IDragOverListener target) {
		control.setData(DROP_TARGET_ID, target);
	}

	public static boolean performDrag(final Object draggedItem, final Rectangle sourceBounds){
		return performDrag(draggedItem, sourceBounds, null);
	}
	public static boolean performDrag(final Object draggedItem, 
				final Rectangle sourceBounds, final IDragOverListener defaultListener){
		final Display display = Display.getCurrent();
		
		final Tracker tracker = new Tracker(display, SWT.NULL);
        tracker.setStippled(true);
        tracker.setRectangles(new Rectangle[]{ sourceBounds } );
        
        final IDropTarget[] lastDropTarget = new IDropTarget[1];
        
		tracker.addListener(SWT.Move, new Listener() {
			public void handleEvent(Event event) {
				lastDropTarget[0] = null;
				
				Point location = new Point(event.x, event.y);
				Control currentControl = display.getCursorControl();
				IDropTarget dropTarget = getDropTarget(currentControl, draggedItem, location, tracker.getRectangles()[0]);
				if (dropTarget == null && defaultListener != null){
					dropTarget = defaultListener.drag(currentControl, draggedItem, location, tracker.getRectangles()[0]);
				}
				
				Rectangle snapRect = null;
				if (dropTarget != null){
					lastDropTarget[0] = dropTarget;
					snapRect = dropTarget.getSnapRectangle();
				}
				
				if (snapRect == null){
					snapRect = new Rectangle(location.x, location.y, sourceBounds.width, sourceBounds.height);
				}
				tracker.setRectangles(new Rectangle[]{ snapRect });
			}
		});
		
		boolean isOK = tracker.open();
		tracker.dispose();
		
		if (isOK){
			if (lastDropTarget[0] != null){
				lastDropTarget[0].drop();
			}
			return true;
		} else {
			return false;
		}
		
	}
	
	private static IDropTarget getDropTarget(Control targetControl, Object draggedObject, Point position, Rectangle dragRectangle){
		while (targetControl != null){
			if (targetControl.getData(DROP_TARGET_ID) instanceof IDragOverListener){
				IDragOverListener listener = (IDragOverListener)targetControl.getData(DROP_TARGET_ID);
				IDropTarget target = listener.drag(targetControl, draggedObject, position, dragRectangle);
				if (target != null){
					return target;
				}
			}
			targetControl = targetControl.getParent();
		}
		return null;
	}
	
	public static Rectangle toDisplayBounds(Control control){
		return toDisplayBounds(control.getBounds(), control.getParent());
	}
	
	public static Rectangle toDisplayBounds(Rectangle bounds, Control control){
		Point p = control.toDisplay(bounds.x, bounds.y);
		return new Rectangle(p.x, p.y, bounds.width, bounds.height);
	}
	
}
