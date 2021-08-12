package mrl.motion.neural.agility.match;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import mrl.motion.data.MDatabase;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.MotionDistByPoints;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionActionDP;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.data.BallTrajectoryGenerator;
import mrl.util.MathUtil;

public class TransitionDPGenerator {
	
	public static String FILE_PREFIX = "output\\dp_cache\\";
	public static double PASSIVE_ACTION_TIME_OFFSET = 5;
	
	
	
	static void makeManyAction() {
		String[] actions = {
				"i",
				"walk",
				"jog",
				"run",
				
				"bs",
				"casting",
				"cheer",
				"dance",
				"flip",
				"golf",
				"hot",
				"jbs",
				"jd",
				"jk",
				"jump",
				"jump_moving",
				"jump_one",
				"k",
				"roll",
		};
		TransitionData.STRAIGHT_MARGIN = -1;
		String dataFolder = "merged_ue";
		String tPoseFile = "t_pose_ue2.bvh";
		make(dataFolder, tPoseFile, actions, 4);
	}
	
	static void makeStunt() {
		String[] actions = {
				"i",
				"p",
				"k",
				"bs",
				"jk",
				"jbs",
				"jd",
				
				"th",
				"lk",
				"knee",
				"d",
				"g",
				"n",
		};
//		String dataFolder = "dc_stunt";
//		String dataFolder = "stunt_new_label";
//		String dataFolder = "stunt_retargeted";
//		String dataFolder = "stunt_new_label_sOnly";
		String dataFolder = "stunt_new_label_sOnly_short07";
		String tPoseFile = "t_pose_ue2.bvh";
//		String dataFolder = "dc_stunt";
//		String tPoseFile = "t_pose_actor.bvh";
		make(dataFolder, tPoseFile, actions, 1);
	}
	
	
	static void makeStuntLoco() {
		String[] actions = {
				"i",
				"walk",
				"jog",
				"run",
				"p",
				"k",
				"bs",
				"jk",
				"jbs",
				"jd",
				"th",
				"lk",
				"knee",
				"d",
				"g",
				"n",
		};
		TransitionData.STRAIGHT_MARGIN = 1;
		String dataFolder = "stunt_loco";
		String tPoseFile = "t_pose_ue2.bvh";
//		String dataFolder = "dc_stunt";
//		String tPoseFile = "t_pose_actor.bvh";
		make(dataFolder, tPoseFile, actions, 4);
	}
	
	static void makeLocoActor() {
		String[] actions = {
				"idle",
				"walk",
				"jog",
				"run",
				
				"punch",
				"jump",
				"golf",
				"dance",
				"sit",
		};
		TransitionData.STRAIGHT_MARGIN = -1;
		String dataFolder = "loco_actor";
		String tPoseFile = "t_pose_actor.bvh";
		make(dataFolder, tPoseFile, actions, 4);
	}
	
	static void makeFightIdle() {
		String[] actions = {
				"idle",
		};
		String dataFolder = "dcFightIdle";
		String tPoseFile = "t_pose_actor.bvh";
//		String dataFolder = "dc_stunt";
//		String tPoseFile = "t_pose_actor.bvh";
		make(dataFolder, tPoseFile, actions, 1);
	}
	
	static void makeDribble() {
		String[] actions = {
				"stop",
				"dribble",
				"back",
		};
		String dataFolder = "basketTest";
//		String dataFolder = "basketData";
		String tPoseFile = "t_pose_bue.bvh";
		MotionDistByPoints.USE_BALL_CONTACT = true;
		TransitionData.STRAIGHT_MARGIN = 1;
		TransitionData.STRAIGHT_LIMIT_PRESET = new double[] {
			10000, 20, 30
		};
		make(dataFolder, tPoseFile, actions, 3);
	}
	
	static void makeJog() {
		String[] actions = {
				"jog",
		};
//		String dataFolder = "dc_jump_jog";
//		String tPoseFile = "t_pose_actor.bvh";
		String folder = "dc_jog_ue";
		String tPoseFile = "t_pose_ue.bvh";
		TransitionData.STRAIGHT_MARGIN = 3;
		make(folder, tPoseFile, actions, 1);
	}
	
	static void makeLoco() {
//		String[] actions = {
//				"stop",
//				"walk",
//				"jog",
//				"run",
//		};
		String folder = "runjogwalk_withstop";
		String tPoseFile = "t_pose_ue.bvh";
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
	}
	static void makeJump() {
		String[] actions = {
				"jog",
				"jump_both",
				"jump_one",
				"jump_moving",
				"flip"
		};
//		String dataFolder = "dc_jump_jog";
//		String tPoseFile = "t_pose_actor.bvh";
		String folder = "jump_jog_ret_ue4_short05";
//		String folder = "jump_jog_ret_ue4";
		String tPoseFile = "t_pose_ue2.bvh";
		TransitionData.STRAIGHT_MARGIN = 3;
		
		make(folder, tPoseFile, actions, 1);
	}
	
