package com.vladimirkush.geoaction.Services;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.vladimirkush.geoaction.FriendsActivity;
import com.vladimirkush.geoaction.Models.Friend;
import com.vladimirkush.geoaction.R;
import com.vladimirkush.geoaction.Utils.BackendlessHelper;
import com.vladimirkush.geoaction.Utils.DBHelper;
import com.vladimirkush.geoaction.Utils.SharedPreferencesHelper;
import com.vladimirkush.services.FriendsTrackerService;

import java.util.ArrayList;


public class TrackService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private final String LOG_TAG = "LOGTAG";
    private final long MILLIS_NOSPAM_DIFF = 1000 * 60 * 2;  // 30m
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
                // updating current location in cloud
                BackendlessHelper.updateMyLocationInCloudAsync(mLastLocation);
                Log.d(LOG_TAG, "My location Lat:" + mLastLocation.getLatitude() + " Lon: " + mLastLocation.getLongitude());
                ArrayList<String> fr = dbHelper.getAllTrackedFriiendsFBIDs();
                //ArrayList<String> fr = new ArrayList<String>();
                //fr.add("10208941452520304");
                //fr.add("111512256074129");
                // web service call
                mFriendsTrackerService.getFriendsNearMeAsync(fr, mLastLocation.getLatitude(), mLastLocation.getLongitude(), new AsyncCallback<ArrayList<String>>() {
                    @Override
                    public void handleResponse(ArrayList<String> strings) {
                        long currentTimeMillis = System.currentTimeMillis();
                        if (strings.size() > 0) {
                            if (strings.size() > 1) {

                                boolean wasAtLeastOneNotNotifiedYet =true;

                                for (String fbid : strings) {
                                    Log.d(LOG_TAG, "found " + fbid);
                                    Friend f = dbHelper.getFriendByFBId(fbid);
                                    f.setNear(true);

                                    // if happened longer then MILLIS_NOSPAM_DIFF, update timestamp
                                    if(checkIfTimeLongerThan(f.getLastNearTimeMillis(), currentTimeMillis, MILLIS_NOSPAM_DIFF)){
                                        f.setLastNearTimeMillis(currentTimeMillis);
                                    }else{
                                        wasAtLeastOneNotNotifiedYet = false;
                                    }
                                    dbHelper.updateFriend(f);
                                }
                                if(wasAtLeastOneNotNotifiedYet) {
                                    sendNotification("Friends near you", "You have several friends around!");
                                }
                            } else if (strings.size() == 1) {
                                Friend f = dbHelper.getFriendByFBId(strings.get(0));
                                f.setNear(true);

                                if(checkIfTimeLongerThan(f.getLastNearTimeMillis(), currentTimeMillis, MILLIS_NOSPAM_DIFF)){
                                    sendNotification("Friend near you", f.getName() + " is around you!");
                                    // if happened longer then MILLIS_NOSPAM_DIFF, update timestamp
                                    f.setLastNearTimeMillis(currentTimeMillis);
                                    dbHelper.updateFriend(f);

                                }else{
                                    Log.d(LOG_TAG, "friends timstamp does not exceed the difference, no notification");

                                }

                            }


                        }else {
                            Log.d(LOG_TAG, "No friends found");
                        }
                        setNotNearFriendsStatus(strings);
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
    private boolean checkIfTimeLongerThan(long pastTimeMills, long currentTimeMills, long diffParamMills){
        long diffMillis = currentTimeMills - pastTimeMills ;
        return diffMillis >= diffParamMills;
    }



    private void setNotNearFriendsStatus(ArrayList<String> foundFriends){
        ArrayList<Friend> friends = dbHelper.getAllFriends();
        int count= 0;
        for(Friend f:friends){
            if(!foundFriends.contains(f.getFbID())){  //  if a friend in db is not near, set it up
                f.setNear(false);
                dbHelper.updateFriend(f);
                count++;
            }
        }
        Log.d(LOG_TAG, "Friends in db updated: " + count);

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

    private void sendNotification(String title, String text) {
        long when = System.currentTimeMillis();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.two_friends))
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSound(SharedPreferencesHelper.getNotificationURI(this))
                        .setAutoCancel(true);
        if(SharedPreferencesHelper.isVibratePermitted(this)){
            mBuilder.setVibrate(new long[]{1000, 1000, 1000});
        }
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, FriendsActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        //Intent mainIntent = new Intent(this, LoginActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(FriendsActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        //stackBuilder.addNextIntent(mainIntent);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        int mId = (int) when;
        mNotificationManager.notify(mId, mBuilder.build());
    }
}
