
package com.jyoti.janacare;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.jyoti.janacare.models.TodayStepSummaryRecord;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *  IntentService for handling incoming intents that are generated as a result of requesting
 *  activity updates using
 *  {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates}.
 */
public class DetectedActivitiesIntentService extends IntentService {

    private static long  startTime = 0;
    private static long endTime = 0;
    private static int stepsToNotify = 0;
    private int totalSteps = 0;
    static  SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
    protected static final String TAG = "detection-intent";
    static Calendar cal = Calendar.getInstance();
    //static Date now = new Date();
    private static int timerToNotify = 10;
    private static long inactiveTimer = 200;
    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent service = new Intent(this, GoogleFitService.class);
        service.putExtra(Constants.SERVICE_REQUEST_TYPE, Constants.TYPE_REQUEST_CONNECTION);
        startService(service);
    }

    /**
     * Handles incoming intents.
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        int walking = intent.getIntExtra("walking", 0);
        if(walking == 1 && startTime == 0){
            cal.setTime(new Date());
            startTime = cal.getTimeInMillis();
        }
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        if(result != null) {


            DetectedActivity detectedActivities = result.getMostProbableActivity();

            if (detectedActivities.getType() == DetectedActivity.STILL) {
                //Toast.makeText(this, "u are STILL", Toast.LENGTH_LONG).show();
                Log.d(TAG, "u are STILL");
            /* user is not moving
            * initialize endtime
            * calcutate steps betwen starttime and endtime
            * store it into db
            * intialize a to_notify_steps
            */

                cal.setTime(new Date());
                endTime = cal.getTimeInMillis();
                if (startTime != 0) {

                    totalSteps = GoogleFitService.getStepCountBetweenInterval(startTime, endTime);
                    Log.d("detected", startTime + " " + endTime);
                    if (totalSteps > 0) {
                        TodayStepSummaryRecord rec = new TodayStepSummaryRecord(totalSteps, dateFormat.format(startTime), dateFormat.format(endTime), 0);
                        boolean status = DiskCacheService.addDailyEntry(rec);
                        if (status) {
                            Log.d(TAG, totalSteps + " added to database");
                            //Toast.makeText(this, "added to database", Toast.LENGTH_LONG).show();
                        }
                        List<TodayStepSummaryRecord> list = DiskCacheService.getDailyEntries();
                        for (TodayStepSummaryRecord t : list) {
                            Log.d("records", t.toString());
                        }

                    }
                    stepsToNotify += totalSteps;
                    startTime = 0;
                }

                timerToNotify -= 1;
                if (timerToNotify == 0 && stepsToNotify > 0) {
                    //todo notify user
                    //todo u just waled stepstonotify steps;
                    //Toast.makeText(this, "u just walked "+stepsToNotify+" steps", Toast.LENGTH_LONG).show();
                    notify("you walked " + stepsToNotify + " steps");
                    Log.d(TAG, "u just walked " + stepsToNotify + " steps");
                    stepsToNotify = 0;
                    timerToNotify = 10;
                }
                inactiveTimer--;
                if (inactiveTimer == 0) {
                    //todo notify user u are inactive from a long period of time
                    notify("you are inactive from long time. \n ");
                    //Toast.makeText(this, "u are inactive from a long period of time", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "u are inactive from a long period of time");
                }

            } else if (detectedActivities.getType() == DetectedActivity.RUNNING ||
                    detectedActivities.getType() == DetectedActivity.WALKING ||
                    detectedActivities.getType() == DetectedActivity.ON_FOOT) {
                Log.d(TAG, "u are ON_FOOT");
                //Toast.makeText(this, "u are ON_FOOT", Toast.LENGTH_LONG).show();
            /* user is running walking or on foot
            * initialize start time
            * */
                timerToNotify = 10;//reset notification timer
                inactiveTimer = 200;
                if (startTime == 0) // once startTime started on walking dont update it ever time walking is recognized
                {
                    cal.setTime(new Date());
                    startTime = cal.getTimeInMillis();
                    startTime -= 120000; //set the startTime one minute before so thatwe donot miss any updates
                }

            } else {
                Log.d(TAG, "u are doing " + detectedActivities.getType());
                //Toast.makeText(this, "u are doing "+detectedActivities.getType(), Toast.LENGTH_LONG).show();
                if (startTime == 0) // once startTime started on walking dont update it ever time walking is recognized
                {
                    cal.setTime(new Date());
                    startTime = cal.getTimeInMillis();
                    startTime -= 120000; //set the startTime one minute before so thatwe donot miss any updates
                }
                timerToNotify--;
                inactiveTimer--;

            }
        }
    }

       private void  notify(String message)
        {
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);


            int icon = R.drawable.running_icon;
            CharSequence tickerText = "Janacare";
            long when = System.currentTimeMillis();

            Notification notification = new Notification(icon,
                    tickerText, when);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            Context context = getApplicationContext();
            CharSequence contentTitle = "Habbits";
            CharSequence contentText = message;
            Intent notificationIntent = new Intent(context,
                    MainActivity.class);
            PendingIntent contentIntent = PendingIntent
                    .getActivity(context, 0, notificationIntent, 0);

            notification.setLatestEventInfo(context, contentTitle,
                    contentText, contentIntent);

            mNotificationManager.notify(1, notification);
            // Log.d("test", "Saving Data to File from Service.");
        }


}