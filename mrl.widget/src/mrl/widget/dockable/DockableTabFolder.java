package mrl.widget.dockable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * 도킹이 되는 CTabFolder
 * 
 * 다른 DockableTabFolder나 TabItem을 자신으로 받아들일 수 있다.
 * 
 * @author whcjs
 */
public class DockableTabFolder extends CTabFolder{
	
	public static boolean DEFAULT_SIMPLE = false;
	
	public DockableTabFolder(Composite parent, int style) {
		super(parent, style);
		
		this.setSimple(DEFAULT_SIMPLE);
		this.setBorderVisible(true);
		this.setTabHeight(22);
		setSelectionForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
		setSelectionBackground(new Color[] {
				getDisplay().getSystemColor(SWT.COLOR_WHITE),
				getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
				}, new int[] { 100 }, true);
		
		this.addDragDetectListener(new DragDetectListener() {
			public void dragDetected(DragDetectEvent e) {
				CTabItem selectedItem = getSelection();
				if (selectedItem == null) return;
				boolean isSelected = selectedItem.getBounds().contains(e.x, e.y);
				
				Object draggedItem = isSelected ? selectedItem : DockableTabFolder.this;
				if (getItemCount() <= 1){
					draggedItem = DockableTabFolder.this;
				}
				DragUtil.performDrag(draggedItem, 
						DragUtil.toDisplayBounds(DockableTabFolder.this), 
						new FloatingDragOverListener());
			}
		});
		
		DragUtil.setDragTarget(this, new TabFolderDragOverListener(this));
		
		this.addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void close(CTabFolderEvent event) {
				if (event.item instanceof CTabItem){
					onItemClose((CTabItem)event.item);
				}
				
				checkItemCount(false);
			}
		});
	}
	
	public CTabItem addItem(String label, Control control){
		return addItem(label, control, true);
	}
	public CTabItem addItem(String label, Control control, boolean closable){
		CTabItem item = new CTabItem(this, closable ? (SWT.BORDER | SWT.CLOSE) : SWT.BORDER);
		item.setControl(control);
		item.setText(label);
		if (getItemCount() == 1){
			setSelection(item);
		}
		return item;
	}
	
	public void checkItemCount(boolean isAfterClose){
		int limitCount = isAfterClose ? 0 : 1;
		if (getItemCount() <= limitCount){
			getDisplay().timerExec(1, new Runnable() {
				public void run() {
					dispose();
				}
			});
		}
	}
	
	public void closeItem(CTabItem item){
		onItemClose(item);
		item.dispose();
		checkItemCount(true);
	}
	
	private void onItemClose(CTabItem item){
		final Control control = item.getControl();
		if (control != null && !control.isDisposed()){
			if (control instanceof UndisposableTabControl){
				((UndisposableTabControl)control).switchParent();
			} else {
				control.dispose();
			}
		}
	}
	
}
