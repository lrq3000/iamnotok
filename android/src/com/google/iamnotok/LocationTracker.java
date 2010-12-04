package com.google.iamnotok;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.maps.GeoPoint;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author vytautas@google.com (Vytautas Vaitukaitis)
 *
 */
public class LocationTracker {
  private static final String mLogTag = "IAmNotOk! - LocationTracker";
  
  private static final int MAX_LOCATION_TRIALS = 10;
  private static final int GET_LOCATION_TIMEOUT = 1000;
  
  private Context mContext;
  private LocationManager mLocationManager;
  private GpsStatus.Listener mGpsStatusListener;
  private LocationListener mGpsLocationListener;
  private LocationListener mCellLocationListener;
  private Location mLocation;
  private Location mLastNotifiedLocation;
  private int mLastNotifiedLocationTime;
  private boolean mIsGpsConnected = false;
  
  public LocationTracker(Context context) {
    mContext = context;
    mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
  }
  
  public void startTracker() {
    if (mGpsStatusListener != null) {
      return;
    }
    
    mGpsStatusListener = new GpsStatusListener();
    mLocationManager.addGpsStatusListener(mGpsStatusListener);
    
    mGpsLocationListener = new UserLocationListener();
    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mGpsLocationListener);
    
    mCellLocationListener = new UserLocationListener();
    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mCellLocationListener);
  }
  
  public void stopTracker() {
    if (mGpsStatusListener != null) {
      mLocationManager.removeGpsStatusListener(mGpsStatusListener);
    }
    
    if (mGpsLocationListener != null) {
      mLocationManager.removeUpdates(mGpsLocationListener);
    }
    
    if (mCellLocationListener != null) {
      mLocationManager.removeUpdates(mCellLocationListener);
    }
  }
  
  private synchronized void setLocation(Location location, LocationListener listener) {
    if (isGpsAvailable() && listener == mCellLocationListener) {
	  return;
    }
    mLocation = location;
    this.notifyAll();
  }
  
  private synchronized boolean isGpsAvailable() {
    return mIsGpsConnected;
  }
  
  public String convertLocationToAddress(Location location) {
    if (location == null) {
      return "<No Address>";
    }
	  
    String address = "";
    Geocoder geoCoder = new Geocoder(
        mContext, Locale.getDefault());
    GeoPoint point = new GeoPoint(
            (int) (location.getLatitude() * 1E6), 
            (int) (location.getLongitude() * 1E6));
    try {
      List<Address> addresses = geoCoder.getFromLocation(
        point.getLatitudeE6()  / 1E6, 
        point.getLongitudeE6() / 1E6, 1);
   
      if (addresses.size() > 0) {
        for (int index = 0; index < addresses.get(0).getMaxAddressLineIndex(); index++)
          address += addresses.get(0).getAddressLine(index) + " ";
      }
    }
    catch (IOException e) {        
      e.printStackTrace();
      address = "<No Address>";
    }   
      
    return address;
  }

  /**
   * Should be called from a separate thread since may block waiting for
   * location.
   */
  public synchronized Location getLocation() {
    for (int trials = 0; trials < MAX_LOCATION_TRIALS && mLocation == null; ++trials) {
      try {
        Log.d(mLogTag, "Waiting for location");
        this.wait(GET_LOCATION_TIMEOUT);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    if (mLocation == null) {
      return null;
    }
    
    // Construct a copy of the current location.
    mLastNotifiedLocation = mLocation;
    return new Location(mLocation);
  }
  
  public String getLocationAddress() {
    // TODO: use the convertLocationToAddress function like this:
    //       return convertLocationToAddress(getLocation());
    // Currently, it hangs on use because of maps api.
    return "<No Address>";
  }
  
  public synchronized boolean shouldSendAnotherUpdate() {
    if (mLastNotifiedLocation == null)
      return true;
    else {
      // TODO(vytautas): check if the location has changed significantly or got
      // more accurate.
      return true;
    }
  }
  
  private class GpsStatusListener implements GpsStatus.Listener {
	@Override
	public void onGpsStatusChanged(int event) {
      switch (event) {
      case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
    	if (mLastNotifiedLocation != null) {
    	  mIsGpsConnected = (SystemClock.elapsedRealtime() - mLastNotifiedLocationTime) < 3000;
    	}
    	break;
      case GpsStatus.GPS_EVENT_FIRST_FIX:
        mIsGpsConnected = true;
      }
	}
  }
  
  private class UserLocationListener implements LocationListener {
    public void onLocationChanged(Location location) {
      Log.d(mLogTag, "Location has changed");
      // TODO(vytautas): add some additional logic here that checks if the new
      // location is more accurate, etc.
      LocationTracker.this.setLocation(location, this);
    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }    
  }

}
