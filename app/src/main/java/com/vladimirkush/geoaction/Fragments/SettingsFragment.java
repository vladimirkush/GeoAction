package com.vladimirkush.geoaction.Fragments;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;


import com.vladimirkush.geoaction.Models.Friend;
import com.vladimirkush.geoaction.R;
import com.vladimirkush.geoaction.LocalServices.TrackService;
import com.vladimirkush.geoaction.DataAccess.BackendlessHelper;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.DataAccess.DBHelper;
import com.vladimirkush.geoaction.DataAccess.SharedPreferencesHelper;

import java.util.ArrayList;


public class SettingsFragment extends PreferenceFragment {
    private final String    LOG_TAG = "LOGTAG";
    private SharedPreferences.OnSharedPreferenceChangeListener mListener;
    private Activity        mActivity;
    private AlarmManager    mAlarmMgr;
    private PendingIntent   mAlarmIntent;
    private DBHelper        mDbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mDbHelper = new DBHelper(mActivity);

        //configure alarm
        mAlarmMgr = (AlarmManager) mActivity.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(mActivity, TrackService.class);
        mAlarmIntent = PendingIntent.getService(mActivity, Constants.ALARM_MANAGER_REQUEST_CODE, intent, 0);

        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(getString(R.string.shared_preferences_file_key));

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // setting enabled only if user is logged with bacebook
        boolean fbConnected = SharedPreferencesHelper.isFacebookLoggedIn(mActivity);
        Preference trackingPref = getPreferenceScreen().findPreference(getString(R.string.prefs_tracking_permitted));

        trackingPref.setEnabled(fbConnected);
        if (fbConnected){
            trackingPref.setSummary(getString(R.string.prefstring_tracking_isfbuser));
        }else{
            trackingPref.setSummary(getString(R.string.prefstring_tracking_notfbuser));
        }

        // set summary for ringtone
        Preference ringtonePref = getPreferenceScreen().findPreference(getString(R.string.prefs_ringtone_uri));
        Uri uri = SharedPreferencesHelper.getNotificationURI(mActivity);
        Ringtone ringtone = RingtoneManager.getRingtone(mActivity, uri);
        String title = ringtone.getTitle(mActivity);
        // Set summary to be the user-description for the selected value
        ringtonePref.setSummary(title);

        mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                        if (key.equals(getString(R.string.prefs_ringtone_uri))) {
                            Preference ringtonePref = findPreference(key);

                            Uri uri = SharedPreferencesHelper.getNotificationURI(mActivity);

                            Ringtone ringtone = RingtoneManager.getRingtone(mActivity, uri);
                            String title = ringtone.getTitle(mActivity);
                            // Set summary to be the user-description for the selected value
                            ringtonePref.setSummary(title);
                        }else if(key.equals(getString(R.string.prefs_tracking_permitted))){
                            if(SharedPreferencesHelper.isAlarmPermitted(mActivity) &&
                                    !SharedPreferencesHelper.isAlarmActive(mActivity) &&
                                    SharedPreferencesHelper.isFacebookLoggedIn(mActivity)){
                                Log.d(LOG_TAG, "activating alarm for tracking service");

                                SharedPreferencesHelper.setIsAlarmActive(mActivity, true);
                                mAlarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                        SystemClock.elapsedRealtime() + 1000, 60 * 1000, mAlarmIntent); // fire each minute
                                BackendlessHelper.setMeTrackableAsync(true);
                            }else{
                                Log.d(LOG_TAG, "alarm disactivated");
                                mAlarmMgr.cancel(mAlarmIntent);
                                SharedPreferencesHelper.setIsAlarmActive(mActivity, false);
                                BackendlessHelper.setMeTrackableAsync(false);
                                setNotNearFriendsStatus();

                            }
                        }
                    }
                };
        manager.getSharedPreferences().registerOnSharedPreferenceChangeListener(mListener);
    }


    private void setNotNearFriendsStatus(){
        ArrayList<Friend> friends = mDbHelper.getAllFriends();
        for(Friend f:friends){
            f.setNear(false);
            mDbHelper.updateFriend(f);
        }


    }


    @Override
     public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(mListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(mListener);
    }
}
