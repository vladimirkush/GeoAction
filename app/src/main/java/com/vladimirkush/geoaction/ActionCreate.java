package com.vladimirkush.geoaction;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.vladimirkush.geoaction.Services.GeofenceTransitionsIntentService;
import com.google.android.gms.maps.model.LatLng;
import com.vladimirkush.geoaction.Utils.Constants;


public class ActionCreate extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final String LOG_TAG = "LOGTAG";
    private Constants.ActionType actionType = Constants.ActionType.REMINDER;

    //fields
    private PendingIntent mGeofencePendingIntent;
    private GoogleApiClient mGoogleApiClient;
    // views
    private RadioButton mRadioReminder;
    private RadioButton mRadioSMS;
    private RadioButton mRadioEmail;
    private RadioButton mRadioEnterArea;
    private RadioButton mRadioExitArea;
    private LinearLayout mReminderLayout;
    private LinearLayout mSMSLayout;
    private LinearLayout mEmailLayout;
    private TextView    mRadiusLabel;


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
        mRadioEnterArea= (RadioButton) findViewById(R.id.radio_enter);
        mRadioExitArea = (RadioButton) findViewById(R.id.radio_exit);
        mReminderLayout = (LinearLayout) findViewById(R.id.reminder_container);
        mSMSLayout = (LinearLayout) findViewById(R.id.sms_container);
        mEmailLayout = (LinearLayout) findViewById(R.id.email_container);
        mRadiusLabel = (TextView) findViewById(R.id.label_radius);



        mRadioReminder.setChecked(true);    // default checked radio
        mRadioEnterArea.setChecked(true);
        setViewByActionType(Constants.ActionType.REMINDER);
    }

    public void onRadioButtonClick(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
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
    }

    /* setup views according to chosen type of action */
    private void setViewByActionType(Constants.ActionType type){
        actionType = type;
        switch (type){

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
            if(resultCode == Constants.MAP_DATA_RESULT_OK){

                LatLng areaCenter = data.getParcelableExtra(Constants.AREA_CENTER_KEY);
                int radius = data.getIntExtra(Constants.AREA_RADIUS_KEY, -1);
                //Toast.makeText(this, "sucess in getting map data", Toast.LENGTH_LONG).show();
                Log.d(LOG_TAG, "Center lat: "+areaCenter.latitude + " lon: "+ areaCenter.longitude +" radius: " + radius + "m");

                mRadiusLabel.setText("Radius: " + radius + "m");
            }
            if (resultCode == Constants.MAP_DATA_RESULT_CANCEL) {
                Log.d(LOG_TAG, "area trigger chosing cancelled");
            }
        }
    }

    // request Pending Intent from the system
    private PendingIntent getGeofencePendingIntent() {
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
}
