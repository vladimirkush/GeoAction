package com.vladimirkush.geoaction;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

public class ActionCreate extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_create);
    }

    public void onRadioButtonClick(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_reminder:
                if (checked)
                    // reminder

                    break;
            case R.id.radio_sms:
                if (checked)
                    // SMS

                break;
            case R.id.radio_email:
                if (checked)
                    // email

                break;
        }
    }

    public void onLocationChooserClick(View view) {
        Toast.makeText(this, "clicked chose map", Toast.LENGTH_SHORT).show();
    }
}
