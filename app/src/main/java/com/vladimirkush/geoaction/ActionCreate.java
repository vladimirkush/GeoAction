package com.vladimirkush.geoaction;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;
import com.vladimirkush.geoaction.Services.GeofenceTransitionsIntentService;
import com.google.android.gms.maps.model.LatLng;
import com.vladimirkush.geoaction.Utils.Constants;


public class ActionCreate extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback {
    private final String LOG_TAG = "LOGTAG";
    private Constants.ActionType actionType = Constants.ActionType.REMINDER;

    //fields
    private PendingIntent   mGeofencePendingIntent;
    private GoogleApiClient mGoogleApiClient;
    private Geofence        mGeofence;
    private LatLng          mAreaCenter;
    private int             mRadius;
    // views
    private RadioButton     mRadioReminder;
    private RadioButton     mRadioSMS;
    private RadioButton     mRadioEmail;
    private RadioButton     mRadioEnterArea;
    private RadioButton     mRadioExitArea;
    private LinearLayout    mReminderLayout;
    private LinearLayout    mSMSLayout;
    private LinearLayout    mEmailLayout;
    private TextView        mRadiusLabel;

    private EditText        mReminderTitle;
    private EditText        mReminderText;
    private LBAction tempAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_create);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // assign views
        mRadioReminder = (RadioButton) findViewById(R.id.radio_reminder);
        mRadioSMS = (RadioButton) findViewById(R.id.radio_sms);
        mRadioEmail = (RadioButton) findViewById(R.id.radio_email);
        mRadioEnterArea = (RadioButton) findViewById(R.id.radio_enter);
        mRadioExitArea = (RadioButton) findViewById(R.id.radio_exit);
        mReminderLayout = (LinearLayout) findViewById(R.id.reminder_container);
        mSMSLayout = (LinearLayout) findViewById(R.id.sms_container);
        mEmailLayout = (LinearLayout) findViewById(R.id.email_container);
        mRadiusLabel = (TextView) findViewById(R.id.label_radius);
        mReminderTitle = (EditText) findViewById(R.id.et_reminder_title);
        mReminderText = (EditText) findViewById(R.id.et_reminder_text);

        mRadioReminder.setChecked(true);    // default checked radio
        mRadioEnterArea.setChecked(true);
        setViewByActionType(Constants.ActionType.REMINDER);
    }

    public void onRadioButtonClick(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radio_reminder:
                if (checked)
                    // reminder
                    setViewByActionType(Constants.ActionType.REMINDER);
                break;
            case R.id.radio_sms:
                if (checked)
                    // SMS
                    setViewByActionType(Constants.ActionType.SMS);
                break;
            case R.id.radio_email:
                if (checked)
                    // email
                    setViewByActionType(Constants.ActionType.EMAIL);
                break;
        }
    }

    public void onLocationChooserClick(View view) {
        //Toast.makeText(this, "clicked chose map", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LocationChooserActivity.class);
        startActivityForResult(intent, Constants.MAP_DATA_REQUEST_CODE);
    }

    public void onSaveActionClick(View view) {
        // TODO -  save LB action , register geofence and update on cloud

        if(mRadioReminder.isChecked()){ // create LBRemainder
            LBReminder reminder = new LBReminder();
            reminder.setDirectionTrigger(mRadioEnterArea.isChecked()? LBAction.DirectionTrigger.ENTER :  LBAction.DirectionTrigger.EXIT);
            reminder.setID("temp id");
            reminder.setTitle(mReminderTitle.getText().toString());
            reminder.setMessage(mReminderText.getText().toString());
            reminder.setRadius(mRadius);
            reminder.setTriggerCenter(mAreaCenter);

            tempAction = reminder;
            registerGeofence(reminder);

        }else if(mRadioSMS.isChecked()){// create LBSms

            // TODO implement action = new LBSms();


        }else {                         // create LBEmail
            return;
            // TODO implement action = new LBEmail();


        }
        finish();
    }

    /* setup views according to chosen type of action */
    private void setViewByActionType(Constants.ActionType type) {
        actionType = type;
        switch (type) {

            case EMAIL:
                mSMSLayout.setVisibility(View.GONE);
                mReminderLayout.setVisibility(View.GONE);
                mEmailLayout.setVisibility(View.VISIBLE);
                break;
            case REMINDER:
                mSMSLayout.setVisibility(View.GONE);
                mReminderLayout.setVisibility(View.VISIBLE);
                mEmailLayout.setVisibility(View.GONE);
                break;

            case SMS:
                mSMSLayout.setVisibility(View.VISIBLE);
                mReminderLayout.setVisibility(View.GONE);
                mEmailLayout.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.MAP_DATA_REQUEST_CODE) {
            if (resultCode == Constants.MAP_DATA_RESULT_OK) {

                mAreaCenter = data.getParcelableExtra(Constants.AREA_CENTER_KEY);
                mRadius = data.getIntExtra(Constants.AREA_RADIUS_KEY, -1);
                //Toast.makeText(this, "sucess in getting map data", Toast.LENGTH_LONG).show();
                Log.d(LOG_TAG, "Center lat: " + mAreaCenter.latitude + " lon: " + mAreaCenter.longitude + " radius: " + mRadius + "m");

                mRadiusLabel.setText("Radius: " + mRadius + "m");
            }
            if (resultCode == Constants.MAP_DATA_RESULT_CANCEL) {
                Log.d(LOG_TAG, "area trigger chosing cancelled");
            }
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

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
                .setRequestId(lbAction.getID())
                .setCircularRegion(
                        lbAction.getTriggerCenter().latitude,
                        lbAction.getTriggerCenter().longitude,
                        lbAction.getRadius()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                //.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setTransitionTypes(lbAction.getDirectionTrigger() == LBAction.DirectionTrigger.ENTER ? Geofence.GEOFENCE_TRANSITION_ENTER : Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        return geofence;
    }

    // request Pending Intent from the system for geofences
    private PendingIntent getGeofencePendingIntent(LBAction lbAction) {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        intent.putExtra("Title", ((LBReminder)lbAction).getTitle());
        intent.putExtra("Text", ((LBReminder)lbAction).getMessage());
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getGeofencePendingIntentTemp() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }


    private void registerGeofence(LBAction lbAction) {
        if (!(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.PERMISSION_LOCATION_REQUEST);
                // TODO no permissions granted
        } else {

            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(lbAction),
                    getGeofencePendingIntent(lbAction)
            ).setResultCallback(this);
            Toast.makeText(this, "Geofence registered", Toast.LENGTH_LONG).show();
        }

    }

    private void unregisterGeofences(LBAction lbAction){
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                // This is the same pending intent that was used in addGeofences().
                getGeofencePendingIntentTemp())
                .setResultCallback(this); // Result processed in onResult().
        Toast.makeText(this, "Geofence unregistered", Toast.LENGTH_LONG).show();
    }

    // handles result of a pending intent
    @Override
    public void onResult(@NonNull Result result) {
        Log.d(LOG_TAG, "in onResult");
    }

    public void onDeleteActionClick(View view) {

        unregisterGeofences(tempAction);
    }
}
