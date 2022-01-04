package com.ista.android.apps.location.gps.gnsslogger;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.NumberFormat;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.app.Dialog;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.apps.location.gps.gnsslogger.BuildConfig;
import com.google.android.apps.location.gps.gnsslogger.R;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

public class SensorFragment extends Fragment implements SensorEventListener {


    Switch Calibrated_logSensor;
    Switch Uncalibrated_logSensor;
    Switch logRnx_calibrated_Sensor;
    Switch logRnx_uncalibrated_Sensor;
    boolean sensorState = false;
    boolean lastSensorState = false;
    boolean sensorState2 = false;
    boolean lastSensorState2 = false;

    final long starttime = SystemClock.uptimeMillis();

    Chronometer sensorChronometer;


    Boolean _areLecturesLoaded =false;

    RinexLogger mRinexLogger;
    boolean header_exist = false;

    //the Sensor Manager
    private SensorManager sManager;
    private Sensor mSensor;
    private File csv = null;
    private File csv2 = null;
    

    XYSeries accX = new XYSeries("Acceleromenter X-axis");
    XYSeries accY = new XYSeries("Y-axis");
    XYSeries accZ = new XYSeries("Z-axis");

    XYSeries gyroX = new XYSeries("Gyroscope X-axis");
    XYSeries gyroY = new XYSeries("Y-axis");
    XYSeries gyroZ = new XYSeries("Z-axis");

    XYSeries magX = new XYSeries("Magnetometer X-axis");
    XYSeries magY = new XYSeries("Y-axis");
    XYSeries magZ = new XYSeries("Z-axis");

    GraphicalView accView;
    GraphicalView gyroView;
    GraphicalView magView;

    private int accCount = 0;
    private int gyroCount = 0;
    private int magCount = 0;
    public static long gpstimeoffset = 0;
    long gpstimemilli =0 ;
    XYMultipleSeriesRenderer accMultipleRenderer;
    XYMultipleSeriesRenderer gyroMultipleRenderer;
    XYMultipleSeriesRenderer magMultipleRenderer;

    long test;

