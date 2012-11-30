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
import android.support.v4.app.NotificationCompat;
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
	private final static String LOG_TAG = "ImNotOk - EmergencyNotificationService";

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
		EMERGENCY_STATE,
	}
	public static VigilanceState applicationState = VigilanceState.NORMAL_STATE;

	/** Default time allowed for user to cancel the emergency response. */
	private static final long DEFAULT_WAIT_TO_CANCEL_MS = 10000;
	private static final long DEFAULT_WAIT_BETWEEN_MESSAGES_MS = 5 * 60 * 1000;

	private int notificationID = 0;
	private LocationTracker locationTracker;
	private LocationUtils locationUtils;
	private boolean notifyViaSMS = true;
	private boolean notifyViaEmail = true;
	private boolean notifyViaCall = false;
	private long waitBetweenMessagesMs = DEFAULT_WAIT_BETWEEN_MESSAGES_MS;

	private final AccountUtils accountUtils = new AccountUtils(this);
	private final FormatUtils formatUtils = new FormatUtils();

	private final NotificationSender emailNotificationSender = new EmailNotificationSender(formatUtils, accountUtils);
	private final NotificationSender smsNotificationSender = new SmsNotificationSender(this, formatUtils, accountUtils);
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
		contactHelper = new EmergencyContactsHelper(this, new ContactLookupUtil());
		locationUtils = new LocationUtils();
		locationTracker = new LocationTrackerImpl(
				(LocationManager) this.getSystemService(Context.LOCATION_SERVICE),
				locationUtils,
				new Geocoder(this, Locale.getDefault()));
	}

	protected void onDistanceThresholdPassed(LocationAddress locationAddress) {
		if (applicationState != VigilanceState.EMERGENCY_STATE) {
			return;
		}

		Log.d("iamnotok", "onDistanceThresholdPassed");

		sendMessageAndResetTimer(locationAddress);
	}

	private void sendMessageAndResetTimer(LocationAddress locationAddress) {
		setNotificationTimer();
		sendEmergencyMessages(locationAddress);
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
		Log.d(LOG_TAG, "onStart() called");

		readPreferences();

		if (!(notifyViaCall || notifyViaEmail || notifyViaSMS)) {
			Toast.makeText(this, R.string.no_notification_defined, Toast.LENGTH_LONG).show();
			return;
		}

		if (applicationState == VigilanceState.NORMAL_STATE) {
			Log.d(LOG_TAG, "Starting the service");
			changeState(VigilanceState.WAITING_STATE);

			// Vibrate for 300 milliseconds
			((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);

			// Make sure the location tracker is active
			locationTracker.activate();

			boolean showNotification = intent == null ? true : intent.getBooleanExtra(SHOW_NOTIFICATION_WITH_DISABLE, false);
			if (showNotification) {
				this.showDisableNotificationAndWaitToInvokeResponse();
			} else {
				changeState(VigilanceState.EMERGENCY_STATE);
				this.invokeEmergencyResponse();
			}
			super.onStartCommand(intent, 0, startId);
		} else {
			Log.d(LOG_TAG,
					"Application already in either waiting or emergency mode.");
		}
	}

	private void readPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		notifyViaSMS = prefs.getBoolean(getString(R.string.checkbox_sms_notification), true);
		notifyViaEmail = prefs.getBoolean(getString(R.string.checkbox_email_notification), true);
		notifyViaCall = prefs.getBoolean(getString(R.string.checkbox_call_notification), false);
		waitBetweenMessagesMs = readWaitBetweenMessagesMs(prefs);
	}

	private synchronized void setNotificationTimer() {
		cancelNotificationsTimer();
		Log.d(LOG_TAG, "Setting notification");
		this.notificationsTimer = new Timer();
		this.notificationsTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Log.d(LOG_TAG, "Sending timed notification");
				sendEmergencyMessages(getLocationAddress());
			}
		}, waitBetweenMessagesMs, waitBetweenMessagesMs);
	}

	private void invokeEmergencyResponse() {
		Log.d(LOG_TAG, "Invoking emergency response");

		this.sendBroadcast(new Intent(SERVICE_I_AM_NOT_OK_INTENT));

		if (notifyViaCall) {
			emergencyCaller.makeCall(this.contactHelper.getAllContacts());
		}

		sendMessageAndResetTimer(getLocationAddress());
	}

	private void sendEmergencyMessages(LocationAddress locationAddress) {
		if (notifyViaSMS) {
			smsNotificationSender.sendNotifications(contactHelper.getAllContacts(), locationAddress, applicationState);
		}
		if (notifyViaEmail) {
			emailNotificationSender.sendNotifications(contactHelper.getAllContacts(), locationAddress, applicationState);
		}
	}

	private void showDisableNotificationAndWaitToInvokeResponse() {
		Log.d(LOG_TAG, "Showing notification and waiting");

		// Show a notification.
		final NotificationManager notificationManager = (NotificationManager) this
				.getSystemService(NOTIFICATION_SERVICE);

		Intent disableEmergencyIntent = new Intent(STOP_EMERGENCY_INTENT);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				disableEmergencyIntent, 0);

		Notification notification = new NotificationCompat.Builder(this)
			.setTicker(this.getString(R.string.emergency_response_starting))
			.setSmallIcon(android.R.drawable.stat_sys_warning)
			.setContentTitle(this.getString(R.string.emergency_response_starting))
			.setContentText(this.getString(R.string.click_to_disable))
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.setOngoing(true)
			.build();

		notificationManager.notify(notificationID, notification);

		// Register a receiver that can receive the cancellation intents.
		final BroadcastReceiver cancellationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(LOG_TAG, "Received cancellation intent...");
				if (EmergencyNotificationService.applicationState == VigilanceState.WAITING_STATE) {
					Log.d(LOG_TAG,
							"Application in waiting state, cancelling the emergency");
					changeState(VigilanceState.NORMAL_STATE);
					locationTracker.deactivate();
				}
			}
		};
		this.registerReceiver(cancellationReceiver, new IntentFilter(STOP_EMERGENCY_INTENT));

		// Register a receiver that can receive the I am now OK intents.
		final BroadcastReceiver imnowOKReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(LOG_TAG, "Received I am now OK intent...");
				if (applicationState == VigilanceState.EMERGENCY_STATE) {
					Log.d(LOG_TAG, "Application in emergency state, I am now OK");
					stopEmergency();
				}
			}
		};
		this.registerReceiver(imnowOKReceiver, new IntentFilter(I_AM_NOW_OK_INTENT));

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				unregisterReceiver(cancellationReceiver);
				notificationManager.cancel(notificationID++);
				if (applicationState == VigilanceState.WAITING_STATE) {
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
		applicationState = new_state;

		// Push the updates to the widget.
		ComponentName thisWidget = new ComponentName(this,
				EmergencyButtonWidgetProvider.class);
		RemoteViews views = new RemoteViews(this.getPackageName(),
				R.layout.emergency_button_widget);
		EmergencyButtonWidgetProvider.setupViews(this, views);
		AppWidgetManager.getInstance(this).updateAppWidget(thisWidget, views);
	}

	private long getWaitingTime() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		String prefName = getString(R.string.edittext_cancelation_delay);
		String prefVal = prefs.getString(prefName, null);
		Log.d(LOG_TAG, String.format("from prefs: %s=%s", prefName, prefVal));

		try {
			return prefVal == null ? DEFAULT_WAIT_TO_CANCEL_MS : Integer.parseInt(prefVal) * 1000;
		} catch (NumberFormatException e) {
			Log.e("delay_time", String.format("Badly formatted pref: %s=%s", prefName, prefVal));
			return DEFAULT_WAIT_TO_CANCEL_MS;
		}
	}

	private void stopEmergency() {
		Log.d(LOG_TAG, "Stopping emergency");
		cancelNotificationsTimer();
		this.changeState(VigilanceState.NORMAL_STATE);
		sendEmergencyMessages(getLocationAddress());
		locationTracker.deactivate();

		Intent iAmNowOkIntent = new Intent(SERVICE_I_AM_NOW_OK_INTENT);
		this.sendBroadcast(iAmNowOkIntent);
	}

	private synchronized void cancelNotificationsTimer() {
		if (this.notificationsTimer != null) {
			Log.d(LOG_TAG, "Canceling notification timer");
			this.notificationsTimer.cancel();
			this.notificationsTimer = null;
		}
	}

	/**
	 * Returns location address, and registers the distance threshold listener on first invocation
	 */
	private LocationAddress getLocationAddress() {
		registerDistanceThresholdListener();
		return locationTracker.getLocationAddress();
	}

	private boolean registeredDistanceThresholdListener = false;
	private void registerDistanceThresholdListener() {
		if (registeredDistanceThresholdListener)
			return;
		registeredDistanceThresholdListener = true;
		locationTracker.setDistanceThresholdListener(new LocationTracker.DistanceThresholdListener() {
			@Override
			public void notify(LocationTracker.LocationAddress locationAddress) {
				onDistanceThresholdPassed(locationAddress);
			}
		});
	}
}
