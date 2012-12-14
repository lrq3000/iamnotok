package com.google.iamnotok;

import java.util.Collection;
import java.util.List;

import com.google.iamnotok.Contact.Attribute;

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
		if (database.containsContactWithSystemID(systemID)) {
			Log.d(LOG, "contact " + systemID + " already exists");
			return false;
		}
		Contact contact = lookupContact(systemID);
		if (contact == null) {
			Log.d(LOG, "no system contact with id: " + systemID);
			return false;
		}
		
		// XXX "Select" the first phone and email until the user interface for
		// selecting phones and emails is done.
		keepFirst(contact.getEmails());
		keepFirst(contact.getPhones());
		
		database.addContact(contact);
		return true;
	}

	private void keepFirst(List<Attribute> list) {
		if (list.size() > 1) {
			Attribute first = list.get(0);
			list.clear();
			list.add(first);
		}
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
