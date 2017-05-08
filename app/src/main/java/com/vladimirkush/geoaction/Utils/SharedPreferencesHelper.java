package com.vladimirkush.geoaction.Utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import com.vladimirkush.geoaction.R;

import java.net.URI;

public class SharedPreferencesHelper {




    public static void setIsFacebookLoggedIn(Context ctx, boolean flag){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(ctx.getString(R.string.prefs_fb_logged_in), flag);
        editor.commit();
    }

    public static boolean isFacebookLoggedIn(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        if(!sharedPref.getBoolean(ctx.getString(R.string.prefs_fb_logged_in), false)){
            return false;
        }else{
            return true;
        }
    }

    public static void setIsAlarmActive(Context ctx, boolean flag){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(ctx.getString(R.string.prefs_tracking_activated), flag);
        editor.commit();
    }

    public static boolean isAlarmActive(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        if(!sharedPref.getBoolean(ctx.getString(R.string.prefs_tracking_activated), false)){
            return false;
        }else{
            return true;
        }
    }

    public static void setIsAlarmPermitted(Context ctx, boolean flag){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(ctx.getString(R.string.prefs_tracking_permitted), flag);
        editor.commit();
    }

    public static boolean isAlarmPermitted(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        if(!sharedPref.getBoolean(ctx.getString(R.string.prefs_tracking_permitted), false)){
            return false;
        }else{
            return true;
        }
    }

    public static Uri getNotificationURI(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        String uriStr = sharedPref.getString(ctx.getString(R.string.prefs_ringtone_uri), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());

        return Uri.parse(uriStr);
    }

    public static boolean isVibratePermitted(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        if(!sharedPref.getBoolean(ctx.getString(R.string.prefs_vibrate_permitted), false)){
            return false;
        }else{
            return true;
        }
    }

    public static void clearAllPreferences(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear().commit();
    }

}
