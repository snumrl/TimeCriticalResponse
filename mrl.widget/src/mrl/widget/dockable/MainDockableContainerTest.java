package mrl.widget.dockable;

import mrl.widget.dockable.SashFormContainer.DockingPosition;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class MainDockableContainerTest {
	
	private static int itemCount = 0;
	
	public static void main(String[] args) {
		Display display = new Display ();
		final Shell shell = new Shell (display);
		shell.setSize(800, 600);
		shell.setLayout(new FillLayout());
		
		// Docking 가능 영역을 MainContainer로 구성합니다.
		// Main Container 내부의 TabFolder 및 Tab Item은 
		// 이 Main Container 안으로만 Docking이 가능합니다.
		final MainDockableContainer mainContainer = new MainDockableContainer(shell, SWT.NONE);
		// 실제로 넣을 Control들은 이 MainContainer의 Container안에 넣습니다.
		SashFormContainer container = mainContainer.getContainer();
		
		// 중앙에 Text Control을 넣습니다. 부모는 위에서 받은 SahsFormContainer를 줍니다.
		final Text myText = new Text(container, SWT.BORDER | SWT.MULTI);
		myText.setText("Main Text");
		myText.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
		// SashFormContainer에 추가적으로 이 Text Control을 등록해주어야 합니다.
		container.dropInitialControl(myText);
		
		// 두번째 Control으로 DockableTabFolder를 생성합니다. 
		// 마찬가지로 SashFromContainer를 부모로 합니다.
		// ( DockableTablFolder와 여기의 TabItem들은 Docking이 가능합니다. )
		DockableTabFolder folder = new DockableTabFolder(container, SWT.BORDER);
		folder.setSimple(true);
		// 두번째 Control을 SashFromContainer에 등록하면서 어디에 위치시킬지를 지정합니다.
		container.dropNewControl(folder, DockingPosition.Right, null);
		
		// SashFormContainer의 좌/우, 혹은 위/아래 비율을 설정합니다.
		container.setWeights(new int[]{ 7, 3 });
		
		// TabFolder에 TabItem들을 구성합니다.
		// 일반 CTabFolder와 동일하게 Item 들을 구성하면 됩니다.
		CTabItem item = new CTabItem(folder, SWT.BORDER | SWT.CLOSE);
		item.setText("Restorable");
//		item.setImage(getIcon());
		// TabItem의 Control을 UndisposableTabControl로 지정하면
		// 해당 Control은 TabItem이 닫혀도 dispose되지 않고 남아 있으며,
		// restoreControl함수로 추후 다시 띄울 수 있습니다.
		final UndisposableTabControl restoreControl = new UndisposableTabControl(item, mainContainer);
		Text restoreText = new Text(restoreControl, SWT.BORDER);
		restoreText.setText("Restorable");
		item.setControl(restoreControl);
		
		// 그냥 일반 예제 아이템을 생성합니다.
		createSampleItems(folder);
		
		
		// 테스트용 키 액션을 답니다.
		myText.addKeyListener(new KeyAdapter() {
			private boolean childShellVisible = true;
			
			public void keyPressed(KeyEvent e) {
				if (e.character == 'r'){
					// UndisposableTabControl의 restoreControl 함수로
					// 숨겨져 있던 Control을 다시 TabItem과 함께 띄울 수 있습니다.
					restoreControl.restoreControl(DockingPosition.Right, 
							(SashFormContainer)myText.getParent(), new int[]{ 7, 3 });
				}
				if (e.character == 'h'){
					// Floating 된 Shell들은 개념적으로 MainContainer의 하위에 속해 있으며
					// MainContainer의 setChildShellVisible 함수로 숨기거나 다시 보이게 할 수 있습니다.
					childShellVisible = !childShellVisible;
					mainContainer.setChildShellVisible(childShellVisible);
				}
				if (e.character == 'f'){
					// TabItem들이 이리 저리 옮겨 다닐 수 있고, Floating이 되어 있을 수도 있는데,
					// 개념적으로 다 MainContainer안에 속하기 때문에,
					// MainContainer의 findTabItem함수로 해당 이름의 TabItem을 찾을 수 있습니다.
					// ( 같은 이름의 TabItem이 여러개 있을 경우 그 중 아무거나가 반환됩니다. )
					System.out.println(mainContainer.findTabItem("Restorable") != null);
				}
			}
		});
		
		shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
	}
	
	private static void createSampleItems(DockableTabFolder folder){
		createItem(folder);
		createItem(folder);
		createItem(folder);
		folder.setSelection(0);
	}
	
	private static CTabItem createItem(CTabFolder tabFolder){
		itemCount++;
		String name = "Item " + itemCount;
		CTabItem item = new CTabItem(tabFolder, SWT.BORDER | SWT.CLOSE);
		Text control = new Text(tabFolder, SWT.BORDER);
		control.setText(name);
		item.setControl(control);
		item.setText(name);
		item.setImage(getIcon());
		return item;
	}
	
	private static Image icon;
	private static Image getIcon(){
		if (icon == null){
			Display display = Display.getCurrent();
			Image infoImage = display.getSystemImage(SWT.ICON_INFORMATION);
			Rectangle size = infoImage.getBounds();
			icon = new Image(display, 16, 16);
			GC gc = new GC(icon);
			gc.setAntialias(SWT.ON);
			gc.setAdvanced(true);
			gc.drawImage(infoImage, 0, 0, size.width, size.height, 0, 0, 16, 16);
			gc.dispose();
		}
		return icon;
	}
}
