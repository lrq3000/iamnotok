package com.google.iamnotok;

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.iamnotok.utils.LocationUtils;

public class TestActivity extends Activity {
	private static final String LOG_TAG = "TEST_ACT";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);

		final LocationTracker lt = new LocationTrackerImpl(
				(LocationManager) this.getSystemService(Context.LOCATION_SERVICE),
				new LocationUtils(),
				new Geocoder(this, Locale.getDefault()));

		((Button)findViewById(R.id.activate)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Activate Clicked");
				lt.activate();
			}
		});

		((Button)findViewById(R.id.deactivate)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "deact Clicked");
				lt.deactivate();
			}
		});

		((Button)findViewById(R.id.register)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "reg Clicked");
				lt.registerListenersForBetterLocation(new LocationTracker.DistanceThresholdListener() {
					@Override
					public void notifyNewLocation(Location location, Address address) {
						Log.d(LOG_TAG, "New location: " + location);
						Log.d(LOG_TAG, "New address: " + address);
						((TextView) findViewById(R.id.text)).setText(location + " : " + address);
					}
				});
			}
		});

		((Button)findViewById(R.id.getLoc)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Get Clicked");
				lt.notifyListeners();
			}
		});
	}
}
