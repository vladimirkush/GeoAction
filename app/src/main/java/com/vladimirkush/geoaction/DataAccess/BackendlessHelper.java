package com.vladimirkush.geoaction.DataAccess;

import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.local.UserIdStorageFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Holds several converter methods for manipulating Backendless data objects
 */

public class BackendlessHelper {
    public static final String LOG_TAG = "LOGTAG";

    public static final String ACTIONS_TABLE_NAME = "Actions";

    public static final String ACTIONS_OBJECT_ID = "objectId";
    private static final String ACTIONS_lOCAL_ID = "localId";
    private static final String ACTIONS_COLUMN_ACTION_TYPE = "actionType";
    private static final String ACTIONS_COLUMN_RADIUS = "radius";
    private static final String ACTIONS_COLUMN_DIRECTION_TRIGGER = "directionTrigger";
    private static final String ACTIONS_COLUMN_LAT = "latitude";
    private static final String ACTIONS_COLUMN_LON = "longitude";
    private static final String ACTIONS_COLUMN_STATUS = "status";
    private static final String ACTIONS_COLUMN_TO = "recipients";
    private static final String ACTIONS_COLUMN_MESSAGE = "message";
    private static final String ACTIONS_COLUMN_SUBJECT = "subject";  // for reminder used for title


    public static Map getMapForSingleAction(LBAction action){
        HashMap map = new HashMap();

        map.put(ACTIONS_COLUMN_ACTION_TYPE, action.getActionType().toString());
        map.put(ACTIONS_lOCAL_ID, action.getID());
        map.put(ACTIONS_OBJECT_ID, action.getExternalID());
        map.put(ACTIONS_COLUMN_RADIUS, action.getRadius());
        map.put(ACTIONS_COLUMN_DIRECTION_TRIGGER, action.getDirectionTrigger().toString());
        map.put(ACTIONS_COLUMN_LAT, action.getTriggerCenter().latitude);
        map.put(ACTIONS_COLUMN_LON, action.getTriggerCenter().longitude);
        map.put(ACTIONS_COLUMN_STATUS, action.getStatus().toString());
        // type-specific params
        if (action instanceof LBReminder){
            LBReminder remAct = (LBReminder) action;
            map.put(ACTIONS_COLUMN_SUBJECT, remAct.getTitle());
            map.put(ACTIONS_COLUMN_MESSAGE, remAct.getMessage());
        }else if(action instanceof LBEmail){
            LBEmail emailAct = (LBEmail) action;
            map.put(ACTIONS_COLUMN_SUBJECT, emailAct.getSubject());
            map.put(ACTIONS_COLUMN_TO, emailAct.getToAsSingleString());
            map.put(ACTIONS_COLUMN_MESSAGE, emailAct.getMessage());

        }else if (action instanceof LBSms){
            LBSms smsAct = (LBSms) action;
            map.put(ACTIONS_COLUMN_TO, smsAct.getToAsSingleString());
            map.put(ACTIONS_COLUMN_MESSAGE, smsAct.getMessage());
        }else {
            Log.e(LOG_TAG, "DB: insertion error - action subtype not compatible");
            return null;
        }
        return map;
    }


