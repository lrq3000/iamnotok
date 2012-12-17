package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class Contact {

	private final static String LOG = "Contact";
	private final static int NO_ID = 0;
	
	private final long id;
	private final String systemID;
	private final List<Notification> phones;
	private final List<Notification> emails;
	
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
		this.phones = phones;
		this.emails = emails;
		this.dirty = (id == NO_ID); // Not stored yet
	}

	public void validate(Contact source) {
		setName(source.getName());
		validateNotifications(source.phones, phones);
		validateNotifications(source.emails, emails);
	}

	private void validateNotifications(List<Notification> src, List<Notification> dst) {
		if (src.isEmpty())
			return;
		
		List<Notification> validated = new ArrayList<Notification>();
		
		// Remove notifications that do not exists in src
		boolean removed = false;
		for (Notification n : dst) {
			if (Notification.containsTarget(src, n.getTarget())) {
				validated.add(n);
			} else {
				Log.d(LOG, "removing " + n);
				removed = true;
			}
		}
		
		// Add new notifications from src and update existing notifications
		boolean added = false;
		boolean updated = false;
		for (Notification a : src) {
			Notification b = Notification.lookupTarget(validated, a.getTarget());
			if (b == null) {
				Log.d(LOG, "adding " + a);
				validated.add(a);
				added = true;
			} else {
				b.setLabel(a.getLabel());
				if (b.isDirty())
					updated = true;
			}
		}
		
		// Ensure that contact is not invalidated after removing enabled
		// notifications by enabling first notification.
		if (removed) {
			if (!Notification.containsEnabled(validated)) {
				validated.get(0).setEnabled(true);
			}
		}

		// Update dst if needed
		if (added || removed || updated) {
			Log.d(LOG, "notifications were modified");
			dst.clear();
			dst.addAll(validated);
			dirty  = true;
		}
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

	public List<Notification> getSMSNotifications() {
		return phones;
	}

	public List<String> getEnabledPhones() {
		return Notification.enabledTargets(phones);
	}

	public List<Notification> getEmailNotifications() {
		return emails;
	}
	
	public List<String> getEnabledEmails() {
		return Notification.enabledTargets(emails);
	}

	public boolean isDirty() {
		return dirty || Notification.containsDirty(phones) || Notification.containsDirty(emails);
	}
	
	public void beClean() {
		if (dirty) {
			dirty = false;
			for (Notification phone : phones) {
				phone.beClean();
			}
			for (Notification email : emails) {
				email.beClean();
			}
		}
	}

	@Override
	public String toString() {
		return "<Contact " + name + " phones: " + getEnabledPhones() + " emails: " + getEnabledEmails() + ">";
	}

}