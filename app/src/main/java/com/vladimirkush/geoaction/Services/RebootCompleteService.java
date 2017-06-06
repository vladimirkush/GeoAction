package com.vladimirkush.geoaction.Services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.R;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.Utils.DBHelper;
import com.vladimirkush.geoaction.Utils.GeofenceHelper;
import com.vladimirkush.geoaction.Utils.SharedPreferencesHelper;

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

        // init Backendless API
        String backendlessKey = getString(R.string.backendless_key);
        String backendlessAppId = getString(R.string.backendless_app_id);
        String version = "v1";
        Backendless.initApp( this, backendlessAppId, backendlessKey, version );

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
        Backendless.UserService.isValidLogin(new AsyncCallback<Boolean>() {
             @Override
             public void handleResponse(Boolean loginValid) {
                 if (loginValid){
                     GeofenceHelper geofenceHelper = new GeofenceHelper(mContext);
                     DBHelper dbHelper = new DBHelper(mContext);
                     geofenceHelper.connectGoogleApi();
                     ArrayList<LBAction> listActions = dbHelper.getAllActions();
                     geofenceHelper.registerGeofences(listActions, mGoogleApiClient);
                     Log.d(LOG_TAG, "Actions registered again");
                     mGoogleApiClient.disconnect();

                    if(SharedPreferencesHelper.isFacebookLoggedIn(mContext) &&
                            SharedPreferencesHelper.isAlarmPermitted(mContext)){
                        //configure alarm
                        AlarmManager mAlarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                        Intent intent = new Intent(mContext, TrackService.class);
                        PendingIntent mAlarmIntent = PendingIntent.getService(mContext, Constants.ALARM_MANAGER_REQUEST_CODE, intent, 0);
                        Log.d(LOG_TAG, "activating alarm for tracking service");
                        //SharedPreferencesHelper.setIsAlarmActive(ctx, true);

                        mAlarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                SystemClock.elapsedRealtime() + 1000, 60 * 1000, mAlarmIntent); // fire each minute
                    }


                     stopSelf();
                     Log.d(LOG_TAG, "RebootCompleteService ended");
                 }else{
                     Log.d(LOG_TAG, "Backendless login is invalid, stopping service");
                     stopSelf();
                 }
             }

             @Override
             public void handleFault(BackendlessFault backendlessFault) {
                 Log.d(LOG_TAG, "Backendless error, stopping service");
                 stopSelf();

             }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "Google API conection suspended");
        stopSelf();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "Google API conection failed");
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