    public File createSensorLog_calibration() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String state = Environment.getExternalStorageState();
        File folder;
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File folder_root = new File(Environment.getExternalStorageDirectory(), "GNSS-INS Logger");
            folder_root.mkdir();
            folder = new File(folder_root, "Calibr_IMU_Log");
            folder.mkdir();
        }
        else {
            return null;
        }
        String filedate = dateFormat.format(new Date());
        String filename = Build.MODEL + "_IMU_" + filedate + "." + "txt";
        csv = new File(folder, filename);// Filename
        return csv;
    }
    public File createSensorLog_uncalibration() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String state = Environment.getExternalStorageState();
        File folder;
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File folder_root = new File(Environment.getExternalStorageDirectory(), "GNSS-INS Logger");
            folder_root.mkdir();
            folder = new File(folder_root, "Uncalibr_IMU_Log");
            folder.mkdir();
        }
        else {
            return null;
        }
        String filedate = dateFormat.format(new Date());
        String filename2 = Build.MODEL + "_IMU_" + filedate + "." + "txt";
        csv2 = new File(folder, filename2);// Filename
        return csv2;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor, container, false);
        super.onCreate(savedInstanceState);


        //timestampmag = (TextView) view.findViewById(R.id.timestampmag);


        //test = (TextView) findViewById(R.id.textView2);
        //get a hook to the sensor service
        sManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        DateFormat df = new SimpleDateFormat("yyyyddMMhhmmss");
        String filename = "Sensorlog" + "_" + df.format(new Date()) + "." + "txt";

    /*    Button startBtn = (Button) view.findViewById(R.id.sendEmail);
        startBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sendEmail();
            }
            // graph.addSeries(series);
        });*/

        // From here the chartEngine is set up        
        // Renderers that control the accelerometer graphs

        Calibrated_logSensor = (Switch) view.findViewById(R.id.calibrsensorLog);
        Uncalibrated_logSensor = (Switch) view.findViewById(R.id.uncalibrsensorLog);
        logRnx_calibrated_Sensor = (Switch) view.findViewById(R.id.calibrsensorRnxLog);
        logRnx_uncalibrated_Sensor = (Switch) view.findViewById(R.id.uncalibrsensorRnxLog);

        XYSeriesRenderer accXRenderer = new XYSeriesRenderer();
        accXRenderer.setColor(Color.BLUE);
        accXRenderer.setLineWidth(8);

        XYSeriesRenderer accYRenderer = new XYSeriesRenderer();
        accYRenderer.setColor(Color.RED);
        accYRenderer.setLineWidth(8);

        XYSeriesRenderer accZRenderer = new XYSeriesRenderer();
        accZRenderer.setColor(Color.GREEN);
        accZRenderer.setLineWidth(5);

        //Dataset that host the series which are continuously appended with sensor values

        XYMultipleSeriesDataset accDataSet = new XYMultipleSeriesDataset();
        accDataSet.addSeries(accX);
        accDataSet.addSeries(accY);
        accDataSet.addSeries(accZ);

        //the MultipleSeriesRenderer that combines the single graphs

        accMultipleRenderer = new XYMultipleSeriesRenderer();
        accMultipleRenderer.addSeriesRenderer(accXRenderer);
        accMultipleRenderer.addSeriesRenderer(accYRenderer);
        accMultipleRenderer.addSeriesRenderer(accZRenderer);
        accMultipleRenderer.setShowGrid(true);
        accMultipleRenderer.setGridLineWidth(8);
        accMultipleRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        accMultipleRenderer.setLegendTextSize(25);
        accMultipleRenderer.setLabelsTextSize(25);
        accMultipleRenderer.setYLabelsColor(0,Color.BLACK);
        accMultipleRenderer.setXLabelsColor(Color.BLACK);
        accMultipleRenderer.setZoomEnabled(true, true);
        accMultipleRenderer.setPanEnabled(false, true);
        accMultipleRenderer.setClickEnabled(true);
        accMultipleRenderer.setYTitle("m/s^2");
        accMultipleRenderer.setAxisTitleTextSize(25);
        accMultipleRenderer.setXTitle("                                                                                            Epoch (milliseconds)");
        accMultipleRenderer.setXLabels(0);
        accMultipleRenderer.setChartTitle("Calibrated Accelerometer vs Time");
        accMultipleRenderer.setChartTitleTextSize(30);

        //set up the Linear layout and add the graphs

        LinearLayout accLayout = view.findViewById(R.id.plotAcc);
        accView = ChartFactory.getLineChartView(getContext(), accDataSet, accMultipleRenderer);
        accLayout.addView(accView);

        // Renderers that control the gyroscope graphs

        XYSeriesRenderer gyroXRenderer = new XYSeriesRenderer();
        gyroXRenderer.setColor(Color.BLUE);
        gyroXRenderer.setLineWidth(8);

        XYSeriesRenderer gyroYRenderer = new XYSeriesRenderer();
        gyroYRenderer.setColor(Color.RED);
        gyroYRenderer.setLineWidth(8);

        XYSeriesRenderer gyroZRenderer = new XYSeriesRenderer();
        gyroZRenderer.setColor(Color.GREEN);
        gyroZRenderer.setLineWidth(5);

        //Dataset that host the series which are continuously appended with sensor values

        XYMultipleSeriesDataset gyroDataSet = new XYMultipleSeriesDataset();
        gyroDataSet.addSeries(gyroX);
        gyroDataSet.addSeries(gyroY);
        gyroDataSet.addSeries(gyroZ);

        //the MultipleSeriesRenderer that combines the single graphs

        gyroMultipleRenderer = new XYMultipleSeriesRenderer();
        gyroMultipleRenderer.addSeriesRenderer(gyroXRenderer);
        gyroMultipleRenderer.addSeriesRenderer(gyroYRenderer);
        gyroMultipleRenderer.addSeriesRenderer(gyroZRenderer);
        gyroMultipleRenderer.setShowGrid(true);
        gyroMultipleRenderer.setGridLineWidth(8);
        gyroMultipleRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        gyroMultipleRenderer.setLegendTextSize(25);
        gyroMultipleRenderer.setLabelsTextSize(25);
        gyroMultipleRenderer.setYLabelsColor(0,Color.BLACK);
        gyroMultipleRenderer.setXLabelsColor(Color.BLACK);
        gyroMultipleRenderer.setZoomEnabled(true, true);
        gyroMultipleRenderer.setPanEnabled(false, true);
        gyroMultipleRenderer.setClickEnabled(true);
        gyroMultipleRenderer.setYTitle("rad/s");
        gyroMultipleRenderer.setAxisTitleTextSize(25);
        gyroMultipleRenderer.setXTitle("                                                                                            Epoch (milliseconds)");
        gyroMultipleRenderer.setXLabels(0);
        gyroMultipleRenderer.setChartTitle("Calibrated Gyroscope vs Time");
        gyroMultipleRenderer.setChartTitleTextSize(30);


        //set up the Linear layout and add the graphs

        LinearLayout gyroLayout = view.findViewById(R.id.plotGyro);
        gyroView = ChartFactory.getLineChartView(getContext(), gyroDataSet, gyroMultipleRenderer);
        gyroLayout.addView(gyroView);

        //and the same for the magnetometer rendering        
        XYSeriesRenderer magXRenderer = new XYSeriesRenderer();
        magXRenderer.setLineWidth(8);
        magXRenderer.setColor(Color.BLUE);

        XYSeriesRenderer magYRenderer = new XYSeriesRenderer();
        magYRenderer.setColor(Color.RED);
        magYRenderer.setLineWidth(8);

        XYSeriesRenderer magZRenderer = new XYSeriesRenderer();
        magZRenderer.setColor(Color.GREEN);
        magZRenderer.setLineWidth(5);

        XYMultipleSeriesDataset magDataSet = new XYMultipleSeriesDataset();
        magDataSet.addSeries(magX);
        magDataSet.addSeries(magY);
        magDataSet.addSeries(magZ);

        magMultipleRenderer = new XYMultipleSeriesRenderer();
        magMultipleRenderer.addSeriesRenderer(magXRenderer);
        magMultipleRenderer.addSeriesRenderer(magYRenderer);
        magMultipleRenderer.addSeriesRenderer(magZRenderer);
        magMultipleRenderer.setShowGrid(true);
        magMultipleRenderer.setGridLineWidth(8);
        magMultipleRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        magMultipleRenderer.setLegendTextSize(25);
        gyroMultipleRenderer.setLabelsTextSize(25);
        magMultipleRenderer.setYLabelsColor(0,Color.BLACK);
        magMultipleRenderer.setXLabelsColor(Color.BLACK);
        magMultipleRenderer.setZoomEnabled(true, true);
        magMultipleRenderer.setPanEnabled(false, true);
        magMultipleRenderer.setClickEnabled(true);
        magMultipleRenderer.setXTitle("                                                                                             Epoch (milliseconds)");
        magMultipleRenderer.setYTitle("µT");
        magMultipleRenderer.setAxisTitleTextSize(25);
        magMultipleRenderer.setXLabels(0);

      //  LinearLayout magLayout = view.findViewById(R.id.plotMag);
        magView = ChartFactory.getLineChartView(getContext(), magDataSet, magMultipleRenderer);
      //  magLayout.addView(magView);

        sensorChronometer = (Chronometer) view.findViewById(R.id.sensor_chronometer);
        TextView chronoSensorText = (TextView) view.findViewById(R.id.sensor_timer_display);
        chronoSensorText.setText("Log Duration:");

        return view;

    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && !_areLecturesLoaded ) {
            _areLecturesLoaded = true;
            final Dialog axisInfo = new Dialog(getContext());
            axisInfo.setContentView(R.layout.info_axis);
            ImageView axisImage = (ImageView) axisInfo.findViewById(R.id.axis_image);
            axisImage.setImageResource(R.drawable.coordinates); // place axis image here
            axisImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    axisInfo.cancel();
                }
            });
            axisInfo.show();
        }
    }

    //when this Activity starts
    @Override
    public void onResume() {
        super.onResume();
        /*register the sensor listener to listen to the gyroscope sensor, use the
         * callbacks defined in this class, and gather the sensor information as
         * quick as possible*/
        if (Uncalibrated_logSensor.isChecked() || logRnx_uncalibrated_Sensor.isChecked()) {
            sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED), SensorManager.SENSOR_DELAY_FASTEST);
            sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), SensorManager.SENSOR_DELAY_FASTEST);
            sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SensorManager.SENSOR_DELAY_FASTEST); }
        else {
            sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
            sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SensorManager.SENSOR_DELAY_FASTEST);
            }
    }

    //When this Activity isn't visible anymore
    // @Override
    //public void onStop()
    // {

    // }


    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        //Do nothing
    }

    public void writeSensorLog_calibr(long utc_timestamp,long gps_timestamp, int sens_type, String ax, String ay, String az, File csv) throws IOException {

        FileWriter file_writer = new FileWriter(csv, true);
        FileWriter file_writer_time = new FileWriter(csv,true);

        if (header_exist == false) {
            String header_l1 = "*************************************************************************************************************************" + "\n";
            String header_l2 = "Sensor Log file is generated using GNSS/IMU Logger (Version v2.0.0.0)"             + "\n";
            String header_l3 = "Sensor Flag- 1 : Accelerometer [m/s2]  2: Gyroscope [rad/s]  3: Magnetometer [μT]" + "\n";
            String header_l4 = "UNIX Epoch timestamp-utc,timestamp-gps, sensor Flag,x-axis,y-axis,z-axis" + "\n";
            String header_l5 = "N/A (GPS Time epoch not Available) :  Sensor Log waiting for GPS Time from RINEX Logger" + "\n";
            String header_l6 = "*************************************************************************************************************************" + "\n";
            file_writer.append(header_l1);
            file_writer.append(header_l2);
            file_writer.append(header_l3);
            file_writer.append(header_l4);
            file_writer.append(header_l5);
            file_writer.append(header_l6);
            header_exist = true;
        }

        if(gps_timestamp == 0){
        String s = String.format("%s,   %15s,     %s,     %15s,     %15s,     %15s %n", utc_timestamp,"N/A", sens_type, ax, ay, az);
        file_writer.append(s);}
        else{
            String s = String.format("%s,   %15s,     %s,     %15s,     %15s,     %15s %n", utc_timestamp,gps_timestamp, sens_type, ax, ay, az);
            file_writer.append(s);}
        //String offset = String.format("%s,%s,%s %n",currentTimeMillis,eventtimestamp,elapsedRealtimeNanos);
        // file_writer_time.append(offset);
        file_writer.close();
        //file_writer_time.close();
    }

    public void writeSensorLog_uncalibr_withrnx(long utc_timestamp,long gps_timestamp, int sens_type, String ax, String ay, String az, File csv) throws IOException {

        FileWriter file_writer = new FileWriter(csv, true);
        FileWriter file_writer_time = new FileWriter(csv,true);

        if (header_exist == false) {
            String header_l1 = "*************************************************************************************************************************" + "\n";
            String header_l2 = "Sensor Log file is generated using GNSS/IMU Logger (Version v2.0.0.0)"             + "\n";
            String header_l3 = "Sensor Flag- 1 : Accelerometer [m/s2]  2: Gyroscope [rad/s]  3: Magnetometer [μT]" + "\n";
            String header_l4 = "UNIX Epoch timestamp-utc,timestamp-gps, sensor Flag,x-axis,y-axis,z-axis" + "\n";
            String header_l5 = "N/A (GPS Time epoch not Available) :  Sensor Log waiting for GPS Time from RINEX Logger" + "\n";
            String header_l6 = "*************************************************************************************************************************" + "\n";
            file_writer.append(header_l1);
            file_writer.append(header_l2);
            file_writer.append(header_l3);
            file_writer.append(header_l4);
            file_writer.append(header_l5);
            file_writer.append(header_l6);
            header_exist = true;
        }

        if(gps_timestamp == 0){
            String s = String.format("%s,   %15s,     %s,     %15s,     %15s,     %15s %n", utc_timestamp,"N/A", sens_type, ax, ay, az);
            file_writer.append(s);}
        else{
            String s = String.format("%s,   %15s,     %s,     %15s,     %15s,     %15s %n", utc_timestamp,gps_timestamp, sens_type, ax, ay, az);
            file_writer.append(s);}
        //String offset = String.format("%s,%s,%s %n",currentTimeMillis,eventtimestamp,elapsedRealtimeNanos);
        // file_writer_time.append(offset);
        file_writer.close();
        //file_writer_time.close();
    }

    public void writeSensorLog_uncalibr(long sensor_event, int sens_type, String ax, String ay, String az, File csv) throws IOException {

        FileWriter file_writer = new FileWriter(csv, true);
        FileWriter file_writer_time = new FileWriter(csv,true);

        if (header_exist == false) {
            String header_l1 = "*************************************************************************************************************************" + "\n";
            String header_l2 = "Sensor Log file is generated using GNSS/IMU Logger (Version v2.0.0.0)"             + "\n";
            String header_l3 = "Sensor Flag- 1 : Accelerometer [m/s2]  2: Gyroscope [rad/s]  3: Magnetometer [μT]" + "\n";
            String header_l4 = "Sensor Event in Milliseconds, sensor Flag,x-axis,y-axis,z-axis" + "\n";
            String header_l5 = "Use only for calibration Purpose" + "\n";
            String header_l6 = "*************************************************************************************************************************" + "\n";
            file_writer.append(header_l1);
            file_writer.append(header_l2);
            file_writer.append(header_l3);
            file_writer.append(header_l4);
            file_writer.append(header_l5);
            file_writer.append(header_l6);
            header_exist = true;
        }
            String s = String.format("%15s,     %s,     %15s,     %15s,     %15s %n", sensor_event, sens_type, ax, ay, az);
            file_writer.append(s);
            file_writer.close();
        //file_writer_time.close();
    }

  protected void sendEmail() {

        DateFormat df = new SimpleDateFormat("yyyyddMMhhmmss");
        String emailSubject = "Sensorlog" + "_" + df.format(new Date()) + "." + "txt";
        File file = new File(Environment.getExternalStorageDirectory() + "/Sensorlogger/" + "Sensorlog" + ".txt");
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("*/*");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "RINEX-Log");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        //Uri fileURI = Uri.fromFile(mFile);
        //Context mContext = null;
        Uri fileURI = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".Provider", file);
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileURI);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //getUiComponent().startActivity(Intent.createChooser(emailIntent, "Send log.."));
        Log.i("Send email", "");
        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            Log.i("Finished sending email...", "");
        } catch (android.content.ActivityNotFoundException ex) {
            //Toast.makeText(MainActivity.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    void gnsstimeupdate( long gnsstimeoffset){
        gpstimeoffset = gnsstimeoffset;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(Uncalibrated_logSensor.isChecked() || Calibrated_logSensor.isChecked()){
            sensorState=true;
        }
        if(!(Uncalibrated_logSensor.isChecked() || Calibrated_logSensor.isChecked())){
            sensorState = false;
        }
        if(sensorState&& !lastSensorState){
            csv = createSensorLog_calibration();
            csv2 = createSensorLog_uncalibration();
            header_exist = false;
            sensorChronometer.setBase(SystemClock.elapsedRealtime());
            sensorChronometer.start();

            Toast.makeText(getContext(), "File opened: " + csv.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Toast.makeText(getContext(), "File opened: " + csv2.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        }
        lastSensorState = sensorState;
        //if sensor is unreliable, return void
        //if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
         //   return;
        //}
        if(logRnx_uncalibrated_Sensor.isChecked() || logRnx_calibrated_Sensor.isChecked()){
            sensorState2=true;
        }
        if(!(logRnx_uncalibrated_Sensor.isChecked() || logRnx_calibrated_Sensor.isChecked())){
            sensorState2 = false;
        }
        if(sensorState2&& !lastSensorState2){
            csv = createSensorLog_calibration();
            csv2 = createSensorLog_uncalibration();
            mRinexLogger.startNewLog();
            header_exist = false;
            sensorChronometer.setBase(SystemClock.elapsedRealtime());
            sensorChronometer.start();
        }
        lastSensorState2 = sensorState2;

        if(!(logRnx_uncalibrated_Sensor.isChecked() || logRnx_calibrated_Sensor.isChecked()) && !(Uncalibrated_logSensor.isChecked() || Calibrated_logSensor.isChecked())){
            sensorChronometer.stop();
        }

        Sensor sensor = event.sensor;

        long timeInMillis = System.currentTimeMillis() + ((event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L);
        long eventtimeMillis =  (event.timestamp ) / 1000000L;

        if(gpstimeoffset == 0){
            gpstimemilli = 0;
        }
        else
        {
            gpstimemilli = (gpstimeoffset + event.timestamp)/1000000L;
        }

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //TODO: get value
            int sensor_type = 1;

            accCount++;

            if (accCount % 2 == 0) {

                accX.add(timeInMillis, event.values[0]);
                accY.add(timeInMillis, event.values[1]);
                accZ.add(timeInMillis, event.values[2]);


                if (accCount > 1000) {
                    accX.remove(0);
                    accY.remove(0);
                    accZ.remove(0);
                }

                accView.repaint();
                if(accCount%500 ==0)
                    accMultipleRenderer.addXTextLabel(timeInMillis, Long.toString(SystemClock.uptimeMillis()-starttime));
            }
            if(Calibrated_logSensor.isChecked()){
                try {
                    writeSensorLog_calibr(timeInMillis,gpstimemilli,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(logRnx_calibrated_Sensor.isChecked()){
                try {
                    writeSensorLog_calibr(timeInMillis,gpstimemilli,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER_UNCALIBRATED) {
            //TODO: get value
            int sensor_type = 1;
            if(Uncalibrated_logSensor.isChecked()){
                try {
                    writeSensorLog_uncalibr(eventtimeMillis,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv2);
                    //writeTolog(sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]), timeInMillis);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(logRnx_uncalibrated_Sensor.isChecked()){
                try {
                    //writeSensorLog_uncalibr(eventtimeMillis,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv2);
                    writeSensorLog_uncalibr_withrnx(timeInMillis,gpstimemilli,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv2);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } }

        }
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //TODO: get values
            int sensor_type = 2;
            gyroCount++;

            if (gyroCount % 2 == 0) {

                gyroX.add(timeInMillis, event.values[0]);
                gyroY.add(timeInMillis, event.values[1]);
                gyroZ.add(timeInMillis, event.values[2]);

                if (gyroCount > 1000) {
                    gyroX.remove(0);
                    gyroY.remove(0);
                    gyroZ.remove(0);
                }

                gyroView.repaint();
            }
            if(accCount%500==0) {
                gyroMultipleRenderer.addXTextLabel(timeInMillis, Long.toString(SystemClock.uptimeMillis() - starttime));
            }
            if(Calibrated_logSensor.isChecked()){
                try {
                    writeSensorLog_calibr(timeInMillis,gpstimemilli,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(logRnx_calibrated_Sensor.isChecked()){
                try {
                    writeSensorLog_calibr(timeInMillis,gpstimemilli,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
            //TODO: get values
            int sensor_type = 2;
            if(Uncalibrated_logSensor.isChecked()){
                try {
                    writeSensorLog_uncalibr(eventtimeMillis,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv2);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(logRnx_uncalibrated_Sensor.isChecked()){
                try {
                    //writeSensorLog_uncalibr(eventtimeMillis,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv2);
                    writeSensorLog_uncalibr_withrnx(timeInMillis,gpstimemilli,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv2);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //TODO: get values
            int sensor_type = 3;
            magCount++;
            magX.add(timeInMillis, event.values[0]);
            magY.add(timeInMillis, event.values[1]);
            magZ.add(timeInMillis, event.values[2]);
            if(accCount%500==0) {
                magMultipleRenderer.addXTextLabel(timeInMillis, Long.toString(SystemClock.uptimeMillis() - starttime));
            }

            if (magCount > 180) {
                magX.remove(0);
                magY.remove(0);
                magZ.remove(0);
            }
            magView.repaint();

            if(Calibrated_logSensor.isChecked()){
                try {
                    writeSensorLog_calibr(timeInMillis,gpstimemilli,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(logRnx_calibrated_Sensor.isChecked()){
                try {
                    writeSensorLog_calibr(timeInMillis,gpstimemilli,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
            //TODO: get values
            int sensor_type = 3;
            if(Uncalibrated_logSensor.isChecked()){
                try {
                    writeSensorLog_uncalibr(eventtimeMillis,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv2);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(logRnx_uncalibrated_Sensor.isChecked()){
                try {
                    //writeSensorLog_uncalibr(eventtimeMillis,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv2);
                    writeSensorLog_uncalibr_withrnx(timeInMillis,gpstimemilli,sensor_type,Float.toString(event.values[0]), Float.toString(event.values[1]), Float.toString(event.values[2]),csv2);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    public void setRinexLogger(RinexLogger value) {
        mRinexLogger = value;
    }
}
