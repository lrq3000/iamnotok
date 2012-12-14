package com.google.iamnotok;

import java.util.Collection;

import com.google.iamnotok.Contact.Attribute;

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
		String phone = getFirstPhone(contacts);
		if (phone == null) {
			Log.w(LOG_TAG,
					"Unable to find a contact with number, disabled emergency call");
			return false;
		}
		Intent i = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phone,
				null));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
		return true;
	}

	private String getFirstPhone(Collection<Contact> contacts) {
		for (Contact contact : contacts) {
			for (Attribute phone : contact.getPhones()) {
				return phone.value;
			}
		}
		return null;
	}
	
	public void closeCall() {
		// Do we even need that?
	}
}
