package mrl.motion.neural.rl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionActionDP;
import mrl.motion.dp.TransitionData;
import mrl.motion.dp.TransitionData.TransitionNode;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.gmm.GMMConfig;
import mrl.motion.neural.gmm.GMMStuntLocoConfig;
import mrl.motion.neural.rl.MFeatureMatching.MatchResult;
import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.motion.neural.rl.PolicyLearning.GMMQueryLog;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.ListViewerModule;
import mrl.widget.app.ListViewerModule.ListListener;
import mrl.widget.app.Module;

public class PolicyLearningTestModule extends Module{
	
	PolicyLearning learning;
	
	void testDP() {
		TransitionData tData = learning.config.tData;
		TransitionActionDP dp = learning.config.actionDP;
		Motion m = tData.database.findMotion("s_005_3_1:156");
//		before transition :: 53119.90603000798 : GMMStuntOnlyGoal	jbs	5	69	-0.31769	 : rTime=22 : s_005_3_1:156
		TransitionNode tNode = tData.motionToNodeMap[m.motionIndex];
		double d = dp.ctdTable[5][22][tNode.index];
		
		int minTime = -1;
		double mmdd = Integer.MAX_VALUE;
		for (int i = 0; i <= 22; i++) {
			double dd = dp.distanceTable[5][i][tNode.index];
			if (dd < mmdd) {
				mmdd = dd;
				minTime = i;
			}
		}
		System.out.println("min time d : " + minTime + " : " + mmdd);
		
		
		
		double minD = Integer.MAX_VALUE;
		Motion minM = null;
		double[] ttttttt = null;
		for (TransitionNode node : tData.tNodeList) {
			double td = tData.transitionDistance(m.motionIndex, node.motionIndex());
			double d2 = dp.distanceTable[5][22 - 1][node.index];
			double ddd = td + d2;
//			if (ddd < Integer.MAX_VALUE) {
//				System.out.println("pp : " + node.motion + " : " + ddd + " : " + td + " : " + d2);
//			}
			if (ddd < minD) {
				minD = ddd;
				minM = node.motion;
				ttttttt = new double[] { td, d2 };
			}
		}
	
		System.out.println("d : " + d);
		System.out.println(minD + " : " + minM);
		System.out.println(Arrays.toString(ttttttt));
		System.exit(0);
	}

