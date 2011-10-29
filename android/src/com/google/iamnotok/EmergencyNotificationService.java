package com.google.iamnotok;

import java.util.*;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.database.Cursor;
import android.location.*;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.iamnotok.EmergencyContactsHelper.Contact;
import com.google.iamnotok.LocationTracker.LocationAddress;
import com.google.iamnotok.utils.LocationUtils;

/**
 * Puts the phone to the emergency state and notifies the contacts in the
 * emergency contacts' list about the situation.
 * 
 * @author Vytautas
 * @author Raquel
 * @author igalk
 * @author nadir
 */
public class EmergencyNotificationService extends Service {
	private final static String mLogTag = "ImNotOk - EmergencyNotificationService";

	/**
	 * Field name for the boolean that should be passed with the intent to start
	 * this service. It tells whether the notification in the top bar should be
	 * shown. This notification should be against accidental triggering of
	 * emergency. It would allow a user to disable the emergency response within
	 * 10 seconds.
	 */
	public final static String SHOW_NOTIFICATION_WITH_DISABLE = "showNotification";
	private final static String STOP_EMERGENCY_INTENT = "com.google.imnotok.STOP_EMERGENCY";
	public final static String I_AM_NOW_OK_INTENT = "com.google.imnotok.I_AM_NOW_OK";

	public final static int NORMAL_STATE = 0;
	public final static int WAITING_STATE = 1;
	public final static int EMERGENCY_STATE = 2;
	public static int mApplicationState = NORMAL_STATE;

	/** Default time allowed for user to cancel the emergency response. */
	private static int DEFAULT_WAIT_TO_CANCEL = 10000; // milliseconds

	private int mNotificationID = 0;
	private LocationTracker mLocationTracker;
	private LocationUtils mLocationUtils;
	private boolean mNotifyViaSMS = true;
	private boolean mNotifyViaEmail = true;
	private boolean mNotifyViaCall = false;
	private int mWaitBetweenMessages = 5000;
	
