package org.commcare.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;

import org.commcare.dalvik.R;
import org.commcare.interfaces.TimerListener;
import org.commcare.utils.GeoUtils;
import org.commcare.utils.ODKTimer;
import org.commcare.utils.StringUtils;
import org.commcare.views.dialogs.GeoProgressDialog;

import java.text.DecimalFormat;
import java.util.Set;

public class GeoPointActivity extends Activity implements LocationListener, TimerListener {
    private GeoProgressDialog mLocationDialog;
    private LocationManager mLocationManager;
    private Location mLocation;
    private Set<String> mProviders;

    private final static int millisToWait = 60000; //allow to accept location after 60 seconds

    private ODKTimer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(StringUtils.getStringRobust(this, R.string.application_name) +
                " > " + StringUtils.getStringRobust(this, R.string.get_location));

        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        mProviders = GeoUtils.evaluateProviders(mLocationManager);

        setupLocationDialog();
        long mLong = -1;
        if (savedInstanceState != null) {
            mLong = savedInstanceState.getLong("millisRemaining", -1);
        }
        if (mLong > 0) {
            mTimer = new ODKTimer(mLong, this);
        } else {
            mTimer = new ODKTimer(millisToWait, this);
        }
        mTimer.start();

    }

    @Override
    protected void onPause() {
        super.onPause();

        // stops the GPS. Note that this will turn off the GPS if the screen goes to sleep.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(this);
        }

        // We're not using managed dialogs, so we have to dismiss the dialog to prevent it from
        // leaking memory.
        if (mLocationDialog != null && mLocationDialog.isShowing())
            mLocationDialog.dismiss();
    }


    @Override
    protected void onResume() {
        super.onResume();

        mProviders = GeoUtils.evaluateProviders(mLocationManager);
        if (mProviders.isEmpty()) {
            DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mLocation = null;
                    GeoPointActivity.this.finish();
                }
            };

            DialogInterface.OnClickListener onChangeListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int i) {
                    switch (i) {
                        case DialogInterface.BUTTON_POSITIVE:
                            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            mLocation = null;
                            GeoPointActivity.this.finish();
                            break;
                    }
                    dialog.dismiss();
                }
            };

            GeoUtils.showNoGpsDialog(this, onChangeListener, onCancelListener);
        } else {
            for (String provider : mProviders) {
                if ((provider.equals(LocationManager.GPS_PROVIDER) && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
                        (provider.equals(LocationManager.NETWORK_PROVIDER) && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    mLocationManager.requestLocationUpdates(provider, 0, 0, this);
                }
            }
            // TODO PLM: warn user and ask for permissions if the user has disabled them
            mLocationDialog.show();
        }
    }


    /**
     * Sets up the look and actions for the progress dialog while the GPS is searching.
     */
    private void setupLocationDialog() {
        // dialog displayed while fetching gps location

        OnClickListener cancelButtonListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLocation = null;
                finish();
            }
        };

        OnClickListener okButtonListener = new OnClickListener() {
            public void onClick(View v) {
                returnLocation();
            }
        };

        mLocationDialog = new GeoProgressDialog(this, StringUtils.getStringRobust(this, R.string.found_location),
                StringUtils.getStringRobust(this, R.string.finding_location));

        // back button doesn't cancel
        mLocationDialog.setCancelable(false);
        mLocationDialog.setImage(getResources().getDrawable(R.drawable.green_check_mark));
        mLocationDialog.setMessage(StringUtils.getStringRobust(this, R.string.please_wait_long));
        mLocationDialog.setOKButton(StringUtils.getStringRobust(this, R.string.accept_location),
                okButtonListener);
        mLocationDialog.setCancelButton(StringUtils.getStringRobust(this, R.string.cancel_location),
                cancelButtonListener);
    }


    private void returnLocation() {
        if (mLocation != null) {
            Intent i = new Intent();
            i.putExtra(FormEntryActivity.LOCATION_RESULT, GeoUtils.locationToString(mLocation));
            setResult(RESULT_OK, i);
        }
        finish();
    }


    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        if (mLocation != null) {
            String[] args = {mLocation.getProvider(), truncateDouble(mLocation.getAccuracy())};
            mLocationDialog.setMessage(StringUtils.getStringRobust(this,
                    R.string.location_provider_accuracy, args));

            // If location is accurate, we're done
            if (mLocation.getAccuracy() <= GeoUtils.GOOD_ACCURACY) {
                returnLocation();
            }

            // If location isn't great but might be acceptable, notify
            // the user and let them decide whether or not to record it
            mLocationDialog.setLocationFound(
                    mLocation.getAccuracy() < GeoUtils.ACCEPTABLE_ACCURACY
                            || mTimer.getMillisUntilFinished() == 0
            );
        }
    }


    private String truncateDouble(float number) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(number);
    }


    @Override
    public void onProviderDisabled(String provider) {

    }


    @Override
    public void onProviderEnabled(String provider) {

    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                if (mLocation != null) {
                    mLocationDialog.setMessage(StringUtils.getStringRobust(this, R.string.location_accuracy,
                            "" + (int)mLocation.getAccuracy()));
                }
                break;
            case LocationProvider.OUT_OF_SERVICE:
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                break;
        }
    }

    @Override
    public void notifyTimerFinished() {
        onLocationChanged(mLocation);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong("millisRemaining", mTimer.getMillisUntilFinished());
        super.onSaveInstanceState(savedInstanceState);
    }
}
