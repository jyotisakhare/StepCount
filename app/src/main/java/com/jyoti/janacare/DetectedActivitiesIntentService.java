
package com.jyoti.janacare;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

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
    static  SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
    protected static final String TAG = "detection-intent";
    static Calendar cal = Calendar.getInstance();
    //static Date now = new Date();
    private static int timerToNotify = 180;
    private static long inactiveTimer = 7200;
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
    }

    /**
     * Handles incoming intents.
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        DetectedActivity detectedActivities =  result.getMostProbableActivity();

        if(detectedActivities.getType()==DetectedActivity.STILL)
        {
            Log.d(TAG,"u are STILL");
            /* user is not moving
            * initialize endtime
            * calcutate steps betwen starttime and endtime
            * store it into db
            * intialize a to_notify_steps
            */

            cal.setTime(new Date());
            endTime = cal.getTimeInMillis();
//            List<TodayStepSummaryRecord> list1 = DiskCacheService.getDailyEntries();
//            for(TodayStepSummaryRecord t : list1){
//                Log.d("records",t.toString());
//            }
            if(startTime != 0){
                stepsToNotify += GoogleFitService.getStepCountBetweenInterval(startTime,endTime);
                TodayStepSummaryRecord rec = new TodayStepSummaryRecord(stepsToNotify,dateFormat.format(startTime),dateFormat.format(endTime),0);
                boolean status = DiskCacheService.addDailyEntry(rec);
                if(status)
                Log.d(TAG,stepsToNotify+" added to database");
                List<TodayStepSummaryRecord> list = DiskCacheService.getDailyEntries();
                for(TodayStepSummaryRecord t : list){
                    Log.d("records",t.toString());
                }
                startTime = 0;
            }

            timerToNotify -= 1;
            if(timerToNotify == 0 && stepsToNotify > 0){
                //todo notify user
                //todo u just waled stepstonotify steps;
                Toast.makeText(this, "u just walked "+stepsToNotify+" steps", Toast.LENGTH_LONG).show();
                Log.d(TAG,"u just walked "+stepsToNotify+" steps");
                stepsToNotify = 0;
                timerToNotify = 180;
            }
            inactiveTimer--;
            if(inactiveTimer == 0){
                //todo notify user u are inactive from a long period of time
                Toast.makeText(this, "u are inactive from a long period of time", Toast.LENGTH_LONG).show();
                Log.d(TAG,"u are inactive from a long period of time");
            }

        }
        else if(detectedActivities.getType()==DetectedActivity.RUNNING  ||
                 detectedActivities.getType()==DetectedActivity.WALKING ||
                 detectedActivities.getType()==DetectedActivity.ON_FOOT)
        {
            Log.d(TAG,"u are ON_FOOT");
            /* user is running walking or on foot
            * initialize start time
            * */
            timerToNotify = 180;//reset notification timer
            if(startTime == 0) // once startTime started on walking dont update it ever time walking is recognized
            {
                cal.setTime(new Date());
                startTime = cal.getTimeInMillis();
                startTime -= 60000; //set the startTime one minute before so thatwe donot miss any updates
            }

        }else {
            Log.d(TAG,"u are doing "+detectedActivities.getType());
            if(startTime == 0) // once startTime started on walking dont update it ever time walking is recognized
            {
                cal.setTime(new Date());
                startTime = cal.getTimeInMillis();
                startTime -= 60000; //set the startTime one minute before so thatwe donot miss any updates
            }
            timerToNotify--;
            inactiveTimer--;

        }

    }
}