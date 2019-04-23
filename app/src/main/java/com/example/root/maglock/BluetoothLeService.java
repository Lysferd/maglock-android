package com.example.root.maglock;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class BluetoothLeService extends Service {
    private final static String TAG = "MagLock."+BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private Map<String, BluetoothGatt> connectedDeviceMap;


    private boolean descriptionWrite = false;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public static final int CONTACT = 1;
    public static final int  STRIKE = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_CHARACTERISTIC_NOTIFICATION =
            "ACTION_CHARACTERISTIC_NOTIFICATION";
    public final static String NOTIFICATION_DATA =
            "NOTIFICATION_DATA";
    public final static String ACTION_CHARACTERISTIC_DATA =
            "ACTION_CHARACTERISTIC_DATA";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String BATTERY_DATA =
            "com.example.bluetooth.le.BATTERY_DATA";
    public final static String NAME_DATA =
            "NAME_DATA";
    public final static String APPEARANCE_DATA =
            "APPEARANCE_DATA";
    public final static String HEART_DATA =
            "HEART_DATA";
    public final static String RANDOM_DATA =
            "RANDOM_DATA";
    public final static String TEST_DATA =
            "TEST_DATA";
    public final static String DOOR_CONTACT_DATA =
            "DOOR_CONTACT_DATA";
    public final static String DOOR_STRIKE_DATA =
            "DOOR_STRIKE_DATA";
    public final static String ACTION_DESCRIPTOR_WRITE =
            "ACTION_DESCRIPTOR_WRITE";
    public final static String DEVICE_DATA =
            "DEVICE_DATA";
    public final static String CHARACTERISTIC_TYPE =
            "CHARACTERISTIC_TYPE";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    public final static String SERIAL_DESCRIPTOR =
            "SERIAL_DESCRIPTOR";
    public final static String SERIAL_DATA =
            "SERIAL_DATA";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            BluetoothDevice device = gatt.getDevice();
            String address = device.getAddress();

            if (newState == BluetoothProfile.STATE_CONNECTED) {

                if (!connectedDeviceMap.containsKey(address)) {
                    connectedDeviceMap.put(address, gatt);
                }

                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction, device);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        gatt.discoverServices());


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                if (connectedDeviceMap.containsKey(address)){
                    BluetoothGatt bluetoothGatt = connectedDeviceMap.get(address);
                    if( bluetoothGatt != null ){
                        bluetoothGatt.close();
                        bluetoothGatt = null;
                    }
                    connectedDeviceMap.remove(address);
                }

                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction, device);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothDevice device = gatt.getDevice();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }

/*
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(SampleGattAttributes.BATTERY_SERVICE_UUID)
                            .getCharacteristic(SampleGattAttributes.BATTERY_LEVEL_UUID);

            //gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor =
                    characteristic.getDescriptor(SampleGattAttributes.convertFromInteger(0x2902));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
*/
        }



        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
