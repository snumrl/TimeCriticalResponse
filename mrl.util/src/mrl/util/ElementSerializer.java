package mrl.util;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface ElementSerializer<E> {

	public void save(ObjectOutputStream os, E element) throws Exception;
	public E load(ObjectInputStream oi) throws Exception;
	public Class<E> elementClass();
}
