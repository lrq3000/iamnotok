package com.google.iamnotok;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.util.Log;

public class ContactLookupUtil implements ContactLookup {

	private static final String LOG = "ContactLookupUtil";
	
	private static final String[] PROJECTION = {
		Data._ID,
		Data.IS_SUPER_PRIMARY,
		Data.DISPLAY_NAME, 
		Data.MIMETYPE,
		Phone.TYPE,
		Phone.NUMBER,
		Phone.LABEL,
		Email.TYPE,
		Email.DATA,
		Email.LABEL
	};

	private static final String WHERE_CLAUSE = Data.CONTACT_ID + " = ? AND ("
			+ Data.MIMETYPE + " =? OR " + Data.MIMETYPE + " =?)";

	private final Context context;

	public ContactLookupUtil(Context context) {
		this.context = context;
	}
	
	/**
	 * @param id
	 *            The id of the contact to look for.
	 * @param context
	 *            The context to use for the lookup service.
	 * @return the contact with the given id, or null if no such contact exists.
	 */
	public Contact lookup(String id) {
		ContentResolver cr = context.getContentResolver();
		final String[] whereArgs = { id,
				CommonDataKinds.Email.CONTENT_ITEM_TYPE,
				CommonDataKinds.Phone.CONTENT_ITEM_TYPE };

		Cursor cur = cr.query(Data.CONTENT_URI, PROJECTION, WHERE_CLAUSE,
				whereArgs, Data.IS_SUPER_PRIMARY + " DESC ");
		
		if (cur == null) {
			return null;
		}
		try {
			// Getting indexes dynamically make it easier to modify the projection.
			final int mimetypeCol = cur.getColumnIndex(Data.MIMETYPE);
			final int nameCol = cur.getColumnIndex(Data.DISPLAY_NAME);
			final int phoneTypeCol = cur.getColumnIndex(Phone.TYPE);
			final int phoneNumberCol = cur.getColumnIndex(Phone.NUMBER);
			final int phoneLabelCol = cur.getColumnIndex(Phone.LABEL);
			final int emailTypeCol = cur.getColumnIndex(Email.TYPE);
			final int emailDataCol = cur.getColumnIndex(Email.DATA);
			final int emailLabelCol = cur.getColumnIndex(Email.LABEL);
			
			String name = null;
			Set<String> seen = new HashSet<String>();
			List<Notification> notifications = new ArrayList<Notification>();
			
			while (cur.moveToNext()) {
				if (name == null) {
					name = cur.getString(nameCol);
				}
				final String mimetype = cur.getString(mimetypeCol);
				if (mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
					final int type = cur.getInt(phoneTypeCol);
					if (type == Phone.TYPE_MOBILE) {
						final String value = cur.getString(phoneNumberCol);
						// Add phone, ignoring duplicates
						if (!seen.contains(value)) {
							seen.add(value);
							String label = cur.getString(phoneLabelCol);
							if (label == null)
								label = context.getString(Phone.getTypeLabelResource(type));
							Log.d(LOG, "adding phone number: " + value + " label: " + label);
							notifications.add(new Notification(Notification.TYPE_SMS, value, label));
						}
					}
				} else if (mimetype.equals(Email.CONTENT_ITEM_TYPE)) {
					final String value = cur.getString(emailDataCol);
					// Add email, ignoring duplicates
					if (!seen.contains(value)) {
						seen.add(value);
						String label = cur.getString(emailLabelCol);
						if (label == null) {
							final int type = cur.getInt(emailTypeCol);
							label = context.getString(Email.getTypeLabelResource(type));
						}
						Log.d(LOG, "adding email data: " + value + " label: " + label);
						notifications.add(new Notification(Notification.TYPE_EMAIL, value, label));
					}
				}
			}
			return new Contact(id, name, notifications);
		} finally {
			cur.close();
		}
	}

}
