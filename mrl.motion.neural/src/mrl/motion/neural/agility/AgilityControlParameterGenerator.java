package mrl.motion.neural.agility;

import java.util.ArrayList;

import mrl.motion.neural.agility.MotionMatchingSampling.MMControlParameter;
import mrl.motion.neural.data.ControlDataGenerator;
import mrl.util.MathUtil;

public class AgilityControlParameterGenerator extends ControlDataGenerator{
	
	public static boolean ADD_TIMING_PARAMETER = false;


	protected AgilityModel model;
	private int paramIndex = 0;
	private ArrayList<MMControlParameter> controlParameters;
	protected double activation;
	
	public AgilityControlParameterGenerator(AgilityModel model, ArrayList<MMControlParameter> controlParameters) {
		this.model = model;
		this.controlParameters = controlParameters;
	}
	
	@Override
	public double[] getControl(int index) {
		if (paramIndex >= controlParameters.size()) return null;
		
		MMControlParameter cp = controlParameters.get(paramIndex);
		while (index >= cp.frame) {
			paramIndex++;
			cp = controlParameters.get(paramIndex);
		}
		if (cp.frame >= mList.size() - 4) return null;
		
		double[] control = cp.goal.getControlParameter(mList, index, cp.frame);
		int remainTime = cp.frame - index;
		activation = cp.goal.getActivation(remainTime);
		if (ADD_TIMING_PARAMETER) {
			control = MathUtil.concatenate(control, new double[] { cp.goal.agility });
//			control = MathUtil.concatenate(control, new double[] { cp.goal.timeLimit });
		}
		return control;
	}
	
	public double[] getHasBall(int index){
		if (Double.isNaN(activation)) return null;
		return new double[] { activation };
	}
	
	public boolean[] getNormalMarking(){
		if (model.getActionSize() <= 0) return null;
		return getTrueList(model.getActionSize()+1);
	}
	
	/**
	 * Deprecated. Old version
	 */
	public static boolean _USE_ADDITIONAL_SIZE = false;
	
	public static double[] getActionType(int actionSize, int action){
		if (_USE_ADDITIONAL_SIZE) {
			double[] typeArray = new double[actionSize+1];
			typeArray[action+1] = 1;
			return typeArray;
		} else {
			double[] typeArray = new double[actionSize];
			typeArray[action] = 1;
			return typeArray;
		}
	}

}
