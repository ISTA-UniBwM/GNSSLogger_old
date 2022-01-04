package com.ista.android.apps.location.gps.gnsslogger;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import java.util.List;
import android.hardware.Sensor;

import com.google.android.apps.location.gps.gnsslogger.R;


public class SensorListFragment extends Fragment {

    private TextView sensor_type;
    private TextView sensor_vendor;
    private TextView sensor_version;
    TextView tv1=null;
    private SensorManager mSensorManager;
    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sensor_list, parent, false);
        super.onCreate(savedInstanceState);

        sensor_type = (TextView) view.findViewById(R.id.sensor_type);
        sensor_type.setVisibility(View.GONE);
        sensor_vendor = (TextView) view.findViewById(R.id.sensor_vendor);
        sensor_vendor.setVisibility(View.GONE);
        sensor_version = (TextView) view.findViewById(R.id.sensor_version);
        sensor_version.setVisibility(View.GONE);

        mSensorManager = (SensorManager)  getActivity().getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> mList= mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (int i = 1; i < mList.size(); i++) {
            sensor_type.setVisibility(View.VISIBLE);
            sensor_vendor.setVisibility(View.VISIBLE);
            sensor_version.setVisibility(View.VISIBLE);

            sensor_type.append(String.format("%s  \n",mList.get(i).getName()));
            sensor_vendor.append(String.format("%s\n",mList.get(i).getVendor()));
            sensor_version.append(String.format("%s\n",mList.get(i).getVersion()));
        }
            return view;
}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //  if (id == R.id.action_settings) {
        //  return true;
        //  }
        return super.onOptionsItemSelected(item);
    }
}