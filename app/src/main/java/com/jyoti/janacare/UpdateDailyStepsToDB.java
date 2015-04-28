package com.jyoti.janacare;

import android.util.Log;

import com.jyoti.janacare.models.TodayStepSummaryRecord;
import com.jyoti.janacare.models.WeekStepSummaryRecord;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by jsakhare on 28/04/15.
 */
public class UpdateDailyStepsToDB extends TimerTask {

    @Override
    public void run() {
        SimpleDateFormat dateFormate = new SimpleDateFormat("yyyy.MM.dd");
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 58);
        cal.set(Calendar.SECOND, 0);
        long startTime = cal.getTimeInMillis();
        int totalSteps =0 ;//= GoogleFitService.getStepCountBetweenInterval(startTime, endTime);
        List<TodayStepSummaryRecord> todayRec = DiskCacheService.getDailyEntries();
        for(TodayStepSummaryRecord t: todayRec){
            totalSteps += t.getSteps();
        }
        WeekStepSummaryRecord rec = new WeekStepSummaryRecord(dateFormate.format(now),totalSteps);
        boolean status = DiskCacheService.addWeeklyEntry(rec);
        if (status){
            Log.d("timertask","weekly entry added successfully");
        }
    }
}
