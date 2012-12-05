package com.google.iamnotok;

import com.google.iamnotok.LocationTracker.LocationAddress;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.preference.PreferenceManager;

public class Preferences {
	
	public enum VigilanceState {
		NORMAL_STATE,
		WAITING_STATE,
		EMERGENCY_STATE,
	}
	
	// Keys
	// TODO: consistent names
	public static final String ACCOUNT_NAME_KEY                 = "select_account_list";
	public static final String PHONE_NUMBER_KEY                 = "account_phone_number";
	public static final String QUITE_MODE_KEY                   = "Enable Quiet Mode";
	public static final String CUSTOM_MESSAGE_KEY               = "edittext_custom_message";
	public static final String SMS_NOTIFICATION_KEY             = "sms_notification";
	public static final String EMAIL_NOTIFICATION_KEY           = "email_notification";
	public static final String CALL_NOTIFICATION_KEY            = "email_notification";
	public static final String MESSAGE_INTERVAL_SECONDS_KEY     = "edittext_message_interval";
	public static final String CANCELATION_DELAY_SECONDS_KEY    = "cancelation_delay";
	public static final String VIGILANCE_STATE_KEY              = "vigilanceStateKey";
	public static final String CURRENT_LOCATION_KEY             = "keyCurrentLocationAddress";
	public static final String LAST_NOTIFIED_LOCATION_KEY       = "keyLastNotifiedAddress";
	
	// Default values
	public static final long DEFAULT_MESSAGE_INTERVAL_SECONDS 	= 5 * 60;
	public static final long DEFAULT_CANCELATION_DELAY_SECONDS 	= 10;
	
	private final SharedPreferences preferences;

	public Preferences(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	// User preferences
	
	public String getAccountName() {
		return preferences.getString(ACCOUNT_NAME_KEY, "");
	}

	public void setAccountName(String name) {
		preferences.edit().putString(ACCOUNT_NAME_KEY, name).commit();
	}
	
	public String getPhoneNumber() {
		return preferences.getString(PHONE_NUMBER_KEY, "");
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
		
	// Application data 
	
	public VigilanceState getVigilanceState() {
		try {
			int ordinal = preferences.getInt(VIGILANCE_STATE_KEY, VigilanceState.NORMAL_STATE.ordinal());
			return VigilanceState.values()[ordinal];
		} catch (ClassCastException e) {
			// Stored value cannot be parsed as int (unlikely)
			return VigilanceState.NORMAL_STATE;
		}
	}

	public void setVigilanceState(VigilanceState state) {
		preferences.edit().putInt(VIGILANCE_STATE_KEY, state.ordinal()).commit();
	}

	public LocationAddress getCurrentLocation() {
		String encoded = preferences.getString(CURRENT_LOCATION_KEY, null);
		if (encoded == null) {
			return null;
		}
		return decodeLocationAddress(encoded);
	}

	public void setCurrentLocation(LocationAddress address) {
		String encoded = encodeLocationAddress(address);
		preferences.edit().putString(CURRENT_LOCATION_KEY, encoded).commit();
	}

	public LocationAddress getLastNotifiedLocation() {
		String encoded = preferences.getString(LAST_NOTIFIED_LOCATION_KEY, null);
		if (encoded == null) {
			return null;
		}
		return decodeLocationAddress(encoded);
	}

	public void setLastNotifiedLocation(LocationAddress address) {
		String encoded = encodeLocationAddress(address);
		preferences.edit().putString(LAST_NOTIFIED_LOCATION_KEY, encoded).commit();
	}

	// Helpers
	
	private String encodeLocationAddress(LocationAddress address) {
		Parcel parcel = Parcel.obtain();
		address.writeToParcel(parcel, 0);
		return parcel.marshall().toString();
	}

	private LocationAddress decodeLocationAddress(String encoded) {
		Parcel parcel = Parcel.obtain();
		parcel.unmarshall(encoded.getBytes(), 0, encoded.length());
		return LocationAddress.readFromParcel(parcel);
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
