package com.google.iamnotok.utils;

import java.io.OutputStreamWriter;

import android.content.Context;
import android.util.Log;

public class IamNotOKLogger {

	private static final String LOG_FILE_NAME = "/data/data/com.google.iamnotok/log.txt";
	
	private static String DEBUG_LOG_LEVEL = "DEBUG";
	private static String INFO_LOG_LEVEL = "INFO";

	private static String LOG_LEVEL = DEBUG_LOG_LEVEL;
	
	public static void Log(Context context, String tag, String msg){
		if (LOG_LEVEL.equals(DEBUG_LOG_LEVEL)){
			Log.d(tag, msg);
		} else if (LOG_LEVEL.equals(INFO_LOG_LEVEL)){
			Log.i(tag, msg);
		}
		
		writeToFile(context, tag, msg);
	}
	
	public static void Log(String tag, String msg,Throwable e){
		if (LOG_LEVEL.equals(DEBUG_LOG_LEVEL)){
			Log.d(tag, msg, e);
		} else if (LOG_LEVEL.equals(INFO_LOG_LEVEL)){
			Log.i(tag, msg, e);
		}
	}
	
	private static void writeToFile (Context context, String tag, String msg){
		try {
			  OutputStreamWriter out = new OutputStreamWriter(context.openFileOutput(LOG_FILE_NAME,Context.MODE_APPEND));
			  out.write(tag+":"+msg);
			  out.close();
			} catch (java.io.IOException e) {
			 Log.e("IamNotOKLogger","unable to write to file ",e);
			}
	}
}
