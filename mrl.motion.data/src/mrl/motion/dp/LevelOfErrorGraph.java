package mrl.motion.dp;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MotionAnnotationManager;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.graph.MGraphGenerator;
import mrl.util.Configuration;

public class LevelOfErrorGraph {
	
	public static MDatabase loadDatabase() {
		setConfiguration();
		MDatabase database = MDatabase.load();
		return database;
	}
	public static void setConfiguration() {
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 200;
		MDatabase.loadEventAnnotations = true;
		MotionAnnotationManager.BASE_EVENT_MARGIN = 7;
		MotionAnnotationManager.USE_OPPOSITE_MOTION = false;
		MotionDistByPoints.CONTACT_CONFLICT_ERROR = Configuration.MGRAPH_EDGE_WEIGHT_LIMIT/4;
	}

	public static void main(String[] args) {
		MDatabase database = loadDatabase();
		
		MGraphGenerator g = new MGraphGenerator(database);
		g.saveResult();
		
	}
}
