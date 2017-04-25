package com.vladimirkush.geoaction.Services;


import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.vladimirkush.geoaction.Utils.DBHelper;
import com.vladimirkush.services.FriendsTrackerService;

import java.util.ArrayList;


public class TrackService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private final String LOG_TAG = "LOGTAG";
    private DBHelper dbHelper;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    FriendsTrackerService mFriendsTrackerService;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "Service - onStartCommand");

        mGoogleApiClient.connect();
        return START_STICKY;
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();

        Log.d(LOG_TAG, "Service - onConnected()");
        if (!(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            Log.d(LOG_TAG, "Track service - no permissions for location. Calling stopSelf()");
            stopSelf();

        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastLocation != null) {
                Log.d(LOG_TAG, "My location- Lat:" + mLastLocation.getLatitude() + " Lon: " + mLastLocation.getLongitude());
                ArrayList<String> fr = new ArrayList<String>();
                fr.add("10208941452520304");
                fr.add("111512256074129");
                mFriendsTrackerService.getFriendsNearMeAsync(fr, mLastLocation.getLatitude(), mLastLocation.getLongitude(), new AsyncCallback<ArrayList<String>>() {
                    @Override
                    public void handleResponse(ArrayList<String> strings) {
                        if (strings.size() > 0) {
                            for (String foundFriend : strings) {
                                Log.d(LOG_TAG, "found " + foundFriend);
                            }
                        } else {
                            Log.d(LOG_TAG, "No friends found");
                        }
                        Log.d(LOG_TAG, "Handle response ended. Stopping service..");
                        stopSelf();
                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Log.d(LOG_TAG, "Error while retrieving friends near me");
                        Log.d(LOG_TAG, "Handle fault ended. Stopping service..");
                        stopSelf();
                    }
                });
            }else{// location is null
                Log.d(LOG_TAG, "Last known location is null. Stopping service..");
                stopSelf();
            }

        }

    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Service - onCreate()");
        FriendsTrackerService.initApplication(this);
        mFriendsTrackerService = FriendsTrackerService.getInstance();

        dbHelper = new DBHelper(this);
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);             // update every 1 sec
        mLocationRequest.setFastestInterval(1000);      // set max upd time of 1 sec
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);




    }


    protected void startLocationUpdates() {
        Log.d(LOG_TAG, "Location updates started");

        // check if permissions granted
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "Track service - google api connection suspended. Calling stopSelf()");
        stopSelf();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "Track service - google api connection faild. Calling stopSelf()");
        stopSelf();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "Service - onDestroy()");
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }
}
