package com.google.iamnotok;

import java.util.List;

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

	private final static String LOG_TAG = "EmergencyNotificationService";

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
	
	private final AccountUtils accountUtils = new AccountUtils(this);
	private final FormatUtils formatUtils = new FormatUtils();

	private final NotificationSender emailNotificationSender = new EmailNotificationSender(formatUtils, accountUtils);
	private final NotificationSender smsNotificationSender = new SmsNotificationSender(this, formatUtils, accountUtils);
	private final EmergencyCaller emergencyCaller = new EmergencyCaller(this);

	private Application application;

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
		application = (Application)getApplication();
		locationTracker = new LocationTracker();
		preferences = new Preferences(this);

		// Show a notification.
		this.notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
		this.alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
	}
		
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent.getAction();
		Log.d(LOG_TAG, "Service received action: " + action);
		if (action.equals(ACTION_START_EMERGENCY)) {
			startEmergency(intent);
		} else if (action.equals(ACTION_ACTIVATE_EMERGENCY)) {
			activateEmergency();
		} else if (action.equals(ACTION_CANCEL_EMERGENCY)) {
			cancelEmergency();
		} else if (action.equals(ACTION_STOP_EMERGENCY)) {
			stopEmergency();
		} else if (action.equals(ACTION_SEND_EMERGENCY)) {
			sendEmergencyMessages();
		} else {
			Log.w(LOG_TAG, "Unknown action: " + action);
		}
		return START_NOT_STICKY;
	}

	// Handling actions
	
	private void startEmergency(Intent intent) {
		// TODO: remove this when each contact will have its own notifications list.
		if (!(preferences.getNotifyViaCall() || 
			  preferences.getNotifyViaEmail() ||
			  preferences.getNotifyViaSMS())) {
			Log.d(LOG_TAG, "No notification option selected");
			Toast.makeText(this, R.string.no_notification_defined, Toast.LENGTH_LONG).show();
			return;
		}

		// TODO: Check that we have someone to notify
		
		VigilanceState state = preferences.getVigilanceState();
		if (state != VigilanceState.NORMAL_STATE) {
			Log.w(LOG_TAG, "Application already in " + state);
			return;
		}

		Log.d(LOG_TAG, "Starting service");
		changeState(VigilanceState.WAITING_STATE);

		// Vibrate for 300 milliseconds
		((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);

		// Make sure the location tracker is active
		LocationTracker.activate(this);

		if (intent.getBooleanExtra(SHOW_NOTIFICATION_WITH_DISABLE, false)) {
			this.showDisableNotificationAndWaitToInvokeResponse();
		} else {
			startService(getActivateIntent(this));
		}
	}
	
	private void activateEmergency() {
		Log.i(LOG_TAG, "Activating emergency state");
		notificationManager.cancel(NOTIFICATION_ID);
		changeState(VigilanceState.EMERGENCY_STATE);
		
		// Validate stored contacts in case they were modified in the system
		// contacts database after they were added to the application. We will
		// not update the contacts again for this emergency session.
		application.validateContacts();
		
		invokeEmergencyResponse();		
	}
	
	private void cancelEmergency() {
		alarmManager.cancel(getWaitingPendingIntent());
		notificationManager.cancel(NOTIFICATION_ID);
		VigilanceState state = preferences.getVigilanceState();
		if (state == VigilanceState.WAITING_STATE) {
			Log.i(LOG_TAG, "Cancelling emergency state");
			changeState(VigilanceState.NORMAL_STATE);
			LocationTracker.deactivate(this);
		} else {
			Log.w(LOG_TAG, "Attempt to cancel emergency in state: " + state);
		}		
	}
	
	private void stopEmergency() {
		VigilanceState state = preferences.getVigilanceState();
		if (state == VigilanceState.EMERGENCY_STATE) {
			Log.i(LOG_TAG, "Stopping emergency state");
			cancelNotificationsTimer();
			this.changeState(VigilanceState.NORMAL_STATE);
			sendEmergencyMessages();
			LocationTracker.deactivate(this);
		} else {
			Log.w(LOG_TAG, "Attempt to stop emergency in state: " + state);
		}
	}
	
	private void sendEmergencyMessages() {
		Log.i(LOG_TAG, "Sending emergency messages");
		LocationAddress locationAddress = getLocationAddress();		
		List<Contact> contacts = application.getAllContacts();
		if (preferences.getNotifyViaSMS()) {
			smsNotificationSender.sendNotifications(contacts, locationAddress, preferences.getVigilanceState());
		}
		if (preferences.getNotifyViaEmail()) {
			emailNotificationSender.sendNotifications(contacts, locationAddress, preferences.getVigilanceState());
		}
	}

	// Sending emergency response
	
	private void showDisableNotificationAndWaitToInvokeResponse() {
		long waitingMilliseconds = preferences.getCancelationDelayMilliseconds();
		Log.i(LOG_TAG, "Activating emergency state in " + waitingMilliseconds / 1000 + " seconds");

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

		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + waitingMilliseconds, getWaitingPendingIntent());
	}

	private void invokeEmergencyResponse() {
		Log.d(LOG_TAG, "Invoking emergency response");
		if (preferences.getNotifyViaCall()) {
			emergencyCaller.makeCall(application.getAllContacts());
		}
		sendMessageAndResetNotificationsTimer();
	}

	// Keeping application state
	
	private synchronized void changeState(VigilanceState new_state) {
		Log.d(LOG_TAG, "Changing state from " + preferences.getVigilanceState() + " to " + new_state);
		preferences.setVigilanceState(new_state);
	}

	// Handling repeating notifications
	
	private synchronized void sendMessageAndResetNotificationsTimer() {
		alarmManager.setRepeating(
				AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime(),
				preferences.getMessageIntervalMilliseconds(),
				getSendEmergencyPendingIntent());
	}

	private synchronized void cancelNotificationsTimer() {
		alarmManager.cancel(getSendEmergencyPendingIntent());
	}

	// Watching location
	
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
	
	protected void onDistanceThresholdPassed(LocationAddress locationAddress) {
		if (preferences.getVigilanceState() != VigilanceState.EMERGENCY_STATE) {
			return;
		}
		Log.d(LOG_TAG, "onDistanceThresholdPassed");
		sendMessageAndResetNotificationsTimer();
	}

}
