package mrl.motion.neural.agility.measure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.util.MathUtil;
import mrl.util.Utils;

public class BaseCriticalTimeMeasure {

	static String label;
	
	private TransitionData tData;
	private MDatabase database;
	private int[] actionTypes;
	
	public BaseCriticalTimeMeasure(TransitionData tData) {
		this.tData = tData;
		this.database = tData.database;
		actionTypes = tData.motionActionTypes;
	}
	
	public void measure() {
		ArrayList<MatchData> total = new ArrayList<MatchData>();
		double interval = 0.05;
		for (double rot = -Math.PI + 0.1; rot < Math.PI - 0.1; rot+=interval) {
//		for (double rot = -Math.PI; rot < Math.PI; rot+=0.5) {
			ArrayList<MatchData> matchList = new ArrayList<MatchData>();
			for (MotionData mData : database.getMotionDataList()) {
//				System.out.println("search : " + mData.file.getName());
				matchList.addAll(findMatch(mData.motionList, rot));
			}
			if (matchList.size() == 0) {
				System.out.println("no :: " + rot);
				continue;
			}
			MatchData[] array = Utils.toArray(matchList);
			Arrays.sort(array);
			
			ArrayList<MatchData> best = pickBest(1, array);
			for (MatchData m : best) {
				m.goal = rot;
			}
			total.addAll(best);
		}
		
		WeightedKDE kde = new WeightedKDE();
		for (MatchData m : total) {
			kde.addSample(new double[] { m.goal }, m.cTime);
		}
		String outputFolder = "output\\rotBaseCTime\\";
		kde.save(outputFolder + label + ".txt");
	}
	
	private ArrayList<MatchData> pickBest(int n, MatchData[] array){
		boolean[] isMarked = new boolean[database.getMotionList().length];
		ArrayList<MatchData> best = new ArrayList<MatchData>();
		for (MatchData match : array) {
			int start = match.motion.motionIndex;
			int end = start + match.cTime;
			if (isMarked(isMarked, start, end)) continue;
			best.add(match);
			if (best.size() >= n) break;
			
			for (int i = start; i < end; i++) {
				isMarked[i] = true;
			}
		}
		return best;
	}
	
	private boolean isMarked(boolean[] isMarked, int start, int end) {
		for (int i = start; i < end; i++) {
			if (isMarked[i]) return true;
		}
		return false;
	}
	
//	private boolean print = false;
	private ArrayList<MatchData> findMatch(ArrayList<Motion> motionList, double rotation) {
		ArrayList<MatchData> matchList = new ArrayList<MatchData>();
		double[] rotList = getRotationList(motionList);
		for (int i = 0; i < rotList.length; i++) {
//			print = (motionList.get(0).motionData.file.getName().startsWith("PC_W_RapidTurn_Jog")
//					     && (i == 260));
//			if (print) System.out.println("check : " + motionList.get(i));
			if (actionTypes[motionList.get(i).motionIndex] < 0) continue;
			int ct = chceckCompletionTime(rotList, i, rotation, 60);
//			if (print) System.exit(0);
			if (ct > 0) {
				matchList.add(new MatchData(motionList.get(i), ct));
			}
		}
		return matchList;
	}
	
	private int chceckCompletionTime(double[] rotList, int index, double rotation, int maxCheckTime) {
		double rSum = 0;
		for (int i = 0; i < maxCheckTime; i++) {
			int idx = index + i;
			if (idx >= rotList.length) break;
			rSum += rotList[idx];
			if ((Math.abs(rSum) > Math.abs(rotation) && (Math.signum(rSum) == Math.signum(rotation)))) {
				return i;
			}
		}
		return -1;
	}
	
	
	public static double[] getRotationList(ArrayList<Motion> motionList) {
		ArrayList<Pose2d> poseList = new ArrayList<Pose2d>();
		for (Motion m : motionList) {
			poseList.add(Pose2d.getPoseByPelvis(m));
//			poseList.add(Pose2d.getPose(m));
		}
//		boolean print = motionList.get(0).motionData.file.getName().startsWith("PC_W_RapidTurn_Jog");
//		boolean print = motionList.get(0).motionData.file.getName().startsWith("PC_W_RapidTurnStart_Jog");
		double[] rotList = new double[motionList.size()];
		for (int i = 0; i < poseList.size(); i++) {
			int prevIdx = Math.max(0, i - 1);
			Pose2d prev = poseList.get(prevIdx);
			Pose2d current = poseList.get(i);
			rotList[i] = MathUtil.directionalAngle(prev.direction, current.direction);
//			if (print) {
//				System.out.println("rot : " + motionList.get(i) + "\t" + rotList[i] + "\t\t" + Math.toDegrees(rotList[i]));
//			}
		}
//		if (print) System.exit(0);
		return rotList;
	}
	
	private static class MatchData implements Comparable<MatchData>{
		Motion motion;
		int cTime;
		double goal;
		public MatchData(Motion motion, int cTime) {
			this.motion = motion;
			this.cTime = cTime;
		}
		@Override
		public int compareTo(MatchData o) {
			return Integer.compare(cTime, o.cTime);
		}
	}
	
	public static void main(String[] args) {
		String folder = "runjogwalk_withstop";
		String tPoseFile = "t_pose_ue.bvh";
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		label = "jog";
		TransitionData tData = new TransitionData(database, new String[] { label }, 1);
		BaseCriticalTimeMeasure measure = new BaseCriticalTimeMeasure(tData);
		measure.measure();
	}
}
