package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

public class Contact {

	private final static int NO_ID = 0;
	
	private final long id;
	private final String systemID;
	private final String name;
	private final List<Notification> phones;
	private final List<Notification> emails;

	// Creating contact from system contacts database
	public Contact(String systemID, String name, List<Notification> phones, List<Notification> emails) {
		this(NO_ID, systemID, name, phones, emails);
	}

	// Creating contact stored in iamnotok database
	public Contact(long id, String systemID, String name, List<Notification> phones, List<Notification> emails) {
		this.id = id;
		this.systemID = systemID;
		this.name = name;
		this.phones = phones;
		this.emails = emails;
	}

	public long getID() {
		return id;
	}

	public String getSystemID() {
		return systemID;
	}

	public String getName() {
		return name;
	}

	public List<Notification> getSMSNotifications() {
		return phones;
	}

	public List<String> getEnabledPhones() {
		List<String> result = new ArrayList<String>();
		for (Notification phone : phones) {
			if (phone.isEnabled())
				result.add(phone.target);
		}
		return result;
	}

	public List<Notification> getEmailNotifications() {
		return emails;
	}
	
	public List<String> getEnabledEmails() {
		List<String> result = new ArrayList<String>();
		for (Notification email : emails) {
			if (email.isEnabled())
				result.add(email.target);
		}
		return result;
	}

	@Override
	public String toString() {
		return "<Contact " + name + " phones: " + getEnabledPhones() + " emails: " + getEnabledEmails() + ">";
	}

}