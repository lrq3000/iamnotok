package com.google.iamnotok;

import android.location.Address;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class LocationAddress implements Parcelable {
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

