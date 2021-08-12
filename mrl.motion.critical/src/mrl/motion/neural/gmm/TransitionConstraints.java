package mrl.motion.neural.gmm;

import java.util.ArrayList;
import java.util.Arrays;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.dp.TransitionData;
import mrl.motion.dp.TransitionData.TransitionNode;
import mrl.motion.neural.rl.PolicyLearning;
import mrl.util.Configuration;

public class TransitionConstraints {

	public TransitionData tData;
	private boolean[] isNearAction;
	private TransitionNode[] motionToNodeMap;
	private Motion[] mList;
	
	private int[] actionPrevMargins;
	
	public TransitionConstraints(TransitionData tData) {
		this.tData = tData;
		
		this.mList = tData.mList;
		this.isNearAction = tData.isNearAction;
		this.motionToNodeMap = tData.motionToNodeMap;
		
		actionPrevMargins = new int[tData.actionLabels.length];
		for (MotionAnnotation ann : tData.database.getEventAnnotations()) {
			int actionIdx = tData.actionIndex(ann.type);
			if (actionIdx < 0) continue;
			if (actionIdx < tData.continuousLabelSize) {
				actionPrevMargins[actionIdx] = Configuration.MOTION_TRANSITON_MARGIN;
			} else {
				int margin = ann.interactionFrame - ann.startFrame + Configuration.MOTION_TRANSITON_MARGIN;
				actionPrevMargins[actionIdx] = Math.max(actionPrevMargins[actionIdx], margin + 1);
			}
		}
		System.out.println("action prev margins :: " + Arrays.toString(actionPrevMargins));
	}
	
	public ArrayList<Integer> getActionFrames(int actionType) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < tData.motionActionTypes.length; i++) {
			if (tData.motionActionTypes[i] == actionType) {
				list.add(i);
			}
		}
		return list;
	}

	public boolean isValidTransition(int currentMIndex, int targetMIndex, int goalAction) {
		boolean isSequential = (targetMIndex == (currentMIndex + 1));
		if (!isSequential) {
			if (isNearAction[currentMIndex] || isNearAction[targetMIndex]) return false;
			if (motionToNodeMap[targetMIndex] == null || !motionToNodeMap[targetMIndex].isTransitionable) return false;
			
			int backwardLimit = TransitionData.NO_BACKWARD_TRANSITION_MARGIN;
			int nextMotionIndex = currentMIndex + 1;
			if (targetMIndex < nextMotionIndex && targetMIndex > nextMotionIndex - backwardLimit
					&& mList[targetMIndex].frameIndex >= backwardLimit) {
				return false;
			}
		}
		
		int currentAction = tData.motionNearActionTypes[currentMIndex];
		int targetAction = tData.motionNearActionTypes[targetMIndex];
		if (targetAction >= 0 && targetAction != currentAction && targetAction != goalAction) {
			return false;
		}
		
		if (!isSequential) {
			int tAfterIdx = tData.transitionAfterMIndex[targetMIndex];
			int tAfterAction = tData.motionNearActionTypes[tAfterIdx];
			if (tAfterAction >= 0 && tAfterAction != currentAction && tAfterAction != goalAction) {
				return false;
			}
		}
		/*
		if (PolicyLearning.useDP && goalAction >= tData.continuousLabelSize) {
			
		} else {
			if (remainTime - Configuration.MOTION_TRANSITON_MARGIN <= actionPrevMargins[goalAction]) {
				if (tAfterAction != goalAction) {
					return false;
				}
			}
		}
		*/
		
		return true;
	}
}
