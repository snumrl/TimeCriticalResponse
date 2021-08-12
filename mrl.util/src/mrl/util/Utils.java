package mrl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.vecmath.Point3d;

public class Utils {
	
	public static String DOUBLE_FORMAT = "%.5f";
	
	/**
	 * return random value between '-1'  and  '1'
	 * @return
	 */
	public static double rand1(){
		return (MathUtil.random.nextDouble() - 0.5)*2;
	}
	
	public static String[] concatenate(String[] ...dataList){
		int lenSum = 0;
		for (String[] list : dataList){
			if (list == null) continue;
			lenSum += list.length;
		}
		String[] array = new String[lenSum];
		int idx = 0;
		for (String[] list : dataList){
			if (list == null) continue;
			System.arraycopy(list, 0, array, idx, list.length);
			idx += list.length;
		}
		return array;
	}
	
	public static void assertEqual(Object o1, Object o2) {
		if (o1 == o2) return;
		if ((o1 == null || o2 == null) || !o1.equals(o2)) {
			throw new RuntimeException("assertEqual :: " + o1 + " , " + o2); 
		}
	}
	
	public static int RGB(int r, int g, int b){
		return (r << 16) + (g << 8) + b;
	}
	
	public static <E> ArrayList<E> singleList(E element){
		ArrayList<E> list = new ArrayList<E>();
		list.add(element);
		return list;
	}
	
	public static <E> E end(List<E> list){
		return list.get(list.size()-1);
	}
	
	public static <E> int indexOf(E[] array, E element){
		for (int i = 0; i < array.length; i++) {
			if (array[i] != null && array[i].equals(element)) return i;
		}
		return -1;
	}
	
	public static ArrayList<int[]> getTrueSequences(boolean[] array){
		ArrayList<int[]> result = new ArrayList<int[]>();
		int start = -1;
		for (int i = 0; i < array.length; i++) {
			if (array[i] == false){
				if (start >= 0){
					result.add(new int[]{start, i-1});
					start = -1;
				}
			} else {
				if (start < 0){
					start = i;
				}
			}
		}
		if (start >= 0){
			result.add(new int[]{start, array.length-1});
		}
		return result;
	}
	
	public static <E> ArrayList<E> uniqueList(Collection<E> c){
		ArrayList<E> list = new ArrayList<E>();
		HashSet<E> set = new HashSet<E>();
		for (E e : c){
			if (!set.contains(e)){
				list.add(e);
				set.add(e);
			}
		}
		return list;
	}
	
	public static <E> ArrayList<E> getBiggest(ArrayList<ArrayList<E>> list){
		ArrayList<E> biggest = null;
		int maxLen = -1;
		for (ArrayList<E> l : list){
			if (l.size() > maxLen){
				biggest = l;
				maxLen = l.size();
			}
		}
		return biggest;
	}
	
	public static void copyFolder(File source, File target){
		target.mkdirs();
		for (File f : source.listFiles()){
			if (f.isDirectory()) {
				copyFolder(f, new File(target.getAbsolutePath() + "\\" + f.getName()));
			} else {
				copyFile(f, new File(target.getAbsolutePath() + "\\" + f.getName()));
			}
		}
	}
	
