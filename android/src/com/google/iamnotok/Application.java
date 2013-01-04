package com.google.iamnotok;

import java.util.List;
import android.util.Log;

/**
 * Maintains application data
 */
public class Application extends android.app.Application {
	
	private static final String LOG = "Application";

	private ContactLookup lookupUtil = new ContactLookupUtil(this);
	private Database database = new Database(this);
	private List<Contact> contacts;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(LOG, "created");
	}
	
	public void validateContacts() {
		Log.i(LOG, "validating contacts");
		for (Contact contact : getAllContacts()) {
			Log.d(LOG, "validating contact: " + contact.getName());
			Contact source = lookupUtil.lookup(contact.getSystemID());
			if (source == null) {
				Log.d(LOG, "Keeping stored info for contact " + contact.getName());
				continue;
			}
			contact.validate(source);
			if (contact.isDirty()) {
				Log.d(LOG, "contact " + contact.getName() + " was modfied");
				database.updateContact(contact);
			}			
		}
	}
	
	public List<Contact> getAllContacts() {
		if (contacts == null) {
			contacts = database.getAllContacts();
		}
		return contacts;
	}

	public boolean addContact(String systemID) {
		for (Contact contact : getAllContacts()) {
			if (contact.getSystemID().equals(systemID)) {
				Log.d(LOG, "Contact " + systemID + " already exists");
				return false;
			}
		}
		Contact contact = lookupUtil.lookup(systemID);
		if (contact == null) {
			return false;
		}
		
		// XXX Select the first phone and email until the user interface for
		// selecting phones and emails is done.
		NotificationList phones = contact.getSMSNotifications();
		if (!phones.isEmpty())
			phones.get(0).setEnabled(true);
		NotificationList emails = contact.getEmailNotifications();
		if (!emails.isEmpty())
			emails.get(0).setEnabled(true);
		
		database.addContact(contact);
		contacts.add(contact);
		return true;
	}

	public void deleteContact(Contact contact) {
		database.deleteContactWithID(contact.getID());
		contacts.remove(contact);
	}
	
}
