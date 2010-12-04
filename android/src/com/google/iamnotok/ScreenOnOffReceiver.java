package com.google.iamnotok;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.iamnotok.utils.LimitedQueue;

public class ScreenOnOffReceiver extends BroadcastReceiver {

	private LimitedQueue<Long> clicksQueue = new LimitedQueue<Long>(6);
	private int timeLimit = 5*1000; //5 seconds
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.w("ImNotOK", "We're on "+ intent.getAction());
		
		long time = System.currentTimeMillis();
		this.clicksQueue.add(new Long(time));
		
		if (clicksQueue.size() < 3) return;
		
		long timeInterval = clicksQueue.get(0).longValue() - clicksQueue.get(2).longValue();
		Log.w("ImNotOK",Long.toString(timeInterval/1000));
		
		if (timeInterval < timeLimit){
			Log.w("ImNotOK","triggering the event");
			this.TriggerEvent(context);
		}
	
	}

	private void TriggerEvent(Context context) {
        //calling to example phone number
		String number = "0544330376";
		Intent i = new Intent(Intent.ACTION_CALL,
                Uri.fromParts("tel", number, null));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        Log.d("ImNotOK", "Calling Emergency number");
        
		
	}
}
