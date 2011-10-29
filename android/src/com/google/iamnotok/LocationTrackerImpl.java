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
	
	private final LocationManager locationManager;
	private LocationListener locationListener;
	private Location bestKnownLocation;
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
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		
		updateLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
		updateLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
	}
	
	/**
	 * Update location if more accurate or significantly newer
	 */
	private void updateLocation(Location newLocation) {
		Log.i(LOG_TAG, "In updatLocation");
		
		if (locationUtils.isBetterLocation(newLocation, this.bestKnownLocation)) {
			this.bestKnownLocation = newLocation;
			notifyListeners();
			
			Log.i(LOG_TAG,"Updatitng best location to "+newLocation);
		}
	}

	@Override
	public void deactivate() {
		Log.i(LOG_TAG, "Deactivating");
		locationManager.removeUpdates(locationListener);
		// keep last known location
	}

	@Override
	public Location getLocation() {
		Log.i(LOG_TAG, "In getLocation. The returned locations is "+bestKnownLocation);
		return bestKnownLocation;
	}

	@Override
	public void registerListenersForBetterLocation(float thresholdChangeMeters, Listener listener) {
		// TODO: threshold - use or remove
		listeners.add(listener);
		Log.i(LOG_TAG, "In registerListenersForBetterLocation adding listener "+listener.getClass().getName());
	}

	// Synchronized so 2 concurrent calls() won't confuse listeners
	private synchronized void notifyListeners() {
		// TODO: take thresholds into account
		for (Listener listener : listeners) {
			listener.notifyNewLocation(bestKnownLocation);
		}
		Log.i(LOG_TAG, "Done notifying all listeners with best location "+bestKnownLocation);
	}
}
