package com.google.iamnotok;

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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * @author vytautas@google.com (Vytautas Vaitukaitis)
 *
 */
public class LocationTracker {
  private static final String mLogTag = "IAmNotOk! - LocationTracker";
  
  private static final float MINIMUM_NOTIFIED_DISTANCE = 500;
  private static final float MINIMUM_NOTIFIED_ACCURACY_CHANGE = 10;
  
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
	Log.i(mLogTag, "Location tracker started.");
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
    Log.i(mLogTag, "Location tracker stopped.");
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
    try {
      List<Address> addresses = geoCoder.getFromLocation(
    		  location.getLatitude(), 
    		  location.getLongitude(), 1);
   
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
    /*for (int trials = 0; trials < MAX_LOCATION_TRIALS && mLocation == null; ++trials) {
      try {
        Log.d(mLogTag, "Waiting for location");
        this.wait(GET_LOCATION_TIMEOUT);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    if (mLocation == null) {
      return null;
    }*/
    
    // Construct a copy of the current location.
    mLastNotifiedLocation = mLocation;
    return new Location(mLocation);
  }
  
  public String getLocationAddress() {
	if (mLocation != null) {
	  return convertLocationToAddress(getLocation());
	}
    return "<No Address>";
  }
  
  public synchronized boolean shouldSendAnotherUpdate() {
    if ((mLastNotifiedLocation == null) ||
    	(mLocation.distanceTo(mLastNotifiedLocation) > MINIMUM_NOTIFIED_DISTANCE) ||
    	((mLocation.getAccuracy() - mLastNotifiedLocation.getAccuracy()) > MINIMUM_NOTIFIED_ACCURACY_CHANGE)) {
      return true;
  	}
  	return false;
  }
  
  private class GpsStatusListener implements GpsStatus.Listener {
	@Override
	public void onGpsStatusChanged(int event) {
		Log.i(mLogTag, "GPS location has changed.");
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
    @Override
	public void onLocationChanged(Location location) {
      Log.d(mLogTag, "Location has changed");
      // TODO(vytautas): add some additional logic here that checks if the new
      // location is more accurate, etc.
      LocationTracker.this.setLocation(location, this);
    }

    @Override
	public void onProviderDisabled(String provider) {
    }

    @Override
	public void onProviderEnabled(String provider) {
    }

    @Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
    }    
  }

}
