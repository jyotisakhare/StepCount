package com.jyoti.janacare;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.jyoti.janacare.StepCountContract.DailyEntry;
import com.jyoti.janacare.StepCountContract.WeeklyEntry;
import com.jyoti.janacare.models.TodayStepSummaryRecord;
import com.jyoti.janacare.models.WeekStepSummaryRecord;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by jsakhare on 12/04/15.
 */

public class DataBaseService extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "DataBaseService";
    // Database Name
    public static final String DATABASE_NAME = "StepCount";


    public DataBaseService(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        final String CREATE_TABLE_DAILY_ENTRY = "CREATE TABLE " + DailyEntry.TABLE_NAME + "("
                + DailyEntry._ID + " INTEGER PRIMARY KEY ,"
                + DailyEntry.COLUMN_STEPS + " INTEGER NOT NULL,"
                + DailyEntry.COLUMN_START_TIME + " TEXT NOT NULL,"
                + DailyEntry.COLUMN_END_TIME + " TEXT NOT NULL,"
                + DailyEntry.COLUMN_NOTIFY_USER + " INTEGER ,"
                + DailyEntry.COLUMN_DATE + " TEXT"
                + ")";
        final String CREATE_TABLE_WEEKLY_ENTRY = "CREATE TABLE " + WeeklyEntry.TABLE_NAME + "("
                + WeeklyEntry._ID + " INTEGER PRIMARY KEY ,"
                + WeeklyEntry.COLUMN_STEPS + " INTEGER NOT NULL,"
                + WeeklyEntry.COLUMN_DATE + " TEXT TEXT UNIQUE NOT NULL"
                + ")";

        db.execSQL(CREATE_TABLE_DAILY_ENTRY);
        db.execSQL(CREATE_TABLE_WEEKLY_ENTRY);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + DailyEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WeeklyEntry.TABLE_NAME);
        // Create tables again
        onCreate(db);
    }

    public synchronized Boolean addWeeklyEntry(WeekStepSummaryRecord entry) {
        try {
            SimpleDateFormat dateFormate = new SimpleDateFormat("yyyy.MM.dd");
            Date now = new Date();
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            //DateFormat dateform = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            values.put(WeeklyEntry.COLUMN_DATE, entry.getDate());
            values.put(WeeklyEntry.COLUMN_STEPS, entry.getSteps());
            db.update(WeeklyEntry.TABLE_NAME, values, WeeklyEntry.COLUMN_DATE+ "=?",new String[]{dateFormate.format(now)});
            db.close();
            return true;
        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
    public synchronized Boolean addDailyEntry(TodayStepSummaryRecord entry) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            DateFormat dateform = new SimpleDateFormat("yyyy-MM-dd");
            values.put(DailyEntry.COLUMN_STEPS, entry.getSteps());
            values.put(DailyEntry.COLUMN_START_TIME, entry.getStartTime());
            values.put(DailyEntry.COLUMN_END_TIME, entry.getEndTime());
            values.put(DailyEntry.COLUMN_NOTIFY_USER,entry.getNotifyUser());
            values.put(DailyEntry.COLUMN_DATE,dateform.format(new Date()));
            db.insert(DailyEntry.TABLE_NAME, null, values);
            db.close();
            return true;
        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<WeekStepSummaryRecord> getWeeKlyEntries() {
        List<WeekStepSummaryRecord> weekRecordList = new ArrayList<WeekStepSummaryRecord>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            DateFormat dateform = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Cursor cursor = db.query(WeeklyEntry.TABLE_NAME, new String[]{WeeklyEntry.COLUMN_STEPS, WeeklyEntry.COLUMN_DATE}, null, null, null, null, WeeklyEntry._ID + " DESC ", " 7");
            //int size=cursor.getCount();
            if (cursor != null && cursor.moveToFirst()){
                do {
                    WeekStepSummaryRecord entry = new WeekStepSummaryRecord(cursor.getString(1),cursor.getInt(0));
                    weekRecordList.add(entry);
                    cursor.moveToNext();
                }while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
            e.printStackTrace();
        }
        return weekRecordList;
    }

    public List<TodayStepSummaryRecord> getDailyEntries() {
        //todo write where condition here
        DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        List<TodayStepSummaryRecord> todayRecordList = new ArrayList<TodayStepSummaryRecord>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            DateFormat dateform = new SimpleDateFormat("HH:mm");
            Cursor cursor = db.query(false,DailyEntry.TABLE_NAME, new String[]{DailyEntry.COLUMN_STEPS, DailyEntry.COLUMN_START_TIME,DailyEntry.COLUMN_END_TIME,DailyEntry.COLUMN_NOTIFY_USER},
                    DailyEntry.COLUMN_DATE  + "= ?  " , new String[] {dateformat.format(new Date())},
                    null,null, null, null,null);
            //int size=cursor.getCount();
            //cursor.moveToFirst();
            if (cursor != null && cursor.moveToFirst()){
            do {
                TodayStepSummaryRecord entry = new TodayStepSummaryRecord(cursor.getInt(0),cursor.getString(1),cursor.getString(2),cursor.getInt(3));
                todayRecordList.add(entry);
            } while (cursor.moveToNext());
             }



        } catch (Exception e) {
           // Log.d(TAG,e.getMessage());
            e.printStackTrace();
        }
        return todayRecordList;
    }



    public synchronized void clearEntries() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(WeeklyEntry.TABLE_NAME, null, null);
            db.delete(DailyEntry.TABLE_NAME, null, null);
            db.close();
        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
            e.printStackTrace();
        }
    }


//    public synchronized Boolean deleteEntry(String key) {
//        try {
//            SQLiteDatabase db = this.getWritableDatabase();
//            db.delete(TABLE_CACHE, KEY + " = ?", new String[] { key });
//            db.close();
//            return true;
//        } catch (Exception e) {
//        }
//        return false;
//    }

//    public synchronized Boolean updateAccessForEntry(String key) {
//        try {
//            SQLiteDatabase db = this.getWritableDatabase();
//            DateFormat dateform = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
//            ContentValues values = new ContentValues();
//            values.put(KEY_ACCESS, dateform.format(new Date()));
//            int updateStatus = db.update(TABLE_CACHE, values, KEY + " = ?", new String[] { key });
//            if(updateStatus != 0) return true;
//        } catch (Exception e) {
//        }
//        return false;
//    }

//    public CacheEntity getOldestEntry() {
//        try {
//            SQLiteDatabase db = this.getReadableDatabase();
//            DateFormat dateform = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
//            Cursor cursor = db.query(TABLE_CACHE, new String[] { KEY, KEY_EXPIRY, KEY_ACCESS ,KEY_VALUE}, null, null, null, null, KEY_ACCESS, "1");
//            if (cursor != null) cursor.moveToFirst();
//            CacheEntity entry = new CacheEntity(cursor.getString(0),dateform.parse(cursor.getString(1)),dateform.parse(cursor.getString(2)),cursor.getString(3));
//            return entry;
//        } catch (Exception e) {
//        }
//        return null;
//    }

}
