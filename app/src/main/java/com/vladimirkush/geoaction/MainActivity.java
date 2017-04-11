package com.vladimirkush.geoaction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.vladimirkush.geoaction.Adapters.ActionsListAdapter;
import com.vladimirkush.geoaction.Asynctasks.DeleteBulkFromCloud;
import com.vladimirkush.geoaction.Interfaces.DeleteItemHandler;
import com.vladimirkush.geoaction.Interfaces.SendItemHandler;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Utils.AndroidDatabaseManager;
import com.vladimirkush.geoaction.Utils.BackendlessHelper;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.Utils.DBHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback, DeleteItemHandler, SendItemHandler {
    private final String LOG_TAG = "LOGTAG";
    private final String SEND_URL_PREFIX = "http://geoaction.service/?id=";

    private GoogleApiClient mGoogleApiClient;
    private ArrayList<LBAction> mActionList;
    private DBHelper dbHelper;
    private ActionsListAdapter adapter;
    private FloatingActionButton fab;
    private BackendlessUser user;
    private boolean mIsLoginPersistent;
    private RecyclerView rvActionList;
    private Drawer mDrawer;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mToolbar= (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);

        setupDrawer();

        dbHelper = new DBHelper(getApplicationContext());

        //debug
        //deleteAllItems();
        //dbHelper.deleteDB();

        fab = (FloatingActionButton) findViewById(R.id.fab) ;
        fab.setOnTouchListener(this);
        mIsLoginPersistent = (boolean)getIntent().getExtras().get(Constants.LOGIN_IS_PERSISTENT_KEY);

        rvActionList = (RecyclerView) findViewById(R.id.rvActionsList);
        mActionList = dbHelper.getAllActions();
        adapter = new ActionsListAdapter(this, mActionList);
        rvActionList.setAdapter(adapter);
        rvActionList.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(rvActionList);

        // decorate RecyclerView
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        rvActionList.addItemDecoration(itemDecoration);

        // set handlers for delete and send
        adapter.setDeleteItemHandler(this);
        adapter.setSendItemHandler(this);
        user = Backendless.UserService.CurrentUser();
    }


    private void logOutAsync(){
        Backendless.UserService.logout(new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void aVoid) {
                Toast.makeText(getApplicationContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                Toast.makeText(getApplicationContext(), "failed to log out", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteAllItems(){
        List<LBAction> actions = dbHelper.getAllActions();
        if (actions.size() >0) {
            List<String> IDs = new ArrayList<String>();
            for (LBAction act : actions) {
                IDs.add(act.getID() + "");
            }
            unregisterGeofences(IDs);
            dbHelper.deleteAllActions();
            mActionList.clear();
            adapter.notifyDataSetChanged();
        }else{
            Toast.makeText(this,"The list is empty already", Toast.LENGTH_LONG).show();
        }
    }

    private void deleteAllItemsFromCloud(){
        String userID = user.getObjectId();
        String apiURL = "https://api.backendless.com/v1/data/bulk/Actions?where=ownerId%20%3D%20%27"
       +userID+"%27";


        DeleteBulkFromCloud bulkDelete = new DeleteBulkFromCloud(apiURL,getApplicationContext());
        bulkDelete.execute();

    }

    private void setupDrawer(){
        // Header
        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.md_amber_800)// todo change for picture
                .build();

        // Menu items
        PrimaryDrawerItem itemLocateFriends = new PrimaryDrawerItem()
                .withIdentifier(0)
                .withSelectable(false)
                .withIcon(R.drawable.people)
                .withName("Locate Friends");
        PrimaryDrawerItem itemSettings = new PrimaryDrawerItem()
                .withIdentifier(1)
                .withSelectable(false)
                .withIcon(R.drawable.gear)
                .withName("Settings");
        PrimaryDrawerItem itemChangePassword = new PrimaryDrawerItem()
                .withIdentifier(2)
                .withSelectable(false)
                .withIcon(R.drawable.key)
                .withName("Change password");
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

                                break;
                            case 1: //Settings

                                break;
                            case 2: //Change password

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
                                deleteAllItemsFromCloud();
                                break;
                        }
                        return true;
                    }
                })
                .build();

    }




    /* Fab pressed - open Action Creation activity */
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            Intent intent = new Intent(this, ActionCreate.class);
            startActivityForResult(intent, Constants.CREATE_NEW_LBACTION_REQUEST);
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
                adapter.notifyDataSetChanged();

            }
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

    @Override
    protected void onResume() {
        super.onResume();

        adapter.notifyDataSetChanged();
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

    @Override
    public void onResult(@NonNull Result result) {

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
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
            unregisterGeofences(ids);
            adapter.notifyItemRemoved(adapterPosition);
        }
    }

    @Override
    public void sendItem(int adapterPosition, LBAction action) {
        // workaround to fetch externalID from DB
        Long id = action.getID();

        LBAction actionTemp = dbHelper.getAction(id);
        String externalId = actionTemp.getExternalID();
        Log.d(LOG_TAG, "Sending action with id: "+id +" and external: "+externalId);

        if(externalId != null && externalId != "") {
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
