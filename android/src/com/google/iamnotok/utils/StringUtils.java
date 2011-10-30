package com.google.iamnotok.utils;

import java.util.Collection;

public class StringUtils {
	public static <T> String join(Collection<T> collection, String separator) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for(T x : collection) {
			if (!first) {
				builder.append(separator);
				first = false;
			}
			builder.append(x);
		}
		return builder.toString();
	}
}
