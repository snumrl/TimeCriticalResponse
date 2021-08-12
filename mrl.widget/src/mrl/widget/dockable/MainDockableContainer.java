package mrl.widget.dockable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Floating/Docking을 지원하는 루트 Container에 해당하는 클래스
 * 
 * 이 Container 하위에 속한 Control들 안에서 Floating/Docking이 지원이 되게 된다.
 * 
 * Floating으로 새로 뜬 Shell도 개념적으로는 이 Container에 속하게 되어,
 * 이 Container가 닫히면 Floating으로 떠 있던 Shell들도 닫히게 된다.
 * 
 * 그밖에 몇몇 Control의 Tab이 닫히더라도 남겨뒀다가 나중에 그대로 다시 띄워야 하는 경우가 있을때,
 * UndisposableTabControl을 사용하여 생성하면 되고,
 * 해당 Control들을 이 Container가 담고 있게 된다.
 * (StackLayout을 이용하여 표면적으로는 단순히 SashFormContainer만 뜨고
 *  숨겨져 잇는 Control들은 표시가 되지 않는다. )
 * 
 * @author whcjs
 */
public class MainDockableContainer extends Composite{
	
	private SashFormContainer container;
	
	public MainDockableContainer(Composite parent, int style) {
		super(parent, SWT.NONE);
		
		StackLayout layout = new StackLayout();
		setLayout(layout);
		
		container = new SashFormContainer(this, style);
		layout.topControl = container;
		
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				for (Shell shell : FloatingDropTarget.getChildShells(MainDockableContainer.this)){
					shell.close();
				}
				setRedraw(false);
			}
		});
	}
	
	public SashFormContainer getContainer() {
		return container;
	}
	
	public DockableTabFolder createFloatingTabFolder(){
		return FloatingDropTarget.createFloatingTabFolder(this);
	}
	
	public boolean showTabItem(String name){
		CTabItem item = findTabItem(name);
		if (item == null) return false;
		item.getParent().setSelection(item);
		return true;
	}
	
	public CTabItem findTabItem(String name){
		CTabItem item = findTabItem(name, this);
		if (item != null) return item;
		
		for (Shell shell : FloatingDropTarget.getChildShells(MainDockableContainer.this)){
			item = findTabItem(name, shell);
			if (item != null) return item;
		}
		
		return null;
	}
	
	public CTabItem findShowingItem() {
		CTabItem item = findShowingItem(this);
		if (item != null) return item;
		
		for (Shell shell : FloatingDropTarget.getChildShells(MainDockableContainer.this)){
			item = findShowingItem(shell);
			if (item != null) return item;
		}
		return null;
	}
	
	public void setChildShellVisible(boolean visible){
		for (Shell shell : FloatingDropTarget.getChildShells(MainDockableContainer.this)){
			shell.setVisible(visible);
		}
	}
	
	private static CTabItem findShowingItem(Composite root){
		for (Control child : root.getChildren()){
			if (child instanceof CTabFolder){
				CTabFolder folder = (CTabFolder)child;
				for (CTabItem item : folder.getItems()){
					if (item.getParent().getSelection() == item) return item;
				}
			} else if (child instanceof Composite){
				CTabItem item = findShowingItem((Composite)child);
				if (item != null){
					return item;
				}
			}
		}
		return null;
	}
	
	private static CTabItem findTabItem(String name, Composite root){
		for (Control child : root.getChildren()){
			if (child instanceof CTabFolder){
				CTabFolder folder = (CTabFolder)child;
				for (CTabItem item : folder.getItems()){
					if (item.getText().equals(name)){
						return item;
					}
				}
			} else if (child instanceof Composite){
				CTabItem item = findTabItem(name, (Composite)child);
				if (item != null){
					return item;
				}
			}
		}
		return null;
	}
	
	static MainDockableContainer getParentMainContainer(Control control){
		if (control == null) return null;
		if (control instanceof MainDockableContainer){
			return (MainDockableContainer)control;
		}
		return getParentMainContainer(control.getParent());
	}

}
