package mrl.widget.dockable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * SashForm으로 공간을 두개로 나눠서 두개의 Control을 담을 수 있도록 하기 위한 Composite
 * 
 * 두개로 나뉠때 두개의 SashFormContainer를 만들고 그 Container안에 각각의 Control을 담기 때문에, 
 * 재귀적으로 계속해서 Container를 나눌 수 있게 하는 구조로 되어 있다.
 * 
 * 포함되어 있던 Control이 dispose되면 그에 따라 자신이 dispose 되거나
 * sash form을 없애고 다시 하나의 Control만 담도로 복원하는 동작을 수행한다.
 * 
 * @author whcjs
 */
public class SashFormContainer extends Composite{
	
	private static final String DISPOSE_LISTENER_KEY = SashFormContainer.class.getName() + ".dispose";
	
	/**
	 * 두번째 Control을 끼워 넣을때, 어느 위치에 끼워넣을지에 해당하는 클래스
	 * 
	 * @author whcjs
	 */
	public enum DockingPosition { 
		Left, Right, Top, Bottom; 
		
		public boolean isSplittedVertical(){
			return (this == Top || this == Bottom);
		}
		
		public boolean isAttachAfter(){
			return (this == Right || this == Bottom);
		}
	}
	
	private SashForm sashForm;
	
	private boolean isSetDisposed = false;
	
