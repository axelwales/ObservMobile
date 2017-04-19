package com.axelwales.observ;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    ArrayList<RSSResult> resultList;
    ArrayAdapter<RSSResult> resultsAdapter;
    ListView resultListView;
    private EditText xInput;
    private EditText yInput;
    private Button storeButton;
    private Button scanButton;
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


        if( mngr.isWifiEnabled() == false ) {
            mngr.setWifiEnabled(true);
        }

        queue = Volley.newRequestQueue(this);
        xInput = (EditText) findViewById(R.id.xInput);
        yInput = (EditText) findViewById(R.id.yInput);
        storeButton = (Button) findViewById(R.id.storeButton);
        scanButton = (Button) findViewById(R.id.scanButton);
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
    }

    private void scan() {
        boolean b = mngr.startScan();
    }

    private void store() {
        String url = "http://axelwales.pythonanywhere.com/map/fingerprints/";

        final String xPos = xInput.getText().toString().trim();
        final String yPos = yInput.getText().toString().trim();
        JSONObject parameters = new JSONObject();
        JSONArray fingerprints = new JSONArray();
        ArrayAdapter<RSSResult> adapter = this.resultsAdapter;

        try {
            parameters.put("lat", yPos);
            parameters.put("lng", xPos);
            for (int i = 0; i < adapter.getCount(); i++) {
                JSONObject AP = new JSONObject();
                JSONObject APInfo = new JSONObject();

                APInfo.put("bssid", adapter.getItem(i).getBSSID());
                AP.put("rssi", adapter.getItem(i).getRSSI());
                AP.put("access_point", APInfo);

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
                        Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
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
