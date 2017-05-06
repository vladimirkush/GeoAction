package com.vladimirkush.geoaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.model.LatLng;
import com.vladimirkush.geoaction.Adapters.SuggestionsAdapter;
import com.vladimirkush.geoaction.Asynctasks.GetAddressAsyncTask;
import com.vladimirkush.geoaction.Interfaces.SuggestionListener;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBAction.ActionType;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;
import com.vladimirkush.geoaction.Utils.BackendlessHelper;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.Utils.DBHelper;
import com.vladimirkush.geoaction.Utils.GeofenceHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ActionCreate extends AppCompatActivity implements SuggestionListener {
    private final String LOG_TAG = "LOGTAG";

    //fields
    private LatLng mAreaCenter;
    private int mRadius;
    private boolean mIsEditMode;
    private LBAction mEditedLBAction;
    private DBHelper dbHelper;
    private GeofenceHelper geofenceHelper;

    // views
    private RadioButton mRadioReminder;
    private RadioButton mRadioSMS;
    private RadioButton mRadioEmail;
    private RadioButton mRadioEnterArea;
    private RadioButton mRadioExitArea;
    private ImageButton mMapChoserButton;
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
    private PopupWindow mPopupWindow;
    private Toolbar mToolbar;
    private ActionBar mActionBar;
    private RecyclerView mRecyclerView;
    private ArrayList<LBAction> mSuggestionsList;
    private SuggestionsAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_create);

        dbHelper = new DBHelper(getApplicationContext());
        geofenceHelper = new GeofenceHelper(this);
        assignViews();



        Intent intent = getIntent();
        mIsEditMode = intent.getBooleanExtra(Constants.EDIT_MODE_KEY, false);
        if (mIsEditMode) {
            mToolbar.setTitle("Edit the action");
            long id = intent.getLongExtra(Constants.LBACTION_ID_KEY, -1);
            mEditedLBAction = dbHelper.getAction(id);
            Log.d(LOG_TAG, "Editing id: " + id);
            if (id >= 0 || mEditedLBAction != null) {
                setFieldsByAction(mEditedLBAction);
            }
        } else {
            mRadioReminder.setChecked(true);    // default checked radio
            mRadioEnterArea.setChecked(true);
            setViewByActionType(ActionType.REMINDER);
            mRadiusLabel.setText("No location choosen");
        }

        mPopupWindow = createPopupWindow();
        assignAdapter(mPopupWindow);


    }

    private void assignViews(){
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
        mToolbar = (Toolbar) findViewById(R.id.action_create_toolbar);
        mMapChoserButton = (ImageButton) findViewById(R.id.map_image_button);
        mToolbar.setTitle("Create new action");
        setSupportActionBar(mToolbar);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
    }

    // If opened in edit mode or a suggestion chosen, assign all fields per given lbAction
    private void setFieldsByAction(LBAction action) {
        mAreaCenter = action.getTriggerCenter();
        mRadius = action.getRadius();
        mRadiusLabel.setText("Radius: " + mRadius + "m");
        //mAddressLabel.setText(AddressHelper.getAddress(this,mAreaCenter));
        new GetAddressAsyncTask().execute(this, mAddressLabel, mAreaCenter);
        LBAction.DirectionTrigger dir = action.getDirectionTrigger();
        if (dir == LBAction.DirectionTrigger.ENTER) {
            mRadioEnterArea.setChecked(true);
        } else {
            mRadioExitArea.setChecked(true);
        }
        ActionType type = action.getActionType();
        setViewByActionType(type);
        // assign text values to fields
        switch (type) {
            case REMINDER:
                LBReminder rem = (LBReminder) action;
                mRadioReminder.setChecked(true);
                mReminderTitle.setText(rem.getTitle());
                mReminderText.setText(rem.getMessage());
                break;

            case SMS:
                LBSms sms = (LBSms) action;
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
        if (mIsEditMode || mAreaCenter != null) {   // in case we are editing an existing action or clicking for second time, send the params to mapchooser
            intent.putExtra(Constants.AREA_CENTER_KEY, mAreaCenter);
            intent.putExtra(Constants.AREA_RADIUS_KEY, mRadius);
        }
        startActivityForResult(intent, Constants.MAP_DATA_REQUEST_CODE);
    }

    // Collect all the text data, create an LB action and register a geofence for it
    public void onSaveActionClick(View view) {
        // if in edit mode, we need to unregister the previous one
        if (mIsEditMode) {
            List<String> listIds = new ArrayList<String>();
            listIds.add(mEditedLBAction.getID() + "");
            geofenceHelper.unregisterGeofences(listIds);
        }

        LBAction action = null;
        if (mRadioReminder.isChecked()) { // create LBRemainder TODO check input
            action =  constructActionfromFields(ActionType.REMINDER);
        } else if (mRadioSMS.isChecked()) {// create LBSms TODO check input
            action = constructActionfromFields(ActionType.SMS);
        } else {                         // create LBEmail, TODO check input
            action = constructActionfromFields(ActionType.EMAIL);
        }

        final LBAction actionForSave = action;
        if (mIsEditMode) {    // If edit mode no insertion  - only update
            actionForSave.setID(mEditedLBAction.getID());
            actionForSave.setExternalID(mEditedLBAction.getExternalID());
            dbHelper.updateAction(actionForSave);
            Log.d(LOG_TAG, actionForSave.getActionType() + " updated id: " + actionForSave.getID());
        } else {
            long id = dbHelper.insertAction(actionForSave);  // insert in the db and get ID
            actionForSave.setID(id);                         // assign ID
            Log.d(LOG_TAG, actionForSave.getActionType() + " Assigned id: " + id);
            // saving to cloud
            HashMap map = (HashMap) BackendlessHelper.getMapForSingleAction(action);
            Backendless.Data.of(BackendlessHelper.ACTIONS_TABLE_NAME).save(map, new AsyncCallback<Map>() {
                @Override
                public void handleResponse(Map map) {
                    String objID = (String) map.get(BackendlessHelper.ACTIONS_OBJECT_ID);
                    Log.d(LOG_TAG, "Assigned id from BCKNDLS: " + objID);
                    actionForSave.setExternalID(objID);
                    dbHelper.updateAction(actionForSave);

                }

                @Override
                public void handleFault(BackendlessFault backendlessFault) {
                    Log.d(LOG_TAG, "Backendless async save failed");
                }
            });
        }

        //registerGeofence(reminder);
        geofenceHelper.registerGeofence(actionForSave);


        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private LBAction constructActionfromFields(ActionType type){
        LBAction action =null; // will be reassigned based on type
        switch (type){
            case REMINDER:
                LBReminder reminder = new LBReminder();
                reminder.setDirectionTrigger(mRadioEnterArea.isChecked() ? LBAction.DirectionTrigger.ENTER : LBAction.DirectionTrigger.EXIT);
                reminder.setTitle(mReminderTitle.getText().toString());
                reminder.setMessage(mReminderText.getText().toString());
                reminder.setRadius(mRadius);
                reminder.setTriggerCenter(mAreaCenter);
                action = reminder;
                break;
            case SMS:
                LBSms lbSMS = new LBSms();
                lbSMS.setDirectionTrigger(mRadioEnterArea.isChecked() ? LBAction.DirectionTrigger.ENTER : LBAction.DirectionTrigger.EXIT);
                String toNumbers = mSmsTo.getText().toString();
                String[] numbers = TextUtils.split(toNumbers, ",");
                List<String> numsList = new ArrayList<String>(Arrays.asList(numbers));
                lbSMS.setTo(numsList);
                lbSMS.setMessage(mSmsMessage.getText().toString());
                lbSMS.setRadius(mRadius);
                lbSMS.setTriggerCenter(mAreaCenter);
                action = lbSMS;
                break;
            case EMAIL:
                LBEmail lbEmail = new LBEmail();
                lbEmail.setDirectionTrigger(mRadioEnterArea.isChecked() ? LBAction.DirectionTrigger.ENTER : LBAction.DirectionTrigger.EXIT);
                String toAddresses = mEmailTo.getText().toString();
                String[] addressesArr = TextUtils.split(toAddresses, ",");
                List<String> addressList = new ArrayList<String>(Arrays.asList(addressesArr));
                lbEmail.setTo(addressList);
                lbEmail.setSubject(mEmailSubject.getText().toString());
                lbEmail.setMessage(mEmailMessage.getText().toString());
                lbEmail.setRadius(mRadius);
                lbEmail.setTriggerCenter(mAreaCenter);
                action = lbEmail;
                break;
        }
        return action;
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
                new GetAddressAsyncTask().execute(this, mAddressLabel, mAreaCenter);
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

    private PopupWindow createPopupWindow() {
        // Initialize a new instance of LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);

        // Inflate the custom layout/view
        View popupView = inflater.inflate(R.layout.suggest_popup_window, null);

        // Set an elevation value for popup window
        // Call requires API level 21
        PopupWindow window = new PopupWindow(popupView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
        if (Build.VERSION.SDK_INT >= 21) {
            window.setElevation(5.0f);
        }

        return window;
    }

    private void assignAdapter(PopupWindow mPopupWindow) {
        mRecyclerView = (RecyclerView) mPopupWindow.getContentView().findViewById(R.id.rv_suggestions);
        mSuggestionsList = dbHelper.getAllActions();
        mAdapter = new SuggestionsAdapter(this, mSuggestionsList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter.setSuggestionListener(this);

        // decorate RecyclerView
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecoration);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_create_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.show_suggestions:
                showSuggestionsPopup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void showSuggestionsPopup() {
        if (mPopupWindow != null) {
            mPopupWindow.showAsDropDown(findViewById(R.id.show_suggestions));
        }
    }
    @Override
    public void onSuggestionClicked(int adapterPosition, LBAction action) {
        mPopupWindow.dismiss();
        setFieldsByAction(action);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }


    protected void onStart() {
        geofenceHelper.connectGoogleApi();
        super.onStart();
    }

    protected void onStop() {
        geofenceHelper.disconnectGoogleApi();
        super.onStop();
    }




    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}