	private EmergencyContactsHelper contactHelper;


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mLocationUtils = new LocationUtils();
		mLocationTracker = new LocationTrackerImpl(
				(LocationManager) this.getSystemService(Context.LOCATION_SERVICE),
				mLocationUtils,
				new Geocoder(this, Locale.getDefault()));
		mLocationTracker.setDistanceThresholdListener(new LocationTracker.DistanceThresholdListener() {
			@Override
			public void notify(LocationTracker.LocationAddress locationAddress) {
				onDistanceThresholdPassed(locationAddress);
			}
		});
	}

	protected void onDistanceThresholdPassed(LocationAddress locationAddress) {
		sendEmergencyMessages(locationAddress);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(mLogTag, "onStart() called");

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		mNotifyViaSMS = prefs.getBoolean(
				getString(R.string.checkbox_sms_notification), true);
		mNotifyViaEmail = prefs.getBoolean(
				getString(R.string.checkbox_email_notification), true);
		mNotifyViaCall = prefs.getBoolean(
				getString(R.string.checkbox_call_notification), false);
		
		if (contactHelper == null)
			contactHelper = new EmergencyContactsHelper(getApplicationContext());

		if (!(mNotifyViaCall || mNotifyViaEmail || mNotifyViaSMS)) {
			Toast.makeText(getApplicationContext(),
					R.string.no_notification_defined, Toast.LENGTH_LONG).show();
			return;
		}

		contactHelper.contactIds();

		if (this.getState() == NORMAL_STATE) {
			Log.d(mLogTag, "Starting the service");
			changeState(WAITING_STATE);
			boolean showNotification = true;
			if (intent != null) {
				showNotification = intent.getBooleanExtra(
						SHOW_NOTIFICATION_WITH_DISABLE, false);
			}

			// Get instance of Vibrator from current Context
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

			// Vibrate for 300 milliseconds
			v.vibrate(300);

			// Start the location tracker (nothing happens if called twice).
			mLocationTracker.activate();


			if (showNotification) {
				this.showDisableNotificationAndWaitToInvokeResponse();
			} else {
				this.invokeEmergencyResponse();
			}
			super.onStart(intent, startId);
		} else {
			Log.d(mLogTag,
					"Application already in either waiting or emergency mode.");
		}
	}
	
	private Account getSelectedAccount() {
		// First try to get the selected account from the preferences.
		SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
		String accountName = prefs.getString(
				getString(R.string.select_account_list), "");
		Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
		for (Account account : accounts) {
			if (account.name == accountName) {
				return account;
			}
		}
		
		// If we got here, then we didn't find the account in the preferences.
		// This probably means that the user removed the account after selecting it.
		// In this case, we just return the first available account, if any.
		// TODO: Should we notify the user here?
		if (accounts.length > 0) {
			return accounts[0];
		}
		
		// If there are no google accounts, try looking for a non-google account.
		// NOTE: This can only be used to get the name from.
		accounts = AccountManager.get(this).getAccounts();
		if (accounts.length > 0) {
			return accounts[0];
		}
		
		return new Account("Unidentified User", "com.dummy");
	}
	
	private String getPhoneNumber() {
		// First try to get the phone number from the preferences.
		SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
		String phoneNumber = prefs.getString(
				getString(R.string.account_phone_number), "");
		if (phoneNumber != null && !phoneNumber.equals("")) {
			return phoneNumber;
		}
		
		// If we don't have a phone number in the preferences, try getting it
		// from the weird function called getLine1Number.
		TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String lineNumber = telMgr.getLine1Number();
		if (lineNumber != null && !lineNumber.equals("")) {
			return lineNumber;
		}
		
		return "Unidentified Phone Number";
	}

	private void sendTextNotifications(LocationAddress locationAddress) {
		for (Contact contact : contactHelper.getAllContacts()) {
			if (contact.getPhone() != null) {
				sendTextMessage(contact.getPhone(), locationAddress);
			}
		}
	}

	private void sendEmailNotifications(LocationAddress locationAddress) {
		List<String> emailList = getAllContactEmails();
		if (emailList.size() > 0) {
			sendEmailMessage(emailList, locationAddress);
		}
	}

	private List<String> getAllContactEmails() {
		List<String> emails = new ArrayList<String>();
		for (Contact contact : contactHelper.getAllContacts()) {
			if (contact.getEmail() != null) {
				emails.add(contact.getEmail());
			}
		}
		return emails;
	}
	
	/**
	 * Sends an SMS to another device
	 **/
	private void sendTextMessage(final String phoneNumber, final LocationAddress locationAddress) {
		Thread messageSender = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(mLogTag, "Sending sms to: " + phoneNumber);
				String message = "";
				if (getState() == NORMAL_STATE) {
					message = "I am OK now";
					Log.d(mLogTag, "Sending the message " + message);
				} else {
					Log.d(mLogTag, "Getting location");
					message = formatMessage(locationAddress);
				}

				String SENT = "SMS_SENT";
				String DELIVERED = "SMS_DELIVERED";

				PendingIntent sentPI = PendingIntent.getBroadcast(
						EmergencyNotificationService.this, 0, new Intent(SENT),
						0);
				PendingIntent deliveredPI = PendingIntent.getBroadcast(
						EmergencyNotificationService.this, 0, new Intent(
								DELIVERED), 0);

				// ---when the SMS has been sent---
				registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context arg0, Intent arg1) {
						switch (getResultCode()) {
						case Activity.RESULT_OK:
							Toast.makeText(getBaseContext(), "SMS sent",
									Toast.LENGTH_SHORT).show();
							break;
						case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
							Toast.makeText(getBaseContext(), "Generic failure",
									Toast.LENGTH_SHORT).show();
							break;
						case SmsManager.RESULT_ERROR_NO_SERVICE:
							Toast.makeText(getBaseContext(), "No service",
									Toast.LENGTH_SHORT).show();
							break;
						case SmsManager.RESULT_ERROR_NULL_PDU:
							Toast.makeText(getBaseContext(), "Null PDU",
									Toast.LENGTH_SHORT).show();
							break;
						case SmsManager.RESULT_ERROR_RADIO_OFF:
							Toast.makeText(getBaseContext(), "Radio off",
									Toast.LENGTH_SHORT).show();
							break;
						}
					}
				}, new IntentFilter(SENT));

				// ---when the SMS has been delivered---
				registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context arg0, Intent arg1) {
						switch (getResultCode()) {
						case Activity.RESULT_OK:
							Toast.makeText(getBaseContext(), "SMS delivered",
									Toast.LENGTH_SHORT).show();
							break;
						case Activity.RESULT_CANCELED:
							Toast.makeText(getBaseContext(),
									"SMS not delivered", Toast.LENGTH_SHORT)
									.show();
							break;
						}
					}
				}, new IntentFilter(DELIVERED));

				SmsManager sms = SmsManager.getDefault();
				sms.sendTextMessage(phoneNumber, null, message, sentPI,
						deliveredPI);

			}

		});
		messageSender.start();
	}

	private void callEmergency() {
		String number = null;
		for (Contact contact : contactHelper.getAllContacts()) {
			number = contact.getPhone();
			if (number != null) {
				break;
			}
		}
		if (number == null) {
			Log.w(mLogTag,
					"Unable to find a contact with numnber, disabled emergency call");
			return;
		}
		Intent i = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number,
				null));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}

	private String getMapUrl(LocationTracker.LocationAddress locAddr) {
		String template = "http://maps.google.com/maps?f=q&source=s_q&hl=en&geocode=&q=%f,%f&sll=%f,%f&sspn=0.005055,0.009645&ie=UTF8&z=16";
		return String.format(template, locAddr.location.getLatitude(), locAddr.location.getLongitude(),
				locAddr.location.getLatitude(), locAddr.location.getLongitude());
	}

	/**
	 * Sends an email
	 */
	private void sendEmailMessage(List<String> to, LocationAddress locationAddress) {
		String recipients = formatRecipients(to);
		Log.d(mLogTag, "Sending email to: " + to);
		String subject = formatSubject();
		String message = "";
		if (getState() == NORMAL_STATE) {
			message = "I am OK now";
			Log.d(mLogTag, "Sending the email " + message);
		} else {
			Log.d(mLogTag, "Getting location");
			message = formatMessage(locationAddress);
			if (locationAddress != null) {
				message += " " + getMapUrl(locationAddress);
			}
		}

		try {
			GMailSender sender = new GMailSender("imnotokandroidapplication@gmail.com", "googlezurich");
			String mailAddress = getMailAddress();
			sender.sendMail(mailAddress, subject, message, "imnotokapplication@gmail.com", recipients);
		} catch (Exception e) {
			Log.e("SendMail", e.getMessage(), e);
		}
	}

	private String formatRecipients(List<String> to) {
		if (to == null || to.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder(to.get(0));
		for (int i = 1; i < to.size(); i++) {
			sb.append(",").append(to.get(i));
		}
		return sb.toString();
	}

	private String formatMessage(LocationTracker.LocationAddress locationAddress) {
		String message = "I am not OK!";
		if (locationAddress == null) {
			message += " No location information available!";
		} else {
			message += " My current location is: " + "'" + mLocationUtils.formatAddress(locationAddress.address) + "' ("
					+ "latitude: " + locationAddress.location.getLatitude() + ", longitude: "
					+ locationAddress.location.getLongitude() + ")";
			Log.d(mLogTag, "Sending the location - '" + message + "'");
		}
		return message;
	}

	private String getMailAddress() {
		return getSelectedAccount().name;
	}

	private String getAccountName() {
		String email = getMailAddress();
		if (email.contains("@")) {
			// in case we have a mail, lets try to resolve the username.
			Uri uri = Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(email));
			Cursor cur = getContentResolver().query(
					uri, new String[]{Phone.DISPLAY_NAME}, null, null, null);
			if (cur.moveToFirst()) {
				return cur.getString(cur.getColumnIndex(Phone.DISPLAY_NAME));
			} else {
				return email.substring(0, email.indexOf('@'));
			}
		}
		return email;
	}

	private String formatSubject() {
		String name = getAccountName();
		String lineNumber = getPhoneNumber();
		
		return "Emergency message from " + name + " (" + lineNumber + ")";
	}

	private void invokeEmergencyResponse() {
		Log.d(mLogTag, "Invoking emergency response");

		if (mNotifyViaCall) {
			callEmergency();
		}

		// TODO: change to TimerTask
		while (this.getState() == EMERGENCY_STATE) {
			sendEmergencyMessages(mLocationTracker.getLocationAddress());
			try {
				Thread.sleep(mWaitBetweenMessages);
			} catch (InterruptedException exception) {
				exception.printStackTrace();
			}
		}
	}

	private void sendEmergencyMessages(LocationAddress locationAddress) {
		if (mNotifyViaSMS) {
			sendTextNotifications(locationAddress);
		}
		if (mNotifyViaEmail) {
			sendEmailNotifications(locationAddress);
		}
	}

	private void showDisableNotificationAndWaitToInvokeResponse() {
		Log.d(mLogTag, "Showing notification and waiting");

		// Show a notification.
		final NotificationManager notificationManager = (NotificationManager) this
				.getSystemService(NOTIFICATION_SERVICE);

		Intent disableEmergencyIntent = new Intent(STOP_EMERGENCY_INTENT);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				disableEmergencyIntent, 0);

		Notification notification = new Notification(
				android.R.drawable.stat_sys_warning,
				this.getString(R.string.emergency_response_starting),
				System.currentTimeMillis());
		// Notification should be canceled when clicked
		notification.flags |= Notification.FLAG_AUTO_CANCEL
				| Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(this,
				this.getString(R.string.emergency_response_starting),
				this.getString(R.string.click_to_disable), pendingIntent);

		notificationManager.notify(mNotificationID, notification);

		// Register a receiver that can receive the cancellation intents.
		final BroadcastReceiver cancellationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(mLogTag, "Received cancellation intent...");
				if (EmergencyNotificationService.this.getState() == WAITING_STATE) {
					Log.d(mLogTag,
							"Application in waiting state, cancelling the emergency");
					EmergencyNotificationService.this.changeState(NORMAL_STATE);
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter(STOP_EMERGENCY_INTENT);
		this.registerReceiver(cancellationReceiver, intentFilter);

		// Register a receiver that can receive the I am now OK intents.
		final BroadcastReceiver imnowOKReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(mLogTag, "Received I am now OK intent...");
				if (EmergencyNotificationService.this.getState() == EMERGENCY_STATE) {
					Log.d(mLogTag,
							"Application in emergency state, I am now OK");
					EmergencyNotificationService.this.stopEmergency();
				}
			}
		};
		IntentFilter intentIamNowOKFilter = new IntentFilter(I_AM_NOW_OK_INTENT);
		this.registerReceiver(imnowOKReceiver, intentIamNowOKFilter);

		// Start the waiting in a separate thread since otherwise the service
		// will
		// not be able to receive the intent for canceling the emergency
		// response.
		Thread waiterThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(EmergencyNotificationService.this
							.getWaitingTime());
					EmergencyNotificationService.this
							.unregisterReceiver(cancellationReceiver);
					changeState(EMERGENCY_STATE);
				} catch (InterruptedException exception) {
					exception.printStackTrace();
				} finally {
					notificationManager.cancel(mNotificationID++);
					if (EmergencyNotificationService.this.getState() == EMERGENCY_STATE) {
						invokeEmergencyResponse();
					}
				}
			}
		});
		waiterThread.start();
	}

	private synchronized void changeState(int new_state) {
		mApplicationState = new_state;

		// Push the updates to the widget.
		ComponentName thisWidget = new ComponentName(this,
				EmergencyButtonWidgetProvider.class);
		RemoteViews views = new RemoteViews(this.getPackageName(),
				R.layout.emergency_button_widget);
		EmergencyButtonWidgetProvider.setupViews(this, views);
		AppWidgetManager.getInstance(this).updateAppWidget(thisWidget, views);
	}

	private synchronized int getState() {
		return mApplicationState;
	}

	private int getWaitingTime() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Cancellation time:
		String delay_time = prefs.getString(
				getString(R.string.edittext_cancelation_delay),
				Integer.toString(DEFAULT_WAIT_TO_CANCEL / 1000));
		Log.d(mLogTag, "Delay time received from preferences - " + delay_time);

		int waitForMs;
		try {
			int waitForSecs = Integer.parseInt(delay_time);
			waitForMs = waitForSecs * 1000;
		} catch (NumberFormatException e) {
			Log.e("delay_time", "Delay time ill-formated");
			waitForMs = DEFAULT_WAIT_TO_CANCEL;
		}
		Log.d(mLogTag, "Waiting " + waitForMs + " milliseconds.");
		return waitForMs;
	}

	private void stopEmergency() {
		Log.d(mLogTag, "Stopping emergency");
		this.changeState(NORMAL_STATE);
		sendEmergencyMessages(mLocationTracker.getLocationAddress());
		mLocationTracker.deactivate();
	}
}
