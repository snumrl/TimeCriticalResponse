package mrl.widget.table;

import java.lang.reflect.Field;
import java.util.ArrayList;

import mrl.util.ReflectionUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

public class ObjectTableViewer<E> extends Composite{
	
	private SortableTable<E> table;
	private TableValueGetter<E> valueGetter;
	private Field[] validFiedls;

	public ObjectTableViewer(Composite parent, Class<E> type) {
		super(parent, SWT.NONE);
		
		this.setLayout(new FillLayout());
		
		table = new SortableTable<E>(this);
		validFiedls = ReflectionUtils.getValidFields(type.getFields());
		
		valueGetter = new TableValueGetter<E>() {
			@Override
			public String[] getColumnHeaders() {
				String[] fieldNames = new String[validFiedls.length];
				for (int i = 0; i < fieldNames.length; i++) {
					fieldNames[i] = validFiedls[i].getName();
				}
				return fieldNames;
			}

			@Override
			public String[] getTableValues(E item) {
				return ReflectionUtils.getFieldValues(item, validFiedls);
			}
		};
	}
	
	public void setItemList(ArrayList<E> itemList){
		table.setItemList(itemList, valueGetter);
	}

	public SortableTable<E> getTable() {
		return table;
	}
}
