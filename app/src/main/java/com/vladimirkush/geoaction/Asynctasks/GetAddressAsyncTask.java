package com.vladimirkush.geoaction.Asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.vladimirkush.geoaction.Utils.AddressHelper;


public class GetAddressAsyncTask extends AsyncTask {
    TextView mAddressLabel;
    String mAddress;
    @Override
    protected Object doInBackground(Object[] params) {

        Context ctx = (Context) params[0];
        mAddressLabel= (TextView) params[1];
        LatLng location = (LatLng) params[2];

        mAddress = AddressHelper.getAddress(ctx, location);

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        mAddressLabel.setText(mAddress);
    }
}
