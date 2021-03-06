package com.google.iamnotok.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import com.google.iamnotok.EmergencyNotificationService;
import com.google.iamnotok.Preferences;
import com.google.iamnotok.Preferences.VigilanceState;
import com.google.iamnotok.R;

public class EmergencyButtonWidgetProvider extends AppWidgetProvider implements OnSharedPreferenceChangeListener {
	private Context context;

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    final int numWidgets = appWidgetIds.length;

    for (int i = 0; i < numWidgets; i++) {
      int appWidgetId = appWidgetIds[i];

      // Get the remote views and set the pending intent for the emergency button.
      Preferences pref = new Preferences(context);
      appWidgetManager.updateAppWidget(appWidgetId, setupViews(context, pref.getVigilanceState()));
    }

    super.onUpdate(context, appWidgetManager, appWidgetIds);
  }

  @Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		this.context = context;
		PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
	}

  @Override
	public void onDisabled(Context context) {
	  	PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);
	  	this.context = null;
		super.onDisabled(context);
	}

  @Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	  if (key.equals(Preferences.VIGILANCE_STATE_KEY)) {
	    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	    Preferences pref = new Preferences(context);
;	    appWidgetManager.updateAppWidget(new ComponentName(context.getPackageName(), this.getClass().getName()),
	    		setupViews(context, pref.getVigilanceState()));
	  }
	}

  private RemoteViews setupViews(Context context, VigilanceState state) {
    // Create intent to launch the EmergencyNotificationService
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.emergency_button_widget);

    Intent intent = EmergencyNotificationService.getStartIntent(context)
    	.putExtra(EmergencyNotificationService.SHOW_NOTIFICATION_WITH_DISABLE, true);
    PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
    views.setOnClickPendingIntent(R.id.EmergencyButton, pendingIntent);

    // Create ImNowOk intent
    Intent iAmNowOkIntent = EmergencyNotificationService.getStopIntent(context);
    PendingIntent pendingIAmNowOkIntent = PendingIntent.getService(context, 0, iAmNowOkIntent, 0);
    views.setOnClickPendingIntent(R.id.ImNowOKButton, pendingIAmNowOkIntent);

    Intent stopEmergency = EmergencyNotificationService.getCancelIntent(context);
    PendingIntent pendingStopIntent = PendingIntent.getService(context, 0, stopEmergency, 0);
    views.setOnClickPendingIntent(R.id.CancelEmergencyButton, pendingStopIntent);

    views.setViewVisibility(R.id.ImNowOKButton, View.INVISIBLE);
    views.setViewVisibility(R.id.CancelEmergencyButton, View.INVISIBLE);
    views.setViewVisibility(R.id.EmergencyButton, View.INVISIBLE);

    switch (state) {
      case NORMAL_STATE:
        views.setViewVisibility(R.id.EmergencyButton, View.VISIBLE);
        break;
      case WAITING_STATE:
        views.setViewVisibility(R.id.CancelEmergencyButton, View.VISIBLE);
        break;
      case EMERGENCY_STATE:
        views.setViewVisibility(R.id.ImNowOKButton, View.VISIBLE);
        break;
    }
    return views;
  }
}
