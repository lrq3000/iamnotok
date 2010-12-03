package com.google.iamnotok;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Tracks the events in the system and infers when emergency has happened. When
 * this is the case, sends off an EmergencyIntent to
 * EmergencyNotificationService to invoke the response to the emergency.
 */
public class PatternTrackingService extends Service {
  private final String mLogTag = "ImNotOk - PatternTrackingService";

  // Flag used for the MainActivity to check if the process is running before
  // trying to start off the process again.
  public static boolean mServiceRunning = false;

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
    Log.d(mLogTag, "Starting the service");

    if (!this.startTrackingPatterns()) {
      this.informUserAboutProblemTrackingPatterns();
      this.stopSelf();
    } else {
      mServiceRunning = true;
      Log.d(mLogTag, "The service started successfully");
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }

  private boolean startTrackingPatterns() {
    BroadcastReceiver receiver = new MediaButtonEventReceiver();
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(Intent.ACTION_CALL_BUTTON);
    this.registerReceiver(receiver, intentFilter);
    return true;
  }
  
  private void informUserAboutProblemTrackingPatterns() {
    // Show a notification to the user that we were unable to start the pattern
    // tracker.
    NotificationManager notificationManager =
        (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

    Intent retryLaunchingPatternTrackerIntent = new Intent(this, this.getClass());
    PendingIntent pendingIntent =
        PendingIntent.getService(this, 0, retryLaunchingPatternTrackerIntent, 0);

    Notification notification = new Notification(android.R.drawable.stat_notify_error,
        this.getString(R.string.unable_to_track_actions_for_emergency_title),
        System.currentTimeMillis());
    // Notification should be canceled when clicked
    notification.flags |= Notification.FLAG_AUTO_CANCEL;
    notification.setLatestEventInfo(this,
        this.getString(R.string.unable_to_track_actions_for_emergency_title),
        this.getString(R.string.unable_to_track_actions_for_emergency_msg), pendingIntent);

    notificationManager.notify(0, notification);
  }
  
  private void mediaButtonClicked() {
    Log.d(mLogTag, "Media button has been clicked");
    Toast.makeText(this, "Media button clicked", 5000).show();
  }
  
  private void callButtonClicked() {
    Log.d(mLogTag, "Call button clicked");
    Toast.makeText(this, "Call button clicked", 5000).show();
  }
  
  private void cameraButtonClicked() {
    Log.d(mLogTag, "Camera button clicked");
    Toast.makeText(this, "Camera button clicked", 5000).show();
  }
  
  private class MediaButtonEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d(mLogTag, "Received intent");
      
      if (intent.getAction() == Intent.ACTION_CALL_BUTTON) {
        PatternTrackingService.this.callButtonClicked();
      } else if (intent.getAction() == Intent.ACTION_CAMERA_BUTTON) {
        PatternTrackingService.this.cameraButtonClicked();
      } else if (intent.getAction() == Intent.ACTION_MEDIA_BUTTON) {
        PatternTrackingService.this.mediaButtonClicked();
      }
    }    
  }
}
