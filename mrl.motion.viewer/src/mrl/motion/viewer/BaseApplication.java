package mrl.motion.viewer;

import mrl.util.Configuration;
import mrl.widget.dockable.DockableTabFolder;
import mrl.widget.dockable.MainDockableContainer;
import mrl.widget.dockable.SashFormContainer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public abstract class BaseApplication {

	protected Display display;
	protected Shell shell;
	
	protected MainDockableContainer mainContainer;
	protected SashFormContainer container;
	
	protected String title;
	
	public BaseApplication(){
		title = this.getClass().getSimpleName();
	}
	
	public void open(){
		open(1800, 1200);
	}
	public void open(int width, int height){
		try{
		display = new Display();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());

		mainContainer = new MainDockableContainer(shell, SWT.NONE);
		container = mainContainer.getContainer();
		
		Configuration.DEFAULT_FPS = 30;
		DockableTabFolder.DEFAULT_SIMPLE = true;
		createMenu();
		createInitialUI();
		
		shell.setText(title);
		shell.setSize(width, height);
		shell.setMaximized(true);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		} catch (Throwable t){
			t.printStackTrace();
		}
		System.exit(0);
	}
	
	public void setTitle(String title) {
		this.title = title;
		if (shell != null && !shell.isDisposed()) {
			shell.setText(title);
		}
	}
	
	public Display getDisplay() {
		return display;
	}
	
	public Shell getShell() {
		return shell;
	}
	
	protected void addMenu(Menu parent, String text, int accelerator, final Runnable runnable){
		MenuItem item = new MenuItem (parent, SWT.PUSH);
		item.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				runnable.run();
			}
		});
		item.setText (text);
		if (accelerator != 0){
			item.setAccelerator (accelerator);
		}
	}
	
	protected abstract void createInitialUI();
	protected abstract void createMenu();
}
