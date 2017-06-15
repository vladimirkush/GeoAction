package com.vladimirkush.geoaction.Utils;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.LocalServices.GeofenceTransitionsIntentService;

import java.util.List;

public class GeofenceHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback {
    private final String    LOG_TAG = "LOGTAG";

    private PendingIntent   mGeofencePendingIntent;
    private GoogleApiClient mGoogleApiClient;
    private Context         mContext;

    // ctor
    public GeofenceHelper(Context context) {
        this.mContext = context;

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }
    // getter
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public void connectGoogleApi(){
        mGoogleApiClient.connect();
    }

    public void disconnectGoogleApi(){
        mGoogleApiClient.disconnect();
    }


    // request Pending Intent from the system for geofences
    private PendingIntent getGeofencePendingIntent(LBAction lbAction) {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(mContext, GeofenceTransitionsIntentService.class);
        intent.putExtra(Constants.LBACTION_ID_KEY, lbAction.getID());

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    /** register geofence with lbaction parameters */
    public void registerGeofence(LBAction lbAction) {
        if (!(ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            Log.d(LOG_TAG, "No permissions granted");
        } else {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(lbAction),
                    getGeofencePendingIntent(lbAction)
            ).setResultCallback(this);
        }
    }

    /** register geofence with lbaction parameters and initialized GoogleApiClient */
    public void registerGeofence(LBAction lbAction, GoogleApiClient gac) {
        if (!(ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            Log.d(LOG_TAG, "No permissions granted");
        } else {
            LocationServices.GeofencingApi.addGeofences(
                    gac,
                    getGeofencingRequest(lbAction),
                    getGeofencePendingIntent(lbAction)
            ).setResultCallback(this);
        }
    }

    public boolean isApiConnected(){
        return mGoogleApiClient.isConnected();
    }

    //Creates request for registering a Geofence in the system for tracking
    private GeofencingRequest getGeofencingRequest(LBAction lbAction) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);// change upon params
        builder.addGeofence(createGeofenceForAction(lbAction));
        return builder.build();
    }

    private Geofence createGeofenceForAction(LBAction lbAction) {
        Geofence geofence = new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(lbAction.getID() + "")
                .setCircularRegion(
                        lbAction.getTriggerCenter().latitude,
                        lbAction.getTriggerCenter().longitude,
                        lbAction.getRadius()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(lbAction.getDirectionTrigger() == LBAction.DirectionTrigger.ENTER ? Geofence.GEOFENCE_TRANSITION_ENTER : Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        return geofence;
    }

    /** register multiple geofences based on list of actions */
    public void registerGeofences(List<LBAction> actions){
        for(LBAction act:actions){
            registerGeofence(act);
        }

        Log.d(LOG_TAG, "GeofenceHelper - registered geofences: " + actions.size());

    }

    /** register multiple geofences based on list of actions using GoogleApiClients initialized externally*/
    public void registerGeofences(List<LBAction> actions, GoogleApiClient gac){
        for(LBAction act:actions){
            registerGeofence(act, gac);
        }

        Log.d(LOG_TAG, "GeofenceHelper - registered geofences: " + actions.size());

    }

    /** Remove all geofences from OS based by their ID*/
    public void unregisterGeofences(List<String> geofenceIDs) {
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                geofenceIDs
        ).setResultCallback(this); // Result processed in onResult().
    }




    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "GeofenceHelper::onConnectionSuspended()");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "GeofenceHelper::onConnectionFailed()");
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.d(LOG_TAG, "GeofenceHelper::onResult()");
    }
}
