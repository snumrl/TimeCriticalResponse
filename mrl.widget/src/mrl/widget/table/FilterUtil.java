package mrl.widget.table;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import mrl.util.Utils;

public class FilterUtil {

	public static <E> ArrayList<E> filterItems(ArrayList<E> itemList, String[][] filters, TableValueGetter<E> valueGetter){
		if (valueGetter == null) throw new RuntimeException();
		
		String[] headers = valueGetter.getColumnHeaders();
		ArrayList<E> filteredItems = new ArrayList<E>();
		int[] filterIndices = new int[filters.length];
		String[][] filterValues = new String[filters.length][];
		for (int i = 0; i < filterValues.length; i++) {
			filterIndices[i] = Utils.findIndex(headers, filters[i][0]);
			filterValues[i] = split(filters[i][1]);
			for (int j = 0; j < filterValues[i].length; j++) {
				filterValues[i][j] = filterValues[i][j].toLowerCase();
			}
		}
		
		for (int i = 0; i < itemList.size(); i++) {
			String[] values = valueGetter.getTableValues(itemList.get(i));
			if (isMatch(values, filterIndices, filterValues)){
				filteredItems.add(itemList.get(i));
			}
		}
		return filteredItems;
	}
	
	public static <E> ArrayList<E> filterItems(ArrayList<E> itemList, Object filter){
		try {
			ArrayList<E> result = new ArrayList<E>();
			if (itemList.size() == 0) return result;
			
			Class<?> type = itemList.get(0).getClass();
			Field[] fields = type.getFields();
			final HashMap<String, Field> fieldMap = new HashMap<String, Field>();
			for (Field f : fields){
				fieldMap.put(f.getName(), f);
			}
			
			
			Class<?> filterType = filter.getClass();
			final Field[] filterFields = Utils.getValidFields(filterType.getFields());
			String[][] filterValues = new String[filterFields.length][2];
			final String[] headers = new String[filterFields.length];
			for (int i = 0; i < filterValues.length; i++) {
				filterValues[i][0] = filterFields[i].getName();
				filterValues[i][1] = (String)filterFields[i].get(filter);
				headers[i] = filterFields[i].getName();
			}
			
			TableValueGetter<E> valueGetter = new TableValueGetter<E>() {
				@Override
				public String[] getColumnHeaders() {
					return headers;
				}
				@Override
				public String[] getTableValues(E item) {
					try {
						String[] values = new String[filterFields.length];
						for (int i = 0; i < values.length; i++) {
							Field field = fieldMap.get(filterFields[i].getName());
							values[i] = Utils.objectToString(field.get(item));
						}
						return values;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			return filterItems(itemList, filterValues, valueGetter);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public static boolean isMatch(String[] values, int[] filterIndices, String[][] filterValues){
		for (int i = 0; i < filterIndices.length; i++) {
			if (!isFieldMatch(values[filterIndices[i]], filterValues[i])) return false;
		}
		return true;
	}
	
	public static String[] split(String str){
		str = str.replace("|", "`");
		return str.split("`");
	}
	
	private static boolean isFieldMatch(String value, String[] filter){
		boolean isValueExist = false;
		boolean isMatch = false;
		value = value.toLowerCase();
		for (int i = 0; i < filter.length; i++) {
			String fValue = filter[i].toLowerCase();
			if (fValue.startsWith("!")){
				if (value.equals(fValue.substring(1))) return false;
			} else {
				isValueExist = true;
			}
			
			
			if (fValue.equals("*") || fValue.equals(value)){
				isMatch = true;
			}
		}
		return (!isValueExist) || isMatch;
	}
}
