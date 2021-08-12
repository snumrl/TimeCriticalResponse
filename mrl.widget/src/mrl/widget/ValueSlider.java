package mrl.widget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

public class ValueSlider extends Composite {
	
	protected Slider slider;
	protected Text number;

	private int minSelection = 1;
	private int maxSelection = Integer.MAX_VALUE;
	
	public ValueSlider(Composite parent) {
		super(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(2, false);
		layout.verticalSpacing = 0;
		layout.marginWidth = layout.marginHeight = 0;
		this.setLayout(layout);
		
		slider = new Slider(this, SWT.NONE);
		slider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		number = new Text(this, SWT.BORDER | SWT.READ_ONLY);
		GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
		data.widthHint = 50;
		number.setLayoutData(data);
		
		slider.setMinimum(1);
		slider.setEnabled(false);
		
		slider.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateValue();
			}
		});
	}
	
	private void updateValue(){
		number.setText("" + slider.getSelection());
	}
	
	public void setRange(int min, int max){
		minSelection = min;
		maxSelection = max;
		slider.setEnabled(true);
		slider.setMinimum(minSelection);
		slider.setMaximum(maxSelection + slider.getThumb());
		slider.setSelection(min);
		updateValue();
	}
	
	public void setSelection(int value){
		slider.setSelection(value);
		updateValue();
	}

	public Slider getSlider() {
		return slider;
	}

	public int getSelection(){
		return slider.getSelection();
	}
}
