package mrl.motion.data;

import java.io.File;
import java.io.WriteAbortedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import javax.vecmath.Vector3d;

import mrl.motion.data.MultiCharacterFolder.MultiCharacterFiles;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.util.Configuration;
import mrl.util.IterativeRunnable;
import mrl.util.MathUtil;
import mrl.util.ObjectSerializer;
import mrl.util.Utils;

public class MDatabase {
	
	public enum FrameType{
		Transition, Event, Prefix, Postfix, PreOrPostfix
	}
	
	public static boolean loadEventAnnotations = false;
	public static int MIN_EVENT_ANN_LENGTH = -1;
	
	private File rootFolder;
	private HashMap<String, MotionData> motionDataMap;
	private ArrayList<File> fileList;
	private ArrayList<int[]> fileGroups;
	private MotionData[] motionDataList;
	private Motion[] motionList;
	private MotionInfo[] motionInfoList;
	
	private FrameType[] typeList;
	private double[] weightList;
	public MotionAnnotationManager transitionAnn;
	private MotionAnnotationManager eventAnn;
	private ArrayList<MotionAnnotation> totalAnnotations;
	private MotionDistByPoints dist;
	
	private int[] transitionAfterMIndex;
	
	public static MDatabase load(){
		return new MDatabase(new File(Configuration.MOTION_FOLDER));
	}
	
	private MDatabase(File rootFolder){
		SkeletonData.USE_SINGLE_SKELETON = true;
		this.rootFolder = rootFolder;
		
		loadMotionData(rootFolder);
		
		for (MotionData motionData : motionDataList){
//			MotionEditJNI.instance.checkFootContact(motionData);
			FootContactDetection.checkFootContact(motionData);
		}
		for (int i = 0; i < FootContactDetection.footDistPointOffset.length; i++) {
			SkeletonData.instance.get(FootContactDetection.leftFootJoints[i]).distPointOffset = FootContactDetection.footDistPointOffset[i];
			SkeletonData.instance.get(FootContactDetection.rightFootJoints[i]).distPointOffset = FootContactDetection.footDistPointOffset[i];
		}
		
		if (new File(Configuration.TRANSITION_FOLDER).exists()){
			loadAnnotations();
			
			totalAnnotations = new ArrayList<MotionAnnotation>();
			for (MotionAnnotation ann : eventAnn.getTotalAnnotations()){
				totalAnnotations.add(ann);
			}
		}
		
		if (typeList == null){
			typeList = new FrameType[motionList.length];
			weightList = new double[typeList.length];
			for (int i = 0; i < weightList.length; i++) {
				if (motionList[i].prev != null && motionList[i].next != null){
					weightList[i] = 1;
					typeList[i] = FrameType.Transition;
				}
			}
		} else {
			int count = 0;
			for (FrameType type : typeList){
				if (type != null){
					count++;
				}
			}
			System.out.println("not null size : " + count + " / " + (count/30) + " / " + typeList.length);
		}
		
		calcTransitionAfterMIndex();
	}
	
	private void calcTransitionAfterMIndex() {
		transitionAfterMIndex = new int[motionList.length];
		for (int i = 0; i < motionDataList.length; i++) {
			transitionAfterMIndex[i] = -1;
		}
		
		int start = -1;
		for (int i = 0; i <= typeList.length; i++) {
			if (i == typeList.length || typeList[i] == null || (i > 0 && motionList[i-1].next != motionList[i])) {
				if (start >= 0) {
					int end = i;
					for (int idx = start; idx < end - Configuration.MOTION_TRANSITON_MARGIN; idx++) {
						transitionAfterMIndex[idx] = idx + Configuration.MOTION_TRANSITON_MARGIN;
					}
					start = -1;
				}
			} else {
				if (start < 0) {
					start = i;
				}
			}
		}
	}
	
	public int[] getTransitionAfterMIndex() {
		return transitionAfterMIndex;
	}

