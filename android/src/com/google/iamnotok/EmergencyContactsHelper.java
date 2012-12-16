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
		for (Contact contact : contacts) {
			validateContact(contact);
		}
		return contacts;
	}

	public void validateContact(Contact contact) {
		Log.d(LOG, "validating contact: " + contact.getName());
		Contact source = lookupUtil.lookup(contact.getSystemID());
		if (source == null) {
			Log.d(LOG, "Keeping stored info for contact " + contact.getName());
			return;
		}
		contact.validate(source);
		if (contact.isDirty()) {
			Log.d(LOG, "contact " + contact.getName() + " was modfied");
			database.updateContact(contact);
		}
	}

	public boolean addContact(String systemID) {
		if (database.containsContactWithSystemID(systemID)) {
			Log.d(LOG, "Contact " + systemID + " already exists");
			return false;
		}
		Contact contact = lookupUtil.lookup(systemID);
		if (contact == null) {
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
	
}
