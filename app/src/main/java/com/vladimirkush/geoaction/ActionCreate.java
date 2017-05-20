package com.vladimirkush.geoaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
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
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ActionCreate extends AppCompatActivity implements SuggestionListener {
    private final String LOG_TAG = "LOGTAG";

    //fields
    private LatLng          mAreaCenter;
    private int             mRadius;
    private boolean         mIsEditMode;
    private LBAction        mEditedLBAction;
    private DBHelper        dbHelper;
    private GeofenceHelper  geofenceHelper;

    // views
    private RadioButton     mRadioReminder;
    private RadioButton     mRadioSMS;
    private RadioButton     mRadioEmail;
    private RadioButton     mRadioEnterArea;
    private RadioButton     mRadioExitArea;
    private ImageButton     mMapChoserButton;
    private LinearLayout    mReminderLayout;
    private LinearLayout    mSMSLayout;
    private LinearLayout    mEmailLayout;
    private TextView        mRadiusLabel;
    private TextView        mAddressLabel;

    private EditText        mSmsTo;
    private EditText        mSmsMessage;
    private EditText        mReminderTitle;
    private EditText        mReminderText;

    private EditText        mEmailTo;
    private EditText        mEmailSubject;
    private EditText        mEmailMessage;


    private PopupWindow     mPopupWindow;
    private Toolbar         mToolbar;
    private ActionBar       mActionBar;
    private RecyclerView    mRecyclerView;
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
                mMapChoserButton.setImageResource(R.drawable.google_map_500_ic);
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
        cleanErrors();

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
        if(!checkInput()){  // first of all check input
            return;
        }
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
            HashMap map = (HashMap) BackendlessHelper.getMapForSingleAction(action);
            Backendless.Persistence.of(BackendlessHelper.ACTIONS_TABLE_NAME).save(map, new AsyncCallback<Map>() {
                    @Override
                    public void handleResponse(Map hashMap) {
                        Log.d(LOG_TAG, "object updated in cloud");
                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Log.d(LOG_TAG, "failed updating in cloud");
                    }
                });
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
                    updateSuggestionScores(actionForSave);  // update for suggestions

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
                String[] numbers = toNumbers.replaceAll("^[,\\s]+", "").split("[,\\s]+");;
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
                String[] addressesArr = toAddresses.replaceAll("^[,\\s]+", "").split("[,\\s]+");
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
                mMapChoserButton.setImageResource(R.drawable.google_map_500_ic);
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
                cursor.close();
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
                cursor.close();

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
        mSuggestionsList = dbHelper.getTopSuggestions(3);
        mAdapter = new SuggestionsAdapter(this, mSuggestionsList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter.setSuggestionListener(this);

        // decorate RecyclerView
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecoration);
    }

    /** Implements  naiive algorithm for updating scores of suggestions */
    private void updateSuggestionScores(LBAction newAction){
        ArrayList<LBAction> currentSuggestions = dbHelper.getAllSuggestions();
        long oldestID=0;
        int numOfSuggestions = 0;
        if(!currentSuggestions.isEmpty()) {
            numOfSuggestions = currentSuggestions.size();
             oldestID = currentSuggestions.get(numOfSuggestions-1).getID();
        }
        String newMessage = newAction.getMessage();
        String[] newWords = newMessage.replaceAll("^[,;\\s]+", "").split("[,;\\s]+");
        double addingScore;
        boolean wasOneTheSame = false;
        double oldScoreOfsame = 0;
        for (LBAction oldSuggestion: currentSuggestions){   // check every suggestion in database
            addingScore = 0;
            boolean sameAsOld = false;
            String oldMessage = oldSuggestion.getMessage();
            String[] oldWords = oldMessage.replaceAll("^[,;\\s]+", "").split("[,;\\s]+");

            // determine how much the suggestion fits the new action
            // by number of same words
            int numWordsmatch = countWordsMatching(newWords, oldWords);
            if (newMessage.equalsIgnoreCase(oldMessage)){
                sameAsOld = true;
                wasOneTheSame =true;
                oldScoreOfsame = oldSuggestion.getScore();
            }else if ( numWordsmatch >= 5){
                addingScore += 0.3;
            }else if ( numWordsmatch >= 3){
                addingScore += 0.2;
            }

            //determine how old is an action based on the IDs difference
            long idsDiff =  oldestID - oldSuggestion.getID() ;
            if(numOfSuggestions > 3 && idsDiff >=10){
                dbHelper.deleteSuggestion(oldSuggestion.getID());
            }else{
                addingScore -= idsDiff * 0.1;   // "aging" of old suggestions
                double newScore = oldSuggestion.getScore() + addingScore;
                oldSuggestion.setScore((newScore >=0) ? newScore : 0);
                if(sameAsOld) {
                    // if the same content, prefer to use the new one than the old, so delete the old
                    dbHelper.deleteSuggestion(oldSuggestion.getID());
                }else{
                    dbHelper.updateSuggestion(oldSuggestion);
                }
            }

        }
        if (wasOneTheSame) {
            // if we had suggestions with the same message, they were all deleted
            // and we need to insert a new one with high score, as it is the most recent and relevant
            // for user (due to fact it was equal to at least one suggestion in the past)
            newAction.setScore((oldScoreOfsame + 0.2) > 1 ? (oldScoreOfsame + 0.2) : 1 );
            dbHelper.insertSuggestion(newAction);
        }else{
            // set score to 0 as it is the new suggestion
            newAction.setScore(0);
            dbHelper.insertSuggestion(newAction);
        }
    }

    // count how many strings are in both arrays
    private int countWordsMatching(String[] arr1, String[] arr2){

        Set<String> set1 = new HashSet<String>(Arrays.asList(arr1));
        Set<String> set2 = new HashSet<String>(Arrays.asList(arr2));
        int size1 = set1.size();
        set1.removeAll(set2);
        return size1 - set1.size() ;
    }

    private boolean checkInput(){
        cleanErrors();
        boolean inputChecked=true;
        if(mAreaCenter == null || mRadius==0){
            inputChecked=false;
            mRadiusLabel.setError("No trigger area chosen!");
        }
        if (mRadioReminder.isChecked()) {
            if(mReminderText.getText().toString().equals("")){
                inputChecked=false;
                mReminderText.setError("The message cannot be empty");
            }
        } else if (mRadioSMS.isChecked()) {
            String message = mSmsMessage.getText().toString();
            String smsRecipients = mSmsTo.getText().toString();
            if(message.equals("")){
                inputChecked=false;
                mSmsMessage.setError("The message cannot be empty");
            }
            if(smsRecipients.equals("")){
                inputChecked=false;
                mSmsTo.setError("You must enter at least one number");
            }else{
                String[] numbersArr = smsRecipients.replaceAll("^[,\\s]+", "").split("[,\\s]+");
                for (String number : numbersArr){
                    Log.d(LOG_TAG,"number checking:"+ number);
                    if(!Patterns.PHONE.matcher(number).matches()){
                        mSmsTo.setError("One of the numbers is in incorrect format. Use comma for separate");
                        inputChecked = false;
                        break;
                    }
                }
            }

        } else {
            String message = mEmailMessage.getText().toString();
            String emailRecipients = mEmailTo.getText().toString();
            if(message.equals("")){
                inputChecked=false;
                mEmailMessage.setError("The message cannot be empty");
            }
            if(emailRecipients.equals("")){
                inputChecked=false;
                mEmailTo.setError("You must enter at least one address");
            }else{
                String[] addressesArr = emailRecipients.replaceAll("^[,\\s]+", "").split("[,\\s]+");
                for (String address : addressesArr){
                    address.trim();
                    if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){
                        mEmailTo.setError("one of the emails is in incorrect format. Use comma for separate");
                        inputChecked = false;
                        break;
                    }
                }
            }
        }
        return inputChecked;
    }

    private void cleanErrors(){
        mRadiusLabel.setError(null);
        mReminderText.setError(null);
        mSmsMessage.setError(null);
        mSmsTo.setError(null);
        mEmailMessage.setError(null);
        mEmailTo.setError(null);
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
                int size = dbHelper.getAllSuggestions().size();
                if(size >0) {
                    showSuggestionsPopup();
                }else{
                    Toast.makeText(this, "No suggestions yet, create action first", Toast.LENGTH_LONG).show();
                }
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
        mMapChoserButton.setImageResource(R.drawable.google_map_500_ic);
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


