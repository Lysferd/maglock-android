package com.example.root.maglock;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DetailsActivity extends Activity {

    private BluetoothLeService mBluetoothLeService;

    private String TAG = DetailsActivity.class.getSimpleName();

    public final static String address = "ADDRESS";

    private String mAddress;
    private String LIST_NAME = "NAME";
    private String LIST_UUID = "UUID";

    private boolean mConnected = false;
    private boolean mConnecting = false;
    private boolean called = false;
    private boolean notify = true;

    private int count = 0;

    private TextView mDeviceName;
    private TextView mLed;
    private TextView mExtra;
    private LinearLayout extraLine;

    private Handler handler;
    private ProgressBar connecting;
    private LinearLayout detailsLayout;
    private ImageButton imageButton;
    private ImageButton magnetImage;
    private ImageView imageView;

    private BluetoothGattCharacteristic mDeviceNameCharacteristic;
    private BluetoothGattService mDeviceNameService;

    private BluetoothGattCharacteristic mDoorContactCharacteristic;
    private BluetoothGattCharacteristic mDoorStrikeCharacteristic;
    private BluetoothGattCharacteristic mDoorReqCharacteristic;

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

            if (BluetoothLeService.ACTION_DESCRIPTOR_WRITE.equals(action)) {
                    switch (count) {
                        case 0: // Door_Contact
                            setNotification(mDoorStrikeCharacteristic, notify);
                            count++;
                            break;
                        case 1: // Door_Strike
                            count=0;
                            break;
                        default: // Missed something
                            break;
                    }
            }

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_CONNECTED" );

                mConnected = true;
                mConnecting = false;


            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_DISCONNECTED");
                mConnected = false;
                mConnecting = false;

                Toast.makeText(getApplicationContext(), "Connection Failed, retrying.", Toast.LENGTH_SHORT).show();
                mConnecting = true;

                mBluetoothLeService.connect(mAddress);

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mDeviceNameService != null && mDeviceNameCharacteristic!= null) {
                            final BluetoothGattCharacteristic characteristic = mDeviceNameCharacteristic;
                            final int charaProp = characteristic.getProperties();
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0 ) {
                                //mBluetoothLeService.readCharacteristic(characteristic);
                            }
                        }
                    }
                });

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "ACTION_DATA_AVAILABLE");
                String name = intent.getStringExtra(BluetoothLeService.NAME_DATA);

                if (name != null) {
                    displayName(name);
                    /*
                    mBluetoothLeService.setCharacteristicNotification(mDoorContactCharacteristic, true);
                    final BluetoothGattDescriptor descriptor =
                            mDoorContactCharacteristic.getDescriptor(SampleGattAttributes.convertFromInteger(0x2902));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothLeService.writeDescriptor(descriptor);
                    */
                    setNotification(mDoorContactCharacteristic, true);
                }
                String doorContact = intent.getStringExtra(BluetoothLeService.DOOR_CONTACT_DATA);
                if (doorContact != null) {
                    displayDoorContact(doorContact);
                    if (!called) {
                        //mBluetoothLeService.readCharacteristic(mDoorStrikeCharacteristic);
                        mBluetoothLeService.setCharacteristicNotification(mDoorStrikeCharacteristic, true);
                        final BluetoothGattDescriptor descriptor1 =
                                mDoorStrikeCharacteristic.getDescriptor(SampleGattAttributes.convertFromInteger(0x2902));
                        descriptor1.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothLeService.writeDescriptor(descriptor1);
                        called = true;
                    }
                }

                String doorStrike = intent.getStringExtra(BluetoothLeService.DOOR_STRIKE_DATA);
                if (doorStrike != null) {
                    displayDoorStrike(doorStrike);
                }
                displayExtra(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                Log.d(TAG, String.valueOf(intent.getExtras())) ;
                invalidateOptionsMenu();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        final Intent intent = getIntent();
        mAddress = intent.getStringExtra(address);
        Log.d(TAG, "Address:"+mAddress);

        mDeviceName = (TextView) findViewById(R.id.mDeviceName);
        mLed = (TextView) findViewById(R.id.mLed);
        mExtra = (TextView) findViewById(R.id.mExtra);
        extraLine = (LinearLayout) findViewById(R.id.extraLine);
        imageButton = (ImageButton) findViewById(R.id.imageKey);
        magnetImage = (ImageButton) findViewById(R.id.imageMagnet);

        handler = new Handler();

        detailsLayout = findViewById(R.id.details);
        connecting = findViewById(R.id.connecting);

        handler.post(connectingRunnable);

        imageView = (ImageView) findViewById(R.id.refresh_lock_button);
        imageView.setOnClickListener(onClickListener);



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
        if (mConnected && mBluetoothLeService != null) {
            /*mBluetoothLeService.setCharacteristicNotification(mDoorContactCharacteristic, false);
            final BluetoothGattDescriptor descriptor =
                    mDoorContactCharacteristic.getDescriptor(SampleGattAttributes.convertFromInteger(0x2902));
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mBluetoothLeService.writeDescriptor(descriptor);
            mBluetoothLeService.setCharacteristicNotification(mDoorStrikeCharacteristic, false);
            final BluetoothGattDescriptor descriptor1 =
                    mDoorStrikeCharacteristic.getDescriptor(SampleGattAttributes.convertFromInteger(0x2902));
            descriptor1.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mBluetoothLeService.writeDescriptor(descriptor1);
            */
            notify = false;
            setNotification(mDoorContactCharacteristic, notify);
            mBluetoothLeService.disconnect();
            mConnected = false;
        }
        Log.d(TAG, "isFinishing():" + isFinishing());
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "OnDestroy() called");
        if (mConnected) {
            /*mBluetoothLeService.setCharacteristicNotification(mDoorContactCharacteristic, false);
            final BluetoothGattDescriptor descriptor =
                    mDoorContactCharacteristic.getDescriptor(SampleGattAttributes.convertFromInteger(0x2902));
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mBluetoothLeService.setCharacteristicNotification(mDoorStrikeCharacteristic, false);
            final BluetoothGattDescriptor descriptor1 =
                    mDoorStrikeCharacteristic.getDescriptor(SampleGattAttributes.convertFromInteger(0x2902));
            descriptor1.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mBluetoothLeService.writeDescriptor(descriptor1);*/
            notify = false;
            setNotification(mDoorContactCharacteristic, false);
            mBluetoothLeService.disconnect();
            mConnected = false;
        }
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    public void returnToStart(View view) {
        setResult(RESULT_OK, null);
        finish();
    }

    public void back(View view) {
        finish();
    }

    private void displayGattServices(List<BluetoothGattService> supportedGattServices) {
        if (supportedGattServices == null) return;

        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknowCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();

        for (BluetoothGattService gattService : supportedGattServices) {
            uuid = gattService.getUuid().toString();
            Log.d(TAG, "uuid-" + uuid);
            HashMap<String, String> currentServiceData = new HashMap<>();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<>();

            if (gattService.getUuid().equals(SampleGattAttributes.GENERIC_ACCESS_UUID)) {
                mDeviceNameService = gattService;
                mDeviceNameCharacteristic =
                        gattService.getCharacteristic(SampleGattAttributes.DEVICE_NAME_UUID);
            }
            if (gattService.getUuid().equals(SampleGattAttributes.DOOR_SERVICE_UUID)) {
                mDoorContactCharacteristic =
                        gattService.getCharacteristic(SampleGattAttributes.DOOR_CONTACT_CHARACTERISTIC);
                mDoorStrikeCharacteristic =
                        gattService.getCharacteristic(SampleGattAttributes.DOOR_STRIKE_CHARACTERISTIC);
                mDoorReqCharacteristic =
                        gattService.getCharacteristic(SampleGattAttributes.DOOR_REQ_CHARACTERISTIC);
            }
            else
                mDoorContactCharacteristic = null;

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                Log.d(TAG, "Characteristic UUID-" + uuid);

                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknowCharaString)
                );
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private void displayName(String data) {
        if (data != null) {
           mDeviceName.setText(data);
           mDeviceName.setVisibility(View.VISIBLE);
        }
    }
    private void displayDoorContact(String data) {
        if (data != null) {
            //mLed.setText(data);
            if (data.equals("0")) {
                imageButton.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.colorRed, null));
                imageButton.setImageResource(R.drawable.door_open);
            }
            if (data.equals("1")) {
                imageButton.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.colorGreen, null));
                imageButton.setImageResource(R.drawable.door_closed);
            }
        }
    }
    private void displayDoorStrike(String data) {
        if (data != null) {
            //mLed.setText(data);
            if (data.equals("0")) {
                magnetImage.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.colorRed, null));
                magnetImage.setImageResource(R.drawable.magnet);

                imageView.setImageResource(R.drawable.lock_open_outline);
            }
            if (data.equals("1")) {
                magnetImage.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.colorGreen, null));
                magnetImage.setImageResource(R.drawable.magnet_on);

                imageView.setImageResource(R.drawable.lock);
            }
        }
    }
    private void displayExtra(String data) {
        if (data != null) {
            extraLine.setVisibility(View.VISIBLE);
            String text;
            if (data.equals("1"))
                text = "TRUE";
            else if (data.equals("0"))
                text = "FALSE";
            else
                text = "-ERROR: unexpected value(not 0|1)";

            mExtra.setText(text);
        }
    }

    Runnable connectingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mConnecting) {
                connecting.setVisibility(View.GONE);
                if (mConnected) {
                    detailsLayout.setVisibility(View.VISIBLE);
                }
                else
                    detailsLayout.setVisibility(View.GONE);
            }
            else
                connecting.setVisibility(View.VISIBLE);
            handler.postDelayed(this, 100);
        }
    };

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mDoorReqCharacteristic == null) {
                Log.d(TAG, "Characteristic not found");
            }
            else {
                Log.d(TAG, "Writing REQCharacteristic");
                String tempID = "123456";
                byte[] data = {0,1,2,3,4,5,6};
                mDoorReqCharacteristic.setValue(data);

                //mDoorReqCharacteristic.setValue(Integer.parseInt(tempID), BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                mBluetoothLeService.writeCharacteristic(mDoorReqCharacteristic);
            }
        }
    };

    private void setNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (characteristic == null){
            Log.d(TAG, "ERROR: Null characteristic");
            return;
        }
        mBluetoothLeService.setCharacteristicNotification(characteristic, enabled);
        final BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(SampleGattAttributes.convertFromInteger(0x2902));
        if (enabled)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        else
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothLeService.writeDescriptor(descriptor);
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);

        intentFilter.addAction(BluetoothLeService.ACTION_DESCRIPTOR_WRITE);
        return intentFilter;
    }
}
