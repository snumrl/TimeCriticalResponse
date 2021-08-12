package mrl.motion.neural.dp;

import java.util.ArrayList;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.FootSlipCleanup;
import mrl.motion.data.trasf.MotionTransform;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.LevelOfErrorGraph;
import mrl.motion.dp.MotionGraphDPbyTime;
import mrl.motion.graph.MGraph;
import mrl.motion.graph.MGraph.MGraphEdge;
import mrl.motion.graph.MGraph.MGraphNode;
import mrl.motion.graph.MGraphSearch.SearchSeed;
import mrl.motion.graph.MGraphSearch;
import mrl.motion.graph.MotionSegment;
import mrl.motion.position.PositionMotion;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.IterativeRunnable;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;
import mrl.widget.ValueSlider;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.ListViewerModule;
import mrl.widget.app.ListViewerModule.ListListener;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;
import mrl.widget.app.SliderModule;

public class GraphSearchTestModule extends Module{
	
	public static int DEFAULT_TIME = 40;
	
	private ValueSlider slider;
	private MGraph graph;
	private MotionGraphDPbyTime mGraphDP;

	private ArrayList<SearchSeed> candidates;
	private ArrayList<MotionAnnotation> annList;
	
	private MGraphSearch search;
	private Point2d targetPoint;
	
	@Override
	protected void initializeImpl() {
		AxisAngle4d a = new AxisAngle4d();
		a.set(new Vector3d(0, 1, 0),  Math.PI);
		MotionTransform.baseOrientation.set(a);
		
		MDatabase database = LevelOfErrorGraph.loadDatabase();
		graph = new MGraph(database);
		
		
		
		
		annList = new ArrayList<MotionAnnotation>();
		boolean[] isKick = new boolean[graph.getNodeList().length]; 
		boolean[] isPunch = new boolean[isKick.length]; 
		for (MotionAnnotation ann : database.getEventAnnotations()) {
//		for (MotionAnnotation ann : database.getTransitionAnnotations()) {
			if (ann.interactionFrame > 0) {
				annList.add(ann);
				int mIndex = database.findMotion(ann.file, ann.interactionFrame).motionIndex;
				MGraphNode node = graph.getNodeByMotion(mIndex);
				if (node == null) {
					System.out.println("ann :: " + ann);
					continue;
				}
				if (ann.type.equals("kick")) isKick[graph.getNodeByMotion(mIndex).index] = true;
				if (ann.type.equals("punch")) isPunch[graph.getNodeByMotion(mIndex).index] = true;
			}
		}
		mGraphDP = new MotionGraphDPbyTime(graph);
		mGraphDP.calc(isKick, DEFAULT_TIME);
		System.out.println("Finished");
		
		search = new MGraphSearch(graph) {
			@Override
			protected boolean isReachable(MGraphNode node, int transitionLimit, int remainTime) {
				if (transitionLimit >= 2) return true;
				if (mGraphDP.distanceList[node.index] > remainTime) return false;
				if (transitionLimit == 0) {
					if (mGraphDP.actionMatching[node.index] == null) return false;
				}
				return true;
			}
			@Override
			protected MGraphNode getFinalizableEnd(SearchSeed seed, int maxTime) {
				return mGraphDP.actionMatching[seed.current.index];
			}
		};
		
		getModule(MainViewerModule.class).getPickPoint();
		SliderModule sModule = getModule(SliderModule.class);
		sModule.setDefaultRange(10, 60);
		sModule.setSliderValues(new int[] { DEFAULT_TIME });
		slider = sModule.getSliderList().get(0);
		slider.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
			}
		});
		
		showAnnList();
		addMenu("&Menu", "Show Annotations\tCtrl+&E", SWT.MOD1 + 'E', new Runnable() {
			@Override
			public void run() {
				showAnnList();
			} 
		});
		addMenu("&Menu", "&Rotation\tCtrl+R", SWT.MOD1 + 'R', new Runnable() {
			@Override
			public void run() {
				Point3d p3d = getModule(MainViewerModule.class).getPickPoint();
				if (p3d == null) return;
				targetPoint = Pose2d.to2d(p3d);
				Pose2d control = new Pose2d(targetPoint, new Vector2d(targetPoint));
				getModule(ItemListModule.class).addSingleItem("control", control, new ItemDescription(new Vector3d(0, 1, 0)));
				for (SearchSeed c : candidates) {
					c.error = positionDiff(c.finalPose().position, targetPoint);
				}
				updateCandidates();
			} 
		});
		
		getModule(ItemListModule.class).addSingleItem("Base", Pose2d.BASE, new ItemDescription(new Vector3d(1, 0, 0)));
	}
	
	public static double positionDiff(Point2d p1, Point2d p2) {
		double lengthDiff = MathUtil.length(p1) - MathUtil.length(p2);
		double angleDiff = Math.toDegrees(MathUtil.directionalAngle(p1, p2));
		angleDiff *= 2;
		return lengthDiff*lengthDiff + angleDiff*angleDiff;
	}
	
	private void showAnnList() {
		ListViewerModule listMoudle = getModule(ListViewerModule.class);
		listMoudle.setItems(annList, new ListListener<MotionAnnotation>(){
			@Override
			public String[] getColumnHeaders() {
				return new String[] { "file", "interactionFrame", "type"};	
			}

			@Override
			public String[] getTableValues(MotionAnnotation item) {
				return Utils.toStringArrays(item.file, item.interactionFrame, item.type);
			}
			@Override
			public void onItemSelection(MotionAnnotation item) {
				System.out.println("selected :: " + item);
				int mIndex = graph.getDatabase().findMotion(item.file, item.interactionFrame).motionIndex;
				MGraphNode node = graph.getNodeByMotion(mIndex);
				if (node == null) {
					System.out.println("no node :: " + item);
					return;
				}
				int offset = item.type.equals("kick") ? 7 : 0;
				updateSelection(node, slider.getSelection(), offset);
			}
		});
	}
	
	private void updateSelection(MGraphNode node, int time, int offset) {
		candidates = search.expand(node, 3, time);
		updateCandidates();
	}
	
	
	private void updateCandidates() {
		ListViewerModule listMoudle = getModule(ListViewerModule.class);
		ListListener<SearchSeed> listener = new ListListener<SearchSeed>(){
			@Override
			public String[] getColumnHeaders() {
				return new String[] { "control", "Action", "totalTime"};	
			}

			@Override
			public String[] getTableValues(SearchSeed item) {
				return Utils.toStringArrays(item.error, item.end.motion, item.time);
			}
			@Override
			public void onItemSelection(SearchSeed item) {
				getModule(ItemListModule.class).addSingleItem("motion", toMotion(item));
				getModule(ItemListModule.class).addSingleItem("target pose", item.finalPose());
				getModule(MainViewerModule.class).replay();
			}
		};
		listMoudle.setItems(candidates, listener);
	}
	
	private MotionData toMotion(SearchSeed c) {
		int margin = 10;
		MotionSegment segment = MotionSegment.getPathMotion(graph.getDatabase().getMotionList(), c.getPath(), -margin);
		if (targetPoint != null) {
			Pose2d pose = new Pose2d(targetPoint, new Vector2d(Double.NaN, Double.NaN));
			segment = MotionEdit.getEditedSegment(segment, margin, segment.length() - 1 - margin, pose, -1);
		}
		FootSlipCleanup.clean(segment);
		return new MotionData(MotionSegment.alignToBase(segment.getMotionList(), margin));
	}
	
	
	
	public static void main(String[] args) {
		MainApplication.run(new GraphSearchTestModule());
	}

}
