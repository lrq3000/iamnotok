package com.google.iamnotok;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.iamnotok.LocationTracker.LocationAddress;
import com.google.iamnotok.utils.AccountUtils;
import com.google.iamnotok.utils.FormatUtils;
import com.google.iamnotok.utils.LocationUtils;

/**
 * Puts the phone to the emergency state and notifies the contacts in the
 * emergency contacts' list about the situation.
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
	public final static String I_AM_NOT_OK_INTENT = "com.google.imnotok.I_AM_NOT_OK";
	public final static String STOP_EMERGENCY_INTENT = "com.google.imnotok.STOP_EMERGENCY";
	public final static String I_AM_NOW_OK_INTENT = "com.google.imnotok.I_AM_NOW_OK";

	public final static String SERVICE_I_AM_NOT_OK_INTENT = "com.google.imnotok.SERVICE_I_AM_NOT_OK";
	public final static String SERVICE_I_AM_NOW_OK_INTENT = "com.google.imnotok.SERVICE_I_AM_NOW_OK";

	public enum VigilanceState {
		NORMAL_STATE,
		WAITING_STATE,
		EMERGENCY_STATE
	}
	public static VigilanceState mApplicationState = VigilanceState.NORMAL_STATE;

	/** Default time allowed for user to cancel the emergency response. */
	private static long DEFAULT_WAIT_TO_CANCEL_MS = 10000;
	private static final int DEFAULT_WAIT_BETWEEN_MESSAGES_MS = 5 * 60 * 1000;


	private int mNotificationID = 0;
	private LocationTracker mLocationTracker;
	private LocationUtils mLocationUtils;
	private boolean mNotifyViaSMS = true;
	private boolean mNotifyViaEmail = true;
	private boolean mNotifyViaCall = false;
	private long mWaitBetweenMessagesMs = DEFAULT_WAIT_BETWEEN_MESSAGES_MS;

	private final AccountUtils accountUtils = new AccountUtils(this);
	private final FormatUtils formatUtils = new FormatUtils();

	private final NotificationSender emailNotificationSender = new EmailNotificationSender(formatUtils, accountUtils);
	private final NotificationSender smsNotificationSender = new SmsNotificationSender(formatUtils);
	private final EmergencyCaller emergencyCaller = new EmergencyCaller(getBaseContext());

	private EmergencyContactsHelper contactHelper;

	private Timer notificationsTimer;


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
		if (getState() != VigilanceState.EMERGENCY_STATE) {
			return;
		}

		if (this.notificationsTimer != null) {
			this.notificationsTimer.cancel();
		}
		sendEmergencyMessages(locationAddress);
		setNotificationTimer();
	}
	
	private long readWaitBetweenMessagesMs(SharedPreferences prefs) {
		try {
			String messageIntervalString = prefs.getString(getString(R.string.edittext_message_interval), null);
			if (messageIntervalString != null)
				return Integer.parseInt(messageIntervalString) * 1000; // Convert to milliseconds.
		} catch (NumberFormatException e) {
		}
		return DEFAULT_WAIT_BETWEEN_MESSAGES_MS;
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
		mWaitBetweenMessagesMs = readWaitBetweenMessagesMs(prefs);

		if (contactHelper == null)
			contactHelper = new EmergencyContactsHelper(getApplicationContext());

		if (!(mNotifyViaCall || mNotifyViaEmail || mNotifyViaSMS)) {
			Toast.makeText(getApplicationContext(),
					R.string.no_notification_defined, Toast.LENGTH_LONG).show();
			return;
		}

		contactHelper.contactIds();

		if (this.getState() == VigilanceState.NORMAL_STATE) {
			Log.d(mLogTag, "Starting the service");
			changeState(VigilanceState.WAITING_STATE);
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
				changeState(VigilanceState.EMERGENCY_STATE);
				this.invokeEmergencyResponse();
			}
			super.onStart(intent, startId);
		} else {
			Log.d(mLogTag,
					"Application already in either waiting or emergency mode.");
		}
	}

	private void setNotificationTimer() {
		this.notificationsTimer = new Timer();
		this.notificationsTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				sendEmergencyMessages(mLocationTracker.getLocationAddress());
			}
		}, mWaitBetweenMessagesMs, mWaitBetweenMessagesMs);
	}

	private void invokeEmergencyResponse() {
		Log.d(mLogTag, "Invoking emergency response");

		Intent iAmNotOkIntent = new Intent(SERVICE_I_AM_NOT_OK_INTENT);
		this.sendBroadcast(iAmNotOkIntent);

		if (mNotifyViaCall) {
			emergencyCaller.makeCall(this.contactHelper.getAllContacts());
		}

		sendEmergencyMessages(mLocationTracker.getLocationAddress());
		setNotificationTimer();
	}

	private void sendEmergencyMessages(LocationAddress locationAddress) {
		if (mNotifyViaSMS) {
			smsNotificationSender.sendNotifications(contactHelper.getAllContacts(), locationAddress, getState());
		}
		if (mNotifyViaEmail) {
			emailNotificationSender.sendNotifications(contactHelper.getAllContacts(), locationAddress, getState());
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
				if (EmergencyNotificationService.this.getState() == VigilanceState.WAITING_STATE) {
					Log.d(mLogTag,
							"Application in waiting state, cancelling the emergency");
					changeState(VigilanceState.NORMAL_STATE);
					mLocationTracker.deactivate();
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
				if (getState() == VigilanceState.EMERGENCY_STATE) {
					Log.d(mLogTag, "Application in emergency state, I am now OK");
					stopEmergency();
				}
			}
		};
		IntentFilter intentIamNowOKFilter = new IntentFilter(I_AM_NOW_OK_INTENT);
		this.registerReceiver(imnowOKReceiver, intentIamNowOKFilter);

		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				unregisterReceiver(cancellationReceiver);
				notificationManager.cancel(mNotificationID++);
				if (getState() == VigilanceState.WAITING_STATE) {
					changeState(VigilanceState.EMERGENCY_STATE);
					invokeEmergencyResponse();
				} else {
					// TODO: Do this in the cancellation receiver.
					unregisterReceiver(imnowOKReceiver);
				}
			}
		}, this.getWaitingTime());
	}

	private synchronized void changeState(VigilanceState new_state) {
		mApplicationState = new_state;

		// Push the updates to the widget.
		ComponentName thisWidget = new ComponentName(this,
				EmergencyButtonWidgetProvider.class);
		RemoteViews views = new RemoteViews(this.getPackageName(),
				R.layout.emergency_button_widget);
		EmergencyButtonWidgetProvider.setupViews(this, views);
		AppWidgetManager.getInstance(this).updateAppWidget(thisWidget, views);

		// Broadcast
	}

	private synchronized VigilanceState getState() {
		return mApplicationState;
	}

	private long getWaitingTime() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Cancellation time:
		String delay_time = prefs.getString(
				getString(R.string.edittext_cancelation_delay),
				Long.toString(DEFAULT_WAIT_TO_CANCEL_MS / 1000));
		Log.d(mLogTag, "Delay time received from preferences - " + delay_time);

		long waitForMs;
		try {
			int waitForSecs = Integer.parseInt(delay_time);
			waitForMs = waitForSecs * 1000;
		} catch (NumberFormatException e) {
			Log.e("delay_time", "Delay time ill-formated");
			waitForMs = DEFAULT_WAIT_TO_CANCEL_MS;
		}
		Log.d(mLogTag, "Waiting " + waitForMs + " milliseconds.");
		return waitForMs;
	}

	private void stopEmergency() {
		Log.d(mLogTag, "Stopping emergency");
		if (this.notificationsTimer != null) {
			this.notificationsTimer.cancel();
			this.notificationsTimer = null;
		}
		this.changeState(VigilanceState.NORMAL_STATE);
		sendEmergencyMessages(mLocationTracker.getLocationAddress());
		mLocationTracker.deactivate();

		Intent iAmNowOkIntent = new Intent(SERVICE_I_AM_NOW_OK_INTENT);
		this.sendBroadcast(iAmNowOkIntent);
	}
}
