package com.google.iamnotok;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenOnOffReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
//		Toast.makeText(context, "We're on", Toast.LENGTH_LONG);
		Log.w("ImNotOK", "We're on "+ intent.getAction());
//		Intent i = new Intent(context, ImNotOKService.class);
//		context.getApplicationContext().startService(i);
		
        /*AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_VOICE_CALL, true);
        
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        //calling to example phone number
		String number = "0544330376";
		Intent i = new Intent(Intent.ACTION_CALL,
                Uri.fromParts("tel", number, null));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
//        pm.goToSleep(1000);
        Log.d("ImNotOK", "Calling Emergency number");
        */
	}

}
