package com.google.iamnotok;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Activity for changing application settings - updates SharedPreferences
 * that can be accessed by all other services and activities. Used to store
 * installation-specific settings such as "What triggers emergency", etc.,
 * settings that should be synced with web and in between different
 * installs of the application should go to the database.
 */
public class ApplicationSettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		this.addPreferencesFromResource(R.xml.preferences);
	}
}
