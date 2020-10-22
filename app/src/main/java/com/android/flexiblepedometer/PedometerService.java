package com.android.flexiblepedometer;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by AREG on 28.02.2017.
 */

public class PedometerService extends Service
        implements PedometerAccelerometer.PedometerAccelerometerListener{

    final String FILENAME = "log_file_pedometer";


    public final static String ACCELEROMETER_KEY = "accelerometer_key";
    public final static int ACCELEROMETER_OFF = 0;
    public final static int ACCELEROMETER_ON = 1;

    private final String POWER_TAG = "MY_POWER_TAG";
    private static final int PEDOMETER_NOTIFICATION_ID = 1385;

    private PedometerAccelerometer mPedometerAccelerometer;

    private static boolean sListenAccelerometer;

    private long mLastSpeakTime;

    // preference settings
    private static float sStepWidth;
    private static boolean sBeepStepEnable;

    // variables for pedometer
    private static float mCalories;
    private static int sSteps;
    private static long sTime;
    private static float sMeters;
    private static int Step2Count;
    private static long delta2;
    private static long s2StepTime;
    private static float sStepWidthMidi;
    private static float sStepWidthSum;
    float StepWidth = 0.83f;
    float StepWidthRatio;

    // variables for calorie calculator
    private static boolean mIsRunning = false; // if running must by true
    private static float mBodyWeight = 75;
    private static double METRIC_RUNNING_FACTOR = 1.02784823;
    private static double IMPERIAL_RUNNING_FACTOR = 0.75031498;

    private static double METRIC_WALKING_FACTOR = 0.708;
    private static double IMPERIAL_WALKING_FACTOR = 0.517;


    // variables for timer
    private static boolean sTimerEnable;
    private static boolean sTimerEnablemem;
    private static boolean sBeepEnable;
    private static int sTimerSteps;
    private static float sTimerMeters;
    private static int sTimerTimeStart;
    private static int sTimerTimeOffset;

    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmIntent;

    private static Notification mNotification;
    private static Notification.Builder mBuilder;

    private PowerManager.WakeLock mWakeLock;
    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("PedometerService", "=== onCreate ===");
        mCalories = 0;
        sTime = 0;
        sSteps = 0;
        sMeters = 0;
        s2StepTime = 970;
        sTimerMeters = 0;
        sTimerEnable = false;
        sTimerEnablemem = false;
        sBeepEnable = false;
        sTimerSteps = 0;
        sTimerTimeStart = 0;
        sTimerTimeOffset = 0;
        mLastSpeakTime = System.currentTimeMillis();
        sListenAccelerometer = false;
        mPedometerAccelerometer = new PedometerAccelerometer(PedometerService.this);
        mPedometerAccelerometer.PedometerAccelerometerListenerSet(this);

        // Schedule every hour
        mAlarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, EveryHourHandler.class);
        mAlarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + MillisecondsBeforeNextHour(),
                AlarmManager.INTERVAL_HOUR, mAlarmIntent);

        // not turn off CPU if accelerometer listener is not null
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, POWER_TAG);



        //===========================================================================================
        // get step width
        //===========================================================================================

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String StepWidthPreference_str = sharedPref.getString("StepWidthPreference","83");
        sStepWidth = Float.parseFloat(StepWidthPreference_str);
        StepWidthRatio = sStepWidth / 83f;
        Log.d("PedometerService", "StepWidthRatio: " + StepWidthRatio);

        sBeepStepEnable = sharedPref.getBoolean("BeepCheckBox", false);
        Log.d("PedometerService", "sBeepStepEnable: " + sBeepStepEnable);

        //Log.d("PedometerService", "StepWidthPreference_float: " + sStepWidth);
        //sStepWidth = sharedPref.getInt(getApplicationContext().getResources().getString(R.string.StepWidthPreference), 20);
        sStepWidth = sStepWidth / 100;
        //   Log.d("PedometerService", "sStepWidth = " + sStepWidth);
        // ==========================================================================================



        // ==========================================================================================
        //  send saved sensitivity setting to >> PedometerAccelerometer
        // ==========================================================================================

        String SensitivityPreference_str = sharedPref.getString("SensitivityPreference","15");
        //   Log.d("PedometerService", " __ "+ SensitivityPreference_str);
        float SensitivityPreference_float = Float.parseFloat(SensitivityPreference_str);
        //   Log.d("PedometerService", "SensitivityPreference_float: " + SensitivityPreference_float);
        PedometerAccelerometer.setSensitivity(SensitivityPreference_float);
        // ==========================================================================================




        // update today's time and steps from SharedPreferences
        Date date = new Date();
        int[] steps_time = PedometerSharedPreferences.ReadStepsAndTime(this, String.valueOf(date.getDate())
                + String.valueOf(date.getMonth()) + String.valueOf(date.getYear() + 1900));
        Log.d("PedometerService", "====steps_time======: " + Arrays.toString(steps_time));
        for (int i = 0; i < 24; i++) {
            if (steps_time[i] > sSteps) {
                sSteps = steps_time[i];
            }
        }
        sTime = steps_time[24] * 1000; // seconds to miliseconds
        sMeters = steps_time[25];
        mCalories = steps_time[26];
        Log.d("PedometerService", "sTime: " + String.valueOf(sTime));
        Log.d("PedometerService", "sSteps: " + String.valueOf(sSteps));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("PedometerService", "=== onStartCommand ===");
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (intent != null) {
            int val = intent.getIntExtra(ACCELEROMETER_KEY, 2);
            if (val == ACCELEROMETER_OFF) {
                vibrator.vibrate(80);
                mWakeLock.release();
                sListenAccelerometer = false;
                mPedometerAccelerometer.StopListen();
        //        PedometerSharedPreferences.WriteCurrentStepsAndTime(this); // store current steps and time ================================================
            }
            if (val == ACCELEROMETER_ON) {
                vibrator.vibrate(80);
                mWakeLock.acquire();
                sListenAccelerometer = true;
                mPedometerAccelerometer.StartListen();
            }
        }

        if (mBuilder == null) {
            mBuilder = new Notification.Builder(this);
        }
        getMyActivityNotification(this);
        startForeground(PEDOMETER_NOTIFICATION_ID, mNotification);

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("PedometerService", "===============onDestroy called===================");
        // Remove all our notifications
        NotificationManager mNotificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(PEDOMETER_NOTIFICATION_ID);
        // Stop listen data from accelerometer
        mPedometerAccelerometer.StopListen();
        // If the alarm has been set, cancel it.
        if (mAlarmManager!= null) {
            mAlarmManager.cancel(mAlarmIntent);
        }
        if (sSteps != 0) {
            Log.d("PedometerService", "saved sSteps");
          //  PedometerSharedPreferences.WriteCurrentStepsAndTime(this); // store current steps and time
        }

        if ((mWakeLock != null) && (mWakeLock.isHeld())) {
            mWakeLock.release();
        }
    }

    // Listener, calls when step has been done
    public void StepHasBeenDone() {
        if(sTimerEnable&&(sTimerEnablemem)==false){
            //writeFile("=== Start ===");
            //Log.d("PedometerService", "=== Start ===");
        }
      //  if(sTimerEnablemem&&!sTimerEnable){writeFile("=== Stop ===");}
        sTimerEnablemem = sTimerEnable;

        //Log.d("StepHasBeenDone", "StepHasBeenDone()");
        // increase steps
        sSteps++;
        // int meters = Math.round(sSteps * sStepWidth);
        // check time
        long now = System.currentTimeMillis();
        long delta = now - mLastSpeakTime;
        mLastSpeakTime = now;

        if (delta / 1000 <= 3) { // less than 5 seconds
            sTime += delta;


//----------------------------------------------------------------------------------------------
            //define StepWidth
            if (Step2Count < 1) {
                delta2 = delta;
                Step2Count++;
            } else {
                Step2Count=0;
                delta2 = delta2 + delta;
       //         Log.d("PedometerService", "delta2=  " + delta2);

                s2StepTime = (s2StepTime * 19 + delta2) / 20;


                if(s2StepTime <= 921) {StepWidth = 0.934f;}
                if(s2StepTime > 925) {StepWidth = 0.928f;}
                if(s2StepTime > 930) {StepWidth = 0.922f;}
                if(s2StepTime > 935) {StepWidth = 0.916f;}
                if(s2StepTime > 940) {StepWidth = 0.910f;}
                if(s2StepTime > 945) {StepWidth = 0.904f;}
                if(s2StepTime > 950) {StepWidth = 0.898f;}
                if(s2StepTime > 955) {StepWidth = 0.892f;}
                if(s2StepTime > 960) {StepWidth = 0.886f;}
                if(s2StepTime > 965) {StepWidth = 0.880f;}
                if(s2StepTime > 970) {StepWidth = 0.874f;}
                if(s2StepTime > 975) {StepWidth = 0.868f;}
                if(s2StepTime > 980) {StepWidth = 0.862f;}
                if(s2StepTime > 985) {StepWidth = 0.856f;}
                if(s2StepTime > 990) {StepWidth = 0.850f;}
                if(s2StepTime > 995) {StepWidth = 0.844f;}
                if(s2StepTime > 1000) {StepWidth = 0.838f;}
                if(s2StepTime > 1005) {StepWidth = 0.832f;}
                if(s2StepTime > 1010) {StepWidth = 0.826f;}
                if(s2StepTime > 1015) {StepWidth = 0.820f;}
                if(s2StepTime > 1020) {StepWidth = 0.814f;}
                if(s2StepTime > 1025) {StepWidth = 0.808f;}
                if(s2StepTime > 1030) {StepWidth = 0.802f;}
                if(s2StepTime > 1035) {StepWidth = 0.796f;}
                if(s2StepTime > 1040) {StepWidth = 0.790f;}
                if(s2StepTime > 1045) {StepWidth = 0.784f;}
                if(s2StepTime > 1050) {StepWidth = 0.778f;}
                if(s2StepTime > 1055) {StepWidth = 0.772f;}
                if(s2StepTime > 1060) {StepWidth = 0.766f;}
                if(s2StepTime > 1065) {StepWidth = 0.760f;}
                if(s2StepTime > 1070) {StepWidth = 0.754f;}
                if(s2StepTime > 1075) {StepWidth = 0.748f;}
                if(s2StepTime > 1080) {StepWidth = 0.742f;}
                if(s2StepTime > 1085) {StepWidth = 0.736f;}
                if(s2StepTime > 1090) {StepWidth = 0.734f;}
                if(s2StepTime > 1095) {StepWidth = 0.732f;}
                if(s2StepTime > 1100) {StepWidth = 0.731f;}
                if(s2StepTime > 1110) {StepWidth = 0.730f;}
                //if(s2StepTime > 1120) {StepWidth = 0.694f;}
                //if(s2StepTime > 1130) {StepWidth = 0.682f;}
                //if(s2StepTime > 1140) {StepWidth = 0.672f;}
                //if(s2StepTime > 1150) {StepWidth = 0.663f;}
                //if(s2StepTime > 1160) {StepWidth = 0.657f;}
                //if(s2StepTime > 1170) {StepWidth = 0.652f;}
                //if(s2StepTime > 1180) {StepWidth = 0.647f;}
                //if(s2StepTime > 1190) {StepWidth = 0.642f;}
                //if(s2StepTime > 1200) {StepWidth = 0.638f;}
                //if(s2StepTime > 1220) {StepWidth = 0.631f;}
                //if(s2StepTime > 1240) {StepWidth = 0.626f;}
                //if(s2StepTime > 1260) {StepWidth = 0.620f;}
                //if(s2StepTime > 1280) {StepWidth = 0.615f;}
                //if(s2StepTime > 1300) {StepWidth = 0.610f;}

                // StepWidth = 2.104f - 0.001f * s2StepTime;


                //s2StepTime = delta2;
                sStepWidth = (sStepWidth * 19 + StepWidth) / 20;
                Log.d("PedometerService", "===== sStepWidth ======  " + sStepWidth);

                // prepare data
                String s2StepTime_str = String.valueOf(s2StepTime);
                String delta2_str = String.valueOf(delta2);

                if (s2StepTime_str.length() == 3) {s2StepTime_str= "0"+ s2StepTime_str;}
                if (delta2_str.length() == 3) {delta2_str= "0"+ delta2_str;}
                String sending_data = (String.valueOf(s2StepTime_str + "  ( " + delta2_str + " )  " + String.format("%.1f", sStepWidth * 100)));

                // record data
                if(sTimerEnable) {writeFile(sending_data);}
            }



          //  s2StepTime = delta2;
          //  Log.d("PedometerService", "s2StepTime=  " + s2StepTime);
//----------------------------------------------------------------------------------------------
            sMeters = sMeters + sStepWidth;

            mCalories += (mBodyWeight * (mIsRunning ? METRIC_RUNNING_FACTOR : METRIC_WALKING_FACTOR))
                    // Distance:
                    * sStepWidth // stepmeters
                    / 1000.0; // centimeters/kilometer
            sStepWidthSum = sStepWidthSum + sStepWidth;
            sStepWidthMidi = sStepWidthSum / sSteps;
            Log.d("PedometerService", "-----------sStepWidthMidi = " + sStepWidthMidi);
        }



        // timer increase steps
        if (sTimerEnable == true) {
            sTimerSteps++;
            sTimerMeters = sTimerMeters + sStepWidth;

            sStepWidthSum = sStepWidthSum + sStepWidth;
            sStepWidthMidi = sStepWidthSum / sSteps / 2f;
            Log.d("PedometerService", "-----------sStepWidthMidi = " + sStepWidthMidi);
            //Log.d("PedometerService", "-----------sTimerMeters = " + sTimerMeters);
            //    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            //    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); // 200 is duration in ms
        }
        if (sBeepEnable == true) {
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); // 200 is duration in ms
        }
        //if (sMeters != meters) {
        //    sMeters = meters;
            UpdateNotification(this);
        //}
    }

    // return current pedometer state, True - pedometer is on, False - pedometer is off
    public static boolean isListenAccelerometer() {
        return sListenAccelerometer;
    }

    // return steps with
    public static float get2StepTime() {
        return s2StepTime;
    }
    public static float getStepWidth() {
        return sStepWidth;
    }
    public static float getStepWidthMidi() { return sStepWidthMidi; }

    // set steps with
    public static void setStepWidth(float stepWidth, Context context) {
        sStepWidth = stepWidth;
        Log.d("123", "sStepWidth: " + String.valueOf(sStepWidth));
        UpdateNotification(context);
    }
    // return the distance today
    public static float getCalories() {
        return mCalories;
    }
    // return the number of steps today
    public static int getSteps() {
        return sSteps;
    }
    // return the distance today
    public static float getMeters() {
        return sMeters;
    }

    // return the today's total time
    public static int getTotalTime() {
        Long result = sTime/(long)1000;
        return result.intValue();
    }

    // reset steps (if it is new day)
    public static void resetSteps(Context context) {
        sSteps = 0;
        sMeters = 0;
        mCalories = 0;
        sStepWidthMidi = 0;
        sStepWidthSum = 0;
        UpdateNotification(context);
    }

    // reset time (if it is new day)
    public static void resetTime() {
        sTime = 0;
    }

    // -----------------------------------------------
    // ------------- START TIMER SECTION -------------
    // -----------------------------------------------
    public static boolean isTimerEnable() {
        //   buzz();
        return sTimerEnable;
    }
    public void buzz() {
        //   Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //   vibrator.vibrate(80);

    }
    public static boolean isBeepEnable() {
        return sBeepEnable;
    }

    public static void setTimerEnable(boolean timerEnable) {
        sTimerEnable = timerEnable;
     //   String str = "";
        if(timerEnable){sTimerEnablemem = false;}

     //   PedometerService pese = new  PedometerService();
     //   pese.GowriteFile();
     //   Log.d("PedometerService", "=== XXXX ===");

    }
    public static void setBeepEnable(boolean beepEnable) {
        sBeepEnable = beepEnable;
    }

    public static int getTimerSteps() {
        return sTimerSteps;
    }

    public static float getTimerMeters() {
        return sTimerMeters;
    }
    public static void setTimerSteps(int timerSteps) {
        sTimerSteps = timerSteps;
    }
    public static void setTimerMeters(int timerMeters) { sTimerMeters = timerMeters; }
    public static void setTimerStepsWidth(int timerMeters) {
        sStepWidth = timerMeters;
        sStepWidthMidi = timerMeters;
    }
    public static int getTimerTimeOffset() {
        return sTimerTimeOffset;
    }

    public static void setTimerTimeOffset(int timerTimeOffset) {
        sTimerTimeOffset = timerTimeOffset;
    }

    public static int getTimerTimeStart() {
        return sTimerTimeStart;
    }

    public static void setTimerTimeStart(int timerTimeStart) {
        sTimerTimeStart = timerTimeStart;
    }
    // -----------------------------------------------
    // -------------- END TIMER SECTION --------------
    // -----------------------------------------------

    // prepare notification
    private static void getMyActivityNotification(Context context) {
        // Convert steps to meters
        int meters = Math.round(sMeters);
        // prepare data
        String text = HomeFragment.IntToString(meters);
        // create intent for updating service view
        Intent notificationIntent = new Intent(context, PedometerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0); // PendingIntent.FLAG_UPDATE_CURRENT

        // Set current distance in meters
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.pedometer_service);
        remoteViews.setTextViewText(R.id.distance, text + "m");
        // Set image view (on or off pedometer)

        if (sListenAccelerometer == false) {
            remoteViews.setImageViewResource(R.id.state, R.mipmap.red_foot);
        } else {
            remoteViews.setImageViewResource(R.id.state, R.mipmap.green_foot);
        }

        // set receiver for ImageView
        Intent buttonStateIntent = new Intent(context, NotificationStateButtonHandler.class);
        buttonStateIntent.putExtra("action", "close");

        PendingIntent buttonClosePendingIntent = pendingIntent.getBroadcast(context, 0, buttonStateIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.state, buttonClosePendingIntent);

        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);

        if (mNotification == null) {
            mNotification = mBuilder.build();
        }
        mNotification.contentView = remoteViews;
    }

    // update notification
    private static void UpdateNotification(Context context) {
        getMyActivityNotification(context);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(PEDOMETER_NOTIFICATION_ID, mNotification);
    }

    // return milliseconds before next hour
    private int MillisecondsBeforeNextHour() {
        Date date=new Date();
        return ((60 - date.getMinutes()) * 60 * 1000);
    }

    // write to file
    void writeFile(String sending_data) {
        try {
            // отрываем поток для записи
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
            openFileOutput(FILENAME, MODE_APPEND)));

            // record data
            bw.write(sending_data);
            bw.write("\r\n");

            // закрываем поток
            bw.close();
            //Log.d("PedometerService", "Файл записан");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
