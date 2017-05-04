package com.vladimirkush.geoaction.Asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.vladimirkush.geoaction.R;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class DeleteBulkFromCloud extends AsyncTask<Void,Void,String> {
    private final String LOG_TAG = "LOGTAG";

    private String apiURL;
    private Context ctx;

    public DeleteBulkFromCloud(String apiURL, Context ctx) {
        this.apiURL = apiURL;
        this.ctx = ctx;
    }

    @Override
    protected String doInBackground(Void... params) {
        HttpURLConnection urlConnection = null;
        String backendlessKey = ctx.getString(R.string.backendless_REST_key);
        String backendlessAppId = ctx.getString(R.string.backendless_app_id);

        try {
            URL url = new URL(apiURL);
            Log.d(LOG_TAG,"URL is "+apiURL);

            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty( "application-id", backendlessAppId  );
            urlConnection.setRequestProperty( "secret-key",backendlessKey );
            urlConnection.setRequestProperty( "application-type", "REST" );
            urlConnection.setRequestMethod("DELETE");

            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            Log.d(LOG_TAG, "response code: " + responseCode);


        } catch (MalformedURLException e) {

            e.printStackTrace();
            Log.d(LOG_TAG, e.getMessage());
        } catch (IOException e) {

            e.printStackTrace();
            Log.d(LOG_TAG, e.getMessage());
        }finally {

            urlConnection.disconnect();
        }

        return null;
    }

}
