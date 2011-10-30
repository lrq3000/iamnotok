package com.google.iamnotok.utils;

import java.io.OutputStreamWriter;

import android.content.Context;
import android.util.Log;

public class IamNotOKLogger {

	private static final String LOG_FILE_NAME = "log.txt";

	private static int DEBUG_LOG_LEVEL = 3;

	private static final String LOG_TAG = "IAMNOTOK";

	public static void Log(Context context, String tag, String msg) {
		log(DEBUG_LOG_LEVEL, msg);

		if (context != null) {
			writeToFile(context, tag, msg);
		}
	}

	public static void Log(String msg, Throwable e) {
		log(DEBUG_LOG_LEVEL, msg, e);
	}

	private static void writeToFile(Context context, String tag, String msg) {
		try {
			OutputStreamWriter out = new OutputStreamWriter(
					context.openFileOutput(LOG_FILE_NAME, Context.MODE_APPEND));
			out.write(tag + ":" + msg);
			out.close();
		} catch (java.io.IOException e) {
			Log.e("IamNotOKLogger", "unable to write to file ", e);
		}
	}

	public static void log(int level, String str) {
		switch (level) {
		case 0:
			Log.e(LOG_TAG, str);
			break;
		case 1:
			Log.w(LOG_TAG, str);
			break;
		case 2:
			Log.i(LOG_TAG, str);
			break;
		case 3:
			Log.d(LOG_TAG, str);
			break;
		default:
			Log.v(LOG_TAG, str);
			break;
		}
	}

	public static void log(int level, String str, Throwable e) {
		switch (level) {
		case 0:
			Log.e(LOG_TAG, str, e);
			break;
		case 1:
			Log.w(LOG_TAG, str, e);
			break;
		case 2:
			Log.i(LOG_TAG, str, e);
			break;
		case 3:
			Log.d(LOG_TAG, str, e);
			break;
		default:
			Log.v(LOG_TAG, str, e);
			break;
		}

	}

}