    public static LBAction getActionFromMapping(Map map){
        LBAction lbAction = null;
        ActionType actionType =  ActionType.valueOf((String)map.get(ACTIONS_COLUMN_ACTION_TYPE));


        switch(actionType){
            case  REMINDER:
                LBReminder actRem = new LBReminder();
                actRem.setTitle((String)map.get(ACTIONS_COLUMN_SUBJECT));
                actRem.setMessage((String)map.get(ACTIONS_COLUMN_MESSAGE));
                lbAction = actRem;
                break;

            case SMS:
                LBSms actSms = new LBSms();
                String toSms = (String)map.get(ACTIONS_COLUMN_TO);
                String[] arrTo = TextUtils.split(toSms, ",");                            //split "to" by comma into array
                List<String> strings = new ArrayList<String>(Arrays.asList(arrTo));   // turn array into list
                actSms.setTo(strings);
                actSms.setMessage((String)map.get(ACTIONS_COLUMN_MESSAGE));
                lbAction = actSms;
                break;

            case EMAIL:
                LBEmail actEmail = new LBEmail();
                String toEml = (String)map.get(ACTIONS_COLUMN_TO);
                String[] arrEml = TextUtils.split(toEml, ",");                            //split "to" by comma into array
                List<String> stringsEml = new ArrayList<String>(Arrays.asList(arrEml));   // turn array into list
                actEmail.setTo(stringsEml);
                actEmail.setMessage((String)map.get(ACTIONS_COLUMN_MESSAGE));
                actEmail.setSubject((String)map.get(ACTIONS_COLUMN_SUBJECT));
                lbAction = actEmail;
                break;
            default:
                return null;
        }

        Status st = LBAction.Status.valueOf((String)map.get(ACTIONS_COLUMN_STATUS));
        DirectionTrigger dir = DirectionTrigger.valueOf((String)map.get(ACTIONS_COLUMN_DIRECTION_TRIGGER));
        double lat = (double)map.get(ACTIONS_COLUMN_LAT);
        double lon = (double)map.get(ACTIONS_COLUMN_LON);
        LatLng ll = new LatLng(lat, lon);

        // assign shared values
        lbAction.setActionType(actionType);
        lbAction.setExternalID((String)map.get(ACTIONS_OBJECT_ID));
        //lbAction.setID((long)map.get(ACTIONS_lOCAL_ID));
        lbAction.setRadius((int)map.get(ACTIONS_COLUMN_RADIUS));
        lbAction.setTriggerCenter(ll);
        lbAction.setDirectionTrigger(dir);
        lbAction.setStatus(st);

        return lbAction;

    }

    public static void setMeTrackableAsync(final boolean trackable){
            String currentUserObjectId = UserIdStorageFactory.instance().getStorage().get();
            if(currentUserObjectId != null && !currentUserObjectId.isEmpty()) {
                Backendless.Data.of(BackendlessUser.class).findById(currentUserObjectId, new AsyncCallback<BackendlessUser>() {
                    @Override
                    public void handleResponse(BackendlessUser backendlessUser) {

                        Log.d(LOG_TAG, "Current fb user: " + backendlessUser.getEmail());
                        backendlessUser.setProperty("trackable", trackable);
                        Backendless.UserService.update(backendlessUser, new AsyncCallback<BackendlessUser>() {
                            @Override
                            public void handleResponse(BackendlessUser backendlessUser) {
                                Log.d(LOG_TAG, "Fb trackable updated in cloud: " + backendlessUser.getEmail());

                            }

                            @Override
                            public void handleFault(BackendlessFault backendlessFault) {
                                Log.d(LOG_TAG, "Fb trackable update in cloud failed: " + backendlessFault.getMessage());

                            }
                        });
                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                    }
                });
            }else{
                Log.d(LOG_TAG, "Backendless user is not logged in");

            }

    }


    public static void setMeTrackableSync(final boolean trackable){
        String currentUserObjectId = UserIdStorageFactory.instance().getStorage().get();
        if(currentUserObjectId != null && !currentUserObjectId.isEmpty()) {
            try {
                BackendlessUser backendlessUser = Backendless.Data.of(BackendlessUser.class).findById(currentUserObjectId);
                Log.d(LOG_TAG, "Current fb user: " + backendlessUser.getEmail());
                backendlessUser.setProperty("trackable", trackable);
                Backendless.UserService.update(backendlessUser);
            }catch (BackendlessException e){
                Log.d(LOG_TAG, "Exception during sync FBuser update:" + e.getDetail() );
                e.printStackTrace();
            }
        }
    }


    public static void updateMyLocationInCloudAsync(final Location loaction){
        String userId = UserIdStorageFactory.instance().getStorage().get();
        Backendless.UserService.findById(userId, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser backendlessUser) {
                Log.d(LOG_TAG, "Current fb user: "+ backendlessUser.getEmail() );
                backendlessUser.setProperty("latitude", loaction.getLatitude());
                backendlessUser.setProperty("longitude", loaction.getLongitude());
                Backendless.UserService.update(backendlessUser, new AsyncCallback<BackendlessUser>() {
                    @Override
                    public void handleResponse(BackendlessUser backendlessUser) {
                        Log.d(LOG_TAG, "Fb user updated in cloud: "+ backendlessUser.getEmail() );

                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        Log.d(LOG_TAG, "Fb user update in cloud failed: "+ backendlessFault.getMessage() );
                    }
                });
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                Log.d(LOG_TAG, "Error retreiving user for location update: "+ backendlessFault.getMessage() );
            }
        });


    }
}
