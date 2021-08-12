package mrl.widget.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import mrl.util.Utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class SortableTable<E> extends Composite{
	
	private Table table;
	private CacheData[] cacheDataList;
	private boolean showIndex = true;
	private boolean[] isNumberColumn;
	private String[] headers;
	
	private boolean enableCheck;
	
	private int[] columnWidth;
	
	public SortableTable(Composite parent) {
		this(parent, false);
	}
	public SortableTable(Composite parent, boolean enableCheck) {
		super(parent, SWT.NONE);
		
		this.enableCheck = enableCheck;
		
		this.setLayout(new FillLayout());

		int style = SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL;
		if (enableCheck) style |= SWT.CHECK;
		table = new Table(this, style);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		Menu menu = new Menu(table);
		table.setMenu(menu);
		if (enableCheck){
			MenuItem item1 = new MenuItem (menu, SWT.PUSH);
			item1.setText("Select All");
			item1.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					setCheckedAll(true);
					notifySelection();
				}
			});
			MenuItem item2 = new MenuItem (menu, SWT.PUSH);
			item2.setText("Deselect All");
			item2.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					setCheckedAll(false);
					notifySelection();
				}
			});
			MenuItem item3 = new MenuItem (menu, SWT.PUSH);
			item3.setText("Select this only");
			item3.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event e) {
					setCheckedAll(false);
					TableItem[] items = table.getSelection();
					for (TableItem item : items){
						item.setChecked(true);
					}
					notifySelection();
				}
			});
		}
		MenuItem item4 = new MenuItem (menu, SWT.PUSH);
		item4.setText("Copy contents to clipboard");
		item4.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				copyContentsToClipboard();
			}
		});
		MenuItem item5 = new MenuItem (menu, SWT.PUSH);
		item5.setText("Copy selected contents to clipboard");
		item5.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				copySelectedContentsToClipboard();
			}
		});
	}
	
	public void copyContentsToClipboard(){
		final Clipboard cb = new Clipboard(getDisplay());
		TextTransfer textTransfer = TextTransfer.getInstance();
		
		StringBuilder sb = new StringBuilder();
		sb.append(Utils.toString((Object[])headers) + "\r\n");
		for (CacheData cache : cacheDataList){
			sb.append(Utils.toString((Object[])cache.values) + "\r\n");
		}
		cb.setContents(new Object[]{sb.toString()}, new Transfer[]{textTransfer});
		cb.dispose();
	}
	
	public void copySelectedContentsToClipboard(){
		final Clipboard cb = new Clipboard(getDisplay());
		TextTransfer textTransfer = TextTransfer.getInstance();
		
		StringBuilder sb = new StringBuilder();
		for (int selected : table.getSelectionIndices()) {
			CacheData cache = cacheDataList[selected];
			sb.append(Utils.toString((Object[])cache.values) + "\r\n");
		}
		cb.setContents(new Object[]{sb.toString()}, new Transfer[]{textTransfer});
		cb.dispose();
	}
	
	private void notifySelection(){
		Event event = new Event();
		event.widget = table;
		table.notifyListeners(SWT.Selection, event);
	}
	
	@SuppressWarnings("unchecked")
	public E getSelectedItem(){
		TableItem[] selection = table.getSelection();
		if (selection.length == 1){
			return (E)selection[0].getData();
		} else {
			return null;
		}
	}
	
	public void select(E item){
		for (int i = 0; i < cacheDataList.length; i++) {
			if (cacheDataList[i].data == item){
				table.setSelection(i);
				break;
			}
		}
		notifySelection();
	}
	
	public void setColumnWidth(int[] columnWidth) {
		this.columnWidth = columnWidth;
	}
	
	public Table getTable() {
		return table;
	}
	
	public int getItemSize(){
		if (cacheDataList == null) return -1;
		return cacheDataList.length;
	}
	
	public void setShowIndex(boolean showIndex) {
		this.showIndex = showIndex;
	}
	
	@SuppressWarnings("unchecked")
	public E getItem(int index){
		return (E)(cacheDataList[index].data);
	}

	public void setColumnHeaders(String[] headers){
		this.headers = headers;
		
		table.removeAll();
		for (TableColumn column : table.getColumns()){
			column.dispose();
		}
		
		if (showIndex){
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText("Index");
		}
		for (int i = 0; i < headers.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(headers[i]);
		}
		
		Listener sortListener = new Listener() {
			public void handleEvent(Event e) {
				TableColumn currentColumn = (TableColumn) e.widget;
				sort(currentColumn);
			}
		};
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumn(i).pack();
			table.getColumn(i).addListener(SWT.Selection, sortListener);
		}
		
		table.setSortColumn(table.getColumn(0));
		table.setSortDirection(SWT.UP);
	}
	
	private void sort(TableColumn currentColumn){
		TableItem[] itemList = table.getItems();
		for (int i = 0; i < cacheDataList.length; i++) {
			cacheDataList[i].checked = itemList[i].getChecked();
		}
		
		// determine new sort column and direction
		TableColumn sortColumn = table.getSortColumn();
		int dir = table.getSortDirection();
		if (sortColumn == currentColumn) {
			dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
		} else {
			table.setSortColumn(currentColumn);
			dir = SWT.UP;
		}
		// sort the data based on column and direction
		final int index = table.indexOf(currentColumn);
		final int sign = (dir == SWT.UP) ? 1 : -1;
		Arrays.sort(cacheDataList, new Comparator<CacheData>() {
			@Override
			public int compare(CacheData o1, CacheData o2) {
				boolean isInvalid1 = (o1.values.length <= index) || o1.values[index] == null;
				boolean isInvalid2 = (o2.values.length <= index) || o2.values[index] == null;
				if (isInvalid1 && isInvalid2) return 0;
				if (isInvalid1) return sign;
				if (isInvalid2) return -sign;
				
				if (isNumberColumn[index]){
					return sign * Double.compare(Double.parseDouble(o1.values[index]), Double.parseDouble(o2.values[index]));
				} else {
					return sign * o1.values[index].compareTo(o2.values[index]);
				}
			}
		});
		
		// update data displayed in table
		table.setSortDirection(dir);
//		table.clearAll();
		
		table.setVisible(false);
		table.removeAll();
		for (int i = 0; i < cacheDataList.length; i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(cacheDataList[i].values);
			item.setData(cacheDataList[i].data);
			item.setChecked(cacheDataList[i].checked);
		}
		table.setVisible(true);
	}
	public void setFocus(int index){
		table.getItem(index).setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
	}
	public void setHighlight(boolean[] highlighted){
		for (int i = 0; i < highlighted.length; i++) {
			Color color = getDisplay().getSystemColor(highlighted[i] ? SWT.COLOR_BLACK : SWT.COLOR_GRAY);
			table.getItem(i).setForeground(color);
		}
	}
	public void setChecked(boolean[] checked){
		for (int i = 0; i < checked.length; i++) {
			table.getItem(i).setChecked(checked[i]);
		}
	}
	public boolean[] getChecked(){
		TableItem[] items = table.getItems();
		boolean[] checked = new boolean[items.length];
		for (int i = 0; i < checked.length; i++) {
			checked[i] = items[i].getChecked();
		}
		return checked;
	}
	
	public void setItemList(ArrayList<E> itemList){
		setItemList(itemList, null);
	}
	public void setItemList(ArrayList<E> itemList, TableValueGetter<E> valueGetter){
		table.setVisible(false);
		
		if (valueGetter != null){
			setColumnHeaders(valueGetter.getColumnHeaders());
		}
		
		table.removeAll();
		
		cacheDataList = new CacheData[itemList.size()];
		isNumberColumn = new boolean[table.getColumnCount()];
		for (int i = 0; i < isNumberColumn.length; i++) {
			isNumberColumn[i] = true;
		}
		
		for (int i = 0; i < itemList.size(); i++) {
			String[] values;
			if (valueGetter == null){
				values = ((SortableTableItem)itemList.get(i)).getTableValues();
			} else {
				values = valueGetter.getTableValues(itemList.get(i));
			}
			cacheDataList[i] = new CacheData(i, itemList.get(i), values, showIndex);
			
			int colLen = Math.min(isNumberColumn.length, cacheDataList[i].values.length);
			for (int j = 0; j < colLen; j++) {
				if (isNumberColumn[j]){
					try {
						Double.parseDouble(cacheDataList[i].values[j]);
					} catch (NumberFormatException e) {
						isNumberColumn[j] = false;
					}
				}
			}
			
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(cacheDataList[i].values);
			item.setData(cacheDataList[i].data);
			
			if (enableCheck){
				item.setChecked(true);
			}
		}
		
		for (int i = 0; i < table.getColumnCount(); i++) {
			if (columnWidth != null) {
				table.getColumn(i).setWidth(columnWidth[i]);
			} else {
				table.getColumn(i).pack();
			}
		}
		
		table.setVisible(true);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<E> getCheckedItems(){
		if (!enableCheck) throw new RuntimeException();
		ArrayList<E> list = new ArrayList<E>();
		TableItem[] items = table.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getChecked()){
				list.add((E)cacheDataList[i].data);
			}
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<E> getItemList(){
		ArrayList<E> list = new ArrayList<E>();
		TableItem[] items = table.getItems();
		for (int i = 0; i < items.length; i++) {
			list.add((E)cacheDataList[i].data);
		}
		return list;
	}
	
	public void setCheckedAll(boolean checked){
		if (!enableCheck) throw new RuntimeException();
		TableItem[] items = table.getItems();
		for (TableItem item : items){
			item.setChecked(checked);
		}
	}
	
	public void setSelection(int index){
		table.setSelection(index);
		table.notifyListeners(SWT.Selection, new Event());
	}
	
	private static class CacheData{
		@SuppressWarnings("unused")
		int index;
		Object data;
		String[] values;
		boolean checked;
		
		public CacheData(int index, Object data, String[] values, boolean showIndex) {
			this.index = index;
			this.data = data;
			
			if (showIndex){
				this.values = new String[values.length + 1];
				this.values[0] = Integer.toString(index);
				System.arraycopy(values, 0, this.values, 1, values.length);
			} else {
				this.values = values;
			}
		}
		
		
	}

}