	/**
	 * This function does not work.
	 */
	@Deprecated
	public void addMirroredData(){
		FrameType[] typeOrigin = typeList;
		double[] weightOrigin = weightList;
		typeList = new FrameType[typeList.length*2];
		weightList = new double[weightList.length*2];
		for (int i = 0; i < typeOrigin.length; i++) {
			typeList[i] = typeList[i + typeOrigin.length] = typeOrigin[i];
			weightList[i] = weightList[i + weightOrigin.length] = weightOrigin[i];
		}
		MotionData[] originMotionData = motionDataList ; 
		
		motionDataList = new MotionData[originMotionData.length*2];
		for (int i = 0; i < originMotionData.length; i++) {
			motionDataList[i] = originMotionData[i];
			motionDataList[i + originMotionData.length] = new MotionData(originMotionData[i], true);
		}
		
		motionList = new Motion[motionList.length*2];
		motionInfoList = new MotionInfo[motionInfoList.length*2];
		int idx = 0;
		for (MotionData mData : motionDataList){
			for (Motion m : mData.motionList){
				motionList[idx] = m;
				motionInfoList[idx] = new MotionInfo(mData, mData.file.getName(), m.frameIndex);
				m.motionIndex = idx;
				idx++;
			}
		}
	}
	
	public MotionDistByPoints getDist(){
		if (dist == null){
			dist = new MotionDistByPoints(getSkeletonData(), getMotionList());
		}
		return dist;
	}
	
	public SkeletonData getSkeletonData(){
		return SkeletonData.instance;
//		return getMotionDataList()[0].skeletonData;
	}
	
	public String getDatabaseName() {
		return rootFolder.getParentFile().getName();
	}
	
	public MotionData[] getMotionDataList() {
		return motionDataList;
	}

	public Motion[] getMotionList() {
		return motionList;
	}

	public double[] getWeightList() {
		return weightList;
	}
	
	public FrameType[] getTypeList() {
		return typeList;
	}
	
	public MotionInfo[] getMotionInfoList() {
		return motionInfoList;
	}

	public ArrayList<MotionAnnotation> getEventAnnotations() {
		if (!loadEventAnnotations) return getTransitionAnnotations();
		if (eventAnn == null) return null;
		return eventAnn.getTotalAnnotations();
	}
	
	public ArrayList<MotionAnnotation> getTotalAnnotations() {
		return totalAnnotations;
	}
	
	public ArrayList<MotionAnnotation> getTransitionAnnotations() {
		return transitionAnn.getTotalAnnotations();
	}
	public int getInteractionMotionIndex(MotionAnnotation ann) {
		if (ann.interactionFrame < 0) return -1;
		return findMotion(ann.file, ann.interactionFrame).motionIndex;
	}

	public int findMotionDataIndex(String motionFile){
		for (int i = 0; i < motionDataList.length; i++) {
			if (motionDataList[i].file.getName().equals(motionFile)) return i;
		}
		return -1;
	}
	
	public Motion findMotion(String motionString){
		String[] tokens = motionString.split(":");
		String file = tokens[0] + ".bvh";
		int frame = Integer.parseInt(tokens[1]);
		return findMotion(file, frame);
	}
	public Motion findMotion(String motionFile, int frameIndex){
		MotionData data = motionDataMap.get(motionFile);
		if (data == null){
			System.out.println("no file : " + motionFile);
			throw new RuntimeException();
		}
		Motion motion = data.motionList.get(frameIndex);
		if (motion == null){
			System.out.println("no file : " + motionFile + " ; " + frameIndex);
			throw new RuntimeException();
		}
		return motion;
	}
	
