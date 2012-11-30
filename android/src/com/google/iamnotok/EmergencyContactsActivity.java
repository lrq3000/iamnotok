package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

import com.google.iamnotok.EmergencyNotificationService.VigilanceState;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemLongClickListener;

/**
 * A simple list of contacts: list/add/remove.
 */
public class EmergencyContactsActivity extends ListActivity {
	private static final int CONTACT_PICKER_RESULT = 1001;
	private EmergencyContactsHelper contactsHelper;

	private Button emergencyButton = null;
	private Button cancelButton = null;
	private Button okButton = null;

	private BroadcastReceiver stateChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("EmergencyContactActivity.BroadcastReceiver", "Action: " + intent.getAction());
			EmergencyContactsActivity.this.updateEmergencyButtonStatus((VigilanceState) intent.getSerializableExtra(EmergencyNotificationService.NEW_STATE_EXTRA));
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts_list);

		contactsHelper = new EmergencyContactsHelper(this, new ContactLookupUtil());
		setListAdapter(createAdapter());

		emergencyButton = (Button) findViewById(R.id.ContactListEmergencyButton);
		cancelButton = (Button) findViewById(R.id.ContactListCancelEmergencyButton);
		okButton = (Button) findViewById(R.id.ContactListImNowOKButton);

		setupEmergencyButtonViews();
		setupListView();
		registerReceivers();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateEmergencyButtonStatus(EmergencyNotificationService.applicationState);
		registerReceiver(stateChangeReceiver, new IntentFilter(EmergencyNotificationService.STATE_CHANGE_INTENT));
	}

	@Override
	protected void onPause() {
		unregisterReceiver(stateChangeReceiver);
		super.onPause();
	}

	private void registerReceivers() {
		// TODO: Move to Manifest
		// Register the Screen on/off receiver.
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(getString(R.string.quiet_mode_enable), true)) {
			ScreenOnOffReceiver.register(getApplicationContext());
		}
	}

	protected void setupEmergencyButtonViews() {
		// Create intent to launch the EmergencyNotificationService and button.
		final Intent intent = new Intent(this,
				EmergencyNotificationService.class);
		intent.putExtra(
				EmergencyNotificationService.SHOW_NOTIFICATION_WITH_DISABLE,
				true);
		emergencyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(intent);
				emergencyButton.setVisibility(View.GONE);
				cancelButton.setVisibility(View.VISIBLE);

			}
		});

		// Create CancelEmergency intent and button.
		final Intent cancelEmergencyIntent = new Intent(
				EmergencyNotificationService.STOP_EMERGENCY_INTENT);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EmergencyContactsActivity.this
						.sendBroadcast(cancelEmergencyIntent);
				cancelButton.setVisibility(View.GONE);
				emergencyButton.setVisibility(View.VISIBLE);
			}
		});

		// Create ImNowOk intent and button.
		final Intent iAmNowOkIntent = new Intent(
				EmergencyNotificationService.I_AM_NOW_OK_INTENT);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EmergencyContactsActivity.this.sendBroadcast(iAmNowOkIntent);
			}
		});
	}

	protected void updateEmergencyButtonStatus(VigilanceState newStatus) {
		emergencyButton.setVisibility(View.GONE);
		cancelButton.setVisibility(View.GONE);
		okButton.setVisibility(View.GONE);
		switch (newStatus) {
		case NORMAL_STATE:
			emergencyButton.setVisibility(View.VISIBLE);
			break;
		case WAITING_STATE:
			cancelButton.setVisibility(View.VISIBLE);
			break;
		case EMERGENCY_STATE:
			okButton.setVisibility(View.VISIBLE);
			break;
		}
	}

	protected void setupListView() {
		// Long click to remove contacts.
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(final AdapterView<?> av, View v,
					final int pos, long id) {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE:
							// Remove contact.
							ContactAdapter adapter = (ContactAdapter) getListAdapter();
							Contact contact = (Contact) adapter.getItem(pos);
							contactsHelper.deleteContact(contact.getId());
							adapter.setList(new ArrayList<Contact>(
									contactsHelper.getAllContacts()));
							adapter.notifyDataSetChanged();
							break;
						case DialogInterface.BUTTON_NEGATIVE:
							break;
						}
					}
				};
				AlertDialog.Builder builder = new AlertDialog.Builder(
						EmergencyContactsActivity.this);
				builder.setTitle("Delete contact");
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setMessage("Are you sure?")
						.setPositiveButton("Yes", dialogClickListener)
						.setNegativeButton("No", dialogClickListener).show();
				return false;
			}
		});
		getListView().setOnItemClickListener(
				new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> av, View v, int pos,
							long id) {
						Intent intent = new Intent(
								EmergencyContactsActivity.this,
								ContactDetailChooserActivity.class);
						intent.putExtra(ContactDetailChooserActivity.EXTRA_ID, id);
						startActivity(intent);
					}

				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contacts_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.add_contact) {
			// Launch contact picker.
			Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
					android.provider.ContactsContract.Contacts.CONTENT_URI);
			startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
		} else if (item.getItemId() == R.id.preferences) {
			// Send of an intent to start off the ApplicationSettingsActivity
			Intent intent = new Intent(EmergencyContactsActivity.this,
					ApplicationSettingsActivity.class);
			EmergencyContactsActivity.this.startActivity(intent);
		}
		return super.onMenuItemSelected(featureId, item);
	}

	protected ListAdapter createAdapter() {
		List<Contact> list = new ArrayList<Contact>(
				contactsHelper.getAllContacts());
		return new ContactAdapter(this, list);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				// Extract contact id.
				String contactId = data.getData().getLastPathSegment();
				// Try to store it.
				Log.d("Hello", "Add: " + contactId);
				if (!contactsHelper.addContact(contactId)) {
					Log.e("Hello", "Add failed.");
					break;
				}
				ContactAdapter adapter = (ContactAdapter) getListAdapter();
				adapter.setList(new ArrayList<Contact>(contactsHelper.getAllContacts()));
				adapter.notifyDataSetChanged();
				break;
			}
		} else {
			Log.w("Hello", "Activity result not ok");
		}
	}

	private static class ContactAdapter extends BaseAdapter {

		private List<Contact> list;
		private LayoutInflater mInflater;

		private ContactAdapter(Context context, List<Contact> list) {
			this.list = list;
			mInflater = LayoutInflater.from(context);
		}

		public void setList(List<Contact> newList) {
			this.list = newList;
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
			return Long.parseLong(list.get(pos).getId());
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Contact friend = list.get(position);

			View v = null;
			if (convertView != null) {
				v = convertView;
			} else {
				v = mInflater.inflate(R.layout.contact_line, null);
			}
			TextView textView = (TextView) v.findViewById(R.id.name);
			textView.setText(friend.getName());

			String phone = friend.getPhone();
			v.findViewById(R.id.phone).setVisibility(
					phone == null ? View.GONE : View.VISIBLE);
			TextView phoneView = (TextView) v.findViewById(R.id.phone_value);
			if (phone != null) {
				phoneView.setText(phone);
			}

			String email = friend.getEmail();
			v.findViewById(R.id.email).setVisibility(
					email == null ? View.GONE : View.VISIBLE);
			if (email != null) {
				TextView emailView = (TextView) v
						.findViewById(R.id.email_value);
				emailView.setText(email);
			}
			return v;
		}

	}

}