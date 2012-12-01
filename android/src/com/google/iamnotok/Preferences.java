package com.google.iamnotok;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	
	public static final String ACCOUNT_NAME_KEY = "select_account_list";
	public static final String QUITE_MODE_KEY = "Enable Quiet Mode";
	public static final String CUSTOM_MESSAGE_KEY = "edittext_custom_message";

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
		// If the key does not exists we treat it as enabled.
		return preferences.getBoolean(QUITE_MODE_KEY, true);
	}
	
	public String getCustomMessage() {
		return preferences.getString(CUSTOM_MESSAGE_KEY, "");
	}
	
}
