package com.vladimirkush.geoaction;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.gms.maps.GoogleMap;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;
import com.vladimirkush.geoaction.Utils.BackendlessHelper;

import java.util.Map;

public class RecieveActivity extends AppCompatActivity {
    private final String LOG_TAG = "LOGTAG";
    private final String MAIN_TITLE = "New Geo Action Incoming: ";

    //views
    private TextView mMainTitle;
    private TextView mToContent;
    private TextView mSubjectLabel;
    private TextView mSubjectContent;
    private TextView mMessageContent;
    private TextView mRadiusContent;
    private TextView mDirection;
    private TextView mAddress;


    private LinearLayout mToLayout;
    private LinearLayout mSubjectLayout;


    private String externalId;
    private LBAction lbAction;
    private GoogleMap mMap;

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

               }

               @Override
               public void handleFault(BackendlessFault backendlessFault) {
                   Log.d(LOG_TAG, "Fetching action from cloud failed" );
                   Log.d(LOG_TAG, backendlessFault.getMessage() );
                   showErrorDialog("Network error occured, you can try one more time when the network is available");

               }
       });

    }

    public void onAcceptClick(View view) {
        // check if user is logged in, if yes add to db, if no send to login

        finish();
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

    // Assign values to views according to action type
    private void setLayoutAndFields(LBAction action){
        LBAction.ActionType type = action.getActionType();
        String radiusStr = action.getRadius()+"m";
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
}
