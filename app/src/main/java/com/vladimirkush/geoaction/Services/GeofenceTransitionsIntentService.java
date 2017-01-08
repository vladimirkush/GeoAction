package com.vladimirkush.geoaction.Services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

public class GeofenceTransitionsIntentService extends IntentService {


    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            //TODO get data from intent (id of Geofence and LBaction)
        }
    }

}
