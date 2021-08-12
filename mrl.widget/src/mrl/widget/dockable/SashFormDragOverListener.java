package mrl.widget.dockable;

import mrl.widget.dockable.SashFormContainer.DockingPosition;

import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * 사용자가 드래그를 하여 지정된 SashFormContainer위에 끌어놓았을때
 * 해당 SashFormContainer의 어느 위치에 맞을지를 계산하여 그에 맞는 DropTarget을 생성하는 클래스
 * 
 * @author whcjs
 */
public class SashFormDragOverListener implements IDragOverListener{
	
	private SashFormContainer targetContainer;
	private int parentSashFormCount;
	
	public SashFormDragOverListener(SashFormContainer targetContainer) {
		this.targetContainer = targetContainer;
		
		parentSashFormCount = 0;
		Composite parent = targetContainer.getParent();
		while (parent != null){
			if (parent instanceof SashFormContainer){
				parentSashFormCount++;
			}
			parent = parent.getParent();
		}
	}
	
	private boolean isAssignable(Object draggedObject){
		return (draggedObject instanceof CTabItem
				 || draggedObject instanceof DockableTabFolder);
	}

	public IDropTarget drag(Control currentControl, Object draggedObject,
			Point position, Rectangle dragRectangle) {
		
		// isAssignable
		if (!isAssignable(draggedObject)) return null;
		
		Rectangle folderBounds = DragUtil.toDisplayBounds(targetContainer);
		int topDistance = position.y - folderBounds.y;
		int bottomDistance = folderBounds.y + folderBounds.height - position.y;
		int leftDistance = position.x - folderBounds.x;
		int rightDistance = folderBounds.x + folderBounds.width - position.x;
		
		int horizontalMin = Math.min(leftDistance, rightDistance);
		int verticalMin = Math.min(topDistance, bottomDistance);
		int minimum = Math.min(horizontalMin, verticalMin);
		
		if (minimum > 120){
			// 중앙으로 드래그 한 경우 그냥 플로팅이 되도록 한다.
			return null;
		} else if (parentSashFormCount > 0 && minimum < parentSashFormCount * 10){
			// 부모 SashForm이 있고 아주 구석으로 드래그 한 경우, 부모에서 처리하도록 null을 반환한다.
			return null;
		}
		
		DockingPosition dockingPosition;
		if (verticalMin < horizontalMin){
			dockingPosition = (topDistance < bottomDistance) ? DockingPosition.Top : DockingPosition.Bottom;
		} else {
			dockingPosition = (leftDistance < rightDistance) ? DockingPosition.Left : DockingPosition.Right;
		}
		
		return new DockingDropTarget(draggedObject, dockingPosition, 
										targetContainer, folderBounds);
	}
	
}
