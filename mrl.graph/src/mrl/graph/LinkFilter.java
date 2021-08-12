package mrl.graph;

public interface LinkFilter<L extends DefaultLink<?>>{
	public boolean isValid(L link);
}
