package com.vladimirkush.geoaction.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.vladimirkush.geoaction.Models.Friend;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBAction.ActionType;
import com.vladimirkush.geoaction.Models.LBAction.DirectionTrigger;
import com.vladimirkush.geoaction.Models.LBAction.Status;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* This class holds all interactions with inner sqlite database
 */

public class DBHelper extends SQLiteOpenHelper {

    // inner classes - Contract for table columns
    public static class ActionsEntry implements BaseColumns {
        public static final String ACTIONS_TABLE_NAME = "actions";
        public static final String ACTIONS_COLUMN_EXTERNAL_ID = "externalId";
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

    public static class FriendsEntry implements BaseColumns{
        public static final String FRIENDS_TABLE_NAME = "friends";
        public static final String FRIENDS_COLUMN_FBID = "fbid";
        public static final String FRIENDS_COLUMN_NAME = "name";
        public static final String FRIENDS_COLUMN_STATUS = "status";
        public static final String FRIENDS_COLUMN_USERICON = "usericon";
        public static final String FRIENDS_COLUMN_LAT = "lat";
        public static final String FRIENDS_COLUMN_LON = "lon";
        public static final String FRIENDS_COLUMN_ISNEAR = "isNear";
        public static final String FRIENDS_COLUMN_LAST_NEAR_TIME = "lastNearTimeMillis";

    }

    // ----constants----
    public static final String LOG_TAG = "LOGTAG";
    public static final String DATABASE_NAME = "GeoActionsCache.db";

    private Context context;
    // ----prebuilt queries----
    // CREATE TABLE for actions
    private static final String SQL_CREATE_TABLE_ACTIONS =
            "CREATE TABLE IF NOT EXISTS " + ActionsEntry.ACTIONS_TABLE_NAME +
                                                            " (" +
                    ActionsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_EXTERNAL_ID + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_RADIUS + " INTEGER, " +
                    ActionsEntry.ACTIONS_COLUMN_DIRECTION_TRIGGER + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_LAT + " REAL, " +
                    ActionsEntry.ACTIONS_COLUMN_LON + " REAL, " +
                    ActionsEntry.ACTIONS_COLUMN_STATUS + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_TO + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_MESSAGE + " TEXT, " +
                    ActionsEntry.ACTIONS_COLUMN_SUBJECT + " TEXT " +
                                                            ")";

    // CREATE TABLE for friends
    private static final String SQL_CREATE_TABLE_FRIENDS =
            "CREATE TABLE IF NOT EXISTS " + FriendsEntry.FRIENDS_TABLE_NAME +
                    " (" +
                    FriendsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FriendsEntry.FRIENDS_COLUMN_FBID + " TEXT, " +
                    FriendsEntry.FRIENDS_COLUMN_NAME + " TEXT, " +
                    FriendsEntry.FRIENDS_COLUMN_STATUS + " TEXT, " +
                    FriendsEntry.FRIENDS_COLUMN_USERICON + " BLOB, " +
                    FriendsEntry.FRIENDS_COLUMN_LAT + " REAL, " +
                    FriendsEntry.FRIENDS_COLUMN_LON + " REAL, " +
                    FriendsEntry.FRIENDS_COLUMN_ISNEAR + " INTEGER, " +
                    FriendsEntry.FRIENDS_COLUMN_LAST_NEAR_TIME + " INTEGER " +

                    ")";

    // DROP TABLE actions
    private static final String SQL_DELETE_ENTRIES_ACTIONS =
            "DELETE FROM  " + ActionsEntry.ACTIONS_TABLE_NAME;

    // DROP TABLE friends
    private static final String SQL_DELETE_ENTRIES_FRIENDS =
            "DELETE FROM  " + FriendsEntry.FRIENDS_TABLE_NAME;

    // ----methods----
    //ctor
    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
        this.context=context;
       // SQLiteDatabase db = this.getWritableDatabase();
        //db.execSQL(SQL_CREATE_TABLE_FRIENDS);// TODO delete?
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_ACTIONS);
        db.execSQL(SQL_CREATE_TABLE_FRIENDS);
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

    // INSERT new friend to DB, return Pk _ID
    public long insertFriend(Friend friend){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = getContentValuesForInsertUpdateFriend(friend);
        return db.insert(FriendsEntry.FRIENDS_TABLE_NAME, null, contentValues);

    }

    // UPDATE one friend of the same id as in incoming object, with all the values contained in it
    public int updateFriend(Friend friend){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = getContentValuesForInsertUpdateFriend(friend);
        long id = friend.getID();

        String selection = FriendsEntry._ID + " = ?";
        String[] selectionArgs = { id + "" };

        return db.update(FriendsEntry.FRIENDS_TABLE_NAME,
                contentValues,
                selection,
                selectionArgs);


    }



