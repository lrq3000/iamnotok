package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class Database {
	
	private static final String LOG = "Database";
	
	private static final String NAME = "iamnotok.db";
	private static final int VERSION = 1;
	
	private static final String CONTACT_TABLE = "contact";
	private static final String CONTACT_ID = "_id";
	private static final String CONTACT_SYSTEM_ID = "system_id";
	private static final String CONTACT_NAME = "name";

	private static final String NOTIFICATION_TABLE = "notification";
	private static final String NOTIFICATION_ID = "_id";
	private static final String NOTIFICATION_CONTACT_ID = "contact_id";
	private static final String NOTIFICATION_TYPE = "type";
	private static final String NOTIFICATION_TARGET = "target";

	private static final String NOTIFICATION_TYPE_EMAIL = "EMAIL";
	private static final String NOTIFICATION_TYPE_SMS = "SMS";
	
	static class Helper extends SQLiteOpenHelper {

		public Helper(Context context) {
			super(context, NAME, null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(LOG, "creating database");
			db.execSQL("create table " + CONTACT_TABLE + " ("
					    + CONTACT_ID + " integer primary key autoincrement not null, "
						+ CONTACT_SYSTEM_ID + " text not null, "
						+ CONTACT_NAME + " text not null"
						+ " )");
			db.execSQL("create table " + NOTIFICATION_TABLE + " ( "
						+ NOTIFICATION_ID + " integer primary key autoincrement not null, "
						+ NOTIFICATION_CONTACT_ID + " text not null references " + CONTACT_TABLE + "(" + CONTACT_ID + "), "
						+ NOTIFICATION_TYPE + " text not null, "
						+ NOTIFICATION_TARGET + " text not null "
					+ ")");
			db.execSQL("create unique index contact_notification on " + NOTIFICATION_TABLE + "("
						+ NOTIFICATION_CONTACT_ID + ", "
						+ NOTIFICATION_TYPE + ", "
						+ NOTIFICATION_TARGET
					+ ")");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(LOG, "upgrading database from version: " + oldVersion + " to version: " + newVersion);
		}
	}

	private final Helper helper;
	
	public Database(Context context) {
		helper = new Helper(context);
	}
	
	public void close() {
		helper.close();
	}
	
	// Accessing contacts
	
	public List<Contact> getAllContacts() {
		SQLiteDatabase db = helper.getWritableDatabase();
		List<Contact> result = new ArrayList<Contact>();
		Cursor cursor = db.rawQuery(
				"select * from " + CONTACT_TABLE + " order by " + CONTACT_ID, 
				null);
		try {
			while (cursor.moveToNext()) {
				long id = cursor.getLong(0);
				String systemID = cursor.getString(1);
				String name = cursor.getString(2);
				List<String> phones = getNotifications(id, NOTIFICATION_TYPE_SMS);
				List<String> emails = getNotifications(id, NOTIFICATION_TYPE_EMAIL);
				result.add(new Contact(id, systemID, name, phones, emails));
			}
		} finally {
			cursor.close();
		}
		return result;
	}

	public void addContact(Contact contact) {
		Log.d(LOG, "adding contact: " + contact);
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(CONTACT_SYSTEM_ID, contact.getSystemID());
			values.put(CONTACT_NAME, contact.getName());
			long id = db.insertOrThrow(CONTACT_TABLE, null, values);
			// XXX Handle multiple phones and email addresses
			String phone = contact.getSelectedPhone();
			if (phone != null) {
				addNotification(id, NOTIFICATION_TYPE_SMS, phone);
			}
			String email = contact.getSelectedEmail();
			if (email != null) {
				addNotification(id, NOTIFICATION_TYPE_EMAIL, email);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	public void deleteContactWithID(long id) {
		Log.d(LOG, "deleting contact: " + id);
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			String[] args = {String.valueOf(id)};
			// Must delete notifications first as they reference the contact id
			db.delete(NOTIFICATION_TABLE, NOTIFICATION_CONTACT_ID + "=?", args);
			db.delete(CONTACT_TABLE, CONTACT_ID + "=?", args);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}		
	}

	public boolean containsContactWithSystemID(String systemID) {
		SQLiteDatabase db = helper.getWritableDatabase();
		String[] args = {systemID};
		Cursor cursor = db.rawQuery(
				"select " + CONTACT_ID + " from " + CONTACT_TABLE + " where " + CONTACT_SYSTEM_ID + "=?",
				args);
		try {
			return cursor.getCount() > 0;
		} finally {
			cursor.close();
		}
	}

	// Accessing notifications
	
	private List<String> getNotifications(long id, String type) {
		SQLiteDatabase db = helper.getWritableDatabase();
		List<String> result = new ArrayList<String>();
		String[] args = {String.valueOf(id), type};
		Cursor cursor = db.rawQuery(
				"select " + NOTIFICATION_TARGET
				+ " from " + NOTIFICATION_TABLE
				+ " where " + NOTIFICATION_CONTACT_ID + "=? and " + NOTIFICATION_TYPE + "=? "
				+ " order by " + NOTIFICATION_ID, 
				args);
		try {
			while (cursor.moveToNext()) {
				result.add(cursor.getString(0));
			}
		} finally {
			cursor.close();
		}
		return result;
	}

	private void addNotification(long contactID, String type, String target) {
		Log.d(LOG, "adding notification contactID: " + contactID + " type: " + type + " target: " + target);
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(NOTIFICATION_CONTACT_ID, contactID);
		values.put(NOTIFICATION_TYPE, type);
		values.put(NOTIFICATION_TARGET, target);
		db.insertOrThrow(NOTIFICATION_TABLE, null, values);
	}

}
