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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.local.UserIdStorageFactory;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.vladimirkush.geoaction.Adapters.ActionsListAdapter;
import com.vladimirkush.geoaction.Interfaces.DeleteItemHandler;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Utils.AndroidDatabaseManager;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.Utils.DBHelper;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback, DeleteItemHandler {
    private final String LOG_TAG = "LOGTAG";

    private GoogleApiClient mGoogleApiClient;
    private ArrayList<LBAction> mActionList;
    private DBHelper dbHelper;
    private ActionsListAdapter adapter;
    private FloatingActionButton fab;
    private TextView tvLabel;
    private BackendlessUser user;
    private boolean mIsLoginPersistent;
    private RecyclerView rvActionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //debug
        //DBHelper dbHelper = new DBHelper(getApplicationContext());
        //dbHelper.deleteDB();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        dbHelper = new DBHelper(getApplicationContext());
        fab = (FloatingActionButton) findViewById(R.id.fab) ;
        fab.setOnTouchListener(this);
        tvLabel = (TextView) findViewById(R.id.label_logged_in);
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

        adapter.setDeleteItemHandler(this);

        // init Backendless API
        String backendlessKey = getString(R.string.backendless_key);
        String backendlessAppId = getString(R.string.backendless_app_id);
        String version = "v1";
        Backendless.initApp( this, backendlessAppId, backendlessKey, version );

        Backendless.UserService.isValidLogin(new AsyncCallback<Boolean>() {
            @Override
            public void handleResponse(Boolean aBoolean) {
               String currentUserObjectId = UserIdStorageFactory.instance().getStorage().get();
                Backendless.Data.of( BackendlessUser.class ).findById( currentUserObjectId, new AsyncCallback<BackendlessUser>(){

                    @Override
                    public void handleResponse(BackendlessUser backendlessUser) {
                        Log.d(LOG_TAG, "login validation success");
                        user = backendlessUser;
                        tvLabel.setText(user.getEmail());
                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Toast.makeText(getApplicationContext(), "login validation failed", Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "login validation failed: "+ backendlessFault.getMessage());
                    }
                } );

                if(!mIsLoginPersistent) {
                    user = Backendless.UserService.CurrentUser();
                    tvLabel.setText(user.getEmail());
                }
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                tvLabel.setText("not logged in");
                logOutOnClick(null);
            }
        });

    }
    public void logOutOnClick(View view) {
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





    public void dbmanagerClick(View view) {

        Intent dbmanager = new Intent(this, AndroidDatabaseManager.class);
        startActivity(dbmanager);
    }

    /* open Action Creation activity */
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
}
