package com.vladimirkush.geoaction;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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

    private TextView        mRadiusTextView;

    private boolean         mZoomOnceFlag;
    private boolean         mIsLocationUpdateStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_chooser);

        mRadiusTextView = (TextView) findViewById(R.id.tv_radius);

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
            showLastKnownLocationOnMap();
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
        showLastKnownLocationOnMap();
        Log.d(LOG_TAG, "Lat: " + mLastLocation.getLatitude() + ", Lon: " + mLastLocation.getLongitude());


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
               adjustCircleRadius(latLng, mMarker.getPosition());
            }
        });

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

        showLastKnownLocationOnMap();

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
                    showLastKnownLocationOnMap();

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
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // show device on the map - blue dot
                mMap.setMyLocationEnabled(true);

            }

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
        if(mCircle != null && mMarker != null){
            int radius = (int)getDistanceBetween(clickPosition, center );
            mCircle.setRadius(radius);
            mRadiusTextView.setText("Radius: "+ radius + "m");
        }
    }

    private double getDistanceBetween(LatLng src, LatLng dest){
        float[] results = new float[3];
        Location.distanceBetween(src.latitude, src.longitude,dest.latitude,dest.longitude, results);
        return results[0];
    }
}
