package com.reandroid.apkeditor.merge;

import android.util.Log;

public class LogUtil {
    private static Merger.LogListener logListener;

    public static boolean logEnabled;

    public static void setLogListener(Merger.LogListener listener) {
        logListener = listener;
    }

    public static void logMessage(CharSequence msg) {
        if (logListener != null && logEnabled) {
            logListener.onLog(msg);
        }
        Log.d("", msg.toString());
    }
}