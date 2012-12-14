package com.google.iamnotok;

import java.util.List;

public class Contact {

	public static class Attribute {
		public final String value;
		public final String label;
		public Attribute(String value, String label) {
			this.value = value;
			this.label = label;
		}
		@Override
		public String toString() {
			return value + " (" + label + ")"; 
		}
	}
	
	private final static int NO_ID = 0;
	
	private final long id;
	private final String systemID;
	private final String name;
	private final List<Attribute> phones;
	private final List<Attribute> emails;

	// Creating contact from system contacts database
	public Contact(String systemID, String name, List<Attribute> phones, List<Attribute> emails) {
		this(NO_ID, systemID, name, phones, emails);
	}

	// Creating contact stored in iamnotok database
	public Contact(long id, String systemID, String name, List<Attribute> phones, List<Attribute> emails) {
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

	public List<Attribute> getPhones() {
		return phones;
	}

	public List<Attribute> getEmails() {
		return emails;
	}

	@Override
	public String toString() {
		return "<Contact " + name + " phones: " + getPhones() + " emails: " + getEmails() + ">";
	}
}