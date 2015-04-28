package com.jyoti.janacare;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.jyoti.janacare.models.TodayStepSummaryRecord;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.concurrent.TimeUnit;


public class MainActivity extends ActionBarActivity {
    private ProgressBar progressBar;
    public final static String TAG = "MainActivity";
    private ConnectionResult mFitResultResolution;
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;
    private static final int REQUEST_OAUTH = 1431;
    private TextView step_count,inactive_time,goal_percent;
    private ListView todayStepCountList;
    private ListView todayStepCountListCache;
    private  TodayStepRecordAdapter todayStepRecordAdapter;
    private  TodayStepRecordAdapter todayStepRecordAdapterCache;
    static int totalSteps=0;
    SharedPreferences pref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar=(ProgressBar)findViewById(R.id.progressBar);
        //todayStepCountList = (ListView)findViewById(R.id.today_step_count_list);
        todayStepCountListCache = (ListView)findViewById(R.id.today_step_count_list_from_cache);
        List<TodayStepSummaryRecord> list = new ArrayList<TodayStepSummaryRecord>();
        todayStepRecordAdapter = new TodayStepRecordAdapter(this,R.layout.today_step_count_row,list);
        todayStepRecordAdapterCache = new TodayStepRecordAdapter(this,R.layout.today_step_count_row,list);
        //todayStepCountList.setAdapter(todayStepRecordAdapter);
        todayStepCountListCache.setAdapter(todayStepRecordAdapterCache);
        step_count =  (TextView)findViewById(R.id.step_count);
        inactive_time = (TextView)findViewById(R.id.inactvie_time);
        goal_percent = (TextView)findViewById(R.id.goal_percent);
         pref = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        //loading cache data
        getTodayStepSummary();


        LocalBroadcastManager.getInstance(this).registerReceiver(mFitStatusReceiver, new IntentFilter(Constants.FIT_NOTIFY_INTENT));
        //LocalBroadcastManager.getInstance(this).registerReceiver(mFitDataReceiver, new IntentFilter(Constants.HISTORY_INTENT));

