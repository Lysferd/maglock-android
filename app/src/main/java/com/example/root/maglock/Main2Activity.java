package com.example.root.maglock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.root.maglock.DetailsActivity.address;
import static com.example.root.maglock.SearchActivity.hasMyService;

public class Main2Activity extends AppCompatActivity {
    private String TAG = "MAGLOCK."+Main2Activity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 400;

    private ScanCallback mScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeService mBluetoothLeService;

    private gridItemAdapter mAdapter;
    private GridView gridView;
    private ConstraintLayout griditem;

    private Switch aSwitch;
    private Button button;

    private boolean scanning = false;

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
                        "Bluetooth working, ready for connection", Toast.LENGTH_SHORT).show();
                //mBluetoothLeService.connect(mAddress);
                //mConnecting = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
            Log.d(TAG, "Service Disconnected");
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);
                mAdapter.setConnection(position, true);
                mAdapter.notifyDataSetChanged();
                stopScanning();
            }
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_DISCONNECTED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);
                mAdapter.setConnection(position, false);
                mAdapter.notifyDataSetChanged();
                mBluetoothLeService.connect(address);
            }
            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);
                getGattServices(mBluetoothLeService.getSupportedGattServices(address), position);
                mBluetoothLeService.setNotify(address, mAdapter.getItem(position, gridItemAdapter.CONTACT));
                startScanning();
            }
            if (BluetoothLeService.ACTION_DESCRIPTOR_WRITE.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                int type = intent.getIntExtra(BluetoothLeService.CHARACTERISTIC_TYPE, 0);
                if (type == 0) {
                    Log.d(TAG, "Unknown characteristic type");
                    return;
                }
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);

                switch (type) {
                    case (BluetoothLeService.CONTACT):

                        Log.d(TAG, "Contact descriptor notify called, proceeding to start notify from Strike");
                        BluetoothGattCharacteristic characteristic = mAdapter.getItem(position, gridItemAdapter.STRIKE);
                        mBluetoothLeService.setNotify(address, characteristic);
                        break;
                    case (BluetoothLeService.STRIKE):
                        Log.d(TAG, "Strike descriptor notify called, nothing else to be done");
                        mBluetoothLeService.readCharacteristic(mAdapter.getItem(position, gridItemAdapter.CONTACT), address);

                        break;
                }
            }
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);
                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.containsKey(BluetoothLeService.DOOR_CONTACT_DATA)) {
                    displayDoorContactData(intent.getStringExtra(BluetoothLeService.DOOR_CONTACT_DATA), position);
                    mAdapter.notifyDataSetChanged();
                }
                if (bundle.containsKey(BluetoothLeService.DOOR_STRIKE_DATA)) {
                    displayDoorStrikeData(intent.getStringExtra(BluetoothLeService.DOOR_STRIKE_DATA), position);
                    mAdapter.notifyDataSetChanged();
                }
                if (bundle.containsKey(BluetoothLeService.EXTRA_DATA)) {
                    Log.d(TAG, intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
            }
        }
    };

    private void displayDoorStrikeData(String data, int position) {
        if (data != null) {
            if (data.equals("0")) {
                mAdapter.setStrike(position, false);
            }
            if (data.equals("1")) {
                mAdapter.setStrike(position, true);
            }
        }
    }

    private void displayDoorContactData(String data, int position) {
        if (data != null) {
            if (data.equals("0")) {
                mAdapter.setDoor(position, false);
            }
            if (data.equals("1")) {
                mAdapter.setDoor(position, true);
            }
        }
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        aSwitch = findViewById(R.id.main2_bluetooth_switch);
        // Checking whether the bluetooth is active, and if not, asks for permission to activate it.
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
        if (savedInstanceState == null) {
            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();
            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {
                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {
                    aSwitch.setChecked(true);
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            }
        }
        button = (Button) findViewById(R.id.main2_refresh_button);

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Initialize the gridView and link it to the adapter that will populate it.
        gridView = findViewById(R.id.grid);
        griditem = findViewById(R.id.griditem);


        mAdapter = new gridItemAdapter(getApplicationContext(),
                LayoutInflater.from(this));
        gridView.setAdapter(mAdapter);
        gridView.setClickable(true);
        /* Set the itemclickListener for the gridview, in this case, onclick means to send an open
         * request for the raspberrypie since if everything works correctly the service will keep an
         * eye on all the different devices(when we have other devices, that is).
         */
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick position:" + position);
                if (mAdapter.getConnection(position)) {
                    BluetoothGattCharacteristic characteristic = mAdapter.getItem(position, gridItemAdapter.REQ);
                    byte[] data = {1,2,3,4,5,6,7,8,9,0};
                    characteristic.setValue(data);
                    ScanResult scanResult = (ScanResult) mAdapter.getItem(position);
                    String address = scanResult.getDevice().getAddress();
                    mBluetoothLeService.writeCharacteristic(characteristic, address);
                }

            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!scanning) {
                    button.setBackground(getDrawable(R.drawable.ic_action_stop));
                    startScanning();
                }
                else {
                    button.setBackground(getDrawable(R.drawable.ic_action_refresh));
                    stopScanning();
                }
            }
        });

        // Logic for the bluetoothSwitch, as well as defining the visibility of the refresh button.
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    mBluetoothAdapter.disable();
                    button.setVisibility(View.INVISIBLE);
                }
                else {
                    mBluetoothAdapter.enable();
                    button.setVisibility(View.VISIBLE);
                }
            }
        });
        final Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DESCRIPTOR_WRITE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);

        return intentFilter;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                aSwitch.setChecked(true);
            }
            else {
                aSwitch.setChecked(false);
            }
        }
    }
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        //builder.setServiceUuid(Constants.Service_UUID);
        //builder.setServiceUuid(Constants.magLock_UUID);
        //scanFilters.add(builder.build());
        //builder.setServiceUuid(Constants.magLock_UUID2);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        //builder.setReportDelay(1000);
        return builder.build();
    }
    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

            mScanCallback = new SampleScanCallback();
            scanning = true;
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

        } else {
            Toast.makeText(this, R.string.already_scanning, Toast.LENGTH_LONG).show();
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

    private class SampleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //noinspection ConstantConditions
            List<ParcelUuid> parcelUuids = result.getScanRecord().getServiceUuids();
            if (parcelUuids == null) {
                Log.d(TAG, "No ServiceUUIDs, returning.");
                return;
            }
            else {
                Log.d(TAG, "Device:"+result.getScanRecord().getDeviceName());
                for (ParcelUuid uuid : parcelUuids) {
                    Log.d(TAG, "uuid:" + uuid.toString());
                }
            }
            if (hasMyService(result.getScanRecord())) {
                if (mAdapter.getPosition(result.getDevice().getAddress())==-1) {
                    mAdapter.add(result);
                    mBluetoothLeService.connect(result.getDevice().getAddress());

                    int count = mAdapter.getCount();
                    int base = dp(100);
                    ViewGroup.LayoutParams params = gridView.getLayoutParams();
                    if (count == 0) {
                        params.height = 10;
                        params.width = 10;
                    }
                    else if (count < 4) {
                        gridView.setNumColumns(count);
                        params.height = base;
                        params.width = (base)*count;
                    }
                    else {
                        int major, minor;
                        major = count/3;
                        minor = count%3;
                        if (minor != 0) {
                            major++;
                        }
                        params.height = (base)*major;
                    }
                    gridView.setLayoutParams(params);
                    gridView.invalidate();
                }
                else mAdapter.add(result);
            }

            mAdapter.notifyDataSetChanged();
            Log.d(TAG, result.toString());
            Log.d(TAG, "Done");
        }
    }

    private void getGattServices(List<BluetoothGattService> supportedGattServices, int position) {
        if (supportedGattServices==null) return;

        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknowCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();

        for (BluetoothGattService service:supportedGattServices) {
            if (service.getUuid().equals(SampleGattAttributes.DOOR_SERVICE_UUID)) {
                mAdapter.addCharacteristics(position,
                        service.getCharacteristic(SampleGattAttributes.DOOR_CONTACT_CHARACTERISTIC),
                        gridItemAdapter.CONTACT);
                mAdapter.addCharacteristics(position,
                        service.getCharacteristic(SampleGattAttributes.DOOR_STRIKE_CHARACTERISTIC),
                        gridItemAdapter.STRIKE);
                mAdapter.addCharacteristics(position,
                        service.getCharacteristic(SampleGattAttributes.DOOR_REQ_CHARACTERISTIC),
                        gridItemAdapter.REQ);
            }
        }
    }

    public int dp(int value) {
        return (int) (value * Resources.getSystem().getDisplayMetrics().density + 0.5f);
    }
}
