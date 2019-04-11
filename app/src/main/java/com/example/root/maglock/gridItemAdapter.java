package com.example.root.maglock;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;


public class gridItemAdapter extends BaseAdapter {

    public static final int CONTACT = 1;
    public static final int  STRIKE = 2;
    public static final int     REQ = 3;

    private static final String TAG = ScanFragment.class.getSimpleName();

    private ArrayList<ScanResult> mArrayList;
    private ArrayList<Boolean> mConnectedList, mDoorList, mStrikeList, mTableList, mTransition;
    private ArrayList<BluetoothGattCharacteristic> mDoorContactList, mDoorStrikeList, mDoorReqList;
    private LayoutInflater mInflater;
    private Context mContext;

    gridItemAdapter(Context context, LayoutInflater inflater) {
        super();
        mContext = context;
        mInflater = inflater;
        mArrayList = new ArrayList<>();
        mConnectedList = new ArrayList<>();
        mDoorList = new ArrayList<>();
        mDoorContactList = new ArrayList<>();
        mDoorStrikeList = new ArrayList<>();
        mDoorReqList = new ArrayList<>();
        mStrikeList = new ArrayList<>();
        mTableList = new ArrayList<>();
        mTransition = new ArrayList<>();
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
    public long getItemId(int position) { return mArrayList.get(position).getDevice().getAddress().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Reuse an old view if we can, otherwise create a new one.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.griditem, null);
        }
        /* Check if there is something in the field for name, if there is and it
         * is View.GONE, make it View.VISIBLE. Otherwise, don't bother.
         */
        TextView textView = (TextView) convertView.findViewById(R.id.grid_name);

        convertView.setBackgroundColor(0);
        ScanResult scanResult = mArrayList.get(position);
        Boolean connected = mConnectedList.get(position);
        Boolean door = mDoorList.get(position);
        Boolean strike = mStrikeList.get(position);
        Boolean table = mTableList.get(position);
        Boolean transition = mTransition.get(position);

        ImageView imageView = (ImageView) convertView.findViewById(R.id.grid_lock);
        //ImageView imageButton = (ImageView) convertView.findViewById(R.id.grid_door);
        //ImageView tableButton = convertView.findViewById(R.id.grid_table_door);

        //imageView.setColorFilter(Color.parseColor("#3DACF7"));
        ////imageView.setBackgroundResource(R.drawable.round_button);
        //imageView.setImageResource(R.drawable.lock);

        TransitionDrawable transitionDrawable = (TransitionDrawable) imageView.getBackground();

