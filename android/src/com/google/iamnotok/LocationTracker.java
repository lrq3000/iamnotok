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

	private static final String LOG = "LocationTracker";

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
		Log.i(LOG, "activating");
		Location gpsLocation = null;
		Location networkLocation = null;
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					UPDATE_INTERVAL_MS, 0, getUpdateLocationPendingIntent(context));
			gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			Log.d(LOG, "last known gps location: " + gpsLocation);
		}
		if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					UPDATE_INTERVAL_MS, 0, getUpdateLocationPendingIntent(context));
			networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Log.d(LOG, "last known network location: " + networkLocation);
		}
		
		if (gpsLocation != null && networkLocation != null) {
			if (LocationUtils.isBetterLocation(networkLocation, gpsLocation)) {
				updateLastKnownLocation(context, networkLocation);
			} else {
				updateLastKnownLocation(context, gpsLocation);
			}
		} else if (gpsLocation != null) {
			updateLastKnownLocation(context, gpsLocation);
		} else if (networkLocation != null) {
			updateLastKnownLocation(context, networkLocation);
		} else {
			Log.d(LOG, "last location unknown");
		}		
	}

	private static void updateLastKnownLocation(Context context, Location location) {
		Log.d(LOG, "last known location provider: " + location.getProvider());
		Intent intent = getUpdateLocationIntent(context);
		intent.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
		context.startService(intent);		
	}
	
	/**
	 * Update location if more accurate or significantly newer
	 */
	private void updateLocation(Location newLocation) {
		Log.d(LOG, "received new location: " + newLocation);
		LocationAddress currentLocationAddress = getCurrentLocation(preferences);
		Log.d(LOG, "current location: " + currentLocationAddress.location);
		if (LocationUtils.isBetterLocation(newLocation, currentLocationAddress.location)) {
			Log.i(LOG, "updating current location to " + newLocation);
			updateAddressAndNotify(newLocation);
		}
	}

	/**
	 * Notifying only if last notified location is farther than
	 * METERS_THRESHOLD_FOR_NOTIFY from current. On no previous notifications,
	 * we do NOT notify. (TODO: why?)
	 */
	private boolean shouldNotify() {
		LocationAddress lastNotifiedLocationAddress = preferences.getLastNotifiedLocation();
		if (lastNotifiedLocationAddress == null) {
			Log.d(LOG, "last notified location unknown");
			return false;
		}
		Log.d(LOG, "last notified location: " + lastNotifiedLocationAddress.location);
		LocationAddress currentLocationAddress = getCurrentLocation(preferences);
		Log.d(LOG, "current location: " + currentLocationAddress.location);
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
	                    Log.i(LOG, "current address: " + address);
	                    preferences.setCurrentLocation(new LocationAddress(location, address));
	                } else {
	                	Log.i(LOG, "current address unknown");
	                	// XXX Store current location with null address?
	                }
	            } catch (IOException e) {
	                Log.e(LOG, "Impossible to connect to Geocoder", e);
                	// XXX Store current location with null address?
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
		Log.i(LOG, "deactivating");
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
		if (listener != null) {
			Log.i(LOG, "notifying listener");
			listener.notify(currentLocationAddress);
		}
		// XXX: It does not makes sense to update last notified location if
		// notification was not sent.
		Log.i(LOG, "storing last notified address " + currentLocationAddress);
		preferences.setLastNotifiedLocation(currentLocationAddress);
	}

	
	// TODO: why static?
	
	public static LocationAddress getLocationAddress(Context context) {
		Preferences pref = new Preferences(context);
		LocationAddress currentLocationAddress = getCurrentLocation(pref);
		
		// XXX: This does not make sense - querying current location should not
		// change last notified location.
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
