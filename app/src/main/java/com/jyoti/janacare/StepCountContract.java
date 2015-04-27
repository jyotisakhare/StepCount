package com.jyoti.janacare;

import android.provider.BaseColumns;

/**
 * Created by jsakhare on 26/04/15.
 */
public class StepCountContract {

    /* Inner class that defines the table contents of the location table */
    public static final class DailyEntry implements BaseColumns {
       // Table name
        public static final String TABLE_NAME = "DailyReport";
        public static final String COLUMN_STEPS = "steps";
        public static final String COLUMN_START_TIME = "start_time";
        public static final String COLUMN_END_TIME = "end_time";
        public static final String COLUMN_NOTIFY_USER = "notify_user";
        public static final String COLUMN_DATE="date";

    }
    public static final class WeeklyEntry implements BaseColumns {
        // Table name
        public static final String TABLE_NAME = "WeeklyReport";
        //public static final String COLUMN_ID = "id";
        public static final String COLUMN_STEPS = "steps";
        public static final String COLUMN_DATE = "date";

    }
}
