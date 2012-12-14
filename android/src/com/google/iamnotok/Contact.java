package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

public class Contact {

	private final static int NO_ID = 0;
	
	private final long id;
	private final String systemID;
	private final String name;
	private final List<Notification> notifications;

	// Creating contact from system contacts database
	public Contact(String systemID, String name, List<Notification> notifications) {
		this(NO_ID, systemID, name, notifications);
	}

	// Creating contact stored in iamnotok database
	public Contact(long id, String systemID, String name, List<Notification> notifications) {
		this.id = id;
		this.systemID = systemID;
		this.name = name;
		this.notifications = notifications;
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

	public List<Notification> getAllNotifications() {
		return notifications;
	}

	public List<Notification> getSMSNotifications() {
		List<Notification> result = new ArrayList<Notification>();
		for (Notification n : notifications) {
			if (n.type.equals(Notification.TYPE_SMS))
				result.add(n);
		}
		return result;
	}

	public List<String> getEnabledPhones() {
		List<String> result = new ArrayList<String>();
		for (Notification n : getSMSNotifications()) {
			if (n.isEnabled())
				result.add(n.target);
		}
		return result;
	}

	public List<Notification> getEmailNotifications() {
		List<Notification> result = new ArrayList<Notification>();
		for (Notification n : notifications) {
			if (n.type.equals(Notification.TYPE_EMAIL))
				result.add(n);
		}
		return result;
	}
	
	public List<String> getEnabledEmails() {
		List<String> result = new ArrayList<String>();
		for (Notification n : getEmailNotifications()) {
			if (n.isEnabled())
				result.add(n.target);
		}
		return result;
	}

	@Override
	public String toString() {
		return "<Contact " + name + " phones: " + getEnabledPhones() + " emails: " + getEnabledEmails() + ">";
	}

}