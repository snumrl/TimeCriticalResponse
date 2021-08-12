package mrl.widget.dockable;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;

public class DualThumbSlider extends Composite{
	
	private static final int DEFAULT_THUMB_WIDTH = 12;
	
	private ArrayList<DualThumbListener> listeners = new ArrayList<DualThumbListener>();
	
	private Composite left;
	private Composite middle;
	private Composite right;
	private Sash sash1;
	private Sash sash2;
	private FormData sash1Data;
	private FormData sash2Data;
	
	private int barMargin = 2;
	
	private double minimum = 0;
	private double maximum = 100;
	private double leftSelection = minimum;
	private double rightSelection = maximum;
	
	private Color enableColor;
	private Color disableColor;
	private Color sashColor;
	
	public DualThumbSlider(Composite parent, int style) {
		super(parent, style);
		
		this.setLayout(new FormLayout());
		
		int sashStyle = SWT.VERTICAL | SWT.BORDER;
		
		left = new Composite(this, SWT.NONE);
		sash1 = new Sash(this, sashStyle);
		middle = new Composite(this, SWT.NONE);
		sash2 = new Sash(this, sashStyle);
		right = new Composite(this, SWT.NONE);
		
		FormData button1Data = new FormData ();
		button1Data.left = new FormAttachment (0, 0);
		button1Data.right = new FormAttachment (sash1, 0);
		button1Data.top = new FormAttachment (0, barMargin);
		button1Data.bottom = new FormAttachment (100, -barMargin);
		left.setLayoutData (button1Data);
		
		FormData button2Data = new FormData ();
		button2Data.left = new FormAttachment (sash1, 0);
		button2Data.right = new FormAttachment (sash2, 0);
		button2Data.top = new FormAttachment (0, barMargin);
		button2Data.bottom = new FormAttachment (100, -barMargin);
		middle.setLayoutData (button2Data);
		
		FormData button3Data = new FormData ();
		button3Data.left = new FormAttachment (sash2, 0);
		button3Data.right = new FormAttachment (100, 0);
		button3Data.top = new FormAttachment (0, barMargin);
		button3Data.bottom = new FormAttachment (100, -barMargin);
		right.setLayoutData (button3Data);
		
		enableColor = new Color(null, 104, 155, 249);
		disableColor = new Color(null, 246, 245, 240);
		sashColor = new Color(null, 186, 209, 252);
		
		left.setBackground(disableColor);
		middle.setBackground(enableColor);
		right.setBackground(disableColor);
		sash1.setBackground(sashColor);
		sash2.setBackground(sashColor);
		
		{
			sash1Data = new FormData ();
			sash1Data.top = new FormAttachment (0, 0);
			sash1Data.bottom = new FormAttachment (100, 0);
			sash1Data.width = DEFAULT_THUMB_WIDTH;
			sash1.setLayoutData (sash1Data);
			
			sash1.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					onLeftSashSelectionChange(e.x, -1);
				}
			});
		}
		{
			sash2Data = new FormData ();
			sash2Data.top = new FormAttachment (0, 0);
			sash2Data.bottom = new FormAttachment (100, 0);
			sash2Data.width = DEFAULT_THUMB_WIDTH;
			sash2.setLayoutData (sash2Data);
			
			sash2.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					Rectangle sash1Rect = sash1.getBounds ();
					Rectangle sash2Rect = sash2.getBounds ();
					Rectangle shellRect = DualThumbSlider.this.getClientArea ();
					int left = sash1Rect.x + sash1Rect.width;
					int right = shellRect.width - sash2Rect.width;
					e.x = Math.max (Math.min (e.x, right), left);
					if (e.x != sash2Rect.x)  {
						int uiBound = shellRect.width - sash1Rect.width - sash2Rect.width;
						double valueBound = maximum - minimum;
						rightSelection = minimum + ((e.x - sash1Rect.width) * valueBound / uiBound);
						rightSelection = Math.max(leftSelection, rightSelection);
						updateThumbStatus();
						notifyRightThumbChanged();
					}
				}
			});
		}
		{
			MiddleController controller = new MiddleController();
			middle.addMouseListener(controller);
			middle.addMouseMoveListener(controller);
		}
		
		this.addControlListener(new ControlListener(){
			public void controlMoved(ControlEvent e) {
			}
			public void controlResized(ControlEvent e) {
				updateThumbStatus();
			}
		});
		
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				enableColor.dispose();
				disableColor.dispose();
				sashColor.dispose();
			}
		});
		
		Listener dbClickListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				leftSelection = minimum;
				rightSelection = maximum;
				updateThumbStatus();
				notifyLeftThumbChanged();
				notifyRightThumbChanged();
			}
		};
		left.addListener(SWT.MouseDoubleClick, dbClickListener);
		middle.addListener(SWT.MouseDoubleClick, dbClickListener);
		right.addListener(SWT.MouseDoubleClick, dbClickListener);
		
		this.layout();
	}
	
	private void onLeftSashSelectionChange(int selectionX, double holdingInterval){
		boolean isHoldingMode = (holdingInterval > 0);
		
		Rectangle sash1Rect = sash1.getBounds ();
		Rectangle sash2Rect = sash2.getBounds ();
		Rectangle shellRect = DualThumbSlider.this.getClientArea ();
		int right = sash2Rect.x - sash1Rect.width;
		selectionX = Math.max (Math.min (selectionX, right), 0);
		if (selectionX != sash1Rect.x)  {
			int uiBound = shellRect.width - sash1Rect.width - sash2Rect.width;
			double valueBound = maximum - minimum;
			leftSelection = minimum + (selectionX * valueBound / uiBound);
			if (isHoldingMode){
				leftSelection = Math.min(leftSelection, maximum - holdingInterval);
				rightSelection = leftSelection + holdingInterval;
			}
			leftSelection = Math.min(leftSelection, rightSelection);
			updateThumbStatus();
			notifyLeftThumbChanged();
			if (isHoldingMode){
				notifyRightThumbChanged();
			}
		}
	}
	
	private void updateThumbStatus(){
		Rectangle sash1Rect = sash1.getBounds ();
		Rectangle sash2Rect = sash2.getBounds ();
		Rectangle shellRect = DualThumbSlider.this.getClientArea ();
		
		int uiBound = shellRect.width - sash1Rect.width - sash2Rect.width;
		double valueBound = maximum - minimum;
		int leftIndex = (int)Math.round((leftSelection - minimum) * uiBound / valueBound);
		int rightIndex = (int)Math.round((rightSelection - minimum) * uiBound / valueBound);
		if (minimum == maximum || Double.isNaN(minimum)){
			rightIndex = uiBound;
		}
		
		sash1Data.left = new FormAttachment(0, leftIndex);
		sash2Data.left = new FormAttachment(0, sash1Rect.width + rightIndex);
		layout();
	}
	
	public void setThumbWidth(int width){
		sash1Data.width = width;
		sash2Data.width = width;
		updateThumbStatus();
	}
	
	
	public void setBound(double minimum, double maximum){
		this.minimum = minimum;
		this.maximum = maximum;
		
		this.leftSelection = minimum;
		this.rightSelection = maximum;
		
		updateThumbStatus();
		
		boolean disable = (minimum == maximum || Double.isNaN(minimum));
		setCompositeEnabled(this, !disable);
	}
	
	public static void setCompositeEnabled(Composite composite, boolean arg) {
		if (composite != null)
		{
			for (Control control : composite.getChildren())
			{
				
				if (control instanceof Composite)
				{
					setCompositeEnabled((Composite)control, arg);
				}
				
				control.setEnabled(arg);
				control.redraw();
			}
		}
	}
	
	public double getMinimum(){
		return minimum;
	}
	public double getMaximum(){
		return maximum;
	}
	
	public void setLeftSelection(double selection){
		this.leftSelection = selection;
		updateThumbStatus();
	}
	public void setRightSelection(double selection){
		this.rightSelection = selection;
		updateThumbStatus();
	}
	public double getLeftSelection(){
		return leftSelection;
	}
	public double getRightSelection(){
		return rightSelection;
	}
	
	public void setLeftToolTipText(String text){
		sash1.setToolTipText(text);
	}
	public void setRightToolTipText(String text){
		sash2.setToolTipText(text);
	}
	
	///////////////////////////////////////////////////////////////////////////
	//              		 아래 부터는 Listener 관련 							///
	///////////////////////////////////////////////////////////////////////////
	
	public void addDualThumbListener(DualThumbListener listener){
		listeners.add(listener);
	}
	public void removeDualThumbListener(DualThumbListener listener){
		listeners.remove(listener);
	}
	public void notifyLeftThumbChanged(){
		for (DualThumbListener listener : listeners){
			listener.onLeftThumbChanged(this);
		}
	}
	public void notifyRightThumbChanged(){
		for (DualThumbListener listener : listeners){
			listener.onRightThumbChanged(this);
		}
	}
	
	
	private class MiddleController implements MouseListener, MouseMoveListener{

		private boolean isDragging;
		private int leftMargin;
		private double holdingInterval;
		
		public void mouseDoubleClick(MouseEvent e) {}

		public void mouseDown(MouseEvent e) {
			isDragging = true;
			
			leftMargin = e.x;
			holdingInterval = rightSelection - leftSelection;
		}

		public void mouseUp(MouseEvent e) {
			isDragging = false;
		}

		public void mouseMove(MouseEvent e) {
			if (!isDragging) return;
			
			int dx = e.x - leftMargin;
			
			onLeftSashSelectionChange(Math.max(sash1.getBounds().x + dx, 0), holdingInterval);
		}
		
	}
}
