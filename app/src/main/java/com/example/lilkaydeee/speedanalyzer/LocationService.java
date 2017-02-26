package com.example.lilkaydeee.speedanalyzer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class LocationService extends Service implements
        LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private static final long MINIMUM_INTERVAL = 1000;
    private static final long FASTEST_INTERVAL = 1000;
    static double distance;
    private final IBinder mBinder = new LocalBinder();
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation, lStart, lEnd;
    String speedUnitType, distanceUnitType;
    double speed;
    double filterSpeed;

    public LocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        makeLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        return mBinder;
    }

    protected void makeLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(MINIMUM_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);
        } catch (SecurityException e) {
        }
    }

    protected void startlocationUpdates() {
        try {
            PendingResult<Status> pendingResult =
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                            mLocationRequest, this);
        } catch (SecurityException e) {
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (MainActivity.mProgressDialog.isShowing())
            MainActivity.mProgressDialog.dismiss();
        toastMessage("Couldn't complete, connection got suspended, Please check your internet settings");
    }

    private void toastMessage(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        MainActivity.mProgressDialog.dismiss();
        mCurrentLocation = location;
        if (lStart == null) {
            lStart = mCurrentLocation;
            lEnd = mCurrentLocation;
        } else
            lEnd = mCurrentLocation;
        speed = location.getSpeed();

        distance = distance + (lStart.distanceTo(lEnd));
        effectSettingsToApp();
        updateUI();
        lStart = lEnd;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        toastMessage("Connection failed, Please check your internet settings");
    }

/** takes in the elapsed time in millisSeconds and returns
 *  the time in HH:MM:MM format
 * */
    private String formatTime(long timeInMillisSeconds) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(timeInMillisSeconds),
                TimeUnit.MILLISECONDS.toMinutes(timeInMillisSeconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeInMillisSeconds)),
                TimeUnit.MILLISECONDS.toSeconds(timeInMillisSeconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInMillisSeconds)));
    }

    /** called to update the app's GUI every Second */

    private void updateUI() {
        filterSpeed = filterSpeed(filterSpeed, speed, 3);
        long diff = System.currentTimeMillis() - MainActivity.startTime;
            commitChanges(convertSpeed(filterSpeed), convertDistance(distance), diff);
    }

    private void setUnitToNull() {
        MainActivity.speedTextView.setText(getString(R.string.dash));
        MainActivity.distanceTextView.setText(getString(R.string.dash));
        MainActivity.timeTextView.setText(getString(R.string.dash));
    }

    /**
      * @param speed
     * @param distance
     * @param time
     * called in updateUI with current speed, distance, time
     * sets the UI with the passed in values
     */
    private void commitChanges(double speed, double distance, long time) {
        MainActivity.speedTextView.setText(new DecimalFormat("#.##").format(speed) + "  "+speedUnitType);
        MainActivity.timeTextView.setText(formatTime(time));
        MainActivity.distanceTextView.setText(new DecimalFormat("#.##").format(distance) +"  "+distanceUnitType);
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public Location getLocation() {
        return mCurrentLocation;
    }

    /**
     *
     * @param previous
     * @param current
     * @param ratio
     * @return
     *
     * returns filtered speed based on a ratio of the previous speed
     * and the current speed
     */
    private double filterSpeed(final double previous, final double current, final int ratio) {
        if (Double.isNaN(previous))
            return current;
        if (Double.isNaN(current)) {
            return previous;
        }
        return (current / ratio + previous * (1.0 - 1.0 / ratio));
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        lStart = null;
        lEnd = null;
        distance = 0;

        return super.onUnbind(intent);
    }

    /**
     * gets users preferred units from shared preferences and
     * set the current distanceUnitType and speedUnitType     *
     *  */
    private void effectSettingsToApp() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        distanceUnitType = preferences.getString(
                getString(R.string.pref_distance_unit_key), getString(R.string.pref_distance_unit_default));
        speedUnitType = preferences.getString(
                getString(R.string.pref_speed_unit_key), getString(R.string.pref_speed_unit_default));
//        convertDistance(distanceUnitType);
//        convertSpeed(speedUnitType, location);
    }

    private double convertSpeed(double speed) {
        if (speedUnitType.equals(getString(R.string.pref_speed_meters))) {
            return speed;
        } else if (speedUnitType.equals(getString(R.string.pref_speed_kilometers))) {
            return speed * (18 / 5);
        }
        return 0.0;
    }

    private double convertDistance(double distance) {
        if (distanceUnitType.equals(getString(R.string.meters))) {
            return distance;
        } else if (distanceUnitType.equals(getString(R.string.kilometer))) {
            return distance / 1000.00;
        }
        return 0.0;
    }
//    private String speedAnalysis(double speed){
//        if (speed == 0)
//            return "You have stopped";
//        if (speed > 0.1 && speed <1.8  )
//            return "Seems you're";
//        if (speed > 6.0 &&)
//    }

    protected class LocalBinder extends Binder {

        public LocationService getService() {
            return LocationService.this;
        }

    }
}