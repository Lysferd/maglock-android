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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.UserStateListener;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Expression;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Filter;
import com.amazonaws.mobileconnectors.dynamodbv2.document.GetItemOperationConfig;
import com.amazonaws.mobileconnectors.dynamodbv2.document.QueryOperationConfig;
import com.amazonaws.mobileconnectors.dynamodbv2.document.ScanOperationConfig;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Search;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Table;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Primitive;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.example.root.maglock.SearchActivity.hasMyService;

//import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

public class Main2Activity extends AppCompatActivity {
    private static String TAG = "MAGLOCK."+Main2Activity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 400;

    private ScanCallback mScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeService mBluetoothLeService;

    private gridItemAdapter mAdapter;
    private GridView gridView;
    private ProgressBar progressBar;

    private Switch aSwitch;
    private Button button;

    private boolean scanning = false;
    private boolean stateChanged = false;
    private boolean identityChecked = false;

    private SimpleDateFormat sdf;

    private AmazonDynamoDBClient client;
    private AmazonDynamoDB dynamoDB;

    private Context appContext;
    private AWSMobileClient auth;
    private UserStateListener listener;

    private CognitoCachingCredentialsProvider credentialsProvider;

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

        /*
        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();
        */
        /*sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        System.out.println(sdf.format(new Date())); //-prints-> 2015-01-22T03:23:26Z

        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails userStateDetails) {
                Log.i(TAG, "AWSMobileClient initialized. User State is " + userStateDetails.getUserState());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Initialization error.", e);
            }
        });
        uploadWithTransferUtility();
        */
        /*client = .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-east-1"))
                .build();
                */
/*/////////////////////////////////////////////////////////////////////////////////////////////
        String tableName = "Movies";

        CreateTableRequest request = new CreateTableRequest(
                Arrays.asList(new AttributeDefinition("year", ScalarAttributeType.N),
                new AttributeDefinition("title", ScalarAttributeType.S)),
                tableName,
                Arrays.asList(new KeySchemaElement("year", KeyType.HASH), // Partition
                // key
                new KeySchemaElement("title", KeyType.RANGE)), // Sort key);
                new ProvisionedThroughput(10L, 10L)
        );

        client = AmazonDynamoDBClient.builder().build;
        try {
            Log.d(TAG, "Attempting to create table; please wait...");
            Table table = dynamoDB.createTable(tableName,
                    Arrays.asList(new KeySchemaElement("year", KeyType.HASH), // Partition
                            // key
                            new KeySchemaElement("title", KeyType.RANGE)), // Sort key
                    Arrays.asList(new AttributeDefinition("year", ScalarAttributeType.N),
                            new AttributeDefinition("title", ScalarAttributeType.S)),
                    new ProvisionedThroughput(10L, 10L));
            table.waitForActive();
            Log.d(TAG, "Success.  Table status: " + table.getDescription().getTableStatus());

        }
        catch (Exception e) {
            Log.d(TAG, "Unable to create table: ");
            Log.d(TAG, e.getMessage());
        }
        *////////////////////////////////////////////////////////////////////////////////////////////////

        //CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider();



        /////////////////////////////////////////////////
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

