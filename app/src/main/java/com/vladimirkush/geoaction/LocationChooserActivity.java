package com.vladimirkush.geoaction;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.services.FriendsTrackerService;

import java.util.ArrayList;

public class LocationChooserActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final String LOG_TAG = "LOGTAG";
    private final float   ZOOM_RATE = 14;

    private GoogleMap       mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location        mLastLocation;
    private LocationRequest mLocationRequest;
    private Marker          mMarker;
    private Circle          mCircle;
    private int          mRadius = 200;
    private LatLng          mAreaCenter;

    private TextView        mRadiusTextView;

    private boolean         mIsEditMode =  false;
    private boolean         mZoomOnceFlag;
    private boolean         mIsLocationUpdateStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_chooser);
        FriendsTrackerService.initApplication(this);
        mRadiusTextView = (TextView) findViewById(R.id.tv_radius);
        Intent intent = getIntent();
        if(intent.getParcelableExtra(Constants.AREA_CENTER_KEY)!= null){
            mIsEditMode = true;
            mAreaCenter = intent.getParcelableExtra(Constants.AREA_CENTER_KEY);
            mRadius  = intent.getIntExtra(Constants.AREA_RADIUS_KEY, -1);
        }

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        createLocationRequest();

        // init mapFragment and map object
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mZoomOnceFlag = false;
       // mRadius = 50;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (!(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.PERMISSION_LOCATION_REQUEST);

        } else {

            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if(!mIsEditMode) {
                showLastKnownLocationOnMap();
            }
            if(!mIsLocationUpdateStarted) {
                startLocationUpdates();
                mIsLocationUpdateStarted = true;
            }

        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "onConnectionFailed");
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if(!mIsEditMode) {
            showLastKnownLocationOnMap();
        }
        Log.d(LOG_TAG, "Lat: " + mLastLocation.getLatitude() + ", Lon: " + mLastLocation.getLongitude());
        //TODO starttest

        FriendsTrackerService friendsTrackerService = FriendsTrackerService.getInstance();
        ArrayList<String> fr = new ArrayList<String>();
        fr.add("10208941452520304");
        fr.add("111512256074129");
        friendsTrackerService.getFriendsNearMeAsync(fr, location.getLatitude(), location.getLongitude(), new AsyncCallback<ArrayList<String>>() {
            @Override
            public void handleResponse(ArrayList<String> strings) {
                if(strings.size() > 0) {
                    for (String foundFriend : strings) {
                        Log.d(LOG_TAG, "found " + foundFriend);
                    }
                }else{
                    Log.d(LOG_TAG, "No friends found");
                }
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                Log.d(LOG_TAG, "Error while retrieving friends near me");
            }
        });
        //TODO endtest
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(mCircle != null && mMarker != null) {
                    adjustCircleRadius(latLng, mMarker.getPosition());
                }
            }
        });

        showMyLocation(true);
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if(mMarker != null)
                    mMarker.remove();

                if(mCircle != null)
                    mCircle.remove();

                mMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("center of area")
                        .draggable(false));

                mCircle = mMap.addCircle(new CircleOptions()
                                        .center(latLng)
                                        .radius(mRadius)
                                        .clickable(false));
                mRadiusTextView.setText("Radius: "+ (int)mCircle.getRadius() + "m");
                Log.d(LOG_TAG, "marker pos: lat "+mMarker.getPosition().latitude+ " lon "+mMarker.getPosition().latitude);
            }
        });


        // needed to disable default click event on marker
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });

        if(mIsEditMode){    // we are in edit mode


            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(mAreaCenter)
                    .title("center of area")
                    .draggable(false));

            mCircle = mMap.addCircle(new CircleOptions()
                    .center(mAreaCenter)
                    .radius(mRadius)
                    .clickable(false));
            mRadiusTextView.setText("Radius: "+ (int)mCircle.getRadius() + "m");
            CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(mAreaCenter, ZOOM_RATE);
            mMap.animateCamera(upd);

        }else {
            showLastKnownLocationOnMap();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_LOCATION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if(!mIsLocationUpdateStarted) {
                        startLocationUpdates();
                        mIsLocationUpdateStarted = true;
                    }
                    if(!mIsEditMode) {
                        showLastKnownLocationOnMap();
                    }

                } else {
                    alertNoLocationPermissions();

                }

            }

        }
    }

    /* setup location updates' properties */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);             // update every 1 sec
        mLocationRequest.setFastestInterval(1000);      // set max upd time of 1 sec
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

            // response to user decision
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.d(LOG_TAG, "SETTINGS SUCCESS");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.d(LOG_TAG, "SETTINGS RESOLUTION REQUIRED");
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.d(LOG_TAG, "SETTINGS UNAVAILABLE");
                        break;
                }
            }
        });
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        mIsLocationUpdateStarted = false;
    }

    /* request system for periodic location updates */
    protected void startLocationUpdates() {
        Log.d(LOG_TAG, "Location updates started");

        // check if permissions granted
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        mZoomOnceFlag = false; // here we allow to center map on location for one time
    }

    /* if permission denied, alert user and exit */
    private void alertNoLocationPermissions() {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(getResources().getString(R.string.locatioNnotGrantedMsg));
        dlgAlert.setTitle(getResources().getString(R.string.locatioNnotGrantedTitle));
        dlgAlert.setPositiveButton(getResources().getString(R.string.locatioNnotGrantedButtonText), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    /* method for updating a map with new location */
    private void showLastKnownLocationOnMap() {
        if (mLastLocation != null && mMap != null) {
            // check if permissions granted
            showMyLocation(true);

            // center camera on device's location only for the first time
            if(!mZoomOnceFlag) {
                LatLng lastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(lastLatLng, ZOOM_RATE);
                mMap.moveCamera(upd);
                mZoomOnceFlag = true;
            }


        }
    }

    private void adjustCircleRadius(LatLng clickPosition, LatLng center ){

        int radius = (int)getDistanceBetween(clickPosition, center );
        mCircle.setRadius(radius);
        mRadiusTextView.setText("Radius: "+ radius + "m");

    }

    private double getDistanceBetween(LatLng src, LatLng dest){
        float[] results = new float[3];
        Location.distanceBetween(src.latitude, src.longitude,dest.latitude,dest.longitude, results);
        return results[0];
    }

    public void onConfirmClick(View view) {
        Intent returnIntent = new Intent();
        if(mCircle !=null) {
            returnIntent.putExtra(Constants.AREA_CENTER_KEY, mCircle.getCenter());
            returnIntent.putExtra(Constants.AREA_RADIUS_KEY, (int) mCircle.getRadius());
            setResult(Constants.MAP_DATA_RESULT_OK,returnIntent);

        }else{
            setResult(Constants.MAP_DATA_RESULT_CANCEL,returnIntent);
        }

        finish();
    }

    private void showMyLocation(boolean show){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(show);
        }
    }
}
