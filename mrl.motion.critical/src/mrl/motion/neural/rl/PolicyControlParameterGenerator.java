package mrl.motion.neural.rl;

import java.util.ArrayList;

import mrl.motion.neural.agility.MotionMatchingSampling.MMControlParameter;
import mrl.motion.neural.data.ControlDataGenerator;
import mrl.motion.neural.gmm.GMMConfig;
import mrl.motion.neural.rl.PolicyDataGeneration.PDGControlParameter;
import mrl.util.MathUtil;

public class PolicyControlParameterGenerator extends ControlDataGenerator{
	
	public static boolean ADD_TIMING_PARAMETER = false;
	public static int MARKING_OFFSET = 1;


	protected GMMConfig config;
	private int paramIndex = 0;
	private ArrayList<PDGControlParameter> controlParameters;
	protected double activation;
	
	public PolicyControlParameterGenerator(GMMConfig config, ArrayList<PDGControlParameter> controlParameters) {
		this.config = config;
		this.controlParameters = controlParameters;
	}
	
	@Override
	public double[] getControl(int index) {
		if (paramIndex >= controlParameters.size()) return null;
		
		PDGControlParameter cp = controlParameters.get(paramIndex);
		while (index >= cp.frame) {
			paramIndex++;
			cp = controlParameters.get(paramIndex);
		}
		if (cp.frame >= mList.size() - 4) return null;
		
		double[] control = cp.goal.getControlParameter(mList, index, cp.frame);
		int remainTime = cp.frame - index;
		if (PolicyDataGeneration.useDynamicAgility) {
			control = MathUtil.concatenate(control, new double[] { cp.agility });
		}
		activation = cp.goal.getActivation(remainTime);
//		if (ADD_TIMING_PARAMETER) {
//			control = MathUtil.concatenate(control, new double[] { cp.goal.agility });
////			control = MathUtil.concatenate(control, new double[] { cp.goal.timeLimit });
//		}
		return control;
	}
	
	public double[] getHasBall(int index){
		if (Double.isNaN(activation)) return null;
		return new double[] { activation };
	}
	
	public boolean[] getNormalMarking(){
		if (config.getActionSize() <= 0) return null;
		return getTrueList(config.getActionSize() + MARKING_OFFSET);
//		return getTrueList(config.getActionSize()+1); // 20210128
	}
}
