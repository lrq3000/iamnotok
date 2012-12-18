package com.google.iamnotok;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationList implements Iterable<Notification> {
	
	private final List<Notification> notifications;
	private boolean dirty = false;
	
	// Creating
	
	public NotificationList(List<Notification> notifications) {
		this.notifications = notifications;
	}
	
	// Read only list interface for adapting to a view
	
	public Notification get(int location) {
		return notifications.get(location);
	}
	
	public int size() {
		return notifications.size();
	}

	public boolean isEmpty() {
		return notifications.isEmpty();
	}
	
	// Iterable interface
	
	@Override
	public Iterator<Notification> iterator() {
		return notifications.iterator();
	}

	// NotificationList interface
	
	public void validate(NotificationList other) {
		if (other.notifications.isEmpty())
			return;
	
		// Remove notifications not in other
		Iterator<Notification> it = notifications.iterator();
		while (it.hasNext()) {
			Notification n = it.next();
			if (other.findTarget(n.getTarget()) == null) {
				it.remove();
				dirty = true;
			}
		}
		
		// Add or validate notifications from other
		for (Notification n : other.notifications) {
			Notification existing = findTarget(n.getTarget());
			if (existing != null) {
				existing.validate(n);
			} else {
				notifications.add(n);
			}
		}
		
		// Ensure that I was not disabled by removing the only enabled
		// notification.
		if (!notifications.isEmpty() && !isEnabled()) {
			notifications.get(0).setEnabled(true);
		}
	}
		
	public List<String> getEnabledTargets() {
		List<String> enabled = new ArrayList<String>();
		for (Notification n : notifications) {
			if (n.isEnabled())
				enabled.add(n.getTarget());
		}
		return enabled;
	}
	
	public boolean isEnabled() {
		for (Notification n : notifications) {
			if (n.isEnabled())
				return true;
		}
		return false;
	}
	
	public boolean isDirty() {
		if (dirty)
			return true;
		for (Notification n : notifications) {
			if (n.isDirty())
				return true;
		}
		return false;
	}
	
	public void beClean() {
		dirty = false;
		for (Notification n : notifications) {
			n.beClean();
		}
	}

	// Helpers
	
	// Return notification with target or null if no such notification exists
	private Notification findTarget(String target) {
		for (Notification n : notifications) {
			if (n.getTarget().equals(target))
				return n;
		}
		return null;
	}

}
