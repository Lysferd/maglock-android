package com.example.root.maglock;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;

public class StartActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 400;
    private BluetoothAdapter mBluetoothAdapter;

    private Switch aSwitch;
    private Button button;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        aSwitch = (Switch) findViewById(R.id.bluetoothSwitch);
        button = (Button) findViewById(R.id.SearchActivityButton);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
        }
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

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    mBluetoothAdapter.disable();
                }
                else {
                    mBluetoothAdapter.enable();
                }
            }
        });
        handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!mBluetoothAdapter.isEnabled()) {
                    button.setVisibility(View.GONE);
                }
                else {
                    button.setVisibility(View.VISIBLE);
                }
                handler.postDelayed(this, 500);
            }
        });

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

    public void launchSearchActivity(View view) {

        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);

    }

    public void exitApp(View view) {
        finish();
        System.exit(0);
    }
}
