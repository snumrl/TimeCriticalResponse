package mrl.widget.dockable;

import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

/**
 * 사용자가 DockableTabFolder 위로 드래그를 하였을때,
 * 그 위치가 TabFolder의 Tab위라면 정확히 어떤 Item 사이인지를 계산하여
 * 그 위치에 맞도록 끼워넣는 DropTarget을 생성하는 클래스
 * 
 * @author whcjs
 */
public class TabFolderDragOverListener implements IDragOverListener{
	
	private DockableTabFolder dockableTabfolder;

	public TabFolderDragOverListener(DockableTabFolder dockableTabfolder) {
		this.dockableTabfolder = dockableTabfolder;
	}
	
	private boolean isAssignable(Object draggedObject){
		return (draggedObject instanceof CTabItem
				 || draggedObject instanceof DockableTabFolder);
	}
	
	public IDropTarget drag(Control currentControl, Object draggedObject,
							Point position, Rectangle dragRectangle) {
		
		// isAssignable
		if (!isAssignable(draggedObject)) return null;
		
		Rectangle folderBounds = DragUtil.toDisplayBounds(dockableTabfolder);
		
		if (draggedObject == dockableTabfolder){
			return new BlankDropTarget(folderBounds);
		}
		
		Rectangle snapRectangle = null;
		Rectangle barBounds = new Rectangle(folderBounds.x, folderBounds.y, 
				folderBounds.width, dockableTabfolder.getTabHeight());
		if (barBounds.contains(position)){
			int targetIndex = -1;
			if (draggedObject instanceof CTabItem){
				for (CTabItem item : dockableTabfolder.getItems()){
					Rectangle itemBounds = DragUtil.toDisplayBounds(item.getBounds(), dockableTabfolder);
					if (itemBounds.contains(position)){
						snapRectangle = itemBounds;
						targetIndex = dockableTabfolder.indexOf(item);
						break;
					}
				}
				CTabItem draggedItem = (CTabItem)draggedObject;
				if (draggedItem.getParent() == dockableTabfolder){
					int index = dockableTabfolder.indexOf(draggedItem);
					if (targetIndex > index){
						targetIndex++;
					}
				}
			}
			if (snapRectangle == null){
				snapRectangle = barBounds;
			}
			return new TabFolderTabDropTarget(draggedObject, dockableTabfolder, targetIndex, snapRectangle);
		}
		
		return null;
	}
	
}
