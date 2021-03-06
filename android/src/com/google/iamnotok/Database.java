package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

import com.google.iamnotok.utils.StringUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class Database {
	
	private static final String LOG = "Database";
	
	private static final String NAME = "iamnotok.db";
	private static final int VERSION = 3;
	
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
	private static final String NOTIFICATION_ENABLED = "enabled";
	
	private static final String CONTACT_NOTIFICTION_INDEX = "contact_notification";
		
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
						+ NOTIFICATION_LABEL + " text not null, "
						+ NOTIFICATION_ENABLED + " integer not null"
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
				List<Notification> phones = getNotifications(id, Notification.TYPE_SMS);
				List<Notification> emails = getNotifications(id, Notification.TYPE_EMAIL);
				Contact c = new Contact(id, systemID, name, phones, emails);
				Log.d(LOG, "loding " + c);
				result.add(c);
			}
		} finally {
			cursor.close();
		}
		return result;
	}

	public void insertContact(Contact contact) {
		Log.d(LOG, "inserting " + contact);
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(CONTACT_SYSTEM_ID, contact.getSystemID());
			values.put(CONTACT_NAME, contact.getName());
			long id = db.insertOrThrow(CONTACT_TABLE, null, values);
			for (Notification phone : contact.getSMSNotifications()) {
				insertNotification(id, phone);
			}
			for (Notification email : contact.getEmailNotifications()) {
				insertNotification(id, email);
			}
			db.setTransactionSuccessful();
			// New contact is always dirty
			contact.setID(id);
			contact.beClean();
		} finally {
			db.endTransaction();
		}
	}
	
	public void updateContact(Contact contact) {
		Log.d(LOG, "updating " + contact);
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(CONTACT_NAME, contact.getName());
			String[] args = {String.valueOf(contact.getID())};
			db.update(CONTACT_TABLE, values, CONTACT_ID + "=?", args);
			deleteRemovedNotifications(contact);
			for (Notification n : contact.getSMSNotifications()) {
				if (n.getID() == Notification.NO_ID)
					insertNotification(contact.getID(), n);
				else if (n.isDirty())
					updateNotification(n);
			}
			for (Notification n : contact.getEmailNotifications()) {
				if (n.getID() == Notification.NO_ID)
					insertNotification(contact.getID(), n);
				else if (n.isDirty())
					updateNotification(n);
			}
			db.setTransactionSuccessful();
			contact.beClean();
		} finally {
			db.endTransaction();
		}
	}

	public void deleteContact(long id) {
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

	// Accessing notifications
	
	private List<Notification> getNotifications(long contactID, String type) {
		SQLiteDatabase db = helper.getWritableDatabase();
		List<Notification> result = new ArrayList<Notification>();
		String[] args = {String.valueOf(contactID), type};
		Cursor cursor = db.rawQuery(
				"select " + NOTIFICATION_ID + "," 
						  + NOTIFICATION_TARGET + "," 
						  + NOTIFICATION_LABEL + "," 
						  + NOTIFICATION_ENABLED
				+ " from " + NOTIFICATION_TABLE
				+ " where " + NOTIFICATION_CONTACT_ID + "=? and " 
							+ NOTIFICATION_TYPE + "=?"
				+ " order by " + NOTIFICATION_ID, 
				args);
		try {
			while (cursor.moveToNext()) {
				long id = cursor.getLong(0);
				String target = cursor.getString(1);
				String label = cursor.getString(2);
				boolean enabled = cursor.getInt(3) == 1;
				Notification n =new Notification(id, type, target, label, enabled); 
				Log.d(LOG, "loading " + n);
				result.add(n);
			}
		} finally {
			cursor.close();
		}
		return result;
	}

	private void insertNotification(long contactID, Notification n) {
		Log.d(LOG, "adding notification contactID: " + contactID 
				+ " type: " + n.getType()
				+ " target: " + n.getTarget() 
				+ " label: " + n.getLabel() 
				+ " enabled: " + n.isEnabled());
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(NOTIFICATION_CONTACT_ID, contactID);
		values.put(NOTIFICATION_TYPE, n.getType());
		values.put(NOTIFICATION_TARGET, n.getTarget());
		values.put(NOTIFICATION_LABEL, n.getLabel());
		values.put(NOTIFICATION_ENABLED, n.isEnabled() ? 1 : 0);
		long id = db.insertOrThrow(NOTIFICATION_TABLE, null, values);
		n.setID(id);
	}

	private void updateNotification(Notification n) {
		Log.d(LOG, "updating " + n);
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(NOTIFICATION_LABEL, n.getLabel());
		values.put(NOTIFICATION_ENABLED, n.isEnabled() ? 1 : 0);
		String[] args = {String.valueOf(n.getID())};
		db.update(NOTIFICATION_TABLE, values, NOTIFICATION_ID + "=?", args);
	}
	
	private void deleteRemovedNotifications(Contact contact) {
		Log.d(LOG, "deleting removed notifications for contact: " + contact.getID());
		List<String> ids = new ArrayList<String>();
		for (Notification n : contact.getSMSNotifications()) {
			ids.add(String.valueOf(n.getID()));
		}
		for (Notification n : contact.getEmailNotifications()) {
			ids.add(String.valueOf(n.getID()));
		}
		String where = NOTIFICATION_CONTACT_ID + "=" + contact.getID()
				+ " and " + NOTIFICATION_ID + " not in ("
				+ StringUtils.join(ids, ",") + ")";
		SQLiteDatabase db = helper.getWritableDatabase();
		db.delete(NOTIFICATION_TABLE, where, null);
	}

}
