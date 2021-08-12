package mrl.widget;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public abstract class ShellOpener {
	
	protected Shell shell;
	protected Menu menu;
	
	public static void open(ShellOpener opener){
		open(null, 1400, 1000, opener);
	}
	
	public static void open(String title, int width, int height, ShellOpener opener){
		final Display display = new Display();
		Shell shell = new Shell(display);
		opener.shell = shell;
		
		shell.setLayout(new FillLayout());
		
		opener.createComposite(shell);
		
		if (title != null){
			shell.setText(title);
		}
		shell.setSize(width, height);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		System.exit(0);
	}
	
	private void initMenu(){
		Menu bar = new Menu (shell, SWT.BAR);
		shell.setMenuBar (bar);
		MenuItem menuItem = new MenuItem (bar, SWT.CASCADE);
		menuItem.setText ("&Menu");
		menu = new Menu (shell, SWT.DROP_DOWN);
		menuItem.setMenu (menu);
	}
	
	protected void addMenu(String text, int accelerator, final Runnable runnable){
		if (menu == null){
			initMenu();
		}
		
		MenuItem item = new MenuItem (menu, SWT.PUSH);
		item.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				runnable.run();
			}
		});
		item.setText (text);
		item.setAccelerator (accelerator);
	}

	public abstract void createComposite(Shell shell);
}
