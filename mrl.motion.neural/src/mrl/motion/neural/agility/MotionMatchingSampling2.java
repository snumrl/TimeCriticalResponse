package mrl.motion.neural.agility;

import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel.AgilityGoal;
import mrl.motion.neural.agility.MotionMatchingSampling.MMControlParameter;
import mrl.motion.neural.agility.match.MMatching;
import mrl.motion.neural.agility.match.MMatching.MatchingPath;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.util.MathUtil;

public class MotionMatchingSampling2 {

	public static int UPDATE_DATA_MARGIN = 200;
	public static int UPDATE_DATA_REMAIN_MARGIN = 100;
	
	public MDatabase db;
	public AgilityModel model;
	public MMatching matching;
	public ArrayList<MMControlParameter> controlParameters = new ArrayList<MMControlParameter>();
	public Motion currentMotion = null;
	public MotionSegment totalSegment = null;
	
	protected int alreadyProcessedFrames = 0;
	
	public ArrayList<double[]> generatedXList;
	public ArrayList<double[]> generatedYList;
	public AgilityControlParameterGenerator pg;
	
	public MotionMatchingSampling2(MDatabase database, AgilityModel model) {
		this.db = database;
		this.model = model;
		matching = model.makeMMatching(database);
	}
	
	public void reset() {
		totalSegment = null;
		currentMotion = null;
		controlParameters.clear();
	}
	
	public boolean sampleSingle(AgilityGoal goal) {
		currentMotion = currentMotion();
		System.out.println("search best : " + goal);
		MatchingPath c = matching.searchBest(currentMotion.motionIndex, goal);
		
		MotionSegment segment = MotionSegment.getPathMotion(db.getMotionList(), c.getPath(), 0);
		int timeConstraint = Math.min(c.time, goal.timeLimit);
		
		MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, goal.getEditingConstraint(), timeConstraint);
		edited = new MotionSegment(edited, 1, edited.length()-1);
		totalSegment = MotionSegment.stitch(totalSegment, edited, true);
		
		int length = MathUtil.round(totalSegment.lastMotion().knot);
		System.out.println("result path ::: " + length + " : " + goal + " : " + c.current +  " : " + c.time + " / " + goal.timeLimit);
		controlParameters.add(new MMControlParameter(length, goal));
		currentMotion = c.current;
		
		
		int startIndex = alreadyProcessedFrames;
		int endIndex = alreadyProcessedFrames + UPDATE_DATA_MARGIN;
		if (length  < endIndex + UPDATE_DATA_REMAIN_MARGIN) {
			generatedXList = null;
			generatedYList = null;
			return false;
		}
		
		
		MotionSegment postProcessed = new MotionSegment(totalSegment);
//		System.out.println("Start knot :: " + totalSegment.firstMotion().knot + " : " + alreadyProcessedFrames + " : " + endIndex);
		FootSlipCleanup.clean(postProcessed);
		ArrayList<Motion> mList = MotionData.divideByKnot(postProcessed.getMotionList(), 0);
//		System.out.println("knot divided :: " + mList.size() + " : " + totalSegment.lastMotion().knot + " : " + length);
		
		ArrayList<double[]> dataList = MotionDataConverter.motionToData(mList, mList.get(0), false);
		pg = new AgilityControlParameterGenerator(model, controlParameters);
		pg.setData(mList, dataList);
		
		generatedXList = new ArrayList<double[]>();
		generatedYList = new ArrayList<double[]>();
		for (int index = startIndex; index < endIndex; index++) {
			double[] control = pg.getControl(index);
			if (control == null) {
				System.out.println("control null :: " + index + " : " + mList.size() + " : " + length + " : " + controlParameters.size());
				for (MMControlParameter cc : controlParameters) {
					System.out.println(cc.goal + " : " + cc.frame);
				}
				System.exit(0);
			}
			generatedXList.add(control);
			generatedYList.add(dataList.get(index + 1));
		}
		
		alreadyProcessedFrames = UPDATE_DATA_REMAIN_MARGIN;
		int remove = endIndex - alreadyProcessedFrames;
		ArrayList<Motion> totalList = totalSegment.getMotionList();
		int cutIndex = -1;
		for (int i = 0; i < totalList.size(); i++) {
			if (totalList.get(i).knot > remove) {
				cutIndex = i - 1;
//				System.out.println("cut : " + i + " : " + totalList.get(i-1).knot + " - " + totalList.get(i).knot + " : " + remove);
				break;
			}
		}
		totalSegment = new MotionSegment(totalSegment, cutIndex, totalSegment.length() - 1);
		totalSegment.moveKnot(-remove);
//		System.out.println("after removed knot : " + totalSegment.firstMotion().knot + " : " + totalSegment.lastMotion().knot);
		ArrayList<MMControlParameter> remainParameters = new ArrayList<MMControlParameter>();
		for (MMControlParameter cp : controlParameters) {
			cp.frame -= remove;
			if (cp.frame >= 0) remainParameters.add(cp);
		}
		controlParameters = remainParameters;
		return true;
	}
	
	public Motion currentMotion() {
		if (currentMotion == null) {
			currentMotion = matching.tData.mList[matching.pickRandomStartMotion()];
		}
		return currentMotion;
	}
	
}
