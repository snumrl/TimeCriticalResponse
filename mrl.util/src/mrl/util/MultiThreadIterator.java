package mrl.util;

public abstract class MultiThreadIterator {
	
	private int threadSize = 16;
	private int processMargin = 300;
	
	
	private int dataSize;
	private boolean[] isProcessed;
	
	private int seedingIndex;
	private int finishedIndex;
	
	private long startTime;
	
	public MultiThreadIterator() {
	}

	public void setThreadSize(int threadSize) {
		this.threadSize = threadSize;
	}

	public void setProcessMargin(int processMargin) {
		this.processMargin = processMargin;
	}

	public void run(int dataSize){
		this.dataSize = dataSize;
		
		
		startTime = System.currentTimeMillis();
		isProcessed = new boolean[dataSize];
		
		SubThread[] threads = new SubThread[threadSize];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new SubThread();
			threads[i].start();
		}
		
		for (int i = 0; i < dataSize; i++) {
			while (!isProcessed(i)){
			}
			
			mainProcess(i);
			
			setFinishedIndex(i);
		}
	}
	
	private int getSeed(){
		while (true){
			synchronized (this) {
				if (seedingIndex < finishedIndex + processMargin) break;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		synchronized (this) {
			int seed = seedingIndex;
			if (seed >= dataSize) return -1;
			seedingIndex++;
			if ((seedingIndex % 100) == 0){
				System.out.println("seed number : " + seedingIndex + " / " + dataSize 
						+ " ( time : " + TimeChecker.timeString(System.currentTimeMillis()-startTime) + " )");
			}
			return seed;
		}
	}

	public abstract void iterate(int index);
	public abstract void mainProcess(int index);
	
	protected boolean isProcessed(int index){
		synchronized (isProcessed) {
			return isProcessed[index];
		}
	}
	
	protected void setFinishedIndex(int index){
		synchronized (this) {
			finishedIndex = index;
		}
	}
	
	private class SubThread extends Thread{
		
		@Override
		public void run(){
			while (true){
				int seed = getSeed();
				if (seed < 0) break;
				iterate(seed);
				synchronized (isProcessed) {
					isProcessed[seed] = true;
				}
			}
		}
		
	}
}
