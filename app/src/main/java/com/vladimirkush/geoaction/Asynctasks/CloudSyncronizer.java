package com.vladimirkush.geoaction.Asynctasks;


import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;
import com.vladimirkush.geoaction.Adapters.ActionsListAdapter;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Utils.BackendlessHelper;
import com.vladimirkush.geoaction.Utils.DBHelper;
import com.vladimirkush.geoaction.Utils.GeofenceHelper;

import java.util.ArrayList;
import java.util.Map;

public class CloudSyncronizer extends AsyncTask<Void, Void, Void> {
    private final String LOG_TAG = "LOGTAG";

    private DBHelper dbHelper;
    private Activity activity;
    private ProgressDialog mProgressDialog;
    private ActionsListAdapter adapter;
    private ArrayList<LBAction> actionList;
    private GeofenceHelper geofenceHelper;

    // ctor
    public CloudSyncronizer(Activity activity, ActionsListAdapter adapter, ArrayList<LBAction> actionList){
        this.activity = activity;
        dbHelper = new DBHelper(activity);
        geofenceHelper = new GeofenceHelper(activity);
        geofenceHelper.connectGoogleApi();
        this.adapter = adapter;
        this.actionList = actionList;


    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(activity);
        mProgressDialog.setMessage("Syncing...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();


    }

    @Override
    protected Void doInBackground(Void... params) {
        String userId = Backendless.UserService.loggedInUser();
        Log.d(LOG_TAG, "UserId: " + userId);
        String whereClause = "ownerId = '" + userId + "'";
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause( whereClause );
        dataQuery.setPageSize( 100 );
        BackendlessCollection<Map> result;
        try {
            // download actions from cloud that current user created in the past
            result = Backendless.Persistence.of("Actions").find(dataQuery);
        }catch (BackendlessException ex){
            Log.d(LOG_TAG, "Get all actions from cloud failed: " + ex.getMessage());
            return null;
        }
        Log.d(LOG_TAG, "Actions downloaded successfully: " + result.getTotalObjects());

        // load all results in db
        for(Map map: result.getCurrentPage()){
            LBAction action = BackendlessHelper.getActionFromMapping(map);
            dbHelper.insertAction(action);
        }
        ArrayList<LBAction> actions = dbHelper.getAllActions();
        actionList.addAll(actions);
        geofenceHelper.registerGeofences(actionList);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        adapter.notifyDataSetChanged();

        geofenceHelper.disconnectGoogleApi();
        if(mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
