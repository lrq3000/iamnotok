package com.google.iamnotok;

import java.util.*;

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
	private ContactLookup lookupUtil;

	public EmergencyContactsHelper(Context context, ContactLookup lookupUtil) {
		this.context = context;
		this.lookupUtil = lookupUtil;
	}
	
	private List<String> getIds() {
		String idsCommaSeparatedString = getPrefs().getString(CONTACT_IDS_PROPERTY_NAME, "");
		if (null == idsCommaSeparatedString || "".equals(idsCommaSeparatedString)) {
			return new ArrayList<String>();
		}
		// here we'd like to return the contacts in the original order, by with no repeats
		String[] ids = idsCommaSeparatedString.split(",");
		List<String> result = new ArrayList<String>();
		for (String id : ids) {
			if (!"".equals(id) && !result.contains(id)) {
				result.add(id);
			}
		}
		return result;
	}

	public Collection<Contact> getAllContacts() {
		Collection<Contact> result = new ArrayList<Contact>();
		
		List<String> ids = getIds();
		for (String id : ids) {
			Contact contact = lookupContact(id);
			if (contact != null) {
				result.add(contact);
			}
		}
		return result;
	}

	public Contact getContactWithId(String id) {
		return getIds().contains(id) ? lookupContact(id) : null;
	}
	
	private Contact lookupContact(String id) {
		return lookupUtil.lookup(context, id);
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
		List<String> updatedIds = getIds();
		if (updatedIds.contains(id)) { // fail if already in list
			return false;
		}
		updatedIds.add(id);
		
		if (lookupContact(id) == null)
			return false;
		return updateContactIdsInPrefs(updatedIds);
	}
	
	private boolean updateContactIdsInPrefs(Collection<String> ids) {
		String newValue = StringUtils.join(ids, ",");
		return getPrefs().edit().putString(CONTACT_IDS_PROPERTY_NAME, newValue).commit();
	}

	public boolean deleteContact(String id) {
		List<String> updatedIds = getIds();
		if (!updatedIds.remove(id)) // fail if not in list
			return false;
		return updateContactIdsInPrefs(updatedIds);
	}

	private SharedPreferences getPrefs() {
		return context.getSharedPreferences(PREFS_NAME, 0);
	}

	private boolean hasContact(String id) {
		return getIds().contains(id) && lookupContact(id) != null;
	}
}
