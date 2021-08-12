package mrl.motion.neural.data;

import java.io.File;
import java.util.ArrayList;

import mrl.motion.data.Motion;
import mrl.motion.graph.MotionSegment;
import mrl.util.Configuration;
import mrl.util.MathUtil;

public class RNNDataGenerator {
	
	public static String OUTPUT_PATH = "output";
	public static boolean NO_INPUT = false;
	public static boolean APPEND_POSE_AS_INPUT = false;
	public static boolean USE_RESIDUAL = false;
	public static boolean USE_VELOCITY = false;
	
	public static void setResidualWithInput(){
		USE_RESIDUAL = true;
		APPEND_POSE_AS_INPUT = true;
	}
	
	public static String prepareTrainingFolder(String label) {
		label = label.replace(".", "");
		String outputPath = Normalizer.NEURALDATA_PREFIX + "\\" + label;
		new File(outputPath + "\\train").mkdirs();
		OUTPUT_PATH = outputPath + "\\data";
		new File(OUTPUT_PATH).mkdirs();
		return OUTPUT_PATH;
	}

	public static void generate(MotionSegment segment, ControlDataGenerator c){
		generate(segment, c, false);
	}
	public static void generate(MotionSegment segment, ControlDataGenerator c, boolean append){
		ArrayList<Motion> mList = segment.getMotionList();
		Motion first = segment.getEntireMotion().get(Configuration.BLEND_MARGIN-1);
		generate(mList, first, c, append);
	}
	
	public static void generate(ArrayList<Motion> mList, ControlDataGenerator c){
		generate(mList, mList.get(0), c, false);
	}
	public static void generate(ArrayList<Motion> mList, Motion firstMotion, ControlDataGenerator c, boolean append){
		new File(OUTPUT_PATH).mkdirs();
		String path = OUTPUT_PATH + "\\";
		
		ArrayList<double[]> mDataList = MotionDataConverter.motionToData(mList,firstMotion, false);
		
		c.setData(mList, mDataList);
		
		ArrayList<double[]> xList = new ArrayList<double[]>();
		ArrayList<double[]> yList = new ArrayList<double[]>();
		boolean useActivation = false;
		int activationLen = 0;
		for (int i = 0; i < mList.size(); i++) {
			double[] x = c.getControl(i);
			if (x == null) break;
			double[] y = mDataList.get(i+1);
			
			if (APPEND_POSE_AS_INPUT){
				if (NO_INPUT){
					x = mDataList.get(i);
				} else {
					x = MathUtil.concatenate(x, mDataList.get(i));
				}
			}
			
			if (USE_RESIDUAL){
				y = MathUtil.copy(y);
				int poseStart = MotionDataConverter.ROOT_OFFSET;
				double[] prevY = mDataList.get(i);
				for (int j = poseStart; j < y.length; j++) {
					y[j] = y[j] - prevY[j];
				}
			}
			xList.add(x);
			
			if (USE_VELOCITY){
				int poseStart = MotionDataConverter.ROOT_OFFSET;
				int vLen = y.length - poseStart;
				double[] velocity = new double[vLen];
				double[] prevY = mDataList.get(i);
				for (int j = 0; j < velocity.length; j++) {
					int idx = poseStart + j;
					velocity[j] = y[idx] - prevY[idx];
				}
				y = MathUtil.concatenate(y, velocity);
			}
			
			double[] prediction = c.getPrediction(i+1);
			if (prediction != null){
				y = MathUtil.concatenate(y, prediction);
			}
			
			double[] add = c.getHasBall(i+1);
			if (add != null){
				useActivation = true;
				activationLen = add.length;
				y = MathUtil.concatenate(y, add);
				if (MotionDataConverter.includeBall && add[0] < 0.5){
					for (int j = 0; j < 8; j++) {
						y[j] = Double.NaN;
					}
				}
			}
			yList.add(y);
		}
		
		if (append){
			double[][] xNormal = DataExtractor.readNormalizeInfo(path + "xNormal.dat");
			double[][] yNormal = DataExtractor.readNormalizeInfo(path + "yNormal.dat");
			
			ArrayList<double[]> xData = DataExtractor.readData(path + "xData.dat");
			for (double[] data : xList){
				data = DataExtractor.getNormalizedData(data, xNormal);
				xData.add(data);
			}
			ArrayList<double[]> yData = DataExtractor.readData(path + "yData.dat");
			for (double[] data : yList){
				data = DataExtractor.getNormalizedData(data, yNormal);
				yData.add(data);
			}
			DataExtractor.writeData(path + "xData.dat", xData);
			DataExtractor.writeData(path + "yData.dat", yData);
		} else {
			DataExtractor.writeNormalizeInfo(path + "xNormal.dat", xList, c.getNormalMarking());
			DataExtractor.writeDataWithNormalize(path + "xData.dat", xList, path + "xNormal.dat");
			
			boolean[] marking = MotionDataConverter.getNormalMarking();
			if (useActivation) {
				int yLength = yList.get(0).length;
				boolean[] markingAll = new boolean[yLength];
				System.arraycopy(marking, 0, markingAll, 0, marking.length);
				for (int j = 0; j < activationLen; j++) {
					markingAll[yLength - 1 - j] = true;
				}
				marking = markingAll;
			}
			DataExtractor.writeNormalizeInfo(path + "yNormal.dat", yList, marking);
			DataExtractor.writeDataWithNormalize(path + "yData.dat", yList, path + "yNormal.dat");
		}
	}
	
