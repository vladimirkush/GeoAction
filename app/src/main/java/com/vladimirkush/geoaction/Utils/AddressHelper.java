package com.vladimirkush.geoaction.Utils;


import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddressHelper {
    public static final String LOG_TAG = "LOGTAG";

    /** Get textual address from latlon, using geocoder */
    public static String getAddress (Context context, LatLng location){
        List<Address> addresses = null;
        Geocoder geocoder = new Geocoder(context);
        try {
            addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            Log.e(LOG_TAG, "IO exeption:"+ioException.getMessage());
        } catch (IllegalArgumentException illegalArgumentException) {

            // Catch invalid latitude or longitude values.
            Log.e(LOG_TAG, "Illegal arg exeption:"+illegalArgumentException.getMessage());
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            Log.e(LOG_TAG, "No addresses returned");
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            String addressStr = TextUtils.join(", ",
                    addressFragments);
            Log.d(LOG_TAG, "ADDRESS :" + addressStr);
            return addressStr;

        }
        return "";
    }
}
