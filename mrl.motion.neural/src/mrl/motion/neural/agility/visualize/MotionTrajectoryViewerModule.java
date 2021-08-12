package mrl.motion.neural.agility.visualize;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.edit.MotionEdit;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.TrainingDataGenerator;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.MathUtil;
import mrl.util.Utils;
import mrl.widget.ScalableCanvas2D;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.app.Module;

public class MotionTrajectoryViewerModule extends Module{
	
	Point2d targetPos = new Point2d(100.36473270469685, -122.43809938950699);
//	ArrayList<Motion> trajectory;
	int timeIterval = 5;
	int timeLimit = 45;

	@Override
	protected void initializeImpl() {
		
		for (int i = 0; i < 3; i++) {
			makeWindow(i);
		}
	}
	
	private void makeWindow(int type) {
		MDatabase database = TrainingDataGenerator.loadDatabase("dc_loco");
		MotionData mData = database.getMotionDataList()[database.findMotionDataIndex("PC_W_RapidTurn_Jog-002_C.bvh")];
		
		int last = 293;
		if (type == 1) last++;
		ArrayList<Motion> trajectory = Utils.cut(mData.motionList, 238, last);
		trajectory = Pose2d.getAlignedMotion(trajectory, 0);
		
		
		System.out.println("tLen : " + trajectory.size());
		System.out.println(Pose2d.getPose(Utils.last(trajectory)).position);
		
		if (type == 1) {
			// edit by time
			System.out.println("tp : " + Pose2d.getPose(Utils.last(trajectory)));
			MotionSegment segment = new MotionSegment(database.getMotionList(), trajectory.get(0).motionIndex, Utils.last(trajectory).motionIndex);
			System.out.println("ss : " + Pose2d.getPose(Utils.last(segment.getMotionList())));
			MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, new Pose2d(Double.NaN, Double.NaN, Double.NaN, Double.NaN), timeLimit+1);
			System.out.println("ep : " + Pose2d.getPose(Utils.last(edited.getMotionList())));
			ArrayList<Motion> eList = edited.getMotionList();
			System.out.println("ep : " + Utils.last(edited.getMotionList()).knot);
			for (int i = 0; i < eList.size(); i++) {
				trajectory.get(i).knot = eList.get(i).knot;
			}
			trajectory = MotionData.divideByKnot(edited.getMotionList());
			System.out.println("dlen : " + trajectory.size());
			System.out.println("tp : " + Pose2d.getPose(Utils.last(trajectory)));
			System.out.println("tlen : " +trajectory.size());
		}
		if (type == 2) {
			// edit by position
			System.out.println("tp : " + Pose2d.getPose(Utils.last(trajectory)));
			trajectory = Utils.cut(trajectory, 0, timeLimit);
			MotionSegment segment = new MotionSegment(database.getMotionList(), trajectory.get(0).motionIndex, Utils.last(trajectory).motionIndex);
			System.out.println("ss : " + Pose2d.getPose(Utils.last(segment.getMotionList())));
			MotionSegment edited = MotionEdit.getEditedSegment(segment, 0, segment.length()-1, new Pose2d(targetPos.x, targetPos.y, Double.NaN, Double.NaN), -1);
			System.out.println("ep : " + Pose2d.getPose(Utils.last(edited.getMotionList())));
			ArrayList<Motion> eList = edited.getMotionList();
			System.out.println("ep : " + Utils.last(edited.getMotionList()).knot);
			trajectory = eList;
//			for (int i = 0; i < eList.size(); i++) {
//				trajectory.get(i).knot = eList.get(i).knot;
//			}
//			trajectory = MotionData.divideByKnot(edited.getMotionList());
			System.out.println("dlen : " + trajectory.size());
			System.out.println("tp : " + Pose2d.getPose(Utils.last(trajectory)));
			System.out.println("tlen : " +trajectory.size());
		}
		
		getModule(MainViewerModule.class);
		getModule(ItemListModule.class).addSingleItem("motion", new MotionData(trajectory));
		
