package com.example.root.maglock;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private static final int REQUEST_EXIT = 1334;
    private String TAG = "MAGLOCK."+SearchActivity.class.getSimpleName();

    private ScanResultAdapter mAdapter;
    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;

    private ProgressBar progressBar;

    private boolean scanning = false;
    private Handler progressHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);


        GridView gridView = findViewById(R.id.grid);
        progressBar = findViewById(R.id.progress_circular);

        mAdapter = new ScanResultAdapter(getApplicationContext(),
                LayoutInflater.from(this));

        gridView.setAdapter(mAdapter);

        gridView.setClickable(true);

        gridView.setOnItemClickListener(onItemClickListener);

        progressHandler = new Handler();

        progressHandler.post(runnable);

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        startScanning();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EXIT) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanning) {
            stopScanning();
        }
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

    public void returnToStart(View view) {
        finish();
    }

    public void back(View view) {
        finish();
    }

    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);


            List<ParcelUuid> parcelUuids = result.getScanRecord().getServiceUuids();
            if (parcelUuids == null) {
                Log.d(TAG, "No ServiceUUIDs, another failure");
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

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getApplicationContext(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG)
                    .show();
        }
    }

    public static boolean hasMyService( ScanRecord record) {
        final String myServiceID = "3d22744e";
        List<ParcelUuid> uuids = record.getServiceUuids();
        if (uuids != null) {
            for (ParcelUuid uuid : uuids) {
                String strUUID = uuid.toString();
                if (strUUID.contains(myServiceID)) {
                    Log.d("test", "UUID found!!!!");
                    return true;
                }
                else {
                    Log.d("Test", "not found");
                    return false;
                }
            }
        }
        return false;
    }

    public void addNew(View view) {
        Log.d(TAG, "addNew Called");
        if (scanning) {
            stopScanning();
        }

        mAdapter.add((ScanResult) null);
        mAdapter.notifyDataSetChanged();

    }

    @Override
    public void finishFromChild(Activity child) {
        super.finishFromChild(child);
        if (child.getClass().equals(DetailsActivity.class)) {
            finish();
        }
    }

    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Log.d(TAG, "position " + position);

            final ScanResult result = (ScanResult) mAdapter.getItem(position);
            final BluetoothDevice device = result.getDevice();
            if (device == null) {
                Log.d(TAG, "No device.");
                return;
            }

            stopScanning();
            final Intent intent = new Intent(getApplicationContext(), DetailsActivity.class);

            String extraAddress = device.getAddress();
            intent.putExtra(DetailsActivity.address, extraAddress);

            startActivityForResult(intent, REQUEST_EXIT);

        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!scanning) {
                if (progressBar.getVisibility()!=View.GONE) {
                    progressBar.setVisibility(View.GONE);
                }
            }
            else {
                if (progressBar.getVisibility()!=View.VISIBLE) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
            progressHandler.postDelayed(this, 1000);
        }
    };
}
