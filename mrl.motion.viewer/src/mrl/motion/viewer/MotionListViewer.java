package mrl.motion.viewer;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;

import mrl.motion.data.FootContactDetection;
import mrl.motion.data.MotionData;

public class MotionListViewer extends Composite{

	
	protected ArrayList<MotionListGetter> motionDataList = new ArrayList<MotionListViewer.MotionListGetter>();
	
	protected List motionListControl;
	protected MotionIntervalSelector motionSelector;
	
	protected MotionData[] currentMotionList;

	public MotionListViewer(Composite parent) {
		this(parent, 175);
	}
	public MotionListViewer(Composite parent, int listWidth) {
		super(parent, SWT.NONE);
		
		this.setLayout(new FillLayout());
		SashForm sash = new SashForm(this, SWT.HORIZONTAL);
		
		motionListControl = new List(sash, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		motionListControl.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateMotionSelection();
			}
		});
		motionSelector = new MotionIntervalSelector(sash);;
		sash.setWeights(new int[] { 2, 16 });
	}
	
	public MotionIntervalSelector getMotionSelector() {
		return motionSelector;
	}
	
	public List getMotionListControl() {
		return motionListControl;
	}
	
	public ArrayList<MotionListGetter> getMotionDataList() {
		return motionDataList;
	}
	
	public void setMotionDataIndex(int index){
		motionListControl.select(index);
		motionListControl.notifyListeners(SWT.Selection, new Event());
	}

	public void setMotionDataList(ArrayList<MotionListGetter> motionDataList) {
		this.motionDataList = motionDataList;
		updateMotionList(false);
	}
	
	public void updateMotionList(boolean resetSelection){
		motionListControl.removeAll();
		String[] items = new String[motionDataList.size()];
		for (int i = 0; i < items.length; i++) {
			items[i] = motionDataList.get(i).getLabel();
		}
		motionListControl.setItems(items);
		motionListControl.select(0);
		motionListControl.notifyListeners(SWT.Selection, new Event());
	}

	protected void updateMotionSelection(){
		int idx = motionListControl.getSelectionIndex();
		if (idx < 0) return;
		
		currentMotionList = motionDataList.get(idx).getMotionDataList();
		for (MotionData motionData : currentMotionList){
			FootContactDetection.checkFootContact(motionData);
		}
//		for (int i = 0; i < motionList.length; i++) {
//			currentMotionList[i].skeletonData = currentMotionList[0].skeletonData;
//		}
		motionSelector.getNavigator().stopAnimation();
		motionSelector.setMotionData(currentMotionList);
		motionSelector.getNavigator().startAnimation();
		
	}
	
	public MotionData[] getCurrentMotionList() {
		return currentMotionList;
	}
	
	public int getMotionDataIndex(){
		return motionListControl.getSelectionIndex();
	}
	
	public int getMotionIndex(){
		return motionSelector.getNavigator().getMotionIndex();
	}
	
	
	public static interface MotionListGetter{
		public MotionData[] getMotionDataList();
		public String getLabel();
	}
	
	public static class SingleMotionGetter implements MotionListGetter{
		private String label;
		private MotionData motionData;
		public SingleMotionGetter(String label, MotionData motionData) {
			this.label = label;
			this.motionData = motionData;
		}
		@Override
		public MotionData[] getMotionDataList() {
			return new MotionData[] { motionData };
		}
		@Override
		public String getLabel() {
			return label;
		}
	}
	
	public static class MultiMotionGetter implements MotionListGetter{
		
		private String label;
		private MotionData[] motionDataList;
		
		public MultiMotionGetter(String label, MotionData[] motionDataList) {
			this.label = label;
			this.motionDataList = motionDataList;
		}

		@Override
		public MotionData[] getMotionDataList() {
			return motionDataList;
		}

		@Override
		public String getLabel() {
			return label;
		}
		
	}
}
