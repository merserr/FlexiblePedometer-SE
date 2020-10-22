package com.android.flexiblepedometer;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;

/**
 * Created by AREG on 03.03.2017.
 */

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_fragment);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("SensitivityPreference")) {
            //Log.d("SettingsFragment", key + "=====SensitivityPreference======");
            //float senspref = sharedPreferences.getFloat(key, 5f);
            //Log.d("SettingsFragment", key + senspref);

            String SensitivityPreference_str = sharedPreferences.getString("SensitivityPreference","15");
            Log.d("SettingsFragment", key + " __ "+ SensitivityPreference_str);
            float SensitivityPreference_float = Float.parseFloat(SensitivityPreference_str);
            Log.d("SettingsFragment", key + " __ "+ SensitivityPreference_float);
            PedometerAccelerometer.setSensitivity(SensitivityPreference_float);
            //PedometerAccelerometer.setSensitivity(sharedPreferences.getFloat(key, 5f));

        } else if (key.equals("StepWidthPreference")) {
    //        Log.d("SettingsFragment", key + "=====StepWidthPreference======");

            String regular = sharedPreferences.getString("StepWidthPreference","83");
    //        Log.d("SettingsFragment  =  ", key + regular);
            int ttt = Integer.parseInt(regular);
    //        Log.d("SettingsFragment = ", key + ttt);

            //        PedometerService.setStepWidth((float)sharedPreferences.getInt(key, 80) / 100f, getActivity());
            //PedometerService.setStepWidth((float)sharedPreferences.getInt(key, 80) / 100f, getActivity());
        //    PedometerService.setStepWidth((float)sharedPreferences.getInt(key, 80) / 100f, getActivity());

        } else if (key.equals("StepsPerDayPreference")) {
     //       Log.d("SettingsFragment", key + "=====StepsPerDayPreference======");
            String StepsPerDayPreference_str = sharedPreferences.getString("StepsPerDayPreference","7000");
            int StepsPerDayPreference_int = Integer.parseInt(StepsPerDayPreference_str);
    //        Log.d("SettingsFragment", key + " __ "+ StepsPerDayPreference_int);

            //        PedometerService.setStepWidth((float)sharedPreferences.getInt(key, 80) / 100f, getActivity());
        }else if (key.equals("BeepCheckBox")) {
                   Log.d("SettingsFragment", key + sharedPreferences.getBoolean("BeepCheckBox", false));

            //        Log.d("SettingsFragment", key + " __ "+ StepsPerDayPreference_int);

            //        PedometerService.setStepWidth((float)sharedPreferences.getInt(key, 80) / 100f, getActivity());
        }
    }
}
