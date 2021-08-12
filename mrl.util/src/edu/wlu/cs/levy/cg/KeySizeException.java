package edu.wlu.cs.levy.cg;

/**
 * KeySizeException is thrown when a KDTree method is invoked on a key whose
 * size (array length) mismatches the one used in the that KDTree's constructor.
 * 
 * @author Simon Levy
 * @version %I%, %G%
 * @since JDK1.2
 */

public class KeySizeException extends KDException {

	protected KeySizeException() {
		super("Key size mismatch");
	}

	// arbitrary; every serializable class has to have one of these
	public static final long serialVersionUID = 2L;

}
