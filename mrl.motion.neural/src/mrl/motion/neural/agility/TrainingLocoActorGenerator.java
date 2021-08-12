package mrl.motion.neural.agility;

import java.io.File;
import java.util.ArrayList;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionData;
import mrl.motion.data.parser.BVHWriter;
import mrl.motion.dp.TransitionData;
import mrl.motion.neural.agility.match.MMatching;
import mrl.motion.neural.data.MotionDataConverter;
import mrl.motion.neural.data.RNNDataGenerator;
import mrl.util.Configuration;

public class TrainingLocoActorGenerator {
	
	public static void main(String[] args) {
		String folder;
		AgilityModel model;
		int dataGenSize;
		
		dataGenSize = 300;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		
		String tPoseFile = "t_pose_actor.bvh";
		folder = "loco_actor";
		
		AgilityModel.GOAL_TIME_LIMIT = 60; 
		AgilityModel.TIME_EXTENSION_RATIO = 0.1;
		MMatching.TIMING_EARLY_FINISH_WEIGHT = 0.5;
		
		TransitionData.STRAIGHT_MARGIN = -1;
		MotionDataConverter.useMatrixForAll = true;
		model = new LocoActorModel();
		
		MDatabase database = TrainingDataGenerator.loadDatabase(folder, tPoseFile);
		MotionMatchingSampling3 sampling = new MotionMatchingSampling3(database, model);
//		GoalBasedSampling sampling = new GoalBasedSampling(graph, model);
		ArrayList<Motion> mList = sampling.sample(dataGenSize);
		
		int PREV_ACTION_FRAME = 30;
		int POST_ACTION_FRAME = 30;
		MotionAnnotation[] motionToActionAnn = new MotionAnnotation[database.getMotionList().length];
		for (MotionAnnotation ann : database.getEventAnnotations()) {
			Motion interaction = database.findMotion(ann.file, ann.interactionFrame);
			motionToActionAnn[interaction.motionIndex] = ann;
		}
		MotionAnnotation[] motionToTransitionAnn = new MotionAnnotation[database.getMotionList().length];
		for (MotionAnnotation ann : database.getTransitionAnnotations()) {
			Motion start = database.findMotion(ann.file, ann.startFrame);
			Motion end = database.findMotion(ann.file, ann.endFrame);
			Motion interaction = null;
			
			if (ann.type == null || ann.type.equals("")) {
				for (int i = start.motionIndex; i < end.motionIndex; i++) {
					if (motionToActionAnn[i] != null && motionToActionAnn[i].type != null && !motionToActionAnn[i].type.equals("")) {
						ann.type = motionToActionAnn[i].type;
						interaction = database.findMotion(motionToActionAnn[i].file, motionToActionAnn[i].interactionFrame);
						break;						
					}
				}
			}

			for (int i = start.motionIndex; i < end.motionIndex; i++) {
				motionToTransitionAnn[i] = ann;
			}
			for (int i = interaction.motionIndex; i < interaction.motionIndex + POST_ACTION_FRAME; i++)	{
				//motionToTransitionAnn[i];
			}
		}
		
		for (int i = 0; i < mList.size(); i++) {
			Motion m = mList.get(i);
			MotionAnnotation transitionAnn = motionToTransitionAnn[m.motionIndex];			
			System.out.println(i + "\t" + m + "\t" + transitionAnn.type);			
		}
		
		mList.get(0).knot = Double.NaN;
		BVHWriter bw = new BVHWriter(new File(Configuration.BASE_MOTION_FILE));
		bw.write(new File("augmented.bvh"), new MotionData(mList));
		
//		String label = folder + "_test";
//		RNNDataGenerator.prepareTrainingFolder(label);
//		RNNDataGenerator.generate(mList, new AgilityControlParameterGenerator(model, sampling.controlParameters));
	}
}
