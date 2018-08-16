package xandragon.util;

import java.util.ArrayList;

@SuppressWarnings("serial")
/** This class is an ArrayList that allows inputting arrays. */
public class AppendableArray<T> extends ArrayList<T> {
	
	public AppendableArray() {
		super();
	}
	
	
	public void add(T[] items) {
		for (T item : items) {
			super.add(item);
		}
	}
}
