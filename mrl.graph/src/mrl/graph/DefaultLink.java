package mrl.graph;

public class DefaultLink<N extends DefaultNode> {
	protected N source;
	protected N target;
	protected double distance = 1;
	
	public DefaultLink(N source, N target) {
		this.source = source;
		this.target = target;
	}

	public N source() {
		return source;
	}

	public N target() {
		return target;
	}
	
	public void changeSource(N source){
		this.source = source;
	}
	
	public void changeTarget(N target){
		this.target = target;
	}
	
	public void changeDistance(double distance){
		this.distance = distance;
	}

	public double distance() {
		return distance;
	}
}