        if (connected) {
            //imageView.setColorFilter(Color.parseColor("#77C344"));
            //imageView.setBackgroundResource(R.drawable.round_button2);
            //imageView.setBackgroundResource(R.drawable.transition_roundbutton);
            //if (transition) {
                transitionDrawable.startTransition(500);
            //    mTransition.set(position, false);
            //}
        }
        else {
            //if (transition) {
                transitionDrawable.resetTransition();
                //mTransition.set(position, false);
            //}
            //imageView.setColorFilter(Color.parseColor("#3DACF7"));
            //imageView.setBackgroundResource(R.drawable.round_button);
        }
        //imageView.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.colorGreen, null));
        /*
        if (!connected) {
            imageView.setColorFilter(Color.LTGRAY);
            imageButton.setColorFilter(Color.LTGRAY);
            tableButton.setColorFilter(Color.LTGRAY);
            imageButton.setImageResource(R.drawable.door);
            imageView.setImageResource(R.drawable.lock);
            tableButton.setImageResource(R.drawable.door);
        }
        else*//* {
            imageView.setColorFilter(Color.BLACK);
            if (door == null) {
                imageButton.setColorFilter(Color.LTGRAY);
                imageButton.setImageResource(R.drawable.door);
            }
            else if (!door) {
                imageButton.setColorFilter(ResourcesCompat.getColor(parent.getResources(), R.color.colorRed, null));
                imageButton.setImageResource(R.drawable.door_open);
            }
            else {
                imageButton.setColorFilter(ResourcesCompat.getColor(parent.getResources(), R.color.colorGreen, null));
                imageButton.setImageResource(R.drawable.door_closed);
            }
            if (strike == null) {
                //imageView.setColorFilter(Color.LTGRAY);
            }
            else if (!strike) {
                imageView.setImageResource(R.drawable.lock_open_outline);
            }
            else {
                imageView.setImageResource(R.drawable.lock);
            }
        }*/
        /*if (table == null) {
            tableButton.setColorFilter(Color.LTGRAY);
            tableButton.setImageResource(R.drawable.door);
            tableButton.setVisibility(View.VISIBLE);
        }
        else if (!table) {
            tableButton.setVisibility(View.VISIBLE);
            tableButton.setImageResource(R.drawable.door_open);
            tableButton.setColorFilter(ResourcesCompat.getColor(parent.getResources(), R.color.colorRed, null));
        }
        else {
            tableButton.setVisibility(View.VISIBLE);
            tableButton.setImageResource(R.drawable.door_closed);
            tableButton.setColorFilter(ResourcesCompat.getColor(parent.getResources(), R.color.colorGreen, null));
        }
        */
        if (!(Objects.requireNonNull(scanResult.getScanRecord()).getDeviceName() == null)) {
            textView.setText(scanResult.getScanRecord().getDeviceName());
        }
        else {
            textView.setText(R.string.no_name);
        }
        if (textView!=null && textView.getVisibility()==View.INVISIBLE) {
            textView.setVisibility(View.VISIBLE);
        }
        return convertView;
    }

    /**
     * Search the adapter for an existing device address and return it, otherwise return -1.
     */
    public int getPosition(String address) {
        int position = -1;
        for (int i = 0; i < mArrayList.size(); i++) {
            String mAddress = mArrayList.get(i).getDevice().getAddress();
            if (mAddress.equals(address)) {
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
            String address = scanResult.getDevice().getAddress();
            int existingPosition = getPosition(address);
            String tempName = scanResult.getDevice().getName();
            Log.d(TAG, "TempName:" + tempName + " - " + address);

            if (existingPosition >= 0) {
                // Device is already in list, update its record.
                mArrayList.set(existingPosition, scanResult);

                return false;
            } else {
                // Add new Device's ScanResult to list.
                mArrayList.add(scanResult);
                mConnectedList.add(false);
                mDoorList.add(null);
                mStrikeList.add(null);
                mDoorContactList.add(null);
                mDoorStrikeList.add(null);
                mDoorReqList.add(null);
                mTableList.add(null);
                mTransition.add(false);
                return true;
            }
        }
    }

    public void setConnection(int position, boolean state) {
        mConnectedList.set(position, state);
        mTransition.set(position, true);
    }
    public void setDoor(int position, boolean state) {
        mDoorList.set(position, state);
    }
    public void setStrike(int position, boolean state) {
        mStrikeList.set(position, state);
    }
    public void setStrikeNull(int position) {
        mStrikeList.set(position, null);
    }
    public void setTableDoor(int position, boolean state) {
        mTableList.set(position, state);
    }
    public void addCharacteristics(int position, BluetoothGattCharacteristic characteristic, int type) {
        switch (type) {
            case CONTACT:
                mDoorContactList.set(position, characteristic);
                break;
            case STRIKE:
                mDoorStrikeList.set(position, characteristic);
                break;
            case REQ:
                mDoorReqList.set(position, characteristic);
                break;
        }
    }

    public BluetoothGattCharacteristic getItem(int position, int type) {
        BluetoothGattCharacteristic characteristic;
        switch (type) {
            case CONTACT:
                characteristic = mDoorContactList.get(position);
                if (characteristic!=null) {
                    return mDoorContactList.get(position);
                }
                break;
            case STRIKE:
                characteristic = mDoorStrikeList.get(position);
                if (characteristic != null) {
                    return mDoorStrikeList.get(position);
                }
                break;
            case REQ:
                characteristic = mDoorReqList.get(position);
                if (characteristic != null) {
                    return mDoorReqList.get(position);
                }
                break;
        }
        return null;
    }

    public boolean getConnection(int position) {
        if (mConnectedList.get(position) == null) {
            return false;
        }
        return mConnectedList.get(position);
    }

    public void setDoorNull(int position) {
        mDoorList.set(position, null);
    }

    public void clear() {
        mArrayList.clear();
        mConnectedList.clear();
        mDoorList.clear();
        mStrikeList.clear();
        mTableList.clear();
        mDoorContactList.clear();
        mDoorStrikeList.clear();
        mDoorReqList.clear();
    }
}