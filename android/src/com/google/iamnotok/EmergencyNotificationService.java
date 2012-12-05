package com.google.iamnotok;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.iamnotok.LocationTracker.LocationAddress;
import com.google.iamnotok.Preferences.VigilanceState;
import com.google.iamnotok.senders.NotificationSender;
import com.google.iamnotok.senders.SmsNotificationSender;
import com.google.iamnotok.senders.email_sender.EmailNotificationSender;
import com.google.iamnotok.utils.AccountUtils;
import com.google.iamnotok.utils.FormatUtils;

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

	private final static String ACTION_START_EMERGENCY = "startEmergency";
	private final static String ACTION_ACTIVATE_EMERGENCY = "activateEmergency";
	private final static String ACTION_CANCEL_EMERGENCY = "cancelEmergency";
	private final static String ACTION_STOP_EMERGENCY = "stopEmergency";
	private final static String ACTION_SEND_EMERGENCY = "sendEmergency";

	private static final int NOTIFICATION_ID = 0;
	private LocationTracker locationTracker;
	private Preferences preferences;
	
	private boolean notifyViaSMS = true;
	private boolean notifyViaEmail = true;
	private boolean notifyViaCall = false;
	private long waitBetweenMessagesMs = Preferences.DEFAULT_MESSAGE_INTERVAL_SECONDS * 1000;

	private final AccountUtils accountUtils = new AccountUtils(this);
	private final FormatUtils formatUtils = new FormatUtils();

	private final NotificationSender emailNotificationSender = new EmailNotificationSender(formatUtils, accountUtils);
	private final NotificationSender smsNotificationSender = new SmsNotificationSender(this, formatUtils, accountUtils);
	private final EmergencyCaller emergencyCaller = new EmergencyCaller(getBaseContext());

	private EmergencyContactsHelper contactHelper;

	NotificationManager notificationManager;
	AlarmManager alarmManager;

	public static Intent getStartIntent(Context context) {
		return new Intent(context, EmergencyNotificationService.class).setAction(ACTION_START_EMERGENCY);
	}

	private static Intent getActivateIntent(Context context) {
		return new Intent(context, EmergencyNotificationService.class).setAction(ACTION_ACTIVATE_EMERGENCY);
	}

	public static Intent getCancelIntent(Context context) {
		return new Intent(context, EmergencyNotificationService.class).setAction(ACTION_CANCEL_EMERGENCY);
	}

	public static Intent getStopIntent(Context context) {
		return new Intent(context, EmergencyNotificationService.class).setAction(ACTION_STOP_EMERGENCY);
	}

	private PendingIntent getWaitingPendingIntent() {
		return PendingIntent.getService(this, 0, getActivateIntent(this), 0);
	}

	private PendingIntent getSendEmergencyPendingIntent() {
		return PendingIntent.getService(this, 0,
				new Intent(this, this.getClass()).setAction(ACTION_SEND_EMERGENCY), 0);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		contactHelper = new EmergencyContactsHelper(this, new ContactLookupUtil());
		locationTracker = new LocationTracker();
		preferences = new Preferences(this);

		// Show a notification.
		this.notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
		this.alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
	}

	protected void onDistanceThresholdPassed(LocationAddress locationAddress) {
		if (preferences.getVigilanceState() != VigilanceState.EMERGENCY_STATE) {
			return;
		}

		Log.d("iamnotok", "onDistanceThresholdPassed");

		sendMessageAndResetTimer(locationAddress);
	}

	private void sendMessageAndResetTimer(LocationAddress locationAddress) {
		setNotificationTimer();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent.getAction();
		Log.i(LOG_TAG, "Service received action: " + action);
		if (action.equals(ACTION_START_EMERGENCY)) {
			readPreferences();

			if (!(notifyViaCall || notifyViaEmail || notifyViaSMS)) {
				Toast.makeText(this, R.string.no_notification_defined, Toast.LENGTH_LONG).show();
				return START_NOT_STICKY;
			}

			// TODO: Check that we have someone to notify
			
			if (preferences.getVigilanceState() != VigilanceState.NORMAL_STATE) {
				Log.d(LOG_TAG, "Application already in either waiting or emergency mode.");
				return START_NOT_STICKY;
			}

			Log.d(LOG_TAG, "Starting the service");
			changeState(VigilanceState.WAITING_STATE);

			// Vibrate for 300 milliseconds
			((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);

			// Make sure the location tracker is active
			LocationTracker.activate(this);

			boolean showNotification = (intent == null) || (intent.getBooleanExtra(SHOW_NOTIFICATION_WITH_DISABLE, false));
			if (showNotification) {
				this.showDisableNotificationAndWaitToInvokeResponse();
			} else {
				startService(getActivateIntent(this));
			}
		} else if (action.equals(ACTION_ACTIVATE_EMERGENCY)) {
			notificationManager.cancel(NOTIFICATION_ID);
			changeState(VigilanceState.EMERGENCY_STATE);
			invokeEmergencyResponse();
		} else if (action.equals(ACTION_CANCEL_EMERGENCY)) {
			alarmManager.cancel(getWaitingPendingIntent());
			notificationManager.cancel(NOTIFICATION_ID);
			if (preferences.getVigilanceState() == VigilanceState.WAITING_STATE) {
				Log.d(LOG_TAG, "Application in waiting state, cancelling the emergency");
				changeState(VigilanceState.NORMAL_STATE);
				LocationTracker.deactivate(this);
			} else {
				Log.w(LOG_TAG, "Trying to cancel a notificaiton in state: " + preferences.getVigilanceState().name());
			}
		} else if (action.equals(ACTION_STOP_EMERGENCY)) {
			if (preferences.getVigilanceState() == VigilanceState.EMERGENCY_STATE) {
				Log.d(LOG_TAG, "Application in emergency state, I am now OK");
				stopEmergency();
			} else {
				Log.w(LOG_TAG, "Trying to stop a notification in state: " + preferences.getVigilanceState().name());
			}
		} else if (action.equals(ACTION_SEND_EMERGENCY)) {
			sendEmergencyMessages(getLocationAddress());
		} else {
			Log.e(LOG_TAG, "Unknown action: " + action);
		}

		return START_NOT_STICKY;
	}

	private void readPreferences() {
		notifyViaSMS = preferences.getSMSNotification();
		notifyViaEmail = preferences.getEmailNotification();
		notifyViaCall = preferences.getCallNotification();
		waitBetweenMessagesMs = preferences.getMessageIntervalMilliseconds();
	}

	private synchronized void setNotificationTimer() {
		alarmManager.setRepeating(
				AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime(),
				waitBetweenMessagesMs,
				getSendEmergencyPendingIntent());
	}

	private void invokeEmergencyResponse() {
		Log.d(LOG_TAG, "Invoking emergency response");

		if (notifyViaCall) {
			emergencyCaller.makeCall(this.contactHelper.getAllContacts());
		}

		sendMessageAndResetTimer(getLocationAddress());
	}

	private void sendEmergencyMessages(LocationAddress locationAddress) {
		if (notifyViaSMS) {
			smsNotificationSender.sendNotifications(contactHelper.getAllContacts(), locationAddress, preferences.getVigilanceState());
		}
		if (notifyViaEmail) {
			emailNotificationSender.sendNotifications(contactHelper.getAllContacts(), locationAddress, preferences.getVigilanceState());
		}
	}

	private void showDisableNotificationAndWaitToInvokeResponse() {
		Log.d(LOG_TAG, "Showing notification and waiting");

		Intent cancelEmergencyIntent = new Intent(this, this.getClass()).setAction(ACTION_CANCEL_EMERGENCY);

		Notification notification = new NotificationCompat.Builder(this)
			.setTicker(this.getString(R.string.emergency_response_starting))
			.setSmallIcon(android.R.drawable.stat_sys_warning)
			.setContentTitle(this.getString(R.string.emergency_response_starting))
			.setContentText(this.getString(R.string.click_to_disable))
			.setContentIntent(PendingIntent.getService(this, 0, cancelEmergencyIntent, 0))
			.setAutoCancel(true)
			.setOngoing(true)
			.build();

		notificationManager.notify(NOTIFICATION_ID, notification);

		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + getWaitingTime(), getWaitingPendingIntent());
	}

	private synchronized void changeState(VigilanceState new_state) {
		Log.i(LOG_TAG, "Changing state from: " + preferences.getVigilanceState().name() + " to " + new_state);
		preferences.setVigilanceState(new_state);
	}

	private long getWaitingTime() {
		return preferences.getCancelationDelayMilliseconds();
	}

	private void stopEmergency() {
		Log.d(LOG_TAG, "Stopping emergency");
		cancelNotificationsTimer();
		this.changeState(VigilanceState.NORMAL_STATE);
		sendEmergencyMessages(getLocationAddress());
		LocationTracker.deactivate(this);
	}

	private synchronized void cancelNotificationsTimer() {
		alarmManager.cancel(getSendEmergencyPendingIntent());
	}

	/**
	 * Returns location address, and registers the distance threshold listener on first invocation
	 */
	private LocationAddress getLocationAddress() {
		registerDistanceThresholdListener();
		return LocationTracker.getLocationAddress(this);
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
