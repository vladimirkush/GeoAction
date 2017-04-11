package com.vladimirkush.geoaction;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.vladimirkush.geoaction.Asynctasks.GetAddressAsyncTask;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBAction.ActionType;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;
import com.vladimirkush.geoaction.Services.GeofenceTransitionsIntentService;
import com.vladimirkush.geoaction.Utils.BackendlessHelper;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.Utils.DBHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ActionCreate extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback {
    private final String LOG_TAG = "LOGTAG";

    //fields
    private GoogleApiClient mGoogleApiClient;
    private LatLng mAreaCenter;
    private int mRadius;
    private boolean mIsEditMode;
    private LBAction mEditedLBAction;
    private DBHelper dbHelper;

    // views
    private RadioButton mRadioReminder;
    private RadioButton mRadioSMS;
    private RadioButton mRadioEmail;
    private RadioButton mRadioEnterArea;
    private RadioButton mRadioExitArea;
    private LinearLayout mReminderLayout;
    private LinearLayout mSMSLayout;
    private LinearLayout mEmailLayout;
    private TextView mRadiusLabel;
    private TextView mAddressLabel;

    private EditText mSmsTo;
    private EditText mSmsMessage;
    private EditText mReminderTitle;
    private EditText mReminderText;

    private EditText mEmailTo;
    private EditText mEmailSubject;
    private EditText mEmailMessage;
    private PendingIntent mGeofencePendingIntent;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_create);

        dbHelper = new DBHelper(getApplicationContext());

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
        mSmsTo = (EditText) findViewById(R.id.et_sms_to);
        mSmsMessage = (EditText) findViewById(R.id.et_sms_text);
        mEmailTo = (EditText) findViewById(R.id.et_email_to);
        mEmailSubject = (EditText) findViewById(R.id.et_email_subj);
        mEmailMessage = (EditText) findViewById(R.id.et_email_text);
        mAddressLabel = (TextView) findViewById(R.id.label_address);
        mToolbar= (Toolbar) findViewById(R.id.action_create_toolbar);
        mToolbar.setTitle("Create new action");
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        Intent intent = getIntent();
        mIsEditMode = intent.getBooleanExtra(Constants.EDIT_MODE_KEY, false);
        if(mIsEditMode){
            mToolbar.setTitle("Edit the action");
            long id = intent.getLongExtra(Constants.LBACTION_ID_KEY, -1);
            mEditedLBAction = dbHelper.getAction(id);
            Log.d(LOG_TAG, "Editing id: " + id);
            if(id >= 0 || mEditedLBAction != null) {
                setFieldsByAction(mEditedLBAction);
            }
        }else{
            mRadioReminder.setChecked(true);    // default checked radio
            mRadioEnterArea.setChecked(true);
            setViewByActionType(ActionType.REMINDER);
        }


    }

    // If opened in edit mode, assign all fields per given lbAction
    private void setFieldsByAction(LBAction action) {
        mAreaCenter = action.getTriggerCenter();
        mRadius = action.getRadius();
        mRadiusLabel.setText("Radius: " + mRadius + "m");
        //mAddressLabel.setText(AddressHelper.getAddress(this,mAreaCenter));
        new GetAddressAsyncTask().execute(this, mAddressLabel,mAreaCenter);
        LBAction.DirectionTrigger dir = action.getDirectionTrigger();
        if(dir == LBAction.DirectionTrigger.ENTER){
            mRadioEnterArea.setChecked(true);
        }else {
            mRadioExitArea.setChecked(true);
        }
        ActionType type = action.getActionType();
        setViewByActionType(type);
        // assign text values to fields
        switch (type){
            case REMINDER:
                LBReminder rem = (LBReminder) action;
                mRadioReminder.setChecked(true);
                mReminderTitle.setText(rem.getTitle());
                mReminderText.setText(rem.getMessage());
                break;

            case SMS:
                LBSms sms = (LBSms)action;
                mRadioSMS.setChecked(true);
                mSmsTo.setText(sms.getToAsSingleString());
                mSmsMessage.setText(sms.getMessage());
                break;

            case EMAIL:
                LBEmail email = (LBEmail) action;
                mRadioEmail.setChecked(true);
                mEmailTo.setText(email.getToAsSingleString());
                mEmailSubject.setText(email.getSubject());
                mEmailMessage.setText(email.getMessage());
                break;
        }
    }


    public void onRadioButtonClick(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radio_reminder:
                if (checked)
                    // reminder
                    setViewByActionType(ActionType.REMINDER);
                break;
            case R.id.radio_sms:
                if (checked)
                    // SMS
                    // check permissions
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

                        if (checkSelfPermission(Manifest.permission.SEND_SMS)
                                == PackageManager.PERMISSION_DENIED) {

                            Log.d(LOG_TAG, "permission denied to SEND_SMS - requesting it");
                            String[] permissions = {Manifest.permission.SEND_SMS};

                            requestPermissions(permissions, Constants.PERMISSION_SEND_SMS_REQUEST);
                            mRadioSMS.setEnabled(false);
                        } else {
                            mRadioSMS.setEnabled(true);
                            setViewByActionType(ActionType.SMS);


                        }
                    }
                break;
            case R.id.radio_email:
                if (checked)
                    // email
                    setViewByActionType(ActionType.EMAIL);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_SEND_SMS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mRadioSMS.setEnabled(true);
                    setViewByActionType(ActionType.SMS);

                } else {
                    mRadioSMS.setEnabled(false);

                }

            }

        }
    }

    // SMS mode - clicked "To:"
    public void btnSMSToOnclick(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, Constants.CONTACT_PICK_REQUEST_CODE);
    }

    // EMAIL mode - clicked "To:"
    public void btnEmailToOnClick(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
        startActivityForResult(intent, Constants.CONTACT_EMAILS_PICK_REQUEST_CODE);
    }

    // clicked map button
    public void onLocationChooserClick(View view) {
        Intent intent = new Intent(this, LocationChooserActivity.class);
        startActivityForResult(intent, Constants.MAP_DATA_REQUEST_CODE);
    }

    // Collect all the text data, create an LB action and register a geofence for it
    public void onSaveActionClick(View view) {
        // if in edit mode, we need to unregister the previous one
        if(mIsEditMode){
            List<String> listIds = new ArrayList<String>();
            listIds.add(mEditedLBAction.getID()+"");
            unregisterGeofences(listIds);
        }

        if (mRadioReminder.isChecked()) { // create LBRemainder TODO check input
            final LBReminder reminder = new LBReminder();
            reminder.setDirectionTrigger(mRadioEnterArea.isChecked() ? LBAction.DirectionTrigger.ENTER : LBAction.DirectionTrigger.EXIT);
            reminder.setTitle(mReminderTitle.getText().toString());
            reminder.setMessage(mReminderText.getText().toString());
            reminder.setRadius(mRadius);
            reminder.setTriggerCenter(mAreaCenter);

            if(mIsEditMode){    // If edit mode no insertion  - only update
                reminder.setID(mEditedLBAction.getID());
                dbHelper.updateAction(reminder);
                Log.d(LOG_TAG, "REMINDER updated id: " + reminder.getID());
            }else {
                long id = dbHelper.insertAction(reminder);  // insert in the db and get ID
                reminder.setID(id);                         // assign ID
                Log.d(LOG_TAG, "REMINDER Assigned id: " + id);
                // saving to cloud
                HashMap map = (HashMap) BackendlessHelper.getMapForSingleAction(reminder);
                Backendless.Data.of(BackendlessHelper.ACTIONS_TABLE_NAME).save(map, new AsyncCallback<Map>() {
                    @Override
                    public void handleResponse(Map map) {
                        String objID = (String)map.get(BackendlessHelper.ACTIONS_OBJECT_ID);
                        Log.d(LOG_TAG, "Assigned id from BCKNDLS: "+objID);
                        reminder.setExternalID(objID);
                        dbHelper.updateAction(reminder);

                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Log.d(LOG_TAG, "Backendless async save failed");
                    }
                });
            }


            registerGeofence(reminder);

        } else if (mRadioSMS.isChecked()) {// create LBSms TODO check input
            final LBSms lbSMS = new LBSms();
            lbSMS.setDirectionTrigger(mRadioEnterArea.isChecked() ? LBAction.DirectionTrigger.ENTER : LBAction.DirectionTrigger.EXIT);
            String toNumbers = mSmsTo.getText().toString();
            String[] numbers = TextUtils.split(toNumbers, ",");
            List<String> numsList = new ArrayList<String>(Arrays.asList(numbers));
            lbSMS.setTo(numsList);
            lbSMS.setMessage(mSmsMessage.getText().toString());
            lbSMS.setRadius(mRadius);
            lbSMS.setTriggerCenter(mAreaCenter);

            if(mIsEditMode) {// If edit mode no insertion  - only update
                lbSMS.setID(mEditedLBAction.getID());
                dbHelper.updateAction(lbSMS);
                Log.d(LOG_TAG, "SMS updated id: " + lbSMS.getID());

            }else {
                long id = dbHelper.insertAction(lbSMS);  // insert in the db and get ID
                lbSMS.setID(id);                         // assign ID
                Log.d(LOG_TAG, "SMS Assigned id: " + id);
                // saving to cloud
                HashMap map = (HashMap) BackendlessHelper.getMapForSingleAction(lbSMS);
                Backendless.Data.of(BackendlessHelper.ACTIONS_TABLE_NAME).save(map, new AsyncCallback<Map>() {
                    @Override
                    public void handleResponse(Map map) {
                        String objID = (String)map.get(BackendlessHelper.ACTIONS_OBJECT_ID);
                        Log.d(LOG_TAG, "Assigned id from BCKNDLS: "+objID);
                        lbSMS.setExternalID(objID);
                        dbHelper.updateAction(lbSMS);

                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Log.d(LOG_TAG, "Backendless async save failed");
                    }
                });
            }
            registerGeofence(lbSMS);


        } else {                         // create LBEmail, TODO check input
            final LBEmail lbEmail = new LBEmail();
            lbEmail.setDirectionTrigger(mRadioEnterArea.isChecked() ? LBAction.DirectionTrigger.ENTER : LBAction.DirectionTrigger.EXIT);
            String toAddresses = mEmailTo.getText().toString();
            String[] addressesArr = TextUtils.split(toAddresses, ",");
            List<String> addressList = new ArrayList<String>(Arrays.asList(addressesArr));
            lbEmail.setTo(addressList);
            lbEmail.setSubject(mEmailSubject.getText().toString());
            lbEmail.setMessage(mEmailMessage.getText().toString());
            lbEmail.setRadius(mRadius);
            lbEmail.setTriggerCenter(mAreaCenter);

            if(mIsEditMode) {// If edit mode no insertion  - only update
                lbEmail.setID(mEditedLBAction.getID());
                dbHelper.updateAction(lbEmail);
                Log.d(LOG_TAG, "EMAIL updated id: " + lbEmail.getID());
            }else {
                long id = dbHelper.insertAction(lbEmail);  // insert in the db and get ID
                lbEmail.setID(id);                         // assign ID
                Log.d(LOG_TAG, "EMAIL Assigned id: " + id);
                // saving to cloud
                HashMap map = (HashMap) BackendlessHelper.getMapForSingleAction(lbEmail);
                Backendless.Data.of(BackendlessHelper.ACTIONS_TABLE_NAME).save(map, new AsyncCallback<Map>() {
                    @Override
                    public void handleResponse(Map map) {
                        String objID = (String)map.get(BackendlessHelper.ACTIONS_OBJECT_ID);
                        Log.d(LOG_TAG, "Assigned id from BCKNDLS: "+objID);
                        lbEmail.setExternalID(objID);
                        dbHelper.updateAction(lbEmail);

                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Log.d(LOG_TAG, "Backendless async save failed");
                    }
                });
            }
            registerGeofence(lbEmail);
        }
        Intent returnIntent = new Intent();
        setResult(RESULT_OK,returnIntent);
        finish();
    }

    // setup views according to chosen type of action
    private void setViewByActionType(ActionType type) {

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
                new GetAddressAsyncTask().execute(this, mAddressLabel,mAreaCenter);
            }
            if (resultCode == Constants.MAP_DATA_RESULT_CANCEL) {
                Log.d(LOG_TAG, "area trigger chosing cancelled");
            }
        } else if (requestCode == Constants.CONTACT_PICK_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri contactUri = data.getData();
                // We only need the NUMBER column, because there will be only one row in the result
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

                Cursor cursor = getContentResolver()
                        .query(contactUri, projection, null, null, null);
                cursor.moveToFirst();

                // Retrieve the phone number from the NUMBER column
                int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(column);
                if (mSmsTo.getText().length() == 0) {
                    mSmsTo.append(number);
                } else {
                    mSmsTo.append(", " + number);
                }
            }
        } else if (requestCode == Constants.CONTACT_EMAILS_PICK_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri contactUri = data.getData();
                String[] projection = {ContactsContract.CommonDataKinds.Email.ADDRESS};

                Cursor cursor = getContentResolver()
                        .query(contactUri, projection, null, null, null);
                cursor.moveToFirst();
                int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                String emailAddr = cursor.getString(column);
                if (mEmailTo.getText().length() == 0) {
                    mEmailTo.append(emailAddr);
                } else {
                    mEmailTo.append(", " + emailAddr);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
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
        intent.putExtra(Constants.LBACTION_ID_KEY, lbAction.getID());

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
            //pIntent = getGeofencePendingIntent(lbAction);
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(lbAction),
                    getGeofencePendingIntent(lbAction)
            ).setResultCallback(this);
            Toast.makeText(this, "Geofence registered", Toast.LENGTH_LONG).show();
        }

    }

    private void unregisterGeofences(List<String> geofenceIDs) {

        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                // This is the same pending intent that was used in addGeofences().
                geofenceIDs
        )
                .setResultCallback(this); // Result processed in onResult().
        Toast.makeText(this, "Geofence id's unregistered: " + geofenceIDs.size(), Toast.LENGTH_LONG).show();
    }

    // handles result of a pending intent
    @Override
    public void onResult(@NonNull Result result) {
        Log.d(LOG_TAG, "in onResult");
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}


