package com.google.iamnotok;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.iamnotok.utils.StringUtils;

/**
 * Maintains a list of contact ids. Quite crude but should do the job for now.
 */
public class EmergencyContactsHelper {
	private static final String PREFS_NAME = "MyPrefsFile";
	private static final String CONTACT_IDS_PROPERTY_NAME = "contact_ids";

	private Context context;

	public EmergencyContactsHelper(Context context) {
		this.context = context;
	}
	
	private Set<String> getIds() {
		return new LinkedHashSet<String>(Arrays.asList(prefs().getString(CONTACT_IDS_PROPERTY_NAME, "").split(",")));
	}

	public Collection<Contact> getAllContacts() {
		Collection<Contact> result = new ArrayList<Contact>();
		for (String id : getIds()) {
			Contact contact = lookupContact(id);
			if (contact != null)
				result.add(contact);
		}
		return result;
	}

	public Contact getContactWithId(String id) {
		return getIds().contains(id) ? lookupContact(id) : null;
	}
	
	private Contact lookupContact(String id) {
		Contact result = new Contact(context, id);
		if (!result.lookup())
			return null;
		return result;
	}

	public Contact getContactWithName(String contactName) {
		for (String id : getIds()) {
			Contact c = lookupContact(id);
			if (c != null && c.getName().equals(contactName))
				return c;
		}
		return null;
	}

	public boolean addContact(String id) {
		Set<String> updatedIds = getIds();
		if (!updatedIds.add(id)) // fail if already in set
			return false;
		if (lookupContact(id) == null)
			return false;
		return updateContactIdsInPrefs(updatedIds);
	}
	
	private boolean updateContactIdsInPrefs(Collection<String> ids) {
		String newValue = StringUtils.join(ids, ",");
		return prefs().edit().putString(CONTACT_IDS_PROPERTY_NAME, newValue).commit();
	}

	public boolean deleteContact(String id) {
		Set<String> updatedIds = getIds();
		if (!updatedIds.remove(id)) // fail if not in set
			return false;
		return updateContactIdsInPrefs(updatedIds);
	}

	private SharedPreferences prefs() {
		return context.getSharedPreferences(PREFS_NAME, 0);
	}

	private boolean hasContact(String id) {
		return getIds().contains(id) && lookupContact(id) != null;
	}
}
