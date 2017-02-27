package com.vladimirkush.geoaction.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBAction.*;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* This class holds all interactions with inner sqlite database
 */

public class DBHelper extends SQLiteOpenHelper {

    // inner class - Contract for table columns
    public static class ActionsEntry implements BaseColumns {
        public static final String ACTIONS_TABLE_NAME = "actions";
        public static final String ACTIONS_COLUMN_ACTION_TYPE = "actionType";
        public static final String ACTIONS_COLUMN_RADIUS = "radius";
        public static final String ACTIONS_COLUMN_DIRECTION_TRIGGER = "directionTrigger";
        public static final String ACTIONS_COLUMN_LAT = "latitude";
        public static final String ACTIONS_COLUMN_LON = "longitude";
        public static final String ACTIONS_COLUMN_STATUS = "status";
        public static final String ACTIONS_COLUMN_TO = "to";
        public static final String ACTIONS_COLUMN_MESSAGE = "message";
        public static final String ACTIONS_COLUMN_SUBJECT = "subject";  // for reminder used for title
    }

    // ----constants----
    public static final String LOG_TAG = "LOGTAG";
    public static final String DATABASE_NAME = "GeoActionsCache.db";

    // ----prebuilt queries----
    // CREATE TABLE
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS" + ActionsEntry.ACTIONS_TABLE_NAME +
                                                            " (" +
                    ActionsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE + " TEXT," +
                    ActionsEntry.ACTIONS_COLUMN_RADIUS + " INTEGER," +
                    ActionsEntry.ACTIONS_COLUMN_DIRECTION_TRIGGER + " TEXT," +
                    ActionsEntry.ACTIONS_COLUMN_LAT + " REAL," +
                    ActionsEntry.ACTIONS_COLUMN_LON + " REAL," +
                    ActionsEntry.ACTIONS_COLUMN_STATUS + " TEXT," +
                    ActionsEntry.ACTIONS_COLUMN_TO + " TEXT," +
                    ActionsEntry.ACTIONS_COLUMN_MESSAGE + " TEXT," +
                    ActionsEntry.ACTIONS_COLUMN_SUBJECT + " TEXT," +
                                                            ")";

    // DROP TABLE
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ActionsEntry.ACTIONS_TABLE_NAME;



    // ----methods----
    //ctor
    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
        Log.d(LOG_TAG, "DB: DBHelper constructor invoked");
    }

    // INSERT new action to DB, return Pk _ID
    public long insertAction(LBAction lbAction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        // shared params
        contentValues.put(ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE, lbAction.getActionType().toString());
        contentValues.put(ActionsEntry.ACTIONS_COLUMN_RADIUS, lbAction.getRadius());
        contentValues.put(ActionsEntry.ACTIONS_COLUMN_DIRECTION_TRIGGER, lbAction.getDirectionTrigger().toString());
        contentValues.put(ActionsEntry.ACTIONS_COLUMN_LAT, lbAction.getTriggerCenter().latitude);
        contentValues.put(ActionsEntry.ACTIONS_COLUMN_LON, lbAction.getTriggerCenter().longitude);
        contentValues.put(ActionsEntry.ACTIONS_COLUMN_STATUS, lbAction.getStatus().toString());
        // type-specific params
        if (lbAction instanceof LBReminder){
            LBReminder remAct = (LBReminder) lbAction;
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_SUBJECT, remAct.getTitle());
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_MESSAGE, remAct.getMessage());
        }else if(lbAction instanceof LBEmail){
            LBEmail emailAct = (LBEmail) lbAction;
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_SUBJECT, emailAct.getSubject());
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_TO, emailAct.getTo().toString());
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_MESSAGE, emailAct.getMessage());

        }else if (lbAction instanceof LBSms){
            LBSms smsAct = (LBSms) lbAction;
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_TO, smsAct.getTo().toString());
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_MESSAGE, smsAct.getMessage());
        }else {
            Log.e(LOG_TAG, "DB: insertion error - lbAction subtype not compatible");
        }

        return db.insert(ActionsEntry.ACTIONS_TABLE_NAME, null, contentValues);

    }

    public void deleteAllActions(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES);
    }

    public void createTableForActions(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, ActionsEntry.ACTIONS_TABLE_NAME);
        return numRows;
    }

    public LBAction getAction(int id){
        SQLiteDatabase db = this.getReadableDatabase();


        String[] projection = {                         // colunmns to retrieve
                ActionsEntry._ID,
                ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE,
                ActionsEntry.ACTIONS_COLUMN_RADIUS,
                ActionsEntry.ACTIONS_COLUMN_DIRECTION_TRIGGER,
                ActionsEntry.ACTIONS_COLUMN_LAT,
                ActionsEntry.ACTIONS_COLUMN_LON,
                ActionsEntry.ACTIONS_COLUMN_TO,
                ActionsEntry.ACTIONS_COLUMN_MESSAGE,
                ActionsEntry.ACTIONS_COLUMN_SUBJECT,
        };

        String selection = ActionsEntry._ID + " = ?";
        String[] selectionArgs = { id + "" };
        String sortOrder =
                ActionsEntry._ID + " DESC";

        Cursor cursor = db.query(
                ActionsEntry.ACTIONS_TABLE_NAME,          // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        LBAction lbAction = null; // this will be reassigned based on the type and returned
        cursor.moveToFirst();

        //determine the type of an action
        ActionType actionType =  ActionType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE)));


        switch(actionType){
            case  REMINDER:
                LBReminder actRem = new LBReminder();
                actRem.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_SUBJECT)));
                actRem.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_MESSAGE)));
                lbAction = actRem;
                break;

            case SMS:
                LBSms actSms = new LBSms();
                String toSms = cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_SUBJECT));
                String[] arrTo = TextUtils.split(toSms, ",");                            //split "to" by comma into array
                List<String> strings = new ArrayList<String>(Arrays.asList(arrTo));   // turn array into list
                actSms.setTo(strings);
                actSms.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_MESSAGE)));
                lbAction = actSms;
                break;

            case EMAIL:
                LBEmail actEmail = new LBEmail();
                String toEml = cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_SUBJECT));
                String[] arrEml = TextUtils.split(toEml, ",");                            //split "to" by comma into array
                List<String> stringsEml = new ArrayList<String>(Arrays.asList(arrEml));   // turn array into list
                actEmail.setTo(stringsEml);
                actEmail.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_MESSAGE)));
                actEmail.setSubject(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_SUBJECT)));
                lbAction = actEmail;
                break;
            default:
                Log.e(LOG_TAG, "DB: error retreiving action with id: "+id);
                return null;
        }

        Status st = Status.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_STATUS)));
        DirectionTrigger dir = DirectionTrigger.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_DIRECTION_TRIGGER)));
        double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_LAT));
        double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_LON));
        LatLng ll = new LatLng(lat, lon);

        // assign shared values
        lbAction.setActionType(actionType);
        lbAction.setID(cursor.getLong(cursor.getColumnIndexOrThrow(ActionsEntry._ID)));
        lbAction.setRadius(cursor.getInt(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_RADIUS)));
        lbAction.setTriggerCenter(ll);
        lbAction.setDirectionTrigger(dir);
        lbAction.setStatus(st);

        cursor.close();
        return  lbAction;
    }




    //delete action by id
    //return all actions as actions
    //update action status
    //update action (multiple params)
    //TODO



    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        Log.d(LOG_TAG, "DB: " + DATABASE_NAME + "created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        Log.d(LOG_TAG, "DB: " + DATABASE_NAME + "upgraded");

    }
}
