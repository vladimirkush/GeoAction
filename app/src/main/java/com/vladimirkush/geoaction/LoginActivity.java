package com.vladimirkush.geoaction;

import android.content.res.Resources;
import android.opengl.Visibility;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.async.callback.BackendlessCallback;
import com.backendless.exceptions.BackendlessFault;

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


        // test user registration
        BackendlessUser user = new BackendlessUser();
        user.setEmail( "a@a.aa" );
        user.setPassword( "aaa" );

        Backendless.UserService.register( user, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse( BackendlessUser backendlessUser ) {
                Log.d( LOG_TAG, backendlessUser.getEmail() + " successfully registered" );
                Toast.makeText(getApplicationContext(), "registration successfull", Toast.LENGTH_LONG).show();
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

            }
        } );


    }


    public void registerOnClick(View view) {
        if (mLoginMode) {
            mLoginMode = !mLoginMode;
            setUI(mLoginMode);
        }else{
            // TODO attempt register

        }

    }


    public void loginOnClick(View view) {
        if(mLoginMode){
            // TODO attempt login
        }else{
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
    }

}
