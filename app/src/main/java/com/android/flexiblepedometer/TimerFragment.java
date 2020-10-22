package com.android.flexiblepedometer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by AREG on 02.03.2017.
 */

public class TimerFragment extends Fragment {

    private TextView mTimerSteps;
    private TextView mTimerSpeed;
    private TextView mTimerDistance;
    private TextView mTimerTotalTime;
    private TextView mTimerstepwidth;
    private TextView mTimer2stepTime;
    private Button mTimerButton;
    private Button mStateButton;
    private Intent mPedometerService;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private float mStepWidth;
    private float mStepWidthMidi;
    private float mStepTime;
    private float meters;
    private int distance;
    private int steps;
    private static boolean sBeepStepEnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.timer_fragment, container, false);

        // get step width
        mStepWidth = PedometerService.getStepWidth();
        mStepWidthMidi = PedometerService.getStepWidthMidi();
        mStepTime = PedometerService.get2StepTime();

        mTimerSpeed = (TextView) v.findViewById(R.id.total_speed);
        mTimerSteps = (TextView) v.findViewById(R.id.total_steps);
        mTimerDistance = (TextView) v.findViewById(R.id.total_distance);
        mTimerTotalTime = (TextView) v.findViewById(R.id.total_time);
        mTimerstepwidth = (TextView) v.findViewById(R.id.stepwidth);
        mTimer2stepTime = (TextView) v.findViewById(R.id.steptime);


        mTimerSteps.setTextColor(Color.RED);
        mTimerDistance.setTextColor(Color.RED);
        if (PedometerService.isTimerEnable() == true) {
            mTimerTotalTime.setTextColor(Color.GREEN);
        }else{
            mTimerTotalTime.setTextColor(Color.BLUE);
        }
        mTimerSpeed.setTextColor(Color.RED);

        mStateButton = (Button) v.findViewById(R.id.state_button);
        mTimerButton = (Button) v.findViewById(R.id.start_pause_stop);

        final Context ctx = (Context)TimerFragment.this.getActivity();

        steps = PedometerService.getTimerSteps();
        meters = PedometerService.getTimerMeters();

        int offsetTime = PedometerService.getTimerTimeOffset();
        //int distance = Math.round(steps * mStepWidth);
        int distance = Math.round(meters);

        mTimerSteps.setText(HomeFragment.IntToString(steps));
        mTimerDistance.setText(HomeFragment.IntToString(distance) + getContext().getResources().
                getString(R.string.History_Meters_Abbreviation));
        mTimerSpeed.setText("0 " + getContext().getResources().getString(R.string.History_Speed_Abbreviation));

        if (PedometerService.isListenAccelerometer() == false) {
            mStateButton.setText(R.string.START);
            mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.red_foot, 0, 0, 0);
        } else {
            mStateButton.setText(R.string.STOP);
            mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.green_foot, 0, 0, 0);
        }
        mStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getResources().getString(R.string.START).equals(mStateButton.getText())) {
                    mStateButton.setText(R.string.STOP);
                    mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.green_foot, 0, 0, 0);
                    SendIntentToService(PedometerService.ACCELEROMETER_ON);
                } else {
                    mStateButton.setText(R.string.START);
                    mStateButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.red_foot, 0, 0, 0);
                    SendIntentToService(PedometerService.ACCELEROMETER_OFF);
                }
            }
        });


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        sBeepStepEnable = sharedPref.getBoolean("BeepCheckBox", false);
        Log.d("TimerFragment", "sBeepStepEnable: " + sBeepStepEnable);




        InitButtonLogic(offsetTime);

        mTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PedometerService.isTimerEnable() == false) { // turn on
                    if (sBeepStepEnable == true) {               // beep on
                        PedometerService.setBeepEnable(true);

                    } else {                                     // beep off
                        PedometerService.setBeepEnable(false);

                    }
                    mTimerTotalTime.setTextColor(Color.GREEN);
                    PedometerService.setTimerEnable(true);
                    InitButtonLogic(PedometerService.getTimerTimeOffset());
                    PedometerService.setTimerTimeStart((int)(System.currentTimeMillis() / 1000));

                    Intent intent = new Intent(PedometerActivity.BROADCAST_ACTION);
                    intent.putExtra("callrequest", "start");
                    //String sndstring = String.valueOf(steps) + "  " + String.format("%.0f",meters) + "m  (" + String.format("%.2f", mStepWidth)+ ")";
                    // Log.d ("sssss", sndstring);
                    //intent.putExtra("sndstring", sndstring) ;
                    ctx.sendBroadcast(intent);

                } else { // turn off
                    mTimerTotalTime.setTextColor(Color.BLUE);
                    PedometerService.setTimerEnable(false);
                    PedometerService.setBeepEnable(false);
                    // change offset time
                    int time = (int)(System.currentTimeMillis() / 1000);
                    time -= PedometerService.getTimerTimeStart();
                    PedometerService.setTimerTimeOffset(PedometerService.getTimerTimeOffset() + time);
                    InitButtonLogic(PedometerService.getTimerTimeOffset());

                    Intent intent = new Intent(PedometerActivity.BROADCAST_ACTION);
                    intent.putExtra("callrequest", "stop");
                    String sndstring = String.valueOf(steps) + "  " + String.format("%.0f",meters) + "m  (" + String.format("%.1f", (float)meters / (float)steps * 100)+ ")";
                   // Log.d ("sssss", sndstring);
                    intent.putExtra("sndstring", sndstring) ;
                    ctx.sendBroadcast(intent);

                }
            }
        });

        mTimerButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (PedometerService.isTimerEnable() == false) {
                    PedometerService.setTimerSteps(0);
                    PedometerService.setTimerMeters(0);
                    //PedometerService.setTimerStepsWidth(0);
                    PedometerService.setTimerTimeOffset(0);
                    InitButtonLogic(PedometerService.getTimerTimeOffset());
                    mTimerSpeed.setText("0.0 " + getContext().getResources().
                            getString(R.string.History_Speed_Abbreviation));
                    mTimerSteps.setText("0");
                    mTimerDistance.setText("0" + getContext().getResources().
                            getString(R.string.History_Meters_Abbreviation));
                    mTimerstepwidth.setText("0.0 " + " см");
                    mTimer2stepTime.setText("0.0 " + " см");

                    UpdateTime();
                }
                return true;
            }
        });

        UpdateTime();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer = new Timer(); // Create timer
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (PedometerService.isTimerEnable() == true) { // if timer is turn on
                    Handler refresh = new Handler(Looper.getMainLooper());
                    refresh.post(new Runnable() {
                        public void run()
                        {
                            UpdateTime();
                        }
                    });
                }
            }
        };
        mTimer.schedule(mTimerTask, 10, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop timer
        mTimer.cancel();
        mTimer.purge();
    }

    // init button
    private void InitButtonLogic(int offsetTime) {
        if (PedometerService.isTimerEnable() == false) {
            if (offsetTime == 0) {
                mTimerButton.setText(R.string.START_TIMER);
            } else {
                mTimerButton.setText(R.string.START_RESET_TIMER);
            }
            mTimerButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.start, 0, 0, 0);
        } else {
            mTimerButton.setText(R.string.STOP_TIMER);
            mTimerButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.stop, 0, 0, 0);
        }
    }

    // Create new intent and send it to service for transmitting in service new value of
    // accelerometer listener (on or off)
    private void SendIntentToService(int lister_state) {
        mPedometerService = new Intent(getActivity(), PedometerService.class);
        mPedometerService.putExtra(PedometerService.ACCELEROMETER_KEY, lister_state);
        getActivity().startService(mPedometerService);
    }


    // update time on screen
    private void UpdateTime() {
        int offsetTime = PedometerService.getTimerTimeOffset();
        int startTime = PedometerService.getTimerTimeStart();
        steps = PedometerService.getTimerSteps();
        //int distance = Math.round(steps * mStepWidth);
        meters = PedometerService.getTimerMeters();
        distance = Math.round(meters);
    //    Log.d("TimerFragment", "-----------sTimerMeters = " + meters);
        mStepWidth = PedometerService.getStepWidth();
        mStepWidthMidi = PedometerService.getStepWidthMidi();
        mStepTime = PedometerService.get2StepTime();
    //    mStepWidth = 0.83f;
        int speed = 0;
        int total_time = 0;
        if (PedometerService.isTimerEnable() == true) { // if timer is turn on
            total_time = (int)(System.currentTimeMillis() / 1000) - startTime;
        }
        total_time += offsetTime;
        mTimerTotalTime.setText(HomeFragment.TimeToString(total_time));
        // update speed
        if (total_time != 0) {
            speed = (int)(((float)distance / (float)total_time) * 360f);
            mTimerSpeed.setText(String.valueOf(((float)speed / 100f)) + " " + getContext().getResources().getString(R.string.History_Speed_Abbreviation));
            mTimerSteps.setText(HomeFragment.IntToString(steps));
            mTimerDistance.setText(HomeFragment.IntToString(distance) + getContext().getResources().getString(R.string.History_Meters_Abbreviation));

            String StepWidthOut = String.format("%.1f", mStepWidth * 100);
            if(steps != 0){
                String StepWidthMidiOut = String.format("%.1f", (float)meters / (float)steps * 100);
                String StepTimeOut = String.format("%.0f", mStepTime);
                mTimerstepwidth.setText(StepWidthOut + " см");
                mTimer2stepTime.setText(StepWidthMidiOut + " см");
            }
        }
    }
}
