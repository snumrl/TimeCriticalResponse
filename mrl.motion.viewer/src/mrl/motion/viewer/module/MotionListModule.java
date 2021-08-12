package mrl.motion.viewer.module;

import java.io.File;

import javax.vecmath.Vector3d;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.MDatabase;
import mrl.motion.data.MotionData;
import mrl.motion.data.MultiCharacterFolder;
import mrl.motion.data.MultiCharacterFolder.MultiCharacterFiles;
import mrl.widget.app.Item;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.app.Module;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class MotionListModule extends Module{
	
	public static Vector3d basicColor = new Vector3d(0.8, 0.8, 0.8);
	public static Vector3d selectedColor = new Vector3d(0.9, 0.9, 0);
	
	private Tree tree;
	private Item rootItem;

	@Override
	protected void initializeImpl() {
		getModule(MainViewerModule.class);
		rootItem = new Item(null);
		rootItem.setLabel("Motion File");
		final ItemListModule itemListModule = getModule(ItemListModule.class);
		itemListModule.addItem(rootItem);
		
		tree = addWindow(new Tree (dummyParent(), SWT.BORDER), WindowPosition.Left);
		tree.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (tree.getSelectionCount() != 1) return;
				itemListModule.removeChildren(rootItem);
//				itemListModule.removeItemByClass(MotionItem.class);
				
				MotionData[] mList = getSelectedMotionData();
				int max = 1;
				
				Vector3d[] colorList = new Vector3d[Math.max(16, mList.length+3)];
				for (int i = 0; i < colorList.length; i++) {
					colorList[i] = basicColor;
				}
				colorList[0] = new Vector3d(1, 0.8, 0.7);
				colorList[1] = new Vector3d(0.7, 1, 0.8);
				colorList[2] = new Vector3d(0.6, 0.6, 1);
				
				for (int i = 0; i < mList.length; i++) {
					MotionData mData = mList[i];
					Item item = new Item(mData);
					item.setLabel(mData.file.getName());
					item.getDescription().color = colorList[i]; 
					itemListModule.addItem(rootItem, item);
					max = Math.max(max, mData.motionList.size());
				}
			}
		});
	}
	
	public MotionData[] getSelectedMotionData(){
		if (tree.getSelectionCount() != 1) return null;
		Object data = tree.getSelection()[0].getData();
		MotionData[] mList = null;
		if (data instanceof MultiCharacterFiles){
			MultiCharacterFiles files = (MultiCharacterFiles)data;
			mList = files.loadBVH();
		} else if (data instanceof MotionData){
			mList = new MotionData[]{ (MotionData) data };
		} else {
			throw new RuntimeException();
		}
		
		for (MotionData motionData : mList){
			try{
				FootContactDetection.checkFootContact(motionData);
			} catch (Exception e){
			}
		}
		return mList;
	}

	public Tree getTree() {
		return tree;
	}

	public void loadMotionFolder(String folder){
		tree.removeAll();
		tree.clearAll(true);
		
		MultiCharacterFolder mFolder = MultiCharacterFolder.loadFolder(new File(folder), false);
		for (MultiCharacterFiles files : mFolder.list){
			TreeItem tItem = new TreeItem (tree, 0);
			tItem.setText(files.label);
			tItem.setData(files);
		}
	}
	
	public void setDatabase(MDatabase database){
		tree.removeAll();
		tree.clearAll(true);
		
		for (MotionData mData : database.getMotionDataList()){
			TreeItem tItem = new TreeItem (tree, 0);
			tItem.setText(mData.file.getName());
			tItem.setData(mData);
		}
	}
}
