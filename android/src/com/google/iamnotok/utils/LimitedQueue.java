package com.google.iamnotok.utils;

import java.util.LinkedList;


public class LimitedQueue<E> extends LinkedList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3865727704298539447L;
	private int maxSize = 0;
	//private E first Item = null;
	
	public LimitedQueue(int maxSizeP){
		this.maxSize = maxSizeP;
	}

	public boolean offer(E item){
		if (this.size() == maxSize){
			this.remove();
		}
		
		return this.offer(item);
	}
	
}
