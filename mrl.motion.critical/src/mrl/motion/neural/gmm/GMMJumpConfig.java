package mrl.motion.neural.gmm;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.trasf.Pose2d;
import mrl.motion.dp.TransitionData;
import mrl.motion.graph.MotionSegment;
import mrl.motion.neural.agility.AgilityModel;
import mrl.motion.neural.agility.RotationModel;
import mrl.motion.neural.rl.PolicyControlParameterGenerator;
import mrl.motion.neural.rl.PolicyEvaluation.MatchingPath;
import mrl.motion.position.PositionMotion;
import mrl.util.MathUtil;
import mrl.util.Matrix2d;
import mrl.util.Utils;

public class GMMJumpConfig extends GMMConfig{
	
	public static boolean USE_DYNAMIC_TIME = false;
	
	public static double LOCOMOTION_RATIO = 0.8;
	public static boolean FIX_LOCOMOTION_AFTER_ACTION = true;
	
	public static int LOCO_ACTION_SIZE = 1;
	
	public static double LOCO_TIME_OFFSET = 10;
	
	public static String[] actionTypes = {
			"jog",
			"jump_both",
			"jump_one",
			"jump_moving",
			"flip"
	};
	
	public static double[][] timeOffset = {
//			{ 6, 6, 20, 20 }, // jog
			{ 6, 6, 20, 20 }, // jog
			{ 17.000, 10.000, 218.649, 128.207 }, // jump_both
			{ 15.000, 14.000, 160.925, 96.277 }, // jump_one
			{ 12.500, 14.500, 161.906, 161.909 }, // jump_moving
			{ 21.500, 10.000, 211.103, 55.133 }, // flip
	};
	
	public static Point2d meanTargetPoint = new Point2d(100, 0);
	public static double POSITION_WEIGHT = 1d/100; // 50cm == 1 radian(60 degree)
	
	
	public static double NOISE_LENGTH = 50;
	public static boolean USE_STRAIGHT_SAMPLING = false;
	
	

	public GMMJumpConfig(String name) {
		super(name, actionTypes, actionTypes, LOCO_ACTION_SIZE);
		TransitionData.STRAIGHT_MARGIN = 3;
		PolicyControlParameterGenerator.MARKING_OFFSET = 0;
	}
	
	
	@Override
	public int getControlParameterSize() {
		return super.getControlParameterSize() + 1; // direction
	}
	
	@Override
	public GMMGoalGenerator makeGoalGenerator() {
		return new JumpGoalGenerator();
	}
	
	private class JumpGoalGenerator extends GMMGoalGenerator{
		
		private GMMJumpGoal lastGoal;
		private boolean sampleStraight = false;
		private Pose2d prevPose = new Pose2d(Pose2d.BASE);
		
		private int dynamicTime = -1;
		
		public JumpGoalGenerator() {
			lastGoal = new GMMJumpGoal(0, 20, RotationModel.sampleRotation(), new Point2d());
			sampleDynamicTime();
		}
		
		void sampleDynamicTime() {
			if (dynamicTime < 0 || (MathUtil.random.nextDouble() < 0.2)) {
				dynamicTime = 14 + MathUtil.random.nextInt(40 - 14);
			}
		}
		
