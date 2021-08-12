package mrl.motion.data;

import java.util.ArrayList;

import mrl.util.Utils;

public abstract class SequenceStitch<E> {
	
	private ArrayList<E> sequence = new ArrayList<E>();
	
	public SequenceStitch(int margin) {
	}
	
	public ArrayList<E> getSequence() {
		return sequence;
	}

	public void append(ArrayList<E> list, int margin){
		if (sequence.size() <= margin){
			sequence.addAll(list);
			return;
		}
		
		int len = margin*2 + 1;
		double unit = 1d/len;
		
		E lastOfSeq = Utils.last(sequence);
		E startOfNew = list.get(0);
		for (int i = 0; i < margin; i++) {
			int idx = sequence.size() - margin + i;
			E e1 = sequence.get(sequence.size() - margin + i);
			E e2 = startOfNew;
			double ratio = (i + 1)*unit;
			sequence.set(idx, interpolate(e1, e2, ratio));
		}
		for (int i = 0; i < Math.min(margin, list.size()); i++) {
			E e1 = lastOfSeq;
			E e2 = list.get(i);
			double ratio = (margin + i + 1)*unit;
			sequence.add(interpolate(e1, e2, ratio));
		}
		for (int i = margin; i < list.size(); i++) {
			sequence.add(list.get(i));
		}
	}
	
	protected abstract E interpolate(E e1, E e2, double ratio); 
	
}
