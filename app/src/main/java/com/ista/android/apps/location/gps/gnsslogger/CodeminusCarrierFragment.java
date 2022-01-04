package com.ista.android.apps.location.gps.gnsslogger;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.cts.asn1.supl2.rrlp_components.SVID;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.collection.ArrayMap;

import com.google.android.apps.location.gps.gnsslogger.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.XYChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import android.widget.Button;

import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;

import static java.lang.Float.NaN;

public class CodeminusCarrierFragment extends Fragment implements SensorEventListener {

    Switch logcmc;
    private SensorManager sManager;
    private Sensor mSensor;
    XYSeries gyroX = new XYSeries("Gyroscope X-axis");
    XYSeries gyroY = new XYSeries("Y-axis");
    XYSeries gyroZ = new XYSeries("Z-axis");
    XYMultipleSeriesRenderer gyroMultipleRenderer;
    private int gyroCount = 0;
    final long starttime = SystemClock.uptimeMillis();
    static GraphicalView mgyroChartView;
    private RelativeLayout mgyroLayout;
    private static double cmc_val;
    static GraphicalView cmcView;
    private Button reset_cmc_plot;



    static XYSeries CMC = new XYSeries("Sat1");
    XYMultipleSeriesRenderer accMultipleRenderer;
    /**
     * The number of Gnss constellations
     */
    private static final int NUMBER_OF_CONSTELLATIONS = 6;
    /**
     * The index in data set we reserved for the plot containing all constellations
     */
    private static final int DATA_SET_INDEX_ALL = 0;
    /**
     * The position of the prearrange residual plot tab
     */
    private static final int CMC_TAB = 0;
    /**
     * Data format used to format the data in the text view
     */
    private static final DecimalFormat sDataFormat =
            new DecimalFormat("##.#", new DecimalFormatSymbols(Locale.US));
    private XYMultipleSeriesRenderer m1CurrentRenderer,m2CurrentRenderer;
    private final PlotFragment.ColorMap mColorMap = new PlotFragment.ColorMap();
    public DataSet m1DataSet,m2DataSet;
    /**
     * Total number of kinds of plot tabs
     */
    private static final int NUMBER_OF_TABS = 1;
    private int mCurrentTab = 0;
    private double mLastTimeReceivedSeconds = 0;
    private static final double TIME_INTERVAL_SECONDS = 60;
    private RelativeLayout mLayout,m2Layout;
    static GraphicalView mChartView, m2ChartView;
    private TextView mAnalysisView;
    static boolean Reset_timer = false;
    private double initialtimeInSeconds = 0.0;
    private double mInitialTimeSeconds = -1;
    private static final double SPEED_OF_LIGHT = 299792458.0; // [m/s]
    private static final double Frequency_L1 = 154.0 * 10.23e6;
    private static final double Wavelength_L1 = SPEED_OF_LIGHT / Frequency_L1;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View plotView = inflater.inflate(R.layout.fragment_codeminuscarrier, container, false /* attachToRoot */);
        //addListenerOnButton();

        m1DataSet
                = new DataSet(NUMBER_OF_TABS, NUMBER_OF_CONSTELLATIONS, getContext(), mColorMap);
        m2DataSet
                = new DataSet(NUMBER_OF_TABS, NUMBER_OF_CONSTELLATIONS, getContext(), mColorMap);

        //minor plot for gyroscope
        sManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        XYSeriesRenderer gyroXRenderer = new XYSeriesRenderer();
        gyroXRenderer.setColor(Color.BLUE);
        gyroXRenderer.setLineWidth(5);

        XYSeriesRenderer gyroYRenderer = new XYSeriesRenderer();
        gyroYRenderer.setColor(Color.RED);
        gyroYRenderer.setLineWidth(5);

        XYSeriesRenderer gyroZRenderer = new XYSeriesRenderer();
        gyroZRenderer.setColor(Color.GREEN);
        gyroZRenderer.setLineWidth(5);

        //Dataset that host the series which are continuously appended with sensor values

        final XYMultipleSeriesDataset gyroDataSet = new XYMultipleSeriesDataset();
        gyroDataSet.addSeries(gyroX);
        gyroDataSet.addSeries(gyroY);
        gyroDataSet.addSeries(gyroZ);

        //the MultipleSeriesRenderer that combines the single graphs

