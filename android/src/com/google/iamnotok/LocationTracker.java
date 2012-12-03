package com.google.iamnotok;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.iamnotok.utils.LocationUtils;

public class LocationTracker extends IntentService {
	public static class LocationAddress implements Parcelable {
		/**
		 * location instance holding the full location information
		 */
		public final Location location;

		/**
		 * address reverse geolocated address of the location above. Can be null.
		 */
		public final Address address;
		public LocationAddress(Location location, Address address) {
			this.location = location;
			this.address = address;
		}

		@Override
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeParcelable(location, flags);
			dest.writeParcelable(address, flags);
		}

		public static LocationAddress readFromParcel(Parcel source) {
			return new LocationAddress(Location.CREATOR.createFromParcel(source), Address.CREATOR.createFromParcel(source));
		}
	}
	public interface DistanceThresholdListener {
		void notify(LocationAddress locationAddress);
	}

	private static final String LOG_TAG = "LocationTracker";

	private static final long UPDATE_INTERVAL_MS = 60000;
	private static final float METERS_THRESHOLD_FOR_NOTIFY = 1000;

	private static final String ACTION_UPDATE_LOCATION = "updateLocation";

	private Geocoder geocoder;
	private DistanceThresholdListener listener;
	private Preferences preferences;

	private static Intent getUpdateLocationIntent(Context context) {
		return new Intent(context, LocationTracker.class).setAction(ACTION_UPDATE_LOCATION);
	}

	private static PendingIntent getUpdateLocationPendingIntent(Context context) {
		return PendingIntent.getService(context, 0, getUpdateLocationIntent(context), 0);
	}

	public LocationTracker() {
		super(LocationTracker.class.getName());
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.geocoder = new Geocoder(this, Locale.getDefault());
		this.preferences = new Preferences(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent.getAction().equals(ACTION_UPDATE_LOCATION) && intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
			updateLocation((Location) intent.getSerializableExtra(LocationManager.KEY_LOCATION_CHANGED));
		}
	}

	public static void activate(Context context) {
		Log.i(LOG_TAG,"In activate");
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_MS, 0, getUpdateLocationPendingIntent(context));
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL_MS, 0, getUpdateLocationPendingIntent(context));

		Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (gpsLocation != null && networkLocation != null) {
			if (LocationUtils.isBetterLocation(networkLocation, gpsLocation)) {
				gpsLocation = null;
			} else {
				networkLocation = null;
			}
		}
		Location bestCurrentLocation = null;
		if (gpsLocation != null) {
			bestCurrentLocation = gpsLocation;
		} else if (networkLocation != null) {
			bestCurrentLocation = networkLocation;
		}

		if (bestCurrentLocation != null) {
			context.startService(getUpdateLocationIntent(context).putExtra(LocationManager.KEY_LOCATION_CHANGED, bestCurrentLocation));
		}
	}

	/**
	 * Update location if more accurate or significantly newer
	 */
	private void updateLocation(Location newLocation) {
		Log.i(LOG_TAG, "In updatLocation");

		LocationAddress currentLocationAddress = getCurrentLocation(preferences);
		if (LocationUtils.isBetterLocation(newLocation, currentLocationAddress.location)) {
			currentLocationAddress = new LocationAddress(newLocation, null);
			updateAddressAndNotify(currentLocationAddress.location);

			Log.i(LOG_TAG,"Updatitng best location to "+newLocation);
		}
	}

	/**
	 * Notifying only if last notified location is farther than
	 * METERS_THRESHOLD_FOR_NOTIFY from current. On no previous notifications,
	 * we do NOT notify. (TODO: why?)
	 */
	private boolean shouldNotify() {
		LocationAddress lastNotifiedLocationAddress = preferences.getLastNotifiedLocation();
		if (lastNotifiedLocationAddress == null)
			return false;
		LocationAddress currentLocationAddress = getCurrentLocation(preferences);
		float distance = lastNotifiedLocationAddress.location.distanceTo(currentLocationAddress.location);
		return distance > METERS_THRESHOLD_FOR_NOTIFY;
	}

	private void updateAddressAndNotify(final Location location) {
	    Thread thread = new Thread() {
	        @Override public void run() {
	            try {
	                List<Address> list = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
	                if (list != null && list.size() > 0) {
	                    Address address = list.get(0);
	                    preferences.setCurrentLocation(new LocationAddress(location, address));
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

	public static void deactivate(Context context) {
		Log.i(LOG_TAG, "Deactivating");
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(getUpdateLocationPendingIntent(context));
		// TODO: keep last known location?
	}

	// TODO: VERY IMPORTANT: this should'nt be here. This should be sent by intent!
	public void setDistanceThresholdListener(DistanceThresholdListener listener) {
		this.listener = listener;
	}

	// Synchronized so 2 concurrent calls() won't confuse listeners
	private synchronized void notifyListeners() {
		LocationAddress currentLocationAddress = getCurrentLocation(preferences);
		if (listener != null)
			listener.notify(currentLocationAddress);
		preferences.setLastNotifiedLocation(currentLocationAddress);
		Log.i(LOG_TAG, "Done notifying all listeners with best location " + currentLocationAddress);
	}

	
	// TODO: why static?
	
	public static LocationAddress getLocationAddress(Context context) {
		Preferences pref = new Preferences(context);
		LocationAddress currentLocationAddress = getCurrentLocation(pref);
		pref.setLastNotifiedLocation(currentLocationAddress);
		return currentLocationAddress;
	}
	
	// Returns stored location address from preferences or new location address
	// with null location and address.
	private static LocationAddress getCurrentLocation(Preferences pref) {
		LocationAddress currentLocationAddress = pref.getCurrentLocation();
		if (currentLocationAddress == null)
			return new LocationAddress(null, null);
		return currentLocationAddress;
	}
}
