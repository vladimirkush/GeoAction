package com.vladimirkush.geoaction.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Utils.DBHelper;
import com.vladimirkush.geoaction.Utils.GeofenceHelper;

import java.util.ArrayList;


public class RebootCompleteService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final String        LOG_TAG = "LOGTAG";
    private GoogleApiClient     mGoogleApiClient;
    private Context             mContext;


    public RebootCompleteService() {
    }

    @Override
    public int onStartCommand(Intent intent,  int flags, int startId) {
        mContext = getApplicationContext();
        Log.d(LOG_TAG, "RebootCompleteService started");
        // Create an instance of GoogleAPIClient.

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();

        return START_STICKY;
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        GeofenceHelper geofenceHelper = new GeofenceHelper(mContext);
        DBHelper dbHelper = new DBHelper(mContext);
        geofenceHelper.connectGoogleApi();
        ArrayList<LBAction> listActions = dbHelper.getAllActions();
        geofenceHelper.registerGeofences(listActions, mGoogleApiClient);
        Log.d(LOG_TAG, "Actions registered again");

        mGoogleApiClient.disconnect();
        stopSelf();
        Log.d(LOG_TAG, "RebootCompleteService ended");

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "Google API conection suspended");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "Google API conection failed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
