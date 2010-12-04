package com.google.iamnotok;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

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

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference.getKey().equalsIgnoreCase(getString(R.string.quiet_mode_enable))) {
			ScreenOnOffReceiver.register(getApplicationContext(), 
					((CheckBoxPreference) preference).isChecked());
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}


}
