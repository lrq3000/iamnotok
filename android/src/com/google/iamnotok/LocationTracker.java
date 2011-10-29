package com.google.iamnotok;

import android.location.Location;

public interface LocationTracker {
	public interface Listener {
		void notifyNewLocation(Location location);
	}
	public void activate();
	public void deactivate();

	/**
	 * Register a listener to be called when location is updated by a substantial distance or better accuracy
	 */
	public void registerListenersForBetterLocation(Listener listener);

	public void notifyListeners();
}