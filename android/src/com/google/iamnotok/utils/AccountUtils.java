package com.google.iamnotok.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.iamnotok.Preferences;
import com.google.iamnotok.R;

public class AccountUtils {
	private static final String LOG_TAG = "AccountUtils";
	
	private final Context context;

	public AccountUtils(Context context) {
		this.context = context;
	}

	public String getMailAddress() {
		return getSelectedAccount().name;
	}

	private Account getSelectedAccount() {
		// First try to get the selected account from the preferences.
		Preferences pref = new Preferences(this.context);
				
		String accountName = pref.getAccountName();
		Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
		for (Account account : accounts) {
			if (account.name != null && account.name.equals(accountName)) {
				Log.d(LOG_TAG, "using prefered account: " + accountName);
				return account;
			}
		}

		// If we got here, then we didn't find the account in the preferences.
		// This probably means that the user removed the account after selecting it.
		// In this case, we clear the preferred name and return the first available 
		// account, if any.
		// TODO: Should we notify the user here?
		
		if (!accountName.equals("")) {
			Log.d(LOG_TAG, "no such account: " + accountName + " clearing preferred account");
			pref.setAccountName("");
		}
		
		if (accounts.length > 0) {
			Log.d(LOG_TAG, "using default gmail account: " + accounts[0].name);
			return accounts[0];
		}

		// If there are no google accounts, try looking for a non-google account.
		// NOTE: This can only be used to get the name from.
		accounts = AccountManager.get(context).getAccounts();
		if (accounts.length > 0) {
			Log.d(LOG_TAG, "using default account: " + accounts[0].name);
			return accounts[0];
		}

		Log.d(LOG_TAG, "using dummy account");
		return new Account("Unidentified User", "com.dummy");
	}

	public String getAccountName() {
		String email = getMailAddress();
		if (email.contains("@")) {
			// in case we have a mail, lets try to resolve the username.
			Uri uri = Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(email));
			Cursor cur = context.getContentResolver().query(
					uri, new String[]{Phone.DISPLAY_NAME}, null, null, null);
			if (cur.moveToFirst()) {
				return cur.getString(cur.getColumnIndex(Phone.DISPLAY_NAME));
			} else {
				return email.substring(0, email.indexOf('@'));
			}
		}
		return email;
	}

	public String getPhoneNumber() {
		// First try to get the phone number from the preferences.
		SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(context);
		String phoneNumber = prefs.getString(
				context.getString(R.string.account_phone_number), "");
		if (phoneNumber != null && !phoneNumber.equals("")) {
			return phoneNumber;
		}

		// If we don't have a phone number in the preferences, try getting it
		// from the weird function called getLine1Number.
		TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String lineNumber = telMgr.getLine1Number();
		if (lineNumber != null && !lineNumber.equals("")) {
			return lineNumber;
		}

		return "Unidentified Phone Number";
	}
	
	public String getCustomMessage() {
		Preferences pref = new Preferences(this.context);
		return pref.getCustomMessage();
	}
}
