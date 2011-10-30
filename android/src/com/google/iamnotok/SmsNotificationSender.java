package com.google.iamnotok;

import java.util.Collection;

import android.telephony.SmsManager;
import android.util.Log;

import com.google.iamnotok.EmergencyContactsHelper.Contact;
import com.google.iamnotok.EmergencyNotificationService.VigilanceState;
import com.google.iamnotok.LocationTracker.LocationAddress;
import com.google.iamnotok.utils.FormatUtils;

public class SmsNotificationSender implements NotificationSender {
	private static final String LOG_TAG = "IAMNOTOK - smser";

	private final FormatUtils formatUtils;

	public SmsNotificationSender(FormatUtils formatUtils) {
		this.formatUtils = formatUtils;
	}

	@Override
	public boolean sendNotifications(
			Collection<Contact> contacts, LocationAddress locationAddress, VigilanceState state) {
		for (Contact contact : contacts) {
			if (contact.getPhone() != null) {
				sendTextMessage(contact.getPhone(), locationAddress, state);
			}
		}

		return true;
	}

	/**
	 * Sends an SMS to another device
	 **/
	private void sendTextMessage(final String phoneNumber, final LocationAddress locationAddress, final VigilanceState state) {
		Thread messageSender = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(LOG_TAG, "Sending sms to: " + phoneNumber);
				String message = "";
				if (state == VigilanceState.NORMAL_STATE) {
					message = "I am OK now";
					Log.d(LOG_TAG, "Sending the message " + message);
				} else {
					Log.d(LOG_TAG, "Getting location");
					message = formatUtils.formatMessage(locationAddress);
				}

				SmsManager sms = SmsManager.getDefault();
				sms.sendTextMessage(phoneNumber, null, message, null, null);

			}

		});
		messageSender.start();
	}
}
