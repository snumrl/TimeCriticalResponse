package mrl.motion.neural.gmm;

import java.util.ArrayList;

import mrl.motion.neural.rl.MFeatureMatching.MotionFeature;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;
import mrl.util.MeanCounter;
import mrl.util.Pair;
import mrl.util.TimeChecker;

public class AABBManager {
	
	public static int AABB_GROUP_SIZE = 4;
	public static int SUB_AABB_SIZE = 16;

	public ArrayList<MotionFeature> featureList;
	private ArrayList<AABBGroup> aabbGroups;

	public AABBManager(ArrayList<MotionFeature> featureList) {
		this.featureList = featureList;
		
		aabbGroups = new ArrayList<AABBGroup>();
		
		int curIndex = 0;
		AABBGroup group = null;
		AABB subAABB = null;
		while (curIndex < featureList.size()) {
			MotionFeature f = featureList.get(curIndex);
			curIndex++;
			if (f == null) continue;
			if (group == null) {
				group = new AABBGroup();
				aabbGroups.add(group);
			}
			if (subAABB == null) {
				subAABB = new AABB();
				group.subAABBs.add(subAABB);
			}
			subAABB.add(f);
			group.main.add(f);
			if (subAABB.featureList.size() == SUB_AABB_SIZE) {
				subAABB = null;
				if (group.subAABBs.size() == AABB_GROUP_SIZE) {
					group = null;
				}
			}
		}
		System.out.println("check aabb size #############################");
		System.out.println("aabbgroups :: " + aabbGroups.size());
		for (AABBGroup g : aabbGroups) {
			if (g.subAABBs.size() != 4) {
				System.out.println("invalid group size :: " + g.subAABBs.size() + " : " + aabbGroups.indexOf(g));
			}
			for (AABB a : g.subAABBs) {
				if (a.featureList.size() != 16) {
					System.out.println("invalid group size :: " + a.featureList.size() + " : " + g.subAABBs.indexOf(a));
					
				}
			}
		}
		System.out.println("#######################################################");
	}
	
	public void test(MotionQuery query, int mIndex) {
		for (AABBGroup group : aabbGroups) {
			for (MotionFeature f : group.main.featureList) {
				if (f.motionIndex == mIndex) {
					double d1 = group.main.distance(query.feature, Integer.MAX_VALUE);
					System.out.println("d1 : " + d1);
					for (AABB sub : group.subAABBs) {
						for (MotionFeature ff : sub.featureList) {
							if (ff.motionIndex == mIndex) {
								double d2 = sub.distance(query.feature, Integer.MAX_VALUE);
								System.out.println("d2 : " + d2);
								return;
							}
						}
					}
				}
			}
		}
	}
	
//	public static MeanCounter[] counter;
//	static {
//		counter = new MeanCounter[3];
//		for (int i = 0; i < counter.length; i++) {
//			counter[i] = new MeanCounter();
//		}
//	}
//	public static void printCounter() {
//		System.out.println("aabb check counter::");
//		for (int i = 0; i < counter.length; i++) {
//			System.out.println(i + " : " + counter[i]);
//		}
//	}
	public Pair<MotionFeature, Double> findMatch(MotionQuery query, ValidityChecker checker, int startingMIndex) {
		MotionFeature match = null;
		double minDist = Integer.MAX_VALUE;
		
//		TimeChecker.instance.state("findMatch:1");
		match = featureList.get(startingMIndex);
		if (match != null && checker.isValid(startingMIndex)) {
			minDist = match.distance(query.feature.data);
		}
		for (AABBGroup group : aabbGroups) {
			double md = group.main.distance(query.feature, minDist);
			if (md < 0) continue;
			for (AABB aabb : group.subAABBs) {
				double sd = aabb.distance(query.feature, minDist);
				if (sd < 0) continue;
				for (MotionFeature f : aabb.featureList) {
					double d = f.distance(query.feature.data);
					if (d < minDist) {
						if (checker.isValid(f.motionIndex)) {
							minDist = d;
							match = f;
						}
					}
				}
			}
		}
		
		return new Pair<MotionFeature, Double>(match, minDist);
	}
	
	public static class AABBGroup{
		AABB main = new AABB();
		ArrayList<AABB> subAABBs = new ArrayList<AABB>();
	}
	
	public interface ValidityChecker{
		public boolean isValid(int motionIndex);
	}
}
