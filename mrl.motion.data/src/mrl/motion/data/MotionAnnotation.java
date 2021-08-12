package mrl.motion.data;

import java.io.File;
import java.util.ArrayList;

import mrl.util.ObjectSerializer;
import mrl.util.Utils;
import mrl.widget.table.FilterUtil;
import mrl.widget.table.TableValueGetter;

public class MotionAnnotation{
	
	public static String defaultType = null;

	public String file;
	public int person;
	public int oppositePerson;
	public int startFrame;
	public int endFrame;
	
	public Boolean include;
	public Boolean isAlone;
	
	
	
	public String type;
	public String subtype;
	
	public String mood;
	public String power;
	
	public String beforeActiveState;
	public String afterActiveState;
	public String beforePassiveState;
	public String afterPassiveState;
	
	public Boolean mutipleInteraction;
	public int interactionFrame = -1;
	public String interactionActivePart = null;
	public String interactionPassivePart = null;	
	public String interactionType;
	
	public double weight = 1;
	
	public MotionAnnotation(){
		type = defaultType;
	}
	
	public boolean isNoType() {
		return type == null || type.trim().length() == 0;
	}
	
	public int length() {
		return endFrame - startFrame + 1;
	}
	
	@Override
	public int hashCode(){
		return file.hashCode() + (person<<8) + startFrame  + (endFrame << 5);
	}
	
	@Override
	public boolean equals(Object obj){
		if (obj instanceof MotionAnnotation){
			MotionAnnotation ann = (MotionAnnotation)obj;
			return file.equals(ann.file) && person == ann.person && startFrame == ann.startFrame && endFrame == ann.endFrame; 
		}
		return false;
	}
	
	@Override
	public String toString(){
		return Utils.toString(file, person, startFrame, endFrame, type, subtype);
	}

	public String getOppositeFile(){
		int pIndex = this.oppositePerson;
		if (pIndex == 0){
			pIndex = (person % 2) + 1; 
		}
		return file.substring(0, file.length() - "1.bvh".length()) + pIndex + ".bvh";
	}

	public static ArrayList<MotionAnnotation> filter(ArrayList<MotionAnnotation> annList, String type, String interactionType){
		return FilterUtil.filterItems(annList,  
				new String[][]{
					{ "type", type },
					{ "interactionType", interactionType }
				}, 
				new TableValueGetter<MotionAnnotation>() {
					@Override
					public String[] getTableValues(MotionAnnotation item) {
						return new String[]{ item.type, item.interactionType };
					}

					@Override
					public String[] getColumnHeaders() {
						return new String[]{ "type", "interactionType" };
					}
				}
		);
	}
	
	public boolean isAction(){
		MotionAnnotation ann = this;
		if (ann.startFrame == ann.endFrame) return false;
		if ("1".equals(ann.type)) return false;
		if (ann.mutipleInteraction != null && ann.mutipleInteraction == true) return false;
		if (ann.include != null && ann.include == false) return false;
		return type != null && type.length() > 0 && !type.equals("other");
	}
	
	public boolean isInclude(){
		return include == null || include == true;
	}
	
	public boolean isAlone(){
		return isAlone != null && isAlone == true;
	}
	
	public boolean isTransitionable(){
		return "walk".equals(type) || "standing".equals(type);
	}
	
	public static class MotionAnnValueGetter implements TableValueGetter<MotionAnnotation>{

		@Override
		public String[] getColumnHeaders() {
			return new String[] { "file", "frames", "type", "subtype", "befPassState", "aftPassState", "active part", "passive part", "inter type", "inter frame", "isAlone", "include", "multi-iterac" };	
		}

		@Override
		public String[] getTableValues(MotionAnnotation item) {
			return Utils.toStringArrays(item.file, item.endFrame - item.startFrame + 1, item.type, item.subtype, item.beforePassiveState, item.afterPassiveState,
					item.interactionActivePart, item.interactionPassivePart, item.interactionType, item.interactionFrame, 
					item.isAlone, item.include, item.mutipleInteraction);
		}
	}
	
	public static ArrayList<MotionAnnotation> load(File file){
		return ObjectSerializer.load(MotionAnnotation.class, file);
	}
	public static void save(ArrayList<MotionAnnotation> annotationList, File file){
		ObjectSerializer.save(MotionAnnotation.class, annotationList, file);
	}
	
}
