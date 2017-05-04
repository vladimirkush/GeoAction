package com.vladimirkush.geoaction.Utils;


import android.content.Context;
import android.content.SharedPreferences;

import com.vladimirkush.geoaction.R;

public class SharedPreferencesHelper {
    public static final String PREFS_FBLOGGED_IN = "fb_logged_in";
    public static final String PREFS_ALARM_ACTIVATED_FLAG_KEY = "alarm_activated";
    public static final String PREFS_ALARM_PERMITTED_FLAG_KEY = "alarm_permitted";



    public static void setIsFacebookLoggedIn(Context ctx, boolean flag){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREFS_FBLOGGED_IN, flag);
        editor.commit();
    }

    public static boolean isFacebookLoggedIn(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        if(!sharedPref.getBoolean(PREFS_FBLOGGED_IN, false)){
            return false;
        }else{
            return true;
        }
    }

    public static void setIsAlarmActive(Context ctx, boolean flag){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREFS_ALARM_ACTIVATED_FLAG_KEY, flag);
        editor.commit();
    }

    public static boolean isAlarmActive(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        if(!sharedPref.getBoolean(PREFS_ALARM_ACTIVATED_FLAG_KEY, false)){
            return false;
        }else{
            return true;
        }
    }

    public static void setIsAlarmPermitted(Context ctx, boolean flag){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREFS_ALARM_PERMITTED_FLAG_KEY, flag);
        editor.commit();
    }

    public static boolean isAlarmPermitted(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.shared_preferences_file_key), ctx.MODE_PRIVATE);
        if(!sharedPref.getBoolean(PREFS_ALARM_PERMITTED_FLAG_KEY, false)){
            return false;
        }else{
            return true;
        }
    }

}
