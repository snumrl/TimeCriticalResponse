package mrl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class ObjectSerializer {

	public static <E> ArrayList<E> load(Class<E> type, File file){
		try {
			Field[] fields = type.getFields();
			HashMap<String, Field> fieldMap = new HashMap<String, Field>();
			for (Field f : fields){
				fieldMap.put(f.getName(), f);
			}
			
			ArrayList<E> list = new ArrayList<E>();
			if (file.exists() == false) return list;
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.equals("{")){
					E element = type.newInstance();
					while ((line = br.readLine()) != null) {
						line = line.trim();
						if (line.startsWith("}")){
							break;
						}
						
						int idx = line.indexOf(":");
						String name = line.substring(0, idx);
						String valueStr = line.substring(idx + 1);
						Field field = fieldMap.get(name);
						if (field == null) continue;
						
						Class<?> fieldType = field.getType();
						if (fieldType.equals(int.class)){
							try {
								field.setInt(element, Integer.parseInt(valueStr));
							} catch (NumberFormatException e) {
							}
						} else if (fieldType.equals(double.class)){
							try {
								field.setDouble(element, Double.parseDouble(valueStr));
							} catch (NumberFormatException e) {
							}
						} else if (fieldType.equals(Boolean.class)){
							Boolean value = null;
							if (valueStr.equals("true")){
								value = true;
							} else if (valueStr.equals("false")){
								value = false;
							}
							field.set(element, value);
						} else if (fieldType.equals(boolean.class)){
							boolean value = valueStr.equals("true");
							field.set(element, value);
						} else if (fieldType.equals(String.class)){
							field.set(element, valueStr);
						} else {
							br.close();
							throw new RuntimeException("Unkown Field Type : " + type + " : " + fieldType.getCanonicalName());
						}
					}
					list.add(element);
				}
			}
			br.close();
			return list;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	
	public static <E> void save(Class<E> type, ArrayList<E> list, File file){
		try {
			Field[] fields = type.getFields();
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			for (E element : list){
				bw.write("{\r\n");
				for (int i = 0; i < fields.length; i++) {
					Object value = fields[i].get(element);
					String valueStr = value == null ? "" : value.toString();
					bw.write(String.format("\t%s:%s\r\n", fields[i].getName(), valueStr));
				}
				bw.write("},\r\n");
			}
			bw.close();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <E> E loadObject(Class<E> type, File file){
		ArrayList<E> list = load(type, file);
		if (list.size() > 0){
			return list.get(0);
		} else {
			return null;
		}
	}
	
	public static <E> void saveObject(Class<E> type, E object, File file){
		ArrayList<E> list = new ArrayList<E>(1);
		list.add(object);
		save(type, list, file);
	}
	
	public static void save(ObjectOutputStream os, Object element){
		try {
			Class<? extends Object> type = element.getClass();
			Field[] fields = type.getFields();
			os.writeObject(type);
			os.writeInt(fields.length);
			for (int i = 0; i < fields.length; i++) {
				Object value = fields[i].get(element);
				os.writeObject(fields[i].getName());
				os.writeObject(value);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public static Object load(ObjectInputStream oi){
		try {
			Class<?> type = (Class<?>)oi.readObject();
			HashMap<String, Field> fieldMap = new HashMap<String, Field>();
			for (Field field : type.getFields()){
				fieldMap.put(field.getName(), field);
			}
			
			Object element = type.newInstance();
			int len = oi.readInt();
			for (int i = 0; i < len; i++) {
				String name = (String)oi.readObject();
				Object value = oi.readObject();
				
				Field field = fieldMap.get(name);
				if (field != null){
					field.set(element, value);
				}
			}
			return element;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
