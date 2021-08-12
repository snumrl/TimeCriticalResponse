package mrl.util.heap;

public class MinHeap<E extends HeapNode> {

	E[] nodeList;
	int maxIndex = 1;
	
	public MinHeap(E[] nodeList){
		this.nodeList = nodeList;
	}
	
	public int getCurrentSize(){
		return maxIndex - 1;
	}
	
	/**
	 * 현재 heap의 node들 중 value가 가장 작은 node를 반환하는 함수
	 * @return
	 */
	public E getMinimum(){
		while (true){
			if (maxIndex <= 1) return null;
			// 기본적으로 1번째 index에 있는 node가 가장 작은 value를 가진 node이지만,
			// 속도상의 이유로 Intrinsic dimensionality 조건을 확인 하지 않았을 수 있기 때문에,
			// 확인이 안된 경우 확인 하고 조건을 만족하지 못하면 value를 infinity 값으로 두고
			// 다시 가장 value 값이 낮은 node를 새로 뽑아서 확인하는 식으로 반복한다.
			E node = nodeList[1];
			// 해당 node에 대해 intrinsic dimensionality 조건을 확인하고
			// 올바른 경우에는 node를 반환하고 그렇지 않으면
			// 해당 node의 value가 바뀜에 따라 heap을 재구성한다.
			if (node.validate()){
				return node;
			} else {
//					shiftDown(1);
				removeNode(node);
				node.onRemoveByValidation();
			}
		}
	}
	
	/**
	 * heap에 새로운 node를 추가하는 함수
	 * @param node
	 */
	public void addNode(E node){
		// 일단 끝 부분에 node를 추가시킨다.
		nodeList[maxIndex] = node;
		node.index = maxIndex;
		maxIndex++;
		
		// 새로 추가된 위치에서 부모 node를 recursive 하게 확인하면서
		// 부모의 value가 더 크다면 자신과 부모의 위치를 바꾼다.
		shiftUp(maxIndex - 1);
	}
	
	/**
	 * heap에서 node를 제거하는 함수
	 * @param node
	 */
	public void removeNode(E node){
//		if (node.index == -1) throw new RuntimeException();
		if (node.index == -1) return;
		
		if (node.index == maxIndex - 1){
			// 마지막에 위치한 node라면 그냥 빼고 maxIndex를 1 줄이면 된다.
			maxIndex--;
			nodeList[maxIndex] = null;
			node.index = -1;
		} else {
			// 다른 위치에 있는 node라면 맨 마지막 위치의 node와 위치를 바꾸고 
			// 마찬가지로 maxIndex를 1 줄여서 해당 노드를 뺀다.				 
			E movedNode = nodeList[maxIndex - 1];
			nodeList[node.index] = movedNode;
			movedNode.index = node.index;
			nodeList[maxIndex - 1] = null;
			maxIndex--;
							
			
			// 새로 위치가 바뀐 노드에 대해서 자식과 부모위 값을 보고 적절히 위치를 조정한다.
			shiftDown(node.index);
			if (nodeList[node.index] == movedNode){
				shiftUp(node.index);
			}
			
			// 제거된 노드의 index를 -1로 두어 삭제된 노드임을 표시한다.
			node.index = -1;
		}
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
		E temp = nodeList[i1];
		nodeList[i1] = nodeList[i2];
		nodeList[i2] = temp;
		nodeList[i1].index = i1;
		nodeList[i2].index = i2;
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
}
