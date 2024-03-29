package com.example.root.maglock;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ScanResultAdapter extends BaseAdapter {

    private static final String TAG = ScanFragment.class.getSimpleName();

    private ArrayList<ScanResult> mArrayList;
    private ArrayList<String> mNameList;


    private Context mContext;

    private LayoutInflater mInflater;

    ScanResultAdapter(Context context, LayoutInflater inflater) {
        super();
        mContext = context;
        mInflater = inflater;
        mArrayList = new ArrayList<>();
        mNameList = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return mArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return mArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mArrayList.get(position).getDevice().getAddress().hashCode();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        // Reuse an old view if we can, otherwise create a new one.
        if (view == null) {
            view = mInflater.inflate(R.layout.listitem_scanresult, null);
        }

        TextView deviceNameView = (TextView) view.findViewById(R.id.device_name);
        TextView deviceAddressView = (TextView) view.findViewById(R.id.device_address);
        //TextView lastSeenView = (TextView) view.findViewById(R.id.last_seen);

        ScanResult scanResult = mArrayList.get(position);


        String named = (String) deviceNameView.getText();

        if (scanResult == null) {
            deviceNameView.setText("Dummy");
            deviceAddressView.setText("D-U-M-M-Y");
            return view;
        }
        String tempname = scanResult.getDevice().getName();
        Log.d(TAG, "tempname on view: " + tempname);

        String name = scanResult.getDevice().getName();
        if (name == null) {
            name = mContext.getResources().getString(R.string.no_name);
        }
        deviceNameView.setText(name);

        deviceAddressView.setText(scanResult.getDevice().getAddress());
        //lastSeenView.setText(getTimeSinceString(mContext, scanResult.getTimestampNanos()));
        return view;
    }

    /**
     * Search the adapter for an existing device address and return it, otherwise return -1.
     */
    public int getPosition(String address) {
        int position = -1;
        for (int i = 0; i < mArrayList.size(); i++) {
            if (mArrayList.get(i).getDevice().getAddress().equals(address)) {
                position = i;
                break;
            }
        }
        return position;
    }

    /**
     * Add a ScanResult item to the adapter if a result from that device isn't already present.
     * Otherwise updates the existing position with the new ScanResult.
     */
    public boolean add(ScanResult scanResult) {

        if (scanResult == null) {
            mArrayList.add(scanResult);
            Log.d(TAG, "Dummy result added");
            return true;
        }
        else {
            int existingPosition = getPosition(scanResult.getDevice().getAddress());
            String tempName = scanResult.getDevice().getName();
            Log.d(TAG, "TempName:" + tempName);

            if (existingPosition >= 0) {
                // Device is already in list, update its record.
                mArrayList.set(existingPosition, scanResult);
                return false;
            } else {
                // Add new Device's ScanResult to list.
                mArrayList.add(scanResult);
                return true;
            }
        }
    }
    public void add(String string) {
        mNameList.add(string);
    }

    /**
     * Clear out the adapter.
     */
    public void clear() {
        mArrayList.clear();
    }

    /**
     * Takes in a number of nanoseconds and returns a human-readable string giving a vague
     * description of how long ago that was.
     */

    public static String getTimeSinceString(Context context, long timeNanoseconds) {
        String lastSeenText = context.getResources().getString(R.string.last_seen) + " ";

        long timeSince = SystemClock.elapsedRealtimeNanos() - timeNanoseconds;
        long secondsSince = TimeUnit.SECONDS.convert(timeSince, TimeUnit.NANOSECONDS);

        if (secondsSince < 5) {
            lastSeenText += context.getResources().getString(R.string.just_now);
        } else if (secondsSince < 60) {
            lastSeenText += secondsSince + " " + context.getResources()
                    .getString(R.string.seconds_ago);
        } else {
            long minutesSince = TimeUnit.MINUTES.convert(secondsSince, TimeUnit.SECONDS);
            if (minutesSince < 60) {
                if (minutesSince == 1) {
                    lastSeenText += minutesSince + " " + context.getResources()
                            .getString(R.string.minute_ago);
                } else {
                    lastSeenText += minutesSince + " " + context.getResources()
                            .getString(R.string.minutes_ago);
                }
            } else {
                long hoursSince = TimeUnit.HOURS.convert(minutesSince, TimeUnit.MINUTES);
                if (hoursSince == 1) {
                    lastSeenText += hoursSince + " " + context.getResources()
                            .getString(R.string.hour_ago);
                } else {
                    lastSeenText += hoursSince + " " + context.getResources()
                            .getString(R.string.hours_ago);
                }
            }
        }

        return lastSeenText;
    }

}
