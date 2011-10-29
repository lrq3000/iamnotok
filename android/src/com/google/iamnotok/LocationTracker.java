package com.google.iamnotok;

import android.location.Address;
import android.location.Location;

public interface LocationTracker {
	public class LocationAddress {
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
	}
	
	public LocationAddress getLocationAddress();
	
	/**
	 * Will be called when location is updated by a substantial distance 
	 */
	public interface DistanceThresholdListener {
		void notify(LocationAddress locationAddress);
	}
	public void setDistanceThresholdListener(DistanceThresholdListener listener);
	
	public void activate();
	public void deactivate();
}