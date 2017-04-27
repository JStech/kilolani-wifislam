package net.stechschulte.kilolani;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

public class Navigate extends AppCompatActivity {

    private final static String TAG="Navigate";
    private final static int ACCESS_FINE_LOCATION_PERM_REQ = 1001;
    private final static int ACCESS_COARSE_LOCATION_PERM_REQ = 1002;
    private final static int WRITE_EXTERNAL_STORAGE_PERM_REQ = 1003;

    ListView listView;
    ArrayList<String> status_list;
    private BroadcastReceiver statusListUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton start_button = (FloatingActionButton) findViewById(R.id.start_scan);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent scanIntent = new Intent(Navigate.this, RFScanService.class);
                scanIntent.setAction(RFScanService.ACTION_START_SCAN);
                startService(scanIntent);
            }
        });

        FloatingActionButton stop_button = (FloatingActionButton) findViewById(R.id.stop_scan);
        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent scanIntent = new Intent(Navigate.this, RFScanService.class);
                scanIntent.setAction(RFScanService.ACTION_STOP_SCAN);
                stopService(scanIntent);
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_PERM_REQ);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    ACCESS_COARSE_LOCATION_PERM_REQ);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_PERM_REQ);
        }

        status_list = new ArrayList<>();
        listView = (ListView) findViewById(R.id.status_list);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                R.layout.status_list, status_list);
        listView.setAdapter(adapter);

        statusListUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String statusUpdate = intent.getStringExtra("Update");
                status_list.add(statusUpdate);
                adapter.notifyDataSetChanged();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(statusListUpdate,
                new IntentFilter("RFScanUpdate"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == ACCESS_FINE_LOCATION_PERM_REQ &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // TODO: What you want to do when it works or maybe .PERMISSION_DENIED if it works better
        }
        if (requestCode == ACCESS_COARSE_LOCATION_PERM_REQ &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // TODO: What you want to do when it works or maybe .PERMISSION_DENIED if it works better
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_navigate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
