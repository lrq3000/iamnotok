package com.google.iamnotok;

import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.iamnotok.LocationTracker.DistanceThresholdListener;
import com.google.iamnotok.LocationTracker.LocationAddress;
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
				lt.setDistanceThresholdListener(new DistanceThresholdListener() {
					@Override
					public void notify(LocationAddress locationAddress) {
						Log.d(LOG_TAG, "New location: " + locationAddress.location);
						Log.d(LOG_TAG, "New address: " + locationAddress.address);
						Date date = new Date();
						((TextView) findViewById(R.id.time)).setText("notif: " + date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds());
						((TextView) findViewById(R.id.loc)).setText(locationAddress.location.getLatitude() + ":" + locationAddress.location.getLongitude());
						if (locationAddress.address == null) {
							((TextView) findViewById(R.id.addr)).setText("No address :(");
						} else {
							((TextView) findViewById(R.id.addr)).setText(locationAddress.address.getAddressLine(0) + ", " + locationAddress.address.getLocality());;
						}
					}
				});
			}
		});

		((Button)findViewById(R.id.getLoc)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Get Clicked");
				Date date = new Date();
				LocationAddress locationAddress = lt.getLocationAddress();
				((TextView) findViewById(R.id.time)).setText("get: " + date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds());
				((TextView) findViewById(R.id.loc)).setText(locationAddress.location.getLatitude() + ":" + locationAddress.location.getLongitude());
				if (locationAddress.address == null) {
					((TextView) findViewById(R.id.addr)).setText("No address :(");
				} else {
					((TextView) findViewById(R.id.addr)).setText(locationAddress.address.getAddressLine(0) + ", " + locationAddress.address.getLocality());;
				}
			}
		});
	}
}