	public SashFormContainer(Composite parent, int style) {
		super(parent, style);
		
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				isSetDisposed = true;
			}
		});
		
		this.setLayout(new FillLayout());
		
		applyDisposeListener(this);
		DragUtil.setDragTarget(this, new SashFormDragOverListener(this));
	}
	
	public void setWeights(int[] weights){
		if (sashForm != null){
			sashForm.setWeights(weights);
		}
	}
	
	public CTabItem createNewDropItem(DockingPosition position, int[] defaultWeight, boolean isClosable){
		SashFormContainer container = this;
		
		DockableTabFolder tabFolder = container.getExistingTabFolder(position);
		
		boolean isTabFolderExisted = (tabFolder != null);
		if (!isTabFolderExisted){
			tabFolder = new DockableTabFolder(container, SWT.BORDER);
			container.dropNewControl(tabFolder, position, null);
			if (defaultWeight != null){
				container.setWeights(defaultWeight);
			}
		}
		
		CTabItem tabItem = new CTabItem(tabFolder, isClosable ? SWT.CLOSE : SWT.NONE);
		tabFolder.setSelection(tabItem);
		return tabItem;
	}
	
	/**
	 * 처음의 하나의 Control을 등록하는 함수.
	 * 
	 * 입력된 control은 이 Container에 속한 새 Control일 수도 있고,
	 * 다른 Container에 속한 기존의 Control 일 수도 있다.
	 * 
	 * @param control
	 */
	public void dropInitialControl(Control control){
		applyDisposeListener(control);
		
		if (control.getParent() != this){
			final Composite controlsParent = control.getParent();
			control.setParent(this);
			control.setLayoutData(null);
			if (controlsParent != this && controlsParent instanceof SashFormContainer){
				getDisplay().timerExec(1, new Runnable() {
					public void run() {
						if (controlsParent.isDisposed()) return;
						controlsParent.dispose();
					}
				});
			}
		}
	}

	/**
	 * 이미 하나의 Control(혹은 sashForm에 2개의 Control)이 있는 상황에서
	 * 또다른 Control을 끼워 넣는 함수.
	 * 
	 * 입력된 control이 들어간 SashFormContainer를 반환한다.
	 * 
	 * @param control
	 * @param position
	 * @param controlBounds
	 */
	public SashFormContainer dropNewControl(Control control, DockingPosition position, Rectangle controlBounds){
		
		Control[] children = getChildren();
		if (sashForm != null && children.length > 2){
			throw new UnsupportedOperationException();
		}
		
		final Composite controlsParent = control.getParent();
		
		applyDisposeListener(control);
		
		SashForm oldSashForm = sashForm;
		int style = position.isSplittedVertical() ? SWT.VERTICAL : SWT.HORIZONTAL;
		sashForm = new SashForm(this, style);
		
		SashFormContainer container1 = new SashFormContainer(sashForm, getStyle());
		SashFormContainer container2 = new SashFormContainer(sashForm, getStyle());
		
		if (oldSashForm == null){
			Control originControl = (children[0] != control) ? children[0] : children[1];
			applyDisposeListener(originControl);
			
			if (position.isAttachAfter()){
				originControl.setParent(container1);
				control.setParent(container2);
			} else {
				originControl.setParent(container2);
				control.setParent(container1);
			}
			originControl.setLayoutData(null);
		} else {
			if (position.isAttachAfter()){
				container1.sashForm = oldSashForm;
				oldSashForm.setParent(container1);
				control.setParent(container2);
			} else {
				container2.sashForm = oldSashForm;
				oldSashForm.setParent(container2);
				control.setParent(container1);
			}
		}
		control.setLayoutData(null);
		
		Rectangle fullBounds = getBounds();
		if (controlBounds != null){
			int[] weights = new int[2];
			if (position.isSplittedVertical()){
				weights[0] = controlBounds.height;
				weights[1] = fullBounds.height - weights[0];
			} else {
				weights[0] = controlBounds.width;
				weights[1] = fullBounds.width - weights[0];
			}
			if (position.isAttachAfter()){
				int temp = weights[1];
				weights[1] = weights[0];
				weights[0] = temp;
			}
			sashForm.setWeights(weights);
		}
		
		this.layout();
		
		if (controlsParent != this && controlsParent instanceof SashFormContainer){
			getDisplay().timerExec(1, new Runnable() {
				public void run() {
					if (controlsParent.isDisposed()) return;
					controlsParent.dispose();
				}
			});
		}
		
		return position.isAttachAfter() ? container2 : container1;
	}
	
	public SashForm getSashForm() {
		return sashForm;
	}

	public boolean isSplitted(){
		return (sashForm != null);
	}
	
	public boolean isSplittedVertical(){
		return (sashForm.getStyle() & SWT.VERTICAL) != 0;
	}
	
	public SashFormContainer[] getSplittedContainers(){
		Control[] children = sashForm.getChildren();
		return new SashFormContainer[]{
			(SashFormContainer)children[0], (SashFormContainer)children[1]
		};
	}
	
	public DockableTabFolder getExistingTabFolder(DockingPosition position){
		if (sashForm != null) return null;
		
		if (getParent() instanceof SashForm){
			SashFormContainer parent = (SashFormContainer)getParent().getParent();
			DockableTabFolder target = getExistingTabFolder(parent, position);
			if (target != null){
				return target;
			} else if (parent.getParent() instanceof SashForm){
				target = getExistingTabFolder((SashFormContainer)parent.getParent().getParent(), position);
				if (target != null){
					return target;
				}
			}
		}
		
		return null;
	}
	
	private DockableTabFolder getExistingTabFolder(SashFormContainer parent, DockingPosition position){
		if (parent.isSplittedVertical() == position.isSplittedVertical()){
			int index = position.isAttachAfter() ? 1: 0;
			SashFormContainer target = parent.getSplittedContainers()[index];
			if (!target.isSplitted() && target.getChildren()[0] instanceof CTabFolder){
				return (DockableTabFolder)target.getChildren()[0];
			}
		}
		return null;
	}
	
	
	private void applyDisposeListener(Control control){
		if (control.getData(DISPOSE_LISTENER_KEY) == null){
			control.addDisposeListener(disposeListener);
		}
	}
	
	private static final ControlDisposeListener disposeListener = new ControlDisposeListener();
	
	private static class ControlDisposeListener implements DisposeListener{
		
		public void widgetDisposed(DisposeEvent e) {
			Control control = (Control)e.widget;
			
			checkFocusControl(control);
			
			final Composite parent = control.getParent();
			if (parent == null) return;
			
			if (parent instanceof Shell){
				// 부모가 Shell인 경우 닫아준다.
				parent.getDisplay().timerExec(1, new Runnable() {
					public void run() {
						if (parent.isDisposed()) return;
						((Shell)parent).close();
					}
				});
			} else if (parent instanceof SashFormContainer){
				if (((SashFormContainer)parent).isSetDisposed){
					return;
				}
				
				// 부모가 SashFormContainer 인 경우 다 비었으므로 dispose 해준다.
				parent.getDisplay().timerExec(1, new Runnable() {
					public void run() {
						if (parent.isDisposed()) return;
						parent.dispose();
					}
				});
			} else if (parent instanceof SashForm &&
						parent.getParent() instanceof SashFormContainer){
				if (((SashFormContainer)parent.getParent()).isSetDisposed){
					return;
				}
				
				// 부모가 sashForm이고 그 위의 부모가 SashFormContainer인 경우
				// sashForm에 두개의 Control이 나뉘어져 있다가 하나가 닫힌 경우므로
				// sashForm을 없애고 다시 하나 남은 Control의 부모를 SashFormContainer로 바꿔준다.
				final SashFormContainer container = (SashFormContainer)parent.getParent();
				Control[] children = container.sashForm.getChildren();
				Control remainControl = (children[0] == control) ? children[1] : children[0];
				if (remainControl instanceof SashFormContainer){
					SashFormContainer remainContainer = (SashFormContainer)remainControl;
					if (!remainContainer.isSplitted() && remainContainer.getChildren().length == 1){
						remainControl = remainContainer.getChildren()[0];
						remainContainer.removeDisposeListener(this);
					}
				}
				remainControl.setParent(container);
				remainControl.setLayoutData(null);
				container.sashForm.dispose();
				container.sashForm = null;
				container.getDisplay().timerExec(1, new Runnable() {
					public void run() {
						if (!container.isDisposed()){
							container.layout();
						}
					}
				});
			}
		}
		
		/**
		 * 현재 포커스를 가진 Control이 닫히면서 문제가 생기는 경우가 있어,
		 * 닫히려는 Control에 포커스가 가있다면 Shell에 포커스가 가도록 한다.
		 * 
		 * @param control
		 */
		private void checkFocusControl(Control control){
			Control focusControl = control.getDisplay().getFocusControl();
			if (focusControl == null || focusControl instanceof Shell){
				return;
			}
			
			Composite parent = control.getParent();
			Composite currentParent = focusControl.getParent();
			
			while (currentParent != null){
				if (parent == currentParent){
					control.getShell().setFocus();
					return;
				}
				currentParent = currentParent.getParent();
			}
		}
	}
}