    public void deleteAllActions(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES_ACTIONS);
    }

    public void deleteAllFriends(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES_FRIENDS);
    }

    public void createTableForActions(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(SQL_CREATE_TABLE_ACTIONS);
    }


    public LBAction getAction(long id){
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {                         // colunmns to retrieve
                ActionsEntry._ID,
                ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE,
                ActionsEntry.ACTIONS_COLUMN_EXTERNAL_ID,
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

    public Friend getFriend (long id){
        SQLiteDatabase db = this.getReadableDatabase();


        String[] projection = {                         // colunmns to retrieve
                FriendsEntry._ID,
                FriendsEntry.FRIENDS_COLUMN_FBID,
                FriendsEntry.FRIENDS_COLUMN_NAME,
                FriendsEntry.FRIENDS_COLUMN_STATUS,
                FriendsEntry.FRIENDS_COLUMN_USERICON,
                FriendsEntry.FRIENDS_COLUMN_LAT,
                FriendsEntry.FRIENDS_COLUMN_LON,
                FriendsEntry.FRIENDS_COLUMN_ISNEAR,
                FriendsEntry.FRIENDS_COLUMN_LAST_NEAR_TIME
        };
        String selection = FriendsEntry._ID + " = ?";
        String[] selectionArgs = { id + "" };
        String sortOrder =
                FriendsEntry._ID + " DESC";

        Cursor cursor = db.query(
                FriendsEntry.FRIENDS_TABLE_NAME,          // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        cursor.moveToFirst();
        Friend friend = getFriendFromCursorRow(cursor);
        if(friend==null){
            Log.e(LOG_TAG, "DB: error retrieving friend with id: "+id);
        }
        cursor.close();


        return friend;
    }

    public Friend getFriendByFBId (String fbId){
        SQLiteDatabase db = this.getReadableDatabase();


        String[] projection = {                         // colunmns to retrieve
                FriendsEntry._ID,
                FriendsEntry.FRIENDS_COLUMN_FBID,
                FriendsEntry.FRIENDS_COLUMN_NAME,
                FriendsEntry.FRIENDS_COLUMN_STATUS,
                FriendsEntry.FRIENDS_COLUMN_USERICON,
                FriendsEntry.FRIENDS_COLUMN_LAT,
                FriendsEntry.FRIENDS_COLUMN_LON,
                FriendsEntry.FRIENDS_COLUMN_ISNEAR,
                FriendsEntry.FRIENDS_COLUMN_LAST_NEAR_TIME

        };
        String selection = FriendsEntry.FRIENDS_COLUMN_FBID + " = ?";
        String[] selectionArgs = { fbId + "" };
        String sortOrder =
                FriendsEntry.FRIENDS_COLUMN_FBID + " DESC";

        Cursor cursor = db.query(
                FriendsEntry.FRIENDS_TABLE_NAME,          // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        if(cursor.moveToFirst()) {  // if we found any
            Friend friend = getFriendFromCursorRow(cursor);
            if (friend == null) {
                Log.e(LOG_TAG, "DB: error retrieving friend with fbid: " + fbId);
            }
            cursor.close();
            return friend;
        }else{
            return null;
        }



    }



    private Friend getFriendFromCursorRow(Cursor cursor){
        Friend friend = new Friend();
        try {
            friend.setID(cursor.getLong(cursor.getColumnIndexOrThrow(FriendsEntry._ID)));
            friend.setFbID(cursor.getString(cursor.getColumnIndexOrThrow(FriendsEntry.FRIENDS_COLUMN_FBID)));
            friend.setName(cursor.getString(cursor.getColumnIndexOrThrow(FriendsEntry.FRIENDS_COLUMN_NAME)));
            Friend.Status status = Friend.Status.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(FriendsEntry.FRIENDS_COLUMN_STATUS)));
            friend.setStatus(status);
            byte[] img = cursor.getBlob(cursor.getColumnIndexOrThrow(FriendsEntry.FRIENDS_COLUMN_USERICON));
            friend.setUserIcon(getBitmapImageFromBytes(img));
            friend.setLat(cursor.getDouble(cursor.getColumnIndexOrThrow(FriendsEntry.FRIENDS_COLUMN_LAT)));
            friend.setLon(cursor.getDouble(cursor.getColumnIndexOrThrow(FriendsEntry.FRIENDS_COLUMN_LON)));
            friend.setNear(cursor.getInt(cursor.getColumnIndexOrThrow(FriendsEntry.FRIENDS_COLUMN_ISNEAR)) != 0);
            friend.setLastNearTimeMillis(cursor.getLong(cursor.getColumnIndexOrThrow(FriendsEntry.FRIENDS_COLUMN_LAST_NEAR_TIME)));
        }catch ( Exception e){
            e.printStackTrace();
            return null;
        }
        return friend;
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

    // GET all data from "actions" table as list
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

    // GET all data from "friends" table as list
    public ArrayList<Friend> getAllFriends(){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Friend> friendList = new ArrayList<Friend>();
        Cursor  cursor = db.rawQuery("select * from " + FriendsEntry.FRIENDS_TABLE_NAME +
                                        " ORDER BY "+FriendsEntry.FRIENDS_COLUMN_ISNEAR + " DESC, " +
                                        FriendsEntry._ID + " DESC"
                                        , null);

        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast() == false) {

                Friend friend = getFriendFromCursorRow(cursor);
                if(friend==null){
                    Log.e(LOG_TAG, "DB: error retrieving friend in getAllFriends");
                }

                friendList.add(friend);
                cursor.moveToNext();
            }// while
        }

        return friendList;
    }

    public ArrayList<String> getAllTrackedFriiendsFBIDs(){
        ArrayList<String> ids = new ArrayList<String>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {                         // colunmns to retrieve
                FriendsEntry.FRIENDS_COLUMN_FBID,
                FriendsEntry.FRIENDS_COLUMN_STATUS,
        };
        String selection = FriendsEntry.FRIENDS_COLUMN_STATUS + " = ?";
        String[] selectionArgs = { Friend.Status.TRACED.toString() };
        //String sortOrder =
         //       FriendsEntry.FRIENDS_COLUMN_FBID + " DESC";

        Cursor cursor = db.query(
                FriendsEntry.FRIENDS_TABLE_NAME,          // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );


        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast() == false) {

                String fbid = cursor.getString(cursor.getColumnIndexOrThrow(FriendsEntry.FRIENDS_COLUMN_FBID));
                if (fbid == null) {
                    Log.e(LOG_TAG, "DB: error retrieving tracked friend  fbid: " + fbid);
                }

                ids.add(fbid);
                cursor.moveToNext();
            }// while
        }



        return ids;
    }



    private ContentValues getContentValuesForInsertUpdate(LBAction lbAction){
        ContentValues contentValues = new ContentValues();

        // shared params
        contentValues.put(ActionsEntry.ACTIONS_COLUMN_ACTION_TYPE, lbAction.getActionType().toString());
        contentValues.put(ActionsEntry.ACTIONS_COLUMN_EXTERNAL_ID, lbAction.getExternalID());
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

    private ContentValues getContentValuesForInsertUpdateFriend(Friend friend){
        ContentValues contentValues = new ContentValues();
        contentValues.put(FriendsEntry.FRIENDS_COLUMN_FBID, friend.getFbID());
        contentValues.put(FriendsEntry.FRIENDS_COLUMN_NAME, friend.getName());
        contentValues.put(FriendsEntry.FRIENDS_COLUMN_STATUS, friend.getStatus().toString());
        contentValues.put(FriendsEntry.FRIENDS_COLUMN_LAT, friend.getLat());
        contentValues.put(FriendsEntry.FRIENDS_COLUMN_LON, friend.getLon());
        contentValues.put(FriendsEntry.FRIENDS_COLUMN_ISNEAR, friend.isNear()? 1 : 0);
        contentValues.put(FriendsEntry.FRIENDS_COLUMN_LAST_NEAR_TIME, friend.getLastNearTimeMillis());

        byte[] imgBytes = getBytesFromBitmap(friend.getUserIcon());
        contentValues.put(FriendsEntry.FRIENDS_COLUMN_USERICON, imgBytes);
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
        lbAction.setExternalID(cursor.getString(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_EXTERNAL_ID)));
        lbAction.setID(cursor.getLong(cursor.getColumnIndexOrThrow(ActionsEntry._ID)));
        lbAction.setRadius(cursor.getInt(cursor.getColumnIndexOrThrow(ActionsEntry.ACTIONS_COLUMN_RADIUS)));
        lbAction.setTriggerCenter(ll);
        lbAction.setDirectionTrigger(dir);
        lbAction.setStatus(st);

        return lbAction;
    }

    // convert from bitmap to byte array
    private byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    // convert from byte array to bitmap
    private Bitmap getBitmapImageFromBytes(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
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
