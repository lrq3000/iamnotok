package com.google.iamnotok;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Shows the main screen of the application.
 */
public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button changeApplicationSettingsButton = (Button) this.findViewById(R.id.ApplicationSettingsButton);
        changeApplicationSettingsButton.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				// Send of an intent to start off the EmergencyInfoActivity
				Intent intent = new Intent(MainActivity.this, ApplicationSettingsActivity.class);
				MainActivity.this.startActivity(intent);
			}
		});
   
        Button contactPickerButton = (Button) this.findViewById(R.id.ContactPickerButton);
        contactPickerButton.setOnClickListener(new OnClickListener() {          
            public void onClick(View v) {
                // Send of an intent to start off the ContactPickerActivity
                Intent intent = new Intent(MainActivity.this, EmergencyContactsActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });
        
        Button IAmNotOKStartButton = (Button) this.findViewById(R.id.IAmNotOKStartButton);
        IAmNotOKStartButton.setOnClickListener(new OnClickListener() {          
            public void onClick(View v) {
                // Send of an intent to start off the EmergencyInfoActivity
                Intent intent = new Intent(MainActivity.this, EmergencyNotificationService.class);
                MainActivity.this.startService(intent);
            }
        });
        
        // Check if PatternTrackingService is running (this could happen if
        // the application is just installed), if not, start it
        if (!PatternTrackingService.mServiceRunning) {
          Intent patternTrackingServiceIntent =
            new Intent(this, PatternTrackingService.class);
          this.startService(patternTrackingServiceIntent);
        }
    }
}