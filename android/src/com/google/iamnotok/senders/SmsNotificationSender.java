package com.google.iamnotok.senders;

import java.util.ArrayList;
import java.util.Collection;

import android.content.Context;
import android.telephony.SmsManager;

import com.google.iamnotok.Contact;
import com.google.iamnotok.LocationTracker.LocationAddress;
import com.google.iamnotok.Preferences.VigilanceState;
import com.google.iamnotok.utils.AccountUtils;
import com.google.iamnotok.utils.FormatUtils;
import com.google.iamnotok.utils.IamNotOKLogger;

public class SmsNotificationSender implements NotificationSender {

	private final AccountUtils accountUtils;
	private final FormatUtils formatUtils;
	private final SmsManager smsManager = SmsManager.getDefault();

	private final Context context;

	public SmsNotificationSender(Context context, FormatUtils formatUtils, AccountUtils accountUtils) {
		this.accountUtils = accountUtils;
		this.formatUtils = formatUtils;
		this.context = context;
	}

	@Override
	public boolean sendNotifications(Collection<Contact> contacts,
			LocationAddress locationAddress, VigilanceState state) {
		for (Contact contact : contacts) {
			if (contact.getSelectedPhone() != null) {
				sendTextMessage(contact.getSelectedPhone(), locationAddress, state);
			}
		}

		return true;
	}

	/**
	 * Sends an SMS to another device
	 **/
	private void sendTextMessage(final String phoneNumber,
			final LocationAddress locationAddress, final VigilanceState state) {
		Thread messageSender = new Thread(new Runnable() {
			@Override
			public void run() {
				String message = "";
				if (state == VigilanceState.NORMAL_STATE) {
					message = "I am OK now";
				} else {
					message = formatUtils.formatMessage(
							locationAddress, accountUtils.getCustomMessage());
				}

				IamNotOKLogger.Log(context, "SmsSender", "Sending SMS to "
						+ phoneNumber + " : " + message);
				ArrayList<String> parts = smsManager.divideMessage(message);
				smsManager.sendMultipartTextMessage(phoneNumber, null, parts,
						null, null);

			}

		});
		messageSender.start();
	}
}
