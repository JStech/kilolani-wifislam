package net.stechschulte.kilolani;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RFScanService extends Service {
    protected static final int RESCAN_PERIOD = 2000;
    private static final int MAX_POSITIONS = 300;
    private static final int DESIRED_NUM_PEERS = 3;
    private static final int DESIRED_NUM_POSITIONS = 10;
    private Location last_location = null;
    private Position last_position = null;
    private ArrayList<Position> positions;
    private MapDatabaseHelper mapDB;
    private Context context;

    public class RFScanBinder extends Binder {
        RFScanService getService() {
            return RFScanService.this;
        }
    }

    private final static String TAG = "RFScanService";
    public static final String ACTION_START_SCAN = "net.stechschulte.kilolani.action.START_SCAN";
    public static final String ACTION_STOP_SCAN = "net.stechschulte.kilolani.action.STOP_SCAN";

    private HandlerThread scanThread;
    private Handler scanHandler;
    private RFScanRunnable scanRunnable;

    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver bleResultsReceiver;

    private WifiManager wifiManager;
    private BroadcastReceiver wifiResultsReceiver;
    private List<ScanResult> results;

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onCreate() {
        super.onCreate();
        sendUpdateToActivity("Starting service");
        context = this.getApplicationContext();

        positions = new ArrayList<Position>(MAX_POSITIONS);
        mapDB = new MapDatabaseHelper(this);

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        bluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();

        // wifiResultsReceiver is what kicks off localization
        wifiResultsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendUpdateToActivity("WiFi scan finished");
                results = wifiManager.getScanResults();
                Position new_position = null;
                long currentTime = System.currentTimeMillis();

                // if we have a good, recent location, we'll use it
                /*if (last_location != null && (currentTime - last_location.getTime() < RESCAN_PERIOD)
                        && last_location.getAccuracy() <= 3. ) { //}*/
                if (last_location != null) {
                    new_position = new Position(last_location.getLatitude(), last_location.getLongitude(),
                            last_location.getAccuracy(), System.currentTimeMillis());
                    for (ScanResult r : results) {
                        new_position.addObservation(r.BSSID, r.level);
                    }
                    while (positions.size() >= MAX_POSITIONS) {
                        positions.remove(0);
                    }
                    mapDB.insertPosition(new_position);
                    TcpClient.startActionSharePositions(context, Arrays.asList(new_position));
                    sendUpdateToActivity("inserted new position");
                    //positions.add(new_position);
                } else if (last_location==null) {
                    // TODO: relocalize using map
                } else {
                    // TODO: localize using prior positions, map
                }
            }
        };

        // not currently using Bluetooth results
        bleResultsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String address = dev.getAddress();
                }
            }
        };

        // register wifiResultsReceiver
        registerReceiver(wifiResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                sendUpdateToActivity("Got new location");
                last_location = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // start location updates
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, RESCAN_PERIOD,
                    0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, RESCAN_PERIOD,
                    0, locationListener);
        } catch (SecurityException se) {
            Log.e(TAG, se.getLocalizedMessage());
        }

        // Thread to set off scans on a heartbeat
        scanThread = new HandlerThread("ScanThread");
        scanThread.start();
        scanHandler = new Handler(scanThread.getLooper());
        scanRunnable = new RFScanRunnable();
   }

    @Override
    public void onDestroy() {
        // TODO: write remaining positions to map

        // tear it all down
        unregisterReceiver(wifiResultsReceiver);
        unregisterReceiver(bleResultsReceiver);
        scanHandler.removeCallbacks(scanRunnable);
        scanThread.quitSafely();
        locationManager.removeUpdates(locationListener);

        // TODO: remove this code
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream("/data/data/net.stechschulte.kilolani/databases/KilolaniMap").getChannel();
            outChannel = new FileOutputStream("/sdcard/KilolaniMap").getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        } finally {
            try {
                if (inChannel != null) inChannel.close();
            } catch (IOException e) {}
            try {
                if (outChannel != null) outChannel.close();
            } catch (IOException e) {}
        }

        sendUpdateToActivity("Stopping service");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new RFScanBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scanHandler.removeCallbacks(scanRunnable);
        scanHandler.postDelayed(scanRunnable, RESCAN_PERIOD);

        bluetoothAdapter.getDefaultAdapter().enable();
        registerReceiver(bleResultsReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        return START_STICKY;
    }

    private class RFScanRunnable implements Runnable {
        public void run() {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            wifiManager.startScan();
            bluetoothAdapter.startDiscovery();

            // check that we have enough peers
            if (ManageSharedPrefs.getInstance().getPeers().size() < DESIRED_NUM_PEERS) {
                TcpClient.startActionFindPeers(context, 2);
            }

            // check if we have enough positions in the area (if we know where we are to begin with)
            float radius = (float)(3*Constants.tau + 3*Constants.sigma_walk);
            if (last_position!=null &&
                    mapDB.countObservationsNear(last_position, radius)<DESIRED_NUM_POSITIONS) {
                TcpClient.startActionRequestPositions(context, last_position, radius);
            }

            scanHandler.postDelayed(this, RESCAN_PERIOD);
        }
    }

    // function to update UI
    private void sendUpdateToActivity(String update) {
        Intent intent = new Intent("RFScanUpdate");
        intent.putExtra("Update", update);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
