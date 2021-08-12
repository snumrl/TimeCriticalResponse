package mrl.util;

import java.util.LinkedList;

import mrl.util.Utils;

public class TimeWarping {
	
	private double[][] distanceMap;
	private WarpData[][] warpMap;
	private WarpData tail;
	private double warpingDistance;
	
	private int offset1;
	private int offset2;
	

	public TimeWarping(double[][] distanceMap) {
		this(distanceMap, false);
	}
	public TimeWarping(double[][] distanceMap, boolean startFree) {
		this(distanceMap, startFree, null, 1);
	}
	public TimeWarping(double[][] distanceMap, boolean startFree, WarpChecker checker, int countLimit){
		this(distanceMap, startFree, checker, countLimit, 0, 0, distanceMap.length, distanceMap[0].length);
	}
	public TimeWarping(double[][] distanceMap, boolean startFree, WarpChecker checker, int countLimit,
			int offset1, int offset2, int size1, int size2) {
		this.distanceMap = distanceMap;
		this.offset1 = offset1;
		this.offset2 = offset2;
		warpMap = new WarpData[size1][size2];
		
		for (int i = 0; i < size1; i++) {
			for (int j = 0; j < size2; j++) {
				double d = dist(i,j);
				if (i == 0 && j == 0){
					warpMap[i][j] = new WarpData(null, d, 0, 0);
				} else if (i == 0){
					if (startFree){
						warpMap[i][j] = new WarpData(null, d, 0, 0);
					} else {
						warpMap[i][j] = new WarpData(warpMap[i][j-1], warpMap[i][j-1].distance + d, 0, 0);
						if (j > 2) warpMap[i][j].distance = Integer.MAX_VALUE;
					}
				} else if (j == 0){
//					if (startFree){
//						warpMap[i][j] = new WarpData(null, d, 0, 0);
//					} else {
						warpMap[i][j] = new WarpData(warpMap[i-1][j], warpMap[i-1][j].distance + d, 0, 0);
						if (i > 2) warpMap[i][j].distance = Integer.MAX_VALUE;
//					}
				} else {
					WarpData w1 = warpMap[i-1][j-1];
					WarpData w2 = warpMap[i][j-1];
					WarpData w3 = warpMap[i-1][j];
					double d1 = w1.distance;
					double d2 = w2.distance;
					double d3 = w3.distance;
					if (w2.horiCount >= countLimit){
						d2 = Integer.MAX_VALUE;
					}
					if (w3.vertiCount >= countLimit){
						d3 = Integer.MAX_VALUE;
					}
					if (checker != null){
						if (!checker.isValid(i-1, j-1, i, j)) d1 = Integer.MAX_VALUE;
						if (!checker.isValid(i, j-1, i, j)) d2 = Integer.MAX_VALUE;
						if (!checker.isValid(i-1, j, i, j)) d3 = Integer.MAX_VALUE;
					}
					
					WarpData prev = null;
					int horiCount = 0;
					int vertiCount = 0;
					if (d1 <= d2 && d1 <= d3){
						prev = w1;
						if (d1 == Integer.MAX_VALUE){
							d = Integer.MAX_VALUE;
						}
					} else if (d2 <= d3){
						prev = w2;
						horiCount = prev.horiCount + 1;
						if (d2 == Integer.MAX_VALUE){
							d = Integer.MAX_VALUE;
						}
					} else {
						prev = w3;
						vertiCount = prev.vertiCount + 1;
						if (d3 == Integer.MAX_VALUE){
							d = Integer.MAX_VALUE;
						}
					}
					if (d >= Integer.MAX_VALUE && dist(i,j) < Integer.MAX_VALUE){
						if (w2.distance < Integer.MAX_VALUE && i > 0 && j > 1 && warpMap[i-1][j-2].distance < Integer.MAX_VALUE){
							warpMap[i][j] = new WarpData(warpMap[i][j-1], warpMap[i-1][j-2].distance + dist(i,j-1) + dist(i,j), 1, 0);
							warpMap[i][j].prev2_1 = warpMap[i][j-1];
							warpMap[i][j].prev2_2 = warpMap[i-1][j-2];
						} else if (w3.distance < Integer.MAX_VALUE && i > 1 && j > 0 && warpMap[i-2][j-1].distance < Integer.MAX_VALUE){
							warpMap[i][j] = new WarpData(warpMap[i-1][j], warpMap[i-2][j-1].distance + dist(i-1,j) + dist(i,j), 0, 1);
							warpMap[i][j].prev2_1 = warpMap[i-1][j];
							warpMap[i][j].prev2_2 = warpMap[i-2][j-1];
						} else {
							warpMap[i][j] = new WarpData(prev, Integer.MAX_VALUE, horiCount, vertiCount);
						}
					} else {
						warpMap[i][j] = new WarpData(prev, prev.distance + d, horiCount, vertiCount);
					}
				}
				warpMap[i][j].i = i;
				warpMap[i][j].j = j;
			}
		}
		tail = warpMap[size1-1][size2-1];
		warpingDistance = tail.distance;
	}
	
	private double dist(int i, int j){
		return distanceMap[i + offset1][j + offset2];
	}
	
	public double warpingDistance(){
		return warpingDistance;
	}
	
	public WarpData getTail() {
		return tail;
	}
	
	public void setTail(WarpData tail) {
		this.tail = tail;
	}
	
	public WarpData[][] getWarpMap() {
		return warpMap;
	}
	
	public int getMatchingIndex(int idx1){
		WarpData wData = tail;
		while (wData != null){
			if (wData.i == idx1) return wData.j;
			wData = wData.prev;
		}
		throw new RuntimeException();
	}
	
	public static interface WarpChecker{
		public boolean isValid(int i1, int j1, int i2, int j2);
	}
	
	public static class WarpData{
		public WarpData prev;
		public WarpData prev2_1;
		public WarpData prev2_2;
		public WarpData prev2_3;
		public double distance;
		public int length;
		public int horiCount;
		public int vertiCount;
		
		public int i;
		public int j;
		
		public WarpData(WarpData prev, double distance, int horiCount,
				int vertiCount) {
			this.prev = prev;
			this.distance = distance;
			this.horiCount = horiCount;
			this.vertiCount = vertiCount;
			
			if (prev == null){
				length = 1;
			} else {
				length = prev.length + 1;
			}
		}
		
		public WarpData[] getPath(){
			LinkedList<WarpData> list = new LinkedList<TimeWarping.WarpData>();
			WarpData current = this;
			while (current != null){
				list.addFirst(current);
				if (current.prev2_1 != null){
					list.addFirst(current.prev2_1);
					if (current.prev2_3 != null){
						list.addFirst(current.prev2_2);
						current = current.prev2_3;
					} else {
						current = current.prev2_2;
					}
				} else {
					current = current.prev;
				}
			}
			return list.toArray(new WarpData[list.size()]);
		}
	}
}
