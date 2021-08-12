package mrl.motion.neural.run;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;

import org.eclipse.swt.SWT;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.SkeletonData;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.JumpModel;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.neural.agility.match.RotationMotionMatching;
import mrl.util.Configuration;
import mrl.util.Utils;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.graph.DrawGraph;
import mrl.widget.graph.DrawLink;
import mrl.widget.graph.DrawNode;
import mrl.widget.graph.GraphViewer;

public class GraphTestModule extends Module{
	
	private MDatabase database;

	@Override
	protected void initializeImpl() {
		MDatabase.loadEventAnnotations = true;
		Configuration.MGRAPH_EDGE_WEIGHT_LIMIT = 300;
		
		JumpModel.rotErrorRatio = 100;
		RotationMotionMatching.STRAIGHT_MARGIN = 3;
		AgilityModel.GOAL_TIME_LIMIT = 30;
		JumpModel.USE_STRAIGHT_SAMPLING = true;
		AgilityModel.TIME_EXTENSION_RATIO = 0.1;
		String folder = "runjogwalk";
		database = TrainingDataGenerator.loadDatabase(folder, "t_pose_actor.bvh");
		
		
		Motion[] s1 = readSequence("PC_W_Clockwise_BigCircle-002_C:244", "PC_W_Clockwise_BigCircle-002_C:249");
		Motion[] s2 = readSequence("PC_W_Straight_Acceleration_Deceleration-001_C:1926", "PC_W_Straight_Acceleration_Deceleration-001_C:1932");
		
//		String joint = "LeftArm";
		String joint = "RightArm";
		double pScale = 100;
		ArrayList<Point2d> pList1 = new ArrayList<Point2d>();
		for (int i = 0; i < s1.length; i++) {
			HashMap<String, Point3d> pMap = Motion.getPointData(SkeletonData.instance, s1[i]);
			Point3d p = pMap.get(joint);
			p.scale(pScale);
			System.out.println(i + " : " + p);
			pList1.add(Pose2d.to2d(p));
		}
		ArrayList<Point2d> pList2 = new ArrayList<Point2d>();
		for (int i = 0; i < s2.length; i++) {
			HashMap<String, Point3d> pMap = Motion.getPointData(SkeletonData.instance, s2[i]);
			Point3d p = pMap.get(joint);
			p.scale(pScale);
			System.out.println(i + " : " + p);
			pList2.add(Pose2d.to2d(p));
		}
		
		Vector2d move = new Vector2d();
		move.sub(Utils.last(pList1), pList2.get(0));
		move.add(new Vector2d(100, 200));
		for (Point2d p : pList2) {
			p.add(move);
		}
		
		Point2d sourceNext = Utils.last(pList1);
		pList1 = Utils.cut(pList1, 0, pList1.size()-2);
		pList2 = blend(pList1, sourceNext, pList2);
		
		DrawGraph g = new DrawGraph();
		for (int i = 0; i < pList1.size(); i++) {
			DrawNode n = new DrawNode();
			n.position = pList1.get(i);
			if (i > 0) {
				DrawLink link = new DrawLink();
				link.source = Utils.last(g.nodeList);
				link.target = n;
				g.linkList.add(link);
			}
			g.nodeList.add(n);
		}
		for (int i = 0; i < pList2.size(); i++) {
			DrawNode n = new DrawNode();
			n.color  = SWT.COLOR_BLUE;
			n.position = pList2.get(i);
			if (i > 0) {
				DrawLink link = new DrawLink();
				link.source = Utils.last(g.nodeList);
				link.target = n;
				g.linkList.add(link);
			}
			g.nodeList.add(n);
		}
		DrawNode n = new DrawNode();
		n.color = SWT.COLOR_BLACK;
		g.nodeList.add(n);
		g.initialize();
		n.position.set(sourceNext);
		
		for (int i = 0; i < pList1.size(); i++) {
			g.nodeList.get(i).position.set(pList1.get(i));
		}
		for (int i = 0; i < pList2.size(); i++) {
			g.nodeList.get(pList1.size() + i).position.set(pList2.get(i));
		}
		
		GraphViewer viewer = addWindow(new GraphViewer(dummyParent()), WindowPosition.Main);
		viewer.setGraph(g);
//		g.nodeList.add(e)
	}
	
	private ArrayList<Point2d> blend(ArrayList<Point2d> source, Point2d sourceNext, ArrayList<Point2d> target){
		ArrayList<Point2d> result = new ArrayList<Point2d>();
		int BLEND_MARGIN = 4;
		for (int i = 0; i < BLEND_MARGIN; i++) {
			double r = (i+1)/(double)(BLEND_MARGIN + 1);
			Point2d p = new Point2d();
			p.interpolate(sourceNext, target.get(i), r);
			result.add(p);
		}
		for (int i = BLEND_MARGIN; i < target.size(); i++) {
			result.add(target.get(i));
		}
		return result;
	}
	
	private Motion[] readSequence(String start, String end) {
		Motion m1 = findMotion(start);
		Motion m2 = findMotion(end);
		ArrayList<Motion> list = new ArrayList<Motion>();
		for (int i = m1.motionIndex; i <= m2.motionIndex; i++) {
			Motion m = Pose2d.getAlignedMotion(database.getMotionList()[i]);
			list.add(m);
		}
		return Utils.toArray(list);
	}
	
	private Motion findMotion(String mName) {
		int idx = mName.indexOf(":");
		String file = mName.substring(0, idx) + ".bvh";
		int frame = Integer.parseInt(mName.substring(idx+1, mName.length()));
		return database.findMotion(file, frame);
	}
	
	public static void main(String[] args) {
		MainApplication.run(new GraphTestModule());
	}

}
