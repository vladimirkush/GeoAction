package com.vladimirkush.geoaction;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.local.UserTokenStorageFactory;


public class LoginActivity extends AppCompatActivity {
    private final String LOG_TAG = "LOGTAG";

    private EditText mEmailEt;
    private EditText mPasswordEt;
    private EditText mReenterPasswordEt;
    private Button mLoginBtn;
    private Button mRegisterBtn;

    private boolean mLoginMode = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //TODO input checks

        // init widgets
        mEmailEt = (EditText) findViewById(R.id.email_et);
        mPasswordEt = (EditText) findViewById(R.id.passw_et);
        mReenterPasswordEt = (EditText) findViewById(R.id.passw_reenter_et);
        mLoginBtn = (Button) findViewById(R.id.login_button);
        mRegisterBtn = (Button) findViewById(R.id.register_button);


        // init Backendless API
        String backendlessKey = getString(R.string.backendless_key);
        String backendlessAppId = getString(R.string.backendless_app_id);
        String version = "v1";
        Backendless.initApp( this, backendlessAppId, backendlessKey, version );

        // check if logged in using StayLoggedIn
        String userToken = UserTokenStorageFactory.instance().getStorage().get();
        if( userToken != null && !userToken.equals( "" ) ) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

    }


    public void registerOnClick(View view) {
        if (mLoginMode) {

            mLoginMode = !mLoginMode;
            setUI(mLoginMode);
        }else{
            // check input and register
            if(checkInput()) {
                attemptRegister();
            }
        }

    }


    public void loginOnClick(View view) {
        if(mLoginMode){
            // check input and login
            if(checkInput()) {
                attemptLogin();
            }
        }else{  // "back to login" clicked
            mLoginMode = !mLoginMode;
            setUI(mLoginMode);

        }
    }

    private void setUI (boolean isLoginMode){
        if(isLoginMode){
            mLoginBtn.setVisibility(View.VISIBLE);
            mLoginBtn.setText(R.string.login_btn_text);
            mReenterPasswordEt.setVisibility(View.GONE);
        }else{
            mLoginBtn.setText(R.string.login_btn_text2);
            mReenterPasswordEt.setVisibility(View.VISIBLE);
        }
        clearUI();
    }

    private void setUIEnabled(boolean enabled){
        mEmailEt.setEnabled(enabled);
        mPasswordEt.setEnabled(enabled);
        mReenterPasswordEt.setEnabled(enabled);
        mLoginBtn.setEnabled(enabled);
        mRegisterBtn.setEnabled(enabled);
    }

    private boolean checkInput(){
        boolean inputValidated = true;

        String email = mEmailEt.getText().toString();
        String pass = mPasswordEt.getText().toString();
        String reenterPass = mReenterPasswordEt.getText().toString();

        if(email.isEmpty()) {
            mEmailEt.setError("Email cannot be empty");
            inputValidated = false;
        }else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmailEt.setError("Email not valid");
            inputValidated = false;
        }

        if(pass.isEmpty()){
            mPasswordEt.setError("Password cannot be empty");
            inputValidated = false;
        }

        if(!mLoginMode){    // user makes registration
            if(!pass.equals(reenterPass)){
                mReenterPasswordEt.setError("Passwords don't match");
                inputValidated = false;
            }
        }




        if(inputValidated) {    // clear all errors
            mEmailEt.setError(null);
            mPasswordEt.setError(null);
            mReenterPasswordEt.setError(null);
        }
        return inputValidated;
    }

    private void attemptLogin(){
        setUIEnabled(false); // disable UI
        String email = mEmailEt.getText().toString();
        String pass = mPasswordEt.getText().toString();

        Backendless.UserService.login( email, pass, new AsyncCallback<BackendlessUser>() {
            public void handleResponse( BackendlessUser user ) {
                // user has been logged in
                Log.d(LOG_TAG, user.getEmail() + " has logged in");
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                setUIEnabled(true); // enable UI
                clearUI();
            }

            public void handleFault( BackendlessFault fault ) {
                // login failed
                setUIEnabled(true); // enable UI
                if(fault.getCode().equals("3003")){
                    Toast.makeText(getApplicationContext(), "Wrong email or password", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), fault.getMessage(), Toast.LENGTH_LONG).show();
                }
                Log.d(LOG_TAG, "failed:" + fault.getCode() + ":\n"+fault.getMessage());
            }
        });   //// true for stay logged in
    }

    private void attemptRegister(){
        setUIEnabled(false); // disable UI
        BackendlessUser user = new BackendlessUser();
        user.setEmail( mEmailEt.getText().toString() );
        user.setPassword( mPasswordEt.getText().toString() );

        Backendless.UserService.register( user, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse( BackendlessUser backendlessUser ) {
                Log.d( LOG_TAG, backendlessUser.getEmail() + " successfully registered" );
                Toast.makeText(getApplicationContext(), "registration successfull", Toast.LENGTH_LONG).show();
                setUIEnabled(true); // enable UI
                clearUI();
                loginOnClick(mLoginBtn); // return to login state
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                if (backendlessFault.getCode().equals("3033")){
                    Toast.makeText(getApplicationContext(), "user already registered", Toast.LENGTH_LONG).show();
                    Log.e(LOG_TAG, backendlessFault.getMessage());

                }else{
                    Toast.makeText(getApplicationContext(), backendlessFault.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(LOG_TAG, backendlessFault.getMessage());
                }
                setUIEnabled(true); // enable UI
            }
        } );
    }

    // clear all text fields
    private void clearUI(){
        mEmailEt.setText("");
        mPasswordEt.setText("");
        mReenterPasswordEt.setText("");
    }

}