		@Override
		public GMMGoal sampleRandomGoal(Pose2d currentPose, Motion currentMotion) {
			int actionType;
			double rotation;
			Point2d point = new Point2d();
			double lRatio = LOCOMOTION_RATIO;
			if (lastGoal.actionType > 0) lRatio = 0.5;
			if ((FIX_LOCOMOTION_AFTER_ACTION && (lastGoal.actionType > 0)) || MathUtil.random.nextDouble() < lRatio) {
				actionType = 0;
				rotation = RotationModel.sampleRotation();
//				if (MathUtil.random.nextDouble() < 0.33) {
//					rotation /= 20;
//				}
				if (lastGoal.actionType > 0) {
					rotation = MathUtil.directionalAngle(currentPose.direction, prevPose.direction);
					sampleStraight = false;
				} else if (USE_STRAIGHT_SAMPLING) {
					if (sampleStraight) {
						rotation /= 20;
					}
					sampleStraight = !sampleStraight;
					if (Math.abs(rotation) < Math.PI / 4) {
						sampleStraight = false;
					}
				}
			} else {
				rotation = 0;
				point = new Point2d(Pose2d.BASE.direction);
				if (lastGoal.actionType > 0) {
					point.set(currentPose.globalToLocal(prevPose.direction));
					rotation = MathUtil.directionalAngle(Pose2d.BASE.direction, point);
					currentPose = prevPose;
				}
				actionType = MathUtil.random.nextInt(actionTypes.length - 1) + 1;
				Vector2d delta = MathUtil.rotate(point, Math.PI/2);
				delta.scale(NOISE_LENGTH*Utils.rand1());
				double targetLen = timeOffset[actionType][2] + MathUtil.random.nextDouble()*NOISE_LENGTH + 30;
				targetLen += timeOffset[lastGoal.actionType][3] *(1 + 0.5*MathUtil.random.nextDouble());
				point.scale(targetLen*1.5);
				point.add(delta);
				
//				sampleStraight = true;
			}
			double tRatio = 1d/AgilityModel.TIME_EXTENSION_RATIO;
			double timeLength = 0;
			timeLength += 8; // base margin
			timeLength += timeOffset[lastGoal.actionType][1] * (lastGoal.isActiveAction() ? tRatio : 1);
			timeLength += timeOffset[actionType][0] * ((actionType > 0) ? tRatio : 1);
			
//			USE_DYNAMIC_TIME
			double dTimeRatio = TIME_RATIO;
			if (USE_DYNAMIC_TIME) dTimeRatio = dynamicTime/30d;
			if ((lastGoal.actionType == 0) && (actionType == 0)) {
				timeLength = 40;
//				timeLength += LOCO_TIME_OFFSET*2;
//				timeLength /= TIME_RATIO;
			} else if (actionType == 0) {
				timeLength = 40;
//				timeLength = 20 + 20*dTimeRatio;
//				timeLength += LOCO_TIME_OFFSET*2;
//				timeLength /= TIME_RATIO;
			} else if (lastGoal.actionType == 0) {
				timeLength = 30*dTimeRatio;
			}
			int adjustedTime = MathUtil.round(timeLength);
			lastGoal = new GMMJumpGoal(actionType, adjustedTime, rotation, point);
			if (USE_DYNAMIC_TIME) lastGoal.dynamicTime = dynamicTime;
			
//			System.out.println("random goal :: " + lastGoal);
			prevPose = currentPose;
			sampleDynamicTime();
			return lastGoal;
		}
	}
	
	HashMap<Integer, Double> directionOffsetMap = new HashMap<Integer, Double>();
	double getJumpDirectionOffset(Motion m) {
		m = tData.database.getMotionList()[m.motionIndex];
		Double offset = directionOffsetMap.get(m.motionIndex);
		if (offset == null) {
			for (MotionAnnotation ann : tData.database.getEventAnnotations()) {
				if (!m.motionData.file.getName().contains(ann.file)) continue;
				if (m.frameIndex >= ann.startFrame && m.frameIndex <= ann.endFrame) {
					Pose2d current = Pose2d.getPose(m);
					Pose2d start = Pose2d.getPose(m.motionData.motionList.get(ann.startFrame));
					Pose2d end = Pose2d.getPose(m.motionData.motionList.get(ann.endFrame));
					Vector2d dir = MathUtil.sub(end.position, start.position);
					dir = current.globalToLocal(dir);
					offset = MathUtil.directionalAngle(Pose2d.BASE.direction, dir);
					directionOffsetMap.put(m.motionIndex, offset);
				}
			}
		}
		return offset;
	}
//	static class 

	public class GMMJumpGoal extends GMMGoal{
		
		public Point2d position;

		public GMMJumpGoal(int actionType, int timeLimit, double direction, Point2d position) {
			super(actionType, timeLimit, direction);
			this.position = position;
		}
		
