package com.google.iamnotok;

import java.util.LinkedList;
import java.util.List;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ContactDetailChooserActivity extends ListActivity {

	public static final String EXTRA_ID = "com.google.iamnotok.extra_id";

	private static final String[] PROJECTION = new String[] { Data._ID,
			Data.IS_SUPER_PRIMARY, Data.DISPLAY_NAME, Phone.LABEL, Phone.TYPE, Phone.NUMBER,
			 Email.LABEL, Email.TYPE, Email.DATA, Data.MIMETYPE };

	private static final int COL_PHONE_LABEL = 3;
	private static final int COL_PHONE_TYPE = 4;
	private static final int COL_PHONE_NUMBER = 5;
	private static final int COL_EMAIL_LABEL = 6;
	private static final int COL_EMAIL_TYPE = 7;
	private static final int COL_EMAIL = 8;

	private static final String WHERE_CLAUSE = Data.CONTACT_ID + " = ? AND ("
			+ Data.MIMETYPE + " =? OR " + Data.MIMETYPE + " =?)";

	private long contactId;

	private List<DetailItem> itemList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detail_chooser);

		contactId = getIntent().getLongExtra(EXTRA_ID, -1);
		if (contactId < 0) {
			finish();
		}
		queryInfo();
		setListAdapter(new DetailsAdapter(this, itemList));
	}

	private void queryInfo() {
		ContentResolver cr = getContentResolver();
		final String[] whereArgs = new String[] { String.valueOf(contactId),
				CommonDataKinds.Email.CONTENT_ITEM_TYPE,
				CommonDataKinds.Phone.CONTENT_ITEM_TYPE };

		Cursor cur = cr.query(Data.CONTENT_URI, PROJECTION, WHERE_CLAUSE,
				whereArgs, Data.MIMETYPE);
		itemList = new LinkedList<ContactDetailChooserActivity.DetailItem>();
		if (cur != null) {
			try {
				final int mimetypeCol = cur.getColumnIndex(Data.MIMETYPE);
				while (cur.moveToNext()) {
					final String mimetype = cur.getString(mimetypeCol);
					if (mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
						final int type = cur.getInt(COL_PHONE_TYPE);
						String typeLabel = null;
						typeLabel = cur.getString(COL_PHONE_LABEL);
						if (typeLabel == null) {
							typeLabel = getString(Phone
									.getTypeLabelResource(type));
						}
						itemList.add(new DetailItem(typeLabel, cur
								.getString(COL_PHONE_NUMBER)));
					} else if (mimetype.equals(Email.CONTENT_ITEM_TYPE)) {
						final int type = cur.getInt(COL_EMAIL_TYPE);
						String typeLabel = null;
						typeLabel = cur.getString(COL_EMAIL_LABEL);
						if (typeLabel == null) {
							typeLabel = getString(Email
									.getTypeLabelResource(type));
						}
						itemList.add(new DetailItem(typeLabel, cur
								.getString(COL_EMAIL)));
					}

				}
			} finally {
				cur.close();
			}
		}

	}

	private static class DetailItem {
		private String type;
		private String data;

		public DetailItem(String type, String data) {
			this.type = type;
			this.data = data;
		}
	}

	private static class DetailsAdapter extends BaseAdapter {

		private List<DetailItem> list;
		private LayoutInflater mInflater;

		private DetailsAdapter(Context context, List<DetailItem> list) {
			this.list = list;
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int pos) {
			return list.get(pos);
		}

		@Override
		public long getItemId(int pos) {
			return pos;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			DetailItem item = list.get(position);

			View v = null;
			if (convertView != null) {
				v = convertView;
			} else {
				v = mInflater.inflate(R.layout.detail_line, null);
			}

			TextView view = (TextView) v.findViewById(R.id.type);
			view.setText(item.type);
			view = (TextView) v.findViewById(R.id.data);
			view.setText(item.data);
			return v;
		}

	}

}
