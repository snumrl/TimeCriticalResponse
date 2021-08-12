package mrl.widget.app;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.table.SortableTable;
import mrl.widget.table.TableValueGetter;

public class ListViewerModule extends Module{
	
	public static boolean SHOW_TAB_ITEM_ON_CHANGE = true;
	private SortableTable<Object> table;
	private ListListener<Object> listener;

	@Override
	protected void initializeImpl() {
		table = new SortableTable<Object>(dummyParent());
		addWindow(table, WindowPosition.Right);
		
		table.getTable().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (listener == null) return;
				Object item = table.getSelectedItem();
				if (item == null) return;
				listener.onItemSelection(item);
			}
		});
	}
	
	public Object getSelectedItem() {
		return table.getSelectedItem();
	}
	
	public <E> void setItems(E[] itemList, ListListener<E> listener){
		ArrayList<Object> list = new ArrayList<Object>();
		for (E item : itemList) list.add(item);
		setItems(list, listener);
	}
	
	@SuppressWarnings("unchecked")
	public <E> void setItems(ArrayList<?> itemList, final ListListener<E> listener){
		ArrayList<Object> list = new ArrayList<Object>();
		list.addAll(itemList);
		table.setItemList(list, new TableValueGetter<Object>() {
			@Override
			public String[] getTableValues(Object item) {
				return listener.getTableValues((E)item);
			}
			@Override
			public String[] getColumnHeaders() {
				return listener.getColumnHeaders();
			}
		});
		this.listener = (ListListener<Object>)listener;
		if (SHOW_TAB_ITEM_ON_CHANGE) showTabItem();
	}
	
	public static interface ListListener<E> extends TableValueGetter<E>{
		
		public void onItemSelection(E item);
		
	}

}
