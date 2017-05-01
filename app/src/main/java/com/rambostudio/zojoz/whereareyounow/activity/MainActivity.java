package com.rambostudio.zojoz.whereareyounow.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.rambostudio.zojoz.whereareyounow.R;
import com.rambostudio.zojoz.whereareyounow.utils.LogUtil;
import com.rambostudio.zojoz.whereareyounow.utils.ToastUtil;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<LocationSettingsResult>
        , LocationListener, OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    protected static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    /**
     * Constant used in the location settings dialog.
     */
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    // Keys for storing activity state in the Bundle.
    protected final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    protected final static String KEY_LOCATION = "location";
    protected final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    // Labels
    protected String mLatitudeLabel, mLongitudeLabel, mLastUpdateTimeLabel;

    GoogleApiClient mGoogleApiClient;
    private Button mStartUpdatesButton;
    private Button mStopUpdatesButton;
    private TextView mLatitudeTextView, mLongitudeTextView, mLastUpdateTimeTextView;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private Location mCurrentLocation;
    private boolean mRequestingLocationUpdates;
    private String mLastUpdateTime;

    // polyline
    protected final static String KEY_POINT_LIST_LOCATION = "point_list_location";
    Polyline mPolyline;
    List<LatLng> mPointList;
    private GoogleMap mGoogleMap;

    // mock up data
    private double mMockLatitude;
    private double mMockLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initInstances();
        setLabels();

        updateValuesFromBundle(savedInstanceState);

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        buildGoogleApiClient();

        createLocationRequest();

        buildLocationSettingRequest();

        setupMap();
        mPointList = new ArrayList<>();

        mMockLatitude= 13.823450;
        mMockLongitude = 100.577079;

        mPointList.add(new LatLng(mMockLatitude,mMockLongitude));
    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }


    /**
     * ********************************** Function ******************************
     */

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void buildLocationSettingRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

    }

    private void createLocationRequest() {
        LogUtil.i(TAG, "createLocationRequest");

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildGoogleApiClient() {
        LogUtil.i(TAG, "buildGoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void initInstances() {
        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);
        mLatitudeTextView = (TextView) findViewById(R.id.latitude_text);
        mLongitudeTextView = (TextView) findViewById(R.id.longitude_text);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.last_update_time_text);
    }

    private void setLabels() {
        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLastUpdateTimeLabel = getResources().getString(R.string.last_update_time_label);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LogUtil.i(TAG, "onConnected");
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateLocationUI();
        } else {
            LogUtil.i(TAG, "onConnected : mCurrentLocation != null");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        LogUtil.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        LogUtil.d(TAG, "onConnectionFailed : " + connectionResult.getErrorMessage());
    }

    public void startUpdatesButtonHandler(View view) {
        LogUtil.i(TAG, "startUpdatesButtonHandler: ");
        checkLocationSettings();
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates.
     */
    public void stopUpdatesButtonHandler(View view) {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        stopLocationUpdates();
    }


    private void checkLocationSettings() {
        LogUtil.i(TAG, "checkLocationSettings");
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, mLocationSettingsRequest);
        result.setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        LogUtil.i(TAG, "checkLocationSettings - onResult");
        Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                LogUtil.i(TAG, "All location settings are satisfied.");
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                LogUtil.i(TAG, "Location settings are not satisfied. Show the user a dialog to " +
                        "upgrade location settings ");
                try {
                    status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    LogUtil.i(TAG, "PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                LogUtil.i(TAG, "Location setting are indaequate, and connot be fixed here. Dialog not created);");
                break;
        }
    }

    /**
     * occur from startResolutionForResult in OnResult
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.i(TAG, "onActivityResult: ");
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        LogUtil.i(TAG, "User accept open GPS");
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        LogUtil.i(TAG, "User not accept open GPS");
                        break;
                }
                break;
        }
    }

    private void startLocationUpdates() {

        LogUtil.i(TAG, "startLocationUpdates");
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                LogUtil.i(TAG, "startLocationUpdates - onResult :" + status.getStatus());
                mRequestingLocationUpdates = true;
                setButtonEnablesState();
            }
        });

    }

    private void setButtonEnablesState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    private void stopLocationUpdates() {

    }

    @Override
    public void onLocationChanged(Location location) {
        LogUtil.i(TAG, "onLocationChanged");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateLocationUI();
        addLocationPolyline(location);
//        addLocationPolylineMockup();
        ToastUtil.shortAlert("Location Updated");
    }

    private void addLocationPolyline(Location location) {
        LogUtil.i(TAG, "addLocationPolyline");
        if (mPointList == null) {
            LogUtil.i(TAG, "addLocationPolyline-mPointList == null");
            mPointList = new ArrayList<>();
        } else {
            LogUtil.i(TAG, "addLocationPolyline-mPointList != null");
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            mPointList.add(latlng);
            mPolyline.setPoints(mPointList);
            mPolyline = mGoogleMap.addPolyline(new PolylineOptions());
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15));
        }
    }

    private void addLocationPolylineMockup() {
        LogUtil.i(TAG, "addLocationPolyline");
        if (mPointList == null) {
            LogUtil.i(TAG, "addLocationPolyline-mPointList == null");
            mPointList = new ArrayList<>();
        } else {
            LogUtil.i(TAG, "addLocationPolyline-mPointList != null");
            mMockLongitude = mMockLongitude + 0.0001;
            LatLng latlng = new LatLng(mMockLatitude,mMockLongitude );
            mPointList.add(latlng);
            mPolyline.setPoints(mPointList);
            mPolyline = mGoogleMap.addPolyline(new PolylineOptions());
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 18));
        }
    }
    /**
     * Updates all UI fields.
     */
    private void updateUI() {
        setButtonEnablesState();
        updateLocationUI();
    }

    private void updateLocationUI() {
        LogUtil.i(TAG, "updateLocationUI");
        if (mCurrentLocation != null) {
            mLatitudeTextView.setText(String.format("%s: %f", mLatitudeLabel,
                    mCurrentLocation.getLatitude()));
            mLongitudeTextView.setText(String.format("%s: %f", mLongitudeLabel,
                    mCurrentLocation.getLongitude()));
            mLastUpdateTimeTextView.setText(String.format("%s: %s", mLastUpdateTimeLabel,
                    mLastUpdateTime));
        } else {
            ToastUtil.shortAlert("mCurrentLocation == null");
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(KEY_REQUESTING_LOCATION_UPDATES);
            }
            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }

            if (savedInstanceState.keySet().contains(KEY_POINT_LIST_LOCATION)) {
                mPointList = savedInstanceState.getParcelableArrayList(KEY_POINT_LIST_LOCATION);
            }
            updateUI();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        outState.putParcelable(KEY_LOCATION, mCurrentLocation);
        outState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);

        outState.putParcelableArrayList(KEY_POINT_LIST_LOCATION, (ArrayList<? extends Parcelable>) mPointList);
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LogUtil.i(TAG, "onMapReady");
        mGoogleMap = googleMap;

        if (mCurrentLocation != null) {
            LogUtil.i(TAG, "found location");
            ToastUtil.shortAlert("found location");
            mPolyline = googleMap.addPolyline(new PolylineOptions());
            mPolyline.setTag("A");
            mPolyline.setPoints(mPointList);
        } else {
            LogUtil.i(TAG, "Not found location");
//            mPolyline = googleMap.addPolyline(new PolylineOptions());
            ToastUtil.shortAlert("Not found location");

            mPolyline = googleMap.addPolyline(new PolylineOptions()
                    .clickable(true));
            mPolyline.setPoints(mPointList);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(13.823, 100.577), 15));
        }

//        if (mPolyline != null) {
//            mPolyline = googleMap.addPolyline(new PolylineOptions()
//                    .clickable(true));
//        }

    }
}
