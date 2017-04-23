package com.vladimirkush.geoaction;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.local.UserIdStorageFactory;
import com.backendless.persistence.local.UserTokenStorageFactory;
import com.facebook.CallbackManager;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.Utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class LoginActivity extends AppCompatActivity {
    private final String LOG_TAG = "LOGTAG";

    private EditText mEmailEt;
    private EditText mPasswordEt;
    private EditText mReenterPasswordEt;
    private Button mLoginBtn;
    private Button mRegisterBtn;
    private ImageButton mFBLoginBtn;

    private boolean mLoginMode = true;
    private boolean mPersistantLogin = true;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        // init widgets
        mEmailEt = (EditText) findViewById(R.id.email_et);
        mPasswordEt = (EditText) findViewById(R.id.passw_et);
        mReenterPasswordEt = (EditText) findViewById(R.id.passw_reenter_et);
        mLoginBtn = (Button) findViewById(R.id.login_button);
        mRegisterBtn = (Button) findViewById(R.id.register_button);
        mFBLoginBtn = (ImageButton)findViewById(R.id.fb_login_btn);

        // init Backendless API
        String backendlessKey = getString(R.string.backendless_key);
        String backendlessAppId = getString(R.string.backendless_app_id);
        String version = "v1";
        Backendless.initApp( this, backendlessAppId, backendlessKey, version );

        // init FB callback manager
        callbackManager = CallbackManager.Factory.create();

        /*// test block - only for showing hash in logs
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.vladimirkush.geoaction",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d(LOG_TAG,"KeyHash: "+ Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }   // end test block*/

        // check if logged in using StayLoggedIn
        if(mPersistantLogin) {
            String userToken = UserTokenStorageFactory.instance().getStorage().get();
            if (userToken != null && !userToken.equals("")) {
                setUIEnabled(false);
                Backendless.UserService.isValidLogin(new AsyncCallback<Boolean>() {
                    @Override
                    public void handleResponse(Boolean aBoolean) {
                        String currentUserObjectId = UserIdStorageFactory.instance().getStorage().get();
                        Backendless.Data.of( BackendlessUser.class ).findById( currentUserObjectId, new AsyncCallback<BackendlessUser>(){

                            @Override
                            public void handleResponse(BackendlessUser backendlessUser) {
                                Log.d(LOG_TAG, "login validation success");
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.putExtra(Constants.LOGIN_IS_PERSISTENT_KEY, mPersistantLogin);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void handleFault(BackendlessFault backendlessFault) {
                                Toast.makeText(getApplicationContext(), "login validation failed", Toast.LENGTH_SHORT).show();
                                Log.d(LOG_TAG, "login validation failed: "+ backendlessFault.getMessage());
                                setUIEnabled(true);

                            }
                        } );

                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Log.d(LOG_TAG, "User not logged in");
                        setUIEnabled(true);
                    }
                });

            }
        }

    }

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult( requestCode, resultCode, data );
        callbackManager.onActivityResult( requestCode, resultCode, data );
    }

    public void registerOnClick(View view) {
        if (mLoginMode) {

            mLoginMode = false;
            setUI(false);
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
            mLoginMode = true;
            setUI(true);

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
        mFBLoginBtn.setEnabled(enabled);
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
                intent.putExtra(Constants.LOGIN_IS_PERSISTENT_KEY, mPersistantLogin);
                startActivity(intent);
                //setUIEnabled(true); // enable UI
                clearUI();
                finish();
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
        }, mPersistantLogin);   //// true for stay logged in
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

    public void facebookLoginOnClick(View view) {
        setUIEnabled(false); // disable UI
        ArrayList<String> fbPermissions = new ArrayList<String>();
        fbPermissions.add("email");
        fbPermissions.add("user_friends");

        Map<String, String> fbFieldMappings = new HashMap<String, String>();
        fbFieldMappings.put( "email", "fb_email" );

        Backendless.UserService.loginWithFacebookSdk(this, fbFieldMappings, fbPermissions, callbackManager, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser backendlessUser) {
                Log.d( LOG_TAG, backendlessUser.getEmail() + " successfully logged in with facebook" );

                // set the SP to know user is fb logged
                SharedPreferencesHelper.setIsFacebookLoggedIn(getApplicationContext(), true);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(Constants.LOGIN_IS_PERSISTENT_KEY, mPersistantLogin);
                startActivity(intent);
                clearUI();
                finish();
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                Log.d( LOG_TAG,"Error while logging in with facebook" );
                Log.d( LOG_TAG, backendlessFault.getMessage() );
                setUIEnabled(true);
            }
        }, mPersistantLogin);
    }

}