	public static void generateMulti(ArrayList<MotionSegment> segmentList, ControlDataGenerator c){
		ArrayList<ArrayList<double[]>> xData = new ArrayList<ArrayList<double[]>>();
		ArrayList<ArrayList<double[]>> yData = new ArrayList<ArrayList<double[]>>();
		ArrayList<double[]> xTotal = new ArrayList<double[]>();
		ArrayList<double[]> yTotal = new ArrayList<double[]>();
		for (MotionSegment segment : segmentList){
			ArrayList<Motion> mList = segment.getMotionList();
			ArrayList<double[]> mDataList = MotionDataConverter.motionToData(segment);
			
			c.setData(mList, mDataList);
			
			ArrayList<double[]> xList = new ArrayList<double[]>();
			ArrayList<double[]> yList = new ArrayList<double[]>();
			for (int i = 0; i < mList.size(); i++) {
				double[] x = c.getControl(i);
				if (x == null) break;
				double[] y = mDataList.get(i+1);
				
				if (APPEND_POSE_AS_INPUT){
					if (NO_INPUT){
						x = mDataList.get(i);
					} else {
						x = MathUtil.concatenate(x, mDataList.get(i));
					}
				}
				
				if (USE_RESIDUAL){
					y = MathUtil.copy(y);
					int poseStart = MotionDataConverter.ROOT_OFFSET;
					double[] prevY = mDataList.get(i);
					for (int j = poseStart; j < y.length; j++) {
						y[j] = y[j] - prevY[j];
					}
				}
				xList.add(x);
				
				if (USE_VELOCITY){
					int poseStart = MotionDataConverter.ROOT_OFFSET;
					int vLen = y.length - poseStart;
					double[] velocity = new double[vLen];
					double[] prevY = mDataList.get(i);
					for (int j = 0; j < velocity.length; j++) {
						int idx = poseStart + j;
						velocity[j] = y[idx] - prevY[idx];
					}
					y = MathUtil.concatenate(y, velocity);
				}
				
				double[] prediction = c.getPrediction(i+1);
				if (prediction != null){
					y = MathUtil.concatenate(y, prediction);
				}
				
				double[] add = c.getHasBall(i+1);
				if (add != null){
					y = MathUtil.concatenate(y, add);
					if (MotionDataConverter.includeBall && add[0] < 0.5){
						for (int j = 0; j < 8; j++) {
							y[j] = Double.NaN;
						}
					}
				}
				yList.add(y);
			}
			
			
			xTotal.addAll(xList);
			yTotal.addAll(yList);
			xData.add(xList);
			yData.add(yList);
		}
		
		String path = OUTPUT_PATH + "\\";
		double[][] xNormal = DataExtractor.writeNormalizeInfo(path + "xNormal.dat", xTotal, c.getNormalMarking());
		for (int i = 0; i < xData.size(); i++) {
			xData.set(i, DataExtractor.getNormalizedData(xData.get(i), xNormal));
		}
//		("xData.dat", xData, "xNormal.dat");
		DataExtractor.writeListData(path + "xData.dat", xData);
		
		// foot contact
		double[][] yNormal = DataExtractor.writeNormalizeInfo(path + "yNormal.dat", yTotal, MotionDataConverter.getNormalMarking());
		for (int i = 0; i < yData.size(); i++) {
			yData.set(i, DataExtractor.getNormalizedData(yData.get(i), yNormal));
		}
		DataExtractor.writeListData(path + "yData.dat", yData);
	}
}
