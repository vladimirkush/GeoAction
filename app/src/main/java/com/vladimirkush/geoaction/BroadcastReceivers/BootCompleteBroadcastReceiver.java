package com.vladimirkush.geoaction.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vladimirkush.geoaction.LocalServices.RebootCompleteService;


public class BootCompleteBroadcastReceiver extends BroadcastReceiver {
    private final String LOG_TAG = "LOGTAG";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "device rebooted");
        Intent serviceIntent = new Intent(context,RebootCompleteService.class);
        context.startService(serviceIntent);


    }
}
