package com.vladimirkush.geoaction;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.vladimirkush.geoaction.Utils.DBHelper;

public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = "LOGTAG";

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

        tvLabel = (TextView) findViewById(R.id.label_logged_in);
        mIsLoginPersistent = (boolean)getIntent().getExtras().get(Constants.LOGIN_IS_PERSISTENT_KEY);

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

            }
        });

    }
    public void logOutOnClick(View view) {
        Backendless.UserService.logout(new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void aVoid) {
                Toast.makeText(getApplicationContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                Toast.makeText(getApplicationContext(), "failed to log out", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* open Action Creation activity */
    public void newActionOnClick(View view) {
        Intent intent = new Intent(this,ActionCreate.class);
        startActivity(intent);
    }

    public void dbmanagerClick(View view) {

        Intent dbmanager = new Intent(this, AndroidDatabaseManager.class);
        startActivity(dbmanager);
    }
}
