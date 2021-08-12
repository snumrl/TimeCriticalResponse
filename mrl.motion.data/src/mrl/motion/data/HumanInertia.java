package mrl.motion.data;

import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

public class HumanInertia {
	
	private static HashMap<String, HumanInertia> map;
	private static HashSet<String> passSet = new HashSet<String>();
	static{
		map = new HashMap<String, HumanInertia>();
		map.put("Hips", new HumanInertia(11.800000, new Quat4d(0.000000, 0.000000, -0.207912, 0.978148), new Vector3d(20.740500, 24.804300, 26.547000)));
		map.put("Spine", new HumanInertia(2.400000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(4.472140, 21.794500, 29.240400)));
		map.put("Spine1", new HumanInertia(24.900000, new Quat4d(0.000000, 0.000000, -0.104528, 0.994522), new Vector3d(37.374700, 21.492500, 33.477200)));
		map.put("Spine2", new HumanInertia(0.000000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(0.000000, 0.000000, 0.000000)));
		map.put("Neck", new HumanInertia(1.100000, new Quat4d(0.000000, 0.000000, 0.190809, 0.981627), new Vector3d(8.090400, 13.211600, 11.441600)));
		map.put("Head", new HumanInertia(4.200000, new Quat4d(0.000000, 0.000000, -0.309017, 0.951057), new Vector3d(20.283700, 16.124500, 13.309500)));
		map.put("LeftShoulder", new HumanInertia(0.000000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(0.000000, 0.000000, 0.000000)));
		map.put("LeftArm", new HumanInertia(2.000000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(27.550000, 9.327380, 9.327380)));
		map.put("LeftForeArm", new HumanInertia(1.400000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(26.672600, 7.745970, 7.745970)));
		map.put("LeftHand", new HumanInertia(0.500000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(15.491900, 8.485280, 4.898980)));
		map.put("RightShoulder", new HumanInertia(0.000000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(0.000000, 0.000000, 0.000000)));
		map.put("RightArm", new HumanInertia(2.000000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(27.550000, 9.327380, 9.327380)));
		map.put("RightForeArm", new HumanInertia(1.400000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(26.672600, 7.745970, 7.745970)));
		map.put("RightHand", new HumanInertia(0.500000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(15.491900, 8.485280, 4.898980)));
		map.put("LeftUpLeg", new HumanInertia(9.800000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(41.786600, 16.635300, 16.635300)));
		map.put("LeftLeg", new HumanInertia(3.800000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(42.445000, 10.588000, 10.588000)));
		map.put("LeftFoot", new HumanInertia(1.000000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(6.000000, 22.181100, 7.745970)));
		map.put("LeftToe", new HumanInertia(0.000000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(0.000000, 0.000000, 0.000000)));
		map.put("RightUpLeg", new HumanInertia(9.800000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(41.786600, 16.635300, 16.635300)));
		map.put("RightLeg", new HumanInertia(3.800000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(42.445000, 10.588000, 10.588000)));
		map.put("RightFoot", new HumanInertia(1.000000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(6.000000, 22.181100, 7.745970)));
		map.put("RightToe", new HumanInertia(0.000000, new Quat4d(0.000000, 0.000000, 0.000000, 1.000000), new Vector3d(0.000000, 0.000000, 0.000000)));
		
		
		passSet.add("LeftShoulder");
		passSet.add("RightShoulder");
		passSet.add("LeftUpLeg");
		passSet.add("RightUpLeg");
		passSet.add("Spine1");
	}
	
	public static HumanInertia get(String joint){
		return map.get(joint);
	}
	
	public static boolean isPass(String joint){
		return passSet.contains(joint);
	}
	

	public double mass;
	public Quat4d rotation;
	public Vector3d inertiaSize;
	
	public HumanInertia(double mass, Quat4d rotation, Vector3d inertiaSize) {
		this.mass = mass;
		this.rotation = rotation;
		this.inertiaSize = inertiaSize;
	}
	
	
	
}
