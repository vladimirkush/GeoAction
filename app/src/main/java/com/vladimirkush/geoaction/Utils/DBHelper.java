package com.vladimirkush.geoaction.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;

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
        public static final String ACTIONS_COLUMN_TO = "to";
        public static final String ACTIONS_COLUMN_MESSAGE = "message";
        public static final String ACTIONS_COLUMN_SUBJECT = "subject";  // for reminder used for title
    }

    // constants
    public static final String LOG_TAG = "LOGTAG";
    public static final String DATABASE_NAME = "GeoActionsCache.db";

    // prebuilt queries

    // CREATE TABLE
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ActionsEntry.ACTIONS_TABLE_NAME +
                                                            " (" +
                    ActionsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE + " TEXT," +
                    ActionsEntry.ACTIONS_COLUMN_RADIUS + " INTEGER," +
                    ActionsEntry.ACTIONS_COLUMN_DIRECTION_TRIGGER + " TEXT," +
                    ActionsEntry.ACTIONS_COLUMN_LAT + " REAL," +
                    ActionsEntry.ACTIONS_COLUMN_LON + " REAL," +
                    ActionsEntry.ACTIONS_COLUMN_TO + " TEXT," +
                    ActionsEntry.ACTIONS_COLUMN_MESSAGE + " TEXT," +
                    ActionsEntry.ACTIONS_COLUMN_SUBJECT + " TEXT," +
                                                            ")";

    // DROP TABLE
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ActionsEntry.ACTIONS_TABLE_NAME;




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
