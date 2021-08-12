package mrl.widget.app;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import mrl.widget.ValueSlider;
import mrl.widget.WidgetUtil;
import mrl.widget.app.MainApplication.WindowPosition;

public class SliderModule extends Module{
	
	private Composite container;
	private ArrayList<ValueSlider> sliderList = new ArrayList<ValueSlider>();
	
	private int rangeMin = 1, rangeMax = 300;
	
	@Override
	protected void initializeImpl() {
		container = addWindow(new Composite(dummyParent(), SWT.NONE), WindowPosition.Right);
		container.setLayout(new GridLayout(1, true));
		
		setSliderSize(1);
	}
	
	public void setDefaultRange(int min, int max){
		rangeMin = min;
		rangeMax = max;
	}
	
	public int[] getSliderValues(){
		int[] values = new int[sliderList.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = sliderList.get(i).getSelection();
		}
		return values;
	}

	public void setSliderSize(int size){
		while (sliderList.size() > size){
			ValueSlider bar = sliderList.remove(sliderList.size() - 1);
			bar.dispose();
		}
		for (int i = sliderList.size(); i < size; i++) {
			ValueSlider bar = new ValueSlider(container);
			bar.setLayoutData(WidgetUtil.gridData(true, false));
			sliderList.add(bar);
		}
		for (ValueSlider bar : sliderList){
			bar.setRange(rangeMin, rangeMax);
		}
	}
	
	public void setSliderValues(int[] values){
		setSliderSize(values.length);
		for (int i = 0; i < values.length; i++) {
			sliderList.get(i).setSelection(values[i]);
		}
	}

	public ArrayList<ValueSlider> getSliderList() {
		return sliderList;
	}
}
