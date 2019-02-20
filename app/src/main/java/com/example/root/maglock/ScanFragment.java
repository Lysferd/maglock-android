package com.example.root.maglock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ScanFragment extends ListFragment {
    /*
    // TODO: Customize parameter argument names
    /private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    */

    private static final String TAG = ScanFragment.class.getSimpleName();


    private static final long SCAN_PERIOD = 5000;

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeScanner mBluetoothLeScanner;

    private ScanCallback mScanCallback;

    private ScanResultAdapter mAdapter;

    private Handler mHandler;

    private static boolean scanning = false;


    public void setBluetoothAdapter(BluetoothAdapter btAdapter) {
        this.mBluetoothAdapter = btAdapter;
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        // Use getActivity().getApplicationContext() instead of just getActivity() because this
        // object lives in a fragment and needs to be kept separate from the Activity lifecycle.
        //
        // We could get a LayoutInflater from the ApplicationContext but it messes with the
        // default theme, so generate it from getActivity() and pass it in separately
        mAdapter = new ScanResultAdapter(getContext(),
                LayoutInflater.from(getActivity()));
        mHandler = new Handler();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                getActivity().invalidateOptionsMenu();
                Log.d(TAG, "updating view");
                mHandler.postDelayed(this, 5000);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        setListAdapter(mAdapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setDivider(null);
        getListView().setDividerHeight(0);

        setEmptyText(getString(R.string.empty_list));

        startScanning();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.scanner_menu, menu);
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
    }

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
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if(scanning) {
            stopScanning();
        }
        Log.d(TAG, "Position: " + position + " | id: " + id);
        final ScanResult result = (ScanResult) mAdapter.getItem(position);
        final BluetoothDevice device = result.getDevice();
        if (device == null) {
            Log.d(TAG, "No device.");
            return;
        }
        Log.d(TAG, "Device Name: " + device.getName());

        /* Creating the Intent to call the activity ScannedDeviceActivity and send the address with
            it so that the activity may connect and acquire the data by itself.
            I expected this to be where I would send the device with it, but only the address is
            actually needed since that's what it uses to actually connect. This is probably because
            initial detection without connection is not that reliable and information may be lost.

         */
        /*
        final Intent intent = new Intent(getActivity(), ScannedDeviceActivity.class);
        String extraAddress = device.getAddress();
        intent.putExtra(ScannedDeviceActivity.address, extraAddress);
        getActivity().startActivity(intent);
        */
    }


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
            Toast.makeText(getActivity(), R.string.already_scanning, Toast.LENGTH_LONG);
        }
        getActivity().invalidateOptionsMenu();
    }

    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        scanning = false;
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

        // Even if no new results, update 'last seen' times.
        mAdapter.notifyDataSetChanged();
        getActivity().invalidateOptionsMenu();
    }

    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        //builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
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
            Toast.makeText(getActivity(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG)
                    .show();
        }
    }
}
