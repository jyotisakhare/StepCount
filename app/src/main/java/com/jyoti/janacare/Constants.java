package com.jyoti.janacare;

/**
 * Created by jsakhare on 26/04/15.
 */
public class Constants {
    public static long DETECTION_INTERVAL_IN_MILLISECONDS=1000;

    public static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";
    public static final String SERVICE_REQUEST_TYPE = "requestType";
    public static final int TYPE_GET_STEP_TODAY_DATA = 1;
    public static final int TYPE_REQUEST_CONNECTION = 2;
    public static final int TYPE_CANCLE_SUBCRIPTION = 3;
    public static final int TYPE_WEEKLY_STEP_COUNT = 4;
    public static final int TYPE_GET_STEP_COUNT_BETWEEN_INTERVAL = 5;
    public static final String HISTORY_INTENT = "fitHistory";
    public static final String HISTORY_EXTRA_STEPS_TODAY = "stepsToday";
    public static final String HISTOY_EXTRA_STEPS_TODAY_SUMMARY ="stepsTodaySummary";
    public static final String HISTOY_EXTRA_STEPS_WEEK_SUMMARY ="stepsWeekSummary";
    public static final String FIT_NOTIFY_INTENT = "fitStatusUpdateIntent";
    public static final String FIT_EXTRA_CONNECTION_MESSAGE = "fitFirstConnection";
    public static final String FIT_EXTRA_NOTIFY_FAILED_STATUS_CODE = "fitExtraFailedStatusCode";
    public static final String FIT_EXTRA_NOTIFY_FAILED_INTENT = "fitExtraFailedIntent";

}
