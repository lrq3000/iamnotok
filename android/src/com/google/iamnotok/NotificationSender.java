package com.google.iamnotok;

import java.util.Collection;

import com.google.iamnotok.EmergencyContactsHelper.Contact;
import com.google.iamnotok.EmergencyNotificationService.VigilanceState;
import com.google.iamnotok.LocationTracker.LocationAddress;

public interface NotificationSender {
	/**
	 * Sends notifications with location to the collection of contacts, or any subset of them.
	 *
	 * @return false if this notifications is impossible to send (shouldn't be sent again).
	 */
	public boolean sendNotifications(
			Collection<Contact> contacts, LocationAddress locationAddress, VigilanceState state);
}
