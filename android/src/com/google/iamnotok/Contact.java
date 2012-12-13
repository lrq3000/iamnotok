package com.google.iamnotok;

import java.util.List;

public class Contact {

	private final static int NO_ID = 0;
	
	private final long id;
	private final String systemID;
	private final String name;
	private final List<String> phones;
	private final List<String> emails;

	// Creating contact from system contacts database
	public Contact(String systemID, String name, List<String> phones, List<String> emails) {
		this(NO_ID, systemID, name, phones, emails);
	}

	// Creating contact stored in iamnotok database
	public Contact(long id, String systemID, String name, List<String> phones, List<String> emails) {
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

	public String getSelectedPhone() {
		if (phones.size() > 0)
			return phones.get(0);
		return null;
	}

	public String getSelectedEmail() {
		if (emails.size() > 0)
			return emails.get(0);
		return null;
	}

	@Override
	public String toString() {
		return "<Contact " + name + " phone: " + getSelectedPhone() + " email: " + getSelectedEmail() + ">";
	}
}