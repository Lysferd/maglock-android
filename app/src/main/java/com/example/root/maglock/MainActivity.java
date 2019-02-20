package com.example.root.maglock;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MagLock." + MainActivity.class.getSimpleName();

    private TextView mTextMessage;
    private Button mButton;
    private Button mRefreshButton;
    private int which = 0;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 400;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Toolbar toolbar;
    private static boolean scanning = false;
    private ScanResultAdapter mAdapter;
    private ScanCallback mScanCallback;
    private Handler mHandler;
    private BluetoothLeService mBluetoothLeService;
    private String mAddress;
    private boolean mConnected = false;
    private boolean mConnecting = false;
    private TextView mBatteryField;
    private TextView mHeartMeasure;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if(!mBluetoothLeService.initialize()) {
                Log.d(TAG,"Could not start Bluetooth");
                Toast.makeText(getApplicationContext(),
                        "Bluetooth failed", Toast.LENGTH_SHORT).show();
                finish();
            }
            else {
                Log.d(TAG, "Starting Bluetooth");
                Toast.makeText(getApplicationContext(),
                        "Bluetooth working, connecting", Toast.LENGTH_SHORT).show();
                mBluetoothLeService.connect(mAddress);
                mConnecting = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
            mConnecting = false;
            Log.d(TAG, "Service Disconnected");
            Toast.makeText(getApplicationContext(),
                    "Bluetooth Service disconnected", Toast.LENGTH_SHORT).show();
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mConnecting = false;
                //updateTitle(mBluetoothLeService.getDeviceName());
                //updateTitle("test");
                //updateConnectionState(R.string.connected);
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mConnecting = false;
                //updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                displayBattery(intent.getStringExtra(BluetoothLeService.BATTERY_DATA));
                Log.d(TAG, "battery from intent:" + intent.getStringExtra(BluetoothLeService.BATTERY_DATA));
                displayHeart(intent.getStringExtra(BluetoothLeService.HEART_DATA));
                Log.d(TAG, "Heart from intent:" + intent.getStringExtra(BluetoothLeService.HEART_DATA));
                Log.d(TAG, "Intent:"+ intent.getExtras());
                //displayName(intent.getStringExtra(BluetoothLeService.NAME_DATA));
                //displayAppearance(intent.getStringExtra(BluetoothLeService.APPEARANCE_DATA));
            }
        }
    };
    /*
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener

            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    //mTextMessage.setText(R.string.title_dashboard);
                    return true;
            }
            return false;
        }
    };
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //
        //final ActionBar actionBar = getSupportActionBar();

        //mTextMessage = (TextView) findViewById(R.id.message);
        //BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
        }
        if (savedInstanceState == null) {

            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {
                    //setupFragments();
                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    mAdapter = new ScanResultAdapter(this,
                            LayoutInflater.from(this));
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            } else {
                // Bluetooth is not supported.
                //showErrorText(R.string.bt_not_supported);
            }
        }

        mButton = findViewById(R.id.button2);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (which) {
                    case 0:
                        mButton.setBackground(getDrawable(R.drawable.roundbutton2));
                        which++;
                        break;
                    case 1:
                        mButton.setBackground(getDrawable(R.drawable.roundbutton));
                        which--;
                        break;
                }
            }
        });
        mRefreshButton = findViewById(R.id.refresh_button);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mConnected) {
                    if (!scanning) {
                        mRefreshButton.setBackground(getDrawable(R.drawable.ic_action_stop));
                        startScanning();
                    } else {
                        mRefreshButton.setBackground(getDrawable(R.drawable.refreshbutton));
                        stopScanning();
                    }
                }
                else {
                    mRefreshButton.setBackground(getDrawable(R.drawable.refreshbutton));
                    mBluetoothLeService.disconnect();
                    mConnected = false;
                    mAdapter.clear();
                    mBatteryField.setVisibility(View.GONE);
                    mHeartMeasure.setVisibility(View.GONE);
                }
            }
        });

        mBatteryField = (TextView) findViewById(R.id.main2_battery_value);
        mHeartMeasure = (TextView) findViewById(R.id.heart_rate_value);

        final Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        //startService(gattServiceIntent);
        if (gattServiceIntent == null){

        }

        if(!getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)){
            Log.d(TAG, "bindService FAILED");
        }

    }

    private void bindGattService() {
        final Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService!=null) {
            final boolean result = mBluetoothLeService.connect(mAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        else {
            Log.d(TAG, "mBluetoothLeService NULL");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.scanner_menu, menu);
        if (scanning) {
            menu.findItem(R.id.refresh).setActionView(R.layout.actionbar_indeterminate_progress);
            menu.findItem(R.id.stop).setVisible(true);
            menu.findItem(R.id.clear).setVisible(false);
        }
        else if (!scanning) {
            menu.findItem(R.id.refresh).setActionView(null);
            menu.findItem(R.id.stop).setVisible(false);
            menu.findItem(R.id.clear).setVisible(true);
        }
        return true;
    }
    */

    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                startScanning();
                return true;
            case R.id.stop:
                stopScanning();
                return true;
            case R.id.clear:
                mAdapter.clear();
                mAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    */

    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

            /*mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD);*/

            mScanCallback = new SampleScanCallback();
            scanning = true;
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
            //mBluetoothLeScanner.startScan(mScanCallback);


            /*
            String toastText = getString(R.string.scan_start_toast) + " "
                    + TimeUnit.SECONDS.convert(SCAN_PERIOD, TimeUnit.MILLISECONDS) + " "
                    + getString(R.string.seconds);
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
            */
        } else {
            Toast.makeText(this, R.string.already_scanning, Toast.LENGTH_LONG);
        }
        invalidateOptionsMenu();
    }
    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        scanning = false;
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

        // Even if no new results, update 'last seen' times.
        mAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        //builder.setServiceUuid(Constants.Service_UUID);
        builder.setServiceUuid(Constants.magLock_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        builder.setReportDelay(1000);
        return builder.build();
    }

    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                Log.d(TAG, "batch-result: " + result.getDevice().getName() );
                String msg = "adv=";
                ScanRecord record = result.getScanRecord();

                Log.d(TAG, "device-data: " + record.toString() );
                if(!mAdapter.add(result)) {
                    Log.d(TAG, "Already found.");
                }
                else {
                    Log.d(TAG, "Added to list");
                    mAddress = result.getDevice().getAddress();
                    final String maddress = mAddress;
                    stopScanning();
                    //testConnection(mAddress);
                    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                    mConnecting = true;
                    //mBluetoothLeService.disconnect();
                    if (mBluetoothLeService==null){
                        Log.d(TAG, "BLEService: NULL");
                    }

                    mBluetoothLeService.connect(maddress);
                }
            }
            mAdapter.notifyDataSetChanged();
            Log.d(TAG, "Batch");

        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            mAdapter.add(result);
            mAdapter.notifyDataSetChanged();
            Log.d(TAG, result.toString());
            Log.d(TAG, "Done");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getApplicationContext(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    setupFragments();
                } else {
                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                case Constants.DEVICE_SCAN: {

                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void testConnection(String address) {
        final String mAddress = address;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnecting = true;
                mBluetoothLeService.disconnect();
                mBluetoothLeService.connect(mAddress);
            }
        });
    }

    private void setupFragments() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        ScanFragment scannerFragment = new ScanFragment();
        // Fragments can't access system services directly, so pass it the BluetoothAdapter
        scannerFragment.setBluetoothAdapter(mBluetoothAdapter);
        transaction.replace(R.id.scanner_fragment_container, scannerFragment);

        transaction.commit();
    }

    private void displayBattery(String data) {
        if (data != null) {
            mBatteryField.setText(data);
            if ( mBatteryField.getVisibility() == View.GONE) {
                mBatteryField.setVisibility(View.VISIBLE);
            }
        }
    }
    private void displayHeart(String data) {
        if (data != null) {
            mHeartMeasure.setText(data);
            if ( mHeartMeasure.getVisibility() == View.GONE) {
                mHeartMeasure.setVisibility(View.VISIBLE);
            }
        }
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void displayGattServices(List<BluetoothGattService> supportedGattServices) {
        if (supportedGattServices == null) return;

        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for (BluetoothGattService gattService : supportedGattServices) {
            uuid = gattService.getUuid().toString();
            Log.d(TAG, "uuid-" + uuid);
            HashMap<String, String> currentServiceData = new HashMap<>();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString)
            );
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                Log.d(TAG, "Characteristic UUID-" + uuid);

                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        /*
        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
        */
    }
}
