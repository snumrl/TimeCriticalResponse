package mrl.graph;

public class MinHeap{

	private HeapNode[] nodeList;
	private int[] indexMap;
	/**
	 * 현재 heap에 들어있는 node들의 갯수 + 1의 값.
	 * 새 node가 들어오면 이 index에 노드를 넣고, 이 index를 1 증가시킨다.
	 */
	private int maxIndex = 1;
	
	public MinHeap(int size) {
		nodeList = new HeapNode[size+1];
		indexMap = new int[size];
		for (int i = 0; i < indexMap.length; i++) {
			indexMap[i] = -1;
		}
	}
	
	public boolean isEmpty(){
		return maxIndex <= 1;
	}
	
	public boolean contains(int index){
		return indexMap[index] >= 0;
	}
	
	public int removeMinimum(){
		HeapNode node = nodeList[1];
		// 맨 마지막 위치의 node와 위치를 바꾸고 
		// maxIndex를 1 줄여서 해당 노드를 뺀다.				 
		HeapNode movedNode = nodeList[maxIndex - 1];
		nodeList[1] = movedNode;
		indexMap[movedNode.index] = 1;
		nodeList[maxIndex - 1] = null;
		maxIndex--;
		
		indexMap[node.index] = -1;
		
		// 새로 위치가 바뀐 노드에 대해서 자식과 부모위 값을 보고 적절히 위치를 조정한다.
		shiftDown(1);
		return node.index;
	}
	
	public void changeValue(int index, double value){
		HeapNode node = nodeList[indexMap[index]];
		node.value = value;
		shiftUp(indexMap[index]);
	}
	
	
	/**
	 * heap에 새로운 node를 추가하는 함수
	 * @param node
	 */
	public void addNode(int index, double value){
		HeapNode node = new HeapNode(index, value);
		// 일단 끝 부분에 node를 추가시킨다.
		nodeList[maxIndex] = node;
		if (indexMap[index] >= 0) throw new RuntimeException();
		indexMap[index] = maxIndex;
		maxIndex++;
		
		// 새로 추가된 위치에서 부모 node를 recursive 하게 확인하면서
		// 부모의 value가 더 크다면 자신과 부모의 위치를 바꾼다.
		shiftUp(maxIndex - 1);
	}
	
	/**
	 * 주어진 index 위치에서 자식 노드들과 값을 비교해서
	 * 자식 노드들의 값이 더 작다면 둘의 위치를 서로 바꾼뒤,
	 * 아래로 내려간 위치에서 같은 작업을 다시 반복하여
	 * heap이 올바르게 구성될 수 있도록 한다.
	 * 
	 * @param index
	 */
	void shiftDown(int index){
		int child = index*2;
		if (child >= maxIndex) return;
		if (child + 1 < maxIndex){
			if (nodeList[child].value > nodeList[child + 1].value){
				child++;
			}
		}
		if (nodeList[index].value > nodeList[child].value){
			swap(index, child);
			shiftDown(child);
		}
	}
	
	/**
	 * 두 노드의 위치를 서로 바꾼다.
	 * @param i1
	 * @param i2
	 */
	void swap(int i1, int i2){
		HeapNode temp = nodeList[i1];
		nodeList[i1] = nodeList[i2];
		nodeList[i2] = temp;
		indexMap[nodeList[i1].index] = i1;
		indexMap[nodeList[i2].index] = i2;
	}
	
	/**
	 * 주어진 index 위치에서 부모 노드와 값을 비교해서
	 * 부모 노드의 값이 더 크다면 둘의 위치를 서로 바꾼뒤,
	 * 위로 올라간 위치에서 같은 작업을 다시 반복하여
	 * heap이 올바르게 구성될 수 있도록 한다.
	 * @param index
	 */
	void shiftUp(int index){
		int parent = index/2;
		if (parent == 0) return;
		if (nodeList[index].value < nodeList[parent].value){
			swap(index, parent);
			shiftUp(parent);
		}
	}

	private static class HeapNode{
		public int index;
		public double value;
		
		public HeapNode(int index, double value) {
			this.index = index;
			this.value = value;
		}
	}
}
