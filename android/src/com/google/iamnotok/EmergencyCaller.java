package com.google.iamnotok;

import java.util.Collection;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class EmergencyCaller {
	private static final String LOG_TAG = "IamNotOk - caller";

	private final Context context;

	public EmergencyCaller(Context context) {
		this.context = context;
	}

	public boolean makeCall(Collection<Contact> contacts) {
		String number = null;
		for (Contact contact : contacts) {
			number = contact.getSelectedPhone();
			if (number != null) {
				break;
			}
		}
		if (number == null) {
			Log.w(LOG_TAG,
					"Unable to find a contact with number, disabled emergency call");
			return false;
		}
		Intent i = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number,
				null));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
		return true;
	}

	public void closeCall() {
		// Do we even need that?
	}
}