        gyroMultipleRenderer = new XYMultipleSeriesRenderer();
        gyroMultipleRenderer.addSeriesRenderer(gyroXRenderer);
        gyroMultipleRenderer.addSeriesRenderer(gyroYRenderer);
        gyroMultipleRenderer.addSeriesRenderer(gyroZRenderer);
        gyroMultipleRenderer.setShowGrid(true);
        gyroMultipleRenderer.setGridLineWidth(5);
        gyroMultipleRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        gyroMultipleRenderer.setLegendTextSize(15);
        gyroMultipleRenderer.setZoomEnabled(false, false);
        gyroMultipleRenderer.setPanEnabled(false, false);
        gyroMultipleRenderer.setClickEnabled(true);
        gyroMultipleRenderer.setYTitle("rad/s");
        gyroMultipleRenderer.setAxisTitleTextSize(25);
      //  gyroMultipleRenderer.setXTitle("                                                                                            Epoch (milliseconds)");
        gyroMultipleRenderer.setXLabels(0);
        gyroMultipleRenderer.setChartTitle("Gyroscope");
        gyroMultipleRenderer.setChartTitleTextSize(30);
        // Set UI elements handlers
 /*        final Spinner spinner = plotView.findViewById(R.id.cmc_constellation_spinner);
        final Spinner tabSpinner = plotView.findViewById(R.id.cmc_tab_spinner);
       OnItemSelectedListener spinnerOnSelectedListener = new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentTab = tabSpinner.getSelectedItemPosition();
                XYMultipleSeriesRenderer renderer1
                        = m1DataSet.getRenderer(mCurrentTab, spinner.getSelectedItemPosition());
                XYMultipleSeriesDataset dataSet1
                        = m1DataSet.getDataSet(mCurrentTab, spinner.getSelectedItemPosition());
                XYMultipleSeriesRenderer renderer2
                        = m2DataSet.getRenderer(mCurrentTab, spinner.getSelectedItemPosition());
                XYMultipleSeriesDataset dataSet2
                        = m2DataSet.getDataSet(mCurrentTab, spinner.getSelectedItemPosition());
                if (mLastTimeReceivedSeconds > TIME_INTERVAL_SECONDS) {
                    renderer1.setXAxisMax(mLastTimeReceivedSeconds + TIME_INTERVAL_SECONDS);
                    renderer1.setXAxisMin(0);
                    renderer2.setXAxisMax(mLastTimeReceivedSeconds + TIME_INTERVAL_SECONDS);
                    renderer2.setXAxisMin(0);
                }
                mCurrentRenderer = renderer1;
                mCurrentRenderer = renderer2;
                mLayout.removeAllViews();
                mgyroLayout.removeAllViews();
                mgyroChartView = ChartFactory.getLineChartView(getContext(), gyroDataSet, gyroMultipleRenderer);
                mgyroLayout.addView(mgyroChartView);
                mChartView = ChartFactory.getLineChartView(getContext(), dataSet1, renderer1);
                mLayout.addView(mChartView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinner.setOnItemSelectedListener(spinnerOnSelectedListener);
        tabSpinner.setOnItemSelectedListener(spinnerOnSelectedListener);*/


        // Set up the Graph View
        m1CurrentRenderer = m1DataSet.getRenderer(mCurrentTab, DATA_SET_INDEX_ALL);
        XYMultipleSeriesDataset currentDataSet1
                = m1DataSet.getDataSet(mCurrentTab, DATA_SET_INDEX_ALL);
        mChartView = ChartFactory.getLineChartView(getContext(), currentDataSet1, m1CurrentRenderer);
        mAnalysisView = plotView.findViewById(R.id.cmc_analysis);
        mAnalysisView.setTextColor(Color.BLACK);
        // mChartView = ChartFactory.getScatterChartView(getContext(), currentDataSet, mCurrentRenderer);
        mLayout = plotView.findViewById(R.id.plot2);
        //mLayout = plotView.findViewById(R.id.plotminor);
        mLayout.addView(mChartView);



        //set up the Linear layout and add the graphs
      /*  mgyroLayout = plotView.findViewById(R.id.plotminor);
       // mgyroChartView = ChartFactory.getLineChartView(getContext(), currentDataSet2, m2CurrentRenderer);
        mgyroChartView = ChartFactory.getLineChartView(getContext(), gyroDataSet, gyroMultipleRenderer);
        mgyroLayout.addView(mgyroChartView);*/

