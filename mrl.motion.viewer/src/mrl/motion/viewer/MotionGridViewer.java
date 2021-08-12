package mrl.motion.viewer;

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import mrl.motion.data.MotionData;
import mrl.util.MathUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

public class MotionGridViewer extends Composite{
	
	public static int SELECTED_COLOR = SWT.COLOR_YELLOW;
	private static final String INDEX = "index";
	
	private int columns = -1;
	private ArrayList<Composite> containerList = new ArrayList<Composite>();
	private ArrayList<SynchronizedViewer> viewerList = new ArrayList<SynchronizedViewer>();
	private Rectangle[] originPositions;
	private ScrollBar vBar;
	
	private int selection = -1;
	private int focus = -1;
	private double playSpeed = 1;
	private int redrawTime = 25;
	
	private long startTime = -1;
	private int dataSize;
	
	private boolean[] highlighted;
	
	public MotionGridViewer(Composite parent) {
		super(parent, SWT.BORDER | SWT.V_SCROLL);
		
		this.addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				updateLayout();
			}
			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
		
		vBar = getVerticalBar();
		vBar.setIncrement(25);
		vBar.setVisible(false);
		vBar.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				onVScroll();
			}
		});
	}
	
	public void setRedrawTime(int redrawTime) {
		this.redrawTime = redrawTime;
	}

	public double getPlaySpeed() {
		return playSpeed;
	}

	public void setPlaySpeed(double playSpeed) {
		this.playSpeed = playSpeed;
	}

	public boolean[] getHighlighted() {
		return highlighted;
	}
	
	public MotionData[] getHighlightedMotionData() {
		for (int i = 0; i < highlighted.length; i++) {
			if (highlighted[i]) {
				return viewerList.get(i).getMotionDataList();
			}
		}
		return null;
	}
	
	public MotionData[] getSelectedMotionData() {
		if (selection < 0) return null;
		return viewerList.get(selection).getMotionDataList();
	}

	private void onVScroll(){
		int offset = vBar.getSelection();
		for (int i = 0; i < dataSize; i++) {
			Rectangle b = originPositions[i];
			containerList.get(i).setBounds(b.x, b.y - offset, b.width, b.height);
		}
	}
	
	public ArrayList<MultiCharacterViewer> getViewerList() {
		ArrayList<MultiCharacterViewer> list = new ArrayList<MultiCharacterViewer>();
		for (int i = 0; i < dataSize; i++) {
			list.add(viewerList.get(i));
		}
		return list;
	}

	private void updateLayout(){
		int cellWidth = (getSize().x - 20) / columns;
		if (cellWidth == 0) return;
		
		int cellHeight = MathUtil.round(cellWidth*0.8);
		int margin = 0;
		originPositions = new Rectangle[dataSize];
		for (int i = 0; i < dataSize; i++) {
			int c = i % columns;
			int r = i / columns;
			Rectangle b = new Rectangle(c*cellWidth + margin, cellHeight*r + margin, cellWidth - margin*2, cellHeight - margin*2);
			originPositions[i] = b;
			containerList.get(i).setBounds(b);
		}
		
		int needHeight = cellHeight * (int)Math.ceil(dataSize / (double)columns);
		if (needHeight < getSize().y){
			vBar.setVisible(false);
		} else {
			vBar.setVisible(true);
			vBar.setMinimum(0);
			vBar.setMaximum(needHeight);
			vBar.setThumb(getSize().y);
			vBar.setSelection(0);
			onVScroll();
		}
	}
	
	public void setMotionData(ArrayList<MotionData[]> data, Vector3d[] colors, int columns){
		setMotionData(data, colors, columns, -1);
	}
	public void setMotionData(ArrayList<MotionData[]> data, Vector3d[] colors, int columns, int focus){
		this.focus = focus;
		this.columns = columns;
		selection = -1;
		startTime = System.currentTimeMillis();
		
		dataSize = data.size();
		highlighted = new boolean[data.size()];
		
		if (data.size() > viewerList.size()){
			for (int i = viewerList.size(); i < data.size(); i++) {
				createViewer(i);
			}
		}
		
		for (int i = 0; i < viewerList.size(); i++) {
			if (i < data.size()){
				viewerList.get(i).setMotionDataList(data.get(i));
				viewerList.get(i).setSkeletonColorList(colors);
			} else {
				viewerList.get(i).setMotionDataList(null);
			}
		}
		updateLayout();
		
		setSelection(-1);
		updateContainerColor();
	}
	
	private void createViewer(final int idx){
		Composite composite = new Composite(this, SWT.NONE);
		composite.setData(INDEX, idx);
		FillLayout layout = new FillLayout();
		layout.marginWidth = 3;
		layout.marginHeight = 3;
		composite.setLayout(layout);
		containerList.add(composite);
		
		final SynchronizedViewer viewer = new SynchronizedViewer(composite, idx);
		viewerList.add(viewer);
	}
	
	public int getSelection() {
		return selection;
	}

	private void setSelection(int index){
		selection = index;
		updateContainerColor();
		this.notifyListeners(SWT.Selection, new Event());
	}
	
	private void onMouseDoubleClick() {
		updateContainerColor();
		this.notifyListeners(SWT.MouseDoubleClick, new Event());
	}
	
	private void updateContainerColor(){
		for (Composite container : containerList) {
//			if (!container.isVisible()) continue;
			int idx = (Integer)container.getData(INDEX);
			Color c;
			if (idx < highlighted.length && highlighted[idx]){
				c = getDisplay().getSystemColor(SWT.COLOR_RED);
			} else if (idx == focus){
				c = getDisplay().getSystemColor(SWT.COLOR_GREEN);
			} else if (idx == selection){
				c = getDisplay().getSystemColor(SELECTED_COLOR);
			} else {
				c = getDisplay().getSystemColor(SWT.COLOR_WHITE);
			}
			container.setBackground(c);
		}
	}
	
	
	private class SynchronizedViewer extends MultiCharacterViewer{
		
		private int idx;
		
		private double frameTime;
		private int maxFrame;

		public SynchronizedViewer(Composite parent, final int idx) {
			super(parent);
			this.idx = idx;
			
			this.setEnableWheel(false);
			this.setEnableDoubleClick(false);
//			this.setDrawBox(false);
			this.setDrawPlane(false);
			this.setPanningRatio(3);
			
			setVisible(false);
			getParent().setVisible(false);
			
			
			getCanvas().addListener(SWT.MouseDown, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (!isVisible()) return;
					
					setSelection(idx);
				}
			});
			getCanvas().addListener(SWT.MouseDoubleClick, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (!isVisible()) return;
					
					highlighted[idx] = !highlighted[idx];
					onMouseDoubleClick();
				}
			});
		}
		
		@Override
		public void setMotionDataList(MotionData[] motionDataList) {
			if (motionDataList == null){
				setVisible(false);
				getParent().setVisible(false);
				SynchronizedViewer.this.setRedrawTime(1000);
				return;
			}
			
			setVisible(true);
			getParent().setVisible(true);
			SynchronizedViewer.this.setRedrawTime(MotionGridViewer.this.redrawTime);
			frameTime = 1000/(double)motionDataList[0].framerate;
			maxFrame = MotionData.maxFrame(motionDataList);
			super.setMotionDataList(motionDataList);
		}
		
		@Override
		protected void drawObjectsImpl() {
			if (!isVisible()) return;
			
			long timePass = System.currentTimeMillis() - startTime;
			int mIndex = (int)(timePass * playSpeed / frameTime);
			mIndex = mIndex % maxFrame;
			
			setAnimationIndex(mIndex);
			
			if (selection >= 0 && selection != idx){
				MultiCharacterViewer v = viewerList.get(selection);
				this.eye = new Vector3d(v.eye);
				this.center = new Vector3d(v.center);
				this.upVector = new Vector3d(v.upVector);
			}
			
			super.drawObjectsImpl();
		}
		
	}
}
