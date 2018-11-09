package com.example.zxa01.peripheral;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.textCharacteristic)
    TextView mTextCharacteristic;
    private Peripheral peripheral;
    private boolean isStart = false; //啟用廣播

    @OnClick(R.id.broadcast)
    public void submit(View view) {
        isStart = true;
        onStart();
    }

    @OnClick(R.id.stop)
    public void stop(View view) {
        if (peripheral != null) {
            peripheral.stopPeripheral();
            isStart = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setupPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isStart) {
            if (peripheral != null) {
                peripheral.stopPeripheral();
            }
            peripheral = new Peripheral(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (peripheral != null) {
            peripheral.stopPeripheral();
            isStart = false;
        }
    }

    //權限
    private void setupPermissions() {
        boolean isGranted = true;
        String[] needPermission = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        for (String p : needPermission) {
            isGranted &= ActivityCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
        }
        if (!isGranted) {
            requestPermissions(needPermission, 100);
        }
    }
};