package com.jyoti.janacare.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jsakhare on 22/04/15.
 */
public class TodayStepSummaryRecord implements Parcelable {
    int steps;
    String startTime;
    String endTime;



    public TodayStepSummaryRecord(int steps, String startTime, String endTime) {
        this.steps = steps;
        this.startTime = startTime;
        this.endTime = endTime;
    }


    public int getSteps() {
        return steps;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(steps);
        dest.writeString(startTime);
        dest.writeString(endTime);
    }

    @Override
    public String toString() {
        return "steps " +steps + " startTime "+ startTime+" endTime " + endTime;
    }
}