	@Override
	protected void initializeImpl() {
//		GMMConfig config = PolicyLearning.jump();
//		GMMStuntLocoConfig.LOCOMOTION_RATIO = 0.5;
//		GMMConfig config = PolicyLearning.jump();
//		AgilityModel.TIME_EXTENSION_RATIO = 0.25;
//		GMMConfig.TIME_RATIO = 1.25;
//		config.name += "_t25";
		GMMConfig config = PolicyLearning.jump(60, "_short05", false);
//		GMMConfig config = PolicyLearning.jump(40, "");
		
//		GMMConfig config = PolicyLearning.stunt_only(false, "_short07");
//		GMMConfig.TIME_RATIO = 0.5;
		
//		GMMConfig config = PolicyLearning.stunt_loco();
//		GMMStuntLocoConfig.LOCOMOTION_RATIO = 0.8;
//		GMMConfig config = PolicyLearning.loco_action();
//		config.name += " - 25086";
		learning = new PolicyLearning(config, true);
		
//		testDP();
		
//		MDatabase db = learning.tData.database;
//		Motion m1 = db.findMotion("s_009_2_2:176");
//		
//		double[] dList = learning.tData.transitionDistance(m1.motionIndex);
//		double minDist = Integer.MAX_VALUE;
//		int minIndex = -1;
//		double target = 745220.1004665829;
//		for (int i = 0; i < dList.length; i++) {
//			double diff = target - dList[i];
//			diff = diff*diff;
//			if (diff < minDist) {
//				minDist = diff;
//				minIndex = i;
//			}
//		}
//		System.out.println("min dist :: " + minDist + " : " + dList[minIndex] + " : " + learning.tData.tNodeList[minIndex].motion);
//		
//		Motion m2 = db.findMotion("s_020_1_2:505");
//		System.out.println("d1 : " + learning.tData.transitionDistance(m1.motionIndex, m2.motionIndex));
//		System.out.println("d1 : " + learning.tData.transitionDistance(m1.motionIndex, m2.motionIndex+1));
//		System.out.println("d1 : " + learning.tData.transitionDistance(m1.motionIndex, m2.motionIndex-1));
//		System.out.println("d1 : " + learning.tData.transitionDistance(m1.motionIndex, m2.motionIndex));
////		System.out.println("d1 : " + learning.tData.database.getDist().getDistance(m1.motionIndex, m2.motionIndex));
////		System.out.println("d1 : " + learning.tData.database.getDist().getDistance(m1.motionIndex, m2.motionIndex+1));
////		System.out.println("d1 : " + learning.tData.database.getDist().getDistance(m1.motionIndex, m2.motionIndex-1));
////		System.out.println("d1 : " + learning.tData.database.getDist().getDistance(m1.motionIndex, m2.motionIndex));
//		System.exit(0);
		
		
		ArrayList<Object> result = learning.testEpisodes(1000);
		getModule(MainViewerModule.class);
		
//		getModule(ListViewerModule.class).setItems(learning.queryLogList, new ListListener<GMMQueryLog>() {
//			@Override
//			public String[] getColumnHeaders() {
//				return Utils.toStringArrays("Frame", "actionType", "Motion", "Selected", "byDP", "minDist", "dpWeight", "featureDist", "dAction", "dAction_dp", "transitionDist", "actionDist1", "actionDist2", "dp actionDist1", "dp actionDist2", "remainTime");
//			}
//			@Override
//			public String[] getTableValues(GMMQueryLog item) {
//				Motion motion = learning.matching.database.getMotionList()[item.originFeature.motionIndex];
//				Motion selected = learning.matching.database.getMotionList()[item.selectedFeature.motionIndex];
//				Motion byDP = learning.matching.database.getMotionList()[item.byDPFeature.motionIndex];
//				MatchResult m = item.matchResult;
//				MatchResult dp = item.dpMatchResult;
//				return Utils.toStringArrays(String.valueOf(item.frame), item.goal.actionName(), motion.toString(), selected.toString(), byDP.toString(), m.minDist, item.dpWeight, m.featureDist, 
//						m.dAction, dp.dAction, m.transitionDist, 
//						m.actionDist1, m.actionDist2, dp.actionDist1, dp.actionDist2, item.remainTime);
//			}
//			@Override
//			public void onItemSelection(GMMQueryLog item) {
//				getModule(ItemListModule.class).addSingleItem("originFeature", reconstructFeature(item.originFeature), new ItemDescription(new Vector3d(0, 1, 0)));
//				getModule(ItemListModule.class).addSingleItem("queryFeature", reconstructFeature(item.queryFeature), new ItemDescription(new Vector3d(1, 0, 0)));
//				getModule(ItemListModule.class).addSingleItem("selectedFeature", reconstructFeature(item.selectedFeature), new ItemDescription(new Vector3d(0, 0, 1)));
//				getModule(ItemListModule.class).addSingleItem("byQueryFeature", reconstructFeature(item.byQueryFeature), new ItemDescription(new Vector3d(1, 1, 0)));
//			}
//		});
		
		
		for (int i = 0; i < result.size(); i++) {
			if (i == 1) {
				getModule(ItemListModule.class).addSingleItem("result_" + i, result.get(i), new ItemDescription(new Vector3d(0, 1, 0), 10));
			} else {
				getModule(ItemListModule.class).addSingleItem("result_" + i, result.get(i));
			}
		}
		
//		getModule(ItemListModule.class).addSingleItem("base", Pose2d.BASE, new ItemDescription(new Vector3d(1, 0, 0), 10));
	}
	
	private ArrayList<Object> reconstructFeature(MotionFeature feature) {
		ArrayList<Object> list = new ArrayList<Object>();
		if (feature.motionIndex >= 0) {
			Motion m = learning.matching.database.getMotionList()[feature.motionIndex];
			m = PositionMotion.getAlignedMotion(m);
			list.add(new MotionData(Utils.singleList(m)));
		}
//		double[] data = feature.data;
		double[] data = learning.matching.normal.deNormalizeY(feature.data);
		int idx = 0;
		int fpCount = 0;
		for (int offset : MFeatureMatching.futurePoseIndices) {
			idx++;
//			features.add(new double[] { MathUtil.getTranslation(m.root()).y });
			for (String joint : MFeatureMatching.futureJointForPosition) {
				Point3d p = new Point3d(data[idx], data[idx+1], data[idx+2]);
				idx += 3;
//				if (fpCount == 0) {
					list.add(p);
//				}
			}
			fpCount++;
		}
		for (int offset : MFeatureMatching.futureTrajectoryIndices) {
			Pose2d p = new Pose2d(data[idx], data[idx+1], data[idx+2], data[idx+3]);
			idx += 4;
			list.add(p);
		}
		return list;
	}
	
	
	
	public static void main(String[] args) {
		runThisModule();
	}
}
