package mrl.motion.neural.data;

import java.util.ArrayList;

import mrl.util.MathUtil;
import mrl.util.Utils;

public class TimingDataExtractor {
	
	static int TIMING_PREDICTON_MODE = 1;
	static int TIMING_REMOVE_MODE = 2;
			
	static int mode = TIMING_PREDICTON_MODE;

	private Normalizer normal;
	private int tStartIndex;
	private int tEndIndex;
	private int tLength;
	
	public TimingDataExtractor(Normalizer normal, int tStartIndex, int tLength) {
		this.normal = normal;
		this.tStartIndex = tStartIndex;
		this.tLength = tLength;
		tEndIndex  = tStartIndex + tLength - 1;
	}


	public void extract(String targetFolder) {
		targetFolder = targetFolder + "\\";
		
		double[][] xMeanAndStd = new double[2][];
		double[][] yMeanAndStd = new double[2][];
		ArrayList<double[]> xList = new ArrayList<double[]>();
		ArrayList<double[]> yList = new ArrayList<double[]>();
		
		if (mode == TIMING_PREDICTON_MODE) {
			for (int i = 0; i < 2; i++) {
				xMeanAndStd[i] = getInput(normal.xMeanAndStd[i], normal.yMeanAndStd[i]);
				yMeanAndStd[i] = getTiming(normal.xMeanAndStd[i]);
			}
			for (int i = 1; i < normal.xList.size(); i++) {
				xList.add(getInput(normal.xList.get(i), normal.yList.get(i-1)));
				yList.add(getTiming(normal.xList.get(i)));
			}
		} else if (mode == TIMING_REMOVE_MODE) {
			for (int i = 0; i < 2; i++) {
				xMeanAndStd[i] = getTimingRemoved(normal.xMeanAndStd[i]);
			}
			for (int i = 0; i < normal.xList.size(); i++) {
				xList.add(getTimingRemoved(normal.xList.get(i)));
			}
			yMeanAndStd = normal.yMeanAndStd;
			yList = normal.yList;
		}
		
		DataExtractor.writeNormalizeInfo(targetFolder + "xNormal.dat", xMeanAndStd[0], xMeanAndStd[1]);
		DataExtractor.writeNormalizeInfo(targetFolder + "yNormal.dat", yMeanAndStd[0], yMeanAndStd[1]);
		DataExtractor.writeData(targetFolder + "xData.dat", xList);
		DataExtractor.writeData(targetFolder + "yData.dat", yList);
	}
	
	private double[] getTiming(double[] x) {
		return Utils.cut(x, tStartIndex, tEndIndex);
	}
	
	private double[] getInput(double[] x, double[] prevY) {
		double[] xPrefix = null;
		double[] xPostfix = null;
		if (tStartIndex > 0) {
			xPrefix = Utils.cut(x, 0, tStartIndex-1);
		}
		if (tEndIndex < x.length-1) {
			xPostfix = Utils.cut(x, tEndIndex + 1, x.length-1);
		}
		return MathUtil.concatenate(xPrefix, xPostfix, prevY);
	}
	
	private double[] getTimingRemoved(double[] x) {
		double[] xPrefix = null;
		double[] xPostfix = null;
		if (tStartIndex > 0) {
			xPrefix = Utils.cut(x, 0, tStartIndex-1);
		}
		if (tEndIndex < x.length-1) {
			xPostfix = Utils.cut(x, tEndIndex + 1, x.length-1);
		}
		return MathUtil.concatenate(xPrefix, xPostfix);
	}
	
	
	public static void main(String[] args) {
		Normalizer normal = new Normalizer("kickTest_no_interpol");
		TimingDataExtractor e = new TimingDataExtractor(normal, 2, ActionOnlyWaveControl.WAVE_SIZE*2);
		mode = TIMING_REMOVE_MODE; 
		e.extract(RNNDataGenerator.prepareTrainingFolder("tRemoved_kickTest_no_interpol"));
	}
}
