package com.example.zxa01.peripheral;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

import java.util.UUID;

@SuppressLint("NewApi")
public class PeripheralService {
    private static final UUID SERVICE_UUID = UUID.fromString("11101100-0000-0000-8000-000000000000"); //unknown service
    private static final UUID OBJECT_UUID = UUID.fromString("00002AC3-0000-1000-8000-00805f9b34fb"); //object UUID
    private static final UUID CHANGE_UUID = UUID.fromString("00002AC8-0000-1000-8000-00805f9b34fb"); //change UUID
    private static final UUID DESCRIPTORS_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); //description
    private static final int INITIAL_VALUE = 0; //初始值

    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mCharacteristicObject;
    private BluetoothGattCharacteristic mCharacteristicChange;

    public PeripheralService() {

        //讀取 Characteristic
        mCharacteristicObject = new BluetoothGattCharacteristic(OBJECT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        mCharacteristicObject.addDescriptor(getDescriptor());

        //寫入 Characteristic
        mCharacteristicChange = new BluetoothGattCharacteristic(CHANGE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

        //新增至GATT Service
        mService = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mService.addCharacteristic(mCharacteristicObject);
        mService.addCharacteristic(mCharacteristicChange);
        setValue(INITIAL_VALUE);
    }

    protected BluetoothGattService getBluetoothGattService(){
        return mService;
    }

    public ParcelUuid getServiceUUID() {
        return new ParcelUuid(SERVICE_UUID);
    }

    public UUID getObjectUuid() {
        return OBJECT_UUID;
    }

    public static BluetoothGattDescriptor getDescriptor() {
        return new BluetoothGattDescriptor(DESCRIPTORS_UUID,0);
    }

    protected void setValue(int newValue) {
        mCharacteristicObject.setValue(newValue,BluetoothGattCharacteristic.FORMAT_UINT16,0);
    }

}