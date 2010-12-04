package com.google.iamnotok;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.iamnotok.utils.LimitedQueue;

public class ScreenOnOffReceiver extends BroadcastReceiver {

	private static final int CLICK_NUMBER = 6;
	
	private LimitedQueue<Long> clicksQueue = new LimitedQueue<Long>(CLICK_NUMBER);
	private int timeLimit = 5*1000; //5 seconds
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.w("ImNotOK", "We're on "+ intent.getAction());
		
		long time = System.currentTimeMillis();
		this.clicksQueue.offer(new Long(time));
		
		if (clicksQueue.size() < CLICK_NUMBER) return;
		
		long timeInterval = clicksQueue.get(CLICK_NUMBER-1).longValue() - clicksQueue.get(0).longValue();
		Log.w("ImNotOK",Long.toString(timeInterval/1000));
		
		if (timeInterval < timeLimit){
			Log.w("ImNotOK","triggering the event");
			this.TriggerEvent(context.getApplicationContext());
			clicksQueue.clear();
		}
	
	}

	private void TriggerEvent(Context context) {
	    Intent intent = new Intent(context, EmergencyNotificationService.class);
	    intent.putExtra(EmergencyNotificationService.SHOW_NOTIFICATION_WITH_DISABLE, true);
	    context.startService(intent);
	}
}
