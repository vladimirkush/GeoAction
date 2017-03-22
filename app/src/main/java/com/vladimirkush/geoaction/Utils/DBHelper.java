package com.vladimirkush.geoaction.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBAction.ActionType;
import com.vladimirkush.geoaction.Models.LBAction.DirectionTrigger;
import com.vladimirkush.geoaction.Models.LBAction.Status;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;

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
        public static final String ACTIONS_COLUMN_TO = "recipients";
        public static final String ACTIONS_COLUMN_MESSAGE = "message";
        public static final String ACTIONS_COLUMN_SUBJECT = "subject";  // for reminder used for title
    }

    // ----constants----
    public static final String LOG_TAG = "LOGTAG";
    public static final String DATABASE_NAME = "GeoActionsCache.db";

    private Context context;
    // ----prebuilt queries----
    // CREATE TABLE
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + ActionsEntry.ACTIONS_TABLE_NAME +
                                                            " (" +
                    ActionsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_RADIUS + " INTEGER, " +
                    ActionsEntry.ACTIONS_COLUMN_DIRECTION_TRIGGER + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_LAT + " REAL, " +
                    ActionsEntry.ACTIONS_COLUMN_LON + " REAL, " +
                    ActionsEntry.ACTIONS_COLUMN_STATUS + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_TO + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_MESSAGE + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_SUBJECT + " TEXT " +
                                                            ")";

    // DROP TABLE
    private static final String SQL_DELETE_ENTRIES =
            "DELETE FROM  " + ActionsEntry.ACTIONS_TABLE_NAME;



    // ----methods----
    //ctor
    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
        this.context=context;
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

    public void deleteDB(){
        context.deleteDatabase(DATABASE_NAME);
        Log.d(LOG_TAG, "DB: " + DATABASE_NAME + "deleted");
    }

    // INSERT new action to DB, return Pk _ID
    public long insertAction(LBAction lbAction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = getContentValuesForInsertUpdate(lbAction);

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

    public LBAction getAction(long id){
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {                         // colunmns to retrieve
                ActionsEntry._ID,
                ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE,
                ActionsEntry.ACTIONS_COLUMN_RADIUS,
                ActionsEntry.ACTIONS_COLUMN_DIRECTION_TRIGGER,
                ActionsEntry.ACTIONS_COLUMN_LAT,
                ActionsEntry.ACTIONS_COLUMN_LON,
                ActionsEntry.ACTIONS_COLUMN_STATUS,
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


        cursor.moveToFirst();
        LBAction lbAction = getActionFromCursorRow(cursor);
        if(lbAction==null){
            Log.e(LOG_TAG, "DB: error retrieving action with id: "+id);
        }
        cursor.close();
        return  lbAction;
    }

    // DELETE action by id
    public int deleteAction(long id){
        SQLiteDatabase db = this.getWritableDatabase();

        String selection = ActionsEntry._ID + " = ?";
        String[] selectionArgs = { id + "" };
        return db.delete(ActionsEntry.ACTIONS_TABLE_NAME, selection, selectionArgs);

    }

    // UPDATE one action of the same id as in incoming object, with all the values contained in it
    public int updateAction(LBAction lbAction){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = getContentValuesForInsertUpdate(lbAction);
        long id = lbAction.getID();

        String selection = ActionsEntry._ID + " = ?";
        String[] selectionArgs = { id + "" };

        return db.update(ActionsEntry.ACTIONS_TABLE_NAME,
                contentValues,
                selection,
                selectionArgs);


    }

    // GET all data from database as list
    public ArrayList<LBAction> getAllActions(){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<LBAction> actionsList = new ArrayList<LBAction>();
        Cursor  cursor = db.rawQuery("select * from " + ActionsEntry.ACTIONS_TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast() == false) {

                LBAction lbAction = getActionFromCursorRow(cursor);
                if(lbAction==null){
                    Log.e(LOG_TAG, "DB: error retrieving action in getAllActions");
                }

                actionsList.add(lbAction);
                cursor.moveToNext();
            }// while
        }

        return actionsList;
    }

    //return all actions as actions
    //TODO

    private ContentValues getContentValuesForInsertUpdate(LBAction lbAction){
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
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_TO, emailAct.getToAsSingleString());
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_MESSAGE, emailAct.getMessage());

        }else if (lbAction instanceof LBSms){
            LBSms smsAct = (LBSms) lbAction;
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_TO, smsAct.getToAsSingleString());
            contentValues.put(ActionsEntry.ACTIONS_COLUMN_MESSAGE, smsAct.getMessage());
        }else {
            Log.e(LOG_TAG, "DB: insertion error - lbAction subtype not compatible");
            return null;
        }

        return contentValues;
    }

    private LBAction getActionFromCursorRow (Cursor cursor){
        LBAction lbAction = null; // this will be reassigned based on the type and returned
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
                String toSms = cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_TO));
                String[] arrTo = TextUtils.split(toSms, ",");                            //split "to" by comma into array
                List<String> strings = new ArrayList<String>(Arrays.asList(arrTo));   // turn array into list
                actSms.setTo(strings);
                actSms.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_MESSAGE)));
                lbAction = actSms;
                break;

            case EMAIL:
                LBEmail actEmail = new LBEmail();
                String toEml = cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_TO));
                String[] arrEml = TextUtils.split(toEml, ",");                            //split "to" by comma into array
                List<String> stringsEml = new ArrayList<String>(Arrays.asList(arrEml));   // turn array into list
                actEmail.setTo(stringsEml);
                actEmail.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_MESSAGE)));
                actEmail.setSubject(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_SUBJECT)));
                lbAction = actEmail;
                break;
            default:
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

        return lbAction;
    }



    public ArrayList<Cursor> getData(String Query){
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[] { "mesage" };
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
        MatrixCursor Cursor2= new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);


        try{
            String maxQuery = Query ;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);


            //add value to cursor2
            Cursor2.addRow(new Object[] { "Success" });

            alc.set(1,Cursor2);
            if (null != c && c.getCount() > 0) {


                alc.set(0,c);
                c.moveToFirst();

                return alc ;
            }
            return alc;
        } catch(SQLException sqlEx){
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        } catch(Exception ex){

            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+ex.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        }


    }
}
