package mrl.motion.neural.rl;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.rl.MFeatureMatching.MotionQuery;
import mrl.motion.neural.run.PythonRuntimeController;
import mrl.motion.neural.run.ue.MotionController;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.Module;

public class RuntimeMFeatureMatchingOpenGL extends Module{

	public static boolean useUnreal = true;
	
	private MFeatureRuntimeController c;
	private MotionController controller;
	private PolicyMatching selector;
	
	private long startTime = -1;
	private int frame;
	
	public RuntimeMFeatureMatchingOpenGL(MotionController controller, PolicyMatching selector, MFeatureRuntimeController c) {
		this.controller = controller;
		this.selector = selector;
		this.c = c;
	}
	

	@Override
	protected void initializeImpl() {
		getModule(MainViewerModule.class);
		app().addModule(controller);
		
		controller.setRuntimeController(c);
		System.out.println("Load Finished");
		
		startTime = System.currentTimeMillis();
		dummyParent().getDisplay().timerExec(1, new Runnable() {
			@Override
			public void run() {
				if (dummyParent().isDisposed()) return;
				while (true){
					int dt = (int)(System.currentTimeMillis() - startTime);
					int tIndex = dt/33;
					if (frame > tIndex) break;
					
					frame++;
					Motion motion = getMotion();
					getModule(ItemListModule.class).addSingleItem(PythonRuntimeController.MOTION_LABEL, new MotionData(Utils.singleList(motion)));
					getModule(MainViewerModule.class).setCameraCenter(MathUtil.getTranslation(motion.root()));
				}
				dummyParent().getDisplay().timerExec(1, this);
			}
		});
	}
	
	int lastMatchedFrame = -100;
	MotionSegment segment;
	
	public Motion getMotion() {
		int stepSize = Configuration.MOTION_TRANSITON_MARGIN;
		c.frame++;
		int fIndex = c.frame - lastMatchedFrame;
		if (fIndex > stepSize) {
			MotionQuery control = selector.getControl(c.currentFeature, controller.getControl());
			int mIndex = c.matching.findMatch(selector.learning.tData, c.currentMIndex, control, false).first;
			System.out.println("findMatch :: " + c.matching.database.getMotionList()[mIndex]);
			MotionSegment newSegment = new MotionSegment(c.matching.database.getMotionList(), mIndex, mIndex + stepSize);
			if (segment == null) {
				MotionSegment.alignToBase(newSegment);
				segment = newSegment;
			} else {
				int oSize = segment.length();
				segment = MotionSegment.stitch(segment, newSegment, true);
				segment = segment.cut(oSize, 0);
//					MotionSegment.align(segment, newSegment);
			}
			lastMatchedFrame = c.frame;
			fIndex = 0;
		}
		Motion m = segment.getMotionList().get(fIndex);
		System.out.println("f motion : "  + m);
		c.select(m.motionIndex);
		//c.currentFeature = c.matching.getOriginData(m.motionIndex + 1, false);
		c.g.motionList = Utils.singleList(m);
		c.g.pose = Pose2d.getPose(m);
		return m;
	}

}
