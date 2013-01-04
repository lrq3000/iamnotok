package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.iamnotok.Preferences.VigilanceState;
import com.google.iamnotok.utils.AccountUtils;

/**
 * A simple list of contacts: list/add/remove.
 */
public class EmergencyContactsActivity extends ListActivity implements OnSharedPreferenceChangeListener {
	private static final int CONTACT_PICKER_RESULT = 1001;
	private Application application;

	private Button emergencyButton = null;
	private Button cancelButton = null;
	private Button okButton = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts_list);

		if (Build.VERSION.SDK_INT >= 11) {
			updateActionBar();
		}

		application = (Application) getApplication();
		setListAdapter(createAdapter());

		emergencyButton = (Button) findViewById(R.id.ContactListEmergencyButton);
		cancelButton = (Button) findViewById(R.id.ContactListCancelEmergencyButton);
		okButton = (Button) findViewById(R.id.ContactListImNowOKButton);

		setupEmergencyButtonViews();
		setupListView();
		registerReceivers();
	}

	@TargetApi(11)
	private void updateActionBar() {
		getActionBar().setSubtitle(new AccountUtils(this).getAccountName());
		// TODO: Move to using navigationList here.
	}

	@Override
	protected void onResume() {
		super.onResume();
		Preferences pref = new Preferences(this);
		updateEmergencyButtonStatus(pref.getVigilanceState());
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.VIGILANCE_STATE_KEY)) {
			Preferences pref = new Preferences(this);
			updateEmergencyButtonStatus(pref.getVigilanceState());
		} else if (key.equals(Preferences.ACCOUNT_NAME_KEY)) {
			if (Build.VERSION.SDK_INT >= 11) {
				updateActionBar();
			}
		}
	}

	private void registerReceivers() {
		// TODO: Move to Manifest
		// Register the Screen on/off receiver.
		Preferences prefs = new Preferences(this);
		if (prefs.getQuiteMode()) {
			ScreenOnOffReceiver.register(getApplicationContext());
		}
	}

	protected void setupEmergencyButtonViews() {
		// Create intent to launch the EmergencyNotificationService and button.
		emergencyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(EmergencyNotificationService.getStartIntent(EmergencyContactsActivity.this)
						.putExtra(
								EmergencyNotificationService.SHOW_NOTIFICATION_WITH_DISABLE,
								true));
				emergencyButton.setVisibility(View.GONE);
				cancelButton.setVisibility(View.VISIBLE);

			}
		});

		// Create CancelEmergency intent and button.
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(EmergencyNotificationService.getCancelIntent(EmergencyContactsActivity.this));
				cancelButton.setVisibility(View.GONE);
				emergencyButton.setVisibility(View.VISIBLE);
			}
		});

		// Create ImNowOk intent and button.
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(EmergencyNotificationService.getStopIntent(EmergencyContactsActivity.this));
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
							application.deleteContact(contact.getID());
							adapter.setList(application.getAllContacts());
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
		return new ContactAdapter(this, application.getAllContacts());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				String contactSystemID = intent.getData().getLastPathSegment();
				if (!application.addContact(contactSystemID)) {
					break;
				}
				ContactAdapter adapter = (ContactAdapter) getListAdapter();
				adapter.setList(application.getAllContacts());
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
			return Long.parseLong(list.get(pos).getSystemID());
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

			// XXX Show SMS label if contacts has phones
			List<String> phones = friend.getEnabledPhones();
			v.findViewById(R.id.phone).setVisibility(
					phones.isEmpty() ? View.GONE : View.VISIBLE);
			if (!phones.isEmpty()) {
				TextView phoneView = (TextView) v.findViewById(R.id.phone_value);
				phoneView.setText(phones.get(0));
			}

			// XXX Show EMAIL label if contacts has emails
			List<String> emails = friend.getEnabledEmails();
			v.findViewById(R.id.email).setVisibility(
					emails.isEmpty() ? View.GONE : View.VISIBLE);
			if (!emails.isEmpty()) {
				TextView emailView = (TextView) v
						.findViewById(R.id.email_value);
				emailView.setText(emails.get(0));
			}
			return v;
		}

	}

}