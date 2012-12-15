package com.google.iamnotok;

import android.content.Context;

public interface ContactLookup {

	/**
	 * @param id
	 *            The id of the contact to look for.
	 * @return the contact with the given id, or null if no such contact exists.
	 */
	public Contact lookup(String id);
	
}
