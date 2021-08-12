package mrl.util;

import java.util.ArrayList;
import java.util.Arrays;

public class NearestBuffer<E> {

	int size;
	BufferPair lastNode;
	int count = 0;

	public NearestBuffer(int size) {
		this.size = size;
		lastNode = null;
		count = 0;
	}
	
	public void add(E element, double distance) {
		if (count < size) {
			addToValidIndex(new BufferPair(element, distance));
			count++;
		} else {
			if (distance >= minDistance()) return;
			
			removeLastNode();
			addToValidIndex(new BufferPair(element, distance));
		}
	}
	
	public E sample(double firstOffset, double baseOffset) {
		if (count == 0) return null;
		
		// pick first element with additional proportion
		ArrayList<Pair<E, Double>> list = getPairList();
		if (MathUtil.random.nextDouble() < firstOffset) {
			return list.get(0).first;
		}
		
		// pick element by their distance
		double pSum = 0;
		double[] accList = new double[count];
		for (int i = 0; i < count; i++) {
			double p = 1d/(list.get(i).second + baseOffset);
			p = p*p;
			pSum += p;
			accList[i] = pSum;
		}
		double p = MathUtil.random.nextDouble()*pSum;
		for (int i = 0; i < accList.length; i++) {
			if (p <= accList[i]){
				return list.get(i).first;
			}
		}
		throw new RuntimeException();
	}
	
	private double minDistance() {
		if (lastNode == null) {
			return Double.MAX_VALUE;
		} else {
			return lastNode.distance;
		}
	}
	
	private void removeLastNode() {
		lastNode = lastNode.prev;
//		lastNode.next = null;
	}
	
	private void addToValidIndex(BufferPair pair) {
		if (lastNode == null) {
			lastNode = pair;
			return;
		}
		BufferPair current = lastNode;
		BufferPair previousNode = null;
		while (true) {
			if (current == null || pair.distance > current.distance) {
				if (current != null) {
					pair.prev = current;
//					current.next = pair;
				}
				if (previousNode != null) {
//					pair.next = lastNode;
					previousNode.prev = pair;
				} else {
					lastNode = pair;
				}
				return;
			}
			previousNode = current;
			current = current.prev;
		}
	}
	
	public ArrayList<E> getElements(){
		ArrayList<E> list = new ArrayList<E>();
		for (Pair<E, Double> pair : getPairList()) {
			list.add(pair.first);
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Pair<E, Double>> getPairList(){
		ArrayList<Pair<E, Double>> list = new ArrayList<Pair<E, Double>>();
		if (count == 0) return list;
		BufferPair[] tempBuffer = new BufferPair[count];
		BufferPair current = lastNode;
		for (int i = 0; i < tempBuffer.length; i++) {
			tempBuffer[count - i - 1] = current;
			current = current.prev;
		}
		for (BufferPair pair : tempBuffer) {
			list.add(new Pair<E, Double>((E)pair.element, pair.distance));
		}
		return list;
	}
	
	
	public static class BufferPair implements Comparable<BufferPair>{
		Object element;
		double distance;
		
//		BufferPair next;
		BufferPair prev;
		
		public BufferPair(Object element, double distance) {
			this.element = element;
			this.distance = distance;
		}
		
		@Override
		public int compareTo(BufferPair o) {
			return Double.compare(distance, o.distance);
		}
	}
	
	public static void main(String[] args) {
		Integer[] aa = new Integer[3];
		aa[0] = 5;
		aa[1] = 3;
		aa[2] = null;
		Arrays.sort(aa);
		System.out.println(Arrays.toString(aa));
	}
}
