package com.google.iamnotok;

import java.io.IOException;
import java.util.List;

import android.location.Address;
import android.location.Geocoder;
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
	private final Geocoder geocoder;
	private LocationListener locationListener;
	private LocationAddress currentLocationAddress;
	private Location lastNotifiedLocation;
	private final LocationUtils locationUtils;
	private DistanceThresholdListener listener;

	public LocationTrackerImpl(LocationManager locationManager, LocationUtils locationUtils, Geocoder geocoder) {
		this.locationManager = locationManager;
		this.locationUtils = locationUtils;
		this.geocoder = geocoder;
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

		if (locationUtils.isBetterLocation(newLocation, this.currentLocationAddress.location)) {
			this.currentLocationAddress = new LocationAddress(newLocation, null);
			updateAddressAndNotify(this.currentLocationAddress.location);

			Log.i(LOG_TAG,"Updatitng best location to "+newLocation);
		}
	}

	/**
	 * Notifying only if last notified location is farther than METERS_THRESHOLD_FOR_NOTIFY from current.
	 */
	private boolean shouldNotify() {
		return (this.lastNotifiedLocation == null) ||
				(this.lastNotifiedLocation.distanceTo(this.currentLocationAddress.location) > METERS_THRESHOLD_FOR_NOTIFY);
	}

	private void updateAddressAndNotify(final Location location) {
	    Thread thread = new Thread() {
	        @Override public void run() {
	            try {
	                List<Address> list = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
	                if (list != null && list.size() > 0) {
	                    Address address = list.get(0);
	                	currentLocationAddress = new LocationAddress(location, address);
	                }
	            } catch (IOException e) {
	                Log.e(LOG_TAG, "Impossible to connect to Geocoder", e);
	            } finally {
	            	if (shouldNotify()) {
	            		notifyListeners();
	            	}
	            }
	        }
	    };
	    thread.start();
	}

	@Override
	public void deactivate() {
		Log.i(LOG_TAG, "Deactivating");
		locationManager.removeUpdates(locationListener);
		// keep last known location
	}

	@Override
	public void setDistanceThresholdListener(DistanceThresholdListener listener) {
		this.listener = listener;
	}

	// Synchronized so 2 concurrent calls() won't confuse listeners
	private synchronized void notifyListeners() {
		if (listener != null)
			listener.notify(currentLocationAddress);
		this.lastNotifiedLocation = currentLocationAddress.location;
		Log.i(LOG_TAG, "Done notifying all listeners with best location " + currentLocationAddress);
	}

	@Override
	public LocationAddress getLocationAddress() {
		return currentLocationAddress;
	}
}
