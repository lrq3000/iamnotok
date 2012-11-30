package com.google.iamnotok;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.google.iamnotok.EmergencyNotificationService.VigilanceState;

public class EmergencyButtonWidgetProvider extends AppWidgetProvider {

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    final int numWidgets = appWidgetIds.length;

    for (int i = 0; i < numWidgets; i++) {
      int appWidgetId = appWidgetIds[i];

      // Get the remote views and set the pending intent for the emergency button.
      appWidgetManager.updateAppWidget(appWidgetId, setupViews(context, EmergencyNotificationService.applicationState));
    }

    super.onUpdate(context, appWidgetManager, appWidgetIds);
  }

  @Override
	public void onReceive(Context context, Intent intent) {
	  if (intent.getAction() == EmergencyNotificationService.STATE_CHANGE_INTENT) {
	    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	    appWidgetManager.updateAppWidget(new ComponentName(context.getPackageName(), this.getClass().getName()),
	    		setupViews(context, (VigilanceState) intent.getSerializableExtra(EmergencyNotificationService.NEW_STATE_EXTRA)));
	  }
      super.onReceive(context, intent);
	}

  private RemoteViews setupViews(Context context, VigilanceState state) {
    // Create intent to launch the EmergencyNotificationService
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.emergency_button_widget);

    Intent intent = new Intent(context, EmergencyNotificationService.class);
    intent.putExtra(EmergencyNotificationService.SHOW_NOTIFICATION_WITH_DISABLE, true);
    PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
    views.setOnClickPendingIntent(R.id.EmergencyButton, pendingIntent);

    // Create ImNowOk intent
    Intent iAmNowOkIntent = new Intent(EmergencyNotificationService.I_AM_NOW_OK_INTENT);
    PendingIntent pendingIAmNowOkIntent = PendingIntent.getBroadcast(context, 0, iAmNowOkIntent, 0);
    views.setOnClickPendingIntent(R.id.ImNowOKButton, pendingIAmNowOkIntent);

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
