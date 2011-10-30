package com.google.iamnotok;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

public class Contact {
		
		private Context context;
		
		private String id;
		private String name;
		private String phone;
		private String email;

		public Contact(Context context, String id) {
			this.id = id;
			this.context = context;
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