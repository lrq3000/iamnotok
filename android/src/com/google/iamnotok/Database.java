package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

import com.google.iamnotok.Contact.Attribute;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class Database {
	
	private static final String LOG = "Database";
	
	private static final String NAME = "iamnotok.db";
	private static final int VERSION = 2;
	
	private static final String CONTACT_TABLE = "contact";
	private static final String CONTACT_ID = "_id";
	private static final String CONTACT_SYSTEM_ID = "system_id";
	private static final String CONTACT_NAME = "name";

	private static final String NOTIFICATION_TABLE = "notification";
	private static final String NOTIFICATION_ID = "_id";
	private static final String NOTIFICATION_CONTACT_ID = "contact_id";
	private static final String NOTIFICATION_TYPE = "type";
	private static final String NOTIFICATION_TARGET = "target";
	private static final String NOTIFICATION_LABEL = "label";
	
	private static final String CONTACT_NOTIFICTION_INDEX = "contact_notification";
	
	private static final String NOTIFICATION_TYPE_EMAIL = "EMAIL";
	private static final String NOTIFICATION_TYPE_SMS = "SMS";
	
	static class Helper extends SQLiteOpenHelper {

		public Helper(Context context) {
			super(context, NAME, null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(LOG, "creating database");
			db.execSQL("create table " + CONTACT_TABLE + " ("
					    + CONTACT_ID + " integer primary key autoincrement not null, "
						+ CONTACT_SYSTEM_ID + " text not null, "
						+ CONTACT_NAME + " text not null"
						+ " )");
			
			db.execSQL("create table " + NOTIFICATION_TABLE + " ( "
						+ NOTIFICATION_ID + " integer primary key autoincrement not null, "
						+ NOTIFICATION_CONTACT_ID + " integer not null references " + CONTACT_TABLE + "(" + CONTACT_ID + "), "
						+ NOTIFICATION_TYPE + " text not null, "
						+ NOTIFICATION_TARGET + " text not null, "
						+ NOTIFICATION_LABEL + " text not null"
					+ ")");
			
			// Ensure that there is only one notification per contact, type and target
			db.execSQL("create unique index " + CONTACT_NOTIFICTION_INDEX + " on " + NOTIFICATION_TABLE + "("
						+ NOTIFICATION_CONTACT_ID + ", "
						+ NOTIFICATION_TYPE + ", "
						+ NOTIFICATION_TARGET
					+ ")");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(LOG, "upgrading database from version: " + oldVersion + " to version: " + newVersion);
			// XXX Temporary "upgrade" until we ship the application.
			clear(db);
			onCreate(db);
		}
		
		@Override
		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(LOG, "downgrading database from version: " + oldVersion + " to version: " + newVersion);
			// I don't see better solution for downgrade - we cannot know the schema of a future version.
			clear(db);
			onCreate(db);
		}
		
		private void clear(SQLiteDatabase db) {
			// Drop indexes and tables we are going to create.
			db.execSQL("drop index if exists " + CONTACT_NOTIFICTION_INDEX);
			db.execSQL("drop table if exists " + NOTIFICATION_TABLE);
			db.execSQL("drop table if exists " + CONTACT_TABLE);
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
				"select " + CONTACT_ID + "," + CONTACT_SYSTEM_ID + "," + CONTACT_NAME
				+ " from " + CONTACT_TABLE
				+ " order by " + CONTACT_ID, 
				null);
		try {
			while (cursor.moveToNext()) {
				long id = cursor.getLong(0);
				String systemID = cursor.getString(1);
				String name = cursor.getString(2);
				List<Attribute> phones = getNotifications(id, NOTIFICATION_TYPE_SMS);
				List<Attribute> emails = getNotifications(id, NOTIFICATION_TYPE_EMAIL);
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
			for (Attribute phone : contact.getPhones()) {
				addNotification(id, NOTIFICATION_TYPE_SMS, phone.value, phone.label);
			}
			for (Attribute email : contact.getEmails()) {
				addNotification(id, NOTIFICATION_TYPE_EMAIL, email.value, email.label);
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
	
	private List<Attribute> getNotifications(long id, String type) {
		SQLiteDatabase db = helper.getWritableDatabase();
		List<Attribute> result = new ArrayList<Attribute>();
		String[] args = {String.valueOf(id), type};
		Cursor cursor = db.rawQuery(
				"select " + NOTIFICATION_TARGET + "," + NOTIFICATION_LABEL
				+ " from " + NOTIFICATION_TABLE
				+ " where " + NOTIFICATION_CONTACT_ID + "=? and " + NOTIFICATION_TYPE + "=? "
				+ " order by " + NOTIFICATION_ID, 
				args);
		try {
			while (cursor.moveToNext()) {
				String value = cursor.getString(0);
				String label = cursor.getString(1);
				result.add(new Attribute(value, label));
			}
		} finally {
			cursor.close();
		}
		return result;
	}

	private void addNotification(long contactID, String type, String target, String label) {
		Log.d(LOG, "adding notification contactID: " + contactID + " type: " + type 
				+ " target: " + target + " label: " + label);
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(NOTIFICATION_CONTACT_ID, contactID);
		values.put(NOTIFICATION_TYPE, type);
		values.put(NOTIFICATION_TARGET, target);
		values.put(NOTIFICATION_LABEL, label);
		db.insertOrThrow(NOTIFICATION_TABLE, null, values);
	}

}
