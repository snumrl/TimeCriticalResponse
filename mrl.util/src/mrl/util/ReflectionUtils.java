package mrl.util;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ReflectionUtils {


	public static Field[] getValidFields(Field[] fields){
		ArrayList<Field> valid = new ArrayList<Field>();
		for (Field f : fields){
			if (f.getName().startsWith("_")) continue;
			valid.add(f);
		}
		return valid.toArray(new Field[valid.size()]);
	}
	
	public static String[] getFieldValues(Object object, Field[] fields){
		try {
			String[] values = new String[fields.length];
			for (int i = 0; i < fields.length; i++) {
				Object value = fields[i].get(object);
				values[i] = (value == null) ? "" : value.toString();
			}
			return values;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
