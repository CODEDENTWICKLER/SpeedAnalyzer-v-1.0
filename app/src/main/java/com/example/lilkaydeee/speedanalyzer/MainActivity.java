package com.example.lilkaydeee.speedanalyzer;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This app uses GPS to detect user's location to calculate the
 * displacement and hence user's instantaneous speed. The accuracy
 * depends on if the app can get wifi access closeby and if there is
 * network coverage. The app tries to guess what the user is doing at an instant.
 * */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    //    public static long endTime;
    public static long startTime;
    static ProgressDialog mProgressDialog;
    static Button startButton;
    static Button stopButton;
    static TextView distanceTextView;
    static TextView timeTextView;
    static TextView speedTextView;
    static TextView analysisTextView;
    static boolean mBound;
    LocationService mLocationService;
    //    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;
    private LocationManager locationManager;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            LocationService.LocalBinder binder = (LocationService.LocalBinder) iBinder;
            mLocationService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeScreen();

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkGps();
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(getApplicationContext(), "Please Enable GPS in your device",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mBound)
                    bindService();
                showProgressDialog();

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBound)
                    unBindService();
                stopButton.setVisibility(View.GONE);
                startButton.setVisibility(View.VISIBLE);
            }
        });

    }

    private void initializeScreen() {
        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        analysisTextView = (TextView) findViewById(R.id.analysisTextView);
        distanceTextView = (TextView) findViewById(R.id.distanceTextView);
        speedTextView = (TextView) findViewById(R.id.speedTextView);
        timeTextView = (TextView) findViewById(R.id.timeTextView);


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String distanceUnitType = preferences.getString(getString(R.string.pref_distance_unit_key),
                getString(R.string.meters));
        String speedUnitType = preferences.getString(getString(R.string.pref_speed_unit_key),
                getString(R.string.pref_speed_meters));

        distanceTextView.setText(R.string.zero+" "+distanceUnitType);
        speedTextView.setText( R.string.zero +" " +speedUnitType);
        timeTextView.setText(R.string.zero_time);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.hint:
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.hint_text);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
                break;
            case R.id.settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

//    protected void effectPref() {
//        mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
//            @Override
//            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
//                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//                String distanceUnitType = preferences.getString(getString(R.string.pref_distance_unit_key),
//                        getString(R.string.meters));
//                String speedUnitType = preferences.getString(getString(R.string.pref_speed_unit_key),
//                        getString(R.string.pref_speed_meters));
//                distanceTextView.setText(getString(R.string.zero, distanceUnitType));
//                speedTextView.setText(getString(R.string.zero, speedUnitType));
//                timeTextView.setText(getString(R.string.zero_time));
//                preferences.registerOnSharedPreferenceChangeListener(mPrefListener);
//            }
//        };
//    }

    private void bindService() {
        if (mBound)
            return;
        Intent intent = new Intent(getApplicationContext(), LocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
        startTime = System.currentTimeMillis();
    }

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Getting Location...");
        mProgressDialog.show();
        startButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.VISIBLE);
    }

    void unBindService() {
        if (!mBound)
            return;
        mBound = false;
        unbindService(mConnection);
    }

    @Override
    public void onBackPressed() {
        if (!mBound)
            super.onBackPressed();
        else moveTaskToBack(true);
    }

    void checkGps() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            showGPSDisabledAlertToUser();
        }
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Enable GPS to use application")
                .setCancelable(false)
                .setPositiveButton("Enable GPS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
}