        requestFitConnection();

    }

    private void getTodayStepSummary() {
        progressBar.setVisibility(View.GONE);
        Calendar cal = Calendar.getInstance();
        Date now= new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        long time=0;
        dateFormat.format(now);
        cal.setTime(now);
        totalSteps=0;
       todayStepRecordAdapterCache.clear();
        List<TodayStepSummaryRecord> stepRecordListcache = DiskCacheService.getDailyEntries();
        Log.d("cache size",stepRecordListcache.size()+"");
        for (TodayStepSummaryRecord t : stepRecordListcache){
            totalSteps += t.getSteps();
            try {
                time += dateFormat.parse(t.getEndTime()).getTime() - dateFormat.parse(t.getStartTime()).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Log.d("cache data",t.toString());
            todayStepRecordAdapterCache.add(new TodayStepSummaryRecord(t.getSteps(), t.getStartTime(), t.getEndTime()));
        }

        step_count.setText("Total Steps today: " + totalSteps + " in " + TimeUnit.MILLISECONDS.toHours(time) +":"+(TimeUnit.MILLISECONDS.toMinutes(time))%60);
        long min=0;
        if(now.getMinutes() < (TimeUnit.MILLISECONDS.toMinutes(time))%60){
            min = now.getMinutes();
        }else {
            min = (now.getMinutes()-(TimeUnit.MILLISECONDS.toMinutes(time))%60);
        }
        inactive_time.setText("Total inactive Time : " + (now.getHours() - TimeUnit.MILLISECONDS.toHours(time)) + ":" +
                min);
        float goal = Float.parseFloat(pref.getString(getString(R.string.pref_goal_key), getString(R.string.pref_goal_default)));
        float goal_per = ((float)totalSteps/goal)*100;
        goal_percent.setText("You completed " + Math.round(goal_per) + "% of your goal");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            getTodayStepSummary();
            //handleGetStepsButton();
            return true;
        }else if(id == R.id.action_share){
            createShareForecastIntent();
            return true;
        }
        else if(id == R.id.action_settings){
            Intent intent = new Intent(this,SettingsActivity.class);
            startActivity(intent);
            return true;
        }else if(id == R.id.action_weekly_report){
            Intent intent = new Intent(this,WeeklyReportActivity.class);
            startActivity(intent);
            return true;
        }else  if(id == R.id.action_remove_activity_updates){
            Intent service = new Intent(this, GoogleFitService.class);
            service.putExtra(Constants.SERVICE_REQUEST_TYPE, Constants.TYPE_REMOVE_ACTIVITY_DETECTION);
            startService(service);
        }
        return super.onOptionsItemSelected(item);
    }

    private void cancelSubscription() {
        Intent service = new Intent(this, GoogleFitService.class);
        service.putExtra(Constants.SERVICE_REQUEST_TYPE, Constants.TYPE_CANCLE_SUBCRIPTION);
        startService(service);
    }


    private void handleConnectButton() {
        try {
            authInProgress = true;
            mFitResultResolution.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);
            handleGetStepsButton();
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG,
                    "Activity Thread Google Fit Exception while starting resolution activity", e);
        }
    }

    private void handleGetStepsButton() {
        //progressBar.setVisibility(View.VISIBLE);
        Intent service = new Intent(this, GoogleFitService.class);
        service.putExtra(Constants.SERVICE_REQUEST_TYPE, Constants.TYPE_GET_STEP_TODAY_DATA);
        startService(service);
    }

    private void requestFitConnection() {
        progressBar.setVisibility(View.VISIBLE);
        Intent service = new Intent(this, GoogleFitService.class);
        service.putExtra(Constants.SERVICE_REQUEST_TYPE, Constants.TYPE_REQUEST_CONNECTION);
        startService(service);
    }

    private BroadcastReceiver mFitStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(Constants.FIT_EXTRA_NOTIFY_FAILED_STATUS_CODE) &&
                    intent.hasExtra(Constants.FIT_EXTRA_NOTIFY_FAILED_STATUS_CODE)) {
                int statusCode = intent.getIntExtra(Constants.FIT_EXTRA_NOTIFY_FAILED_STATUS_CODE, 0);
                PendingIntent pendingIntent = intent.getParcelableExtra(Constants.FIT_EXTRA_NOTIFY_FAILED_INTENT);
                ConnectionResult result = new ConnectionResult(statusCode, pendingIntent);
                Log.d(TAG, "Fit connection failed - opening connect screen.");
                fitHandleFailedConnection(result);
            }
            if (intent.hasExtra(Constants.FIT_EXTRA_CONNECTION_MESSAGE)) {
                Log.d(TAG, "Fit connection successful - closing connect screen if it's open.");
                fitHandleConnection();
            }
        }
    };

    private BroadcastReceiver mFitDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Calendar cal = Calendar.getInstance();
            Date now= new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            long time=0;
            dateFormat.format(now);
            cal.setTime(now);

             //progressBar.setVisibility(View.GONE);
            //findViewById(R.id.main_title_text).setVisibility(View.VISIBLE);
            if (intent.hasExtra(Constants.HISTORY_EXTRA_STEPS_TODAY)) {

                totalSteps = intent.getIntExtra(Constants.HISTORY_EXTRA_STEPS_TODAY, 0);
                ArrayList<TodayStepSummaryRecord> stepRecordList = intent.getParcelableArrayListExtra(Constants.HISTOY_EXTRA_STEPS_TODAY_SUMMARY);
               // todayStepRecordAdapter.clear();
                for(TodayStepSummaryRecord t:stepRecordList) {
                    try {
                         time += dateFormat.parse(t.getEndTime()).getTime() - dateFormat.parse(t.getStartTime()).getTime();
                        Log.d("Time",""+ TimeUnit.MILLISECONDS.toHours(time)+":"+(TimeUnit.MILLISECONDS.toMinutes(time))%60+" "+dateFormat.parse(t.getEndTime()).getTime() +" "+ dateFormat.parse(t.getStartTime()).getTime());
                        }
                    catch (ParseException e) {
                        e.printStackTrace();
                    }
                         Log.d(TAG, t.toString());
                   // todayStepRecordAdapter.add(t);
                }

                step_count.setText("Total Steps today: " + totalSteps + " in " + TimeUnit.MILLISECONDS.toHours(time) +":"+(TimeUnit.MILLISECONDS.toMinutes(time))%60);
                long min=0;
                if(now.getMinutes() < (TimeUnit.MILLISECONDS.toMinutes(time))%60){
                    min = now.getMinutes();
                }else {
                    min = (now.getMinutes()-(TimeUnit.MILLISECONDS.toMinutes(time))%60);
                }
                inactive_time.setText("Total inactive Time : " + (now.getHours() - TimeUnit.MILLISECONDS.toHours(time)) + ":" +
                        min);
                float goal = Float.parseFloat(pref.getString(getString(R.string.pref_goal_key), getString(R.string.pref_goal_default)));
                float goal_per = ((float)totalSteps/goal)*100;
                goal_percent.setText("You completed "+Math.round(goal_per)+"% of your goal");
                Log.d("goal_per",goal_per+" "+totalSteps+" "+ goal);
               // Toast.makeText(MainActivity.this, "Total Steps today: " + totalSteps, Toast.LENGTH_SHORT).show();
            }

        }
    };

    private void fitHandleConnection() {
        //Toast.makeText(this, "Fit connected", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
        initializeTimerTaskToUpdateDB();
        handleGetStepsButton();
    }

    private void fitHandleFailedConnection(ConnectionResult result) {
        progressBar.setVisibility(View.GONE);
        Log.i(TAG, "Activity Thread Google Fit Connection failed. Cause: " + result.toString());
        if (!result.hasResolution()) {
            // Show the localized error dialog
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), MainActivity.this, 0).show();
            return;
        }

        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an authorization dialog is displayed to the user.
        if (!authInProgress) {
            if (result.getErrorCode() == FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS) {
                try {
                    Log.d(TAG, "Google Fit connection failed with OAuth failure.  Trying to ask for consent (again)");
                    result.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Activity Thread Google Fit Exception while starting resolution activity", e);
                }
            } else {
                Log.i(TAG, "Activity Thread Google Fit Attempting to resolve failed connection");
                mFitResultResolution = result;
                handleConnectButton();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        fitSaveInstanceState(outState);
    }

    private void fitSaveInstanceState(Bundle outState) {
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        fitActivityResult(requestCode, resultCode);
    }

    private void fitActivityResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Fit auth completed.  Asking for reconnect.");
                requestFitConnection();

            } else {
                try {
                    authInProgress = true;
                    mFitResultResolution.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);

                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG,
                            "Activity Thread Google Fit Exception while starting resolution activity", e);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mFitStatusReceiver);
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mFitDataReceiver);
        super.onDestroy();
    }


    private Intent createShareForecastIntent() {

        File screenShot = takeScreenShot();
        Intent shareintent = new Intent(Intent.ACTION_SEND);
        Uri imageUri = Uri.parse(screenShot.getPath());
        Log.d("image path", screenShot.getPath() + " " + imageUri);
        shareintent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareintent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareintent, "Share Report Via..."));
        return shareintent;
    }
    private File takeScreenShot(){
        View rootView = findViewById(android.R.id.content).getRootView();
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = rootView.getDrawingCache();
        File imagePath = new File(Environment.getExternalStorageDirectory() + "/screenshot.jpeg");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return imagePath;
    }


    private void initializeTimerTaskToUpdateDB() {
        Log.d("timer","scheduling timer task");
        long ONCE_PER_DAY = 1000*60*60*24;
        Date date12am = new Date();
        date12am.setHours(23);
        date12am.setMinutes(59);
        UpdateDailyStepsToDB timerTask = new UpdateDailyStepsToDB();
        Timer timer = new Timer();
        timer.schedule(timerTask,date12am,ONCE_PER_DAY);
        Log.d("timer", date12am.toString());
    }

}
