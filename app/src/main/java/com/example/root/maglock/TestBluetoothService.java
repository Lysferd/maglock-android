package com.example.root.maglock;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TestBluetoothService extends Service {

    private final static String TAG = "MagLock."+TestBluetoothService.class.getSimpleName();
    private static final String STATE_CONNECTING =
            "STATE_CONNECTING";
    public static final String ANDROID_CHANNEL_NAME = "ANDROID CHANNEL";
    private static final String DISMISSED_ACTION = "NOTIFICATION DISMISSED";
    private static final String ID_DATA = "ID NUMBER FOR NOTIFICATION";


    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothLeScanner mBluetoothLeScanner;

    private ScanCallback mScanCallback;
    private NotificationManagerCompat mNotificationManager;

    private String mBluetoothDeviceAddress;
    private String mConnectionState;

    private ArrayList<String> addressList;

    private boolean scanning;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // return super.onStartCommand(intent, flags, startId);
        Toast.makeText(this, "TestBluetoothService starting.", Toast.LENGTH_SHORT).show();

        initialize();
        createNotificationChannel();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onCreate() {
        mNotificationManager = NotificationManagerCompat.from(getApplicationContext());
        addressList = new ArrayList<>();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        // super.onDestroy();
        Toast.makeText(this, "TestBluetoothService done.", Toast.LENGTH_SHORT).show();
    }
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothLeScanner == null) {
            return false;
        }
        startScanning();
        return true;
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    public boolean connect(String address) {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "BluetoothAdapter not initialized, connection failed.");
            return false;
        }
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.d(TAG, "No valid address, connection failed.");
            return false;
        }
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
           Log.d(TAG, "Using existing BluetoothGatt to connect.");
           if (mBluetoothGatt.connect()) {
               Log.d(TAG, "mBluetoothGatt.connect() returned true.");
               mConnectionState = STATE_CONNECTING;
               return true;
           } else {
               Log.d(TAG, "Connection Failed, returning false and erasing BluetoothGatt and mBluetoothDeviceAddress.");
               mBluetoothGatt.close();
               mBluetoothGatt = null;
               mBluetoothDeviceAddress = null;
               return false;
           }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device==null) {
            Log.d(TAG, "Device not found, returning false");
            return false;
        }
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        switch (connectionState) {
            case BluetoothProfile.STATE_CONNECTED:
            {
                Log.d(TAG, "Already Connected");
                break;
            }
            case BluetoothProfile.STATE_DISCONNECTED:
            {
                device.connectGatt(this, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                break;
            }
        }
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }
    public boolean startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
            {
                //requestoLocationPermission();
                Toast.makeText(this, "Could not start background service without permission to use location, please allow it and try again later", Toast.LENGTH_LONG).show();
                this.stopSelf();
                return false;
            }
            mScanCallback = new SampleScanCallback();
            scanning = true;
            if (mBluetoothLeScanner==null){
                Log.d(TAG, "Scanner NUll, waiting");
                return false;
            }

            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

        } else {
            Toast.makeText(this, R.string.already_scanning, Toast.LENGTH_LONG).show();
        }
        //invalidateOptionsMenu();
        return true;
    }
    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        scanning = false;
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

        // Even if no new results, update 'last seen' times.
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        //builder.setReportDelay(1000);
        return builder.build();
    }

    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you

        builder.setServiceUuid(Constants.magLock_UUID);

        //scanFilters.add(builder.build());
        scanFilters.add(builder.build());

        return scanFilters;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra(ID_DATA, -1);
            if (position >= 0 ) {
                Log.d(TAG, "Position " + position + " removed.(" + addressList.get(position) + ")");
                addressList.remove(position);
            }
        }
    };
    private class SampleScanCallback extends ScanCallback  {



        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            String address = result.getDevice().getAddress();
            String name = result.getDevice().getName();

            if (addressList.contains(address)) {
                Log.d(TAG, "already found, returning. [" + addressList.indexOf(address) + "](" + address + ")");
                return;
            }
            Log.d(TAG, address + name);

            addressList.add(address);
            Intent intent = new Intent(getApplicationContext(), Main2Activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

            Intent dismiss = new Intent(DISMISSED_ACTION);
            dismiss.putExtra(ID_DATA, addressList.indexOf(address));
            PendingIntent pendingDismiss = PendingIntent.getBroadcast(getApplicationContext(), 0, dismiss, 0);
            registerReceiver(receiver, new IntentFilter(DISMISSED_ACTION));



            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "TEST_CHANNEL")
                    .setSmallIcon(R.drawable.outline_lock_24)
                    .setContentTitle(name)
                    .setContentText("Address:" + address)
                    //.setStyle(new NotificationCompat.BigTextStyle()
                      //      .bigText("Much longer text that cannot fit one line..."))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDeleteIntent(pendingDismiss);// This is the intent for when the user dismisses with a swipe.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(ANDROID_CHANNEL_NAME);
            }

            mNotificationManager.notify(addressList.indexOf(address), builder.build());

            super.onScanResult(callbackType, result);
        }
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(ANDROID_CHANNEL_NAME, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
