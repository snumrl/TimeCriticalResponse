package mrl.widget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

public class TimeSlider extends Composite{

	protected Slider slider;
	protected Text number;
	
	protected Button animationButton;
	
	private double frameTime = 1000/30d;
	private long startTime = -1;
	private Runnable currentAnimator = null;
	
	private int maxFrame;
	
	private int minSelection = 0;
	private int maxSelection = Integer.MAX_VALUE;
	
	protected boolean goStartAfterPlay = false;
	protected boolean startFromZero = true;
	
	public TimeSlider(Composite parent) {
		this(parent, false);
	}
	public TimeSlider(Composite parent, boolean bigButton) {
		super(parent, SWT.NONE);
		
		int columns = bigButton ? 2 : 3;
		GridLayout layout = new GridLayout(columns, false);
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
				if (slider.getSelection() < minSelection){
					slider.setSelection(minSelection);
					slider.redraw();
				}
				if (slider.getSelection() > maxSelection){
					slider.setSelection(maxSelection);
					slider.redraw();
				}
				updateTimeIndex();
			}
		});
		
		animationButton = new Button(this, SWT.PUSH);
		if (bigButton){
			animationButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, columns, 1));
		} else {
			GridData buttonData = new GridData(SWT.FILL, SWT.FILL, false, false);
			buttonData.widthHint = 100;
			animationButton.setLayoutData(buttonData);
		}
		animationButton.setText("Play");
		animationButton.setEnabled(false);
		animationButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (startTime <= 0){
					startAnimation();
				} else {
					stopAnimation();
				}
			}
		});
		
	}
	
	public void onMouseWheel(MouseEvent e){
		if ((e.stateMask & SWT.SHIFT) != 0){
//			// shift 를 누른 경우 slider를 조정한다.
			int index = slider.getSelection();
			int step = 1;
			if ((e.stateMask & SWT.CTRL) != 0) step = 5;
			
			if (e.count > 0){
				index -= step;
			} else {
				index += step;
			}
			index = Math.max(index, slider.getMinimum());
			index = Math.min(index, slider.getMaximum());
			slider.setSelection(index);
			slider.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	public Slider getSlider() {
		return slider;
	}
	
	public void setGoStartAfterPlay(boolean goStartAfterPlay) {
		this.goStartAfterPlay = goStartAfterPlay;
	}
	
	public void setStartFromZero(boolean startFromZero) {
		this.startFromZero = startFromZero;
	}

	public void startAnimation(){
		animationButton.setText("Stop");
		startTime = System.currentTimeMillis();
		
		int index = slider.getSelection();
		if (index >= Math.min(maxFrame, maxSelection) - 1){
			index = minSelection - 1;
		}
		
		final int startIndex = index;
		currentAnimator = new Runnable() {
			@Override
			public void run() {
				if (currentAnimator != this || slider.isDisposed()) return;
				
				long timePass = System.currentTimeMillis() - startTime;
				int mIndex = startIndex + (int)(timePass / frameTime);
				boolean isStop = false;
				if (mIndex >= maxFrame || mIndex >= maxSelection){
					isStop = true;
					
					if (goStartAfterPlay){
						mIndex = minSelection - 1;
					} else {
						mIndex = Math.min(maxFrame, maxSelection) - 1;
					}
				}
				slider.setSelection(mIndex + 1);
				slider.notifyListeners(SWT.Selection, new Event());
				
				if (isStop){
					stopAnimation();
				} else {
					getDisplay().timerExec(5, this);
				}
			}
		};
		currentAnimator.run();
	}
	
	public void stopAnimation(){
		animationButton.setText("Play");
		startTime = -1;
		currentAnimator = null;
	}
	
	public void setMaxFrame(int maxFrame) {
		this.maxFrame = maxFrame;
		
		slider.setEnabled(true);
		animationButton.setEnabled(true);
		slider.setMaximum(maxFrame + slider.getThumb());
		slider.setSelection(1);
		slider.notifyListeners(SWT.Selection, new Event());
	}
	
	public int getMaxFrame() {
		return maxFrame;
	}
	
	public void setMaxFrameOnly(int maxFrame){
		this.maxFrame = maxFrame;
		slider.setMaximum(maxFrame + slider.getThumb());
		if (slider.getSelection() >= maxFrame){
			slider.setSelection(maxFrame);
			slider.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	public void setMinSelection(int minSelection) {
		this.minSelection = minSelection;
		slider.setSelection(minSelection);
		slider.notifyListeners(SWT.Selection, new Event());
	}

	public void setMaxSelection(int maxSelection) {
		this.maxSelection = maxSelection;
		slider.setSelection(maxSelection);
		slider.notifyListeners(SWT.Selection, new Event());
	}
	
	public int getMinSelection() {
		return minSelection;
	}

	public int getMaxSelection() {
		return maxSelection;
	}

	public void setFPS(int fps){
		frameTime = 1000d/fps;
	}

	public void setTimeIndex(int index){
		slider.setSelection(index + 1);
		slider.notifyListeners(SWT.Selection, new Event());
	}
	
	public void updateTimeIndex(){
		number.setText(String.valueOf(slider.getSelection() - (startFromZero ? 1 : 0)));
	}
	
	public int getTimeIndex(){
		return slider.getSelection() - 1;
	}

}
