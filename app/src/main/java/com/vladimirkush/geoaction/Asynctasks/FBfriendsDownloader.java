package com.vladimirkush.geoaction.Asynctasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.vladimirkush.geoaction.Models.Friend;
import com.vladimirkush.geoaction.DataAccess.DBHelper;
import com.vladimirkush.geoaction.DataAccess.SharedPreferencesHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import static com.facebook.FacebookSdk.getApplicationContext;


public class FBfriendsDownloader extends AsyncTask<Void, Void, Void> {
    private final String    LOG_TAG = "LOGTAG";
    private DBHelper        dbHelper;

    @Override
    protected Void doInBackground(Void... params) {
        dbHelper = new DBHelper(getApplicationContext());
        if (SharedPreferencesHelper.isFacebookLoggedIn(getApplicationContext())) {
            AccessToken token = AccessToken.getCurrentAccessToken();
            Log.d(LOG_TAG, "FB Access token: " + token.toString());
            Bundle bundle = new Bundle();
            bundle.putString("fields", "id,name, email,picture.type(large)");
            new GraphRequest(
                    token,
                    "/me/friends",
                    bundle,
                    HttpMethod.GET,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                            handleResponse(response);
                        }
                    }).executeAsync();


        }

        return null;
    }

    /** handles responce from facebook*/
    private void handleResponse(GraphResponse response){
        Log.d(LOG_TAG, "fb graph response: \n" + response.toString());
        JSONObject object = response.getJSONObject();
        try {
            JSONArray arrayOfUsersInFriendList = object.getJSONArray("data");
            int numOfFriends = arrayOfUsersInFriendList.length();
            Log.d(LOG_TAG, "received friends number: " + numOfFriends);
            for (int i = 0; i < numOfFriends; i++) {
                final Friend friend = new Friend();
                JSONObject user = arrayOfUsersInFriendList.getJSONObject(i);
                String userName = user.getString("name");
                final String friendId = user.getString("id");
                friend.setFbID(friendId);
                friend.setName(userName);
                if (dbHelper.getFriendByFBId(friendId) == null) {    // do not download anything if the friend is already in DB
                    Log.d(LOG_TAG, "fb freind name: " + userName + " id: " + friendId);
                    if (user.has("picture")) {
                        URL profilePicUrl = new URL(user.getJSONObject("picture").getJSONObject("data").getString("url"));
                        insertFriendAsync(friend, profilePicUrl);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Facebook service unavailable", Toast.LENGTH_LONG).show();
        }
    }

    /** Constructs the friend, downloads profile picture and inserts into database*/
    private void insertFriendAsync(final Friend friend, final URL pictureUrl) {
        Log.d(LOG_TAG, "Userpic url: " + pictureUrl);
        new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap profilePic = null;
                    try {
                        profilePic = BitmapFactory.decodeStream(pictureUrl.openConnection().getInputStream());
                        friend.setUserIcon(profilePic);
                        dbHelper.insertFriend(friend);
                        Log.d(LOG_TAG, "friend inserted " + friend.getFbID());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(LOG_TAG, "Userpic bytes downloaded: " + profilePic.getByteCount());
                }
            }).start();


    }



}
