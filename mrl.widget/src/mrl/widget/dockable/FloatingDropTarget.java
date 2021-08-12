package mrl.widget.dockable;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * 드래그 된 개체를 새 창으로 떼어내는 동작을 수행하는 DropTarget
 * 
 * 새 Shell을 생성하여 해당 Shell으로 드래그된 개체를 떼어낸다.
 * 
 * @author whcjs
 */
public class FloatingDropTarget implements IDropTarget{
	
	private static final String BASE_CONTROL_KEY = SashFormContainer.class.getName() + ".baseControl";
	
	private Object draggedObject;
	private Rectangle dragRectangle;
	
	public FloatingDropTarget(Object draggedObject, Point position) {
		this.draggedObject = draggedObject;
		
		Control draggedControl = null;
		if (draggedObject instanceof CTabItem){
			draggedControl = ((CTabItem)draggedObject).getControl();
		} else if (draggedObject instanceof DockableTabFolder){
			draggedControl = (Control)draggedObject;
		}
		if (draggedControl != null){
			Point preferSize = draggedControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			int width = preferSize.x;
			if (width < 220){
				width = Math.max(300, draggedControl.getSize().x);
				preferSize = draggedControl.computeSize(width, SWT.DEFAULT);
			}
			int height = preferSize.y;
			if (height < 150){
				height = Math.max(300, draggedControl.getSize().y);
			}
			height += 35; // Shell 타이틀 바 크기정도를 늘려준다.
			if (draggedObject instanceof CTabItem){
				height += 25; // Tab Folder의 Bar 크기정도를 늘려준다.
			}
			dragRectangle = new Rectangle(position.x, position.y, Math.min(width, 600), Math.min(height, 600));
		}
	}

	public void drop() {
		DockableTabFolder draggedFolder = getDraggedFolder();
		if (draggedFolder == null) return;
		
		Shell parentShell;
		MainDockableContainer mainContainer = MainDockableContainer.getParentMainContainer(draggedFolder.getParent());
		if (draggedFolder.getParent().getParent() instanceof Shell){
			parentShell = (Shell)draggedFolder.getShell().getParent();
			if (parentShell.getData(BASE_CONTROL_KEY) instanceof MainDockableContainer){
				mainContainer = (MainDockableContainer)parentShell.getData(BASE_CONTROL_KEY);
			}
		} else {
			parentShell = draggedFolder.getShell();
		}
		
		Shell newShell = new Shell(parentShell, SWT.RESIZE | SWT.CLOSE | SWT.TOOL);
		
		newShell.setData(BASE_CONTROL_KEY, mainContainer);
		newShell.setBounds(dragRectangle);
		newShell.setLayout(new FillLayout());
		newShell.addShellListener(new FloatingShellListener());
		
		SashFormContainer container = new SashFormContainer(newShell, SWT.NONE);
		if (draggedObject instanceof CTabItem){
			CTabItem item = (CTabItem) draggedObject;
			DockableTabFolder newFolder = new DockableTabFolder(container, item.getParent().getStyle());
			container.dropInitialControl(newFolder);
			TabFolderTabDropTarget.dropTabItem(item, newFolder, -1);
		} else if (draggedObject instanceof DockableTabFolder){
			container.dropInitialControl((DockableTabFolder)draggedObject);
		}
		
		newShell.open();
		
	}

	public Cursor getCursor() {
		return null;
	}
	
	private DockableTabFolder getDraggedFolder(){
		if (draggedObject instanceof CTabItem){
			Composite parent = ((CTabItem)draggedObject).getParent();
			if (parent instanceof DockableTabFolder){
				return (DockableTabFolder)parent;
			}
		} else if (draggedObject instanceof DockableTabFolder){
			return (DockableTabFolder)draggedObject;
		}
		return null;
	}

	public Rectangle getSnapRectangle() {
		return dragRectangle;
	}
	
	static List<Shell> getChildShells(MainDockableContainer container){
		ArrayList<Shell> list = new ArrayList<Shell>();
		for (Shell shell : container.getDisplay().getShells()){
			if (shell.getData(BASE_CONTROL_KEY) == container){
				list.add(shell);
			}
		}
		return list;
	}
	
	static DockableTabFolder createFloatingTabFolder(MainDockableContainer mainContainer){
		Shell newShell = new Shell(mainContainer.getShell(), SWT.RESIZE | SWT.CLOSE | SWT.TOOL);
		newShell.setData(BASE_CONTROL_KEY, mainContainer);
		newShell.setLayout(new FillLayout());
		newShell.addShellListener(new FloatingShellListener());
		
		SashFormContainer container = new SashFormContainer(newShell, SWT.NONE);
		DockableTabFolder newFolder = new DockableTabFolder(container, SWT.BORDER);
		container.dropInitialControl(newFolder);
		
		newShell.open();
		
		return newFolder;
	}
	
	
	private static class FloatingShellListener extends ShellAdapter{
		@Override
		public void shellClosed(ShellEvent e) {
			Shell shell = (Shell)e.widget;
			checkUndiposableControl(shell);
		}
		
		private void checkUndiposableControl(Control control){
			if (control instanceof UndisposableTabControl){
				((UndisposableTabControl)control).switchParent();
			} else if (control instanceof Composite){
				for (Control child : ((Composite)control).getChildren()){
					checkUndiposableControl(child);
				}
			}
		}
	}

}
