package com.google.iamnotok;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	
	// Keys
	public static final String ACCOUNT_NAME_KEY                 = "select_account_list";
	public static final String QUITE_MODE_KEY                   = "Enable Quiet Mode";
	public static final String CUSTOM_MESSAGE_KEY               = "edittext_custom_message";
	public static final String SMS_NOTIFICATION_KEY             = "sms_notification";
	public static final String EMAIL_NOTIFICATION_KEY           = "email_notification";
	public static final String CALL_NOTIFICATION_KEY            = "email_notification";
	public static final String MESSAGE_INTERVAL_SECONDS_KEY     = "edittext_message_interval";
	public static final String CANCELATION_DELAY_SECONDS_KEY    = "cancelation_delay";
	
	// Default values
	public static final long DEFAULT_MESSAGE_INTERVAL_SECONDS 	= 5 * 60;
	public static final long DEFAULT_CANCELATION_DELAY_SECONDS 	= 10;
	
	private final SharedPreferences preferences;

	public Preferences(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public String getAccountName() {
		return preferences.getString(ACCOUNT_NAME_KEY, "");
	}

	public void setAccountName(String name) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(ACCOUNT_NAME_KEY, name);
		editor.commit();
	}
	
	public boolean getQuiteMode() {
		// Enabled if not set
		return preferences.getBoolean(QUITE_MODE_KEY, true);
	}
	
	public String getCustomMessage() {
		return preferences.getString(CUSTOM_MESSAGE_KEY, "");
	}
	
	public boolean getSMSNotification() {
		// Enabled if not set
		return preferences.getBoolean(SMS_NOTIFICATION_KEY, true);
	}

	public boolean getEmailNotification() {
		// Enabled if not set
		return preferences.getBoolean(EMAIL_NOTIFICATION_KEY, true);
	}
	
	public boolean getCallNotification() {
		return preferences.getBoolean(CALL_NOTIFICATION_KEY, false);
	}

	public long getMessageIntervalMilliseconds() {
		return getLong(MESSAGE_INTERVAL_SECONDS_KEY, DEFAULT_MESSAGE_INTERVAL_SECONDS) * 1000;
	}

	public long getCancelationDelayMilliseconds() {
		return getLong(CANCELATION_DELAY_SECONDS_KEY, DEFAULT_CANCELATION_DELAY_SECONDS) * 1000;
	}
	
	private long getLong(String key, long defaultValue) {
		try {
			return preferences.getLong(key, defaultValue);
		} catch (ClassCastException e) {
			// Stored value cannot be parsed as long
			return defaultValue;
		}
	}
}
