package com.vladimirkush.geoaction;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.local.UserIdStorageFactory;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;
import com.vladimirkush.geoaction.Utils.AddressHelper;
import com.vladimirkush.geoaction.DataAccess.BackendlessHelper;
import com.vladimirkush.geoaction.DataAccess.DBHelper;
import com.vladimirkush.geoaction.Utils.GeofenceHelper;

import java.util.HashMap;
import java.util.Map;

public class RecieveActivity extends AppCompatActivity implements OnMapReadyCallback,  ResultCallback {
    private final String    LOG_TAG = "LOGTAG";
    private final String    MAIN_TITLE = "New Geo Action Incoming: ";

    //views
    private TextView        mMainTitle;
    private TextView        mToContent;
    private TextView        mSubjectLabel;
    private TextView        mSubjectContent;
    private TextView        mMessageContent;
    private TextView        mRadiusContent;
    private TextView        mDirection;
    private TextView        mAddress;

    private Button          mAcceptBtn;
    private Button          mCancelBtn;

    private LinearLayout    mToLayout;
    private LinearLayout    mSubjectLayout;

    private Circle          mCircle;
    private int             mRadius;
    private LatLng          markerLocation;

    private String          externalId;
    private LBAction        lbAction;
    private GoogleMap       mMap;
    private GeofenceHelper  mGeofenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recieve);
        findViews();
        clearAllFields();

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();
        externalId = data.getQueryParameter("id");
        String msg = "Received id: " + externalId;
        Log.d(LOG_TAG,msg );

        mGeofenceHelper = new GeofenceHelper(this);

        // init mapFragment and map object
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.rcv_map);
        mapFragment.getMapAsync(this);

        // init Backendless API
        String backendlessKey = getString(R.string.backendless_key);
        String backendlessAppId = getString(R.string.backendless_app_id);
        String version = "v1";
        Backendless.initApp( this, backendlessAppId, backendlessKey, version );

        Backendless.Persistence.of( BackendlessHelper.ACTIONS_TABLE_NAME ).findById(externalId, new AsyncCallback<Map>() {
               @Override
               public void handleResponse(Map map) {

                   lbAction = BackendlessHelper.getActionFromMapping(map);
                   Log.d(LOG_TAG, "Fetching action from cloud - success: "+ externalId );
                   setLayoutAndFields(lbAction);
                   if(mMap != null){
                       mMap.getUiSettings().setMapToolbarEnabled(false);
                        mMap.addMarker(new MarkerOptions()
                               .position(markerLocation)
                               .title("center of area")
                               .draggable(false));

                       mCircle = mMap.addCircle(new CircleOptions()
                               .center(markerLocation)
                               .radius(mRadius)
                               .clickable(false));

                       mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                               mCircle.getCenter(), getZoomLevel(mCircle)));
                       String addressStr = AddressHelper.getAddress(getApplicationContext(),lbAction.getTriggerCenter());
                       mAddress.setText(addressStr); // set address
                   }

               }

               @Override
               public void handleFault(BackendlessFault backendlessFault) {
                   Log.d(LOG_TAG, "Fetching action from cloud failed" );
                   Log.d(LOG_TAG, backendlessFault.getMessage() );
                   if (backendlessFault.getCode().equals("1000")){
                       showErrorDialog("We cannot get the Geo Action as it doesn't exist anymore :(");

                   }else {
                       showErrorDialog("Network error occured, you can try one more time when the network is available");
                   }
               }
       });

    }

    public void onAcceptClick(View view) {
        // check if user is logged in, if yes add to db, if no send to login
        mAcceptBtn.setEnabled(false);
        mCancelBtn.setEnabled(false);
        if(lbAction != null){
            Backendless.UserService.isValidLogin(new AsyncCallback<Boolean>() {
                @Override
                public void handleResponse(Boolean aBoolean) {
                    String currentUserObjectId = UserIdStorageFactory.instance().getStorage().get();
                    Backendless.Data.of( BackendlessUser.class ).findById( currentUserObjectId, new AsyncCallback<BackendlessUser>(){

                        @Override
                        public void handleResponse(BackendlessUser backendlessUser) {
                            Log.d(LOG_TAG, "login validation success");
                            final DBHelper dbHelper = new DBHelper(getApplicationContext());
                            long id =  dbHelper.insertAction(lbAction);
                            lbAction.setID(id);
                            lbAction.setExternalID(null); // need for creating a new one on backend instead of updating current
                            HashMap map = (HashMap) BackendlessHelper.getMapForSingleAction(lbAction);
                            Backendless.Data.of(BackendlessHelper.ACTIONS_TABLE_NAME).save(map, new AsyncCallback<Map>() {
                                @Override
                                public void handleResponse(Map map) {
                                    String objID = (String)map.get(BackendlessHelper.ACTIONS_OBJECT_ID);
                                    Log.d(LOG_TAG, "Assigned id from BCKNDLS: "+objID);
                                    lbAction.setExternalID(objID);
                                    dbHelper.updateAction(lbAction);

                                }

                                @Override
                                public void handleFault(BackendlessFault backendlessFault) {
                                    Log.d(LOG_TAG, "Backendless async save failed");
                                }
                            });
                            mGeofenceHelper.registerGeofence(lbAction);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            finish();

                        }

                        @Override
                        public void handleFault(BackendlessFault backendlessFault) {
                            Toast.makeText(getApplicationContext(), "login validation failed", Toast.LENGTH_SHORT).show();
                            Log.d(LOG_TAG, "login validation failed: "+ backendlessFault.getMessage());
                            showErrorDialog("You are not logged in. Please log in the app and try again");


                        }
                    } );

                }

                @Override
                public void handleFault(BackendlessFault backendlessFault) {
                    Log.d(LOG_TAG, "User not logged in");
                    showErrorDialog("You are not logged in. Please log in the app and try again");

                }
            });


        }

    }


    public void onCancellClick(View view) {
        this.finish();
    }

    private void showErrorDialog(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
                .setTitle("Error");
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // get zoom level based on circle radius
    private int getZoomLevel(Circle circle) {
        int zoomLevel = 11;
        if (circle != null) {
            double radius = circle.getRadius() + circle.getRadius() / 2;
            double scale = radius / 500;
            zoomLevel = (int) (16 - Math.log(scale) / Math.log(2));
        }
        return zoomLevel;
    }

    // Assign values to views according to action type
    private void setLayoutAndFields(LBAction action){
        LBAction.ActionType type = action.getActionType();
        mRadius = action.getRadius();
        markerLocation = action.getTriggerCenter();
        String radiusStr = "Radius: " + mRadius + "m";
        mRadiusContent.setText(radiusStr);
        String titleStr = MAIN_TITLE + type.toString();
        mMainTitle.setText(titleStr);

        if(action.getDirectionTrigger() == LBAction.DirectionTrigger.ENTER){
            mDirection.setText(", on ENTERING area");
        }else{
            mDirection.setText(", on EXITING area");
        }

        switch (type){
            case REMINDER:

                mToLayout.setVisibility(View.GONE);
                LBReminder reminder = (LBReminder)action;
                mSubjectLabel.setText("Title: ");
                mSubjectContent.setText(reminder.getTitle());
                mMessageContent.setText(reminder.getMessage());

                break;

            case SMS:
                mSubjectLayout.setVisibility(View.GONE);
                LBSms sms = (LBSms) action;
                mToContent.setText(sms.getToAsSingleString());
                mMessageContent.setText(sms.getMessage());
                break;

            case EMAIL:
                LBEmail email = (LBEmail) action;
                mToContent.setText(email.getToAsSingleString());
                mSubjectLabel.setText("Subject: ");
                mSubjectContent.setText(email.getSubject());
                mMessageContent.setText(email.getMessage());
                break;
        }
        mAddress.setText("Address stub");
    }

    // find all view references
    private void findViews(){
        mMainTitle = (TextView) findViewById(R.id.receive_title_type);
        mToContent= (TextView) findViewById(R.id.receive_to_content);
        mSubjectLabel= (TextView) findViewById(R.id.receive_subject_lbl);
        mSubjectContent= (TextView) findViewById(R.id.receive_subject_content);
        mMessageContent= (TextView) findViewById(R.id.receive_message_content);
        mRadiusContent= (TextView) findViewById(R.id.receive_radius_content);
        mDirection= (TextView) findViewById(R.id.receive_direction);
        mAddress= (TextView) findViewById(R.id.receive_address_content);

        mToLayout= (LinearLayout) findViewById(R.id.receive_to_layout);
        mSubjectLayout= (LinearLayout) findViewById(R.id.receive_subject_layout);

        mAcceptBtn = (Button) findViewById(R.id.recieve_accept_btn);
        mCancelBtn = (Button) findViewById(R.id.receive_cancell_btn);
    }

    private void clearAllFields(){
        mMainTitle.setText("");
        mToContent.setText("");
        mSubjectLabel.setText("");
        mSubjectContent.setText("");
        mMessageContent.setText("");
        mRadiusContent.setText("");
        mDirection.setText("");
        mAddress.setText("");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }




    @Override
    protected void onStart() {
       mGeofenceHelper.connectGoogleApi();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGeofenceHelper.disconnectGoogleApi();
        super.onStop();
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.d(LOG_TAG, "in onResult");
    }


}
