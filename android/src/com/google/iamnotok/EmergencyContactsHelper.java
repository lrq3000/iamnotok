package com.google.iamnotok;

import java.util.Collection;

import android.content.Context;
import android.util.Log;

/**
 * Maintains a list of contacts
 */
public class EmergencyContactsHelper {
	
	private static final String LOG = "EmergencyContactsHelper";

	private Context context;
	private ContactLookup lookupUtil;
	private Database database;

	public EmergencyContactsHelper(Context context, ContactLookup lookupUtil, Database database) {
		this.context = context;
		this.lookupUtil = lookupUtil;
		this.database = database;
	}

	public Collection<Contact> getAllContacts() {
		Collection<Contact> contacts = database.getAllContacts();
		validateContacts(contacts);
		return contacts;
	}

	public boolean addContact(String systemID) {
		Contact contact = lookupContact(systemID);
		if (contact == null) {
			Log.d(LOG, "no system contact with id: " + systemID);
			return false;
		}
		if (database.containsContactWithSystemID(systemID)) {
			Log.d(LOG, "contact " + systemID + " already exists");
			return false;
		}
		database.addContact(contact);
		return true;
	}

	public void deleteContact(long id) {
		database.deleteContactWithID(id);
	}

	private void validateContacts(Collection<Contact> contacts) {
		// XXX Validate with system database and store modified contacts
		// for each contact
		// 		get system contact
		// 		if system contacts has phones:
		//			remove contact phones which are not in system contacts phones.
		//			if contacts has no phone:
		//				copy first phone from system contact
		// 		if system contacts has emails:
		//			remove contact emals which are not in system contacts phones.
		//			if contacts has no email:
		//				copy first email from system contact
		// 		if contact was modifed:
		//			update contact in database
	}
	
	private Contact lookupContact(String systemID) {
		return lookupUtil.lookup(context, systemID);
	}

}
