package com.android.flexiblepedometer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by AREG on 03.03.2017.
 */

public class NotificationStateButtonHandler extends BroadcastReceiver {

    private Intent mPedometerService;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Change pedometer state (on to off, or off to on)
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        if (PedometerService.isListenAccelerometer() == false) {
            SendIntentToService(context, PedometerService.ACCELEROMETER_ON);
            sharedPref.edit().putBoolean("ServiceOn", true).commit();
        } else {
            SendIntentToService(context, PedometerService.ACCELEROMETER_OFF);
            sharedPref.edit().putBoolean("ServiceOn", false).commit();
        }
    }

    // Create new intent and send it to service for transmitting in service new value of
    // accelerometer listener (on or off)
    private void SendIntentToService(Context context, int lister_state) {
        mPedometerService = new Intent(context, PedometerService.class);
        mPedometerService.putExtra(PedometerService.ACCELEROMETER_KEY, lister_state);
        context.startService(mPedometerService);
    }
}
