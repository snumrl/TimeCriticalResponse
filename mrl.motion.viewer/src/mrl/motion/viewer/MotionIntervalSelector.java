package mrl.motion.viewer;

import java.io.File;

import mrl.motion.data.MotionData;
import mrl.motion.data.parser.BVHParser;
import mrl.widget.dockable.DualThumbListener;
import mrl.widget.dockable.DualThumbSlider;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Slider;

public class MotionIntervalSelector extends Composite{
	
	private MultiCharacterNavigator navigator;
	private DualThumbSlider slider;

	public MotionIntervalSelector(Composite parent) {
		super(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(2, true);
		layout.marginWidth = layout.marginHeight = 0;
		this.setLayout(layout);
		
		navigator = new MultiCharacterNavigator(this);
		navigator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		slider = new DualThumbSlider(navigator, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.heightHint = 20;
		slider.setLayoutData(data);
		
		slider.addDualThumbListener(new DualThumbListener() {
			@Override
			public void onRightThumbChanged(DualThumbSlider slider) {
				navigator.setMaxSelection((int)Math.round(slider.getRightSelection()));
			}
			
			@Override
			public void onLeftThumbChanged(DualThumbSlider slider) {
				navigator.setMinSelection((int)Math.round(slider.getLeftSelection()));
			}
		});
		
		navigator.getViewer().getCanvas().addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				if ((e.stateMask & SWT.SHIFT) != 0 && (e.stateMask & SWT.ALT) != 0){
					Slider nSlider = navigator.getSlider();
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
						navigator.setMotionIndex(index);
					} else {
						setSelectionBound((int)slider.getLeftSelection(), (int)index);
						navigator.setMotionIndex(index);
					}
				}
			}
		});
	}

	public MultiCharacterNavigator getNavigator() {
		return navigator;
	}
	
	public void setMotionData(MotionData[] motionDataList){
		int min = 1;
		int max = motionDataList[0].motionList.size();
//		int max = motionDataList[0].motionList.size() + 1;
		setMotionData(motionDataList, min, max);
	}
	
	public void setMotionData(MotionData[] motionDataList, int min, int max){
		slider.setBound(min, max);
		setSelectionBound(min, max);
		navigator.setMotionDataList(motionDataList, max);
	}
	
	public void setMotionData(File bvhFile){
		String name = bvhFile.getName();
		int idx = name.lastIndexOf("_");
		int person = Integer.parseInt(name.substring(idx+1, idx+2));
		
		int persons = person;
		while (true) {
			persons++;
			String name2 = name.substring(0, idx + 1) + persons + name.substring(idx+2, name.length());
			if (new File(bvhFile.getParentFile().getAbsolutePath() + "\\" + name2).exists() == false){
				persons--;
				break;
			}
		}
		
		
//		String name2 = name.substring(0, idx + 1) + ((person)%2 + 1) + name.substring(idx+2, name.length());
		MotionData[] motionDataList = new MotionData[persons];
		for (int i = 0; i < motionDataList.length; i++) {
			int pIdx = (person+i)%persons;
			if (pIdx == 0) pIdx = persons;
			String name2 = name.substring(0, idx + 1) + pIdx + name.substring(idx+2, name.length());
			motionDataList[i] = new BVHParser().parse(new File(bvhFile.getParentFile().getAbsolutePath() + "\\" + name2));
		}
//		motionDataList[0] = new BVHParser().parse(bvhFile);
//		motionDataList[1] = new BVHParser().parse(new File(bvhFile.getParentFile().getAbsolutePath() + "\\" + name2));
		setMotionData(motionDataList);
	}
	
	public void setSelectionBound(int min, int max){
		slider.setLeftSelection(min);
		slider.setRightSelection(max);
		navigator.setMaxSelection(max);
		navigator.setMinSelection(min);
	}
	
	public void selectAll(){
		setSelectionBound((int)slider.getMinimum(), (int)slider.getMaximum());
	}
	
	

}
