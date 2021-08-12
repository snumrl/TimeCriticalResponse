package mrl.motion.data;

import java.io.Serializable;

public class Contact implements Serializable{

	private static final long serialVersionUID = 5652913592300355761L;
	
	public boolean left;
	public boolean right;
	
	public Contact(boolean left, boolean right) {
		this.left = left;
		this.right = right;
	}
	
	public boolean isNoContact(){
		return !left && !right;
	}

	@Override
	public int hashCode(){
		int code = 1;
		if (left) code += 2;
		if (right) code += 4;
		return code;
	}
	
	@Override
	public boolean equals(Object obj){
		if (obj instanceof Contact){
			Contact c = (Contact)obj;
			return left == c.left && right == c.right;
		}
		return false;
	}
	
	public String toString(){
		return "c[" + left + "," + right + "]";
	}
}
