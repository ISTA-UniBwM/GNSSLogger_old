/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ista.android.apps.location.gps.gnsslogger;

import static com.google.common.base.Preconditions.checkArgument;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GnssMeasurement;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.GnssStatus.Callback;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.location.gps.gnsslogger.R;

import android.location.GnssMeasurementsEvent;

import com.ista.android.apps.location.gps.gnsslogger.TimerService.TimerBinder;
import com.ista.android.apps.location.gps.gnsslogger.TimerService.TimerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/** The UI fragment that hosts a logging view. */
public class LoggerFragment extends Fragment implements TimerService.TimerListener {
    private static final String TIMER_FRAGMENT_TAG = "timer";

    private TextView mLogView;
    private ScrollView mScrollView;
    private FileLogger mFileLogger;
    private RinexLogger mRinexLogger;
    private UiLogger mUiLogger;
    private Button mStartLog;
    private boolean israwlogon;
    private Button mstartRaw2Rinex;
    private boolean isrinexlogon;
    private Button mTimer;
    private Button mSendFile;
    private TextView mTimerDisplay;
    private Chronometer logChrono;
    private TimerService mTimerService;
    private TimerValues mTimerValues =
            new TimerValues(0 /* hours */, 0 /* minutes */, 0 /* seconds */);
    private static boolean autoScroll = false;
    private int satelliteCounter = 0;
    private android.location.GnssStatus.Callback mCallback;
    boolean wasshown = false;
    boolean wasshown2 = false;

    private final BroadcastReceiver mBroadcastReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    checkArgument(intent != null, "Intent is null");

                    short intentType =
                            intent.getByteExtra(TimerService.EXTRA_KEY_TYPE, TimerService.TYPE_UNKNOWN);

                    // Be explicit in what types are handled here
                    switch (intentType) {
                        case TimerService.TYPE_UPDATE:
                        case TimerService.TYPE_FINISH:
                            break;
                        default:
                            return;
                    }

                    TimerValues countdown =
                            new TimerValues(intent.getLongExtra(TimerService.EXTRA_KEY_UPDATE_REMAINING, 0L));
                    LoggerFragment.this.displayTimer(countdown, true /* countdownStyle */);

