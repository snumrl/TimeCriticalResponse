package mrl.motion.neural.dancecard;

import java.io.File;

import mrl.motion.data.Motion;
import mrl.motion.data.MotionData;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.data.parser.BVHWriter;

public class FootInversionFix {

	public static void main(String[] args) {
		String bvhFile = "output\\stunt_foot_fix_need\\motion\\trial1-35.bvh";
		MotionData mData = new BVHParser().parse(bvhFile);
		int[][] indices = {
				{0,	53,	69},
				{1,222,231},
				{1,	374,382},
				{0,	503,517},
				{1,	548,556},
				{0,	731,743},
				{0,	779,794},
				{1,	823,833	},
		};
		String[][] footJoints = {
				{	"RightFoot", "RightToe", "RightToe_End"},
				{	"LeftFoot", "LeftToe", "LeftToe_End"},
		};
		for (int[] data : indices) {
			Motion m1 = mData.motionList.get(data[1]);
			Motion m2 = mData.motionList.get(data[2]);
			
			for (int i = data[1]+1; i < data[2]; i++) {
				double r = (i - data[1])/(double)(data[2] - data[1]);
				Motion interpol = Motion.interpolateMotion(m1, m2, r);
				Motion change = mData.motionList.get(i);
				for (String j : footJoints[data[0]]) {
					if (change.get(j) == null) continue;
					change.get(j).set(interpol.get(j));
				}
			}
		}
		
		BVHWriter bw = new BVHWriter(new File(bvhFile));
		String output = "output\\stunt_foot_fix_need\\fixed-35.bvh";
		bw.write(new File(output), mData);
	}
}
