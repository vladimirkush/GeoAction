package com.vladimirkush.geoaction;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.local.UserIdStorageFactory;
import com.backendless.persistence.local.UserTokenStorageFactory;
import com.vladimirkush.geoaction.Utils.Constants;

public class ChangePasswordActivity extends AppCompatActivity {
    private final String    LOG_TAG = "LOGTAG";

    private Toolbar         mToolbar;
    private ActionBar       mActionBar;
    private EditText        mPassOne;
    private EditText        mPassTwo;
    private Button          mConfirm;
    private BackendlessUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mToolbar= (Toolbar) findViewById(R.id.change_password_toolbar);
        mPassOne = (EditText) findViewById(R.id.chpass_pass_one);
        mPassTwo = (EditText) findViewById(R.id.chpass_pass_two);
        mConfirm = (Button) findViewById(R.id.chpass_confirm_button) ;
        mToolbar.setTitle("Change password");
        setSupportActionBar(mToolbar);


        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        setUIEnabled(false);
        validateUserLogin();
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private boolean checkInput(){
        boolean inputClear = true;
        String pass1 = mPassOne.getText().toString();
        String pass2 = mPassTwo.getText().toString();

        if (pass1.isEmpty()){
            mPassOne.setError("Password should not be empty");
            inputClear = false;
        }else if (pass2.isEmpty()){
            mPassTwo.setError("Password should not be empty");
            inputClear = false;
        }else if(!pass1.equals(pass2)){
            mPassTwo.setError("passwords are not same");
            inputClear = false;
        }
        return inputClear;
    }

    private void setUIEnabled(boolean  enabled){
        mPassOne.setEnabled(enabled);
        mPassTwo.setEnabled(enabled);
        mConfirm.setEnabled(enabled);
    }

    public void onConfirmClick(View view) {
        setUIEnabled(false);

        if (checkInput()){
            mUser.setPassword( mPassOne.getText().toString() );
            Backendless.Data.of( BackendlessUser.class ).save( mUser, new AsyncCallback<BackendlessUser>() {
                @Override
                public void handleResponse( BackendlessUser backendlessUser ) {
                    Toast.makeText(getApplicationContext(), "Passwoard was successfully changed", Toast.LENGTH_SHORT).show();
                    setUIEnabled(true);
                    Intent returnIntent = getIntent();
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }

                @Override
                public void handleFault( BackendlessFault backendlessFault ) {
                    Toast.makeText(getApplicationContext(),"Server reported an error - " + backendlessFault.getMessage(), Toast.LENGTH_LONG ).show();
                    setUIEnabled(true);
                }
            } );
        }else{
            setUIEnabled(true);
        }
    }

    private void validateUserLogin(){
        Backendless.UserService.isValidLogin(new AsyncCallback<Boolean>() {
            @Override
            public void handleResponse(Boolean aBoolean) {
                String currentUserObjectId = UserIdStorageFactory.instance().getStorage().get();
                Backendless.Data.of( BackendlessUser.class ).findById( currentUserObjectId, new AsyncCallback<BackendlessUser>(){

                    @Override
                    public void handleResponse(BackendlessUser backendlessUser) {
                        Log.d(LOG_TAG, "login validation success");
                        mUser = backendlessUser;
                        setUIEnabled(true);

                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Toast.makeText(getApplicationContext(), "login validation failed", Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "login validation failed: "+ backendlessFault.getMessage());
                        finish();
                    }
                } );
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                Toast.makeText(getApplicationContext(), "login validation failed", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }



    /**/
}
