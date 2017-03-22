package com.axelwales.observ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class WiFiScanActivity extends AppCompatActivity {

    private WifiScanReceiver wifiReciever;
    private WifiManager mngr;
    ArrayList<RSSResult> resultList;
    ListView resultListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi_scan);

        mngr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        ArrayList<RSSResult> resultList = new ArrayList<>();
        ArrayAdapter<RSSResult> resultsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resultList);

        this.resultListView = (ListView) findViewById(R.id.rssList);
        this.resultListView.setAdapter(resultsAdapter);

        wifiReciever = new WifiScanReceiver(mngr, resultsAdapter);
        registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


        if( mngr.isWifiEnabled() == false ) {
            mngr.setWifiEnabled(true);
        }

        scan();
    }

    private void scan() {
        boolean b = mngr.startScan();
    }

    private void updateList() {

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
}
