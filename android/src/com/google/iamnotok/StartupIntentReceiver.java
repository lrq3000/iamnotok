// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.iamnotok;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupIntentReceiver extends BroadcastReceiver {
  private final String mLogTag = "ImNotOk - StartupIntentReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(
        mLogTag,
        "Received BOOT_COMPLETED intent, starting up PatternTrackingService.");
    Intent patternTrackingServiceIntent =
      new Intent(context, PatternTrackingService.class);
    context.startService(patternTrackingServiceIntent);
  }
}
