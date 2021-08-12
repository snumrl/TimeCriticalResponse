package mrl.util;

public class Configuration {
	
	public static String DATA_FOLDER = "data";
	public static String MOTION_FOLDER = DATA_FOLDER + "\\motion";
	public static String ANNOTATION_FOLDER = DATA_FOLDER + "\\annotation";
	public static String TRANSITION_FOLDER = DATA_FOLDER + "\\transition";
	public static void setDataFolder(String folder){
		DATA_FOLDER = folder;
		MOTION_FOLDER = DATA_FOLDER + "\\motion";
		ANNOTATION_FOLDER = DATA_FOLDER + "\\annotation";
		TRANSITION_FOLDER = DATA_FOLDER + "\\transition";
	}
	public static String BASE_MOTION_FILE = "t_pose.bvh";
	
	public static String MGRAPH_NODE_CACHE_FILE = "mGraphNode.dat";
	public static String MGRAPH_EDGE_CACHE_FILE = "mGraphEdge.dat";
	
	public static double MGRAPH_VELOCITY_WEIGHT = 2.0;
	public static double MGRAPH_EDGE_WEIGHT_LIMIT = 4.0;
	public static int MGRAPH_CONNECTION_MARGIN = 30;

	public static int DEFAULT_FPS = 30;
	public static int MAX_THREAD = 16;
	
	public static int BLEND_MARGIN = 10;
	public static boolean _ENABLE_BLENDING = true;
	
	public static int MOTION_TRANSITON_MARGIN = 8;
}
