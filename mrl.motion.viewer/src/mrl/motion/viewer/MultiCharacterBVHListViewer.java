package mrl.motion.viewer;

import java.io.File;
import java.util.ArrayList;

import javax.vecmath.Vector3d;

import mrl.motion.data.MotionData;
import mrl.motion.data.MultiCharacterFolder;
import mrl.motion.data.MultiCharacterFolder.MultiCharacterFiles;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.viewer.MotionListViewer.MotionListGetter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class MultiCharacterBVHListViewer extends Composite{
	
	private ArrayList<MultiCharacterFolder> folderList = new ArrayList<MultiCharacterFolder>();
	
	private Combo folderCombo;
	private MotionListViewer motionListViewer;

	public MultiCharacterBVHListViewer(Composite parent) {
		this(parent, true);
	}
	public MultiCharacterBVHListViewer(Composite parent, boolean showFolderOption) {
		super(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 1;
		setLayout(layout);
		
		if (showFolderOption){
			Button changeFolder = new Button(this, SWT.PUSH);
			changeFolder.setText("Change Folder");
			changeFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			changeFolder.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					FileDialog d = new FileDialog(getShell(), SWT.OPEN);
					d.setFilterExtensions(new String[]{ "*.bvh" });
					String path = d.open();
					if (path == null) return;
	
					File f = new File(path);
					setRootFolder(f.getParentFile().getAbsoluteFile());
					
					onFolderChange(f.getParentFile().getAbsoluteFile());
				}
			});
			
			folderCombo = new Combo(this, SWT.BORDER | SWT.READ_ONLY);
			folderCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			folderCombo.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					updateFolderSelection();
				}
			});
		}
		
		
		
		motionListViewer = new MotionListViewer(this);
		motionListViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Vector3d[] colors = motionListViewer.getMotionSelector().getNavigator().getColorList();
		colors[0] = new Vector3d(1, 0.8, 0.7);
		colors[1] = new Vector3d(0.7, 1, 0.8);
		colors[2] = new Vector3d(0.6, 0.6, 1);
//		colors[3] = new Vector3d(1, 0.8, 0.7);
	}
	
	public MotionListViewer getMotionListViewer() {
		return motionListViewer;
	}
	
	protected void onFolderChange(File root){
	}

	public void setRootFolder(File root){
		folderList = MultiCharacterFolder.loadChildFolders(root);
		
		if (folderCombo != null){
			folderCombo.removeAll();
			for (MultiCharacterFolder f : folderList) {
				folderCombo.add(f.folder.getAbsolutePath());
			}
			if (folderCombo.getItemCount() > 0){
				folderCombo.select(0);
			}
		}
		
		
		updateFolderSelection();
	}
	
	public ArrayList<File> getSelectedMotionFiles(){
		MultiFileMotionGetter getter = (MultiFileMotionGetter)motionListViewer.getMotionDataList().get(motionListViewer.getMotionDataIndex());
		return getter.files.fileList;
	}
	
	protected void updateFolderSelection(){
		int idx;
		if (folderCombo != null){
			idx = folderCombo.getSelectionIndex();
		} else {
			idx = 0;
		} 
		if (idx < 0) return;
		
		
		MultiCharacterFolder folder = folderList.get(idx);
		ArrayList<MotionListGetter> list = motionListViewer.getMotionDataList();
		list.clear();
		
		for (MultiCharacterFiles files : folder.list){
			list.add(new MultiFileMotionGetter(files));
		}
		motionListViewer.setMotionDataList(list);
	}
	
	public static class MultiFileMotionGetter implements MotionListGetter{
		
		private MultiCharacterFiles files;
		
		public MultiFileMotionGetter(MultiCharacterFiles files) {
			this.files = files;
		}
		
		public MultiCharacterFiles getFiles() {
			return files;
		}
		@Override
		public MotionData[] getMotionDataList() {
			ArrayList<File> fileList = files.fileList;
			MotionData[] motionList = new MotionData[fileList.size()];
			for (int i = 0; i < fileList.size(); i++) {
				MotionData motionData = new BVHParser().parse(fileList.get(i));
				motionList[i] = motionData;
			}
			return motionList;
		}

		@Override
		public String getLabel() {
			return files.label;
		}
		
	}
	
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());

		SkeletonData.USE_SINGLE_SKELETON = false;
		MultiCharacterBVHListViewer viewer = new MultiCharacterBVHListViewer(shell);
		Vector3d[] colors = viewer.getMotionListViewer().getMotionSelector().getNavigator().getColorList();
		for (int i = 0; i < colors.length; i++) {
			colors[i].set(MultiCharacterNavigator.basicColor);
		}
//		viewer.getMotionListViewer().getMotionSelector().getNavigator().getViewer().setBoneThickness(0.1);
		
//		SWTViewableCanvas.isXUp = true;
		MultiCharacterFolder.DEFAULT_MERGE = false;
		viewer.setRootFolder(new File("").getAbsoluteFile());
//		viewer.setRootFolder(new File("test\\temp"));
		
		shell.setText("Motion List");
		shell.setSize(1200, 1000);
		shell.setMaximized(true);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		System.exit(0);
	}
}
