package com.test.googlefitservice;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.jyoti.janacare.DataBaseService;
import com.jyoti.janacare.StepCountContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jsakhare on 26/04/15.
 */
public class TestDb extends AndroidTestCase {
    public static final String TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(DataBaseService.DATABASE_NAME);
    }
    
    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        deleteTheDatabase();
    }


    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        Log.d(TAG,"creating DAtabase");

        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(StepCountContract.DailyEntry.TABLE_NAME);
        tableNameHashSet.add(StepCountContract.WeeklyEntry.TABLE_NAME);

        mContext.deleteDatabase(DataBaseService.DATABASE_NAME);
        SQLiteDatabase db = new DataBaseService(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without both the daily entry and weekly entry tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + StepCountContract.DailyEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> dailyColumnHashSet = new HashSet<String>();
        dailyColumnHashSet.add(StepCountContract.DailyEntry._ID);
        dailyColumnHashSet.add(StepCountContract.DailyEntry.COLUMN_STEPS);
        dailyColumnHashSet.add(StepCountContract.DailyEntry.COLUMN_START_TIME);
        dailyColumnHashSet.add(StepCountContract.DailyEntry.COLUMN_END_TIME);
        dailyColumnHashSet.add(StepCountContract.DailyEntry.COLUMN_NOTIFY_USER);
        dailyColumnHashSet.add(StepCountContract.DailyEntry.COLUMN_DATE);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            dailyColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required Daily entry columns",
                dailyColumnHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + StepCountContract.WeeklyEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> weeklyColumnHashSet = new HashSet<String>();
        weeklyColumnHashSet.add(StepCountContract.WeeklyEntry._ID);
        weeklyColumnHashSet.add(StepCountContract.WeeklyEntry.COLUMN_STEPS);
        weeklyColumnHashSet.add(StepCountContract.WeeklyEntry.COLUMN_STEPS);

         columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            weeklyColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required weekly entry columns",
                weeklyColumnHashSet.isEmpty());
        db.close();
    }
    
    public void testDailyEntryTable(){
        Log.d(TAG,"creating testDailyEntryTable");
        //assertTrue("creating db",false);
        // First step: Get reference to writable database
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        DataBaseService dbHelper = new DataBaseService(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Second Step: Create ContentValues of what you want to insert
        ContentValues testValues = new ContentValues();
        DateFormat dateform = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        testValues.put(StepCountContract.DailyEntry.COLUMN_STEPS, 1000);
        testValues.put(StepCountContract.DailyEntry.COLUMN_START_TIME,dateform.format(new Date()) );
        testValues.put(StepCountContract.DailyEntry.COLUMN_END_TIME, dateform.format(new Date()));
        testValues.put(StepCountContract.DailyEntry.COLUMN_NOTIFY_USER, 0);
        testValues.put(StepCountContract.DailyEntry.COLUMN_DATE, dateform.format(new Date()));

        // Third Step: Insert ContentValues into database and get a row ID back
        long locationRowId;
        locationRowId = db.insert(StepCountContract.DailyEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue(locationRowId != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // Fourth Step: Query the database and receive a Cursor back
        // A cursor is your primary interface to the query results.
        Cursor cursor = db.query(
                StepCountContract.DailyEntry.TABLE_NAME,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        // Move the cursor to a valid database row and check to see if we got any records back
        // from the query
        assertTrue( "Error: No Records returned from daily query", cursor.moveToFirst() );

        // Fifth Step: Validate data in resulting Cursor with the original ContentValues
        // (you can use the validateCurrentRecord function in TestUtilities to validate the
        // query if you like)
        validateCurrentRecord("Error: daily Query Validation Failed", cursor, testValues);
       // String error ="Error: Location Query Validation Failed";

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse("Error: More than one record returned from daily query",
                cursor.moveToNext());

        // Sixth Step: Close Cursor and Database
        cursor.close();
        db.close();

    }
    public void testWeeklyEntryTable(){
        Log.d(TAG,"creating testWeeklyEntryTable");

        // First step: Get reference to writable database
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        DataBaseService dbHelper = new DataBaseService(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Second Step: Create ContentValues of what you want to insert
        ContentValues testValues = new ContentValues();
        DateFormat dateform = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        testValues.put(StepCountContract.WeeklyEntry.COLUMN_STEPS, 1000);
        testValues.put(StepCountContract.WeeklyEntry.COLUMN_DATE,dateform.format(new Date()) );


        // Third Step: Insert ContentValues into database and get a row ID back
        long locationRowId;
        locationRowId = db.insert(StepCountContract.WeeklyEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue(locationRowId != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // Fourth Step: Query the database and receive a Cursor back
        // A cursor is your primary interface to the query results.
        Cursor cursor = db.query(
                StepCountContract.WeeklyEntry.TABLE_NAME,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        // Move the cursor to a valid database row and check to see if we got any records back
        // from the query
        assertTrue( "Error: No Records returned from weekly query", cursor.moveToFirst() );

        // Fifth Step: Validate data in resulting Cursor with the original ContentValues
        // (you can use the validateCurrentRecord function in TestUtilities to validate the
        // query if you like)
        validateCurrentRecord("Error: Weekly Query Validation Failed", cursor, testValues);
        // String error ="Error: Location Query Validation Failed";

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse("Error: More than one record returned from Weekly query",
                cursor.moveToNext());

        // Sixth Step: Close Cursor and Database
        cursor.close();
        db.close();

    }
    public void validateCurrentRecord(String error, Cursor cursor, ContentValues testValues){
        Set<Map.Entry<String, Object>> valueSet = testValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = cursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals("Value '" + entry.getValue().toString() +
                    "' did not match the expected value '" +
                    expectedValue + "'. " + error, expectedValue, cursor.getString(idx));
        }
    }
}


