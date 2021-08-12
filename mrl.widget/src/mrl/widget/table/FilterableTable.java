package mrl.widget.table;

import static mrl.widget.table.FilterUtil.isMatch;
import static mrl.widget.table.FilterUtil.split;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import mrl.util.Utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;

public class FilterableTable<E> extends Composite{
	
	private Composite filterComp;
	private SortableTable<E> table;
	
	private ArrayList<E> totalItems;
	private String[][] totalValues;
	
	private String[] columnHeaders;
	private String[] filterableFields;
	private Combo[] filterCombos;
	
	private boolean enableCheck;
	
	private TableValueGetter<E> valueGetter;
	
	
	public FilterableTable(Composite parent, boolean enableCheck) {
		super(parent, SWT.NONE);
		
		this.enableCheck = enableCheck;
		
		GridLayout mainLayout = new GridLayout(2, true);
		mainLayout.marginWidth = mainLayout.marginHeight = 0;
		this.setLayout(mainLayout);
		
		
		filterComp = new Composite(this, SWT.NONE);
		filterComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2 ,1));
		GridLayout filterLayout = new GridLayout(4, true);
		filterLayout.marginWidth = filterLayout.marginHeight = 0;
		filterComp.setLayout(filterLayout);
		
		Button update = new Button(this, SWT.PUSH);
		update.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		update.setText("Update");
		update.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				onUpdateButton();
			}
		});
		
		
		if (enableCheck){
			Button selectAll = new Button(this, SWT.PUSH);
			selectAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			selectAll.setText("Select All");
			selectAll.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					table.setCheckedAll(true);
				}
			});
			Button deselectAll = new Button(this, SWT.PUSH);
			deselectAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			deselectAll.setText("Deselect All");
			deselectAll.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					table.setCheckedAll(false);
				}
			});
		}
		
		table = new SortableTable<E>(this, enableCheck);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
	}
	
	public E getSelectedItem(){
		return table.getSelectedItem();
	}
	
	protected void onUpdateButton(){
		updateFilter();
	}
	
	public void setColumnHeaders(String[] headers){
		this.columnHeaders = headers;
		table.setColumnHeaders(headers);
	}
	
	public void setItemList(ArrayList<E> itemList){
		setItemList(itemList, null);
	}
	
	public void setItemList(ArrayList<E> itemList, TableValueGetter<E> valueGetter){
		this.valueGetter = valueGetter;
		this.totalItems = itemList;
		
		totalValues = new String[itemList.size()][];
		for (int i = 0; i < itemList.size(); i++) {
			if (valueGetter == null){
				totalValues[i] = ((SortableTableItem)itemList.get(i)).getTableValues();
			} else {
				totalValues[i] = valueGetter.getTableValues(itemList.get(i));
			}
		}
		if (valueGetter != null){
			columnHeaders = valueGetter.getColumnHeaders();
		}
				
		updateSampleValueList();
		updateFilter();
	}
	
	public ArrayList<E> getFilteredItems(){
		return table.getItemList();
	}
	
	public ArrayList<E> getCheckedItems(){
		return table.getCheckedItems();
	}
	
	public SortableTable<E> getSortableTable() {
		return table;
	}
	
	public Table getTable(){
		return table.getTable();
	}

	public void setFilterableFields(String... filterableFields) {
		this.filterableFields = filterableFields;
		
		filterCombos = new Combo[filterableFields.length];
		for (int i = 0; i < filterableFields.length; i++) {
			new Label(filterComp, SWT.NONE).setText(filterableFields[i]);
			filterCombos[i] = new Combo(filterComp, SWT.BORDER);
			filterCombos[i].setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			filterCombos[i].setText("*");
		}
		
		updateSampleValueList();
	}
	
	public void setFilterValue(String field, String value){
		filterCombos[Utils.findIndex(filterableFields, field)].setText(value);
		
		updateFilter();
	}
	
	protected void updateFilter(){
		if (totalItems == null) return;
		
		ArrayList<E> filteredItems = new ArrayList<E>();
		int[] filterIndices = new int[filterableFields.length];
		String[][] filterValues = new String[filterableFields.length][];
		for (int i = 0; i < filterValues.length; i++) {
			filterIndices[i] = Utils.findIndex(columnHeaders, filterableFields[i]);
			filterValues[i] = split(filterCombos[i].getText());
		}
		
		for (int i = 0; i < totalValues.length; i++) {
			if (isMatch(totalValues[i], filterIndices, filterValues)){
				filteredItems.add(totalItems.get(i));
			}
		}
		
		table.setItemList(filteredItems, valueGetter);
		if (enableCheck) table.setCheckedAll(true);
	}
	
	public ArrayList<E> filterByCurrentFilter(ArrayList<E> itemList, TableValueGetter<E> valueGetter){
		ArrayList<E> filteredItems = new ArrayList<E>();
		int[] filterIndices = new int[filterableFields.length];
		String[][] filterValues = new String[filterableFields.length][];
		for (int i = 0; i < filterValues.length; i++) {
			filterIndices[i] = Utils.findIndex(columnHeaders, filterableFields[i]);
			filterValues[i] = split(filterCombos[i].getText());
		}
		
		for (int i = 0; i < itemList.size(); i++) {
			String[] values;
			if (valueGetter == null){
				values = ((SortableTableItem)itemList.get(i)).getTableValues();
			} else {
				values = valueGetter.getTableValues(itemList.get(i));
			}
			if (isMatch(values, filterIndices, filterValues)){
				filteredItems.add(itemList.get(i));
			}
		}
		return filteredItems;
	}
	
	private void updateSampleValueList(){
		if (filterableFields == null || filterCombos == null) return;
		if (totalItems == null) return;
		
		for (int i = 0; i < filterableFields.length; i++) {
			int headerIndex = Utils.findIndex(columnHeaders, filterableFields[i]);
			if (headerIndex < 0) throw new RuntimeException();
			
			HashSet<String> valueSet = new HashSet<String>();
			for (String[] values : totalValues){
				valueSet.add(values[headerIndex]);
			}
			String[] _values = valueSet.toArray(new String[valueSet.size()]);
			Arrays.sort(_values);
			String[] values = new String[_values.length + 1];
			values[0] = "*";
			System.arraycopy(_values, 0, values, 1, _values.length);
			
			String originText = filterCombos[i].getText(); 
			filterCombos[i].setItems(values);
			filterCombos[i].setText(originText);
		}
	}
}
