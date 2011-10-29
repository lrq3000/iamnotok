package com.google.iamnotok;

import android.location.Location;

public interface LocationTracker {
	public interface Listener {
		void notifyNewLocation(Location location);
	}
	void activate();
	void deactivate();
	Location getLocation();
	
	/**
	 * Register a listener to be called when location is updated by a substantial distance or better accuracy
	 * 
	 * @param thresholdChangeMeter threshold to call listener when location is changed by at least threshold meters
	 * @param listener
	 */
	void registerListenersForBetterLocation(float thresholdChangeMeters, Listener listener);
}