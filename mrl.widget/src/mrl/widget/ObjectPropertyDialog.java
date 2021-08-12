package mrl.widget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class ObjectPropertyDialog {

	public static void open(Shell parent, Object object){
		final Shell shell = new Shell(parent, SWT.APPLICATION_MODAL | SWT.CLOSE | SWT.RESIZE);
		shell.setLayout(new GridLayout(2, false));
		
		ObjectPropertyPanel panel = new ObjectPropertyPanel(shell, object.getClass());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		panel.setObject(object);
		
		new Label(shell, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button okButton = new Button(shell, SWT.PUSH);
		okButton.setText("OK");
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData.widthHint = 100;
		okButton.setLayoutData(gridData);
		okButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				shell.close();
			}
		});
		
		Point size = shell.computeSize(300, -1);
		shell.setSize(size.x, size.y);
		
		Point location = parent.getLocation();
		Rectangle bounds = parent.getBounds();
		shell.setLocation(location.x + (bounds.width-size.x)/2, location.y + (bounds.height-size.y)/2);
		
		shell.open();
		
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
}
