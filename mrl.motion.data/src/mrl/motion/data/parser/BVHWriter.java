package mrl.motion.data.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.SkeletonData.Joint;
import mrl.util.Configuration;
import mrl.util.MathUtil;

public class BVHWriter {
	
	public final static int ROTATION_XYZ = 0;
	public final static int ROTATION_ZXY = 1;
	public static int ROTATION_ORDER = ROTATION_ZXY;

	private File baseFile;
	private BVHParser parser = new BVHParser();
	public Double frameTime = null;
	
	public BVHWriter(){
		this(new File(Configuration.BASE_MOTION_FILE));
	}
	public BVHWriter(File baseFile){
		this.baseFile = baseFile;
		parser.parse(baseFile);
	}
	
	public void write(File file, MotionData motionData){
		try {
			int frames = motionData.motionList.size();
			boolean isKnotExist = !Double.isNaN(motionData.motionList.get(0).knot);
			if (isKnotExist){
				frames = (int)Math.ceil(motionData.motionList.get(motionData.motionList.size() - 1).knot) + 1;
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(baseFile)));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("Frames:")){
					line = "Frames:	" + frames;
				}
				if (line.startsWith("Frame Time:")){
					if (frameTime == null){
						bw.write(line + "\r\n");
					} else {
						bw.write("Frame Time:	" + String.format("%.7f", frameTime) + "\r\n");
					}
					break;
				}
				bw.write(line + "\r\n");
			}
			br.close();
			
			
			
			ArrayList<Joint> joints = parser.getJointList();
			for (int fIdx = 0; fIdx < frames; fIdx++) {
				Motion motion;
				if (isKnotExist){
					motion = motionData.motionList.get(MotionData.getFrameIndex(motionData.motionList, fIdx));
				} else {
					motion = motionData.motionList.get(fIdx);
				}
				
				
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < joints.size(); i++) {
					Matrix4d m = motion.get(joints.get(i).name);
					Vector3d t = MathUtil.getTranslation(m);
					if (i == 0){
						sb.append(String.format("%.5f %.5f %.5f", t.x, t.y, t.z));
					}
					
					if (ROTATION_ORDER == ROTATION_XYZ) {
						Vector3d r = MathUtil.matrixToEulerXYZ(m);
						sb.append(String.format(" %.5f %.5f %.5f", r.x, r.y, r.z));
					} else if (ROTATION_ORDER == ROTATION_ZXY) {
						Vector3d r = MathUtil.matrixToEulerZXY(m);
						sb.append(String.format(" %.5f %.5f %.5f", r.z, r.x, r.y));
					} else {
						throw new RuntimeException("invalid rot : " + ROTATION_ORDER);
					}
				}
				bw.write(sb.toString() + "\r\n");
			}
				
			bw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
