package com.google.iamnotok;

import java.util.Collection;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class EmergencyCaller {
	
	private static final String LOG = "EmergencyCaller";

	private final Context context;

	public EmergencyCaller(Context context) {
		this.context = context;
	}

	public boolean makeCall(Collection<Contact> contacts) {
		String phone = getFirstPhone(contacts);
		if (phone == null) {
			Log.w(LOG,
					"Unable to find a contact with number, disabled emergency call");
			return false;
		}
		Intent i = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phone,
				null));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
		return true;
	}

	// Temporary hack until we store the voice call phone number somewhere.
	private String getFirstPhone(Collection<Contact> contacts) {
		for (Contact contact : contacts) {
			for (String phone : contact.getEnabledPhones()) {
				return phone;
			}
		}
		return null;
	}
	
	public void closeCall() {
		// Do we even need that?
	}
}
