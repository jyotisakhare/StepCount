package com.jyoti.janacare.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jsakhare on 22/04/15.
 */
public class WeekStepSummaryRecord implements Parcelable {

    String date;
    int steps;

    public WeekStepSummaryRecord(String date, int steps) {
        this.date = date;
        this.steps = steps;
    }

    public String getDate() {
        return date;
    }

    public int getSteps() {
        return steps;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(steps);
        dest.writeString(date);
    }

    @Override
    public String toString() {
        return "steps " +steps + " date "+ date;
    }
}
