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

import android.content.Context;
import android.content.Intent;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.location.cts.asn1.supl2.rrlp_components.GPSTime;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.google.android.apps.location.gps.gnsslogger.BuildConfig;
import com.ista.location.lbs.gnss.gps.pseudorange.GpsTime;

import org.joda.time.DateTime;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import static java.lang.Math.round;
import java.util.concurrent.TimeUnit;

/**
 * A GNSS logger to store information to a file.
 */
public class RinexLogger implements GnssListener {

    /** Used in Header of Rinex File */
    private static final double VERSION_TAG = 3.03;
    private static double[] ecef ;
    private static final String TYPE = "OBSERVATION DATA";
    private static final String SATSYS = "M: Mixed";
    private static final String PGM = "GNSS/IMU Logger";
    private static final String RUN_BY = "ISTA";
    private static final String OBSERVER = "ISTA";
    private static final String AGENCY = "ISTA";
    private static final String MARKER_TYPE = "GEODETIC";
    private static final String RECEIVER_NUMBER = "Unknown";
    private static final String ANTENNA_NUMBER = "Unknown";
    Boolean alreadyExecuted = false;
    Boolean cycleslip_flag_GPS_L1_next_epoch = false;
    Boolean cycleslip_flag_GPS_L5_next_epoch = false;
    Boolean cycleslip_flag_GLO_L1_next_epoch = false;
    Boolean cycleslip_flag_BED_L1_next_epoch = false;
    Boolean cycleslip_flag_GAL_L1_next_epoch = false;
    Boolean cycleslip_flag_GAL_L5_next_epoch = false;


    /** Container for GNSS Measurments*/
    Vector<GnssMeasurement> epochmeasurementcontainer = new Vector();

    /** Container for CMC Measurments*/
    Vector<cmc_epoch_measurement> CMC_measurement = new Vector();
    Vector<adr_state_epoch_measurement> adr_measurement = new Vector();
    private CodeminusCarrierFragment mcmcfragment;
    private static double timeInSeconds = 1.0e-9;

    public static long SysGnssOffset;

    /** Used in Obs Calculation*/
    private static final double SPEED_OF_LIGHT = 299792458.0; // [m/s]
    private static final double Frequency_L1 = 154.0 * 10.23e6;
    private static final double Frequency_C1 = 152.60 * 10.23e6;
    private static final double Frequency_L5 = 115.0 * 10.23e6;
    private static final double Wavelewngth_L1 = SPEED_OF_LIGHT / Frequency_L1;
    private static final double Wavelewngth_L5 = SPEED_OF_LIGHT / Frequency_L5;
    private static final double Wavelewngth_C1 = SPEED_OF_LIGHT / Frequency_C1;
    private static double l1 = 1.0e-9 ,l5 = 1.0e-9 ,d1 = 1.0e-9,d5 = 1.0e-9,s1 = 1.0e-9,s5 = 1.0e-9,c1 = 1.0e-9,c5 = 1.0e-9;
    private static double tRxSeconds = 1.0e-9,tTxSeconds = 1.0e-9,tau = 1.0e-9;
    private static double gpsweek = 1.0e-9 ,gpssow = 1.0e-9;
    private static double NS_TO_S = 1.0e-9,local_est_GPS_time = 1.0e-9;
    private static  double fullBiasNanos = 1.0e-9,BiasNanos = 1.0e-9;
    private static Integer prn;
    private static double frac = 1.0e-9;
    private static Integer GPS_WEEKSECS = 604800;
    private static Integer GLO_DAYSSECS = 86400;
    private static Integer GLOT_TO_UTC = 10800;
    private static Integer BDST_TO_GPST = 14;
    private static Integer CURRENT_GPS_LEAP_SECOND = 18;
    Boolean set_clockbias = false;

    /**Used for file Storage*/
    private static final int MAX_FILES_STORED = 100;
    private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;
    private final Context mContext;
    private final Object mFileLock = new Object();
    private BufferedWriter mFileWriter;
    private File mFile;
    private String rootfolder = "GNSS-INS Logger";
    private static final String TAG = "RinexLogger";
    private static final String FILE_PREFIX = "Rinex_Log";
    private static final String ERROR_WRITING_FILE = "Problem writing to file.";

    /** USed in Clock Correction **/
    int count =0;
    int averagewindow = 30;
    long movingtimeoffset = 0;
    long movingaveragetimeoffset= 0;
    public long SysgpssOffsetnano;
    public static long gps_localtime_nano,system_elapsedrealtimenano;
    private SensorFragment msensorfragment = new SensorFragment();
    public Boolean hasclockElpasedrealtime = false;
    Vector<Long> SysgpssOffsetnanorecord = new Vector();
    private internalclockoffsetFragment minternalclockoffset;

    private LoggerFragment.UIFragmentComponent mUiComponent;
    public synchronized LoggerFragment.UIFragmentComponent getUiComponent() {
        return mUiComponent;
    }
    public synchronized void setUiComponent(LoggerFragment.UIFragmentComponent value) {
        mUiComponent = value;
    }
    public RinexLogger(Context context) {
        this.mContext = context;
    }

    /**
     * Start a new file logging process.
     */
    public void startNewLog() {
        synchronized (mFileLock) {
            File baseDirectory;
            alreadyExecuted = false;

            String state = Environment.getExternalStorageState();

            if (Environment.MEDIA_MOUNTED.equals(state))
            {
               // Environment.get
                File folder_root = new File(mContext.getExternalFilesDir(null),rootfolder);
                folder_root.mkdir();
                baseDirectory = new File(folder_root, FILE_PREFIX);
                baseDirectory.mkdirs();
            }
            else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                logError("Cannot write to external storage.");
                return;
            } else {
                logError("Cannot read external storage.");
                return;
            }

            SimpleDateFormat formatter = new SimpleDateFormat("yyyMMddHHmmss");
            SimpleDateFormat Year_short = new SimpleDateFormat("yy");

            Date now = new Date();
            String fileName = String.format("%s_%s_%s.%so", Build.MODEL, "_RNX_",formatter.format(now), Year_short.format(now));
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            File currentFile = new File(baseDirectory, fileName);
            String currentFilePath = currentFile.getAbsolutePath();
            BufferedWriter currentFileWriter;
            try {
                currentFileWriter = new BufferedWriter(new FileWriter(currentFile));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePath, e);
                return;
            }
            if (mFileWriter != null) {
                try {
                    mFileWriter.close();
                } catch (IOException e) {
                    logException("Unable to close all file streams.", e);
                    return;
                }
            }
            mFile = currentFile;
            mFileWriter = currentFileWriter;
            Toast.makeText(mContext, "File opened: " + currentFilePath, Toast.LENGTH_SHORT).show();

