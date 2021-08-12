package mrl.motion.viewer;

import java.util.ArrayList;

import mrl.motion.data.MotionData;
import mrl.widget.table.FilterableTable;
import mrl.widget.table.SortableTable;
import mrl.widget.table.TableValueGetter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public abstract class FilterableMotionViewer<E> extends Composite {
	
	private SashForm sashForm;
	private MultiCharacterNavigator navigator;
	private FilterableTable<E> filterableTable;
	private SortableTable<E> sortableTable;

	public FilterableMotionViewer(Composite parent, boolean sortableOnly) {
		this(parent, sortableOnly, false);
	}
	public FilterableMotionViewer(Composite parent, boolean sortableOnly, boolean enableCheck) {
		super(parent, SWT.NONE);
		
		this.setLayout(new FillLayout());
		SashForm sashForm = new SashForm(this, SWT.HORIZONTAL);
		
		navigator = new MultiCharacterNavigator(sashForm);
		
		if (sortableOnly){
			sortableTable = new SortableTable<E>(sashForm, enableCheck);
		} else {
			filterableTable = new FilterableTable<E>(sashForm, enableCheck);
			sortableTable = filterableTable.getSortableTable();
		}
		
		sashForm.setWeights(new int[]{ 70, 30 });
		
		
		sortableTable.getTable().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (sortableTable.getTable().getSelection().length != 1) return;
				@SuppressWarnings("unchecked")
				E selected = (E)sortableTable.getTable().getSelection()[0].getData();
				navigator.stopAnimation();
				navigator.setMotionDataList(getMotionDataList(selected));
				navigator.setMotionIndex(0);
				navigator.startAnimation();
			}
		});
	}
	
	public E getSelectedItem(){
		return sortableTable.getSelectedItem();
	}
	
	public SashForm getSashForm() {
		return sashForm;
	}

	public MultiCharacterNavigator getNavigator() {
		return navigator;
	}

	public FilterableTable<E> getTable() {
		return filterableTable;
	}
	
	public void setSelection(int index){
		sortableTable.setSelection(index);
	}
	
	public void setItemList(ArrayList<E> itemList, TableValueGetter<E> valueGetter){
		sortableTable.setItemList(itemList, valueGetter);
	}

	protected abstract MotionData[] getMotionDataList(E element);
}
