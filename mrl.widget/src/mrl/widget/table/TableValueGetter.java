package mrl.widget.table;

public interface TableValueGetter<E> {
	
	public String[] getColumnHeaders();
	
	public String[] getTableValues(E item);

}
