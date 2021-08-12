package edu.wlu.cs.levy.cg;

import java.util.PriorityQueue;

// Bjoern Heckel's solution to the KD-Tree n-nearest-neighbor problem

class NearestNeighborList<T> {

	static class NeighborEntry<T> implements Comparable<NeighborEntry<T>> {
		final T data;
		final double value;

		public NeighborEntry(final T data, final double value) {
			this.data = data;
			this.value = value;
		}

		public int compareTo(NeighborEntry<T> t) {
			// note that the positions are reversed!
			return Double.compare(t.value, this.value);
		}
	};

	PriorityQueue<NeighborEntry<T>> m_Queue;
	int m_Capacity = 0;

	// constructor
	public NearestNeighborList(int capacity) {
		m_Capacity = capacity;
		m_Queue = new PriorityQueue<NeighborEntry<T>>(m_Capacity);
	}

	public double getMaxPriority() {
		NeighborEntry<T> p = m_Queue.peek();
		return (p == null) ? Double.POSITIVE_INFINITY : p.value;
	}

	public boolean insert(T object, double priority) {
		if (isCapacityReached()) {
			if (priority > getMaxPriority()) {
				// do not insert - all elements in queue have lower priority
				return false;
			}
			m_Queue.add(new NeighborEntry<T>(object, priority));
			// remove object with highest priority
			m_Queue.poll();
		} else {
			m_Queue.add(new NeighborEntry<T>(object, priority));
		}
		return true;
	}

	public boolean isCapacityReached() {
		return m_Queue.size() >= m_Capacity;
	}

	public T getHighest() {
		NeighborEntry<T> p = m_Queue.peek();
		return (p == null) ? null : p.data;
	}

	public boolean isEmpty() {
		return m_Queue.size() == 0;
	}

	public int getSize() {
		return m_Queue.size();
	}

	public T removeHighest() {
		// remove object with highest priority
		NeighborEntry<T> p = m_Queue.poll();
		return (p == null) ? null : p.data;
	}
}
