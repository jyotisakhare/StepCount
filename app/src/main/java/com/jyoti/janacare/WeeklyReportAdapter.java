package com.jyoti.janacare;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jyoti.janacare.models.WeekStepSummaryRecord;

import java.util.List;

/**
 * Created by jsakhare on 24/04/15.
 */
public class WeeklyReportAdapter extends ArrayAdapter<WeekStepSummaryRecord> {

    private final Activity context;
    List weekRecordList;
    int resource;

    public WeeklyReportAdapter(Activity context, int resource, List<WeekStepSummaryRecord> objects) {
        super(context, resource, objects);
        this.context=context;
        this.weekRecordList=objects;
        this.resource=resource;
    }
    static class ViewHolder {
        public TextView day;
        public TextView steps;

    }
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(this.resource, null);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.day = (TextView) rowView.findViewById(R.id.day_row);
            viewHolder.steps = (TextView) rowView.findViewById(R.id.weekly_steps);
            rowView.setTag(viewHolder);
        }

        // fill data
        ViewHolder holder = (ViewHolder) rowView.getTag();
        WeekStepSummaryRecord p = (WeekStepSummaryRecord)  weekRecordList.get(position);
        holder.day.setText(p.getDate());
        holder.steps.setText(String.valueOf(p.getSteps()));
        return rowView;
    }

}