		WindowPosition.Right.weights = new int[] { 40, 60 };
		app().addWindow(new MotionTrajectoryViewer(dummyParent(), trajectory), "type: " + type, WindowPosition.Right);
	}

	
	private class MotionTrajectoryViewer extends ScalableCanvas2D{

		ArrayList<Motion> trajectory;
		
		public MotionTrajectoryViewer(Composite parent, ArrayList<Motion> trajectory) {
			super(parent);
			this.trajectory = trajectory;
		}

		@Override
		protected void drawContents(GC gc) {
			gc.setLineWidth(2);
			double s = 2;
			for (int i = 0; i < trajectory.size()-1; i++) {
				Point2d p1 = Pose2d.getPose(trajectory.get(i)).position;
				Point2d p2 = Pose2d.getPose(trajectory.get(i+1)).position;
				p1.scale(s);
				p2.scale(s);
				if (i >= timeLimit) {
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
				} else {
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
				}
				drawLine(gc, p1.x, p1.y, p2.x, p2.y);
				System.out.println("dLine : " + i);
			}
			
			Point2d tp = new Point2d(targetPos);
			tp.x -= 5;
			tp.y -= 5;
			tp.scale(s);
			for (int i = 0; i < trajectory.size(); i+=timeIterval) {
				Pose2d pose = Pose2d.getPose(trajectory.get(i));
				gc.setLineWidth(2);
				gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
				gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
				Vector2d v = new Vector2d(pose.position);
				if (i < 2) {
					v.sub(Pose2d.getPose(trajectory.get(i + 2)).position);
				} else {
					v.sub(Pose2d.getPose(trajectory.get(i - 2)).position);
				}
				v.normalize();
				v = MathUtil.rotate(v, Math.PI/2);
				
				v.scale(3);
				Point2d p = new Point2d(pose.position);
				Point2d p1 = new Point2d(pose.position);
				Point2d p2 = new Point2d(pose.position);
				p1.sub(v);
				p2.add(v);
				
				p.scale(s);
				p1.scale(s);
				p2.scale(s);
				drawLine(gc, p1.x, p1.y, p2.x, p2.y);
				System.out.println("dArrow " + i);
				
				if (i > 0 /*&& (i%2) == 1*/) {
					gc.setLineWidth(1);
					gc.setLineDash(new int[] { 6, 3 });
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_GREEN));
					drawArrow(gc, p.x, p.y, tp.x, tp.y, 0, 2, 8);
	//				drawLine(gc, p.x, p.y, tp.x, tp.y);
					gc.setLineDash(null);
				}
			}
			
			{
				Pose2d pose = Pose2d.getPose(trajectory.get(0));
				int color = SWT.COLOR_BLUE;
				gc.setForeground(getDisplay().getSystemColor(color));
				gc.setBackground(getDisplay().getSystemColor(color));
				Point2d p1 = new Point2d(pose.position);
				Vector2d p2 = new Vector2d(pose.direction);
				p2.scale(25);
				p2.add(p1);
				p1.scale(s);
				p2.scale(s);
				
				gc.setLineWidth(4);
				drawArrow(gc, p1.x, p1.y, p2.x, p2.y, 0, 2, 15);
				double radius = 5;
				fillOval(gc, p1.x-radius, p1.y-radius, radius*2, radius*2);
			}
			
			{
				gc.setLineWidth(2);
				int color = SWT.COLOR_MAGENTA;
				gc.setForeground(getDisplay().getSystemColor(color));
				gc.setBackground(getDisplay().getSystemColor(color));
				Point2d p1 = new Point2d(targetPos);
				p1.x -= 5;
				p1.y -= 5;
				p1.scale(s);
				double radius = 4;
				fillOval(gc, p1.x-radius, p1.y-radius, radius*2, radius*2);
				radius = 20;
				drawOval(gc, p1.x-radius, p1.y-radius, radius*2, radius*2);
			}
		}
		
		protected Rectangle getContentBoundary(){ 
			Rectangle rect = new Rectangle(0, MathUtil.round(targetPos.y*2), MathUtil.round(targetPos.x*2), Math.abs(MathUtil.round(targetPos.y*2)));
			rect.width += 150;
			int margin = 50;
			rect.x -= margin;
			rect.y -= margin;
			rect.width += margin*2;
			rect.height += margin*2;
			
			return rect;
//			return null;
		};
		
	}
	
	public static void main(String[] args) {
		MainApplication.run(new MotionTrajectoryViewerModule());
	}
}
