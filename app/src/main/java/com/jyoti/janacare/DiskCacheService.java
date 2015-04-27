package com.jyoti.janacare;

import android.content.Context;

import com.jyoti.janacare.models.TodayStepSummaryRecord;
import com.jyoti.janacare.models.WeekStepSummaryRecord;

import java.util.List;

/**
 * Created by jsakhare on 26/04/15.
 */
public class DiskCacheService {

    private static String cache = null;
    private static long maxSize = 1024 * 1024 * 10; //10MB
    private static long currentSize = 0;
    private static DataBaseService dbService = null;
    private static Context context = null;
    private static String TAG = "DISKCACHESERVICE";

    public static void initialize(Context context) {

        dbService = new DataBaseService(context);
    }


    public  static Boolean addWeeklyEntry(WeekStepSummaryRecord entry){
        return dbService.addWeeklyEntry(entry);

    }
    public  static Boolean addDailyEntry(TodayStepSummaryRecord entry){
        return dbService.addDailyEntry(entry);
    }
    public static List<WeekStepSummaryRecord> getWeeKlyEntries(){
        return dbService.getWeeKlyEntries();
    }
    public static List<TodayStepSummaryRecord> getDailyEntries(){
        return dbService.getDailyEntries();
    }
//    private static synchronized void cull(long size) {
//
//        CacheEntity oldentry = dbService.getOldestEntry();
//        removeData(oldentry.getKey());
//
//    }

//    public static synchronized Boolean checkData(String md5) {
//        CacheEntity entry = dbService.getEntry(md5);
//        if ( entry != null && !entry.isExpired()) {
//            return true;
//        } else {
//            removeData(md5);
//        }
//
//        return false;
//    }
    //   public static String getData(String key) {
//
//        CacheEntity entry = dbService.getEntry(key);
//        if(entry != null && !entry.isExpired()){
//            dbService.updateAccessForEntry(key);
//            return entry.getValue();
//        }else{
//            removeData(key);
//        }
//        return  null;
//    }
//    public static synchronized void saveData(String key, String response, Date expiryDate) {
//        if((response == null) || (checkData(key))) return;
//        if (currentSize + response.length() > maxSize) {
//            cull(currentSize + response.length() - maxSize);
//        }
//        Log.d(logTag, "Expiry date for " + key + " => " + expiryDate.toString());
//
//        currentSize += response.length();
//        Date timestamp = new Date();
//        CacheEntity entry = new CacheEntity(key, expiryDate, timestamp,response);
//        dbService.addEntry(entry);
//
//    }

//    public static synchronized void removeData(String key) {
//
//        dbService.deleteEntry(key);
//
//    }

    public static synchronized void clear() {

        dbService.clearEntries();
    }

}
