package com.jyoti.janacare;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jyoti.janacare.models.TodayStepSummaryRecord;

import java.util.List;

/**
 * Created by jsakhare on 22/04/15.
 */
public class TodayStepRecordAdapter extends ArrayAdapter<TodayStepSummaryRecord> {
    private final Activity context;
    List stepRecordList;
    int resource;
    public TodayStepRecordAdapter(Activity context, int resource, List<TodayStepSummaryRecord> objects) {
        super(context, resource, objects);
        this.context=context;
        this.stepRecordList=objects;
        this.resource=resource;
    }
    static class ViewHolder {
        public TextView steps;
        public TextView startTime;
        public TextView endTime;
    }
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(this.resource, null);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.steps = (TextView) rowView.findViewById(R.id.steps_row);
            viewHolder.startTime = (TextView) rowView.findViewById(R.id.start_time_row);
            viewHolder.endTime = (TextView) rowView.findViewById(R.id.end_time_row);
            rowView.setTag(viewHolder);
        }

        // fill data
        ViewHolder holder = (ViewHolder) rowView.getTag();
        TodayStepSummaryRecord p = (TodayStepSummaryRecord)  stepRecordList.get(position);
        holder.steps.setText(String.valueOf(p.getSteps()));
        holder.startTime.setText(p.getStartTime());
        holder.endTime.setText(p.getEndTime());

        return rowView;
    }

}
