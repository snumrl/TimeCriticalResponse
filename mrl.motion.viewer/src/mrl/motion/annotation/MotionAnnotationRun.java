package mrl.motion.annotation;

import java.io.File;
import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionData;
import mrl.motion.data.MultiCharacterFolder;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.viewer.MotionAnimator;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.table.FilterUtil;

public class MotionAnnotationRun {
	
	private static boolean isMatch(MotionAnnotation ann, String[] types) {
		for (String type : types) {
			if (type.equals(ann.type)) return true;
		}
		return false;
	}

	static void filterAnnotation(File folder, String filter, int offset) {
		String[] includeTypes = FilterUtil.split(filter);
				
		for (File f : folder.listFiles()) {
			if (!f.getName().toLowerCase().endsWith(".lab")) continue;
			ArrayList<MotionAnnotation> annList = MotionAnnotation.load(f);
			ArrayList<MotionAnnotation> filtered = new ArrayList<MotionAnnotation>();
			for (MotionAnnotation ann : annList) {
				if (isMatch(ann, includeTypes)) {
					ann.startFrame -= offset;
					ann.endFrame += offset;
					filtered.add(ann);
				}
			}
			if (filtered.size() > 0) {
				MotionAnnotation.save(filtered, f);
			} else {
				f.delete();
			}
		}
	}
	
	static void filterByMotion(File folder, MDatabase database, double heightLimit) {
		Motion[] mList = database.getMotionList();
		for (File f : folder.listFiles()) {
			if (!f.getName().toLowerCase().endsWith(".lab")) continue;
			ArrayList<MotionAnnotation> annList = MotionAnnotation.load(f);
			ArrayList<MotionAnnotation> filtered = new ArrayList<MotionAnnotation>();
			for (MotionAnnotation ann : annList) {
				int start = database.findMotion(ann.file, ann.startFrame).motionIndex;
				int end = start + ann.length();
				if (heightFilter(mList, start, end, heightLimit)) {
					filtered.add(ann);
				} else {
					System.out.println("remove :: " + ann);
				}
			}
			if (filtered.size() > 0) {
				MotionAnnotation.save(filtered, f);
			} else {
				f.delete();
			}
		}
	}
	
	static boolean heightFilter(Motion[] mList, int start, int end, double heightLimit) {
		for (int i = start; i <= end; i++) {
			double y = MathUtil.getTranslation(mList[i].root()).y;
			if (y < heightLimit) return false;
		}
		return true;
	}
	
	static void fillAllRange(String dataFolder, String annFolder, int margin) {
		for (File f : new File(dataFolder).listFiles()) {
			MotionData motion = new BVHParser().parse(f.getAbsolutePath());
			int len = motion.motionList.size();
			MotionAnnotation ann = new MotionAnnotation();
			ann.file = f.getName();
			ann.person = 1;
			ann.startFrame = margin;
			ann.endFrame = len - 1 - margin;
			ann.type = "";
			String labelFile = annFolder + "\\" + MultiCharacterFolder.getTakeName(f.getName()) + ".lab";
			MotionAnnotation.save(Utils.singleList(ann), new File(labelFile));
		}
	}
	
	public static void open(String dataFolder, boolean isAnn, boolean useSubAnn) {
		Configuration.setDataFolder(dataFolder);
		String motionFolder = Configuration.MOTION_FOLDER;
		String annFolder;
		String subAnnFolder = null;
		
		annFolder = isAnn ? Configuration.ANNOTATION_FOLDER : Configuration.TRANSITION_FOLDER;
		if (useSubAnn) {
			subAnnFolder = (!isAnn) ? Configuration.ANNOTATION_FOLDER : Configuration.TRANSITION_FOLDER;
		}
		MotionAnimator.showFootContact = true;
		MotionAnnotationHelper helper = new MotionAnnotationHelper(motionFolder, annFolder, subAnnFolder);
		helper.setTitle(annFolder);
		helper.open();
		System.exit(0);
	}
	
}
