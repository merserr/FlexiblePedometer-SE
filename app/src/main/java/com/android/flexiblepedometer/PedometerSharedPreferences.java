package com.android.flexiblepedometer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by AREG on 09.03.2017.
 */

public class PedometerSharedPreferences {

    // write current steps and time values
    public static void WriteCurrentStepsAndTime(Context context) {
        String mCurrentDate;
        JSONObject jsonObject = null;
        SharedPreferences mSharedPreferences;

        mSharedPreferences = context.getSharedPreferences(context.getResources().
                getString(R.string.SharedPreferences_MainKey), Context.MODE_PRIVATE);
        Date date = new Date();
        // key format day - month - year
        mCurrentDate = "";
        mCurrentDate += String.valueOf(date.getDate());
        mCurrentDate += String.valueOf(date.getMonth());
        mCurrentDate += String.valueOf(date.getYear() + 1900);

        // load data from service
        int calories = (int)PedometerService.getCalories();
        int steps = PedometerService.getSteps();
        int meters = (int)PedometerService.getMeters();
        int time = PedometerService.getTotalTime();

        if (mSharedPreferences.contains(mCurrentDate) == false) {
            // no preference for this day yet, reset data in service (new calculation)
            if (date.getHours() == 0) { // midnight, new day
                PedometerService.resetTime();
                PedometerService.resetSteps(context);
                time = 0;
                steps = 0;
                meters = 0;
                calories =0;
             //==


            }
        } else {
            // preference already present, read it and then write data to the end
            String string = mSharedPreferences.getString(mCurrentDate, null);
            try {
                jsonObject = new JSONObject(string);
            } catch (JSONException e) {
                e.getStackTrace();
            }
        }
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        try {
            jsonObject.put(String.valueOf(date.getHours()), steps);
        } catch (JSONException e) {
            e.getStackTrace();
        }
        if(meters != 0) {
            Log.d("PedometerPreferences", "saved jsonObject: " + jsonObject);
            Log.d("PedometerPreferences", "=======saved date:======= " + mCurrentDate);
            mSharedPreferences.edit().putString(mCurrentDate, jsonObject.toString()).commit();
            mSharedPreferences.edit().putInt("time", time).commit();
            mSharedPreferences.edit().putInt("meters", meters).commit();
            mSharedPreferences.edit().putInt("calories", calories).commit();
            Log.d("PedometerPreferences", "time: " + time);
            Log.d("PedometerPreferences", "meters: " + meters);
            Log.d("PedometerPreferences", "calories: " + calories);
        }
    }

    // read (and write, if it need) steps and time values for day
    public static int[] ReadStepsAndTime(Context context, String Date) {
        int[] stepsAndTime = new int[27];
        SharedPreferences mSharedPreferences;
        JSONObject jsonObject = new JSONObject();

        Log.d("PedometerPreferences", "read date: " + Date);
        mSharedPreferences = context.getSharedPreferences(context.getResources().
                getString(R.string.SharedPreferences_MainKey), Context.MODE_PRIVATE);
        if (mSharedPreferences.contains(Date) == false) {
            Log.d("PedometerPreferences", "no preference for this day yet, all is zero");
            for (int i = 0; i < 27; i++) {           //================================= 26
                // no preference for this day yet, all is zero
                stepsAndTime[i] = 0;
            }
        } else {
            String string = mSharedPreferences.getString(Date, null);
            Log.d("PedometerPreferences", "preference: " +string);
            try {
                jsonObject = new JSONObject(string);
            } catch (JSONException e) {
                e.getStackTrace();
            }
            // load steps
            for (int i = 0; i < 24; i++) {
                if (jsonObject.has(String.valueOf(i)) == true) {
                    try {
                        stepsAndTime[i] = jsonObject.getInt(String.valueOf(i));
                    } catch (JSONException e) {
                        e.getStackTrace();
                    }
                } else {
                    stepsAndTime[i] = 0;
                }
            }
            stepsAndTime[24] = mSharedPreferences.getInt("time", 0);
            stepsAndTime[25] = mSharedPreferences.getInt("meters", 0);
            stepsAndTime[26] = mSharedPreferences.getInt("calories", 0);
            Log.d("PedometerPreferences", "stepsAndTime[24]: " + stepsAndTime[24]);
            Log.d("PedometerPreferences", "stepsAndTime[25]: " + stepsAndTime[25]);
            Log.d("PedometerPreferences", "stepsAndTime[26]: " + stepsAndTime[26]);

        }
        return stepsAndTime;
    }

    // clear all preferences
    public static void ClearAllPreferences(Context context) {
        SharedPreferences mSharedPreferences = context.getSharedPreferences(context.getResources().
                getString(R.string.SharedPreferences_MainKey), Context.MODE_PRIVATE);
        mSharedPreferences.edit().clear().commit();
    }
}
