package mrl.motion.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import mrl.motion.data.SkeletonData.Joint;

public class SkeletonData extends HashMap<String, Joint>{
	
	private static final long serialVersionUID = 7147054068992956927L;
	public static boolean USE_SINGLE_SKELETON = true;
	
	private static Object sync = new Object();
	public static SkeletonData instance;
	public static String defaultRoot;
	public static String[] keyList;
	public static HashMap<String, Integer> keyMap;
	public ArrayList<Joint> jointListByFileOrder;
	

	public enum DOF { RX, RY, RZ, TX, TY, TZ };
	
	public Joint root;
	
	public SkeletonData(Joint root){
		this.root = root;
		this.putAll(root.getChildrenMap());
		
		double maxWeight = 0;
		double weightSum = 0;
		for (Joint j : this.values()){
			if (j == root) continue;
			j.weight = j.length;
			maxWeight = Math.max(j.weight, maxWeight);
			weightSum += j.weight;
		}
		root.weight = maxWeight;
		weightSum += maxWeight;
		for (Joint j : this.values()){
			j.weight /= weightSum;
		}
		
		if (USE_SINGLE_SKELETON){
			if (instance == null){
				synchronized (sync) {
					if (instance == null){
						registerSingleton();
					}
				}
			}
		} else {
			registerSingleton();
		}
	}
	
	private void registerSingleton(){
		defaultRoot = root.name;
		
		keyList = this.keySet().toArray(new String[this.size()]);
		Arrays.sort(keyList);
		keyMap = new HashMap<String, Integer>();
		for (int i = 0; i < keyList.length; i++) {
			keyMap.put(keyList[i], i);
		}
		instance = this;
	}
	
	public static class Joint{
		public String name;
		public Vector3d transition = new Vector3d();
		public double length = 0;
		public Vector3d axis = new Vector3d();
		public DOF[] dof = new DOF[0];
		
		public Joint parent;
		public ArrayList<Joint> children = new ArrayList<Joint>();
		
		private Matrix4d axisMatrix;
		private Matrix4d axisMatrixInverse;
		
		public boolean isEnd = false;
		
		public double weight = -1;
		
		public double limitMin;
		public double limitMax;
		
		public int distPointOffset = 0;
		
		public Joint(String name) {
			this.name = name;
		}
		
		public void addChild(Joint child){
			child.parent = this;
			children.add(child);
		}
		
		public HashMap<String, Joint> getChildrenMap(){
			HashMap<String, Joint> childrenMap = new HashMap<String, Joint>();
			addToChildrenMap(childrenMap);
			return childrenMap;
		}
		
		private void addToChildrenMap(HashMap<String, Joint> JointMap){
			JointMap.put(name, this);
			for (Joint child : children){
				child.addToChildrenMap(JointMap);
			}
		}
		
		private Matrix4d getRotateMatrix(Vector3d axis){
			Matrix4d m1 = new Matrix4d();
			m1.rotX(Math.toRadians(axis.x));
			Matrix4d m2 = new Matrix4d();
			m2.rotY(Math.toRadians(axis.y));
			Matrix4d m3 = new Matrix4d();
			m3.rotZ(Math.toRadians(axis.z));
			m3.mul(m2);
			m3.mul(m1);
			return m3;
		}
		
		public Matrix4d getAxisMatrix(){
			if (axisMatrix == null){
				axisMatrix = getRotateMatrix(axis); 
			}
			return axisMatrix;
		}
		
		public Matrix4d getAxisMatrixInverse(){
			if (axisMatrixInverse == null){
				axisMatrixInverse = new Matrix4d(getAxisMatrix());
				axisMatrixInverse.invert();
			}
			return axisMatrixInverse;
		}
		
	}
}
