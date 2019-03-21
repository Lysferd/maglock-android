package com.example.root.maglock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class Main2Activity extends AppCompatActivity {
    private static String TAG = "MAGLOCK." + Main2Activity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_LOCATION = 400;

    private ScanCallback mScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeService mBluetoothLeService;

    private gridItemAdapter mAdapter;
    private GridView gridView;
    private ProgressBar progressBar;
    private Dialog dialog;
    private TextView textView;

    private Switch aSwitch;
    private Button button;
    private Handler handler;

    private boolean scanning = false;
    private boolean stateChanged = false;
    private volatile boolean taskComplete = false;
    private boolean activeNet = false;
    private boolean itemClicked = false;

    private AmazonDynamoDBAsyncClient dynamoDB;

    private AWSMobileClient auth;

    private CognitoCachingCredentialsProvider credentialsProvider;

    private List<itemWithDate> doorEventsList;
    private itemWithDate lastItem;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.d(TAG, "Could not start Bluetooth");
                Toast.makeText(getApplicationContext(),
                        "Bluetooth failed", Toast.LENGTH_SHORT).show();
                finish();
            } else {
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
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                Log.d(TAG, "ACTION_STATE_CHANGED");
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (state == (BluetoothAdapter.STATE_ON)) {
                    Log.d(TAG, "STATE_ON");
                    if (stateChanged) {
                        for (int i = 0; i < mAdapter.getCount(); i++) {
                            ScanResult result = (ScanResult) mAdapter.getItem(i);
                            mBluetoothLeService.connect(result.getDevice().getAddress());
                        }
                    }
                }
                if (state == (BluetoothAdapter.STATE_OFF)) {
                    Log.d(TAG, "STATE_OFF");
                    stateChanged = true;
                    mBluetoothLeService.disconnect();
                }

            }
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                if (!itemClicked) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                    String address = device.getAddress();
                    int position = mAdapter.getPosition(address);
                    mAdapter.setConnection(position, true);
                    mAdapter.notifyDataSetChanged();
                    stopScanning();
                }
                else {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                    String address = device.getAddress();
                    int position = mAdapter.getPosition(address);
                    BluetoothGattCharacteristic characteristic = mAdapter.getItem(position, gridItemAdapter.REQ);
                    if (characteristic!=null) {
                        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
                        characteristic.setValue(data);
                        mBluetoothLeService.writeCharacteristic(characteristic, address);
                        mBluetoothLeService.disconnect();
                    }
                }
            }
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_DISCONNECTED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);
                mAdapter.setConnection(position, false);
                mAdapter.notifyDataSetChanged();
                if (!itemClicked) {
                    mBluetoothLeService.connect(address);
                }
                else {
                    itemClicked = false;
                }
            }
            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.DEVICE_DATA);
                String address = device.getAddress();
                int position = mAdapter.getPosition(address);
                getGattServices(mBluetoothLeService.getSupportedGattServices(address), position);
                //mBluetoothLeService.setNotify(address, mAdapter.getItem(position, gridItemAdapter.CONTACT));
                //startScanning();
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

        // First Check if there is internet connection;
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        activeNet = activeNetwork != null &&
                activeNetwork.isConnected();

        // Then, call the function that connects to the AWS.
        AWSConnection();

        // Initialize all layout items.
        aSwitch = findViewById(R.id.main2_bluetooth_switch);
        button = (Button) findViewById(R.id.main2_refresh_button);
        gridView = findViewById(R.id.grid);
        progressBar = findViewById(R.id.grid_progressbar);
        mAdapter = new gridItemAdapter(getApplicationContext(),
                LayoutInflater.from(this));
        gridView.setAdapter(mAdapter);
        gridView.setClickable(true);
        gridView.setContextClickable(true);
        dialog = new Dialog(this);
        registerForContextMenu(gridView);
        handler = new Handler();


        /* Set the itemclickListener for the gridview, in this case, onclick means to send an open
         * request for the raspberrypie since if everything works correctly the service will keep an
         * eye on all the different devices(when we have other devices, that is).
         */
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick position:" + position);

                stopScanning();
                if (!mAdapter.getConnection(position)) {
                    ScanResult scanResult = (ScanResult) mAdapter.getItem(position);
                    String address = scanResult.getDevice().getAddress();
                    mBluetoothLeService.connect(address);
                    itemClicked = true;
                    /*
                     If the device is not connected(default), get the device address and connect
                     to it, then toogle the itemClicked flag(This will act when connection is done.
                     */
                }
                else {
                    mBluetoothLeService.disconnect();
                }

                /*
                if (mAdapter.getConnection(position)) {
                    BluetoothGattCharacteristic characteristic = mAdapter.getItem(position, gridItemAdapter.REQ);
                    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
                    characteristic.setValue(data);
                    ScanResult scanResult = (ScanResult) mAdapter.getItem(position);
                    String address = scanResult.getDevice().getAddress();
                    mBluetoothLeService.writeCharacteristic(characteristic, address);
                }
                */

            }
        });
        // Set the refresh button clickListener.
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!scanning) {
                    if (mBluetoothLeScanner == null) {
                        Toast.makeText(getApplicationContext(), "Scanner not yet ready, try again", Toast.LENGTH_SHORT).show();
                        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    }
                    else {
                        button.setBackground(getDrawable(R.drawable.ic_action_stop));
                        startScanning();
                        progressBar.setVisibility(View.VISIBLE);
                    }
                } else {
                    button.setBackground(getDrawable(R.drawable.ic_action_refresh));
                    stopScanning();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        // Logic for the bluetoothSwitch, as well as defining the visibility of the refresh button.
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        if (button.performClick()) {
                            Log.d(TAG, "button clicked.");
                        }
                    }
                    mBluetoothAdapter.disable();
                    button.setVisibility(View.INVISIBLE);
                    for (int i = 0; i < mAdapter.getCount(); i++) {
                        mAdapter.setConnection(i, false);
                        mAdapter.setStrikeNull(i);
                        mAdapter.setDoorNull(i);
                    }
                    mAdapter.notifyDataSetChanged();
                } else {
                    mBluetoothAdapter.enable();
                    button.setVisibility(View.VISIBLE);
                }
            }
        });

        // Checking whether the bluetooth is active, and if not, asks for permission to activate it.
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_LOCATION);
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
                    button.setVisibility(View.INVISIBLE);
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            }
        }

        assert mBluetoothAdapter != null;
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothLeScanner == null) {
            Log.d(TAG, "error, mBluetoothLeScanner receive NULL");
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                activeNet = activeNetwork != null &&
                        activeNetwork.isConnected() &&
                        activeNetwork.getType()==ConnectivityManager.TYPE_WIFI;
                Log.d(TAG, "ActiveNet:" + activeNet);
                handler.postDelayed(this, 60000);
            }
        }, 60000);

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (activeNet) {
                    // Check the number of items on table.

                    if (mAdapter.getCount() > 0 && doorEventsList != null && doorEventsList.size() > 0) {
                        new getSingleTask().execute();
                        for (int i = 0; i < mAdapter.getCount(); i++) {
                            if (lastItem == null) {
                                lastItem = doorEventsList.get(0);
                            }

                            String event = lastItem.item.get("event").getS();
                            boolean state;

                            Log.d(TAG, "Last Event:" + event);
                            if (event.contains("closed")
                                    || event.contains("unlock")) {
                                state = true;
                                mAdapter.setTableDoor(i, state);
                                mAdapter.notifyDataSetChanged();
                            } else if (event.contains("forced")) {
                                state = false;
                                mAdapter.setTableDoor(i, state);
                                mAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    handler.postDelayed(this, 500);
                }
                else {
                    handler.postDelayed(this, 10000);
                }
            }
        });
        final Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.grid) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            ScanResult res = (ScanResult) mAdapter.getItem(info.position);
            menu.setHeaderTitle(Objects.requireNonNull(res.getScanRecord()).getDeviceName());
            String[] menuItems = getResources().getStringArray(R.array.menu);
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Loading Event Log", Toast.LENGTH_SHORT).show();
                }
            });

            new callingDialogAgain().execute();
        }
        if (item.getItemId() == 1) {
            new getTableCount().execute();
        }
        return super.onContextItemSelected(item);
    }

    private List<itemWithDate> getTable() {
        Condition notNullCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.NOT_NULL);
        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put("date", notNullCondition);
        keyConditions.put("priority", notNullCondition);
        keyConditions.put("event", notNullCondition);

        List<itemWithDate> withDateArrayList = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName("maglock-door1")
                    .withExclusiveStartKey(lastEvaluatedKey)
                    .withScanFilter(keyConditions)
                    .withConsistentRead(true);
            com.amazonaws.services.dynamodbv2.model.ScanResult scanResult = dynamoDB.scan(scanRequest);

            for (Map<String, AttributeValue> item : scanResult.getItems()) {
                itemWithDate tempItem = new itemWithDate();
                String event = item.get("event").getS();
                String date = item.get("date").getS();
                String priority = item.get("priority").getN();

                String dateCut = date.substring(0, 19);
                Date date1;
                String mili = date.substring(20);
                int mili1;
                try {
                    date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateCut);
                    mili1 = Integer.parseInt(mili);
                    tempItem.date = date1;
                    tempItem.milis = mili1;
                    tempItem.item = item;
                    withDateArrayList.add(tempItem);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, event + "-" + date + "(" + priority + ")");
            }
            for (int i = 0; i < withDateArrayList.size(); i++) {
                Log.d(TAG, withDateArrayList.get(i).date.toString());
            }
            Collections.sort(withDateArrayList, new Comparator<itemWithDate>() {
                @Override
                public int compare(itemWithDate o1, itemWithDate o2) {
                    // 1<2:-1
                    // 1==2:0
                    // 1>2:1
                    if (o1.date == null || o2.date == null) {
                        return 0;
                    } else if (o1.date.before(o2.date)) {
                        return -1;
                    } else if (o1.date.after(o2.date)) {
                        return 1;
                    } else {
                        return o1.milis.compareTo(o2.milis);
                    }
                }
            });
            Collections.reverse(withDateArrayList);
            Log.d(TAG, withDateArrayList.toString());
            for (int i = 0; i < withDateArrayList.size(); i++) {
                Log.d(TAG,withDateArrayList.get(i).date.toString() + "-" +
                                withDateArrayList.get(i).item.get("event").getS()
                );
                Log.d(TAG, "------------------------------------------------");
            }
            lastEvaluatedKey = scanResult.getLastEvaluatedKey();
        } while (lastEvaluatedKey != null);
        lastItem = withDateArrayList.get(0);
        return withDateArrayList;
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

        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

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
            } else {
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

    public boolean startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

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
        invalidateOptionsMenu();
        return true;
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


            //List<ParcelUuid> parcelUuids = result.getScanRecord().getServiceUuids();
            //if (parcelUuids == null) {
            //    Log.d(TAG, "No ServiceUUIDs, returning.");
            //    return;
            //} else {
                Log.d(TAG, "Device:" + result.getScanRecord().getDeviceName());
            //    for (ParcelUuid uuid : parcelUuids) {
            //        Log.d(TAG, "uuid:" + uuid.toString());
            //    }
            //}
            //if (hasMyService(result.getScanRecord())) {
                if (mAdapter.getPosition(result.getDevice().getAddress()) == -1) {
                    mAdapter.add(result);
                    //mBluetoothLeService.connect(result.getDevice().getAddress());

                    int count = mAdapter.getCount();
                    int base = dp(100);
                    ViewGroup.LayoutParams params = gridView.getLayoutParams();

                    if (count == 0) {
                        params.height = 10;
                        params.width = 10;
                    } else if (count < 4) {
                        gridView.setNumColumns(count);
                        params.height = base;
                        params.width = (base) * count;
                    } else {
                        int major, minor;
                        major = count / 3;
                        minor = count % 3;
                        if (minor != 0) {
                            major++;
                        }
                        params.height = (base) * major;
                    }
                    gridView.setLayoutParams(params);
                    gridView.invalidate();
                } else {
                    mAdapter.add(result);
                }
            //}

            mAdapter.notifyDataSetChanged();
            Log.d(TAG, result.toString());
            Log.d(TAG, "Done");
        }
    }

    private void getGattServices(List<BluetoothGattService> supportedGattServices, int position) {
        if (supportedGattServices == null) return;

        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknowCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();


        for (BluetoothGattService service : supportedGattServices) {
            Log.d(TAG, service.toString());
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

    private void AWSConnection() {
        // If there is no internet, don't bother.
        if (!activeNet) return;
        // Otherwise, connect to the AWS.
        else {
            final CountDownLatch latch = new CountDownLatch(1);
            AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails result) {
                    Log.d(TAG, "result:" + result.getUserState());
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    Log.d(TAG, "Error:" + e);
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            auth = AWSMobileClient.getInstance();
            auth.signOut();
            try {
                credentialsProvider = new CognitoCachingCredentialsProvider(
                        getApplicationContext(), // Context
                        String.valueOf(auth.getConfiguration()
                                .optJsonObject("CredentialsProvider")
                                .optJSONObject("CognitoIdentity")
                                .optJSONObject("Default")
                                .getString("PoolId")

                        ), // Identity Pool ID
                        Regions.fromName(auth.getConfiguration()
                                .optJsonObject("CredentialsProvider")
                                .optJSONObject("CognitoIdentity")
                                .optJSONObject("Default")
                                .getString("Region")
                        )
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }
            dynamoDB = new AmazonDynamoDBAsyncClient(credentialsProvider);
            new getIdentity().execute(credentialsProvider);
        }
    }
    class itemWithDate {
        Date date;
        Integer milis;
        Map<String, AttributeValue> item;
    }

    public void callingDialog(View view) {

        dialog.setContentView(R.layout.popupwindow);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        textView = dialog.findViewById(R.id.testtextview);
        CardView cardView = dialog.findViewById(R.id.dialog);
        ViewGroup.LayoutParams params = cardView.getLayoutParams();
        params.height = (int) (Resources.getSystem().getDisplayMetrics().heightPixels * 0.8);
        params.width = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.8);
        cardView.setLayoutParams(params);
        ViewGroup.LayoutParams textParams = textView.getLayoutParams();
        textParams.height = (int) (params.height * 0.9);
        textParams.width = (int) (params.width * 0.9);
        textView.setLayoutParams(textParams);
        ScrollView scrollView = dialog.findViewById(R.id.scrollview);
        ViewGroup.LayoutParams scrollparams = scrollView.getLayoutParams();
        scrollparams.width = textParams.width;
        scrollparams.height = textParams.height;
        scrollView.setLayoutParams(scrollparams);
        //textView.setMovementMethod(new ScrollingMovementMethod());

        doorEventsList = new ArrayList<>();

        taskComplete = false;

        new AsyncTask<List<Void>, Void, Void>() {
            @Override
            protected Void doInBackground(List<Void>... lists) {
                doorEventsList = getTable();
                taskComplete = true;
                return null;
            }
        }.execute();
        //Log.d(TAG, dynamoDB.listTables().toString());
        while (!taskComplete);
        StringBuilder builder = new StringBuilder();
        int i = 1;
        for (itemWithDate withDate : doorEventsList) {

            Calendar calendar = DateUtils.toCalendar(withDate.date);

            builder.append(calendar.get(Calendar.DATE)).append("/");
            builder.append(calendar.get(Calendar.MONTH)+1).append("/");
            builder.append(calendar.get(Calendar.YEAR)).append(" ");
            builder.append(calendar.get(Calendar.HOUR_OF_DAY)).append(":");
            builder.append(calendar.get(Calendar.MINUTE)).append(":");
            builder.append(calendar.get(Calendar.SECOND)).append("\n");

            //builder.append(withDate.date).append("\n");

            builder.append(withDate.item.get("event").getS()).append("\n");
            builder.append("\n");
        }
        textView.setText(builder.toString());

        dialog.show();
    }
    public void closeDialog(View view) {
        dialog.dismiss();
    }
    class callingDialogAgain extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            //dialog.setContentView(R.layout.popupwindow);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            textView = dialog.findViewById(R.id.testtextview);
            CardView cardView = dialog.findViewById(R.id.dialog);
            ViewGroup.LayoutParams params = cardView.getLayoutParams();
            params.height = (int) (Resources.getSystem().getDisplayMetrics().heightPixels * 0.8);
            params.width = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.8);
            cardView.setLayoutParams(params);
            ViewGroup.LayoutParams textParams = textView.getLayoutParams();
            textParams.height = (int) (params.height * 0.9);
            textParams.width = (int) (params.width * 0.9);
            textView.setLayoutParams(textParams);
            ScrollView scrollView = dialog.findViewById(R.id.scrollview);
            ViewGroup.LayoutParams scrollparams = scrollView.getLayoutParams();
            scrollparams.width = textParams.width;
            scrollparams.height = textParams.height;
            scrollView.setLayoutParams(scrollparams);
            doorEventsList = new ArrayList<>();
            doorEventsList = getTable();
            StringBuilder builder = new StringBuilder();
            //int i = 1;
            for (itemWithDate withDate : doorEventsList) {

                Calendar calendar = DateUtils.toCalendar(withDate.date);

                builder.append(calendar.get(Calendar.DATE)).append("/");
                builder.append(calendar.get(Calendar.MONTH)+1).append("/");
                builder.append(calendar.get(Calendar.YEAR)).append(" ");
                builder.append(calendar.get(Calendar.HOUR_OF_DAY)).append(":");
                builder.append(calendar.get(Calendar.MINUTE)).append(":");
                builder.append(calendar.get(Calendar.SECOND)).append("\n");

                //builder.append(withDate.date).append("\n");

                builder.append(withDate.item.get("event").getS()).append("\n");
                builder.append("\n");
            }
            textView.setText(builder.toString());

            return null;
        }

        @Override
        protected void onPreExecute() {
            dialog.setContentView(R.layout.popupwindow);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.show();
        }
    }
    class getTableTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            doorEventsList = getTable();
            return null;
        }
    }
    class getTableCount extends AsyncTask<Void, Void, Long> {
        @Override
        protected Long doInBackground(Void... voids) {
            DescribeTableResult describeTableResult = dynamoDB.describeTable("maglock-door1");
            TableDescription tableDescription = describeTableResult.getTable();
            long count = tableDescription.getItemCount();
            Log.d(TAG, "TableDescription:" + count);
            return count;
        }

        @Override
        protected void onPostExecute(final Long aLong) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Count:" + aLong.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    class getSingleTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Condition notNullCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.NOT_NULL);
            Map<String, Condition> keyConditions = new HashMap<>();
            keyConditions.put("date", notNullCondition);
            keyConditions.put("priority", notNullCondition);
            keyConditions.put("event", notNullCondition);

            int hours = doorEventsList.get(0).date.getHours();
            if ( hours == 9 || hours == 19 ){
                // If the time has some chance of changing, consider...
            }
            // If it is the last Hour
            if (doorEventsList.get(0).date.getHours()==23)
            // If it is December.
            if (doorEventsList.get(0).date.getMonth()==11) {

            }
            String dateCut = doorEventsList.get(0).item.get("date").getS().substring(0, 13);

            Log.d(TAG, "dateCut:" + dateCut);
            Log.d(TAG, "lastItem:" + lastItem.item.get("date").getS());

            Condition closeTolast = new Condition()
                    .withComparisonOperator(ComparisonOperator.BEGINS_WITH)
                    .withAttributeValueList(new AttributeValue(dateCut));
            keyConditions.put("date", closeTolast);
            Condition notLast = new Condition()
                    .withComparisonOperator(ComparisonOperator.NE)
                    .withAttributeValueList(new AttributeValue(doorEventsList.get(0).item.get("date").getS()));
            keyConditions.put("date", notLast);

            List<itemWithDate> withDateArrayList = new ArrayList<>();
            Map<String, AttributeValue> lastEvaluatedKey = null;

            do {
                ScanRequest scanRequest = new ScanRequest()
                        .withTableName("maglock-door1")
                        .withExclusiveStartKey(lastEvaluatedKey)
                        .withScanFilter(keyConditions)
                        .withConsistentRead(true);
                com.amazonaws.services.dynamodbv2.model.ScanResult scanResult = dynamoDB.scan(scanRequest);
                if (scanResult.getCount() == 0) {
                    Log.d(TAG, "No new event");
                    // Do logic for no new event(repeat everything until it changes)
                } else if (scanResult.getCount() == 1) {
                    Log.d(TAG, "One new event");

                    for (Map<String, AttributeValue> item : scanResult.getItems()) {
                        // Since it is a single result, no need to use the logic for many.
                        itemWithDate tempItem = null;
                        tempItem.item = item;
                        String date = item.get("date").getS().substring(0,19);
                        try {
                            Date date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    Log.d(TAG, "Two or more new events, call getTable.");
                    for (Map<String, AttributeValue> item : scanResult.getItems()) {


                        itemWithDate tempItem = new itemWithDate();
                        String date = item.get("date").getS();

                        String dateString = date.substring(0, 19);
                        Date date1;
                        String mili = date.substring(20);
                        int mili1;
                        try {
                            date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateString);
                            mili1 = Integer.parseInt(mili);
                            tempItem.date = date1;
                            tempItem.milis = mili1;
                            tempItem.item = item;
                            withDateArrayList.add(tempItem);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }



                    }
                    doorEventsList = getTable();
                }
            } while (lastEvaluatedKey!=null);

            return null;
        }
    }
}

class getIdentity extends AsyncTask<CognitoCachingCredentialsProvider, Void, String> {

    @Override
    protected String doInBackground(CognitoCachingCredentialsProvider... cognitoCachingCredentialsProviders) {
        return cognitoCachingCredentialsProviders[0].getIdentityId();
    }

    @Override
    protected void onPostExecute(String s) {
        if (s!=null) {
            Log.d("Identity", "Success:" + s);
        }
        else Log.d("Identity", "getIdentityId failed - return null");
    }
}

