package com.vladimirkush.geoaction.Utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.vladimirkush.geoaction.R;

public class SharedPreferencesHelper {
    static final String    LOG_TAG = "LOGTAG";




    public static void setIsFacebookLoggedIn(Context ctx, boolean flag){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(ctx.getString(R.string.prefs_fb_logged_in), flag);
        editor.apply();
    }

    public static boolean isFacebookLoggedIn(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(ctx.getString(R.string.prefs_fb_logged_in), false);
    }

    public static void setIsAlarmActive(Context ctx, boolean flag){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(ctx.getString(R.string.prefs_tracking_activated), flag);
        editor.apply();
    }

    public static boolean isAlarmActive(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(ctx.getString(R.string.prefs_tracking_activated), false);
    }

    public static void setIsAlarmPermitted(Context ctx, boolean flag){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(ctx.getString(R.string.prefs_tracking_permitted), flag);
        editor.apply();
    }

    public static boolean isAlarmPermitted(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(ctx.getString(R.string.prefs_tracking_permitted), false);
    }

    public static Uri getNotificationURI(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        String uriStr = sharedPref.getString(ctx.getString(R.string.prefs_ringtone_uri), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());

        return Uri.parse(uriStr);
    }

    public static boolean isVibratePermitted(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(ctx.getString(R.string.prefs_vibrate_permitted), false);
    }

    public static void clearAllPreferences(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear().apply();
    }

    public static long getAlertingTimeOutMillis(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
        String millisStr =  sharedPref.getString(ctx.getString(R.string.prefs_tracking_timeout), "60000");
        Log.d(LOG_TAG, millisStr + " millis timeout set" );
        return Long.parseLong(millisStr);
    }

}