        //set up the Linear layout and add the graphs
        m2CurrentRenderer = m2DataSet.getRenderer(mCurrentTab, DATA_SET_INDEX_ALL);
        XYMultipleSeriesDataset currentDataSet2
                = m2DataSet.getDataSet(mCurrentTab, DATA_SET_INDEX_ALL);
        m2Layout = plotView.findViewById(R.id.plot);
        m2ChartView = ChartFactory.getLineChartView(getContext(), currentDataSet2, m2CurrentRenderer);
        m2Layout.addView(m2ChartView);

        return plotView;

    }

 /*   public addListenerOnButton(){
        reset_cmc_plot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }

        });

    }*/

    //when this Activity starts
    @Override
    public void onResume() {
        super.onResume();
        /*register the sensor listener to listen to the gyroscope sensor, use the
         * callbacks defined in this class, and gather the sensor information as
         * quick as possible*/
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        //Do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //if sensor is unreliable, return void
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        Sensor sensor = event.sensor;

        long timeInMillis = System.currentTimeMillis() + ((event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L);

        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //TODO: get values
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

              //  mgyroChartView.repaint();
            }
            if (gyroCount % 500 == 0) {
                gyroMultipleRenderer.addXTextLabel(timeInMillis, Long.toString(SystemClock.uptimeMillis() - starttime));
            }
        }
    }

    protected void updateCMCTab(Vector<RinexLogger.cmc_epoch_measurement> cmc_measurement, double timeInSeconds) {

        if (mInitialTimeSeconds < 0) {
            mInitialTimeSeconds = timeInSeconds;
        }
        mLastTimeReceivedSeconds = timeInSeconds - mInitialTimeSeconds;
        // mLastTimeReceivedSeconds = timeInSeconds;
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int sat_without_clip_count = 0, sat_with_clip_count = 0;
        int GPS_sat_with_clip_count = 0, GALILEO_sat_with_clip_count = 0, BEIDOU_sat_with_clip_count = 0;
        int GPS_sat_without_clip_count = 0, GALILEO_sat_without_clip_count = 0, BEIDOU_sat_without_clip_count = 0;
        for (int i = 0; i < cmc_measurement.size(); i++) {
            if (cmc_measurement.get(i).Constellation_type == 1) {
                if (cmc_measurement.get(i).LLIflag == 1) {
                    GPS_sat_with_clip_count++;
                    sat_with_clip_count++;
                } else {
                    GPS_sat_without_clip_count++;
                    sat_without_clip_count++;
                }
            } else if (cmc_measurement.get(i).Constellation_type == 5) {
                if (cmc_measurement.get(i).LLIflag == 1) {
                    BEIDOU_sat_with_clip_count++;
                    sat_with_clip_count++;
                } else {
                    BEIDOU_sat_without_clip_count++;
                    sat_without_clip_count++;
                }
            } else if (cmc_measurement.get(i).Constellation_type == 6) {
                if (cmc_measurement.get(i).LLIflag == 1) {
                    GALILEO_sat_with_clip_count++;
                    sat_with_clip_count++;
                } else {
                    GALILEO_sat_without_clip_count++;
                    sat_without_clip_count++;
                }
            }
            //sat_without_clip_count++;

            m1DataSet.addValuePlot1(
                    CMC_TAB,
                    cmc_measurement.get(i).Constellation_type,
                    cmc_measurement.get(i).satID,
                    //cmc_measurement.get(i).epoch,
                    mLastTimeReceivedSeconds,
                    cmc_measurement.get(i).cmc_val,
                    cmc_measurement.get(i).LLIflag);

            m2DataSet.addValuePlot2(
                    CMC_TAB,
                    cmc_measurement.get(i).Constellation_type,
                    cmc_measurement.get(i).satID,
                    //cmc_measurement.get(i).epoch,
                    mLastTimeReceivedSeconds,
                    cmc_measurement.get(i).cmc_val,
                    cmc_measurement.get(i).LLIflag);
        }

        builder.append(getString(R.string.satellite_number_sum_slip_hint, sat_with_clip_count) + " | ");
        builder.append(getString(R.string.satellite_number_sum_noslip_hint, sat_without_clip_count) + "\n");
        builder.append(getString(R.string.gps_satellite_number_sum_slip_hint, GPS_sat_with_clip_count) + " | ");
        builder.append(getString(R.string.gps_satellite_number_sum_noslip_hint, GPS_sat_without_clip_count) + "\n");
        builder.append(getString(R.string.gal_satellite_number_sum_slip_hint, GALILEO_sat_with_clip_count) + " | ");
        builder.append(getString(R.string.gal_satellite_number_sum_noslip_hint, GALILEO_sat_without_clip_count) + "\n");
        builder.append(getString(R.string.beidou_satellite_number_sum_slip_hint, BEIDOU_sat_with_clip_count) + " | ");
        builder.append(getString(R.string.beidou_satellite_number_sum_noslip_hint, BEIDOU_sat_without_clip_count) + "\n");

        mAnalysisView.setText(builder);
        m1DataSet.fillInDiscontinuity(CMC_TAB, mLastTimeReceivedSeconds);
        m2DataSet.fillInDiscontinuity(CMC_TAB, mLastTimeReceivedSeconds);

        // Checks if the plot has reached the end of frame and resize

        m1CurrentRenderer.setYAxisMax(50);
        m1CurrentRenderer.setYAxisMin(-50);
        //m1CurrentRenderer.setZoomEnabled(true);
        m1CurrentRenderer.setChartTitle("Code Minus Carrier Normalized (m) ");
        m1CurrentRenderer.setChartTitleTextSize(30);
        if (mLastTimeReceivedSeconds > m1CurrentRenderer.getXAxisMax()) {
            m1CurrentRenderer.setXAxisMax(mLastTimeReceivedSeconds + (mLastTimeReceivedSeconds/2));
            m1CurrentRenderer.setXAxisMin(0);
        }

        m2CurrentRenderer.setChartTitle("Code Minus Carrier (m) X 1E7 ");
        m2CurrentRenderer.setChartTitleTextSize(30);
        if (mLastTimeReceivedSeconds > m2CurrentRenderer.getXAxisMax()) {
            m2CurrentRenderer.setXAxisMax(mLastTimeReceivedSeconds + (mLastTimeReceivedSeconds/2));
            m2CurrentRenderer.setXAxisMin(0);
        }

        mChartView.invalidate();
        mChartView.repaint();
        m2ChartView.invalidate();
        m2ChartView.repaint();
    }
    //}


    /**
     * An utility class stores and maintains all the data sets and corresponding renders.
     * We use 0 as the {@code dataSetIndex} of all constellations and 1 - 6 as the
     * {@code dataSetIndex} of each satellite constellations
     */
    private static class DataSet {
        /**
         * The Y min and max of each plot
         */
        private static final double[][] RENDER_HEIGHTS = {{-100, 100}, {-100, 100}};
        /**
         * <ul>
         *    <li>A list of constellation prefix</li>
         *    <li>G : GPS, US Constellation</li>
         *    <li>S : Satellite-based Augmentation System</li>
         *    <li>R : GLONASS, Russia Constellation</li>
         *    <li>J : QZSS, Japan Constellation</li>
         *    <li>C : BEIDOU China Constellation</li>
         *    <li>E : GALILEO EU Constellation</li>
         *  </ul>
         */
        private static final String[] CONSTELLATION_PREFIX = {"G", "S", "R", "J", "C", "E"};

        private final List<ArrayMap<Integer, Integer>>[] mSatelliteIndex;
        private final List<ArrayMap<Integer, Integer>>[] mSatelliteConstellationIndex;
        private final List<XYMultipleSeriesDataset>[] mDataSetList;
        //private final List<XYMultipleSeriesDataset>[] mResettimer;
        private final List<XYMultipleSeriesRenderer>[] mRendererList;
        private final Context mContext;
        private final PlotFragment.ColorMap mColorMap;

        public DataSet(int numberOfTabs, int numberOfConstellations,
                       Context context, PlotFragment.ColorMap colorMap) {
            mDataSetList = new ArrayList[numberOfTabs];
            mRendererList = new ArrayList[numberOfTabs];
            mSatelliteIndex = new ArrayList[numberOfTabs];
            mSatelliteConstellationIndex = new ArrayList[numberOfTabs];
            mContext = context;
            mColorMap = colorMap;

            // Preparing data sets and renderer for all six constellations
            for (int i = 0; i < numberOfTabs; i++) {
                mDataSetList[i] = new ArrayList<>();
                mRendererList[i] = new ArrayList<>();
                mSatelliteIndex[i] = new ArrayList<>();
                mSatelliteConstellationIndex[i] = new ArrayList<>();
                for (int k = 0; k <= numberOfConstellations; k++) {
                    mSatelliteIndex[i].add(new ArrayMap<Integer, Integer>());
                    mSatelliteConstellationIndex[i].add(new ArrayMap<Integer, Integer>());
                    XYMultipleSeriesRenderer tempRenderer = new XYMultipleSeriesRenderer();
                    setUpRenderer(tempRenderer, i);
                    mRendererList[i].add(tempRenderer);
                    XYMultipleSeriesDataset tempDataSet = new XYMultipleSeriesDataset();
                    mDataSetList[i].add(tempDataSet);
                }
            }
        }

        // The constellation type should range from 1 to 6
        private String getConstellationPrefix(int constellationType) {
            if (constellationType <= GnssStatus.CONSTELLATION_UNKNOWN
                    || constellationType > NUMBER_OF_CONSTELLATIONS) {
                return "";
            }
            return CONSTELLATION_PREFIX[constellationType - 1];
        }

        /**
         * Returns the multiple series data set at specific tab and index
         */
        private XYMultipleSeriesDataset getDataSet(int tab, int dataSetIndex) {
            return mDataSetList[tab].get(dataSetIndex);
        }

        /**
         * Returns the multiple series renderer set at specific tab and index
         */
        private XYMultipleSeriesRenderer getRenderer(int tab, int dataSetIndex) {
            return mRendererList[tab].get(dataSetIndex);
        }

        /**
         * Adds a value into the both the data set containing all constellations and individual data set
         * of the constellation of the satellite
         */
        private void addValuePlot1(int tab, int constellationType, int svID,
                                   double timeInSeconds, double value, int LLI_flag) {

            XYMultipleSeriesDataset dataSetAll = getDataSet(tab, DATA_SET_INDEX_ALL);
            double N = 0;

            XYMultipleSeriesRenderer rendererAll = getRenderer(tab, DATA_SET_INDEX_ALL);

            value = Double.parseDouble(sDataFormat.format(value));

            if ((hasSeen(constellationType, svID, tab))) {
                // If the satellite has been seen before, we retrieve the dataseries it is add and add newlog e
                // data

                N = mDataSetList[tab].get(constellationType).getSeriesAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID)).getY(0);
                if (LLI_flag == 0) {
                    dataSetAll
                            .getSeriesAt(mSatelliteIndex[tab].get(constellationType).get(svID))
                            .add(timeInSeconds, value - N);
                    mDataSetList[tab]
                            .get(constellationType)
                            .getSeriesAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID))
                            .add(timeInSeconds, value - N);
                }

                mRendererList[tab].get(constellationType).getSeriesRenderers();
            } else {

                // If the satellite has not been seen before, we create new dataset and renderer before
                // adding data
                mSatelliteIndex[tab].get(constellationType).put(svID, dataSetAll.getSeriesCount());
                mSatelliteConstellationIndex[tab]
                        .get(constellationType)
                        .put(svID, mDataSetList[tab].get(constellationType).getSeriesCount());

                XYSeries tempSeries = new XYSeries(CONSTELLATION_PREFIX[constellationType - 1] + svID);
                tempSeries.add(timeInSeconds, value);
                dataSetAll.addSeries(tempSeries);
                mDataSetList[tab].get(constellationType).addSeries(tempSeries);
                XYSeriesRenderer tempRenderer = new XYSeriesRenderer();
                tempRenderer.setLineWidth(8);
                tempRenderer.setColor(mColorMap.getColor(svID, constellationType));
                                rendererAll.addSeriesRenderer(tempRenderer);
                mRendererList[tab].get(constellationType).addSeriesRenderer(tempRenderer);

            }
        }

        /**
         * Adds a value into the both the data set containing all constellations and individual data set
         * of the constellation of the satellite
         */
        private void addValuePlot2(int tab, int constellationType, int svID,
                                   double timeInSeconds, double value, int LLI_flag) {

            XYMultipleSeriesDataset dataSetAll = getDataSet(tab, DATA_SET_INDEX_ALL);
            double N = 0;

            XYMultipleSeriesRenderer rendererAll = getRenderer(tab, DATA_SET_INDEX_ALL);

            value = Double.parseDouble(sDataFormat.format(value));

            if ((hasSeen(constellationType, svID, tab))) {
                // If the satellite has been seen before, we retrieve the dataseries it is add and add newlog e
                // data
                //N = mDataSetList[tab].get(constellationType).getSeriesAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID)).getY(0);
                if (LLI_flag == 1) {
                    dataSetAll
                            .getSeriesAt(mSatelliteIndex[tab].get(constellationType).get(svID))
                            .clear();
                    mDataSetList[tab]
                            .get(constellationType)
                            .getSeriesAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID))
                            .clear();
                }
                dataSetAll
                        .getSeriesAt(mSatelliteIndex[tab].get(constellationType).get(svID))
                        .add(timeInSeconds, value/1e7);
                mDataSetList[tab]
                        .get(constellationType)
                        .getSeriesAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID))
                        .add(timeInSeconds, value/1e7);
                if (LLI_flag == 1) {
                    ((XYSeriesRenderer) (mRendererList[tab].get(constellationType).getSeriesRendererAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID)))).setPointStyle(PointStyle.CIRCLE);
                    ((XYSeriesRenderer) (mRendererList[tab].get(constellationType).getSeriesRendererAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID)))).setFillPoints(true);
                } else {
                    ((XYSeriesRenderer) (mRendererList[tab].get(constellationType).getSeriesRendererAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID)))).setFillPoints(false);
                    ((XYSeriesRenderer) (mRendererList[tab].get(constellationType).getSeriesRendererAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID)))).setLineWidth(8);
                    ((XYSeriesRenderer) (mRendererList[tab].get(constellationType).getSeriesRendererAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID)))).setPointStyle(PointStyle.POINT);

                    // mRendererList[tab].get(constellationType).getSeriesRenderers();
                }
            } else {

                // If the satellite has not been seen before, we create new dataset and renderer before
                // adding data
                mSatelliteIndex[tab].get(constellationType).put(svID, dataSetAll.getSeriesCount());
                mSatelliteConstellationIndex[tab]
                        .get(constellationType)
                        .put(svID, mDataSetList[tab].get(constellationType).getSeriesCount());

                XYSeries tempSeries = new XYSeries(CONSTELLATION_PREFIX[constellationType - 1] + svID);
                tempSeries.add(timeInSeconds, value/1e7);
                dataSetAll.addSeries(tempSeries);
                mDataSetList[tab].get(constellationType).addSeries(tempSeries);
                XYSeriesRenderer tempRenderer = new XYSeriesRenderer();
                tempRenderer.setLineWidth(8);
                tempRenderer.setColor(mColorMap.getColor(svID, constellationType));
                rendererAll.addSeriesRenderer(tempRenderer);
                mRendererList[tab].get(constellationType).addSeriesRenderer(tempRenderer);
            }
        }

        /**
         * Creates a discontinuity of the satellites that has been seen but not reported in this batch
         * of measurements
         */
        private void fillInDiscontinuity(int tab, double referenceTimeSeconds) {
            for (XYMultipleSeriesDataset dataSet : mDataSetList[tab]) {
                for (int i = 0; i < dataSet.getSeriesCount(); i++) {
                    if (dataSet.getSeriesAt(i).getMaxX() < referenceTimeSeconds) {
                        dataSet.getSeriesAt(i).add(referenceTimeSeconds, MathHelper.NULL_VALUE);
                    }
                }
            }
        }

        /**
         * Returns a boolean indicating whether the input satellite has been seen.
         */
        private boolean hasSeen(int constellationType, int svID, int tab) {
            return mSatelliteIndex[tab].get(constellationType).containsKey(svID);
        }

        /**
         * Set up a {@link XYMultipleSeriesRenderer} with the specs customized per plot tab.
         */
        private void setUpRenderer(XYMultipleSeriesRenderer renderer, int tabNumber) {

            renderer.setXAxisMin(0);
            renderer.setXAxisMax(60);
           // renderer.setYAxisMin(RENDER_HEIGHTS[tabNumber][0]);
           // renderer.setYAxisMax(RENDER_HEIGHTS[tabNumber][1]);
            renderer.setYAxisAlign(Paint.Align.RIGHT, 0);
            renderer.setLegendTextSize(30);
            renderer.setLabelsTextSize(30);
            renderer.setYLabelsColor(0, Color.BLACK);
            renderer.setXLabelsColor(Color.BLACK);
            renderer.setFitLegend(true);
            renderer.setShowGrid(true);
            renderer.setGridLineWidth(5);
            renderer.setPointSize(10f);
            renderer.setMargins(new int[]{10, 10, 30, 30});
            // setting the plot untouchable
            renderer.setZoomEnabled(false, true);
            renderer.setPanEnabled(false, true);
            renderer.setClickEnabled(false);
            renderer.setMarginsColor(Color.WHITE);
            renderer.setChartTitle(mContext.getResources()
                    .getStringArray(R.array.cmc_plot_titles)[tabNumber]);
            renderer.setChartTitleTextSize(30);
        }
    }

}
