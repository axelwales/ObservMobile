package com.axelwales.observ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WiFiScanActivity extends AppCompatActivity {

    private WifiScanReceiver wifiReciever;
    private WifiManager mngr;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagnetic;
    private SensorEventListener sensorEventListener;
    ArrayList<RSSResult> resultList;
    ArrayAdapter<RSSResult> resultsAdapter;
    ListView resultListView;
    private EditText xInput;
    private EditText yInput;
    private TextView directionLabel;
    private TextView degreeLabel;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    private float azimuth = 0;
    private String direction = "";
    private Button storeButton;
    private Button scanButton;
    private Button estimateButton;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi_scan);
        mngr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        resultList = new ArrayList<>();
        resultsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resultList);

        this.resultListView = (ListView) findViewById(R.id.rssList);
        this.resultListView.setAdapter(resultsAdapter);

        wifiReciever = new WifiScanReceiver(mngr, resultsAdapter);
        registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                    magneticFieldValues = event.values;
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    accelerometerValues = event.values;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        sensorManager.registerListener(sensorEventListener, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, sensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);


        if( mngr.isWifiEnabled() == false ) {
            mngr.setWifiEnabled(true);
        }

        queue = Volley.newRequestQueue(this);
        xInput = (EditText) findViewById(R.id.xInput);
        yInput = (EditText) findViewById(R.id.yInput);
        directionLabel = (TextView) findViewById(R.id.directionLabel);
        degreeLabel = (TextView) findViewById(R.id.degreeLabel);
        storeButton = (Button) findViewById(R.id.storeButton);
        scanButton = (Button) findViewById(R.id.scanButton);
        estimateButton = (Button) findViewById(R.id.estimateButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scan();
            }
        });
        storeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                store();
            }
        });
        estimateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                estimate();
            }
        });
    }

    private void scan() {
        boolean b = mngr.startScan();
        xInput.setTextColor(Color.BLACK);
        yInput.setTextColor(Color.BLACK);

        final float[] R = new float[9];
        final float[] orientation = new float[3];
        if (SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues)) {
            SensorManager.getOrientation(R, orientation);
            azimuth = (float) ( Math.toDegrees( orientation[0] ) + 360 ) % 360;

            if ((316 <= azimuth && azimuth <= 360) || (0 <= azimuth && azimuth <= 45)) {
                direction = "North";
            } else if((46 <= azimuth && azimuth <= 135)) {
                direction = "East";
            } else if((136 <= azimuth && azimuth <= 225)) {
                direction = "South";
            } else if((226 <= azimuth && azimuth <= 315)) {
                direction = "West";
            }
        }
        degreeLabel.setText(Float.toString(azimuth));
        directionLabel.setText(direction);
    }

    private void store() {
        String url = "http://axelwales.pythonanywhere.com/map/fingerprints/";

        final String xPos = xInput.getText().toString().trim();
        final String yPos = yInput.getText().toString().trim();
        final String direction = directionLabel.getText().toString().toLowerCase().trim();
        JSONArray fingerprints = new JSONArray();
        JSONObject parameters = new JSONObject();

        ArrayAdapter<RSSResult> adapter = this.resultsAdapter;

        try {
            for (int i = 0; i < adapter.getCount(); i++) {
                JSONObject fingerprint = new JSONObject();
                JSONObject accessPoint = new JSONObject();

                accessPoint.put("bssid", adapter.getItem(i).getBSSID());
                fingerprint.put("access_point", accessPoint);
                fingerprint.put("rssi", adapter.getItem(i).getRSSI());
                fingerprint.put("direction", direction);
                fingerprints.put(fingerprint);
            }
            parameters.put("lat", yPos);
            parameters.put("lng", xPos);
            parameters.put("fingerprint_set", fingerprints);
        } catch (JSONException e) {}

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                parameters,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("response", response.toString());
                        Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Log.d("error", error.toString());
                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                    }
                }
        );

        queue.add(jsonRequest);
    }

    private void estimate() {
        scan();
        String url = "wherever the algo url is";
        JSONArray fingerprints = new JSONArray();
        JSONObject parameters = new JSONObject();
        ArrayAdapter<RSSResult> adapter = this.resultsAdapter;

        try {
            for (int i = 0; i < adapter.getCount(); i++) {
                JSONObject AP = new JSONObject();
                AP.put("bssid", adapter.getItem(i).getBSSID());
                AP.put("rssi", adapter.getItem(i).getRSSI());
                fingerprints.put(AP);
            }
            parameters.put("fingerprint_set", fingerprints);
        } catch (JSONException e) {}

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                parameters,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("response", response.toString());
                        xInput.setTextColor(Color.RED);
                        yInput.setTextColor(Color.RED);
                        xInput.setText("success");//not sure how the response is formatted yet
                        yInput.setText("success");//not sure how the response is formatted yet
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Log.d("error", error.toString());
                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                    }
                }
        );

        queue.add(jsonRequest);
    }
}



class WifiScanReceiver extends BroadcastReceiver {

    ArrayAdapter<RSSResult> resultAdapter;
    private WifiManager mngr;

    WifiScanReceiver(WifiManager mngr, ArrayAdapter<RSSResult> resultAdapter) {
        this.mngr = mngr;
        this.resultAdapter = resultAdapter;
    }

    public void onReceive(Context c, Intent intent) {
        List<ScanResult> wifiList;
        wifiList = mngr.getScanResults();
        this.resultAdapter.clear();
        for( ScanResult result : wifiList ) {
            this.resultAdapter.add(new RSSResult(result));
        }
        this.resultAdapter.notifyDataSetChanged();
    }
}

class RSSResult {
    private ScanResult fullResult;

    RSSResult(ScanResult fullResult) {
        this.fullResult = fullResult;
    }

    @Override
    public String toString() {
        return "BSSID: " + this.fullResult.BSSID + " RSSI: " + this.fullResult.level;
    }

    public String getBSSID() {
        return this.fullResult.BSSID;
    }

    public double getRSSI() {
        return this.fullResult.level;
    }
}