            // To make sure that files do not fill up the external storage:
            // - Remove all empty files
            FileFilter filter = new FileToDeleteFilter(mFile);
            for (File existingFile : baseDirectory.listFiles(filter)) {
                existingFile.delete();
            }
            // - Trim the number of files with data
            File[] existingFiles = baseDirectory.listFiles();
            int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
            if (filesToDeleteCount > 0) {
                Arrays.sort(existingFiles);
                for (int i = 0; i < filesToDeleteCount; ++i) {
                    existingFiles[i].delete();
                }
            }
        }
    }
    /**
     * Send the current log via email or other options selected from a pop menu shown to the user. A
     * new log is started when calling this function.
     */
    public void send() {
        if (mFile == null) {
            return;
        }
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("*/*");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "RINEX-Log");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        //Uri fileURI = Uri.fromFile(mFile);
        Uri fileURI =
                FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".Provider", mFile);
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileURI);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        getUiComponent().startActivity(Intent.createChooser(emailIntent, "Send log.."));
        if (mFileWriter != null) {
            try {
                mFileWriter.flush();
                mFileWriter.close();
                mFileWriter = null;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            synchronized (mFileLock) {
                if (mFileWriter == null) {
                    return;
                }
                //Location location;
                double a = 6378137; // radius
                double e = 8.1819190842622e-2;  // eccentricity
                double esq = Math.pow(e, 2);
                double lat = Math.toRadians(location.getLatitude());
                double lon = Math.toRadians(location.getLongitude());
                double alt = location.getAltitude();

                double N = a / Math.sqrt(1 - esq * Math.pow(Math.sin(lat), 2));

                double x = (N + alt) * Math.cos(lat) * Math.cos(lon);
                double y = (N + alt) * Math.cos(lat) * Math.sin(lon);
                double z = ((1 - esq) * N + alt) * Math.sin(lat);

                double[] ret = {x, y, z};
                ecef = ret;
            }
        }
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {

        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }
            GnssClock gnssClock = event.getClock();
            SysGnssOffset = gnssClock.getTimeNanos() - SystemClock.elapsedRealtimeNanos();
           // timeInSeconds = TimeUnit.NANOSECONDS.toSeconds(event.getClock().getTimeNanos());
            for (GnssMeasurement measurement : event.getMeasurements()) {
                epochmeasurementcontainer.add(measurement);
            }
            try {
                if(!alreadyExecuted){
                    RinexHeader(gnssClock , epochmeasurementcontainer.firstElement());
                    alreadyExecuted = true;}
                writeGnssMeasurementToRinexFile(gnssClock, epochmeasurementcontainer);
            } catch (IOException e) {
                logException(ERROR_WRITING_FILE, e);
            }
        }
        epochmeasurementcontainer.clear();
    }

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {
    }
    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage navigationMessage) {
    }
    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {
    }
    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus) {
    }
    @Override
    public void onNmeaReceived(long timestamp, String s) {
    }
    @Override
    public void onListenerRegistration(String listener, boolean result) {
    }
    @Override
    public void onTTFFReceived(long l) {
    }

    public void writeGnssMeasurementToRinexFile(GnssClock clock, Vector<GnssMeasurement> measurement)
            throws IOException {

        // Initialize number of satellite in epoch to zero
        int satellite_in_epoch = 0;
        int count =0;
        long movingtimeoffset = 0;


        // Initialize Vector to Store Constellation Specific Measurements
        Vector<GnssMeasurement> measurement_GPS = new Vector();
        Vector<GnssMeasurement> measurement_GAL = new Vector();
        Vector<GnssMeasurement> measurement_GLO = new Vector();
        Vector<GnssMeasurement> measurement_BED = new Vector();

        /**maintaining constant the 'FullBiasNanos' instead of using the instantaneous value. This avoids the 256 ns
         jumps each 3 seconds that create a code-phase
         divergence due to the clock.*/
        if (!set_clockbias)
        {
            fullBiasNanos = clock.getFullBiasNanos();
            BiasNanos = clock.getBiasNanos();
            set_clockbias = true;
        }

        // This routine will check if the pseduorange measurements are valid and then passed to new vector
        Vector <GnssMeasurement> measurement_new = pseudorange_check(clock, measurement);

        // Counting the number of Satellites in epoch
        for(int i=0;i<measurement_new.size();i++) {
            if ((measurement_new.get(i).getConstellationType() == GnssStatus.CONSTELLATION_GPS) &&
                    ((Math.abs(measurement_new.get(i).getCarrierFrequencyHz() - Frequency_L1) <= 100) ||
                            (Float.isNaN(measurement_new.get(i).getCarrierFrequencyHz())))) {
                satellite_in_epoch = satellite_in_epoch + 1;
            } else if ((measurement_new.get(i).getConstellationType() == GnssStatus.CONSTELLATION_GALILEO)
                    && ((Math.abs(measurement_new.get(i).getCarrierFrequencyHz() - Frequency_L1) <= 100)
                    || (Float.isNaN(measurement_new.get(i).getCarrierFrequencyHz())))) {
                satellite_in_epoch = satellite_in_epoch + 1;
            } else if ((measurement_new.get(i).getConstellationType() == GnssStatus.CONSTELLATION_GLONASS)) {
                satellite_in_epoch = satellite_in_epoch+1;
            } else if ((measurement_new.get(i).getConstellationType() == GnssStatus.CONSTELLATION_BEIDOU)) {
                satellite_in_epoch = satellite_in_epoch + 1;
            }
        }

        /**Epoch Header Calculation
        Compute the reception and transmission times*/
        gpsweek = Math.floor(-fullBiasNanos * NS_TO_S / GPS_WEEKSECS);
        local_est_GPS_time = clock.getTimeNanos() + measurement_new.get(0).getTimeOffsetNanos() - (fullBiasNanos + BiasNanos);
        gpssow = local_est_GPS_time * NS_TO_S - gpsweek * GPS_WEEKSECS;
        timeInSeconds = gpssow;
        frac = gpssow - (int)(gpssow + 0.5);
        DateTime epoch = GetFromGps(gpsweek,  gpssow - frac);
        int year = epoch.getYear();
        int month = epoch.getMonthOfYear();
        int date = epoch.getDayOfMonth();
        int hour = epoch.getHourOfDay();
        int min = epoch.getMinuteOfHour();
        int second = epoch.getSecondOfMinute();
        double milli = epoch.getMillisOfSecond();
        double sec = second + (milli*10.0e-4);
        int epoch_flag = 0;
        String formatStr_epoch = "%s%1s%4d%1s%2d%1s%2d%1s%2d%1s%2d%11.7f%2s%1d%3d%6s%15s%n";
        mFileWriter.write(String.format( Locale.US,formatStr_epoch, ">","",year,"",month ,"",date,"",hour,"",min,sec,"",epoch_flag,satellite_in_epoch,"",""));

        // stacking measurement data in constellation specific vector
        for(int i=0;i<measurement_new.size();i++) {
            if ((measurement_new.get(i).getConstellationType() == GnssStatus.CONSTELLATION_GPS)) {
                measurement_GPS.add(measurement_new.get(i));
            } else if ((measurement_new.get(i).getConstellationType() == GnssStatus.CONSTELLATION_GALILEO)) {
                measurement_GAL.add(measurement_new.get(i));
            } else if ((measurement_new.get(i).getConstellationType() == GnssStatus.CONSTELLATION_GLONASS)) {
                measurement_GLO.add(measurement_new.get(i));
            } else if ((measurement_new.get(i).getConstellationType() == GnssStatus.CONSTELLATION_BEIDOU)) {
                measurement_BED.add(measurement_new.get(i));
            }
        }

        //Compute the reception and transmission times
        gpsweek = Math.floor(-fullBiasNanos * NS_TO_S / GPS_WEEKSECS);
        local_est_GPS_time = clock.getTimeNanos() - (fullBiasNanos + BiasNanos);
        gpssow = local_est_GPS_time * NS_TO_S - gpsweek * GPS_WEEKSECS;
        frac = gpssow - (int)(gpssow + 0.5);

        //calculate gps vs utc offset
        gps_localtime_nano = (long) local_est_GPS_time;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if(clock.hasElapsedRealtimeNanos()){
                    system_elapsedrealtimenano = clock.getElapsedRealtimeNanos();
                    hasclockElpasedrealtime = true;
                }
                else{
                    system_elapsedrealtimenano = SystemClock.elapsedRealtimeNanos();
                    hasclockElpasedrealtime = false; }
            }
        else {
            system_elapsedrealtimenano = SystemClock.elapsedRealtimeNanos();
            hasclockElpasedrealtime = false; }

        SysgpssOffsetnano =  gps_localtime_nano - system_elapsedrealtimenano ;

       minternalclockoffset.Timeoffset(SysgpssOffsetnano,gpssow,hasclockElpasedrealtime);
       SysgpssOffsetnanorecord.add(SysgpssOffsetnano);

       if(SysgpssOffsetnanorecord.size() >= averagewindow)
       {
            for (int i=SysgpssOffsetnanorecord.size()-averagewindow; i < SysgpssOffsetnanorecord.size(); i++)
            {
                movingtimeoffset = movingtimeoffset + (SysgpssOffsetnanorecord.get(i)- SysgpssOffsetnanorecord.get(SysgpssOffsetnanorecord.size()-averagewindow));
                count = count+1;
            }
                movingaveragetimeoffset = movingtimeoffset/count + SysgpssOffsetnanorecord.get(SysgpssOffsetnanorecord.size()-averagewindow);
                msensorfragment.gnsstimeupdate(movingaveragetimeoffset);
       }
       else
       {
           for (int i=0; i < SysgpssOffsetnanorecord.size(); i++)
           {
               movingtimeoffset = movingtimeoffset + (SysgpssOffsetnanorecord.get(i)-SysgpssOffsetnanorecord.get(0));
               count = count+1;
           }
           movingaveragetimeoffset = movingtimeoffset/count + SysgpssOffsetnanorecord.get(0);
           msensorfragment.gnsstimeupdate(movingaveragetimeoffset);
       }

        //GPS Logging
        for(int i=0; i < measurement_GPS.size();i++)
        {

            // Initialize Obs Values
            l1 = 1.0e-9;c1 = 1.0e-9;s1 = 1.0e-9;d1 = 1.0e-9;
            tRxSeconds = 1.0e-9;tTxSeconds = 1.0e-9;tau = 1.0e-9;

            // used for cycle slip indicator
            int cycleslip_flag_GPS;

            // Check if L5 observation is available
            boolean GPS_L5_exist = false;
			
			//used for storing CMC values
			cmc_epoch_measurement cmc_meas = new cmc_epoch_measurement();
			adr_state_epoch_measurement adr_state = new adr_state_epoch_measurement();

            // check if carrier frequency (L1) value is available in raw measurement.
            if (((Math.abs(measurement_GPS.get(i).getCarrierFrequencyHz() - Frequency_L1) <= 100)
                    || (Float.isNaN(measurement_GPS.get(i).getCarrierFrequencyHz()))))
            {
                tRxSeconds = gpssow - measurement_GPS.get(i).getTimeOffsetNanos() * NS_TO_S;
                tTxSeconds = measurement_GPS.get(i).getReceivedSvTimeNanos() * NS_TO_S;

                //Compute the travel time, which will be eventually the pseudorange
                tau = check_week_crossover(tRxSeconds,tTxSeconds);

                // travel time * Speed of light
                c1 = tau * SPEED_OF_LIGHT;

                // use getPseudorangeRateMetersPerSecond to smooth pseudorange
                c1 -= frac*measurement_GPS.get(i).getPseudorangeRateMetersPerSecond();

                // get PRN for constellation
                prn = measurement_GPS.get(i).getSvid();

                // Calculate Carrier phase from getAccumulatedDeltaRangeMeters
                l1 = measurement_GPS.get(i).getAccumulatedDeltaRangeMeters() / Wavelewngth_L1;

                //use getPseudorangeRateMetersPerSecond to smooth carrier to nearest timestamp
                l1 -= frac*(measurement_GPS.get(i).getPseudorangeRateMetersPerSecond() / Wavelewngth_L1);

                // Calculate Doppler from getPseudorangeRateMetersPerSecond
                d1 = -measurement_GPS.get(i).getPseudorangeRateMetersPerSecond() / Wavelewngth_L1;

                // get CNR for the PRN
                s1 = measurement_GPS.get(i).getCn0DbHz();
				
				//CMC Calculation
				cmc_meas.cmc_val=c1- (l1*Wavelewngth_L1) ;
                cmc_meas.satID=prn;
                cmc_meas.Constellation_type=GnssStatus.CONSTELLATION_GPS;
                cmc_meas.freq= Frequency_L1;

             //   adr_state.Constellation_type = measurement_GPS.get(i).getConstellationType();
             //   adr_state.satID = measurement_GPS.get(i).getSvid();
             //   adr_state.state = measurement_GPS.get(i).getAccumulatedDeltaRangeState();
             //   adr_state.freq = Frequency_L1;
             //   adr_measurement.add(adr_state);


                /*Check AccumlatedDeltaRangeState to detect Cycle
                * refer https://developer.android.com/reference/android/location/GnssMeasurement#ADR_STATE_HALF_CYCLE_REPORTED*/
                if((measurement_GPS.get(i).getAccumulatedDeltaRangeState()%2 == 0)){
                   // cycleslip_flag_GPS = Integer.parseInt(" ");
                   // l1 = Double.parseDouble("              ");
                    cycleslip_flag_GPS = 1;
                    //cycleslip_flag_GPS_L1_next_epoch = true;
                }
              /*  else if (cycleslip_flag_GPS_L1_next_epoch){
                    cycleslip_flag_GPS = 1;
                    cycleslip_flag_GPS_L1_next_epoch = false;
                }*/
                else
                    cycleslip_flag_GPS = 0;

                cmc_meas.LLIflag = cycleslip_flag_GPS;
                CMC_measurement.add(cmc_meas);

                // Print the GPS L1 Observation
                String formatStr_GPS_L1 = "%s%02d%14.3f%1s%1s%14.3f%d%1s%14.3f%1s%1s%14.3f%1s%1s";
                mFileWriter.write(String.format( Locale.US,formatStr_GPS_L1, "G", prn, c1, "", "", l1, cycleslip_flag_GPS, "", d1, "", "", s1, "", ""));
            }
            for (int j = i+1; j < measurement_GPS.size() ; j++)
            {
                // Initialize Obs Values
                l5 = 1.0e-9;c5 = 1.0e-9;d5 = 1.0e-9;s5 = 1.0e-9;
                tRxSeconds = 1.0e-9;tTxSeconds = 1.0e-9;tau = 1.0e-9;

                // check if carrier frequency (L5) value is available in raw measurement.
                if ((measurement_GPS.get(j).getCarrierFrequencyHz() - Frequency_L5 <= 100))
                {
                    if (measurement_GPS.get(i).getSvid() == measurement_GPS.get(j).getSvid()
                            && (measurement_GPS.get(i).getConstellationType() == measurement_GPS.get(j).getConstellationType()))
                    {
                        tRxSeconds = gpssow - measurement_GPS.get(j).getTimeOffsetNanos() * NS_TO_S;
                        tTxSeconds = measurement_GPS.get(j).getReceivedSvTimeNanos() * NS_TO_S;

                        //Compute the travel time, which will be eventually the pseudorange
                        tau = check_week_crossover(tRxSeconds,tTxSeconds);

                        // travel time * Speed of light
                        c5 = tau * SPEED_OF_LIGHT;

                        // use getPseudorangeRateMetersPerSecond to smooth pseudorange
                        c5 -= frac*measurement_GPS.get(j).getPseudorangeRateMetersPerSecond();

                        // Calculate Carrier phase from getAccumulatedDeltaRangeMeters
                        l5 = measurement_GPS.get(j).getAccumulatedDeltaRangeMeters() / Wavelewngth_L5;

                        //use getPseudorangeRateMetersPerSecond to smooth carrier to nearest timestamp
                        l5 -= frac*(measurement_GPS.get(j).getPseudorangeRateMetersPerSecond() / Wavelewngth_L5);

                        // Calculate Doppler from getPseudorangeRateMetersPerSecond
                        d5 = -measurement_GPS.get(j).getPseudorangeRateMetersPerSecond() / Wavelewngth_L5;

                        // get CNR for the PRN
                        s5 = measurement_GPS.get(j).getCn0DbHz();

                        /*Check AccumlatedDeltaRangeState to detect Cycle
                         * refer https://developer.android.com/reference/android/location/GnssMeasurement#ADR_STATE_HALF_CYCLE_REPORTED*/
                        if((measurement_GPS.get(j).getAccumulatedDeltaRangeState()%2 == 0 )){
                            //cycleslip_flag_GPS = Integer.parseInt(" ");
                            //l5 = Double.parseDouble("              ");
                            cycleslip_flag_GPS = 1;
                            //cycleslip_flag_GPS_L5_next_epoch = true;

                        }
                       /* else if (cycleslip_flag_GPS_L5_next_epoch){
                            cycleslip_flag_GPS = 1;
                            cycleslip_flag_GPS_L5_next_epoch = false;
                        }*/
                        else
                            cycleslip_flag_GPS = 0;

                        // Print the GPS L5 Observation
                        String formatStr_GPS_L5 = "%14.3f%1s%1s%14.3f%d%1s%14.3f%1s%1s%14.3f%1s%1s%n";
                        mFileWriter.write(String.format( Locale.US,formatStr_GPS_L5, c5, "", "", l5, cycleslip_flag_GPS, "", d5, "", "", s5, "", ""));
                        GPS_L5_exist = true;
                    }
                }
            }
            if((GPS_L5_exist == false) && ((Math.abs(measurement_GPS.get(i).getCarrierFrequencyHz() - Frequency_L1) <= 100)
                    || (Float.isNaN(measurement_GPS.get(i).getCarrierFrequencyHz())))) {
                mFileWriter.newLine();
            }
        }

        //GLONASS Logging
        for(int i=0; i < measurement_GLO.size();i++)
        {

            // Initialize Obs Values
            l1 = 1.0e-9;c1 = 1.0e-9;s1 = 1.0e-9;d1 = 1.0e-9;
            tRxSeconds = 1.0e-9;tTxSeconds = 1.0e-9;tau = 1.0e-9;

            // used for cycle slip indicator
            int cycleslip_flag_GLO;

            // Check for Constellation type GLONASS
            if (((measurement_GLO.get(i).getConstellationType() == GnssStatus.CONSTELLATION_GLONASS))) {

                // Make sure that we report GLONASS OSN (PRN) instead of FCN
                if (measurement_GLO.get(i).getSvid() < 93)
                {
                    //Compute the reception and transmission times
                    tRxSeconds = gpssow - measurement_GLO.get(i).getTimeOffsetNanos() * NS_TO_S;
                    tTxSeconds = measurement_GLO.get(i).getReceivedSvTimeNanos() * NS_TO_S;

                    // Get Epoch time
                   // DateTime gpst_epoch = GetFromGps(gpsweek, tRxSeconds);
                    DateTime gpst_epoch = GetFromGps(gpsweek, gpssow);

                    // GLONASS to GPS Time
                    tTxSeconds = glot_to_gpst(gpst_epoch,tTxSeconds) + CURRENT_GPS_LEAP_SECOND;

                    //Compute the travel time, which will be eventually the pseudorange
                    tau = check_week_crossover(tRxSeconds,tTxSeconds);

                    // travel time * Speed of light
                    c1 = tau * SPEED_OF_LIGHT;

                    // use getPseudorangeRateMetersPerSecond to smooth pseudorange
                    c1 -= frac*measurement_GLO.get(i).getPseudorangeRateMetersPerSecond();

                    // get PRN for constellation
                    prn = measurement_GLO.get(i).getSvid();

                    // Calculate Carrier phase from getAccumulatedDeltaRangeMeters
                    l1 = measurement_GLO.get(i).getAccumulatedDeltaRangeMeters() / Wavelewngth_C1;

                    //use getPseudorangeRateMetersPerSecond to smooth carrier to nearest timestamp
                    l1 -= frac*(measurement_GLO.get(i).getPseudorangeRateMetersPerSecond() / Wavelewngth_C1);

                    // Calculate Doppler from getPseudorangeRateMetersPerSecond
                    d1 = -measurement_GLO.get(i).getPseudorangeRateMetersPerSecond() / Wavelewngth_C1;

                    // get CNR for the PRN
                    s1 = measurement_GLO.get(i).getCn0DbHz();

                    /*Check AccumlatedDeltaRangeState to detect Cycle
                     * refer https://developer.android.com/reference/android/location/GnssMeasurement#ADR_STATE_HALF_CYCLE_REPORTED*/
                    if((measurement_GLO.get(i).getAccumulatedDeltaRangeState()%2 == 0)) {
                        //cycleslip_flag_GLO = Integer.parseInt(" ");
                        //l1 = Double.parseDouble("              ");
                        cycleslip_flag_GLO = 1;
                        //cycleslip_flag_GLO_L1_next_epoch = true;
                    }
                   /* else if (cycleslip_flag_GLO_L1_next_epoch){
                        cycleslip_flag_GLO = 1;
                        cycleslip_flag_GLO_L1_next_epoch = false;
                    }*/
                    else
                        cycleslip_flag_GLO = 0;

                    // Print the GLONASS Observation
                    String formatStr_GLO_L1 = "%s%02d%14.3f%1s%1s%14.3f%d%1s%14.3f%1s%1s%14.3f%1s%1s%n";
                    mFileWriter.write(String.format( Locale.US,formatStr_GLO_L1, "R", prn, c1, "", "", l1, cycleslip_flag_GLO, "", d1, "", "", s1, "", ""));
                }
            }
        }

        //BEIDOU Logging
        for(int i=0; i < measurement_BED.size();i++)
        {

            // Initialize Obs Values
            l1 = 1.0e-9;c1 = 1.0e-9;s1 = 1.0e-9;d1 = 1.0e-9;
            tRxSeconds = 1.0e-9;tTxSeconds = 1.0e-9;tau = 1.0e-9;

            // used for cycle slip indicator
            int cycleslip_flag_BED;
			
			//used for storing CMC values
			cmc_epoch_measurement cmc_meas = new cmc_epoch_measurement();

            // Check for Constellation type BEIDOU
            if (((measurement_BED.get(i).getConstellationType() == GnssStatus.CONSTELLATION_BEIDOU)))
            {
                //Compute the reception and transmission times
                tRxSeconds = gpssow - measurement_BED.get(i).getTimeOffsetNanos() * NS_TO_S;
                tTxSeconds = measurement_BED.get(i).getReceivedSvTimeNanos() * NS_TO_S  + BDST_TO_GPST;

                //Compute the travel time, which will be eventually the pseudorange
                tau = check_week_crossover(tRxSeconds,tTxSeconds);

                // travel time * Speed of light
                c1 = tau * SPEED_OF_LIGHT;

                // use getPseudorangeRateMetersPerSecond to smooth pseudorange
                c1 -= frac*measurement_BED.get(i).getPseudorangeRateMetersPerSecond();

                // get PRN for constellation
                prn = measurement_BED.get(i).getSvid();

                // Calculate Carrier phase from getAccumulatedDeltaRangeMeters
                l1 = measurement_BED.get(i).getAccumulatedDeltaRangeMeters() / Wavelewngth_L1;

                //use getPseudorangeRateMetersPerSecond to smooth carrier to nearest timestamp
                l1 -= frac*(measurement_BED.get(i).getPseudorangeRateMetersPerSecond() / Wavelewngth_L1);

                // Calculate Doppler from getPseudorangeRateMetersPerSecond
                d1 = -measurement_BED.get(i).getPseudorangeRateMetersPerSecond() / Wavelewngth_L1;

                // get CNR for the PRN
                s1 = measurement_BED.get(i).getCn0DbHz();
				
				//CMC Calculattion
                cmc_meas.cmc_val=c1- (l1*Wavelewngth_L1) ;
                cmc_meas.satID=prn;
                cmc_meas.Constellation_type=GnssStatus.CONSTELLATION_BEIDOU;
                cmc_meas.freq= Frequency_L1;

                /*Check AccumlatedDeltaRangeState to detect Cycle
                 * refer https://developer.android.com/reference/android/location/GnssMeasurement#ADR_STATE_HALF_CYCLE_REPORTED*/
                if((measurement_BED.get(i).getAccumulatedDeltaRangeState()%2 == 0)){
                    //cycleslip_flag_BED= Integer.parseInt(" ");
                    //l1 = Double.parseDouble("              ");
                    cycleslip_flag_BED = 1;
                    //cycleslip_flag_BED_L1_next_epoch = true;
                }
                /*else if (cycleslip_flag_BED_L1_next_epoch){
                    cycleslip_flag_BED = 1;
                    cycleslip_flag_BED_L1_next_epoch = false;
                }*/
                else
                    cycleslip_flag_BED = 0;

                cmc_meas.LLIflag = cycleslip_flag_BED;
                CMC_measurement.add(cmc_meas);

                // Print the BEIDOU Observation
                String formatStr_BEIDOU_L1 = "%s%02d%14.3f%1s%1s%14.3f%d%1s%14.3f%1s%1s%14.3f%1s%1s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_BEIDOU_L1, "C", prn, c1, "", "", l1, cycleslip_flag_BED, "", d1, "", "", s1, "", ""));
            }
        }

        //GALILEO Logging
        for(int i=0; i < measurement_GAL.size();i++)
        {

            // Initialize Obs Values
            l1 = 1.0e-9;c1 = 1.0e-9;s1 = 1.0e-9;d1 = 1.0e-9;
            tRxSeconds = 1.0e-9;tTxSeconds = 1.0e-9;tau = 1.0e-9;

            // used for cycle slip indicator
            int cycleslip_flag_GAL;

            // Check if L5 observation is available
            boolean GAL_L5_exist = false;
			
			//used for storing CMC values
			cmc_epoch_measurement cmc_meas = new cmc_epoch_measurement();

            // check if carrier frequency (L1) value is available in raw measurement.
            if (((Math.abs(measurement_GAL.get(i).getCarrierFrequencyHz() - Frequency_L1) <= 100) || (Float.isNaN(measurement_GAL.get(i).getCarrierFrequencyHz()))))
            {
                tRxSeconds = gpssow - measurement_GAL.get(i).getTimeOffsetNanos() * NS_TO_S;
                tTxSeconds = measurement_GAL.get(i).getReceivedSvTimeNanos() * NS_TO_S;

                //Compute the travel time, which will be eventually the pseudorange
                tau = check_week_crossover(tRxSeconds,tTxSeconds);

                // travel time * Speed of light
                c1 = tau * SPEED_OF_LIGHT;

                // use getPseudorangeRateMetersPerSecond to smooth pseudorange
                c1 -= frac*measurement_GAL.get(i).getPseudorangeRateMetersPerSecond();

                // get PRN for constellation
                prn = measurement_GAL.get(i).getSvid();

                // Calculate Carrier phase from getAccumulatedDeltaRangeMeters
                l1 = measurement_GAL.get(i).getAccumulatedDeltaRangeMeters() / Wavelewngth_L1;

                //use getPseudorangeRateMetersPerSecond to smooth carrier to nearest timestamp
                l1 -= frac*(measurement_GAL.get(i).getPseudorangeRateMetersPerSecond() / Wavelewngth_L1);

                // Calculate Doppler from getPseudorangeRateMetersPerSecond
                d1 = -measurement_GAL.get(i).getPseudorangeRateMetersPerSecond() / Wavelewngth_L1;

                // get CNR for the PRN
                s1 = measurement_GAL.get(i).getCn0DbHz();
				
				//CMC Calculattion
                cmc_meas.cmc_val=c1- (l1*Wavelewngth_L1) ;
                cmc_meas.satID=prn;
                cmc_meas.Constellation_type=GnssStatus.CONSTELLATION_GALILEO;
                cmc_meas.freq= Frequency_L1;


                /*Check AccumlatedDeltaRangeState to detect Cycle
                 * refer https://developer.android.com/reference/android/location/GnssMeasurement#ADR_STATE_HALF_CYCLE_REPORTED*/
                if((measurement_GAL.get(i).getAccumulatedDeltaRangeState()%2 == 0)){
                    //cycleslip_flag_GAL= Integer.parseInt(" ");
                    //l1 = Double.parseDouble("              ");
                    cycleslip_flag_GAL = 1;
                    //cycleslip_flag_GAL_L1_next_epoch = true;
                }
                /*else if (cycleslip_flag_GAL_L1_next_epoch){
                    cycleslip_flag_GAL = 1;
                    cycleslip_flag_GAL_L1_next_epoch = false;

                }*/
                else
                    cycleslip_flag_GAL = 0;

                cmc_meas.LLIflag = cycleslip_flag_GAL;
                CMC_measurement.add(cmc_meas);

                // Print the GALILEO L1 Observation
                String formatStr_GAL_L1 = "%s%02d%64s%14.3f%1s%1s%14.3f%d%1s%14.3f%1s%1s%14.3f%1s%1s";
                mFileWriter.write(String.format( Locale.US,formatStr_GAL_L1, "E",  prn,"",c1, "", "", l1, cycleslip_flag_GAL, "", d1, "", "", s1, "", ""));
            }
            for (int j = i+1; j < measurement_GAL.size() ; j++)
            {
                // Initialize Obs Values
                l5 = 1.0e-9;c5 = 1.0e-9;d5 = 1.0e-9;s5 = 1.0e-9;
                tRxSeconds = 1.0e-9;tTxSeconds = 1.0e-9;tau = 1.0e-9;

                // check if carrier frequency (L5) value is available in raw measurement
                if ((measurement_GAL.get(j).getCarrierFrequencyHz() - Frequency_L5 <= 100))
                {
                    if (measurement_GAL.get(i).getSvid() == measurement_GAL.get(j).getSvid() && (measurement_GAL.get(i).getConstellationType() == measurement_GAL.get(j).getConstellationType()))
                    {
                        tRxSeconds = gpssow - measurement_GAL.get(j).getTimeOffsetNanos() * NS_TO_S;
                        tTxSeconds = measurement_GAL.get(j).getReceivedSvTimeNanos() * NS_TO_S;

                        //Compute the travel time, which will be eventually the pseudorange
                        tau = check_week_crossover(tRxSeconds,tTxSeconds);

                        // travel time * Speed of light
                        c5 = tau * SPEED_OF_LIGHT;

                        // use getPseudorangeRateMetersPerSecond to smooth pseudorange
                        c5 = c5-frac*measurement_GAL.get(j).getPseudorangeRateMetersPerSecond();

                        // Calculate Carrier phase from getAccumulatedDeltaRangeMeters
                        l5 = measurement_GAL.get(j).getAccumulatedDeltaRangeMeters() / Wavelewngth_L5;

                        //use getPseudorangeRateMetersPerSecond to smooth carrier to nearest timestamp
                        l5 -= frac*(measurement_GAL.get(j).getPseudorangeRateMetersPerSecond() / Wavelewngth_L5);

                        // Calculate Doppler from getPseudorangeRateMetersPerSecond
                        d5 = -measurement_GAL.get(j).getPseudorangeRateMetersPerSecond() / Wavelewngth_L5;

                        // get CNR for the PRN
                        s5 = measurement_GAL.get(j).getCn0DbHz();

                        /*Check AccumlatedDeltaRangeState to detect Cycle
                         * refer https://developer.android.com/reference/android/location/GnssMeasurement#ADR_STATE_HALF_CYCLE_REPORTED*/
                        if((measurement_GAL.get(j).getAccumulatedDeltaRangeState()%2 == 0)){
                            //cycleslip_flag_GAL= Integer.parseInt(" ");
                            //l5 = Double.parseDouble("              ");
                            cycleslip_flag_GAL = 1;
                            //cycleslip_flag_GAL_L5_next_epoch = true;
                        }
                        /*else if (cycleslip_flag_GAL_L5_next_epoch){
                            cycleslip_flag_GAL = 1;
                            cycleslip_flag_GAL_L5_next_epoch = false;
                        }*/
                        else
                            cycleslip_flag_GAL = 0;

                        // Print the GALILEO L5 Observation
                        String formatStr_GAL_L5 = "%14.3f%1s%1s%14.3f%d%1s%14.3f%1s%1s%14.3f%1s%1s%n";
                        mFileWriter.write(String.format( Locale.US,formatStr_GAL_L5, c5, "", "", l5, cycleslip_flag_GAL, "", d5, "", "", s5, "", ""));
                        GAL_L5_exist = true;
                    }
                }
            }
            if((GAL_L5_exist == false)&& ((Math.abs(measurement_GAL.get(i).getCarrierFrequencyHz() - Frequency_L1) <= 100)
                    || (Float.isNaN(measurement_GAL.get(i).getCarrierFrequencyHz()))))
            {
                mFileWriter.newLine();
            }
        }
		
		//Pass CMC vector to plotting
		mcmcfragment.updateCMCTab(CMC_measurement, timeInSeconds);
        CMC_measurement.clear();
    }

    private void logException(String errorMessage, Exception e) {
        Log.e(GnssContainer.TAG + TAG, errorMessage, e);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void logError(String errorMessage) {
        Log.e(GnssContainer.TAG + TAG, errorMessage);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Implements a {@link FileFilter} to delete files that are not in the
     * {@link FileToDeleteFilter#mRetainedFiles}.
     */
    private static class FileToDeleteFilter implements FileFilter {
        private final List<File> mRetainedFiles;

        public FileToDeleteFilter(File... retainedFiles) {
            this.mRetainedFiles = Arrays.asList(retainedFiles);
        }

        /**
         * Returns {@code true} to delete the file, and {@code false} to keep the file.
         *
         * <p>Files are deleted if they are not in the {@link FileToDeleteFilter#mRetainedFiles} list.
         */
        @Override
        public boolean accept(File pathname) {
            if (pathname == null || !pathname.exists()) {
                return false;
            }
            if (mRetainedFiles.contains(pathname)) {
                return false;
            }
            return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
        }
    }

    /** This routine will check the validitiy of pseudorange and then used for Rinex Logging */
    Vector<GnssMeasurement> pseudorange_check(GnssClock clock, Vector<GnssMeasurement>  measurement_nonfilter)
    {
        Vector<GnssMeasurement>  measurement = new Vector();
        for(int i=0;i<measurement_nonfilter.size();i++)
        {
            // Initialize the parameters
            c1=0.0;tRxSeconds=0.0;tTxSeconds=0.0;

            gpsweek = Math.floor((-fullBiasNanos * NS_TO_S / GPS_WEEKSECS));
            local_est_GPS_time = clock.getTimeNanos() - (fullBiasNanos + BiasNanos);
            gpssow = (local_est_GPS_time * NS_TO_S) - (gpsweek * GPS_WEEKSECS);

            // Check GPS and GALILEO
            if((measurement_nonfilter.get(i).getConstellationType()==GnssStatus.CONSTELLATION_GPS)||
                    (measurement_nonfilter.get(i).getConstellationType()==GnssStatus.CONSTELLATION_GALILEO)) {
                tTxSeconds = measurement_nonfilter.get(i).getReceivedSvTimeNanos() * NS_TO_S;
                tRxSeconds = gpssow - measurement_nonfilter.get(i).getTimeOffsetNanos() * NS_TO_S;
            }
            // check GLONASS
        /*    else if((measurement_nonfilter.get(i).getConstellationType()==GnssStatus.CONSTELLATION_GLONASS)) {
                if (measurement_nonfilter.get(i).getSvid() < 93) {
                    tRxSeconds = gpssow - measurement_nonfilter.get(i).getTimeOffsetNanos() * NS_TO_S;
                    tTxSeconds = measurement_nonfilter.get(i).getReceivedSvTimeNanos() * NS_TO_S;
                    DateTime gpst_epoch = GetFromGps(gpsweek, gpssow);
                    tTxSeconds = glot_to_gpst(gpst_epoch,tTxSeconds) + CURRENT_GPS_LEAP_SECOND;
                }
            }*/
            // check BEIDOU
            else if((measurement_nonfilter.get(i).getConstellationType()==GnssStatus.CONSTELLATION_BEIDOU)) {
                tTxSeconds = measurement_nonfilter.get(i).getReceivedSvTimeNanos() * NS_TO_S + BDST_TO_GPST;
                tRxSeconds = gpssow - measurement_nonfilter.get(i).getTimeOffsetNanos() * NS_TO_S;
            }

            tau = check_week_crossover(tRxSeconds,tTxSeconds);

            // Travel time X Speed of Light
            c1 = tau * SPEED_OF_LIGHT;

            // check the range of Pseudorange and store if valid
            if(c1  < 30e6 && c1  > 10e6)
            {
                measurement.add(measurement_nonfilter.get(i));
            }
        }
        return measurement;
    }

    /**Routine for Rinex File Header Printing*/
    public void RinexHeader(GnssClock clock, GnssMeasurement measurement)
            throws IOException {
        synchronized (mFileLock) {

            SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm");

            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date now = new Date();

            // initialize the contents of the file
            try {

                //version line
                String formatStr_ver = "%9.2f%11s%-20s%-20s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_ver, VERSION_TAG, "", TYPE, SATSYS, "RINEX VERSION / TYPE"));

                // Pgm line
                SimpleDateFormat formatterpgm = new SimpleDateFormat("yyyyMMdd HHmmss z");
                formatterpgm.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formatStr_pgm = "%-20s%-20s%-20s%-20s%n";
                mFileWriter.write(String.format(formatStr_pgm, PGM, RUN_BY, formatterpgm.format(now), "PGM / RUN BY / DATE"));
                // Comment line
                mFileWriter.write(String.format("%-60s", "************************************************************COMMENT"));
                mFileWriter.newLine();
                mFileWriter.write(String.format("%-60s", "This file was generated by the GNSS/IMU Logger App          COMMENT"));
                mFileWriter.newLine();
                mFileWriter.write(String.format("%-60s", "for Android devices (Version v1.0.0.0). If you encounter    COMMENT"));
                mFileWriter.newLine();
                mFileWriter.write(String.format("%-60s", "any issues, please send an email to ista-software@unibw.de  COMMENT"));
                mFileWriter.newLine();
                mFileWriter.write(String.format("%-60s", "************************************************************COMMENT"));
                mFileWriter.newLine();
                // Marker name
                String formatStr_marker_name = "%-60s%-20s%n";
                mFileWriter.write(String.format(formatStr_marker_name, RUN_BY, "MARKER NAME"));

                // Marker type
                String formatStr_marker_type = "%-60s%-20s%n";
                mFileWriter.write(String.format(formatStr_marker_type, MARKER_TYPE, "MARKER TYPE"));

                // OBSERVER
                String formatStr_observer = "%-20s%-40s%-20s%n";
                mFileWriter.write(String.format(formatStr_observer, OBSERVER, AGENCY, "OBSERVER / AGENCY"));

                // Receiver type
                String formatStr_receiver_type = "%-20s%-20s%-20s%-20s%n";
                mFileWriter.write(String.format(formatStr_receiver_type, RECEIVER_NUMBER, Build.MANUFACTURER, Build.MODEL, "REC # / TYPE / VERS"));

                // Antenna type
                String formatStr_approx_antenna_type = "%-20s%-40s%-20s%n";
                mFileWriter.write(String.format(formatStr_approx_antenna_type, ANTENNA_NUMBER, Build.MODEL, "ANT # / TYPE"));

                // Approx Position
                String formatStr_approx_pos = "%14.4f%14.4f%14.4f%18s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_approx_pos,ecef[0],ecef[1],ecef[2],"","APPROX POSITION XYZ"));
                //mFileWriter.write(String.format(formatStr_approx_pos, 0.0000, 0.0000, 0.0000, "", "APPROX POSITION XYZ"));

                // Antenna
                String formatStr_antenna = "%14.4f%14.4f%14.4f%18s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_antenna, 0.0000, 0.0000, 0.0000, "", "ANTENNA: DELTA H/E/N"));

                //constellation_header
                String formatStr_constellation_header = "%s%2s%3d%1s%-52s%1s%-20s%n";
                mFileWriter.write(String.format(formatStr_constellation_header, "G", "", 8, "", "C1C L1C D1C S1C C5Q L5Q D5Q S5Q", "", "SYS / # / OBS TYPES"));
                mFileWriter.write(String.format(formatStr_constellation_header, "R", "", 4, "", "C1C L1C D1C S1C", "", "SYS / # / OBS TYPES"));
                mFileWriter.write(String.format(formatStr_constellation_header, "E", "", 12, "", "C1B L1B D1B S1B C1C L1C D1C S1C C5Q L5Q D5Q S5Q", "", "SYS / # / OBS TYPES"));
                mFileWriter.write(String.format(formatStr_constellation_header, "C", "", 4, "", "C2I L2I D2I S2I", "", "SYS / # / OBS TYPES"));
                mFileWriter.write(String.format(formatStr_constellation_header, "J", "", 8, "", "C1C L1C D1C S1C C5Q L5Q D5Q S5Q", "", "SYS / # / OBS TYPES"));

                // Time of First Obs;
                gpsweek = Math.floor(-clock.getFullBiasNanos() * NS_TO_S / GPS_WEEKSECS);
                local_est_GPS_time = clock.getTimeNanos() + measurement.getTimeOffsetNanos() - (clock.getFullBiasNanos() + clock.getBiasNanos());
                gpssow = local_est_GPS_time * NS_TO_S - gpsweek * GPS_WEEKSECS;
                //tRxSeconds = gpssow - measurement_new.get(0).getTimeOffsetNanos() * NS_TO_S;
                frac = gpssow - (int)(gpssow + 0.5);
                DateTime epoch = GetFromGps(gpsweek, gpssow);
                //tRxGNSS = clock.getTimeNanos() + measurement.getTimeOffsetNanos() - (clock.getFullBiasNanos() + clock.getBiasNanos());
                //weekNumberNanos = Math.floor((-clock.getFullBiasNanos() * NS_TO_S / GPS_WEEKSECS));
                //tRxSeconds = tRxGNSS * NS_TO_S - weekNumberNanos * GPS_WEEKSECS;
                //DateTime epoch = GetFromGps(weekNumberNanos, tRxSeconds);
                int year = epoch.getYear();
                int month = epoch.getMonthOfYear();
                int date = epoch.getDayOfMonth();
                int hour = epoch.getHourOfDay();
                int min = epoch.getMinuteOfHour();
                int second = epoch.getSecondOfMinute();
                double milli = epoch.getMillisOfSecond();
                double sec = second + milli*10e-4;

                String formatStr_first_epoch = "%6d%6d%6d%6d%6d%13.7f%5s%3s%26s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_first_epoch, year, month, date, hour, min, sec, "", "GPS", "TIME OF FIRST OBS"));

                //GLONASS SLOT
                String formatStr_glonass_slot = "%3d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%-20s%n";
                mFileWriter.write(String.format(formatStr_glonass_slot, 24, "", "R", 1, "", 1, "",
                        "R", 2, "", -4, "",
                        "R", 3, "", 5, "",
                        "R", 4, "", 6, "",
                        "R", 5, "", 1, "",
                        "R", 6, "", -4, "",
                        "R", 7, "", 5, "",
                        "R", 8, "", 6, "",
                        "GLONASS SLOT / FRQ #"));
                String formatStr_glonass_slot2 = "%3s%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%-20s%n";
                mFileWriter.write(String.format(formatStr_glonass_slot2, "", "", "R", 9, "", -2, "",
                        "R", 10, "", -7, "",
                        "R", 11, "", 0, "",
                        "R", 12, "", -1, "",
                        "R", 13, "", -2, "",
                        "R", 14, "", -7, "",
                        "R", 15, "", 0, "",
                        "R", 16, "", -1, "",
                        "GLONASS SLOT / FRQ #"));
                String formatStr_glonass_slot3 = "%3s%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%s%02d%1s%2d%1s%-20s%n";
                mFileWriter.write(String.format(formatStr_glonass_slot3, "", "", "R", 17, "", 4, "",
                        "R", 18, "", -3, "",
                        "R", 19, "", 3, "",
                        "R", 20, "", 2, "",
                        "R", 21, "", 4, "",
                        "R", 22, "", -3, "",
                        "R", 23, "", 3, "",
                        "R", 24, "", 2, "",
                        "GLONASS SLOT / FRQ #"));
                //Phase Shift
                String formatStr_phase_shift_G_L1C = "%s%1s%3s%1s%8s%46s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_phase_shift_G_L1C, "G", "", "L1C", "", "", "", "SYS / PHASE SHIFT"));
                String formatStr_phase_shift_G_L5Q = "%s%1s%3s%1s%8.5f%46s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_phase_shift_G_L5Q, "G", "", "L5Q", "", -0.25000, "", "SYS / PHASE SHIFT"));
                String formatStr_phase_shift_R_L1C = "%s%1s%3s%1s%8s%46s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_phase_shift_R_L1C, "R", "", "L1C", "", "", "", "SYS / PHASE SHIFT"));
                String formatStr_phase_shift_E_L1B = "%s%1s%3s%1s%8s%46s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_phase_shift_E_L1B, "E", "", "L1B", "", "", "", "SYS / PHASE SHIFT"));
                String formatStr_phase_shift_E_L1C = "%s%1s%3s%1s%8.5f%46s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_phase_shift_E_L1C, "E", "", "L1C", "", 0.50000, "", "SYS / PHASE SHIFT"));
                String formatStr_phase_shift_E_L5Q = "%s%1s%3s%1s%8.5f%46s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_phase_shift_E_L5Q, "E", "", "L5Q", "", -0.25000, "", "SYS / PHASE SHIFT"));
                String formatStr_phase_shift_C_L2I = "%s%1s%3s%1s%8s%46s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_phase_shift_C_L2I, "C", "", "L2I", "", "", "", "SYS / PHASE SHIFT"));
                String formatStr_phase_shift_J_L1C = "%s%1s%3s%1s%8s%46s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_phase_shift_J_L1C, "J", "", "L1C", "", "", "", "SYS / PHASE SHIFT"));
                String formatStr_phase_shift_J_L5Q = "%s%1s%3s%1s%8.5f%46s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_phase_shift_J_L5Q, "J", "", "L5Q", "", -0.25000, "", "SYS / PHASE SHIFT"));

                //GLONASS COD/PHS/BIS
                String formatStr_glonass_code = "%1s%3s%1s%8.3f%1s%3s%1s%8.3f%1s%3s%1s%8.3f%1s%3s%1s%8.3f%8s%-20s%n";
                mFileWriter.write(String.format( Locale.US,formatStr_glonass_code, "", "C1C", "", 0.000, "", "C1P", "", 0.000, "", "C2C", "", 0.000, "", "C2P", "", 0.000, "", "GLONASS COD/PHS/BIS"));

                //END of Header
                String formatStr_endheader = "%60s%-20s%n";
                mFileWriter.write(String.format(formatStr_endheader, "", "END OF HEADER"));

            } catch (IOException e) {
                logException("Count not initialize file", e);
                return;
            }
        }
    }
	
	// class for CMC Measurement
	public class cmc_epoch_measurement{
        double cmc_val;
        double freq;
        Integer satID;
        int Constellation_type;
        int LLIflag;
        double epoch;
    }

    // class for CMC Measurement
    public class adr_state_epoch_measurement{
        int state;
        double freq;
        Integer satID;
        int Constellation_type;
        double epoch;
    }
	
	/**
     * Sets {@link PlotFragment} for receiving Gnss measurement and residual computation results for
     * plot
     */
    public void setCodeMinusCarrierFragment(CodeminusCarrierFragment cmcfragment) {
        this.mcmcfragment = cmcfragment;
    }

    /**
     * Sets {@link PlotFragment} for receiving Gnss measurement and residual computation results for
     * plot
     */
    public void setinternalclockoffset(internalclockoffsetFragment internalclockFragment) {
        this.minternalclockoffset = internalclockFragment;
    }

    /**Routine to get GPS time from Weeknumber and seconds*/
    DateTime GetFromGps(double weeknumber, double seconds)
    {
        int int_weeknumber = (int) weeknumber;
        int int_seconds = (int) seconds;
        int milli_second = (int)((seconds - (int) seconds) * Math.pow(10,3));
        DateTime datum = new DateTime(1980,1,6,0,0,0,0);
        DateTime week = datum.plusDays(int_weeknumber * 7);
        DateTime time = week.plusSeconds(int_seconds);
        time = time.plusMillis(milli_second);
        //DateTime  time = sec.plusMillis(int_microsecond);
        return time;
    }

    /**Routine to check week cross over to calculate tau from Receive and Transmit Time*/
    public static double check_week_crossover(double tRxSeconds,double tTxSeconds)
    {
        double del_sec= 0.0;
        double rho_sec;
        tau =  tRxSeconds - tTxSeconds;
        if(tau>GPS_WEEKSECS/2)
            del_sec = round(tau/GPS_WEEKSECS)*GPS_WEEKSECS;
            rho_sec = tau - del_sec;

        if(rho_sec > 10)
        {
            tau = 0.0; }
        else
            tau = rho_sec;

        return tau;
    }

    /**Routine to get GPS Time from GLONASS Time*/
    public static double glot_to_gpst(DateTime gpst_current_epoch, double tod_seconds)
    {
        double tow_sec = 0;
        double tod_sec_frac = 0.0;
        int tod_sec = 0;

        tod_sec = (int) tod_seconds;
        tod_sec_frac = tod_seconds - (int) tod_seconds;

        DateTime glo_epoch = new DateTime(gpst_current_epoch.getYear(),
                gpst_current_epoch.getMonthOfYear(),
                gpst_current_epoch.getDayOfMonth(),
                gpst_current_epoch.getHourOfDay()+3,
                gpst_current_epoch.getMinuteOfHour(),
                gpst_current_epoch.getSecondOfMinute() - CURRENT_GPS_LEAP_SECOND,
                gpst_current_epoch.getMillisOfSecond());

        DateTime glo_tod = new DateTime(glo_epoch.getYear(),
                glo_epoch.getMonthOfYear(),
                glo_epoch.getDayOfMonth(),
                glo_epoch.getHourOfDay()-glo_epoch.getHourOfDay(),
                glo_epoch.getMinuteOfHour()-glo_epoch.getMinuteOfHour(),
                glo_epoch.getSecondOfMinute()-glo_epoch.getSecondOfMinute());


        DateTime glo_tod_adjusted = glo_tod.plusSeconds(tod_sec);


        int day_of_week_sec = glo_tod_adjusted.getDayOfWeek()*GLO_DAYSSECS;

        tow_sec = day_of_week_sec + tod_seconds - GLOT_TO_UTC ;

        return tow_sec;

    }
}