	static void makeJumpSpeed() {
		String[] actions = {
				"stop",
				"walk",
				"jog",
				"run",
				"jump_both",
				"jump_one",
				"jump_moving",
				"flip"
		};
//		String dataFolder = "dc_jump_jog";
//		String tPoseFile = "t_pose_actor.bvh";
		String folder = "jump_speed_ue4";
		String tPoseFile = "t_pose_jue.bvh";
		TransitionData.STRAIGHT_MARGIN = 1;
		
		make(folder, tPoseFile, actions, 4);
	}

	public static boolean printDistribution = false;
	public static void make(String dataFolder, String tPoseFile, String[] actions, int cLabelSize) {
		MDatabase database = TrainingDataGenerator.loadDatabase(dataFolder, tPoseFile);
//		System.exit(0);
		
//		Motion m1 = database.findMotion("s_001_2_1.bvh",  219);
//		Motion m2 = database.findMotion("s_001_3_1.bvh", 177);
//		System.out.println("mi : " + m1.motionIndex + " , " + m2.motionIndex);
//		double d = database.getDist().getDistance(m1.motionIndex + 1, m2.motionIndex);
//		System.out.println(d);
//		System.exit(0);
		
		if (printDistribution) checkAnnDistribution(database, actions);
		
		TransitionData tData = new TransitionData(database, actions, cLabelSize);
		TransitionActionDP dp = new TransitionActionDP(tData);
		dp.calcAndSave(FILE_PREFIX + dataFolder + ".dat");
	}
	
	static void checkAnnDistribution(MDatabase database, String[] actions) {
		HashMap<String, ArrayList<Double>> prevMarginMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> postMarginMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> prevLengthMarginMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> postLengthMarginMap = new HashMap<String, ArrayList<Double>>();
		for (MotionAnnotation ann : database.getEventAnnotations()) {
			ArrayList<Double> prev = prevMarginMap.get(ann.type);
			if (prev == null) {
				prev = new ArrayList<Double>();
				prevMarginMap.put(ann.type, prev);
			}
			prev.add((double)(ann.interactionFrame - ann.startFrame));
			
			ArrayList<Double> post = postMarginMap.get(ann.type);
			if (post == null) {
				post = new ArrayList<Double>();
				postMarginMap.put(ann.type, post);
			}
			post.add((double)(ann.endFrame - ann.interactionFrame));
			
			{
				ArrayList<Double> length = prevLengthMarginMap.get(ann.type);
				if (length == null) {
					length = new ArrayList<Double>();
					prevLengthMarginMap.put(ann.type, length);
				}
				
				if (ann.interactionFrame >= 0) {
					Pose2d p1 = Pose2d.getPose(database.findMotion(ann.file, ann.startFrame));
					Pose2d p2 = Pose2d.getPose(database.findMotion(ann.file, ann.interactionFrame));
					double dist = p1.position.distance(p2.position);
					length.add(dist);
				}
			}
			{
				ArrayList<Double> length = postLengthMarginMap.get(ann.type);
				if (length == null) {
					length = new ArrayList<Double>();
					postLengthMarginMap.put(ann.type, length);
				}
				
				if (ann.interactionFrame >= 0) {
					Pose2d p1 = Pose2d.getPose(database.findMotion(ann.file, ann.interactionFrame));
					Pose2d p2 = Pose2d.getPose(database.findMotion(ann.file, ann.endFrame));
					double dist = p1.position.distance(p2.position);
					length.add(dist);
				}
			}
		}
		
		System.out.println("double tBase = 5.0;");
		System.out.println("double[][] timeOffset = {");
		for (String action : actions) {
			double prev = getMean(prevMarginMap.get(action));
			double post = getMean(postMarginMap.get(action));
			double prevLen = getMean(prevLengthMarginMap.get(action));
			double postLen = getMean(postLengthMarginMap.get(action));
			if (prev < 0) {
				System.out.println("\t{ tBase, tBase, -1, -1 }, // "+ action);
			} else {
				System.out.println(String.format("\t{ %.3f, %.3f, %.3f, %.3f }, // "+ action, prev, post, prevLen, postLen));
			}
		}
		System.out.println("};");
//		ArrayList<Integer> post = postMarginMap.get(action);
		System.exit(0);
		
	}
	
	static double getMean(ArrayList<Double> list) {
		double[] data = new double[list.size()];
		for (int i = 0; i < data.length; i++) {
			data[i] = list.get(i);
		}
		double[] stat = MathUtil.getStatistics(data);
//		System.out.println(Arrays.toString(stat));
		return stat[0];
	}
	
	public static void main(String[] args) {
//		printDistribution = true;
//		makeManyAction();
//		makeLocoActor();
//		makeStuntLoco();
		makeJump();
//		makeJumpSpeed();
//		makeFightIdle();
//		makeDribble();
//		makeStuntLoco();
//		makeStunt();
//		makeJog();
//		makeLoco();
	}
}
