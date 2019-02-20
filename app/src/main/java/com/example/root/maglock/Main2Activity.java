package com.example.root.maglock;

import android.Manifest;
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
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.root.maglock.SearchActivity.hasMyService;

public class Main2Activity extends AppCompatActivity {
    private String TAG = "MAGLOCK."+SearchActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 400;

    private ScanCallback mScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeService mBluetoothLeService;

    private gridItemAdapter mAdapter;
    private GridView gridView;

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
            }
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);
                mAdapter.setConnection(position, false);
                mAdapter.notifyDataSetChanged();
            }
            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);
                getGattServices(mBluetoothLeService.getSupportedGattServices(address), position);
                mBluetoothLeService.setNotify(address, mAdapter.getItem(position, gridItemAdapter.CONTACT));
            }
            if (BluetoothLeService.ACTION_DESCRIPTOR_WRITE.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        // Checking whether the bluetooth is active, and if not, asks for permission to activate it.
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
        if (savedInstanceState == null) {
            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();
            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {
                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {
                    //aSwitch.setChecked(true);
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            }
        }
        button = (Button) findViewById(R.id.main2_refresh_button);
        aSwitch = findViewById(R.id.main2_bluetooth_switch);
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Initialize the gridView and link it to the adapter that will populate it.
        gridView = findViewById(R.id.grid);

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
                // TO BE DONE
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
                mAdapter.add(result);

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

}
