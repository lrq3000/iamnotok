package com.google.iamnotok;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Syncs the emergency information with the web server. Invoked by the input to
 * the EmergencyInfoActivity and by periodic checks to see if the information
 * on the web server hasn't changed.
 */
public class EmergencyInfoSyncService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
