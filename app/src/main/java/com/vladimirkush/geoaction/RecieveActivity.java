package com.vladimirkush.geoaction;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class RecieveActivity extends AppCompatActivity {
    private final String LOG_TAG = "LOGTAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recieve);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();
        String msg = "Received id: " + data.getQueryParameter("id");
        Log.d(LOG_TAG,msg );
        TextView tv = (TextView) findViewById(R.id.receive_test);
        tv.setText(msg);
    }
}
