package com.android.flexiblepedometer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Date;

public class PedometerActivity extends ActionBarActivity implements HomeFragment.OnHomeFragmentDataListener {

    final String FILENAME = "log_file_pedometer";

    public static final int HOME_FRAGMENT = 1;
    private final int TIMER_FRAGMENT = 2;
    private final int HISTORY_FRAGMENT = 3;
    private final int SETTINGS_FRAGMENT = 4;
    private final int LOG_FRAGMENT = 5;

    private Intent mPedometerService;

    private Toolbar mToolbar;
    private AlertDialog mAlert;

    private Drawer.Result mNavigationDrawer;
    private Fragment mFragment;

    private int mCurrentFragment;
    public final static String BROADCAST_ACTION = "com.android.flexiblepedometer";
    BroadcastReceiver br;

    @Override
    public void OnHomeFragmentDataListener(String string) {
    //    Log.d("PedometerActivity","onFragment1DataListener called");
        mCurrentFragment = TIMER_FRAGMENT;
        DisableDrawerIndicator();
        ShowCurrentFragment();

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("PedometerActivity", "=== onCreate ===");


        setContentView(R.layout.activity_pedometer);

        mCurrentFragment = HOME_FRAGMENT;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // init ToolBar for navigation drawer
        // Handle Toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init Navigation Drawer

        mNavigationDrawer = new Drawer()
                .withActivity(this)
                .withToolbar(mToolbar)
                .withActionBarDrawerToggle(true)
                .withHeader(R.layout.drawer_header)
                .withFooter(R.layout.drawer_footer)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.drawer_item_home).withIcon(FontAwesome.Icon.faw_home).withIdentifier(1),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_measuring).withIcon(FontAwesome.Icon.faw_clock_o),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_history).withIcon(FontAwesome.Icon.faw_calendar).withIdentifier(2),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_delete).withIcon(FontAwesome.Icon.faw_trash_o),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_settings).withIcon(FontAwesome.Icon.faw_cog),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_log).withIcon(FontAwesome.Icon.faw_archive),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_exit).withIcon(FontAwesome.Icon.faw_sign_out)
                        )
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        InputMethodManager inputMethodManager = (InputMethodManager) PedometerActivity.this.getSystemService(PedometerActivity.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(PedometerActivity.this.getCurrentFocus().getWindowToken(), 0);
                    }
                    @Override
                    public void onDrawerClosed(View drawerView) {
                    }
                })
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    // Обработка клика
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        //Toast.makeText(PedometerActivity.this, String.valueOf(position), Toast.LENGTH_SHORT).show();
                        switch (position) {
                            case 1: // home
                                mCurrentFragment = HOME_FRAGMENT;
                                EnableDrawerIndicator();
                                ShowCurrentFragment();
                                break;
                            case 3: // timer
                                mCurrentFragment = TIMER_FRAGMENT;
                                DisableDrawerIndicator();
                                ShowCurrentFragment();
                                break;
                            case 5: // history
                                mCurrentFragment = HISTORY_FRAGMENT;
                                DisableDrawerIndicator();
                                ShowCurrentFragment();
                                break;
                            case 7: // delete history
                                ShowRemoveHistoryDialogWindow(); // confirmation dialog
                                break;
                            case 9: // settings
                                mCurrentFragment = SETTINGS_FRAGMENT;
                                DisableDrawerIndicator();
                                ShowCurrentFragment();
                                break;
                            case 11: // log
                                mCurrentFragment = LOG_FRAGMENT;
                                DisableDrawerIndicator();
                                ShowCurrentFragment();
                                break;
                            case 13: // exit
                                stopService(mPedometerService);
                                finish();
                                break;
                           }
                    }
                })
                .build();

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //What to do on back clicked
                //Log.d("123","setNavigationOnClickListener() called");
                if (mCurrentFragment == HOME_FRAGMENT) {
                    mNavigationDrawer.openDrawer();
                } else {
                    mCurrentFragment = HOME_FRAGMENT;
                    EnableDrawerIndicator();
                    ShowCurrentFragment();
                }
            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (sharedPref.contains(getApplicationContext().
                getResources().getString(R.string.StepWidthPreference)) == false) {
            sharedPref.edit().putInt(getApplicationContext().
                    getResources().getString(R.string.StepWidthPreference), 83).commit();
        }
        if (sharedPref.contains(getApplicationContext().
                getResources().getString(R.string.SensitivityPreference)) == false) {
            sharedPref.edit().putFloat(getApplicationContext().
                    getResources().getString(R.string.SensitivityPreference), 15f).commit();
        }

        if (sharedPref.contains(getApplicationContext().
                getResources().getString(R.string.StepsPerDayPreference)) == false) {
            sharedPref.edit().putInt(getApplicationContext().
                    getResources().getString(R.string.StepsPerDayPreference), 7000).commit();
        }

        // start service

        mPedometerService = new Intent(PedometerActivity.this, PedometerService.class);
        startService(mPedometerService);
        Toast.makeText(this,"Не забудь стартануть!", Toast.LENGTH_LONG).show();

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);

        //==========================================================================

        //final Context ctx = (Context)PedometerActivity.this;
        br = new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {
                String callrequest = intent.getStringExtra("callrequest");
                String sendstring = intent.getStringExtra("sndstring");


                if(callrequest.equals("stop")){
                    //Log.d("PedometerActivity", "BroadcastReceiver onReceive = " + sendstring);
                    writeFile("----------------------------");
                    writeFile("Stop at:  " + GetTime());
                    writeFile(sendstring);
                    writeFile("----------------------------");
                    //Log.d("PedometerActivity", "BroadcastReceiver GetTime() = " + GetTime());
                }
                if(callrequest.equals("start")){
                    //Log.d("PedometerActivity", "BroadcastReceiver onReceive = " + sendstring);
                    writeFile("----------------------------");
                    writeFile("Start at:  " + GetTime());
                    writeFile("----------------------------");
                }


            }
        };  // =================  End BroadcastReceiver ============================

        //  BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, intFilt);
        //==========================================================================
}

    protected void onStart() {
        super.onStart();
        Log.d("PedometerActivity", "=== onStart ===");
        /*

        // start service
        mPedometerService = new Intent(PedometerActivity.this, PedometerService.class);
        startService(mPedometerService);
        Toast.makeText(this,"Не забудь стартануть!", Toast.LENGTH_LONG).show();

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);

        //==========================================================================
*/

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("PedometerActivity", "=== onResume ===");
        ShowCurrentFragment();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("PedometerActivity", "=== onPause ===");
        PedometerSharedPreferences.WriteCurrentStepsAndTime(getApplicationContext());
        // hide dialog window if it is open
        if( mAlert != null && mAlert.isShowing() )
        {
            mAlert.dismiss();
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        Log.d("PedometerActivity", "=== onDestroy ===");
        // дерегистрируем (выключаем) BroadcastReceiver
        unregisterReceiver(br);
    }

    @Override
    public void onBackPressed(){
        if(mNavigationDrawer.isDrawerOpen()){
            mNavigationDrawer.closeDrawer();
        } else if (mCurrentFragment != HOME_FRAGMENT) {
            mCurrentFragment = HOME_FRAGMENT;
            ShowCurrentFragment();
            EnableDrawerIndicator();
        } else{
            super.onBackPressed();
        }
    }

    private void ShowCurrentFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (mCurrentFragment) {
            case HOME_FRAGMENT:
                mToolbar.setTitle(R.string.HomeTitle);
                mFragment = new HomeFragment();
                break;
            case TIMER_FRAGMENT:
                mToolbar.setTitle(R.string.MeasuringTitle);
                mFragment = new TimerFragment();
                break;
            case HISTORY_FRAGMENT:
                mToolbar.setTitle(R.string.HistoryTitle);
                mFragment = new HistoryMainFragment();
                break;
            case SETTINGS_FRAGMENT:
                mToolbar.setTitle(R.string.SettingsTitle);
                mFragment = new SettingsMainFragment();
                break;
            case LOG_FRAGMENT:
                mToolbar.setTitle(R.string.LogTitle);
                mFragment = new LogFragment();
                break;
        }
        fragmentManager.beginTransaction().replace(R.id.content_frame, mFragment).commit();
    }

    private void EnableDrawerIndicator() {
        mNavigationDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void DisableDrawerIndicator() {
        mNavigationDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // Show dialog window for confirmation removing history
    private void ShowRemoveHistoryDialogWindow() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PedometerActivity.this);
        builder.setTitle(R.string.RemoveHistoryTitle)
                .setMessage(R.string.RemoveHistoryBody)
                .setCancelable(false)
                .setPositiveButton(R.string.dialogOkButton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                PedometerSharedPreferences.ClearAllPreferences(getApplicationContext());
                                stopService(mPedometerService);
                                startService(mPedometerService);
                                PedometerService.resetTime();
                                PedometerService.resetSteps(getApplicationContext());
                                mCurrentFragment = HOME_FRAGMENT;
                                ShowCurrentFragment();
                            }
                        })
                .setNegativeButton(R.string.dialogCancelButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        mAlert = builder.create();
        mAlert.show();
    }
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

    String GetTime(){
        Date date = new Date();
        String time = String.valueOf(date.getHours()) + ":" + String.valueOf(date.getMinutes()) + ":" + String.valueOf(date.getSeconds());
 //       Log.d("PedometerService", "====steps_time======: " + time);
        return time;
    }
}
