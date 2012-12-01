package com.google.iamnotok;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

	public static final String ACCOUNT_NAME_KEY = "select_account_list";

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
}
