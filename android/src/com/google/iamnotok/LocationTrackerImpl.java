package com.google.iamnotok;

import java.io.IOException;
import java.util.List;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.google.iamnotok.utils.LocationUtils;

public class LocationTrackerImpl extends IntentService implements LocationTracker {
	private static final String LOG_TAG = "LocationTrackerImpl";

	private static final long UPDATE_INTERVAL_MS = 60000;
	private static final float METERS_THRESHOLD_FOR_NOTIFY = 1000;

	private static final String ACTION_UPDATE_LOCATION = "updateLocation";

	private final LocationManager locationManager;
	private final Geocoder geocoder;
	private LocationAddress currentLocationAddress = new LocationAddress(null, null);
	private LocationAddress lastNotifiedLocationAddress;
	private final LocationUtils locationUtils;
	private DistanceThresholdListener listener;

	private PendingIntent getUpdateLocationPendingIntent(Context context) {
		return PendingIntent.getService(context, 0, new Intent(context, getClass()).setAction(ACTION_UPDATE_LOCATION), 0);
	}

	public LocationTrackerImpl(LocationManager locationManager, LocationUtils locationUtils, Geocoder geocoder) {
		super(LocationTrackerImpl.class.getName());
		this.locationManager = locationManager;
		this.locationUtils = locationUtils;
		this.geocoder = geocoder;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent.getAction().equals(ACTION_UPDATE_LOCATION) && intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
			updateLocation((Location) intent.getSerializableExtra(LocationManager.KEY_LOCATION_CHANGED));
		}
	}

	@Override
	public void activate(Context context) {
		Log.i(LOG_TAG,"In activate");
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_MS, 0, getUpdateLocationPendingIntent(context));
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL_MS, 0, getUpdateLocationPendingIntent(context));

		Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (gpsLocation != null && networkLocation != null) {
			if (locationUtils.isBetterLocation(networkLocation, gpsLocation)) {
				gpsLocation = null;
			} else {
				networkLocation = null;
			}
		}
		if (gpsLocation != null) {
			updateLocation(gpsLocation);
		} else if (networkLocation != null) {
			updateLocation(networkLocation);
		}
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
	 * On no previous notifications, we do NOT notify.
	 */
	private boolean shouldNotify() {
		return (this.lastNotifiedLocationAddress != null) &&
				(this.lastNotifiedLocationAddress.location.distanceTo(
						this.currentLocationAddress.location) > METERS_THRESHOLD_FOR_NOTIFY);
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
	public void deactivate(Context context) {
		Log.i(LOG_TAG, "Deactivating");
		locationManager.removeUpdates(getUpdateLocationPendingIntent(context));
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
		this.lastNotifiedLocationAddress = currentLocationAddress;
		Log.i(LOG_TAG, "Done notifying all listeners with best location " + currentLocationAddress);
	}

	@Override
	public LocationAddress getLocationAddress() {
		lastNotifiedLocationAddress = currentLocationAddress;
		return currentLocationAddress;
	}
}
