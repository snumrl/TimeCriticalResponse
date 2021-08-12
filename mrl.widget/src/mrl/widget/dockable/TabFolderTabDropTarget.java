package mrl.widget.dockable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SWTCustomInner;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

/**
 * 사용자가 DockableTabFolder의 Tab위에 드래그를 하였을때,
 * 드래그한 위치에 드래그된 TabItem이나 TabFolder를 끼워 넣는 DroptTarget
 * 
 * @author whcjs
 */
public class TabFolderTabDropTarget implements IDropTarget{
	
	private Object draggedObject;
	private DockableTabFolder targetFolder;
	private int targetIndex;
	private Rectangle snapRectangle;
	
	public TabFolderTabDropTarget(Object draggedObject,
			DockableTabFolder targetFolder, int targetIndex,
			Rectangle snapRectangle) {
		this.draggedObject = draggedObject;
		this.targetFolder = targetFolder;
		this.targetIndex = targetIndex;
		this.snapRectangle = snapRectangle;
	}

	public void drop() {
		if (draggedObject instanceof CTabItem){
			dropTabItem((CTabItem)draggedObject, targetFolder, targetIndex);
		} else if (draggedObject instanceof DockableTabFolder){
			DockableTabFolder dockable = (DockableTabFolder)draggedObject;
			CTabItem[] items = dockable.getItems();
			for (CTabItem item : items){
				dropTabItem(item, targetFolder, -1);
			}
		}
	}

	public Cursor getCursor() {
		return null;
	}

	public Rectangle getSnapRectangle() {
		return snapRectangle;
	}
	
	public static void dropTabItem(CTabItem item, DockableTabFolder tabFolder, int itemIndex){
		CTabFolder originParent = item.getParent();
		Control control = item.getControl();
		item.setControl(null);
		
		int itemStyle = SWTCustomInner.isCTabItemClosable(item) ? SWT.CLOSE : SWT.NONE;
		if (itemIndex < 0) itemIndex = tabFolder.getItemCount();
		CTabItem movedItem = new CTabItem(tabFolder, itemStyle, itemIndex);
		movedItem.setText(item.getText());
		movedItem.setImage(item.getImage());
		
		item.dispose();
		
		control.setParent(tabFolder);
		movedItem.setControl(control);
		tabFolder.setSelection(movedItem);
		
		if (originParent instanceof DockableTabFolder){
			((DockableTabFolder)originParent).checkItemCount(true);
		}
	}

}
