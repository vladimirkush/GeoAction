package com.vladimirkush.geoaction.Services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;

import com.backendless.Backendless;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.vladimirkush.geoaction.LoginActivity;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;
import com.vladimirkush.geoaction.R;
import com.vladimirkush.geoaction.Utils.DBHelper;
import com.vladimirkush.geoaction.Utils.GeofenceErrorMessages;

import java.util.List;

public class GeofenceTransitionsIntentService extends IntentService {
    private final String LOG_TAG = "LOGTAG";
    private DBHelper dbHelper;

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        dbHelper = new DBHelper(getApplicationContext());       // init dbHelper

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.d(LOG_TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            //Get the action(s) associated by the geofence(s) from db
            for(Geofence g : triggeringGeofences){
                long id = Long.valueOf(g.getRequestId());
                Log.d(LOG_TAG, "IS: id received:" + id);
                if(id >= 0) {                               // workaround for unregistered geofence triggering
                    LBAction lbAction = dbHelper.getAction(id);

                    // trigger action only if it has active status
                    if(lbAction.getStatus() == LBAction.Status.ACTIVE) {
                        lbAction.setStatus(LBAction.Status.PAUSED); // pause action to not trigger once more
                        dbHelper.updateAction(lbAction);
                        // handle reaction
                        switch (lbAction.getActionType()) {
                            case REMINDER:
                                handleReminderAction((LBReminder) lbAction);
                                break;
                            case SMS:
                                handleSMSAction((LBSms) lbAction);
                                break;
                            case EMAIL:
                                handleEmailAction((LBEmail) lbAction);
                                break;
                            default:
                                Log.d(LOG_TAG, "IS: lbAction received from DB has an illegal type");
                                break;
                        }
                    }else{
                        Log.d(LOG_TAG, "Detected paused action: "+lbAction.getID() + "of type "+lbAction.getActionType());
                    }
                }

            }

            Log.d(LOG_TAG, geofenceTransitionDetails);

        } else {
            // Log the error.
            Log.e(LOG_TAG, "invalid transition type");
        }


        dbHelper.close();
    }

    private void handleReminderAction(LBReminder rem){
        // Send notification and log the transition details.
        sendNotification(rem.getTitle(), rem.getMessage());

    }

    private void sendNotification(String title, String text) {
        long when = System.currentTimeMillis();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setVibrate(new long[]{1000, 1000, 1000})
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setAutoCancel(true);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, LoginActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(LoginActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        int mId = (int) when;
        mNotificationManager.notify(mId, mBuilder.build());
    }

    private String getGeofenceTransitionDetails(GeofenceTransitionsIntentService geofenceTransitionsIntentService, int geofenceTransition, List triggeringGeofences) {
        return "stub";
    }



    private void handleSMSAction(LBSms sms){
        // TODO rewrite in a nicer way with pending intents and number preediting
        SmsManager smsManager =  SmsManager.getDefault();
        for(String num : sms.getTo()) {
            smsManager.sendTextMessage(num, null, sms.getMessage(), null, null);
            Log.d(LOG_TAG, "Sent SMS to: "+ num +", text: "+ sms.getMessage());
        }
    }

    private void handleEmailAction(LBEmail email){
       Backendless.Messaging.sendHTMLEmail(email.getSubject(),email.getMessage(), email.getTo());
       List<String> recipients = email.getTo();
       for (String rec: recipients) {
           Log.d(LOG_TAG, "Email sent to:" + rec);
       }
    }

}
