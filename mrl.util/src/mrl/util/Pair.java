package mrl.util;

import java.io.Serializable;

public class Pair<A,B> implements Serializable{
	
	private static final long serialVersionUID = -7220277296655836232L;
	
	public A first;
	public B second;
	
	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}
	
	public A getKey(){
		return first;
	}
	
	public B getValue(){
		return second;
	}
	
	public int hashCode(){
		int code = 0;
		if (first != null) code += first.hashCode();
		if (second != null) code += second.hashCode()*100000;
		return code;
	}
	
	public boolean equals(Object o){
		if (o instanceof Pair){
			@SuppressWarnings("unchecked")
			Pair<A,B> p = (Pair<A,B>)o;
			return Utils.objEqauls(first, p.first) && Utils.objEqauls(second, p.second);
		}
		return false;
	}
}
