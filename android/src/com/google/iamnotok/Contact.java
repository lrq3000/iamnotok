package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class Contact {

	private final static String LOG = "Contact";
	private final static int NO_ID = 0;
	
	private final long id;
	private final String systemID;
	private final String name;
	private final List<Notification> phones;
	private final List<Notification> emails;
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
		validateNotifications(source.phones, phones);
		validateNotifications(source.emails, emails);
	}

	private void validateNotifications(List<Notification> src, List<Notification> dst) {
		if (src.isEmpty())
			return;
		
		// Remove notifications that do not exists in src
		boolean removed = false;
		List<Notification> updated = new ArrayList<Notification>();
		for (Notification n : dst) {
			if (Notification.containsTarget(src, n.target)) {
				updated.add(n);
			} else {
				Log.d(LOG, "removing " + n);
				removed = true;
			}
		}
		
		// Add new notifications from src
		boolean added = false;
		for (Notification n : src) {
			if (!Notification.containsTarget(updated, n.target)) {
				Log.d(LOG, "adding " + n);
				updated.add(n);
				added = true;
			}
			// XXX Update label
		}
		
		// Ensure that contact is not invalidated after removing enabled
		// notifications by enabling first notification.
		if (removed) {
			if (!Notification.containsEnabled(updated)) {
				updated.get(0).setEnabled(true);
			}
		}

		// Update dst if needed
		if (added || removed) {
			Log.d(LOG, "notifications were modified");
			dst.clear();
			dst.addAll(updated);
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
	
	public void setDirty(boolean dirty) {
		if (this.dirty != dirty) {
			this.dirty = dirty;
			// Clean notifications when clean
			if (!dirty) {
				for (Notification phone : phones) {
					phone.setDirty(false);
				}
				for (Notification email : emails) {
					email.setDirty(false);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "<Contact " + name + " phones: " + getEnabledPhones() + " emails: " + getEnabledEmails() + ">";
	}

}