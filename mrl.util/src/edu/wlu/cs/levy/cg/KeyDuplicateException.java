package edu.wlu.cs.levy.cg;

/**
 * KeyDuplicateException is thrown when the <TT>KDTree.insert</TT> method is
 * invoked on a key already in the KDTree.
 * 
 * @author Simon Levy
 * @version %I%, %G%
 * @since JDK1.2
 */

public class KeyDuplicateException extends KDException {

	protected KeyDuplicateException() {
		super("Key already in tree");
	}

	// arbitrary; every serializable class has to have one of these
	public static final long serialVersionUID = 1L;
}