	private void loadAnnotations(){
		typeList = new FrameType[motionList.length];
		weightList = new double[typeList.length];
		for (int i = 0; i < weightList.length; i++) {
			weightList[i] = 1;
		}
		
		transitionAnn = new MotionAnnotationManager(Configuration.TRANSITION_FOLDER, true);
		for (int i = 0; i < typeList.length; i++) {
			MotionInfo motionInfo = motionInfoList[i];
			if (transitionAnn.isAnnotated(motionInfo.fileName, motionInfo.frameIndex)){
				typeList[i] = FrameType.Transition;
			}
		}
		
		
		eventAnn = new MotionAnnotationManager(Configuration.ANNOTATION_FOLDER, false, MIN_EVENT_ANN_LENGTH);
//		if (transitionAnn != null){
//			writeTypeAnnotations();
//			return;
//		}
		
		if (!loadEventAnnotations) return;
		for (int i = 0; i < typeList.length; i++) {
			MotionInfo motionInfo = motionInfoList[i];
			if (eventAnn.isAnnotated(motionInfo.fileName, motionInfo.frameIndex)){
				typeList[i] = FrameType.Event;
			}
		}
//		
//		String filterLabel = null;
////		String filterLabel = "kick";
//		for (MotionAnnotation ann : eventAnn.getActionAnnotations()){
//			if (filterLabel != null && !filterLabel.equals(ann.type)) continue;
//			
////			int margin = 30*2;
////			if (ann.isTransitionable()){
////				margin = 15;
////			}
//			int margin = 10;
//			
//			fillPrePostType(typeList, findMotion(ann.file, ann.startFrame), findMotion(ann.file, ann.endFrame), margin, ann.weight);
//			
//			
//			if (!ann.isAlone()){
//				int oppositePerson = ann.oppositePerson;
//				if (oppositePerson == 0){
//					oppositePerson = (ann.person % 2) + 1; 
//				}
//				String[] fileNames = MotionAnnotationManager.getFileNames(ann.file, 4);
//				String oppositeFile = fileNames[oppositePerson - 1];
//				if (motionDataMap.get(oppositeFile) == null) continue;
//				fillPrePostType(typeList, findMotion(oppositeFile, ann.startFrame), findMotion(oppositeFile, ann.endFrame), margin, ann.weight);
//			}
//		}
//		
//		for (int i = 0; i < typeList.length; i++) {
//			if (typeList[i] == FrameType.Event){
//				typeList[i] = null;
//			}
//		}
//		
//		for (MotionAnnotation ann : eventAnn.getActionAnnotations()){
//			if (ann.isTransitionable()) continue;
//			if (filterLabel != null && !filterLabel.equals(ann.type)) continue;
//			Motion m1 = findMotion(ann.file, ann.startFrame);
//			Motion m2 = findMotion(ann.file, ann.endFrame);
//			for (int i = m1.motionIndex + 1; i <= m2.motionIndex - 1; i++) {
//				if (typeList[i] != null){
//					System.out.println("motion :: " + motionList[i] + " : " + ann);
//					throw new RuntimeException("inv null : " + typeList[i]);
//				}
//				typeList[i] = FrameType.Event;
//			}
//			
////			if (!ann.isAlone()){
////				int oppositePerson = ann.oppositePerson;
////				if (oppositePerson == 0){
////					oppositePerson = (ann.person % 2) + 1; 
////				}
////				String[] fileNames = MotionAnnotationManager.getFileNames(ann.file, 4);
////				String oppositeFile = fileNames[oppositePerson - 1];
////				if (motionDataMap.get(oppositeFile) == null) continue;
////				
////				m1 = findMotion(oppositeFile, ann.startFrame);
////				m2 = findMotion(oppositeFile, ann.endFrame);
////				for (int i = m1.motionIndex + 1; i <= m2.motionIndex - 1; i++) {
////					if (typeList[i] != null) throw new RuntimeException();
////					typeList[i] = FrameType.Event;
////				}
////			}
//		}
		
//		for (MotionAnnotation ann : eventAnn.getActionAnnotations()){
//			if (ann.isTransitionable()) continue;
//			if (ann.isAlone()) continue;
//			if (filterLabel != null && !filterLabel.equals(ann.type)) continue;
//			
//			int oppositePerson = ann.oppositePerson;
//			if (oppositePerson == 0){
//				oppositePerson = (ann.person % 2) + 1; 
//			}
//			String[] fileNames = MotionAnnotationManager.getFileNames(ann.file, 4);
//			String oppositeFile = fileNames[oppositePerson - 1];
//			if (motionDataMap.get(oppositeFile) == null) continue;
//			
//			int defaultMargin = 5;
//			{
//				Motion m1 = findMotion(ann.file, ann.startFrame);
//				Motion m2 = findMotion(ann.file, ann.endFrame);
//				for (int i = 0; i <= defaultMargin; i++) {
//					if (typeList[m1.motionIndex - i] == null){
//						typeList[m1.motionIndex - i] = FrameType.Prefix;
//						weightList[m1.motionIndex - i] = Math.min(weightList[m1.motionIndex - i], ann.weight);
//					}
//					if (typeList[m2.motionIndex + i] == null){
//						typeList[m2.motionIndex + i] = FrameType.Postfix;
//						weightList[m2.motionIndex + i] = Math.min(weightList[m2.motionIndex + i], ann.weight);
//					}
//				}
//			}
//			{
//				Motion m1 = findMotion(oppositeFile, ann.startFrame);
//				Motion m2 = findMotion(oppositeFile, ann.endFrame);
//				for (int i = 0; i <= defaultMargin; i++) {
//					if (typeList[m1.motionIndex - i] == null){
//						typeList[m1.motionIndex - i] = FrameType.Prefix;
//						weightList[m1.motionIndex - i] = Math.min(weightList[m1.motionIndex - i], ann.weight);
//					}
//					if (typeList[m2.motionIndex + i] == null){
//						typeList[m2.motionIndex + i] = FrameType.Postfix;
//						weightList[m2.motionIndex + i] = Math.min(weightList[m2.motionIndex + i], ann.weight);
//					}
//				}
//			}
//		}
		
		int nullCounts = 0;
		int[] typeCounts = new int[FrameType.values().length];
		for (int i = 0; i < typeList.length; i++) {
			if (typeList[i] != null){
				MotionInfo info = motionInfoList[i];
				if (info.frameIndex < Configuration.BLEND_MARGIN
						|| info.motionData.motionList.size() - info.frameIndex < Configuration.BLEND_MARGIN){
					typeList[i] = null;
				}
			}
			
			
			if (typeList[i] == null){
				nullCounts++;
			}
			for (int j = 0; j < typeCounts.length; j++) {
				if (FrameType.values()[j] == typeList[i]){
					typeCounts[j]++;
				}
			}
		}
		System.out.println("type counts : " + Arrays.toString(typeCounts) + " , " + nullCounts);
		
		int sum = 0;
		for (MotionAnnotation ann : transitionAnn.getTotalAnnotations()){
			sum += ann.endFrame - ann.startFrame + 1;
		}
		System.out.println("sum : " + sum);
		
		
//		for (MotionData mData : this.getMotionDataList()){
//			boolean[] isInvalid = new InvalidRotationDetect(mData).checkAll(false);
//			InvalidRotationDetect.adjust(isInvalid, 20);
//			for (int i = 0; i < isInvalid.length; i++) {
//				int idx = mData.motionList.get(i).motionIndex;
//				if (isInvalid[i]){
//					typeList[idx] = null;
//					System.out.println("INVALID ROT :: " + mData.file.getName() + " : " + i);
//				}
//			}
//		}
		
//		writeTypeAnnotations();
	}
	
