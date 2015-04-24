package com.jyoti.janacare;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import com.jyoti.janacare.models.TodayStepSummaryRecord;
import com.jyoti.janacare.models.WeekStepSummaryRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class GoogleFitService extends IntentService {
    public static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";
    public static final String TAG = "GoogleFitService";
    public GoogleApiClient mGoogleApiFitnessClient;
    public boolean mTryingToConnect = false;

    public static final String SERVICE_REQUEST_TYPE = "requestType";
    public static final int TYPE_GET_STEP_TODAY_DATA = 1;
    public static final int TYPE_REQUEST_CONNECTION = 2;
    public static final int TYPE_CANCLE_SUBCRIPTION = 3;
    public static final int TYPE_WEEKLY_STEP_COUNT =4;
    public static final String HISTORY_INTENT = "fitHistory";
    public static final String HISTORY_EXTRA_STEPS_TODAY = "stepsToday";
    public static final String HISTOY_EXTRA_STEPS_TODAY_SUMMARY ="stepsTodaySummary";
    public static final String HISTOY_EXTRA_STEPS_WEEK_SUMMARY ="stepsWeekSummary";
    public static final String FIT_NOTIFY_INTENT = "fitStatusUpdateIntent";
    public static final String FIT_EXTRA_CONNECTION_MESSAGE = "fitFirstConnection";
    public static final String FIT_EXTRA_NOTIFY_FAILED_STATUS_CODE = "fitExtraFailedStatusCode";
    public static final String FIT_EXTRA_NOTIFY_FAILED_INTENT = "fitExtraFailedIntent";
    private OnDataPointListener mListener;
    @Override
    public void onDestroy() {
      super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildFitnessClient();
        Log.d(TAG, "GoogleFitService created");
    }

    public GoogleFitService() {
        super("GoogleFitService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        //Get the request type
        int type = intent.getIntExtra(SERVICE_REQUEST_TYPE, -1);

        //block until google fit connects.
        if (!mGoogleApiFitnessClient.isConnected()) {
            mTryingToConnect = true;
            mGoogleApiFitnessClient.connect();

            //Wait until the service either connects or fails to connect
            while (mTryingToConnect) {
                try {
                    Thread.sleep(100, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (mGoogleApiFitnessClient.isConnected()) {
            if (type == TYPE_GET_STEP_TODAY_DATA) {
                Log.d(TAG, "Requesting steps from Google Fit");
                getStepsToday();
                //Log.d(TAG, "Fit update complete.  Allowing Android to destroy the service.");
            } else if (type == TYPE_REQUEST_CONNECTION) {
                //Don't need to do anything because the connection is already requested above
            }else  if(type==TYPE_CANCLE_SUBCRIPTION){
                cancelSubscription();
            }else if(type == TYPE_WEEKLY_STEP_COUNT){
                getStepCountForAWeek();
            }
        } else {
            //Not connected
            Log.w(TAG, "Fit wasn't able to connect, so the request failed.");
        }
    }

    private void getStepsToday() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startTime = cal.getTimeInMillis();

        final DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult =
                Fitness.HistoryApi.readData(mGoogleApiFitnessClient, readRequest).await(1, TimeUnit.MINUTES);

        DataSet stepData = dataReadResult.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);

        int totalSteps = 0;

        for (DataPoint dp : stepData.getDataPoints()) {
            for(Field field : dp.getDataType().getFields()) {
                if(dp.getValue(field).asInt() < 30) continue;
                int steps = dp.getValue(field).asInt();

                totalSteps += steps;

            }
        }

        publishTodaysStepData(totalSteps);
    }

    private List<TodayStepSummaryRecord> getStepCountSummaryForADay(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        List<TodayStepSummaryRecord> stepRecordList = new ArrayList<TodayStepSummaryRecord>();
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startTime = cal.getTimeInMillis();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.HOURS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(mGoogleApiFitnessClient, readRequest).await();
        if (dataReadResult.getBuckets().size() > 0) {
            Log.d("DataSet.size(): ", String.valueOf(dataReadResult.getBuckets().size()));
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    Log.d(" Bucket  ", "dataSet.dataType:" + dataSet.getDataType().getName());

                    for (DataPoint dp : dataSet.getDataPoints()) {
                        for(Field field : dp.getDataType().getFields()) {
                            //describeDataPoint(dp);
                            if(dp.getValue(field).asInt() < 30) continue;
                            Log.d(TAG, String.valueOf(dp.getValue(field))+dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))+dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                            TodayStepSummaryRecord stepRecord = new TodayStepSummaryRecord(Integer.parseInt(String.valueOf(dp.getValue(field))),
                                    dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)),
                                    dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                            Log.d(TAG,stepRecord.toString());
                            stepRecordList.add(stepRecord);
                        }
                    }
                }
            }
        }
        return stepRecordList;
    }

    private void getStepCountForAWeek(){
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 week before this moment.
        List<WeekStepSummaryRecord> weekrecordList = new ArrayList<WeekStepSummaryRecord>();
        SimpleDateFormat dateFormatWeekRecord = new SimpleDateFormat("EEE, MMM d, ''yy");
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        //cal.set(0,0,0,0,0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        // [END build_read_data_request]
        DataReadResult dataReadResult =  Fitness.HistoryApi.readData(mGoogleApiFitnessClient, readRequest).await();
        if (dataReadResult.getBuckets().size() > 0) {
            Log.d("DataSet.size(): ", String.valueOf(dataReadResult.getBuckets().size()));
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    Log.d(" week  ", "dataSet.dataType:" + dataSet.getDataType().getName());
                    for (DataPoint dp : dataSet.getDataPoints()) {
                        for(Field field : dp.getDataType().getFields()) {
                            if(dp.getValue(field).asInt() < 30) continue;
                            Log.d(TAG, String.valueOf(dp.getValue(field))+dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))+dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                            WeekStepSummaryRecord weekRecord = new WeekStepSummaryRecord(
                                    dateFormatWeekRecord.format(dp.getStartTime(TimeUnit.MILLISECONDS)),
                                    Integer.parseInt(String.valueOf(dp.getValue(field))));
                            Log.d(TAG,weekRecord.toString());
                            weekrecordList.add(weekRecord);
                        }
                    }
                }
            }
        }
        publishWeeklyStepData(weekrecordList);

    }
