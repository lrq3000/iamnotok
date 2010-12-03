package com.google.iamnotok;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class EmergencyButtonWidgetProvider extends AppWidgetProvider {
 
  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    final int numWidgets = appWidgetIds.length;
    
    Log.d("ONUPDATE", "ONUPDATE");
    
    for (int i = 0; i < numWidgets; i++) {
      int appWidgetId = appWidgetIds[i];
      
      // Get the remote views and set the pending intent for the emergency button.
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.emergency_button_widget);
      setupViews(context, views);
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    super.onUpdate(context, appWidgetManager, appWidgetIds);
  }
  
  public static void setupViews(Context context, RemoteViews views) {
    // Create intent to launch the EmergencyNotificationService
    Intent intent = new Intent(context, EmergencyNotificationService.class);
    intent.putExtra(EmergencyNotificationService.SHOW_NOTIFICATION_WITH_DISABLE, true);
    PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
    views.setOnClickPendingIntent(R.id.EmergencyButton, pendingIntent);
    
    // Create ImNowOk intent
    Intent iAmNowOkIntent = new Intent(EmergencyNotificationService.I_AM_NOW_OK_INTENT);
    PendingIntent pendingIAmNowOkIntent = PendingIntent.getBroadcast(context, 0, iAmNowOkIntent, 0);
    views.setOnClickPendingIntent(R.id.ImNowOKButton, pendingIAmNowOkIntent);
    
    views.setViewVisibility(R.id.ImNowOKButton, View.INVISIBLE);
    views.setViewVisibility(R.id.EmergencyButton, View.INVISIBLE);
    switch (EmergencyNotificationService.mApplicationState) {
      case (EmergencyNotificationService.NORMAL_STATE): 
        views.setViewVisibility(R.id.EmergencyButton, View.VISIBLE);
        break;
      case (EmergencyNotificationService.WAITING_STATE): 
        views.setViewVisibility(R.id.EmergencyButton, View.VISIBLE);
        break;
      case (EmergencyNotificationService.EMERGENCY_STATE): 
        views.setViewVisibility(R.id.ImNowOKButton, View.VISIBLE);
        break;
    }
  }
}