	private void writeTypeAnnotations(){
		String typeFolder = "mDatabaseTypeAnns";
		if (new File(typeFolder).exists() == false){
			new File(typeFolder).mkdirs();
		} else {
		}
		for (int[] fileGroup : fileGroups){
			ArrayList<MotionAnnotation> annList = new ArrayList<MotionAnnotation>();
			for (int i = fileGroup[0]; i <= fileGroup[1]; i++) {
				MotionData motionData = motionDataList[i];
				int[][] intervals = Utils.divide(motionData.motionList, new Comparator<Motion>() {
					@Override
					public int compare(Motion o1, Motion o2) {
						if (typeList[o1.motionIndex] == typeList[o2.motionIndex]){
							return 0;
						} else {
							return 1;
						}
					}
				});
				
				
				for (int[] interval : intervals){
					Motion m1 = motionData.motionList.get(interval[0]);
					Motion m2 = motionData.motionList.get(interval[1]);
					FrameType type = typeList[m1.motionIndex];
					if (type != typeList[m2.motionIndex]) throw new RuntimeException();
					if (type == null) continue;
					
					MotionAnnotation ann = new MotionAnnotation();
					ann.file = motionData.file.getName();
					ann.person = i - fileGroup[0] + 1;
					ann.type = type.toString();
					ann.startFrame = interval[0];
					ann.endFrame = interval[1];
					if (weightList[m1.motionIndex] != weightList[m2.motionIndex]){
						System.out.println("weight different : " + weightList[m1.motionIndex] + " , " + weightList[m2.motionIndex]);
					}
					ann.weight = weightList[m1.motionIndex];
					annList.add(ann);
				}
			}
			
			File file = new File(typeFolder + "\\" + MultiCharacterFolder.getTakeName(fileList.get(fileGroup[0]).getName()) + ".lab");
			ObjectSerializer.save(MotionAnnotation.class, annList, file);
		}
	}
	