/*
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(SampleGattAttributes.BATTERY_SERVICE_UUID)
                            .getCharacteristic(SampleGattAttributes.BATTERY_LEVEL_UUID);
            characteristic.setValue(new byte[]{1,1});
            gatt.writeCharacteristic(characteristic);
            */
            BluetoothDevice device = gatt.getDevice();
            UUID characteristic = descriptor.getCharacteristic().getUuid();
            if (characteristic.equals(SampleGattAttributes.DOOR_CONTACT_CHARACTERISTIC)) {
                broadcastUpdate(ACTION_DESCRIPTOR_WRITE, device, CONTACT);
            }
            if (characteristic.equals(SampleGattAttributes.DOOR_STRIKE_CHARACTERISTIC)) {
                broadcastUpdate(ACTION_DESCRIPTOR_WRITE, device, STRIKE);
            }

            broadcastUpdate(ACTION_DESCRIPTOR_WRITE, device);
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG, "READ:"+ String.valueOf(characteristic.getUuid()) + "/n" + "Status:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "READ_SUCCESS");
                BluetoothDevice device = gatt.getDevice();
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, device);
            }
            /*
            if (characteristic.getUuid().equals(SampleGattAttributes.DOOR_CONTACT_CHARACTERISTIC)) {
                BluetoothGattCharacteristic characteristic1 = gatt.getService(SampleGattAttributes.DOOR_SERVICE_UUID)
                        .getCharacteristic(SampleGattAttributes.DOOR_STRIKE_CHARACTERISTIC);
                gatt.readCharacteristic(characteristic1);
            }
            */
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "READ" + String.valueOf(descriptor.getUuid()) + "\n" + "Status:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "READ_SUCCESS");
                BluetoothDevice device = gatt.getDevice();
                broadcastUpdate(ACTION_DATA_AVAILABLE, descriptor, device);
            }
        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            BluetoothDevice device = gatt.getDevice();
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, device);
        }
    };

    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic, BluetoothDevice device) {
        final Intent intent = new Intent(action);
        intent.putExtra(DEVICE_DATA, device);
        Log.d(TAG, device.getAddress() + ":" + characteristic.getUuid().toString());

        if (SampleGattAttributes.DOOR_CONTACT_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data!=null && data.length > 0) {
                if (data.length == 1) {
                    intent.putExtra(DOOR_CONTACT_DATA, String.valueOf(data[0]));
                }
                else
                    intent.putExtra(DOOR_CONTACT_DATA, String.valueOf(false));
            }
        }
        else if (SampleGattAttributes.DOOR_STRIKE_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (data.length == 1) {
                    intent.putExtra(DOOR_STRIKE_DATA, String.valueOf(data[0]));
                }
                else
                    intent.putExtra(DOOR_STRIKE_DATA, String.valueOf(false));
            }
        }
        else if (SampleGattAttributes.SERIAL_NUMBER_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (data.length == 25) {
                    Log.d(TAG, "RECEIVED 25 BYTES");
                }
                /*StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02X", byteChar));
                }*/
                String string = characteristic.getStringValue(0);
                Log.d(TAG, string);
                intent.putExtra(SERIAL_DATA, string);
            }
        }
        else {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                    Log.d(TAG, String.valueOf(byteChar));
                }
                Log.d(TAG, new String(data));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    private void broadcastUpdate(String action, BluetoothDevice device, int type) {
        final Intent intent = new Intent(action);
        intent.putExtra(DEVICE_DATA, device);
        intent.putExtra(CHARACTERISTIC_TYPE, type);

        sendBroadcast(intent);
    }


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    private void broadcastUpdate(final String action, BluetoothDevice device) {
        final Intent intent = new Intent(action);
        intent.putExtra(DEVICE_DATA, device);

        Log.d(TAG, "broadcastUpdate called");
        Log.d(TAG, intent.toString());
        sendBroadcast(intent);
    }

    private void broadcastUpdate(String action, BluetoothGattDescriptor descriptor, BluetoothDevice device) {
        final Intent intent = new Intent(action);
        intent.putExtra(DEVICE_DATA, device);

        Log.d(TAG, String.valueOf(descriptor));
        if (SampleGattAttributes.SERIAL_NUMBER_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
            final byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                if (data.length == 25) {
                    Log.d(TAG, "25 byte received.");
                }
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02x ", byteChar));
                    Log.d(TAG, String.valueOf(byteChar));
                }
                intent.putExtra(SERIAL_DESCRIPTOR, stringBuilder.toString());
            }
        }
        else {
            final byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                    Log.d(TAG, String.valueOf(byteChar));
                }
                Log.d(TAG, new String(data));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattDescriptor descriptor) {
        final Intent intent = new Intent(action);

        Log.d(TAG, String.valueOf(descriptor));

        if (SampleGattAttributes.TEST_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
            final byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {
                if (data.length == 1) {
                    intent.putExtra(TEST_DATA, String.valueOf(data[0]));
                    /*
                    if (data[0] == 0) {
                        intent.putExtra(TEST_DATA, String.valueOf(false));
                    }
                    if (data[0] == 1) {
                        intent.putExtra(TEST_DATA, String.valueOf(true));
                    }
                    */
                }
                else {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data) {
                        stringBuilder.append(String.format("%02X ", byteChar));
                        Log.d(TAG, String.valueOf(byteChar));
                    }
                    intent.putExtra(TEST_DATA, new String(data) + "\n" + stringBuilder.toString());
                }
            }
        } else if (SampleGattAttributes.SERIAL_NUMBER_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
            final byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                if (data.length == 25) {
                    Log.d(TAG, "25 byte received.");
                }
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02x ", byteChar));
                    Log.d(TAG, String.valueOf(byteChar));
                }
                intent.putExtra(SERIAL_DESCRIPTOR, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        else {
            final byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                    Log.d(TAG, String.valueOf(byteChar));
                }
                Log.d(TAG, new String(data));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);


        Log.d(TAG, String.valueOf(characteristic));

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (SampleGattAttributes.HEART_RATE_MEASUREMENTE_UUID.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(HEART_DATA, String.valueOf(heartRate));
        } else if (SampleGattAttributes.BATTERY_LEVEL_UUID.equals(characteristic.getUuid())) {
            int format = -1;
            int flag = characteristic.getProperties();
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
            Log.d(TAG, "Battery level properties: " + flag );
            Log.d(TAG, "Battery level format UINT8");
            final byte[] data = characteristic.getValue();
            int length = data.length;
            Log.d(TAG, "length " + length );
            for (int i=0;i<length;i++)
            {
                String v = String.valueOf(data[i]);
                Log.d(TAG, "byte " + (i+1) + ": " + data[i]);
            }
            Log.d(TAG, "Battery level RAW:" + data );

            final Integer batteryLevel = characteristic.getIntValue(format, 0);
            Log.d(TAG, String.format("Battery Level: %d", batteryLevel));
            intent.putExtra(BATTERY_DATA, String.valueOf(batteryLevel));
        }
        else if (SampleGattAttributes.DEVICE_NAME_UUID.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            byte handler = (byte) flag;
            Log.d(TAG, "Device name properties: " + flag + "(" + handler + ")");
            final byte[] data = characteristic.getValue();
            int length = data.length;
            Log.d(TAG, "length " + length );
            for (int i=0;i<length;i++)
            {
                String v = String.valueOf(data[i]);
                Log.d(TAG, "byte " + (i+1) + ": " + v );
            }
            String s = new String(data);
            Log.d(TAG, "data string: " + s );
            final String deviceName = characteristic.getStringValue(0);
            Log.d(TAG, "Device Name: " + deviceName);
            intent.putExtra(NAME_DATA, deviceName);
        }
        else if (SampleGattAttributes.APPEARANCE_UUID.equals(characteristic.getUuid())) {
            int format = -1;
            int flag = characteristic.getProperties();
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
            Log.d(TAG, "Appearance properties: " + flag);
            Log.d(TAG, "Appearance format UINT16");

            final byte[] appearance = characteristic.getValue();
            //Log.d(TAG, "Appearance: " + appearance);
            //intent.putExtra(APPEARANCE_DATA, String.valueOf(appearance));
            if(appearance != null && appearance.length >0) {
                final StringBuilder stringBuilder = new StringBuilder(appearance.length);
                int count = 0;
                for (byte byteChar : appearance) {
                    Log.d(TAG, "Byte" + count + ": " + byteChar );
                    stringBuilder.append(String.format("%02X ", byteChar));
                    count++;
                }
                intent.putExtra(APPEARANCE_DATA, new String(appearance) + "\n" + stringBuilder.toString());
            }
        }
        else if (SampleGattAttributes.RANDOM_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                    Log.d(TAG, String.valueOf(byteChar));
                    Log.d(TAG, String.valueOf(stringBuilder));
                }
                Log.d(TAG, new String(data));
                intent.putExtra(RANDOM_DATA, stringBuilder.toString());
            }
        }
        else if (SampleGattAttributes.DOOR_CONTACT_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (data.length == 1) {
                    intent.putExtra(DOOR_CONTACT_DATA, String.valueOf(data[0]));
                }
                else
                    intent.putExtra(DOOR_CONTACT_DATA, String.valueOf(false));
            }
        }
        else if (SampleGattAttributes.DOOR_STRIKE_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (data.length == 1) {
                    intent.putExtra(DOOR_STRIKE_DATA, String.valueOf(data[0]));
                }
                else
                    intent.putExtra(DOOR_STRIKE_DATA, String.valueOf(false));
            }
        }
        else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d(TAG, new String(data));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, String address) {
        BluetoothGatt gatt = connectedDeviceMap.get(address);
        if (gatt == null || mBluetoothAdapter == null) {
            return;
        }
        gatt.writeCharacteristic(characteristic);
    }

    public boolean getConnectionState(String address) {
        BluetoothGatt gatt = connectedDeviceMap.get(address);
        if (gatt == null || mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

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

        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connectedDeviceMap = new HashMap<String, BluetoothGatt>();
        Log.d(TAG, "OnCreate Called, test SUCCESS");
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                Log.d(TAG, "mBluetoothGatt.connect() returned true.");
                mConnectionState = STATE_CONNECTING;
                Log.d(TAG, "Connection state = " + mConnectionState);
                return true;
            } else {
                Log.d(TAG, "Connection Failed.");
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if(connectionState == BluetoothProfile.STATE_DISCONNECTED ){
            // connect your device
            device.connectGatt(this, false, mGattCallback, TRANSPORT_LE);
            Log.d(TAG, "connectGatt");
        }else if( connectionState == BluetoothProfile.STATE_CONNECTED ){
            // already connected . send Broadcast if needed
        }


        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        //mBluetoothGatt = device.connectGatt(this, false, mGattCallback, TRANSPORT_LE);

        //Log.d(TAG, "Trying to create a new connection.");
        //Log.d(TAG, "ConnectGatt() - " + mBluetoothGatt.toString());


        /*
            END
         */
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    public String getDeviceName() {
        BluetoothDevice dev = mBluetoothGatt.getDevice();
        String deviceAlias = dev.getName();
        Log.d(TAG, "Device Alias - " + deviceAlias);
        return deviceAlias;
    }

    public String getDeviceAddress() {
        BluetoothDevice device = mBluetoothGatt.getDevice();
        String deviceAddress = device.getAddress();
        Log.d(TAG, "Device Address - " + deviceAddress);
        return deviceAddress;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {

        for (BluetoothGatt gatt : connectedDeviceMap.values()) {
            gatt.disconnect();
            //gatt.close();
            connectedDeviceMap.remove(gatt.getDevice().getAddress());
        }
        //connectedDeviceMap.clear();
        /*if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }*/
        //mBluetoothGatt.disconnect();
    }

    /**
     * Disconnects an individual existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     * @param address - The address from the device you wish to disconnect from, this is used to get
     *                the correct BluetoothGatt from connectedDeviceMap
     */
    public void disconnect(String address) {
        BluetoothGatt gatt = connectedDeviceMap.get(address);
        assert gatt != null;
        gatt.disconnect();
        connectedDeviceMap.remove(address);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic, String address) {
        BluetoothGatt gatt = connectedDeviceMap.get(address);
        if (gatt == null || mBluetoothAdapter == null) {
            return;
        }
        gatt.readCharacteristic(characteristic);

        /*
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        */
    }

    public void  writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetootAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void readDescriptor(BluetoothGattDescriptor descriptor) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readDescriptor(descriptor);
    }
    public void readDescriptor(String address, BluetoothGattDescriptor descriptor) {
        if (connectedDeviceMap.containsKey(address) && mBluetoothAdapter != null) {
            BluetoothGatt gatt = connectedDeviceMap.get(address);
            assert gatt != null;
            gatt.readDescriptor(descriptor);
        }
        else {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
    }

    public void writeDescriptor(BluetoothGattDescriptor descriptor) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetootAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        /* This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }*/

        final Intent intent = new Intent(ACTION_CHARACTERISTIC_NOTIFICATION);
        intent.putExtra(ACTION_CHARACTERISTIC_DATA, characteristic);
        intent.putExtra(NOTIFICATION_DATA, enabled);
        sendBroadcast(intent);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address) {

        BluetoothGatt gatt = connectedDeviceMap.get(address);
        if (gatt == null) return null;

        return gatt.getServices();
    }

    public void notifyHeart() {
        BluetoothGattCharacteristic characteristic =
                mBluetoothGatt.getService(SampleGattAttributes.HEART_RATE_UUID)
                        .getCharacteristic(SampleGattAttributes.HEART_RATE_MEASUREMENTE_UUID);
        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        mBluetoothGatt.readCharacteristic(characteristic);
        Log.d(TAG, "NotifyHeart called");
    }

    public void setNotify(String address, BluetoothGattCharacteristic characteristic) {
        BluetoothGatt gatt = connectedDeviceMap.get(address);
        if (gatt==null) return;

        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.
                getDescriptor(SampleGattAttributes.convertFromInteger(0x2902));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }
}
