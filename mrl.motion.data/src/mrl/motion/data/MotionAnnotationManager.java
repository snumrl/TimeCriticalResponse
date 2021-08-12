package mrl.motion.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import mrl.util.ObjectSerializer;

public class MotionAnnotationManager {
	
	public static int BASE_EVENT_MARGIN = 0;
	public static boolean USE_OPPOSITE_MOTION = false;

	private HashMap<String, HashSet<Integer>> annotationMap;
	private ArrayList<MotionAnnotation> totalAnnotations;

	public MotionAnnotationManager(String annotationFolder) {
		this(annotationFolder, false);
	}
	public MotionAnnotationManager(String annotationFolder, boolean noMargin) {
		this(annotationFolder, noMargin, -1);
	}
	public MotionAnnotationManager(String annotationFolder, boolean noMargin, int minLength) {
		
		annotationMap = new HashMap<String, HashSet<Integer>>();
		totalAnnotations = new ArrayList<MotionAnnotation>();
		
		File folder = new File(annotationFolder);
		if (!folder.exists()) return;
		for (File file : folder.listFiles()){
			if (file.isDirectory() || !file.getName().endsWith(".lab")) continue;
			String defaultType = MotionAnnotation.defaultType;
			ArrayList<MotionAnnotation> list = MotionAnnotation.load(file);
			MotionAnnotation.defaultType = defaultType;
			if (list.size() == 0) continue;
			
			String[] fileNames = getFileNames(list.get(0).file, 4);
			
			for (MotionAnnotation ann : list){
				if (ann.startFrame == 0) continue; // #TODO: danceData 처리하면서 임시로 고침.   
//				if (ann.startFrame == 0) throw new RuntimeException();
				if (!isValid(ann)) continue;
				if (ann.length() < minLength) continue;
				totalAnnotations.add(ann);
				
				int start = ann.startFrame;
				int end = ann.endFrame;
				if (noMargin){
					for (int frame = start; frame <= end; frame++) {
						setAnnotated(ann.file, frame);
					}
				} else {
//					if (ann.isTransitionable()) continue;
					
					if (ann.startFrame == ann.endFrame){
						ann.interactionFrame = ann.startFrame;
//						start -= 5;
//						end += 5;
					} else {
						start -= BASE_EVENT_MARGIN;
						end += BASE_EVENT_MARGIN;
					}
					for (int frame = start; frame <= end; frame++) {
						setAnnotated(ann.file, frame);
					}
					
					
					if (USE_OPPOSITE_MOTION && !ann.isAlone()){
						if (ann.startFrame == ann.endFrame){
							end += 10;
						}
						
						int oppositePerson = ann.oppositePerson;
						if (oppositePerson == 0){
							oppositePerson = (ann.person % 2) + 1; 
						}
						for (int frame = start; frame <= end; frame++) {
							setAnnotated(ann.file, frame);
							setAnnotated(fileNames[oppositePerson - 1], frame);
						}
					}
				}
				
			}
		}
	}
	
	protected boolean isValid(MotionAnnotation ann){
		return true;
	}
	
	public ArrayList<MotionAnnotation> getTotalAnnotations() {
		return totalAnnotations;
	}
	
	public ArrayList<MotionAnnotation> getActionAnnotations(){
		ArrayList<MotionAnnotation> filteredList = new ArrayList<MotionAnnotation>();
		for (MotionAnnotation ann : totalAnnotations){
			if (!ann.isAction()) continue;
			filteredList.add(ann);
		}
		return filteredList;
	}
	
	private void setAnnotated(String file, int frame){
		HashSet<Integer> set = annotationMap.get(file);
		if (set == null){
			set = new HashSet<Integer>();
			annotationMap.put(file, set);
		}
		set.add(frame);
	}
	
	public boolean isAnnotated(String file, int frame){
		HashSet<Integer> set = annotationMap.get(file);
		return set != null && set.contains(frame);
	}
	
	public static String[] getFileNames(String sample, int size){
		String[] fileNames = new String[size];
		String format = sample.substring(0, sample.length() - "1.bvh".length()) + "%d.bvh";
		for (int i = 0; i < fileNames.length; i++) {
			fileNames[i] = String.format(format, i+1);
		}
		return fileNames;
	}
	
	
	public static ArrayList<MotionAnnotation> filterByType(String type, ArrayList<MotionAnnotation> annList){
		ArrayList<MotionAnnotation> filteredList = new ArrayList<MotionAnnotation>();
		for (MotionAnnotation ann : annList){
			if (ann.type.equals(type)){
				filteredList.add(ann);
			}
		}
		return filteredList;
	}
	
}
