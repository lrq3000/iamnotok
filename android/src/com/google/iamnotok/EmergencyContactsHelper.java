package com.google.iamnotok;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.google.iamnotok.utils.StringUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

/**
 * Maintains a list of contact ids. Quite crude but should do the job for now.
 */
public class EmergencyContactsHelper {
	private static final String PREFS_NAME = "MyPrefsFile";
	private static final String CONTACT_IDS_PROPERTY_NAME = "contact_ids";

	private Context context;
	private HashMap<String, Contact> contacts;

	public EmergencyContactsHelper(Context context) {
		this.context = context;
		populateContacts();
	}

	public Collection<String> contactIds() {
		return contacts.keySet();
	}

	private void populateContacts() {
		contacts = new LinkedHashMap<String, EmergencyContactsHelper.Contact>();
		SharedPreferences settings =  prefs();
		String list = settings.getString(CONTACT_IDS_PROPERTY_NAME, "");
		for (String contactId : list.split(",")) {
			Contact contact = new Contact(contactId);
			if (!contact.lookup()) continue;
			contacts.put(contactId, contact);
		}
	}

	public Collection<Contact> getAllContacts() {
		return contacts.values();
	}

	public Contact getContactWithId(String contactId) {
		return contacts.get(contactId);
	}

	public Contact getContactWithName(String contactName) {
		for (Contact contact : contacts.values())
			if (contact.getName().equals(contactName))
				return contact;
		return null;
	}

	public boolean addContact(String contactId) {
		if (hasContact(contactId)) return false;
		Contact contact = new Contact(contactId);
		if (!contact.lookup()) {
			Log.e("ContactsHelper", "Lookup failed for: " + contactId);
			return false;
		}
		contacts.put(contactId, contact);
		return updateContactIdsInPrefs();
	}
	
	private boolean updateContactIdsInPrefs() {
		String newValue = StringUtils.join(contacts.keySet(), ",");
		return prefs().edit().putString(CONTACT_IDS_PROPERTY_NAME, newValue).commit();
	}

	public boolean deleteContact(String contactId) {
		if (!hasContact(contactId)) return false;
		contacts.remove(contactId);
		return updateContactIdsInPrefs();
	}

	private SharedPreferences prefs() {
		return context.getSharedPreferences(PREFS_NAME, 0);
	}

	public boolean hasContact(String contactId) {
		return contacts.containsKey(contactId);
	}

	public class Contact {
		private String id;
		private String name;
		private String phone;
		private String email;

		public Contact(String id) {
			this.id = id;
		}

		public boolean lookup() {
			try {
				ContentResolver cr = context.getContentResolver();
				Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
									  null, ContactsContract.Contacts._ID + " = ?",
									  new String[]{id}, null);
				if (cur.moveToFirst()) {
					this.name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
						Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
										       null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
										       new String[]{id}, null);
						while (pCur.moveToNext()) {
							int phoneType = pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
							if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE){
								this.phone = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
								break;
							}
						}
						if (this.phone == null){ //we did not find a phone that is mobile phone
							Log.w("ContactsHelper", "pCur.moveTONext failed");
						}
					}
					Cursor eCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
										   null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
										   new String[]{id}, null);
					if (eCur.moveToNext()) {
						this.email = eCur.getString(eCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
					} else {
						Log.w("ContactsHelper", "eCur.moveTONext failed");
					}
					Log.d("ContactsHelper", toString());
					return true;
				} else {
					Log.w("ContactsHelper", "cur.moveToFirst() is false");
					return false;
				}
			} catch(Exception e) {
				Log.e("ContactsHelper", e.toString());
				return false;
			}
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getPhone() {
			return phone;
		}

		public String getEmail() {
			return email;
		}

		@Override
		public String toString() {
			return id + ": " + name + " (" + phone + ") <" + email + ">";
		}
	}
}
