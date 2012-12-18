package com.google.iamnotok;

import java.util.List;

public class Contact {

	private final static int NO_ID = 0;
	
	private final long id;
	private final String systemID;
	private final NotificationList phones;
	private final NotificationList emails;
	
	private String name;
	private boolean dirty;

	// Creating contact from system contacts database
	public Contact(String systemID, String name, List<Notification> phones, List<Notification> emails) {
		this(NO_ID, systemID, name, phones, emails);
	}

	// Creating contact stored in iamnotok database
	public Contact(long id, String systemID, String name, List<Notification> phones, List<Notification> emails) {
		this.id = id;
		this.systemID = systemID;
		this.name = name;
		this.phones = new NotificationList(phones);
		this.emails = new NotificationList(emails);
		this.dirty = (id == NO_ID); // Not stored yet
	}

	public void validate(Contact other) {
		setName(other.getName());
		phones.validate(other.getSMSNotifications());
		emails.validate(other.getEmailNotifications());
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
	
	public void setName(String name) {
		if (!this.name.equals(name)) {
			this.name = name;
			this.dirty = true;
		}
	}

	public NotificationList getSMSNotifications() {
		return phones;
	}

	public List<String> getEnabledPhones() {
		return phones.getEnabledTargets();
	}

	public NotificationList getEmailNotifications() {
		return emails;
	}
	
	public List<String> getEnabledEmails() {
		return emails.getEnabledTargets();
	}

	public boolean isDirty() {
		return dirty || phones.isDirty() || emails.isDirty();
	}
	
	public void beClean() {
		if (dirty) {
			dirty = false;
			phones.beClean();
			emails.beClean();
		}
	}

	@Override
	public String toString() {
		return "<Contact " + name + " phones: " + getEnabledPhones() + " emails: " + getEnabledEmails() + ">";
	}

}