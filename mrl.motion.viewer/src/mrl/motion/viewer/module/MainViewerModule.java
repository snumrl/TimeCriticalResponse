package mrl.motion.viewer.module;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Slider;

import mrl.motion.viewer.SWTViewableCanvas;
import mrl.widget.TimeSlider;
import mrl.widget.WidgetUtil;
import mrl.widget.app.Item;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.app.Module;
import mrl.widget.dockable.DualThumbListener;
import mrl.widget.dockable.DualThumbSlider;

public class MainViewerModule extends Module{
	
	private ArrayList<ItemDrawer> drawerList = new ArrayList<ItemDrawer>();
	
	private MainViewer viewer;
	private TimeSlider tSlider;
	private DualThumbSlider slider;
	private boolean showSelectedItemOnly = false;
	protected LinkedList<Vector3d> cameraTracking = new LinkedList<Vector3d>();
	public static int CAMERA_TRACKING_SIZE = 15;

	protected void addDefaultDrawers(){
		addDrawer(new MotionItemDrawer());
		addDrawer(new PositionMotionItemDrawer());
		addDrawer(new PointItemDrawer());
		addDrawer(new Pose2dItemDrawer());
	}
	
	@Override
	protected void initializeImpl() {
		addDefaultDrawers();
		
		Composite composite = addWindow(new Composite(dummyParent(), SWT.NONE), WindowPosition.Main);
		GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 0;
		layout.marginWidth = layout.marginHeight = 0;
		composite.setLayout(layout);
		
		viewer = new MainViewer(composite);
		viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setScale(0.25);
		
		tSlider = new TimeSlider(composite);
		tSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		viewer.getCanvas().addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				tSlider.onMouseWheel(e);
			}
		});
		
		slider = new DualThumbSlider(tSlider, SWT.NONE);
		slider.setLayoutData(WidgetUtil.gridData(true, false, -1, 20));
		
		slider.addDualThumbListener(new DualThumbListener() {
			@Override
			public void onRightThumbChanged(DualThumbSlider slider) {
				tSlider.setMaxSelection((int)Math.round(slider.getRightSelection()));
			}
			
			@Override
			public void onLeftThumbChanged(DualThumbSlider slider) {
				tSlider.setMinSelection((int)Math.round(slider.getLeftSelection()));
			}
		});
		
		viewer.getCanvas().addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				if ((e.stateMask & SWT.SHIFT) != 0 && (e.stateMask & SWT.ALT) != 0){
					Slider nSlider = tSlider.getSlider();
//					// shift와 alt를 누른 경우 slider의 left, right selection을 조정한다.
					int index = nSlider.getSelection();
					int step = 1;
					if ((e.stateMask & SWT.CTRL) != 0) step = 5;
					
					if (e.count > 0){
						index -= step;
					} else {
						index += step;
					}
					
					boolean isLeft = Math.abs(nSlider.getSelection() - slider.getLeftSelection()) < Math.abs(nSlider.getSelection() - slider.getRightSelection());
					if (isLeft){
						setSelectionBound((int)index, (int)slider.getRightSelection());
						tSlider.setTimeIndex(index);
					} else {
						setSelectionBound((int)slider.getLeftSelection(), (int)index);
						tSlider.setTimeIndex(index);
					}
				}
			}
		});
		
		{
			Menu parent = app().getMenu("&File");
			final MenuItem lItem = new MenuItem (parent, SWT.PUSH);
			lItem.setText ("&Load from File\tCtrl+L");
			lItem.setAccelerator (SWT.MOD1 + 'L');
			lItem.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					FileDialog d = new FileDialog(viewer.getShell(), SWT.OPEN);
					String[] exts = new String[]{ "*.txt" };
					d.setFilterExtensions(exts);
					d.setFilterNames(exts);
					String path = d.open();
					if (path == null) return;
					
					path = new File(path).getParent();
					ItemSerializer.loadFromFile(app().getModule(ItemListModule.class), path);
				}
			});
			final MenuItem sItem = new MenuItem (parent, SWT.PUSH);
			sItem.setText ("&Save to File\tCtrl+S");
			sItem.setAccelerator (SWT.MOD1 + 'S');
			sItem.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					FileDialog d = new FileDialog(viewer.getShell(), SWT.SAVE);
					String[] exts = new String[]{ "*" };
					d.setFilterExtensions(exts);
					d.setFilterNames(exts);
					String path = d.open();
					if (path == null) return;
					
					ItemSerializer.saveToFile(app().getModule(ItemListModule.class), path);
				}
			});
		}
		{
			Menu parent = app().getMenu("&View");
			final MenuItem sItem = new MenuItem (parent, SWT.CHECK);
			sItem.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					showSelectedItemOnly = sItem.getSelection();
				}
			});
			sItem.setText ("Show Selected Item &Only\tCtrl+O");
			sItem.setAccelerator (SWT.MOD1 + 'O');
			final MenuItem fItem = new MenuItem (parent, SWT.CHECK);
			fItem.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					MotionItemDrawer.drawFootContact = fItem.getSelection(); 
				}
			});
			fItem.setText ("Show &Foot Contact\tCtrl+F");
			fItem.setAccelerator (SWT.MOD1 + 'F');
			
			final MenuItem pItem = new MenuItem (parent, SWT.CHECK);
			pItem.setText ("Print current frame data info");
			tSlider.getSlider().addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (pItem.getSelection()){
						printItemInfo();
					}
				}
			});
		}
		
		getModule(ItemListModule.class).addModifyListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateTimeSlider();
			}
		});
		updateTimeSlider();
	}
	
	public void addTimeListener(Listener listener){
		tSlider.getSlider().addListener(SWT.Selection, listener);
	}
	
	public Point3d getPickPoint(){
		viewer.doAllwaysPicking = true;
		if (viewer.pickPoint == null) return null;
		Point3d target = new Point3d(viewer.pickPoint);
		target.scale(1/viewer.scale);
		target.y = 0;
		return target;
	}
	
	public void addCameraTracking(Tuple3d p){
		double scale = viewer.scale;
		Vector3d rootP = new Vector3d(p);
		rootP.scale(scale);
		cameraTracking.add(rootP);
		if (cameraTracking.size() > CAMERA_TRACKING_SIZE){
			cameraTracking.removeFirst();
		}
		Vector3d mean = new Vector3d();
		for (Vector3d v : cameraTracking){
			mean.add(v);
		}
		mean.scale(1d/cameraTracking.size());
		Vector3d diff = new Vector3d();
		diff.sub(mean, viewer.center);
		diff.scale(0.1);
		diff.y = 0;
		viewer.center.add(diff);
		viewer.eye.add(diff);
	}
	
	public void setCameraCenter(Tuple3d p) {
		double scale = viewer.scale;
		Vector3d rootP = new Vector3d(p);
		rootP.scale(scale);
		
		Vector3d diff = new Vector3d();
		diff.sub(rootP, viewer.center);
		diff.y = 0;
		viewer.center.add(diff);
		viewer.eye.add(diff);
	}
	
	private void printItemInfo(){
		int timeIndex = tSlider.getTimeIndex();
		ArrayList<ItemMatch> list = collectItems();
		System.out.println("Time Index : " + timeIndex);
		for (ItemMatch match : list){
			Item item = match.item;
			String str = match.drawer.toString(match.item, match.data, timeIndex);
			if (str == null) continue;
			if (!item.isVisible()) str = "(H)" + str;
			if (item.isSelected()) str = "*" + str;
			str = item.getLabel()  + "=" + str;
			System.out.println(str);
		}
	}
	
	private void updateTimeSlider(){
		int timeLen = -1;
		ArrayList<Item> itemList = getModule(ItemListModule.class).getItemList();
		for (Item item : itemList){
			ItemDrawer drawer = getDrawer(item.getData());
			if (drawer != null){
				timeLen = Math.max(timeLen, drawer.getTimeLength(item.getData()));
			}
			if (item.getData() instanceof TimeBasedList){
				TimeBasedList<?> tList = (TimeBasedList<?>)item.getData();
				timeLen = Math.max(timeLen, tList.size());
			}
		}
		if (timeLen > 0 && tSlider.getMaxFrame() != timeLen){
			setMaxFrame(timeLen);
		}
	}
	
	public void addKeyListener(KeyListener listener) {
		viewer.getCanvas().addKeyListener(listener);
	}
	
	public MainViewer getMainViewer(){
		return viewer;
	}
	
	public void addDrawer(ItemDrawer drawer){
		drawerList.add(drawer);
	}
	
	public void replay(){
		tSlider.stopAnimation();
		tSlider.setTimeIndex(0);
		tSlider.startAnimation();
	}
	
	public void play(){
		tSlider.stopAnimation();
		tSlider.startAnimation();
	}
	
	public int getTimeIndex(){
		return tSlider.getTimeIndex();
	}
	
	public void setTimeIndex(int index){
		tSlider.stopAnimation();
		tSlider.setTimeIndex(index);
	}
	
	public void setMaxFrame(int max){
		tSlider.stopAnimation();
		
		int min = 1;
		slider.setBound(min, max);
		setSelectionBound(min, max);
		tSlider.setMaxFrame(max);
	}
	
	public void setSelectionBound(int min, int max){
		slider.setLeftSelection(min);
		slider.setRightSelection(max);
		tSlider.setMaxSelection(max);
		tSlider.setMinSelection(min);
	}
	
	public void selectAll(){
		setSelectionBound((int)slider.getMinimum(), (int)slider.getMaximum());
	}

	private ItemDrawer getDrawer(Object data){
		for (ItemDrawer drawer : drawerList){
			if (drawer.isDrawable(data)){
				return drawer;
			}
		}
		return null;
	}
	
	private ArrayList<ItemMatch> collectItems(){
		ArrayList<Item> itemList = getModule(ItemListModule.class).getItemList();
		ArrayList<ItemMatch> list = new ArrayList<ItemMatch>();
		for (Item item : itemList){
			collectItems(list, item, item.getData());
		}
		return list;
	}
	
	private void collectItems(ArrayList<ItemMatch> matchList, Item item, Object data){
		ItemDrawer drawer = getDrawer(data);
		if (drawer != null){
			matchList.add(new ItemMatch(drawer, item, data));
		} else {
			if (data instanceof TimeBasedList){
				int timeIndex = tSlider.getTimeIndex();
				TimeBasedList<?> tList = (TimeBasedList<?>)data;
				if (timeIndex >= 0 && timeIndex < tList.size()){
					collectItems(matchList, item, tList.get(timeIndex));
				}
			} else if (data instanceof List){
				List<?> list = (List<?>)data;
				if (list.size() == 0) return;
				for (Object lData : list){
					collectItems(matchList, item, lData);
				}
			} else if (data instanceof DescriptedData) {
				DescriptedData dData = (DescriptedData)data;
				drawer = getDrawer(dData.data);
				if (drawer != null){
					matchList.add(new ItemMatch(drawer, item, data));
				}
			}
		}
	}
	
	private static class ItemMatch{
		ItemDrawer drawer;
		Item item;
		Object data;
		
		public ItemMatch(ItemDrawer drawer, Item item, Object data) {
			this.drawer = drawer;
			this.item = item;
			this.data = data;
		}
	}
	
	public class MainViewer extends SWTViewableCanvas{

		public MainViewer(Composite parent) {
			super(parent);
		}

		@Override
		protected void drawObjects() {
			int timeIndex = tSlider.getTimeIndex();
			ArrayList<ItemMatch> list = collectItems();
			for (ItemMatch match : list){
				Item item = match.item;
				if (!item.isVisible()) continue;
				if (showSelectedItemOnly && !item.isSelected()) continue;
				
				Object data = match.data;
				ItemDescription desc = item.getDescription();
				if (data instanceof DescriptedData) {
					DescriptedData dData = (DescriptedData)data;
					data = dData.data;
					desc = dData.description;
					desc.item = match.item;
				}
				match.drawer.draw(gl, glu, glut, desc, data, timeIndex);
			}
//			ArrayList<Item> itemList = getModule(ItemListModule.class).getItemList();
//			for (Item item : itemList){
//				drawData(item, item.getData(), timeIndex);
//			}
		}
		
//		protected void drawData(Item item, Object data, int timeIndex){
//			if (!item.isVisible()) return;
//			if (showSelectedItemOnly && !item.isSelected()) return;
//			ItemDrawer drawer = getDrawer(data);
//			if (drawer != null){
//				drawer.draw(gl, glu, glut, item, data, timeIndex);
//			} else {
//				if (data instanceof TimeBasedList){
//					TimeBasedList<?> list = (TimeBasedList<?>)data;
//					if (timeIndex >= 0 && timeIndex < list.size()){
//						drawData(item, list.get(timeIndex), timeIndex);
//					}
//				} else if (data instanceof List){
//					List<?> list = (List<?>)data;
//					if (list.size() == 0) return;
//					for (Object lData : list){
//						drawData(item, lData, timeIndex);
//					}
//				}
//			}
//		}

		@Override
		protected void init() {
		}
		
	}
	
	public static void run(Object ...items){
		runWithDescription(items, null);
	}
	
	public static void runWithDescription(Object[] items, ItemDescription[] descList){
		MainApplication app = new MainApplication();
		app.addModule(new MainViewerModule());
		app.initializeModules();
		
		ItemListModule itemModule = app.getModule(ItemListModule.class);
		for (int i = 0; i < items.length; i++) {
			itemModule.addSingleItem("Item" + (i+1), items[i], (descList != null ? descList[i] : null));
		}
		app.open();
	}
	
}
