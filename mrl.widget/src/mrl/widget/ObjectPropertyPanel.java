package mrl.widget;

import java.lang.reflect.Field;
import java.util.HashSet;

import mrl.util.Utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ObjectPropertyPanel extends Composite implements ModifyListener{
	
	private Field[] fields;
	private Object object;
	
	private Control[] inputTexts;
	private HashSet<String> disableFields = new HashSet<String>();
	private boolean enableNotify = true;

	public ObjectPropertyPanel(Composite parent, Class<?> type) {
		super(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = 1;
		this.setLayout(layout);
		
		fields = Utils.getValidFields(type.getFields());
		
		inputTexts = new Control[fields.length];
		for (int i = 0; i < fields.length; i++) {
			Label label = new Label(this, SWT.NONE);
			label.setText(fields[i].getName());
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			if (fields[i].getType().equals(String.class)){
				Combo combo = new Combo(this, SWT.BORDER);
				combo.addModifyListener(this);
				inputTexts[i] = combo;
			} else if (fields[i].getType().equals(Boolean.class)){
				Combo combo = new Combo(this, SWT.BORDER | SWT.READ_ONLY);
				combo.addModifyListener(this);
				combo.setItems(new String[]{ "", "true", "false" });
				inputTexts[i] = combo;
			} else if (fields[i].getType().equals(boolean.class)){
				Combo combo = new Combo(this, SWT.BORDER | SWT.READ_ONLY);
				combo.addModifyListener(this);
				combo.setItems(new String[]{ "true", "false" });
				combo.select(0);
				inputTexts[i] = combo;
			} else {
				Text text = new Text(this, SWT.BORDER);
				text.addModifyListener(this);
				inputTexts[i] = text;
			}
			inputTexts[i].setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			inputTexts[i].setEnabled(false);
		}
	}
	
	private int getFieldIndex(String fieldName){
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].getName().equals(fieldName)){
				return i;
			}
		}
		throw new RuntimeException();
	}
	
	public void setSampleValues(String fieldName, String... items){
		((Combo)inputTexts[getFieldIndex(fieldName)]).setItems(items);
	}
	
	public void addDisableFields(String... fieldNames){
		for (String f : fieldNames){
			disableFields.add(f);
		}
		
		for (int i = 0; i < fields.length; i++) {
			if (disableFields.contains(fields[i].getName())){
				inputTexts[i].setEnabled(false);
			}
		}
	}
	
	public void clearDisableFields(){
		disableFields.clear();
	}
	
	public void setFieldFocus(String fieldName){
		(inputTexts[getFieldIndex(fieldName)]).setFocus();
	}
	
	public void setFieldValue(String fieldName, String text){
		Control control = inputTexts[getFieldIndex(fieldName)];
		setText(control, text);
		Event event = new Event();
		event.widget = control;
		control.notifyListeners(SWT.Modify, event);
	}
	
	private void setText(Control control, String text){
		if (control instanceof Text){
			((Text)control).setText(text);
		} else if (control instanceof Combo){
			((Combo)control).setText(text);
		} else {
			throw new RuntimeException();
		}
	}
	
	private String getText(Control control){
		if (control instanceof Text){
			return ((Text)control).getText();
		} else if (control instanceof Combo){
			return ((Combo)control).getText();
		} else {
			throw new RuntimeException();
		}
	}
	
	public Object getObject(){
		return object;
	}
	
	public void setObject(Object object){
		this.object = object;
		
		enableNotify = false;
		if (object == null){
			for (Control control : inputTexts){
				setText(control, "");
				control.setEnabled(false);
			}
		} else {
			try {
				for (int i = 0; i < fields.length; i++) {
					Object value = fields[i].get(object);
					String str = (value == null) ? "" : value.toString();
					setText(inputTexts[i], str);
					if (disableFields.contains(fields[i].getName())){
						inputTexts[i].setEnabled(false);
					} else {
						inputTexts[i].setEnabled(true);
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		enableNotify = true;
		this.notifyListeners(SWT.Modify, new Event());
	}

	@Override
	public void modifyText(ModifyEvent event) {
		if (object == null) return;
		int i = 0;
		for (i = 0; i < inputTexts.length; i++) {
			if (inputTexts[i] == event.widget) break;
		}
		
		String str = getText(inputTexts[i]);
		Field field = fields[i];
		Class<?> type = field.getType();
		try {
			if (type.equals(int.class)){
				try {
					field.setInt(object, Integer.parseInt(str));
				} catch (NumberFormatException e) {
				}
			} else if (type.equals(double.class)){
				try {
					field.setDouble(object, Double.parseDouble(str));
				} catch (NumberFormatException e) {
				}
			} else if (type.equals(Boolean.class)){
				Boolean value = null;
				if (str.equals("true")){
					value = true;
				} else if (str.equals("false")){
					value = false;
				}
				field.set(object, value);
			} else if (type.equals(boolean.class)){
				boolean value = str.equals("true");
				field.set(object, value);
			} else if (type.equals(String.class)){
				field.set(object, str);
			} else {
				throw new RuntimeException("Unkown Field Type : " + type);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if (enableNotify){
			this.notifyListeners(SWT.Modify, new Event());
		}
	}
	
}
