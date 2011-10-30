package com.google.iamnotok;

import java.util.Collection;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.google.iamnotok.EmergencyContactsHelper.Contact;
import com.google.iamnotok.EmergencyNotificationService.VigilanceState;
import com.google.iamnotok.LocationTracker.LocationAddress;
import com.google.iamnotok.utils.FormatUtils;

public class SmsNotificationSender implements NotificationSender {
	private static final String LOG_TAG = "IAMNOTOK - smser";

	private final FormatUtils formatUtils;
	private final Context context;

	public SmsNotificationSender(FormatUtils formatUtils, Context context) {
		this.formatUtils = formatUtils;
		this.context = context;
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

				String SENT = "SMS_SENT";
				String DELIVERED = "SMS_DELIVERED";

				PendingIntent sentPI = PendingIntent.getBroadcast(
						context, 0, new Intent(SENT),
						0);
				PendingIntent deliveredPI = PendingIntent.getBroadcast(
						context, 0, new Intent(
								DELIVERED), 0);

				// ---when the SMS has been sent---
				context.registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context arg0, Intent arg1) {
						switch (getResultCode()) {
						case Activity.RESULT_OK:
							Toast.makeText(context, "SMS sent",
									Toast.LENGTH_SHORT).show();
							break;
						case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
							Toast.makeText(context, "Generic failure",
									Toast.LENGTH_SHORT).show();
							break;
						case SmsManager.RESULT_ERROR_NO_SERVICE:
							Toast.makeText(context, "No service",
									Toast.LENGTH_SHORT).show();
							break;
						case SmsManager.RESULT_ERROR_NULL_PDU:
							Toast.makeText(context, "Null PDU",
									Toast.LENGTH_SHORT).show();
							break;
						case SmsManager.RESULT_ERROR_RADIO_OFF:
							Toast.makeText(context, "Radio off",
									Toast.LENGTH_SHORT).show();
							break;
						}
					}
				}, new IntentFilter(SENT));

				// ---when the SMS has been delivered---
				context.registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context arg0, Intent arg1) {
						switch (getResultCode()) {
						case Activity.RESULT_OK:
							Toast.makeText(context, "SMS delivered",
									Toast.LENGTH_SHORT).show();
							break;
						case Activity.RESULT_CANCELED:
							Toast.makeText(context,
									"SMS not delivered", Toast.LENGTH_SHORT)
									.show();
							break;
						}
					}
				}, new IntentFilter(DELIVERED));

				SmsManager sms = SmsManager.getDefault();
				sms.sendTextMessage(phoneNumber, null, message, sentPI,
						deliveredPI);

			}

		});
		messageSender.start();
	}
}
