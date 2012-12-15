package com.google.iamnotok;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;

/**
 * Activity for changing application settings - updates SharedPreferences
 * that can be accessed by all other services and activities. Used to store
 * installation-specific settings such as "What triggers emergency", etc.,
 * settings that should be synced with web and in between different
 * installs of the application should go to the database.
 */
public class ApplicationSettingsActivity extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);

		// Put all of the google accounts on the phone into the account list.
		Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
		ListPreference accountList = (ListPreference) this.findPreference(
				getString(R.string.select_account_list));
		String[] captions = new String[accounts.length];
		String[] values = new String[accounts.length];
		for (int i = 0; i < accounts.length; ++i) {
			Account account = accounts[i];
			captions[i] = account.name;
			values[i] = account.name;
		}
		accountList.setEntries(captions);
		accountList.setEntryValues(values);

		// If we have a phone number to use, fill it in the phone number field.
		TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String lineNumber = telMgr.getLine1Number();
		EditTextPreference phoneText = (EditTextPreference) this.findPreference(
				getString(R.string.account_phone_number));
		if (phoneText.getText() == null || phoneText.getText().equals("")) {
			phoneText.setText(lineNumber);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference.getKey().equalsIgnoreCase(getString(R.string.quiet_mode_enable))) {
			if (((CheckBoxPreference) preference).isChecked()) {
				ScreenOnOffReceiver.register(getApplicationContext());
			} else {
				ScreenOnOffReceiver.unregister(getApplicationContext());
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}


}
