package mrl.util.heap;

import java.util.Random;

public class MinHeapTest {

	static class TestNode extends HeapNode{
		TestNode(double v){
			value = v;
		}
	}
	
	public static void main(String[] args) {
		int size = 1000;
		MinHeap<HeapNode> heap = new MinHeap<HeapNode>(new HeapNode[size+1]);
		for (int i = 0; i < size; i++) {
			heap.addNode(new TestNode(new Random().nextDouble()));
		}
		
		double lastV = 0;
		while (true){
			HeapNode m = heap.getMinimum();
			if (m.value < lastV) throw new RuntimeException();
			System.out.println(m.value);
			lastV = m.value;
			heap.removeNode(m);
			if (heap.getCurrentSize() == 0) break;
		}
	}
}
