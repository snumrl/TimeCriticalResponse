package mrl.widget.app;

import java.util.ArrayList;
import java.util.HashMap;

import mrl.util.Configuration;
import mrl.util.Pair;
import mrl.widget.dockable.DockableTabFolder;
import mrl.widget.dockable.MainDockableContainer;
import mrl.widget.dockable.SashFormContainer;
import mrl.widget.dockable.SashFormContainer.DockingPosition;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class MainApplication {

	public static int WINDOW_WIDTH = 1400;
	public static int WINDOW_HEIGHT = 800;
	
	public enum WindowPosition { 
		Main(), Left(15, 85), Right(60, 40), Bottom(70, 30);
		
		public int[] weights;
		private WindowPosition(int... weights) {
			this.weights = weights;
		}
		
	}
	
	protected Display display;
	protected Shell shell;
	
	protected MainDockableContainer mainContainer;
	protected SashFormContainer container;
	
	protected String title;
	
	protected DockableTabFolder mainFolder;
	protected DockableTabFolder leftFolder;
	protected DockableTabFolder rightFolder;
	protected DockableTabFolder bottomFolder;
	
	protected ArrayList<Module> moduleList = new ArrayList<Module>();
	protected HashMap<String, Module> moduleMap = new HashMap<String, Module>();
	protected boolean isInitialized = false;
	
	private Menu mainMenuBar;
	
	public MainApplication(){
		title = this.getClass().getSimpleName();
		
		display = new Display();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());

		mainContainer = new MainDockableContainer(shell, SWT.NONE);
		container = mainContainer.getContainer();
		
		mainFolder = new DockableTabFolder(container, SWT.BORDER);
		container.dropInitialControl(mainFolder);
		
		Configuration.DEFAULT_FPS = 30;
		DockableTabFolder.DEFAULT_SIMPLE = true;
	}
	
//	public Menu get
	
	public <E extends Module> E addModule(E module){
		module.setApplication(this);
		moduleList.add(module);
		moduleMap.put(module.getClass().getName(), module);
		if (isInitialized && module.isInitializeOnStart()){
			module.initialize();
		}
		return module;
	}
	
	public Composite dummyParent(){
		return mainContainer;
	}
	
	public void showTabItem(String name){
		mainContainer.showTabItem(name);
	}
	
	public MainDockableContainer getMainContainer() {
		return mainContainer;
	}

	public <E extends Composite> E addWindow(E window, String name, WindowPosition position){
		if (position == WindowPosition.Main){
			window.setParent(mainFolder);
			mainFolder.addItem(name, window);
		} else if (position == WindowPosition.Left){
			if (leftFolder == null){
				SashFormContainer container = getCurrentMainContainer();
				leftFolder = new DockableTabFolder(container, SWT.BORDER);
				container.dropNewControl(leftFolder, DockingPosition.Left, null);
				container.getSashForm().setWeights(position.weights);
			}
			window.setParent(leftFolder);
			leftFolder.addItem(name, window);
		} else if (position == WindowPosition.Right){
			if (rightFolder == null){
				SashFormContainer container = getCurrentMainContainer();
				rightFolder = new DockableTabFolder(container, SWT.BORDER);
				container.dropNewControl(rightFolder, DockingPosition.Right, null);
				container.getSashForm().setWeights(position.weights);
			}
			window.setParent(rightFolder);
			rightFolder.addItem(name, window);
		} else if (position == WindowPosition.Bottom){
			if (bottomFolder == null){
//				DockableTabFolder folder = (rightFolder == null) ? mainFolder : rightFolder;
//				SashFormContainer container = (SashFormContainer)folder.getParent();
				SashFormContainer container = getCurrentMainContainer();
				bottomFolder = new DockableTabFolder(container, SWT.BORDER);
				container.dropNewControl(bottomFolder, DockingPosition.Bottom, null);
				container.getSashForm().setWeights(position.weights);
			}
			window.setParent(bottomFolder);
			bottomFolder.addItem(name, window);
		}
		return window;
	}
	
	private SashFormContainer getCurrentMainContainer(){
		return (SashFormContainer)mainFolder.getParent();
	}
	
	public void initializeModules(){
		if (isInitialized) return;
		isInitialized = true;
		for (Module module : moduleList.toArray(new Module[0])){
			if (module.isInitializeOnStart()){
				module.initialize();
			}
			module.createMenus();
		}
	}
	
	@SuppressWarnings("unchecked")
	public <E extends Module> E getModule(Class<E> c){
		E module = (E)moduleMap.get(c.getName());
		if (module == null){
			System.out.println("No Module Registered : " + c.getName());
			try {
				module = (E)c.newInstance();
				addModule(module);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		if (!module.isInitialized()){
			module.initialize();
		}
		return module;
	}
	
	public void open(){
		open(WINDOW_WIDTH, WINDOW_HEIGHT);
	}
	
	public void open(int width, int height){
		try{
			initializeModules();
			
			init();
			
			shell.setText(title);
			shell.setSize(width, height);
			shell.setMaximized(false);
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
	
	protected void init(){
	}
	
	public Display getDisplay() {
		return display;
	}
	
	public Shell getShell() {
		return shell;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	private HashMap<String, Menu> menuMap = new HashMap<String, Menu>();
	
	public Menu getMenu(String name){
		Menu menu = menuMap.get(name);
		if (menu == null){
			if (mainMenuBar == null){
				mainMenuBar = new Menu (shell, SWT.BAR);
				shell.setMenuBar (mainMenuBar);
			}
			MenuItem menuItem = new MenuItem(mainMenuBar, SWT.CASCADE);
			menuItem.setText (name);
			menu = new Menu (shell, SWT.DROP_DOWN);
			menuItem.setMenu (menu);
			menuMap.put(name, menu);
		}
		return menu;
	}

	/**
	 * addMenu("&Menu", "&Test\tCtrl+T", SWT.MOD1 + 'T', new Runnable() { 
	 * 
	 * 
	 * @param parentName
	 * @param text
	 * @param accelerator
	 * @param runnable
	 */
	public void addMenu(String parentName, String text, int accelerator, final Runnable runnable){
		Menu parent = getMenu(parentName);
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
	
	public static void run(Module... modules){
		MainApplication app = new MainApplication();
		for (Module module : modules){
			app.addModule(module);
		}
		app.setTitle(modules[0].getName());
		app.open();
	}
}
