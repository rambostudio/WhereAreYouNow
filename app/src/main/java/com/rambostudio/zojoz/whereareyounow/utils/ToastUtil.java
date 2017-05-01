package com.rambostudio.zojoz.whereareyounow.utils;

import android.widget.Toast;

import com.rambostudio.zojoz.whereareyounow.manager.Contextor;

/**
 * Created by rambo on 1/5/2560.
 */

public class ToastUtil {
    private static boolean isDebug;

    public static void shortAlert(String text) {
        if (isDebug) {
            Toast.makeText(Contextor.getInstance().getContext(), text, Toast.LENGTH_SHORT).show();
        }
    }
    public static void longAlert(String text) {
        if (isDebug) {
            Toast.makeText(Contextor.getInstance().getContext(), text, Toast.LENGTH_LONG).show();
        }
    }

}
