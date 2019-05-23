package com.cy.helmet.bluetooth.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;

public interface OnReceiverCallback {
    void onCharacteristicWriteRequest(BluetoothGattCharacteristic characteristicRead,
                                      BluetoothGattServer bluetoothGattServer,
                                      BluetoothDevice device,
                                      byte[] value);
    void onConnectionStateChange(int newState);
}
