package com.example.zxa01.peripheral;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import butterknife.BindView;

@SuppressLint("NewApi")
public class Peripheral {
    private static final String TAG = MainActivity.class.getCanonicalName();
    private final static int REQUEST_ENABLE_BT = 1;

    private final AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }
    };

    private PeripheralService peripheralService = new PeripheralService();
    private BluetoothGattService mBluetoothGattService;
    private HashSet<BluetoothDevice> mBluetoothDevices;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private AdvertiseData mAdvData;
    private AdvertiseData mAdvScanResponse;
    private AdvertiseSettings mAdvSettings;
    private BluetoothLeAdvertiser mAdvertiser;
    private MainActivity mMainActivity;
    private TextView mMAC;
    private TextView mConnectDevice;
    private TextView mCharacteristicValue;
    //GATT
    private BluetoothGattServer mGattServer;
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice d, final int status, int newStatus) {
            super.onConnectionStateChange(d, status, newStatus);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newStatus == BluetoothGatt.STATE_CONNECTED) {
                    d.createBond();
                    mBluetoothDevices.add(d);
                    List<String> devices = mBluetoothDevices.stream().map((e)->e.getAddress()).collect(Collectors.toList());
                    mConnectDevice.setText(devices.toString());
                    Log.v(TAG, "已配對。device:" + d.getAddress());
                } else if (newStatus == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices.remove(d);
                    Log.v(TAG, "無法連線至設備。");
                }
            } else {
                mBluetoothDevices.remove(d);
                Log.e(TAG, "連線錯誤：" + status);
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice d, int status) {
            super.onNotificationSent(d, status);
            Log.v(TAG, "通知已送出。");
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice d, int reqId, int offset, BluetoothGattCharacteristic c) {
            super.onCharacteristicReadRequest(d, reqId, offset, c);
            Log.d(TAG, "裝置讀取 Characteristic ( UUID = " + c.getUuid() + " )");
            Log.d(TAG, "Characteristic Value：" + Arrays.toString(c.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(d, reqId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null); //回傳
                return;
            }
            mGattServer.sendResponse(d, reqId, BluetoothGatt.GATT_SUCCESS, offset, c.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice d, int reqId, BluetoothGattCharacteristic c, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] v) {
            super.onCharacteristicWriteRequest(d, reqId, c, preparedWrite, responseNeeded, offset, v);
            Log.v(TAG, "裝置寫入 Characteristic Write Request : " + Arrays.toString(v));
            if (responseNeeded) {
                mGattServer.sendResponse(d, reqId, 0, 0, v);
                mBluetoothGattService.getCharacteristic(peripheralService.getObjectUuid()).setValue(v);
                sendNotificationToDevices(mBluetoothGattService.getCharacteristic(peripheralService.getObjectUuid()));
                mCharacteristicValue.setText(new String(v));
            }
        }
    };

    public Peripheral(MainActivity mainActivity) {

        // UI
        mMainActivity = mainActivity;
        mMAC = mMainActivity.findViewById(R.id.textMAC);
        mConnectDevice = mMainActivity.findViewById(R.id.textDevice);
        mCharacteristicValue = mMainActivity.findViewById(R.id.textCharacteristic);

        // 要求開啟藍芽
        mBluetoothManager = (BluetoothManager) mMainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mMainActivity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // build ble
        mBluetoothGattService = peripheralService.getBluetoothGattService();
        mBluetoothDevices = new HashSet<>();
        mBluetoothAdapter.setName("HANA醬廣播電台");
        mAdvSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build();
        mAdvData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(peripheralService.getServiceUUID())
                .addManufacturerData(0x6666, new byte[]{(byte) 0x45, (byte) 0x43, (byte) 0x48, (byte) 0x4F})
                .build();
        mAdvScanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();
        startPeripheral(mMainActivity);
    }

    protected void startPeripheral(MainActivity mainActivity) {
        mGattServer = mBluetoothManager.openGattServer(mainActivity, mGattServerCallback);
        mGattServer.addService(mBluetoothGattService);
        mMAC.setText(mBluetoothGattService.getUuid().toString());
        mCharacteristicValue.setText("尚未設定");
        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback); //開啟藍芽服務
        }
    }

    protected void stopPeripheral() {
        if (mGattServer != null) {
            mGattServer.close();
        }
        if (mBluetoothAdapter.isEnabled() && mAdvertiser != null) {
            mAdvertiser.stopAdvertising(mAdvCallback);
        }
        mMAC.setText("廣播已關閉");
        mConnectDevice.setText("無裝置連線");
        mCharacteristicValue.setText("尚未廣播，無數值");
    }

    private void sendNotificationToDevices(BluetoothGattCharacteristic c) {
        boolean indicate = (c.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_INDICATE)
                == BluetoothGattCharacteristic.PROPERTY_INDICATE;
        for (BluetoothDevice device : mBluetoothDevices) {
            mGattServer.notifyCharacteristicChanged(device, c, indicate);
        }
    }

}
