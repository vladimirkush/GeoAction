package com.vladimirkush.geoaction;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
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
import com.vladimirkush.geoaction.Utils.AndroidDatabaseManager;
import com.vladimirkush.geoaction.Utils.Constants;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private final String LOG_TAG = "LOGTAG";

    private FloatingActionButton fab;
    private TextView tvLabel;
    private BackendlessUser user;
    private boolean mIsLoginPersistent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //debug
        //DBHelper dbHelper = new DBHelper(getApplicationContext());
        //dbHelper.deleteDB();
        fab = (FloatingActionButton) findViewById(R.id.fab) ;
        fab.setOnTouchListener(this);
        tvLabel = (TextView) findViewById(R.id.label_logged_in);
        mIsLoginPersistent = (boolean)getIntent().getExtras().get(Constants.LOGIN_IS_PERSISTENT_KEY);

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
            startActivity(intent);
        }
        return true;
    }
}