//    public void describeDataPoint(DataPoint dp) {
//        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
//        String msg = "dataPoint: "
//                //+ "type: " + dp.getDataType().getName()
//                +"\n"
//                + ", range: [" + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + "-" + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + "]\n"
//                + ", fields: [";
//
//        for(Field field : dp.getDataType().getFields()) {
//            msg += field.getName() + "=" + dp.getValue(field) + " ";
//        }
//
//        msg += "]";
//        Log.d("msg",msg);
//
//    }

protected void publishTodaysStepData(int totalSteps) {
    ArrayList<TodayStepSummaryRecord> stepsRecordList = (ArrayList<TodayStepSummaryRecord>)getStepCountSummaryForADay();
    Intent intent = new Intent(HISTORY_INTENT);

    intent.putExtra(HISTORY_EXTRA_STEPS_TODAY, totalSteps);
    intent.putParcelableArrayListExtra(HISTOY_EXTRA_STEPS_TODAY_SUMMARY, stepsRecordList);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    protected void publishWeeklyStepData(List<WeekStepSummaryRecord> weeklyRecord) {
        ArrayList<WeekStepSummaryRecord> weeksRecordList = (ArrayList<WeekStepSummaryRecord>)weeklyRecord;
        Intent intent = new Intent(HISTORY_INTENT);
        intent.putParcelableArrayListExtra(HISTOY_EXTRA_STEPS_WEEK_SUMMARY, weeksRecordList);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void buildFitnessClient() {
        // Create the Google API Client
        mGoogleApiFitnessClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.API)
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Google Fit connected.");
                                mTryingToConnect = false;
                                Log.d(TAG, "Notifying the UI that we're connected.");
                                findFitnessDataSources();
                                subscribe();
                                notifyUiFitConnected();


                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                mTryingToConnect = false;
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Google Fit Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Google Fit Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            // Called whenever the API client fails to connect.
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                mTryingToConnect = false;
                                notifyUiFailedConnection(result);
                            }
                        }
                )
                .build();
    }

    private void notifyUiFitConnected() {
        Intent intent = new Intent(FIT_NOTIFY_INTENT);
        intent.putExtra(FIT_EXTRA_CONNECTION_MESSAGE, FIT_EXTRA_CONNECTION_MESSAGE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void notifyUiFailedConnection(ConnectionResult result) {
        Intent intent = new Intent(FIT_NOTIFY_INTENT);
        intent.putExtra(FIT_EXTRA_NOTIFY_FAILED_STATUS_CODE, result.getErrorCode());
        intent.putExtra(FIT_EXTRA_NOTIFY_FAILED_INTENT, result.getResolution());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void findFitnessDataSources() {
        // [START find_data_sources]
        Fitness.SensorsApi.findDataSources(mGoogleApiFitnessClient, new DataSourcesRequest.Builder()
                .setDataTypes(
                        DataType.TYPE_LOCATION_SAMPLE,
                        DataType.TYPE_STEP_COUNT_DELTA,
                        DataType.TYPE_DISTANCE_DELTA,
                        DataType.TYPE_HEART_RATE_BPM)
                .setDataSourceTypes(DataSource.TYPE_RAW, DataSource.TYPE_DERIVED)
                .build())
                .setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult) {
                        Log.i(TAG, " findFitnessDataSources Result: " + dataSourcesResult.getStatus().toString()+String.valueOf(dataSourcesResult.getDataSources().size()));
                        for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                            //Log.i(TAG, "findFitnessDataSources Data source found: " + dataSource.toString());
                            //Log.i(TAG, "findFitnessDataSources Data Source type: " + dataSource.getDataType().getName());
                            //Let's register a listener to receive Activity data!
                            if (dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA)
                                    && mListener == null) {
                                Log.i(TAG, "findFitnessDataSources Data source for LOCATION_SAMPLE found!  Registering.");
                                registerFitnessDataListener(dataSource,
                                        DataType.TYPE_STEP_COUNT_DELTA);
                            }
                        }
                    }
                });
        // [END find_data_sources]
    }
    /**
     * Register a listener with the Sensors API for the provided {@link DataSource} and
     * {@link DataType} combo.
     */
    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {
        mListener = new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                for (Field field : dataPoint.getDataType().getFields()) {
                    Value val = dataPoint.getValue(field);
                    Log.i(TAG, "Detected DataPoint field: " + field.getName());
                    Log.i(TAG, "Detected DataPoint value: " + val);
                }
            }
        };

        Fitness.SensorsApi.add(
                mGoogleApiFitnessClient,
                new SensorRequest.Builder()
                        .setDataSource(dataSource) // Optional but recommended for custom data sets.
                        .setDataType(dataType) // Can't be omitted.
                        .setSamplingRate(10, TimeUnit.SECONDS)
                        .build(),
                mListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener registered!");
                        } else {
                            Log.i(TAG, "Listener not registered.");
                        }
                    }
                });

    }

    /**
     * Unregister the listener with the Sensors API.
     */
    private void unregisterFitnessDataListener() {
        if (mListener == null) {
            return;
        }

        Fitness.SensorsApi.remove(
                mGoogleApiFitnessClient,
                mListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener was removed!");
                        } else {
                            Log.i(TAG, "Listener was not removed.");
                        }
                    }
                });
        // [END unregister_data_listener]
    }

    /**
     * Subscribe to an available {@link DataType}. Subscriptions can exist across application
     * instances (so data is recorded even after the application closes down).  When creating
     * a new subscription, it may already exist from a previous invocation of this app.  If
     * the subscription already exists, the method is a no-op.  However, you can check this with
     * a special success code.
     */
    public void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        // [START subscribe_to_datatype]
        Fitness.RecordingApi.subscribe(mGoogleApiFitnessClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                            }
                        } else {
                            Log.i(TAG, "There was a problem subscribing.");
                        }
                    }
                });
        // [END subscribe_to_datatype]
    }

    /**
     * Fetch a list of all active subscriptions and log it. Since the logger for this sample
     * also prints to the screen, we can see what is happening in this way.
     */
    private void dumpSubscriptionsList() {
        // [START list_current_subscriptions]
        Fitness.RecordingApi.listSubscriptions(mGoogleApiFitnessClient, DataType.TYPE_STEP_COUNT_DELTA)
                // Create the callback to retrieve the list of subscriptions asynchronously.
                .setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
                    @Override
                    public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                        for (Subscription sc : listSubscriptionsResult.getSubscriptions()) {
                            DataType dt = sc.getDataType();
                            Log.i(TAG, "Active subscription for data type: " + dt.getName());
                        }
                    }
                });
        // [END list_current_subscriptions]
    }

    /**
     * Cancel the ACTIVITY_SAMPLE subscription by calling unsubscribe on that {@link DataType}.
     */
    protected void cancelSubscription() {
        final String dataTypeStr = DataType.TYPE_ACTIVITY_SAMPLE.toString();
        Log.i(TAG, "Unsubscribing from data type: " + dataTypeStr);

        // Invoke the Recording API to unsubscribe from the data type and specify a callback that
        // will check the result.
        // [START unsubscribe_from_datatype]
        Fitness.RecordingApi.unsubscribe(mGoogleApiFitnessClient, DataType.TYPE_ACTIVITY_SAMPLE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully unsubscribed for data type: " + dataTypeStr);
                        } else {
                            // Subscription not removed
                            Log.i(TAG, "Failed to unsubscribe for data type: " + dataTypeStr);
                        }
                    }
                });
        // [END unsubscribe_from_datatype]
    }


}