	public static void copyTextFile(File source, File dest){
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest)));
			String line;
			while ((line = br.readLine()) != null) {
				bw.write(line + "\r\n");
			}
			br.close();
			bw.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void copyFile(String source, String target) {
		copyFile(new File(source), new File(target));
	}
	public static void copyFile(File source, File target) {
		try {
			if (target.exists()) target.delete();
			Files.copy(source.toPath(), target.toPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Point3d rgbToReal(int red, int green, int blue){
		Point3d p = new Point3d(red, green, blue);
		p.scale(1/255d);
		return p;
	}
	
	public static void deleteFile(File file){
		if (file.exists() == false) return;
		if (file.isDirectory()){
			for (File f : file.listFiles()){
				deleteFile(f);
			}
		}
		file.delete();
	}
	
	public static Field[] getValidFields(Field[] fields){
		ArrayList<Field> valid = new ArrayList<Field>();
		for (Field f : fields){
			if (f.getName().startsWith("_")) continue;
			valid.add(f);
		}
		return valid.toArray(new Field[valid.size()]);
	}
	
	public static <E> ArrayList<E> list(E[] array){
		ArrayList<E> list = new ArrayList<E>();
		for (E element : array){
			list.add(element);
		}
		return list;
	}
	
	public static <E> ArrayList<E> reverse(ArrayList<E> list){
		ArrayList<E> reversed = new ArrayList<E>();
		for (int i = list.size()-1; i >= 0; i--) {
			reversed.add(list.get(i));
		}
		return reversed;
	}
	
	public static <E> ArrayList<E> copy(List<E> list){
		ArrayList<E> copy = new ArrayList<E>();
		for (E element : list){
			copy.add(element);
		}
		return copy;
	}
	
	public static <E> ArrayList<E> list(Collection<E> collection){
		ArrayList<E> list = new ArrayList<E>();
		for (E element : collection){
			list.add(element);
		}
		return list;
	}
	
	public static <E> E pickRandom(E[] list){
		return pickRandom(list, MathUtil.random);
	}
	public static <E> E pickRandom(E[] list, Random random){
		return pickRandom(list, list.length, random);
	}
	
	public static <E> E pickRandom(E[] list, int pickSize, Random random){
		if (list == null || list.length == 0) return null;
		return list[random.nextInt(Math.min(pickSize, list.length))];
	}
	
	public static <E> E pickRandom(ArrayList<E> list){
		return pickRandom(list, MathUtil.random);
	}
	
	public static <E> E pickRandom(ArrayList<E> list, Random random){
		return pickRandom(list, list.size(), random);
	}
	
	public static <E> E pickRandom(ArrayList<E> list, int pickSize, Random random){
		if (list == null || list.size() == 0) return null;
		return list.get(random.nextInt(Math.min(pickSize, list.size())));
	}
	
	public static <E> ArrayList<E> cut(ArrayList<E> list, int start, int end){
		if (end < 0) end = list.size()-1;
		ArrayList<E> result = new ArrayList<E>();
		for (int i = start; i <= end; i++) {
			result.add(list.get(i));
		}
		return result;
	}
	
	public static int[][] cut(int[][] list, int start, int end){
		int[][] result = new int[end - start + 1][];
		for (int i = start; i <= end; i++) {
			result[i-start] = list[i];
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <E> E[] cut(E[] array, int start, int end){
		int len = end - start + 1;
		E[] result = (E[])Array.newInstance(array.getClass().getComponentType(), len);
		System.arraycopy(array, start, result, 0, len);
		return result;
	}
	public static double[] cut(double[] array, int start, int end){
		int len = end - start + 1;
		double[] result = new double[len];
		System.arraycopy(array, start, result, 0, len);
		return result;
	}
	
	public static <E> ArrayList<E> toList(E[] array){
		return toList(array, 0, array.length-1);
	}
	public static <E> ArrayList<E> toList(E[] array, int start, int end){
		ArrayList<E> list = new ArrayList<E>();
		for (int i = start; i <= end; i++) {
			list.add(array[i]);
		}
		return list;
	}
	
	public static int[] trim(int[] list, int leftMargin, int rightMargin){
		int[] result = new int[list.length - (leftMargin + rightMargin)];
		for (int i = 0; i < result.length; i++) {
			result[i] = list[i + leftMargin];
		}
		return result;
	}
	
	public static <K, V> HashMap<V, K> inverseMap(HashMap<K, V> map){
		HashMap<V, K> inverse = new HashMap<V, K>();
		for (Entry<K, V> entry : map.entrySet()){
			inverse.put(entry.getValue(), entry.getKey());
		}
		return inverse;
	}
	
	@SuppressWarnings("unchecked")
	public static <E> HashSet<E> toSet(E... values){
		HashSet<E> set = new HashSet<E>();
		for (E s : values){
			set.add(s);
		}
		return set;
	}
	
	public static <E> HashSet<E> toSet(ArrayList<E> values){
		HashSet<E> set = new HashSet<E>();
		for (E s : values){
			set.add(s);
		}
		return set;
	}
	
	@SuppressWarnings("unchecked")
	public static <E> E[] toArray(Collection<E> list){
		return toArray(list, (Class<E>)list.iterator().next().getClass());
	}
	
	@SuppressWarnings("unchecked")
	public static <E> E[] toArray(Collection<E> list, Class<E> c){
		try {
			E[] array = (E[])Array.newInstance(c, list.size());
			array = list.toArray(array);
			return array;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static double[] toDoubleArray(List<Double> list){
		double[] array = new double[list.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	public static int[] toIntArray(List<Integer> list){
		int[] array = new int[list.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	public static int[] toArray(Integer[] list){
		int[] result = new int[list.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = list[i];
		}
		return result;
	}
	
	public static <E> boolean contains(E[] array, E element){
		return findIndex(array, element) >= 0;
	}
	
	public static <E> boolean isOverlap(Set<E> set1, Set<E> set2){
		for (E element : set1){
			if (set2.contains(element)) return true;
		}
		return false;
	}
	
	public static <E> int findIndex(E[] array, E element){
		for (int i = 0; i < array.length; i++) {
			if (element.equals(array[i])) return i;
		}
		return -1;
	}
	
	public static <E> boolean arrayEquals(E[] array1, E[] array2){
		if (array1.length != array2.length) return false;
		for (int i = 0; i < array2.length; i++) {
			if (!objEqauls(array1[i], array2[i])){
				return false;
			}
		}
		return true;
	}
	
	public static boolean objEqauls(Object obj1, Object obj2){
		if (obj1 == obj2) return true;
		if (obj1 == null || obj2 == null) return false;
		return obj1.equals(obj2);
	}
	
	public static String toString(Object... args){
		String str = "";
		for (Object obj : args){
			str += objectToString(obj) + "\t";
		}
		return str;
	}
	
	public static String objectToString(Object obj){
		String str;
		if (obj == null){
			str = "";
		} else if (obj instanceof Double || obj instanceof Float){
			str = String.format(DOUBLE_FORMAT, obj);
		} else if (obj.getClass().isArray()){
			int len = Array.getLength(obj);
			str = "[";
			for (int i = 0; i < len; i++) {
				if (i > 0) {
					str += ",";
				}
				str += objectToString(Array.get(obj, i));
			}
			str += "]";
		} else {
			str = obj.toString();
		}
		return str;
	}
	
	public static String[] toStringArrays(Object... args){
		String[] array = new String[args.length];
		for (int i = 0; i < array.length; i++) {
			array[i] = objectToString(args[i]);
		}
		return array;
	}
	
	public static void print(double[][] array){
		for (double[] list : array){
			for (double v : list){
				System.out.print(String.format(DOUBLE_FORMAT + "\t", v));
			}
			System.out.println();
		}
	}
	public static void print(double[] array){
		for (double v : array){
			System.out.print(String.format(DOUBLE_FORMAT + "\t", v));
		}
		System.out.println();
	}
	
	public static <E> int[][] divide(ArrayList<E> list, Comparator<E> comparator){
		ArrayList<int[]> result = new ArrayList<int[]>();
		
		int start = 0;
		for (int j = 1; j < list.size(); j++) {
			E m1 = list.get(j-1);
			E m2 = list.get(j);
			if (comparator.compare(m1, m2) != 0){
				result.add(new int[]{ start, j-1 });
				start = j;
			}
		}
		result.add(new int[]{ start, list.size()-1 });
		
		return result.toArray(new int[result.size()][]);
	}
	
	public static <E> E last(List<E> list){
		return list.get(list.size()-1);
	}
	
	public static <E> E last(E[] list){
		return list[list.length-1];
	}
	
	public static double last(double[] list){
		return list[list.length-1];
	}
	
	public static <E> E getSafe(List<E> list, int index){
		return list.get(Math.max(0, Math.min(list.size()-1, index)));
	}
	
	public static Double[] nativeToObject(double[] array){
		Double[] ret = new Double[array.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = array[i];
		}
		return ret;
	}
	public static double[] objectToNative(Double[] array){
		double[] ret = new double[array.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = array[i];
		}
		return ret;
	}
	
	public static <E> ArrayList<int[]> getNullIntervals(ArrayList<E> list, boolean includeEdge){
		ArrayList<int[]> intervals = new ArrayList<int[]>();
		int lastNotNull = -1;
		boolean isPrevNull = false;
		for (int i = 0; i < list.size(); i++) {
			boolean isNull = list.get(i) == null;
			if (isNull){
			} else {
				if (isPrevNull && i > 0){
					if (includeEdge || lastNotNull >= 0){
						intervals.add(new int[]{ lastNotNull + 1, i - 1 });
					}
				}
				lastNotNull = i;
			}
			isPrevNull = isNull;
		}
		if (includeEdge && isPrevNull){
			intervals.add(new int[]{ lastNotNull + 1, list.size() - 1 });
		}
		return intervals;
	}
	
	public static void runMultiThread(Runnable... runnables){
		Thread[] threadList = new Thread[runnables.length];
		for (int i = 0; i < threadList.length; i++) {
			threadList[i] = new Thread(runnables[i]);
			threadList[i].start();
		}
		
		for (int i = 0; i < threadList.length; i++) {
			if (threadList[i].isAlive()){
				try {
					threadList[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public static void runMultiThread(final IterativeRunnable runnable, final int size){
		Thread[] threadList = new Thread[Configuration.MAX_THREAD];
		
		
		final int interval = Math.max(1, size/Configuration.MAX_THREAD/4);
		final int intervalSize = (size / interval) + 1;
		
		final int[] currentIndex = new int[1];
		for (int i = 0; i < Configuration.MAX_THREAD; i++) {
			Thread thread = new Thread(){
				public void run(){
					try {
						while (true){
							int intervalIndex;
							synchronized (currentIndex) {
								if (currentIndex[0] >= intervalSize) break;
								intervalIndex = currentIndex[0];
								currentIndex[0]++;
							}
							
							int min = intervalIndex * interval;
							int max = Math.min(min + interval, size);
							for (int j = min; j < max; j++) {
								runnable.run(j);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("Error occured in Utils.runMultiThread ::");
						System.out.flush();
						System.err.flush();
						System.exit(0);
					}
				}
			};
			thread.start();
			threadList[i] = thread;
		}
		
		for (int i = 0; i < threadList.length; i++) {
			if (threadList[i].isAlive()){
				try {
					threadList[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
