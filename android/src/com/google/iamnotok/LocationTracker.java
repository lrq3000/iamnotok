package com.google.iamnotok;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * @author vytautas@google.com (Vytautas Vaitukaitis)
 *
 */
public class LocationTracker {
  private static final String mLogTag = "IAmNotOk! - LocationTracker";
  
  private LocationManager mLocationManager;
  private LocationListener mLocationListener;
  private Location mLocation;
  private Location mLastNotifiedLocation;
  
  public LocationTracker(Context context) {
    mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    mLocationListener = new UserLocationListener();
    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
  }
  
  private synchronized void setLocation(Location location) {
    mLocation = location;
    this.notifyAll();
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
  
  private class UserLocationListener implements LocationListener {
    public void onLocationChanged(Location location) {
      Log.d(mLogTag, "Location has changed");
      // TODO(vytautas): add some additional logic here that checks if the new
      // location is more accurate, etc.
      LocationTracker.this.setLocation(location);
    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

  }

}
