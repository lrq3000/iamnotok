package com.google.iamnotok;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.iamnotok.utils.LocationUtils;

public class LocationTrackerImpl implements LocationTracker {
	private static final String LOG_TAG = "LocationTrackerImpl";

	private static final long UPDATE_INTERVAL_MS = 60000;
	private static final float METERS_THRESHOLD_FOR_NOTIFY = 1000;

	private final LocationManager locationManager;
	private LocationListener locationListener;
	private Location bestKnownLocation;
	private Location lastNotifiedLocation;
	private String address;
	private final LocationUtils locationUtils;
	private List<Listener> listeners = new ArrayList<Listener>();

	public LocationTrackerImpl(LocationManager locationManager, LocationUtils locationUtils) {
		this.locationManager = locationManager;
		this.locationUtils = locationUtils;
	}

	@Override
	public void activate() {
		this.locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				updateLocation(location);
			}

			@Override public void onStatusChanged(String provider, int status, Bundle extras) {}
			@Override public void onProviderEnabled(String provider) {}
			@Override public void onProviderDisabled(String provider) {}

		};

		Log.i(LOG_TAG,"In activate");
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_MS, 0, locationListener);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL_MS, 0, locationListener);

		updateLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
		updateLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
	}

	/**
	 * Update location if more accurate or significantly newer
	 */
	private void updateLocation(Location newLocation) {
		Log.i(LOG_TAG, "In updatLocation");

		if (locationUtils.isBetterLocation(newLocation, this.bestKnownLocation)) {
			this.address = null;
			this.bestKnownLocation = newLocation;
			updateAddressAndNotify();

			Log.i(LOG_TAG,"Updatitng best location to "+newLocation);
		}
	}

	/**
	 * Notifying only if last notified location is farther than METERS_THRESHOLD_FOR_NOTIFY from current.
	 */
	private boolean shouldNotify() {
		return (this.lastNotifiedLocation == null) ||
				(this.lastNotifiedLocation.distanceTo(this.bestKnownLocation) > METERS_THRESHOLD_FOR_NOTIFY);
	}

	private void updateAddressAndNotify() {
		// TODO - implement.
		if (shouldNotify()) {
			notifyListeners();
		}
	}

	@Override
	public void deactivate() {
		Log.i(LOG_TAG, "Deactivating");
		locationManager.removeUpdates(locationListener);
		// keep last known location
	}

	@Override
	public void registerListenersForBetterLocation(Listener listener) {
		listeners.add(listener);
	}

	// Synchronized so 2 concurrent calls() won't confuse listeners
	public synchronized void notifyListeners() {
		for (Listener listener : listeners) {
			listener.notifyNewLocation(bestKnownLocation, null);
		}
		this.lastNotifiedLocation = bestKnownLocation;
		Log.i(LOG_TAG, "Done notifying all listeners with best location " + bestKnownLocation);
	}
}
