package com.ista.android.apps.location.gps.gnsslogger;

import android.graphics.Color;
import android.hardware.camera2.params.BlackLevelPattern;
import android.location.GnssClock;
import android.location.GnssMeasurementsEvent;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.location.GnssClock;

import com.google.android.apps.location.gps.gnsslogger.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Vector;


public class internalclockoffsetFragment extends Fragment {

    XYSeries timeoffsetcontainer = new XYSeries("Offset");
    XYSeries movingaveragetimeoffsetcontainer = new XYSeries("Moving Average Offset");

    GraphicalView TimeoffsetView;
    XYMultipleSeriesRenderer offsetMultipleRenderer;
    private double mInitialTimeSeconds = -1;
    private double mLastTimeReceivedSeconds = 0;
    Vector<Long> timeoffsetrecord = new Vector();
    private TextView mAnalysisView;
    private TextView mclassAnalysisView;
    EditText inputaveragewindow_val;
    String temp_inputaveragewindow_val;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_internalclockoffset, container, false);
        super.onCreate(savedInstanceState);

        //inputaveragewindow_val = view.findViewById(R.id.input_windowval);
        //temp_inputaveragewindow_val= inputaveragewindow_val.getText().toString();


        XYSeriesRenderer timeoffsetRenderer = new XYSeriesRenderer();
        timeoffsetRenderer.setColor(Color.BLUE);
        timeoffsetRenderer.setPointStyle(PointStyle.CIRCLE);
        timeoffsetRenderer.setPointStrokeWidth(10);
        timeoffsetRenderer.setLineWidth(8);

        XYSeriesRenderer movingaveragetimeoffsetRenderer = new XYSeriesRenderer();
        movingaveragetimeoffsetRenderer.setColor(Color.RED);
        movingaveragetimeoffsetRenderer.setLineWidth(8);

        XYMultipleSeriesDataset TimeOffsetDataSet = new XYMultipleSeriesDataset();
        TimeOffsetDataSet.addSeries(timeoffsetcontainer);
        TimeOffsetDataSet.addSeries(movingaveragetimeoffsetcontainer);

        //the MultipleSeriesRenderer that combines the single graphs

        offsetMultipleRenderer = new XYMultipleSeriesRenderer();
        offsetMultipleRenderer.addSeriesRenderer(timeoffsetRenderer);
        offsetMultipleRenderer.addSeriesRenderer(movingaveragetimeoffsetRenderer);
        offsetMultipleRenderer.setShowGrid(true);
        offsetMultipleRenderer.setGridLineWidth(8);
        offsetMultipleRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        offsetMultipleRenderer.setLegendTextSize(25);
        offsetMultipleRenderer.setLabelsTextSize(25);
        offsetMultipleRenderer.setYLabelsColor(0, Color.BLACK);
        offsetMultipleRenderer.setXLabelsColor(Color.BLACK);
        offsetMultipleRenderer.setFitLegend(true);
        offsetMultipleRenderer.setZoomEnabled(false, false);
        offsetMultipleRenderer.setPanEnabled(false, false);
        //offsetMultipleRenderer.setClickEnabled(true);
        offsetMultipleRenderer.setAxisTitleTextSize(30);
        //offsetMultipleRenderer.setXLabels(0);

        //set up the Linear layout and add the graphs

        mAnalysisView = view.findViewById(R.id.clock_analysis);
        mAnalysisView.setTextColor(Color.BLACK);

        mclassAnalysisView = view.findViewById(R.id.class_analysis);
        mclassAnalysisView.setTextColor(Color.BLACK);

        RelativeLayout ClockOffsetLayout = view.findViewById(R.id.plot);
        TimeoffsetView = ChartFactory.getLineChartView(getContext(), TimeOffsetDataSet, offsetMultipleRenderer);
        ClockOffsetLayout.addView(TimeoffsetView);

        // Inflate the layout for this fragment
        return view;
    }

    protected void Timeoffset (long timeoffset, double timeepoch, boolean hasclockelapserealtime){

        /** used in clock **/
        long movingtimeoffset =0;
        float movingaveragetimeoffset;
        float currentmovingagerage;
        int averagewindow;
        float currentnormalizedtimeoffset;
        float currentabsolutetimeoffset;
        int count =0;

        GnssClock  clock;
        float normalizedtimeoffset;

        //averagewindow = Integer.parseInt(temp_inputaveragewindow_val);
        averagewindow = 30; //default
        SpannableStringBuilder builder_clock = new SpannableStringBuilder();
        SpannableStringBuilder builder_class = new SpannableStringBuilder();
        if (mInitialTimeSeconds < 0) {
            mInitialTimeSeconds = timeepoch;
        }
        mLastTimeReceivedSeconds = timeepoch - mInitialTimeSeconds;
        timeoffsetrecord.add(timeoffset);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            if(hasclockelapserealtime){
                builder_class.append(String.format("Offset Calculation using : %n"));
                builder_class.append(String.format("GPSTime : GnssClock Class%n"));
                builder_class.append(String.format("ElapsedRealTime : GnssClock Class%n"));}
            else
            {   builder_class.append(String.format("Offset Calculation using : %n"));
                builder_class.append(String.format("GPSTime : GnssClock Class%n"));
                builder_class.append(String.format("ElapsedRealTime : SystemClock Class%n"));}
        }
        else{
            builder_class.append(String.format("Offset Calculation using : %n"));
            builder_class.append(String.format("GPSTime : GnssClock Class%n"));
            builder_class.append(String.format("ElapsedRealTime : SystemClock Class%n"));
        }
        //timeoffsetcontainer.add(mLastTimeReceivedSeconds,normalizedtimeoffset);
        if(timeoffsetrecord.size() >= averagewindow)
        {
           // normalizedtimeoffset = (timeoffset-timeoffsetrecord.get(timeoffsetrecord.size()-averagewindow));
            normalizedtimeoffset = (timeoffset-timeoffsetrecord.get(0));
            normalizedtimeoffset =  normalizedtimeoffset/1000000;
            timeoffsetcontainer.add(mLastTimeReceivedSeconds,normalizedtimeoffset);
            for (int i=timeoffsetrecord.size()-averagewindow; i < timeoffsetrecord.size(); i++)
            {
               // movingtimeoffset = movingtimeoffset + (timeoffsetrecord.get(i) - timeoffsetrecord.get(timeoffsetrecord.size()-averagewindow));
                movingtimeoffset = movingtimeoffset + (timeoffsetrecord.get(i) - timeoffsetrecord.get(0));
                count = count+1;
            }

        movingaveragetimeoffset = movingtimeoffset/count;
        movingaveragetimeoffset = movingaveragetimeoffset/1000000;
        movingaveragetimeoffsetcontainer.add(mLastTimeReceivedSeconds,movingaveragetimeoffset);
        //currentmovingagerage = (float) movingaveragetimeoffset/1000000;
        currentmovingagerage = (float) movingaveragetimeoffset;
        String format_movingaverage = "%s%.3f%n";
            builder_clock.append(String.format(Locale.US,format_movingaverage,"Moving Average Clock Offset (ms) : ",currentmovingagerage));
        }
        else{
            normalizedtimeoffset = (timeoffset-timeoffsetrecord.get(0));
            normalizedtimeoffset = normalizedtimeoffset/1000000;
            timeoffsetcontainer.add(mLastTimeReceivedSeconds,normalizedtimeoffset);
            for (int i=0; i< timeoffsetrecord.size(); i++)
            {
                movingtimeoffset = movingtimeoffset + (timeoffsetrecord.get(i)-timeoffsetrecord.get(0));
                //movingtimeoffset = movingtimeoffset + (timeoffsetrecord.get(i));
                count = count+1;
            }
            movingaveragetimeoffset = movingtimeoffset/count;
            movingaveragetimeoffset = movingaveragetimeoffset/1000000;
            movingaveragetimeoffsetcontainer.add(mLastTimeReceivedSeconds,movingaveragetimeoffset);
            //currentmovingagerage = (float) movingaveragetimeoffset/1000000;
            currentmovingagerage = (float) movingaveragetimeoffset;
            String format_movingaverage = "%s%s%n";
            builder_clock.append(String.format(Locale.US,format_movingaverage,"Moving Average Clock Offset (ms) : ",currentmovingagerage));

        }
        //currentnormalizedtimeoffset = (float) normalizedtimeoffset/1000000;
        currentnormalizedtimeoffset = (float) normalizedtimeoffset;
      //  currentabsolutetimeoffset = (currentnormalizedtimeoffset*1000000 + timeoffsetrecord.get(0))/1000000000;
        String format_currentnormalizedtimeoffset = "%s%s%n";
        builder_clock.append(String.format(Locale.US,format_currentnormalizedtimeoffset,"Current Normalized Clock Offset (ms) : ",currentnormalizedtimeoffset));
       // String format_currentabsolutetimeoffset = "%s%.3f%n";
      //  builder_clock.append(String.format(Locale.US,format_currentabsolutetimeoffset,"Current Absolute Clock Offset (s) : ",currentabsolutetimeoffset));
        String format_averagewindow = "%s%d%n";
        builder_clock.append(String.format(Locale.US,format_averagewindow,"Averaging Window (s) : ",averagewindow));


        mAnalysisView.setText(builder_clock);
        mclassAnalysisView.setText(builder_class);

        offsetMultipleRenderer.setXAxisMax(60);
        offsetMultipleRenderer.setXAxisMin(0);

        offsetMultipleRenderer.setChartTitle("Normalized Clock Offset (ms) vs Epoch");

        offsetMultipleRenderer.setChartTitleTextSize(30);
        if (mLastTimeReceivedSeconds > offsetMultipleRenderer.getXAxisMax()) {
            offsetMultipleRenderer.setXAxisMax(mLastTimeReceivedSeconds + (mLastTimeReceivedSeconds/2));
            offsetMultipleRenderer.setXAxisMin(0);
        }
        TimeoffsetView.invalidate();
        TimeoffsetView.repaint();
    }

}
