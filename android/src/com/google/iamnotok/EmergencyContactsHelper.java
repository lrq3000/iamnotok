package com.google.iamnotok;

import java.util.Collection;
import java.util.List;

import android.util.Log;

/**
 * Maintains a list of contacts
 */
public class EmergencyContactsHelper {
	
	private static final String LOG = "EmergencyContactsHelper";

	private ContactLookup lookupUtil;
	private Database database;

	public EmergencyContactsHelper(ContactLookup lookupUtil, Database database) {
		this.lookupUtil = lookupUtil;
		this.database = database;
	}

	public Collection<Contact> getAllContacts() {
		Collection<Contact> contacts = database.getAllContacts();
		validateContacts(contacts);
		return contacts;
	}

	public boolean addContact(String systemID) {
		if (database.containsContactWithSystemID(systemID)) {
			Log.d(LOG, "contact " + systemID + " already exists");
			return false;
		}
		Contact contact = lookupUtil.lookup(systemID);
		if (contact == null) {
			Log.d(LOG, "no system contact with id: " + systemID);
			return false;
		}
		
		// XXX Select the first phone and email until the user interface for
		// selecting phones and emails is done.
		List<Notification> phones = contact.getSMSNotifications();
		if (!phones.isEmpty())
			phones.get(0).setEnabled(true);
		List<Notification> emails = contact.getEmailNotifications();
		if (!emails.isEmpty())
			emails.get(0).setEnabled(true);
		
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

}
