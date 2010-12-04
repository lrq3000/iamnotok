package com.google.iamnotok;

import android.content.Context;
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
  
  private LocationManager mLocationManager;
  private GpsStatus.Listener mGpsStatusListener;
  private LocationListener mGpsLocationListener;
  private LocationListener mCellLocationListener;
  private Location mLocation;
  private Location mLastNotifiedLocation;
  private int mLastNotifiedLocationTime;
  private boolean mIsGpsConnected = false;
  
  public LocationTracker(Context context) {
    mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    
    mGpsStatusListener = new GpsStatusListener();
    mLocationManager.addGpsStatusListener(mGpsStatusListener);
    
    mGpsLocationListener = new UserLocationListener();
    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mGpsLocationListener);
    
    mCellLocationListener = new UserLocationListener();
    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mCellLocationListener);
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

  /**
   * Should be called from a separate thread since may block waiting for
   * location.
   */
  public synchronized Location getLocation() {
    while (mLocation == null) {
      try {
        Log.d(mLogTag, "Waiting for location");
        this.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    // Construct a copy of the current location.
    mLastNotifiedLocation = mLocation;
    return new Location(mLocation);
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