	private void fillPrePostType(FrameType[] typeList, Motion startMotion, Motion endMotion, int margin, double weight){
		for (int i = 0; i < margin; i++) {
			if (startMotion == null || startMotion.motionIndex < 0) break;
			int idx = startMotion.motionIndex;
			if (typeList[idx] == FrameType.Event || typeList[idx] == FrameType.Transition) break;
			if (typeList[idx] == null){
				typeList[idx] = FrameType.Prefix;
			} else {
				typeList[idx] = FrameType.PreOrPostfix;
			}
			weightList[idx] = Math.min(weightList[idx], weight);
			startMotion = startMotion.prev;
		}
		for (int i = 0; i < margin; i++) {
			if (endMotion == null || endMotion.motionIndex < 0) break;
			int idx = endMotion.motionIndex;
			if (typeList[idx] == FrameType.Event || typeList[idx] == FrameType.Transition) break;
			if (typeList[idx] == null){
				typeList[idx] = FrameType.Postfix;
			} else {
				typeList[idx] = FrameType.PreOrPostfix;
			}
			weightList[idx] = Math.min(weightList[idx], weight);
			endMotion = endMotion.next;
		}
	}
	
	
	private void loadMotionData(File rootFolder){
		long t = System.currentTimeMillis();
		
		fileList = new ArrayList<File>();
		fileGroups = new ArrayList<int[]>();
		int fgIndex = 0;
		ArrayList<MultiCharacterFolder> folderList = MultiCharacterFolder.loadChildFolders(rootFolder);
		for (MultiCharacterFolder folder : folderList) {
			for (MultiCharacterFiles file : folder.list){
				for (File f : file.fileList){
					fileList.add(f);
				}
				fileGroups.add(new int[]{ fgIndex, fgIndex + file.fileList.size()-1});
				fgIndex += file.fileList.size();
			}
		}
		
		
		motionDataList = new MotionData[fileList.size()];
		Utils.runMultiThread(new IterativeRunnable() {
			@Override
			public void run(int index) {
				BVHParser parser = new BVHParser();
				motionDataList[index] = parser.parse(fileList.get(index));
			}
		}, motionDataList.length);
		
		int counts = 0;
		motionDataMap = new HashMap<String, MotionData>();
		for (MotionData motionData : motionDataList){
			counts += motionData.motionList.size();
			motionDataMap.put(motionData.file.getName(), motionData);
		}
		
		int idx = 0;
		motionList = new Motion[counts];
		motionInfoList = new MotionInfo[counts];
		for (MotionData motionData : motionDataList){
			for (int i = 0; i < motionData.motionList.size(); i++) {
				Motion motion = motionData.motionList.get(i);
				motion.motionIndex = idx;
				motion.motionData = motionData;
				motion.frameIndex = i;
				motionList[idx] = motion;
				motionInfoList[idx] = new MotionInfo(motionData, motionData.file.getName(), i);
				idx++;
			}
		}
		
		System.out.println("[MDatabase] loadMotionData - files : " + fileList.size() + ", frames : " + counts + "  ( load time : " + (System.currentTimeMillis() - t) + " )");
		
		
		for (MotionData motionData : motionDataList){
			applyDistortionForEdit(motionData.motionList);
			
			for (int i = 0; i < motionData.motionList.size()-2; i++) {
				Motion m1 = motionData.motionList.get(i);
				Motion m2 = motionData.motionList.get(i+2);
				Vector3d t1 = MathUtil.getTranslation(m1.root());
				Vector3d t2 = MathUtil.getTranslation(m2.root());
				if (t1.equals(t2)){
					System.out.println("zero :: " + motionData.file.getName() + " : " + i);
				}
			}
		}
	}
	
	public static void applyDistortionForEdit(ArrayList<Motion> motionList){
		double eps = 0.00001;
		while (true){
			boolean isChanged = false;
			for (int i = 0; i < motionList.size()-2; i++) {
				Motion m1 = motionList.get(i);
				Motion m2 = motionList.get(i+1);
				Motion m3 = motionList.get(i+2);
				Vector3d t1 = MathUtil.getTranslation(m1.root());
				Vector3d t2 = MathUtil.getTranslation(m2.root());
				Vector3d t3 = MathUtil.getTranslation(m3.root());
				t1.y = 0;
				t2.y = 0;
				t3.y = 0;
				double d1 = MathUtil.distance(t1, t2);
				double d2 = MathUtil.distance(t1, t3);
				if (d1 < eps || d2 < eps){
					t1 = MathUtil.getTranslation(m1.root());
					switch (i%3) {
					case 0: t1.x += 0.0013; break;
					case 1: t1.x -= 0.0011; break;
					case 2: t1.z += 0.0012; break;
					}
					m1.root().setTranslation(t1);
					isChanged = true;
				}
			}
			if (!isChanged) break;
		}
	}
	
	public static class MotionInfo{
		public String fileName;
		public int frameIndex;
		public MotionData motionData;
		
		
		public MotionInfo(MotionData motionData, String fileName, int frameIndex) {
			this.motionData = motionData;
			this.fileName = fileName;
			this.frameIndex = frameIndex;
		}
		
		public String toString(){
			return Utils.toString(fileName, frameIndex);
		}
	}
	
	
	public static void main(String[] args) {
		new MDatabase(new File("bvhOutput"));
	}
}

