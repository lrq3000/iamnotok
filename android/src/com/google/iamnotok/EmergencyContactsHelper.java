package com.google.iamnotok;

import java.util.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.iamnotok.utils.StringUtils;

/**
 * Maintains a list of contact ids. Quite crude but should do the job for now.
 */
public class EmergencyContactsHelper {
	private static final String PREFS_NAME = "MyPrefsFile";
	private static final String CONTACT_IDS_PROPERTY_NAME = "contact_ids";

	private Context context;
	private HashMap<String, Contact> contacts;

	public EmergencyContactsHelper(Context context) {
		this.context = context;
		populateContacts();
	}

	public Collection<String> contactIds() {
		return contacts.keySet();
	}

	private void populateContacts() {
		contacts = new LinkedHashMap<String, Contact>();
		SharedPreferences settings =  prefs();
		String list = settings.getString(CONTACT_IDS_PROPERTY_NAME, "");
		for (String contactId : list.split(",")) {
			Contact contact = new Contact(context, contactId);
			if (!contact.lookup()) continue;
			contacts.put(contactId, contact);
		}
	}

	public Collection<Contact> getAllContacts() {
		return contacts.values();
	}

	public Contact getContactWithId(String contactId) {
		return contacts.get(contactId);
	}

	public Contact getContactWithName(String contactName) {
		for (Contact contact : contacts.values())
			if (contact.getName().equals(contactName))
				return contact;
		return null;
	}

	public boolean addContact(String contactId) {
		if (hasContact(contactId)) return false;
		Contact contact = new Contact(context, contactId);
		if (!contact.lookup()) {
			Log.e("ContactsHelper", "Lookup failed for: " + contactId);
			return false;
		}
		contacts.put(contactId, contact);
		return updateContactIdsInPrefs();
	}
	
	private boolean updateContactIdsInPrefs() {
		String newValue = StringUtils.join(contacts.keySet(), ",");
		return prefs().edit().putString(CONTACT_IDS_PROPERTY_NAME, newValue).commit();
	}

	public boolean deleteContact(String contactId) {
		if (!hasContact(contactId)) return false;
		contacts.remove(contactId);
		return updateContactIdsInPrefs();
	}

	private SharedPreferences prefs() {
		return context.getSharedPreferences(PREFS_NAME, 0);
	}

	public boolean hasContact(String contactId) {
		return contacts.containsKey(contactId);
	}

}
