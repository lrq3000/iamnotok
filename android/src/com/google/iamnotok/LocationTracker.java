package com.google.iamnotok;

import android.location.Address;
import android.location.Location;

public interface LocationTracker {
	public interface Listener {
		/**
		 * Method called on new location update
		 *
		 * @param location instance holding the full location information
		 * @param address reverse geolocated address of the location above. Can be null.
		 */
		void notifyNewLocation(Location location, Address address);
	}
	public void activate();
	public void deactivate();

	/**
	 * Register a listener to be called when location is updated by a substantial distance or better accuracy
	 */
	public void registerListenersForBetterLocation(Listener listener);

	/**
	 * Calls the listeners with the currently best known location.
	 *
	 * This function calls the listeners with cached data - thus is immediate.
	 */
	public void notifyListeners();
}