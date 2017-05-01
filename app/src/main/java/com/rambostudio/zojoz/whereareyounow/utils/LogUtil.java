package com.rambostudio.zojoz.whereareyounow.utils;

import android.util.Log;

/**
 * Created by rambo on 1/5/2560.
 */


public class LogUtil {
    private static boolean isDebug = true;
    public static void e(String tag,String text) {
        if (isDebug) {
            Log.e(tag, text);
        }
    }

    public static void d(String tag,String text) {
        if (isDebug) {
            Log.d(tag, text);
        }
    }
    public static void i(String tag,String text) {
        if (isDebug) {
            Log.i(tag, text);
        }
    }
    public static void wtf(String tag,String text) {
        if (isDebug) {
            Log.wtf(tag, text);
        }
    }

}