		@Override
		public double evaluateFinalError(MatchingPath path, double futureRotation, Pose2d futurePose, Motion finalMotion) {
			if (isActiveAction()) {
				Pose2d finalPose = path.getCurrentPose();
				finalPose = finalPose.localToGlobal(futurePose);
				
				double dTime = 0;
				if (futureRotation == 0) {
					dTime = Math.max(0, path.time - minSearchTime);
//					dTime = path.time - timeLimit;
					dTime *= 1d/10;
				}
				
				double directionOffset = getJumpDirectionOffset(finalMotion);
				double dDir = MathUtil.trimAngle(direction - (path.rotation + futureRotation + directionOffset));
				
				double dPos = finalPose.position.distance(position);
				dPos = dPos * POSITION_WEIGHT;
				dPos = 0;
				
				double error = dPos*dPos + dDir*dDir + dTime*dTime;
				return error*0.1;
			} else {
				double d = MathUtil.trimAngle(direction - (path.rotation + futureRotation));
				return d*d;
			}
		}
		
		@Override
		public boolean isDirectionControl() {
			return true;
		}
		
		
		public double[] getParameter(MatchingPath path) {
//			double[] dir =  new double[] { direction - path.rotation };
//			return MathUtil.concatenate(actionTypeArray(), dir);
			double[] control = actionTypeArray();
			double remain = 0;
			Point2d targetPoint = new Point2d();
			if (isActiveAction()) {
				targetPoint = path.getCurrentPose().globalToLocal(position);
				targetPoint.sub(meanTargetPoint);
				targetPoint.scale(1d/50);
			}
			if (isDirectionControl()) {
				remain = direction - path.rotation; 
			}
			control = MathUtil.concatenate(control, parameter(remain, targetPoint));
			return control;
		}
		
		public double[] getControlParameter(ArrayList<Motion> mList, int currentIndex, int targetIndex) {
			double rotSum = Double.NaN;
			double directionOffset = 0;
			Point2d targetPosition = new Point2d(Double.NaN, Double.NaN);
			if (isActiveAction()) {
				directionOffset = getJumpDirectionOffset(mList.get(targetIndex));
				Pose2d p = PositionMotion.getPose(mList.get(currentIndex));
				Pose2d pTarget = PositionMotion.getPose(mList.get(targetIndex));
				targetPosition = p.globalToLocal(pTarget.position);
			}
			
			if (!isDirectionControl()) {
				rotSum = Double.NaN;
			} else {
				rotSum = directionOffset;
				for (int i = currentIndex; i < targetIndex; i++) {
					Pose2d p = PositionMotion.getPose(mList.get(i));
					Pose2d next = PositionMotion.getPose(mList.get(i + 1));
					double angle = MathUtil.directionalAngle(p.direction, next.direction);
					rotSum += angle;
				}
			}
			double[] control = actionTypeArray();
			control = MathUtil.concatenate(control, parameter(rotSum, targetPosition));
			if (USE_DYNAMIC_TIME) {
				control = MathUtil.concatenate(control, new double[] { dynamicTime });
			}
			return control;
		}
		
		double[] parameter(double rot, Point2d p) {
			Vector2d v = MathUtil.rotate(Pose2d.BASE.direction, rot);
			return new double[] { v.x, v.y }; 
//			return new Pose2d(p, v).toArray();
		}
		
		public Pose2d getEditingConstraint(MotionSegment segment) {
			if (isActiveAction()) {
//				double directionOffset = getJumpDirectionOffset(segment.lastMotion());
//				Vector2d v = new Vector2d(Pose2d.BASE.direction);
//				v = MathUtil.rotate(v, direction - directionOffset);
//				Pose2d p = new Pose2d(position, v);
//				return p;
				return new Pose2d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
				
			} else if (!isDirectionControl()) {
				return new Pose2d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
			} else {
				Vector2d v = new Vector2d(Pose2d.BASE.direction);
				v = MathUtil.rotate(v, direction);
				Pose2d p = new Pose2d(new Point2d(Double.NaN, Double.NaN), v);
				return p;
			}
		}
		
		public Object getParameterObject(Matrix2d startTransform, Matrix2d currentTransform) {
			Pose2d p = Pose2d.byBase(startTransform);
			if (isActiveAction()) {
				p.direction = MathUtil.rotate(p.direction, direction);
				p.position = p.localToGlobal(position);
			} else {
				p.direction = MathUtil.rotate(p.direction, direction);
				p.position = new Point2d(currentTransform.getTranslation());
			}
			return p;
		}
	}
}
