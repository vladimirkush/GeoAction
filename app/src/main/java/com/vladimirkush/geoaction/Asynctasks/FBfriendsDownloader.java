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
import com.vladimirkush.geoaction.Utils.DBHelper;
import com.vladimirkush.geoaction.Utils.SharedPreferencesHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import static com.facebook.FacebookSdk.getApplicationContext;


public class FBfriendsDownloader extends AsyncTask<Void, Void, Void> {
    private final String LOG_TAG = "LOGTAG";
    DBHelper dbHelper;

    @Override
    protected Void doInBackground(Void... params) {
         dbHelper = new DBHelper(getApplicationContext());
        //test
        if(SharedPreferencesHelper.isFacebookLoggedIn(getApplicationContext())) {
            AccessToken token = AccessToken.getCurrentAccessToken();
            Log.d(LOG_TAG, "FB Access token: " + token.toString());
            //String fbid = Profile.getCurrentProfile().getId();
            Bundle bundle = new Bundle();
            bundle.putString("fields", "id,name, email,picture.type(large)");
            new GraphRequest(
                    token,
                    "/me/friends",
                    bundle,
                    HttpMethod.GET,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
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
                                    if(dbHelper.getFriendByFBId(friendId) == null) {    // do not download anything if the friend is already in DB
                                        Log.d(LOG_TAG, "fb freind name: " + userName + " id: " + friendId);
                                        if (user.has("picture")) {

                                            try {
                                                final URL profilePicUrl = new URL(user.getJSONObject("picture").getJSONObject("data").getString("url"));
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Bitmap profilePic = null;
                                                        try {
                                                            profilePic = BitmapFactory.decodeStream(profilePicUrl.openConnection().getInputStream());
                                                            friend.setUserIcon(profilePic);
                                                            dbHelper.insertFriend(friend);
                                                            Log.d(LOG_TAG, "friend inserted " + friend.getFbID());
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                        //mImageView.setBitmap(profilePic);
                                                        Log.d(LOG_TAG, "Userpic bytes downloaded: " + profilePic.getByteCount());
                                                    }
                                                }).start();

                                                Log.d(LOG_TAG, "Userpic url: " + profilePicUrl);

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                        }
                                    }


                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }catch (Exception e){
                                Toast.makeText(getApplicationContext(), "Facebook service unavailable", Toast.LENGTH_LONG).show();
                            }
                        }
                    }).executeAsync();


        }
        // -- test





        return null;
    }


}
