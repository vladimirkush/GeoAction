package com.vladimirkush.geoaction;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.common.api.GoogleApiClient;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.vladimirkush.geoaction.Adapters.ActionsListAdapter;
import com.vladimirkush.geoaction.Asynctasks.CloudSyncronizer;
import com.vladimirkush.geoaction.Asynctasks.DeleteBulkFromCloud;
import com.vladimirkush.geoaction.Asynctasks.FBfriendsDownloader;
import com.vladimirkush.geoaction.Interfaces.DeleteItemHandler;
import com.vladimirkush.geoaction.Interfaces.SendItemHandler;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Services.TrackService;
import com.vladimirkush.geoaction.Utils.AndroidDatabaseManager;
import com.vladimirkush.geoaction.Utils.BackendlessHelper;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.Utils.DBHelper;
import com.vladimirkush.geoaction.Utils.GeofenceHelper;
import com.vladimirkush.geoaction.Utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, DeleteItemHandler, SendItemHandler {
    private final String        LOG_TAG = "LOGTAG";
    private final String        SEND_URL_PREFIX = "http://geoaction.service/?id=";
    private final int           MAX_GEOFENCES = 100;

    private GoogleApiClient     mGoogleApiClient;
    private ArrayList<LBAction> mActionList;
    private DBHelper            dbHelper;
    private GeofenceHelper      geofenceHelper;
    private ActionsListAdapter  mAdapter;
    private FloatingActionButton fab;
    private String              mUserId;
    private RecyclerView        rvActionList;
    private Drawer              mDrawer;
    private Toolbar             mToolbar;
    private AppEventsLogger    mFBLogger;

    private AlarmManager        mAlarmMgr;
    private PendingIntent       mAlarmIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set app settings to default only for the first time
        PreferenceManager.setDefaultValues(this, getString(R.string.shared_preferences_file_key), MODE_PRIVATE, R.xml.preferences, false);

        // facebook logger
        mFBLogger= AppEventsLogger.newLogger(this);
        mFBLogger.logEvent("new logger");
        // toolbar and drawer

        mToolbar= (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        setupDrawer();
        dbHelper = new DBHelper(getApplicationContext());
        geofenceHelper = new GeofenceHelper(this);

        // Floating action button setup
        fab = (FloatingActionButton) findViewById(R.id.fab) ;
        fab.setOnTouchListener(this);

        // recycle view adapter setup
        rvActionList = (RecyclerView) findViewById(R.id.rvActionsList);
        mActionList = dbHelper.getAllActions();
        mAdapter = new ActionsListAdapter(this, mActionList);
        rvActionList.setAdapter(mAdapter);
        rvActionList.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(rvActionList);

        // decorate RecyclerView
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        rvActionList.addItemDecoration(itemDecoration);

        // set handlers for delete and send
        mAdapter.setDeleteItemHandler(this);
        mAdapter.setSendItemHandler(this);
        mUserId = Backendless.UserService.loggedInUser();

        // Download all necessary data from external sources
        Intent incomingIntent = getIntent();
        // download all FB data
        if (SharedPreferencesHelper.isFacebookLoggedIn(this)) {
            new FBfriendsDownloader().execute();
        }
        // download actions data from cloud if it is the first log-in
        if(incomingIntent.getExtras() != null) {
            if (incomingIntent.getExtras().getBoolean(Constants.IS_INITIAL_LOGIN_KEY, false)) {
                new CloudSyncronizer(this, mAdapter, mActionList).execute();
            }
        }


        //configure alarm
        mAlarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, TrackService.class);
        mAlarmIntent = PendingIntent.getService(this, Constants.ALARM_MANAGER_REQUEST_CODE, intent, 0);
        if (!SharedPreferencesHelper.isAlarmActive(this) &&
                SharedPreferencesHelper.isFacebookLoggedIn(this)&&
                SharedPreferencesHelper.isAlarmPermitted(this)) {
            Log.d(LOG_TAG, "activating alarm for tracking service");
            SharedPreferencesHelper.setIsAlarmActive(this, true);

            mAlarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 1000, 60 * 1000, mAlarmIntent); // fire each minute
            BackendlessHelper.setMeTrackableAsync(true);
        }else{
            Log.d(LOG_TAG, "alarm not activated:");
            Log.d(LOG_TAG, "is currently active: " + SharedPreferencesHelper.isAlarmActive(this));
            Log.d(LOG_TAG, "is facebook logged in: " + SharedPreferencesHelper.isFacebookLoggedIn(this));
            Log.d(LOG_TAG, "is permitted by setting: " + SharedPreferencesHelper.isAlarmPermitted(this));
        }

    }

    private void logOutAsync(){
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging out...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                BackendlessHelper.setMeTrackableSync(false);
                Backendless.UserService.logout(new AsyncCallback<Void>() {
                    @Override
                    public void handleResponse(Void aVoid) {
                        deleteAllItems();
                        dbHelper.deleteAllSuggestions();
                        dbHelper.deleteAllFriends();


                        //stop friends tracking feature if activated
                        if(SharedPreferencesHelper.isAlarmActive(getApplicationContext())) {
                            mAlarmMgr.cancel(mAlarmIntent);
                            SharedPreferencesHelper.setIsAlarmPermitted(getApplicationContext(), true);
                            SharedPreferencesHelper.setIsAlarmActive(getApplicationContext(), false);
                            Log.d(LOG_TAG, "Tracking service alarmmanager stopped");
                        }

                        //set global flags
                        SharedPreferencesHelper.setIsFacebookLoggedIn(getApplicationContext(), false);
                        SharedPreferencesHelper.clearAllPreferences(getApplicationContext());
                        PreferenceManager.setDefaultValues(getApplicationContext(), getString(R.string.shared_preferences_file_key), MODE_PRIVATE, R.xml.preferences, true);
                        // lget back to login activity
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        // notify user on sucessfull logout
                        Toast.makeText(getApplicationContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                        progressDialog.dismiss();
                        finish();
                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Toast.makeText(getApplicationContext(), "failed to log out", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        thread.start();
    }

    private void deleteAllItems(){
        List<LBAction> actions = dbHelper.getAllActions();
        if (actions.size() >0) {
            List<String> IDs = new ArrayList<String>();
            for (LBAction act : actions) {
                IDs.add(act.getID() + "");
            }
            geofenceHelper.unregisterGeofences(IDs);
            dbHelper.deleteAllActions();
            mActionList.clear();
            mAdapter.notifyDataSetChanged();
        }else{
            Toast.makeText(this,"The list is empty already", Toast.LENGTH_LONG).show();
        }
    }

    private void deleteAllItemsFromCloud(){

        String apiURL = "https://api.backendless.com/v1/data/bulk/Actions?where=ownerId%20%3D%20%27"
       + mUserId +"%27";


        DeleteBulkFromCloud bulkDelete = new DeleteBulkFromCloud(apiURL,getApplicationContext());
        bulkDelete.execute();

    }

    private void setupDrawer(){
        // Header
        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                //.withHeaderBackground(R.color.md_amber_800)// todo change for picture
                .withHeaderBackground(R.drawable.geoaction_big_logo)
                .build();

        // Menu items
        PrimaryDrawerItem itemLocateFriends = new PrimaryDrawerItem()
                .withIdentifier(0)
                .withSelectable(false)
                .withIcon(R.drawable.people)
                .withName("Locate Friends")
                .withEnabled(false)
                .withDescription("you must login with facebook");
        if(SharedPreferencesHelper.isFacebookLoggedIn(this)){
            itemLocateFriends.withEnabled(true).withDescription(null);
        }

        PrimaryDrawerItem itemSettings = new PrimaryDrawerItem()
                .withIdentifier(1)
                .withSelectable(false)
                .withIcon(R.drawable.gear)
                .withName("Settings");
        PrimaryDrawerItem itemChangePassword = new PrimaryDrawerItem()
                .withIdentifier(2)
                .withSelectable(false)
                .withIcon(R.drawable.key)
                .withName("Change password")
                .withEnabled(true);
        if(SharedPreferencesHelper.isFacebookLoggedIn(this)){
            itemChangePassword.withEnabled(false)
            .withDescription("available for non-facebook users");
        }

        PrimaryDrawerItem itemLogOut = new PrimaryDrawerItem()
                .withIdentifier(3)
                .withSelectable(false)
                .withIcon(R.drawable.logout)
                .withName("Logout");
        SecondaryDrawerItem itemDBManager = new SecondaryDrawerItem()
                .withIdentifier(4)
                .withSelectable(false)
                .withName("DBmanager");
        SecondaryDrawerItem itemDeleteAllActions = new SecondaryDrawerItem()
                .withIdentifier(5)
                .withSelectable(false)
                .withName("Delete all actions");


        // Nav Drawer building
        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(mToolbar)
                .withDisplayBelowStatusBar(true)
                .withTranslucentStatusBar(true)
                .withAccountHeader(header)
                .withSelectedItem(-1)
                .addDrawerItems(
                        itemLocateFriends,
                        new DividerDrawerItem(),
                        itemSettings,
                        new DividerDrawerItem(),
                        itemChangePassword,
                        new DividerDrawerItem(),
                        itemLogOut,
                        new DividerDrawerItem(),
                        itemDBManager,
                        new DividerDrawerItem(),
                        itemDeleteAllActions
                        )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        mDrawer.closeDrawer();

                        switch((int)drawerItem.getIdentifier()){
                            case 0: //Locate friends
                                Intent friendsIntent  = new Intent(getApplicationContext(), FriendsActivity.class);
                                startActivity(friendsIntent);
                                break;
                            case 1: //Settings
                                Intent settingsIntent  = new Intent(getApplicationContext(), SettingsActivity.class);
                                startActivity(settingsIntent);
                                break;
                            case 2: //Change password
                                Intent changePassIntent  = new Intent(getApplicationContext(), ChangePasswordActivity.class);
                                startActivityForResult(changePassIntent, Constants.CHANGE_PASSWORD_REQEST_CODE);
                                break;
                            case 3: // Logout
                                logOutAsync();
                                break;
                            case 4: // DB manager
                                Intent dbmanager = new Intent(getApplicationContext(), AndroidDatabaseManager.class);
                                startActivity(dbmanager);
                                break;

                            case 5: //Delete al items
                                deleteAllItems();
                                //deleteAllItemsFromCloud();
                                break;
                        }
                        return true;
                    }
                })
                .build();

    }

    private void alertCannotAddAction() {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage("Maximum number of location-based actions is reached");
        dlgAlert.setTitle("Cannot add new action");
        dlgAlert.setPositiveButton("Got it", null);

        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }




    /* Fab pressed - open Action Creation activity */
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if(mActionList.size()== MAX_GEOFENCES) {
                // Prevent creating more than 100 geo actions,
                // as android cannot support more than 100 geofences per app
                alertCannotAddAction();
            }else{
                Intent intent = new Intent(this, ActionCreate.class);
                startActivityForResult(intent, Constants.CREATE_NEW_LBACTION_REQUEST);
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == Constants.CREATE_NEW_LBACTION_REQUEST ||
                requestCode == Constants.EDIT_EXISTING_LBACTION_REQUEST ){
            if(resultCode == RESULT_OK){
                ArrayList<LBAction> actions = dbHelper.getAllActions();
                mActionList.clear();
                mActionList.addAll(actions);
                mAdapter.notifyDataSetChanged();
            }
        }else if(requestCode == Constants.CHANGE_PASSWORD_REQEST_CODE ){
            if(resultCode == RESULT_OK){
                logOutAsync();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
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
    public void deleteItem(int adapterPosition, LBAction action) {
        Long id = action.getID();
        // workaround to fetch externalID from DB
        LBAction actionTemp = dbHelper.getAction(id);

        //remove from Backendless cloud
        HashMap map = (HashMap) BackendlessHelper.getMapForSingleAction(actionTemp);
        final String objID = (String)map.get(BackendlessHelper.ACTIONS_OBJECT_ID);
        // remove from cloud
        Backendless.Persistence.of( BackendlessHelper.ACTIONS_TABLE_NAME ).remove(map, new AsyncCallback<Long>() {
            @Override
            public void handleResponse(Long aLong) {
                Log.d(LOG_TAG, "Action deleted from Backendless: "+objID);
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                Log.d(LOG_TAG, "Error deleting from Backendless: "+objID);
                Log.d(LOG_TAG, backendlessFault.getMessage());
            }
        });

        // remove from local DB
        long actionID = action.getID();
        int res =  dbHelper.deleteAction(actionID);
        if (res == 1){
            mActionList.remove(action);
            List <String> ids = new ArrayList<String>();
            ids.add(actionID+"");
            geofenceHelper.unregisterGeofences(ids);
            mAdapter.notifyItemRemoved(adapterPosition);
        }
    }

    @Override
    public void sendItem(int adapterPosition, LBAction action) {
        // workaround to fetch externalID from DB
        Long id = action.getID();

        LBAction actionTemp = dbHelper.getAction(id);
        String externalId = actionTemp.getExternalID();
        Log.d(LOG_TAG, "Sending action with id: "+id +" and external: "+externalId);

        if(externalId != null && !externalId.equals("")) {
            String request = SEND_URL_PREFIX + externalId;
            Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "geo action");
            sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, request);
            startActivity(Intent.createChooser(sendIntent, "Send via"));
        }else{
            Toast.makeText(this,"Something went wrong, please try again",Toast.LENGTH_LONG).show();
        }
    }


}
