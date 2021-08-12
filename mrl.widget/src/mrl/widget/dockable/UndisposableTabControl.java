package mrl.widget.dockable;

import java.util.ArrayList;

import mrl.widget.dockable.SashFormContainer.DockingPosition;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SWTCustomInner;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * TabFolder의 TabControl으로 넣을 Control 중
 * 해당 Tab이 닫히더라도 Control을 dispose하지 않고 남겨뒀다가
 * 나중에 그대로 다시 띄워주는 기능을 지원하기 위한 클래스
 * 
 * TabControl으로 이 Composite을 만들고 실제로 넣을 Control을 자식으로 만들면 된다.
 * 닫혀진 Control을 다시 띄울때는 restoreControl 함수를 이용하면 된다.
 * 
 * @author whcjs
 */
public class UndisposableTabControl extends Composite{
	
	private MainDockableContainer container;
	private String text;
	private Image image;
	private boolean isClosable;
	
	private ArrayList<UndisposableTabControlListener> listeners = new ArrayList<UndisposableTabControlListener>();

	public UndisposableTabControl(CTabItem item, 
						final MainDockableContainer container) {
		super(item.getParent(), SWT.NONE);
		
		this.container = container;
		
		text = item.getText();
		image = item.getImage();
		isClosable = SWTCustomInner.isCTabItemClosable(item);
		
		this.setLayout(new FillLayout());
	}
	
	/**
	 * Tab이 닫힐때 Control이 dispose 되지 않도록 하기 위해
	 * 이 Control의 부모를 main container로 변경하는 함수.
	 */
	void switchParent(){
		container.setRedraw(false);
		this.setLayoutData(null);
		this.setParent(container);
		container.setRedraw(true);
		notifyStatusChanged();
	}
	
	public void restoreControl(DockingPosition position, SashFormContainer container, int[] weights){
		if (getParent() != this.container){
			return;
		}
		
		DockableTabFolder tabFolder = container.getExistingTabFolder(position);
		
		if (tabFolder == null){
			tabFolder = new DockableTabFolder(container, SWT.BORDER);
			container.dropNewControl(tabFolder, position, null);
			if (weights != null){
				container.setWeights(weights);
			}
		}
		this.setLayoutData(null);
		this.setParent(tabFolder);
		
		int style = isClosable ? SWT.CLOSE : SWT.NONE;
		CTabItem item = new CTabItem(tabFolder, style);
		item.setText(text);
		item.setImage(image);
		item.setControl(this);
		tabFolder.setSelection(item);
		
		notifyStatusChanged();
	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public boolean isClosable() {
		return isClosable;
	}

	public void setClosable(boolean isClosable) {
		this.isClosable = isClosable;
	}

	public MainDockableContainer getContainer() {
		return container;
	}
	
	public boolean isHided(){
		if (isDisposed()) return true;
		return (getParent() == container);
	}
	
	public void addListener(UndisposableTabControlListener listener){
		listeners.add(listener);
	}
	
	public void removeListener(UndisposableTabControlListener listener){
		listeners.remove(listener);
	}
	
	private void notifyStatusChanged(){
		for (UndisposableTabControlListener listener : listeners){
			listener.onStatusChanged();
		}
	}
	
}
