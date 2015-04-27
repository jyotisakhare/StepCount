package com.jyoti.janacare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.jyoti.janacare.models.WeekStepSummaryRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class WeeklyReportActivity extends ActionBarActivity {
    public final static String TAG = "WeeklyReportActivity";
    ListView weeklyReportList;
    private ProgressBar progressBar;
    WeeklyReportAdapter weeklyrecordAdapter;
    View rootView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         rootView = findViewById(android.R.id.content).getRootView();
        setContentView(R.layout.activity_weekly_report);
        weeklyReportList = (ListView)findViewById(R.id.weekly_step_count_list);
        progressBar=(ProgressBar)findViewById(R.id.progressBar);
        List<WeekStepSummaryRecord> list = new ArrayList<WeekStepSummaryRecord>();
        weeklyrecordAdapter = new WeeklyReportAdapter(this,R.layout.weekly_step_count_row,list);
        weeklyReportList.setAdapter(weeklyrecordAdapter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle("Weekly Report");
        getWeeklyReport();
        
        LocalBroadcastManager.getInstance(this).registerReceiver(mFitDataReceiver, new IntentFilter(Constants.HISTORY_INTENT));

    }

    private void getWeeklyReport() {
        progressBar.setVisibility(View.VISIBLE);
        Intent service = new Intent(this, GoogleFitService.class);
        service.putExtra(Constants.SERVICE_REQUEST_TYPE, Constants.TYPE_WEEKLY_STEP_COUNT);
        startService(service);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_weekly_report, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this,SettingsActivity.class);
            startActivity(intent);
            return true;

        }else if(id == R.id.week_action_share){

            createShareForecastIntent();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private BroadcastReceiver mFitDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            progressBar.setVisibility(View.GONE);
            findViewById(R.id.weekly_title_text).setVisibility(View.VISIBLE);
            if (intent.hasExtra(Constants.HISTOY_EXTRA_STEPS_WEEK_SUMMARY)) {

                ArrayList<WeekStepSummaryRecord> stepRecordList = intent.getParcelableArrayListExtra(Constants.HISTOY_EXTRA_STEPS_WEEK_SUMMARY);
                weeklyrecordAdapter.clear();
                for(WeekStepSummaryRecord t:stepRecordList) {
                    weeklyrecordAdapter.add(t);
                }
            }

        }
    };
    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mFitDataReceiver);
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
            Log.e("GREC", e.getMessage(), e);
        } catch (IOException e) {
            Log.e("GREC", e.getMessage(), e);
        }

        return imagePath;
    }

}
