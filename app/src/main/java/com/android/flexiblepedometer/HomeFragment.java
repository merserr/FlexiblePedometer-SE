package com.android.flexiblepedometer;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by AREG on 01.03.2017.
 */

public class HomeFragment extends Fragment {

    private LineChart mGraph;

    private int mCurrentCalories;
    private int mCurrentSteps;
    private int mCurrentMeters;
    private int mTodaySteps;
    private float mStepWidth;

    private TextView mCalories;
    private TextView mTime;
    private TextView mSteps;
    private TextView mSpeed;
    private TextView mMeters;
    private Button mStateButton;
    private Button mStateButton2;
    private Intent mPedometerService;

    private Timer mTimer;
    private TimerTask mTimerTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("HomeFragment", "=== onCreateView ===");

        View v = inflater.inflate(R.layout.home_fragment, container, false);

        // get steps per day
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        //mTodaySteps = sharedPref.getInt(getString(R.string.StepsPerDayPreference), 7000);

        String StepsPerDayPreference_str = sharedPref.getString("StepsPerDayPreference","7000");
        mTodaySteps = Integer.parseInt(StepsPerDayPreference_str);
        Log.d("HomeFragment ","mTodaySteps = " + mTodaySteps);



        // get step width

        //mStepWidth = sharedPref.getInt(getString(R.string.StepWidthPreference), 83) / 100f;
        //Log.d("HomeFragment ","mStepWidth = " + mStepWidth);


        String StepWidthPreference_str = sharedPref.getString("StepWidthPreference","83");
        mStepWidth = Float.parseFloat(StepWidthPreference_str) / 100f;
        Log.d("HomeFragment", "mStepWidth = " + mStepWidth);


        mCalories  = (TextView) v.findViewById(R.id.calories);
        mSteps  = (TextView) v.findViewById(R.id.steps);
        mMeters = (TextView) v.findViewById(R.id.meters);
        mTime   = (TextView) v.findViewById(R.id.time);
        mSpeed  = (TextView) v.findViewById(R.id.speed);

        mCalories.setTextColor(Color.BLUE);
        mMeters.setTextColor(Color.RED);
        mSteps.setTextColor(Color.RED);
        mSpeed.setTextColor(Color.YELLOW);
        mTime.setTextColor(Color.YELLOW);
        mMeters.setText("0" + getContext().getResources().
                getString(R.string.History_Meters_Abbreviation));
        mSteps.setText("0 " + getResources().getString(R.string.steps));
        mSpeed.setText("0 " + getContext().getResources().
                getString(R.string.History_Speed_Abbreviation));

        mStateButton = (Button) v.findViewById(R.id.state_button);
        mStateButton2 = (Button) v.findViewById(R.id.state_button2);


/*
        if (PedometerService.isListenAccelerometer() == false) {
            mStateButton.setText(R.string.START);
            mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.red_foot, 0, 0, 0);
        } else {
            mStateButton.setText(R.string.STOP);
            mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.green_foot, 0, 0, 0);
        }
*/

        mStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                if (getResources().getString(R.string.START).equals(mStateButton.getText())) {
                    mStateButton.setText(R.string.STOP);
                    mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.green_foot, 0, 0, 0);
                    SendIntentToService(PedometerService.ACCELEROMETER_ON);
                    sharedPref.edit().putBoolean("ServiceOn", true).commit();

                } else {
                    mStateButton.setText(R.string.START);
                    mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.red_foot, 0, 0, 0);
                    SendIntentToService(PedometerService.ACCELEROMETER_OFF);
                    sharedPref.edit().putBoolean("ServiceOn", false).commit();
                }
            }
        });

        mStateButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mListener.OnHomeFragmentDataListener("ssssss");

            }
        });

        mGraph = (LineChart) v.findViewById(R.id.graph);

        GraphUpdate();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("HomeFragment", "onResume()");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (sharedPref.getBoolean("ServiceOn", false) == false){
            Log.d("HomeFragment", "=====//// ServiceOn ////====");
            mStateButton.setText(R.string.START);
            mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.red_foot, 0, 0, 0);
            if (PedometerService.isListenAccelerometer() == true) { SendIntentToService(PedometerService.ACCELEROMETER_OFF);}
        } else {
            mStateButton.setText(R.string.STOP);
            mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.green_foot, 0, 0, 0);
            if (PedometerService.isListenAccelerometer() == false) { SendIntentToService(PedometerService.ACCELEROMETER_ON);}
        }


        //Log.d("HomeFragment", "onCreateView()");
        mTimer = new Timer(); // Create timer
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                Handler refresh = new Handler(Looper.getMainLooper());
                refresh.post(new Runnable() {
                    public void run()
                    {
                        UpdateScreenData();
                    }
                });
            }
        };
        mTimer.schedule(mTimerTask, 10, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("HomeFragment", "onPause()");
        // Stop timer
        mTimer.cancel();
        mTimer.purge();
    }

    // convert int to string with space between the thousands
    public static String IntToString(int value) {
        String meter_text = String.valueOf(value % 1000);
        if ((value / 1000) != 0) {
            while (meter_text.length() < 3) {
                meter_text = "0" + meter_text;
            }
        }
        if ((value / 1000) != 0) {
            meter_text = String.valueOf(value / 1000) + " " + meter_text;
        }
        return meter_text;
    }

    // convert time in second to string HH:MM:SS
    public static String TimeToString(int seconds) {
        String time = "";
        if ((seconds / 3600) < 10) {
            time += "0";
        }
        time += String.valueOf(seconds / 3600) + ":";
        int Second = seconds % 60;
        int minutes = (seconds % 3600) / 60;
        if (minutes < 10) {
            time += "0";
        }
        time += String.valueOf(minutes) + ":";
        if (Second < 10) {
            time += "0";
        }
        time += String.valueOf(Second);
        return time;
    }

    // Create new intent and send it to service for transmitting in service new value of
    // accelerometer listener (on or off)
    private void SendIntentToService(int lister_state) {
        mPedometerService = new Intent(getActivity(), PedometerService.class);
        mPedometerService.putExtra(PedometerService.ACCELEROMETER_KEY, lister_state);
        getActivity().startService(mPedometerService);
    }

    // Update (if it is need) data on screen
    private void UpdateScreenData() {
        if (mCurrentSteps != PedometerService.getSteps()) {
            mCurrentCalories = (int)PedometerService.getCalories();
            mCurrentSteps = PedometerService.getSteps();
            mCurrentMeters = (int)PedometerService.getMeters();

            int time = PedometerService.getTotalTime();
           // int mCurrentMeters = Math.round(mCurrentSteps * mStepWidth);
            int speed = 0;
            if (time != 0) {
                speed = (int)(((float)mCurrentMeters / (float)time) * 360f);
            }

            mCalories.setText(IntToString(mCurrentCalories) + " " + getResources().getString(R.string.calories));
            mSteps.setText(IntToString(mCurrentSteps) + " " + getResources().getString(R.string.steps));
            mMeters.setText(IntToString(mCurrentMeters) + getContext().getResources().
                    getString(R.string.History_Meters_Abbreviation));
            mTime.setText(TimeToString(time));
            mSpeed.setText(String.valueOf(((float)speed / 100f)) + " " + getContext().getResources().
                    getString(R.string.History_Speed_Abbreviation));
            if (mCurrentMeters >= mTodaySteps) {
                mMeters.setTextColor(Color.GREEN);
            } else if (mCurrentMeters >= (mTodaySteps - 1000)) {
                mMeters.setTextColor(Color.YELLOW);
            } else {
                mMeters.setTextColor(Color.RED);
            }
        }
    }

    // create graph with today's steps by hours
    private void GraphUpdate() {
        // read saved data
        Date date = new Date();
        int[] mY = PedometerSharedPreferences.ReadStepsAndTime(getContext(), String.valueOf(date.getDate())
                + String.valueOf(date.getMonth()) + String.valueOf(date.getYear() + 1900));

        if ((PedometerService.getSteps() != mY[date.getHours()]) && (PedometerService.getSteps() != 0)) {
            mY[date.getHours()] = PedometerService.getSteps();
        }
        for (int i = 0; i < mY.length-2; i++) {
            mY[i] = (int)(mY[i] * mStepWidth);
        }

        // set some settings for graph
        mGraph.getLegend().setEnabled(false); // disable legend
        final Description description = new Description();
        description.setText("");
        mGraph.setDescription(description); // disable description text
        XAxis xAxis = mGraph.getXAxis();
        xAxis.setDrawGridLines(true);
        xAxis.setAxisMaximum(24);
        xAxis.setAxisMinimum(0);
        xAxis.setLabelCount(24);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        YAxis leftAxis = mGraph.getAxisLeft();
        int max = getMaxValue(mY);
        while ((max % 500) != 0) {
            max++;
        }
        if (max != 0) {
            leftAxis.setAxisMaximum(max);
        }
        leftAxis.setAxisMinimum(0);
        leftAxis.setLabelCount(5);
        leftAxis.setDrawGridLines(true);
        leftAxis.setTextColor(Color.WHITE);
        YAxis rightAxis = mGraph.getAxisRight();
        rightAxis.setDrawLabels(false);
        rightAxis.setDrawGridLines(false);
        mGraph.setDoubleTapToZoomEnabled(false);

        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float v, AxisBase axisBase) {
                if ((v % 2) == 0) {
                    return String.valueOf((int)v);
                }
                return "";
            }
        });

        // set data to graph
        int previous_value = mY[0];
        List<ILineDataSet> mDataSets = new ArrayList<ILineDataSet>();
        ArrayList<Entry> mEntries = new ArrayList<Entry>();
        for (int i = 0; i <= date.getHours(); i++) {
            if ((mY[i] == 0) || (i == 0)) {
                mEntries.add(new Entry(i, mY[i]));
            } else {
                mEntries.add(new Entry(i, mY[i] - previous_value));
                previous_value = mY[i];
            }
        }
        Collections.sort(mEntries, new EntryXComparator());
        LineDataSet mDataSet = new LineDataSet(mEntries, "Label");
        mDataSet.setDrawFilled(true);
        mDataSet.setDrawCircles(false);
        mDataSets.add(mDataSet);

        LineData lineData = new LineData(mDataSets);
        lineData.setDrawValues(false);
        lineData.setHighlightEnabled(false);
        mGraph.setData(lineData);
        mGraph.invalidate(); // refresh data
    }

    // getting the maximum value
    private int getMaxValue(int[] array) {
        int maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
            }
        }
        return maxValue;
    }

    public interface OnHomeFragmentDataListener {
        void OnHomeFragmentDataListener(String string);
    }

    private OnHomeFragmentDataListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnHomeFragmentDataListener) {
            mListener = (OnHomeFragmentDataListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragment1DataListener");
        }
    }
}