package com.vladimirkush.geoaction.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class BootCompleteBroadcastReceiver extends BroadcastReceiver {
    private final String LOG_TAG = "LOGTAG";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "device rebooted");
    }
}
