package com.google.iamnotok;

import java.util.Vector;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;

/**
 * A simple list of contacts: list/add/remove.
 *
 * @author Ahmed Abdelkader (ahmadabdolkader@gmail.com)
 */
public class EmergencyContactsActivity extends ListActivity {
	private static final int CONTACT_PICKER_RESULT = 1001;
	private EmergencyContactsHelper contactsHelper;

	private Button emergencyButton = null;
	private Button cancelButton = null;
	private Button okButton = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts_list);

		contactsHelper = new EmergencyContactsHelper(getApplicationContext());
		setListAdapter(createAdapter());

		emergencyButton = (Button) findViewById(R.id.ContactListEmergencyButton);
		cancelButton = (Button) findViewById(R.id.ContactListCancelEmergencyButton);
		okButton = (Button) findViewById(R.id.ContactListImNowOKButton);

		setupEmergencyButtonViews();
		setupListView();
		registerReceivers();
	}

	private void registerReceivers() {
		// Register emergency notification service receiver.
		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d("EmergencyContactActivity.BroadcastReceiver",
				      "Action: " + intent.getAction());
				EmergencyContactsActivity.this.updateEmergencyButtonStatus();
			}
		};
		IntentFilter filter = new IntentFilter(EmergencyNotificationService.SERVICE_I_AM_NOW_OK_INTENT);
		registerReceiver(receiver, filter);
		filter = new IntentFilter(EmergencyNotificationService.SERVICE_I_AM_NOT_OK_INTENT);
		registerReceiver(receiver, filter);

	    // Register the Screen on/off receiver.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(getString(R.string.quiet_mode_enable), true)) {
			ScreenOnOffReceiver.register(getApplicationContext());
		}

	}

	protected void setupEmergencyButtonViews() {
		// Create intent to launch the EmergencyNotificationService and button.
	    final Intent intent = new Intent(this, EmergencyNotificationService.class);
	    intent.putExtra(EmergencyNotificationService.SHOW_NOTIFICATION_WITH_DISABLE, true);
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
				EmergencyContactsActivity.this.sendBroadcast(cancelEmergencyIntent);
				cancelButton.setVisibility(View.GONE);
				emergencyButton.setVisibility(View.VISIBLE);
			}
		});

	    // Create ImNowOk intent and button.
	    final Intent iAmNowOkIntent = new Intent(EmergencyNotificationService.I_AM_NOW_OK_INTENT);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EmergencyContactsActivity.this.sendBroadcast(iAmNowOkIntent);
			}
		});

		updateEmergencyButtonStatus();
	}

	protected void updateEmergencyButtonStatus() {
		emergencyButton.setVisibility(View.GONE);
	    cancelButton.setVisibility(View.GONE);
		okButton.setVisibility(View.GONE);
		switch (EmergencyNotificationService.mApplicationState) {
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
			public boolean onItemLongClick(final AdapterView<?> av, View v, final int pos, long id) {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
					public void onClick(DialogInterface dialog, int which) {
				        switch (which){
				        case DialogInterface.BUTTON_POSITIVE:
				        	// Remove contact.
							@SuppressWarnings("unchecked")
							ArrayAdapter<String> adapter = (ArrayAdapter<String>) getListAdapter();
							String contactName = adapter.getItem(pos);
							EmergencyContactsHelper.Contact contact = contactsHelper.getContactWithName(contactName);
							contactsHelper.deleteContact(contact.getId());
							adapter.remove(adapter.getItem(pos));
				            break;
				        case DialogInterface.BUTTON_NEGATIVE:
				            break;
				        }
				    }
				};
				AlertDialog.Builder builder = new AlertDialog.Builder(EmergencyContactsActivity.this);
				builder.setTitle("Delete contact");
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setMessage("Are you sure?")
				       .setPositiveButton("Yes", dialogClickListener)
				       .setNegativeButton("No", dialogClickListener)
				       .show();
				return false;
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
		Vector<String> a = new Vector<String>();
		ArrayAdapter<String> aa =
			new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, a);
		// Init with stored contacts, if any.
		for (String contactId : contactsHelper.contactIds()) {
			aa.add(contactsHelper.getContactWithId(contactId).getName());
		}
		return aa;
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
				@SuppressWarnings("unchecked")
				ArrayAdapter<String> adapter = (ArrayAdapter<String>) getListAdapter();
				EmergencyContactsHelper.Contact contact = contactsHelper.getContactWithId(contactId);
				Log.d("Hello", "Got: " + contact);
				if (contact == null) break;
				adapter.add(contact.getName());
				break;
			}
		} else {
			Log.w("Hello", "Activity result not ok");
		}
	}
}