        appContext = getApplicationContext();
        auth = AWSMobileClient.getInstance();
        auth.signOut();
        Log.d(TAG, String.valueOf(auth.getConfiguration()));
        try {
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(), // Context
                    String.valueOf(auth.getConfiguration()
                            .optJsonObject("CredentialsProvider")
                            .optJSONObject("CognitoIdentity")
                            .optJSONObject("Default")
                            .getString("PoolId")

                    ), // Identity Pool ID
                    /////////////////////////////Have to use the logic below to get the PoolID and finally populate the credentialsprovider
                    //Regions.US_EAST_1 // Region
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
        AWSConfiguration configuration = auth.getConfiguration();
        try {
            Log.d(TAG, String.valueOf(configuration.optJsonObject("CredentialsProvider")
                    .optJSONObject("CognitoIdentity")
                    .optJSONObject("Default")
                    .getString("PoolId")
            ));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            Log.d(TAG, String.valueOf(configuration
                    .optJsonObject("CredentialsProvider")
                    .optJSONObject("CognitoIdentity")
                    .optJSONObject("Default")
                    .getString("Region")
            ));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Log.d(TAG, credentialsProvider.getIdentityId());

        dynamoDB = new AmazonDynamoDBClient(credentialsProvider);

        new getIdentity().execute("");


                /*
                .withKeySchema(new KeySchemaElement()
                        .withAttributeName("year")
                        .withKeyType(KeyType.HASH))

                .withAttributeDefinitions(new AttributeDefinition()
                        .withAttributeName()
                )
                */
        //Log.d(TAG, credentialsProvider.getIdentityId());
        //dynamoDB = new AmazonDynamoDBClient(credentialsProvider);
        //Log.d(TAG, String.valueOf(dynamoDB.toString()));

        //Log.d(TAG, String.valueOf(auth.getCredentials()));  //No credentials

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
        progressBar = findViewById(R.id.grid_progressbar);


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
                    progressBar.setVisibility(View.VISIBLE);
                }
                else {
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
                    if (progressBar.getVisibility()==View.VISIBLE) {
                        if (button.performClick()) {
                            Log.d(TAG, "button clicked.");
                        }
                    }
                    mBluetoothAdapter.disable();
                    button.setVisibility(View.INVISIBLE);
                    for (int i=0; i<mAdapter.getCount(); i++ ) {
                        mAdapter.setConnection(i, false);
                        mAdapter.setStrikeNull(i);
                        mAdapter.setDoorNull(i);
                    }
                    mAdapter.notifyDataSetChanged();
                }
                else {
                    mBluetoothAdapter.enable();
                    button.setVisibility(View.VISIBLE);
                }
            }
        });
        final Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        /*
        runMutation();
        runQuery();
        subscribe();
        */
        //getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));

    }

    private class getIdentity extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "getIdentity:" + credentialsProvider.getIdentityId());

            /*
            String tableName = "Movies";

            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(tableName)
                    .withKeySchema(Arrays.asList(
                            new KeySchemaElement()
                                    .withAttributeName("year")
                                    .withKeyType(KeyType.HASH),
                            new KeySchemaElement()
                                    .withAttributeName("title")
                                    .withKeyType(KeyType.RANGE)
                    ))
                    .withAttributeDefinitions(Arrays.asList(
                            new AttributeDefinition()
                                    .withAttributeName("year")
                                    .withAttributeType(ScalarAttributeType.N),
                            new AttributeDefinition()
                                    .withAttributeName("title")
                                    .withAttributeType(ScalarAttributeType.S)
                    ));
            request.setProvisionedThroughput(new ProvisionedThroughput()
                    .withReadCapacityUnits(10L)
                    .withWriteCapacityUnits(10L)
            );

            final TableDescription tableDescription = dynamoDB
                    .createTable(request)
                    .getTableDescription();

            Tables.waitForTableToBecomeActive(dynamoDB, tableName);
            */

            Log.d(TAG, String.valueOf(dynamoDB.listTables()));
            Log.d(TAG, String.valueOf(dynamoDB.describeTable("maglock-door1")));
            Table table = Table.loadTable(dynamoDB, "maglock-door1");
            Log.d(TAG, "Hashkeys:" + String.valueOf(table.getHashKeys()));
            Log.d(TAG, "Rangekeys:" + String.valueOf(table.getRangeKeys()));
            List<String> keys = table.getHashKeys();
            Search document = table.query(new Primitive("date"));
            Log.d(TAG, "Document:" + document.getAllResults());
            ScanRequest scanRequest = new ScanRequest()
                    .withProjectionExpression("date");
            ScanOperationConfig scan = new ScanOperationConfig()
                    .withAttributesToGet(Collections.singletonList("date"));
            Expression keyExp = new Expression();

            QueryOperationConfig queryOperationConfig = new QueryOperationConfig();
            queryOperationConfig.withLimit(1);
            queryOperationConfig.withAttributesToGet(Collections.singletonList("date"));
            queryOperationConfig.withBackwardSearch(true);
                //QueryFilter filter = new QueryFilter("date", Filter.FilterCondition (ComparisonOperator.NOT_NULL));
            Filter filter1;
            QueryRequest queryRequest = new QueryRequest()
                    .withTableName("maglock-door1");
            //queryRequest.setFilterExpression("attribute_exists(date)");
            //queryRequest.addKeyConditionsEntry("date", new Condition().withComparisonOperator(ComparisonOperator.NOT_NULL));
            queryRequest.withScanIndexForward(false);
            queryRequest.withAttributesToGet("date", "event", "priority");
            queryRequest.withLimit(1);

                //queryOperationConfig.withFilter(filter);
            queryOperationConfig.withConsistentRead(true);
            GetItemOperationConfig config = new GetItemOperationConfig();
////////////////////////////////////////////////////////////////////////////// NOT YET WORKING
            //Search search = table.scan(scan);
            //Log.d(TAG, "Search" + search.getAllResults());
            //Search search1 = table.query(queryOperationConfig);
            //Log.d(TAG, "Search1" + search1.getCount());
            //////////////////////// Failed as well
            //QueryResult result = dynamoDB.query(queryRequest);
            //Log.d(TAG, "result1:" + result.toString());
            return null;
        }
    }
    /*public File addTextToFile(String text) {

        File myDir = getApplicationContext().getFilesDir();
        Log.d(TAG, myDir.getPath());
        // Documents Path
        String documents = "documents/data";
        File documentsFolder = new File(myDir, documents);
        Log.d(TAG, documentsFolder.getPath());
        boolean created = documentsFolder.mkdirs();
        Log.d(TAG, "folder create? " + created);


        File logFile = new File(documentsFolder.getPath() + "/testfile.txt");
        Log.d(TAG, logFile.getPath());
        if (!logFile.exists()) {
            try {
                //new File(logFile.getParent()).mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logFile;
    }

    private void uploadWithTransferUtility() {

        File file = addTextToFile(String.valueOf(new Date()));
        String path = file.getPath();

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance()))
                        .build();

        TransferObserver uploadObserver =
                transferUtility.upload(
                        "nepu",
                        "public/testfile.txt",
                        new File(path));

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                    Log.d(TAG, "Upload complete");
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
                Log.d(TAG, "Error:" + ex);
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
            Log.d(TAG, "Upload complete, no listener");

        }

        Log.d("YourActivity", "Bytes Transferred: " + uploadObserver.getBytesTransferred());
        Log.d("YourActivity", "Bytes Total: " + uploadObserver.getBytesTotal());
    }*/

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
                else {
                    mAdapter.add(result);
                }
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
