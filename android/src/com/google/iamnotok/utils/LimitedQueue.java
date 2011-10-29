package com.google.iamnotok.utils;

import java.util.LinkedList;


/**
 * This queue has a threshold and only saves the last [threshold] items in it.
 * 
 * @param <E> The type of the elements in the queue.
 */
public class LimitedQueue<E> extends LinkedList<E> {
	private static final long serialVersionUID = -3865727704298539447L;
	
	private int maxSize = 0;
	
	/**
	 * Constructor.
	 * 
	 * @param maxSize The threshold of this queue.
	 */
	public LimitedQueue(int maxSize){
		this.maxSize = maxSize;
	}

	@Override
	public boolean offer(E item){
		if (size() == maxSize){
			remove();
		}
		
		return super.offer(item);
	}
	
}
