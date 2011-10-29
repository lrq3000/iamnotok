package com.google.iamnotok;

import android.location.Location;

public interface LocationTracker {
	public interface Listener {
		void notifyNewLocation(Location location);
	}
	void activate();
	void deactivate();

	/**
	 * Register a listener to be called when location is updated by a substantial distance or better accuracy
	 */
	void registerListenersForBetterLocation(Listener listener);
}