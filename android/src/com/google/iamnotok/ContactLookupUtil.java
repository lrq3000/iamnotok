package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;

public class ContactLookupUtil implements ContactLookup {

	private static final String[] PROJECTION = {
		Data._ID,
		Data.IS_SUPER_PRIMARY,
		Data.DISPLAY_NAME, 
		Data.MIMETYPE,
		Phone.TYPE,
		Phone.NUMBER,
		Email.DATA, 
	};

	private static final String WHERE_CLAUSE = Data.CONTACT_ID + " = ? AND ("
			+ Data.MIMETYPE + " =? OR " + Data.MIMETYPE + " =?)";

	/**
	 * @param id
	 *            The id of the contact to look for.
	 * @param context
	 *            The context to use for the lookup service.
	 * @return the contact with the given id, or null if no such contact exists.
	 */
	public Contact lookup(Context context, String id) {
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
			final int emailDataCol = cur.getColumnIndex(Email.DATA);
			
			String name = null;
			List<String> phones = new ArrayList<String>();
			List<String> emails = new ArrayList<String>();
			
			while (cur.moveToNext()) {
				if (name == null) {
					name = cur.getString(nameCol);
				}
				final String mimetype = cur.getString(mimetypeCol);
				if (mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
					if (cur.getInt(phoneTypeCol) == Phone.TYPE_MOBILE) {
						phones.add(cur.getString(phoneNumberCol));
					}
				} else if (mimetype.equals(Email.CONTENT_ITEM_TYPE)) {
					emails.add(cur.getString(emailDataCol));
				}
			}
			return new Contact(id, name, phones, emails);
		} finally {
			cur.close();
		}
	}

}