                    if (intentType == TimerService.TYPE_FINISH) {
                        LoggerFragment.this.stopAndSend();
                    }
                }
            };
    private ServiceConnection mConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
                    mTimerService = ((TimerService.TimerBinder) serviceBinder).getService();
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    mTimerService = null;
                }
            };

    private final UIFragmentComponent mUiComponent = new UIFragmentComponent();

    public LoggerFragment() {
    }

    public void setUILogger(UiLogger value) {
        mUiLogger = value;
    }

    public void setFileLogger(FileLogger value) {
        mFileLogger = value;
    }

    public void setRinexLogger(RinexLogger value) {
        mRinexLogger = value;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, new IntentFilter(TimerService.TIMER_ACTION));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View newView = inflater.inflate(R.layout.fragment_log, container, false /* attachToRoot */);

        mLogView = (TextView) newView.findViewById(R.id.log_view);
        mScrollView = (ScrollView) newView.findViewById(R.id.log_scroll);

        getActivity()
                .bindService(
                        new Intent(getActivity(), TimerService.class), mConnection, Context.BIND_AUTO_CREATE);

        UiLogger currentUiLogger = mUiLogger;
        if (currentUiLogger != null) {
            currentUiLogger.setUiFragmentComponent(mUiComponent);
        }
        FileLogger currentFileLogger = mFileLogger;
        if (currentFileLogger != null) {
            currentFileLogger.setUiComponent(mUiComponent);
        }
        RinexLogger currentRinexLogger = mRinexLogger;
        if (currentRinexLogger != null) {
            currentRinexLogger.setUiComponent(mUiComponent);
        }

        Button start = (Button) newView.findViewById(R.id.start_log);
        Button end = (Button) newView.findViewById(R.id.end_log);
        Button clear = (Button) newView.findViewById(R.id.clear_log);

        start.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mScrollView.fullScroll(View.FOCUS_UP);
                    }
                });

        end.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });

        clear.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mLogView.setText("");
                    }
                });

        mTimerDisplay = (TextView) newView.findViewById(R.id.timer_display);
        logChrono = (Chronometer) newView.findViewById(R.id.chronometer);

        mTimer = (Button) newView.findViewById(R.id.timer);
        mStartLog = (Button) newView.findViewById(R.id.start_logs);
        mstartRaw2Rinex = (Button) newView.findViewById(R.id.Raw2Rinex);
        mSendFile = (Button) newView.findViewById(R.id.send_file);

        displayTimer(mTimerValues, false /* countdownStyle */);
        enableOptionsRaw(true /* start */);
        enableOptionsRinex(true /* start */);

        mStartLog.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        enableOptionsRaw(false /* start */);
                        Toast.makeText(getContext(), R.string.start_message, Toast.LENGTH_LONG).show();
                        mFileLogger.startNewLog();
                        logChrono.setBase(SystemClock.elapsedRealtime());
                        logChrono.start();
                        israwlogon = true;
                        if (!mTimerValues.isZero() && (mTimerService != null)) {
                            mTimerService.startTimer();
                        }
                    }
                });

        mstartRaw2Rinex.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        enableOptionsRinex(false /* start*/);
                        Toast.makeText(getContext(), R.string.start_message, Toast.LENGTH_LONG).show();
                        mRinexLogger.startNewLog();
                        logChrono.setBase(SystemClock.elapsedRealtime());
                        logChrono.start();
                        isrinexlogon = true;
                        if (!mTimerValues.isZero() && (mTimerService != null)) {
                            mTimerService.startTimer();
                        }
                    }
                });

        mSendFile.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        stopAndSend();
                    }
                });

        mTimer.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        launchTimerDialog();
                    }
                });

        GnssStatus.Callback gnssListener = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                super.onSatelliteStatusChanged(status);
                satelliteCounter = status.getSatelliteCount();
                if (satelliteCounter < 6) {
                    if (!wasshown) {
                        Toast.makeText(getContext(), "GNSS Measurements not available", Toast.LENGTH_LONG).show();
                        wasshown = true;
                    }
                }
                if (satelliteCounter >= 6) {
                    if (!wasshown2) {
                        Toast.makeText(getContext(), "GNSS Measurements available", Toast.LENGTH_LONG).show();
                        wasshown2 = true;
                    }
                }
            }
        };
        LocationManager mLocationmanager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        mLocationmanager.registerGnssStatusCallback(gnssListener);
        return newView;
    }


    void stopAndSend() {
        if (mTimer != null) {
            mTimerService.stopTimer();
        }

        if (israwlogon) {
            enableOptionsRaw(true /* start */);
            Toast.makeText(getContext(), R.string.stop_message, Toast.LENGTH_LONG).show();
            displayTimer(mTimerValues, false /* countdownStyle */);
            logChrono.stop();
            mFileLogger.send();
        }
        else if (isrinexlogon) {
            enableOptionsRinex(true /* start */);
            Toast.makeText(getContext(), R.string.stop_message, Toast.LENGTH_LONG).show();
            displayTimer(mTimerValues, false /* countdownStyle */);
            logChrono.stop();
            mRinexLogger.send();
        }
    }

  void displayTimer(TimerValues values, boolean countdownStyle) {
    String content;

    if (countdownStyle) {
      content = values.toCountdownString();
    } else {
      content = values.toString();
    }

    mTimerDisplay.setText(
        String.format("%s: %s", getResources().getString(R.string.timer_display), content));
  }

  @Override
  public void processTimerValues(TimerValues values) {
    if (mTimerService != null) {
      mTimerService.setTimer(values);
    }
    mTimerValues = values;
    displayTimer(mTimerValues, false /* countdownStyle */);
  }

  private void launchTimerDialog() {
    TimerFragment timer = new TimerFragment();
    timer.setTargetFragment(this, 0);
    timer.setArguments(mTimerValues.toBundle());
    timer.show(getFragmentManager(), TIMER_FRAGMENT_TAG);
  }

    private void enableOptionsRaw(boolean start) {
        mTimer.setEnabled(start);
        mStartLog.setEnabled(start);
        mSendFile.setEnabled(!start);
    }
    private void enableOptionsRinex(boolean start) {
        mTimer.setEnabled(start);
        mstartRaw2Rinex.setEnabled(start);
        mSendFile.setEnabled(!start);
    }


  /**
   * A facade for UI and Activity related operations that are required for {@link GnssListener}s.
   */
  public class UIFragmentComponent {

    private static final int MAX_LENGTH = 42000;
    private static final int LOWER_THRESHOLD = (int) (MAX_LENGTH * 0.5);

    public synchronized void logTextFragment(final String tag, final String text, int color) {
      final SpannableStringBuilder builder = new SpannableStringBuilder();
      builder.append(tag).append(" | ").append(text).append("\n");
      builder.setSpan(
          new ForegroundColorSpan(color),
          0 /* start */,
          builder.length(),
          SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE);

      Activity activity = getActivity();
      if (activity == null) {
        return;
      }
      activity.runOnUiThread(
          new Runnable() {
            @Override
            public void run() {
              mLogView.append(builder);
              SharedPreferences sharedPreferences = PreferenceManager.
                  getDefaultSharedPreferences(getActivity());
              Editable editable = mLogView.getEditableText();
              int length = editable.length();
              if (length > MAX_LENGTH) {
                editable.delete(0, length - LOWER_THRESHOLD);
              }
              if (sharedPreferences.getBoolean(SettingsFragment.PREFERENCE_KEY_AUTO_SCROLL,
                  false /*default return value*/)){
                mScrollView.post(new Runnable() {
                  @Override
                  public void run() {
                    mScrollView.fullScroll(View.FOCUS_DOWN);
                  }
                });
              }
            }
          });
    }

    public void startActivity(Intent intent) {
      getActivity().startActivity(intent);
    }
  }








}
