package com.cy.helmet.bluetooth.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import android.text.TextUtils;
import android.util.Log;

import com.cy.helmet.HelmetApplication;

import java.util.UUID;

import static com.cy.helmet.HelmetApplication.mAppContext;

public class HelmetBluetoothManager {
    public static final String TAG = "helmet_BluetoothManager";
    private static HelmetBluetoothManager mInstance;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdertiseCallback;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGattCharacteristic mCharacteristicRead;
    private BluetoothManager mBluetoothManager;
    private OnReceiverCallback mOnReceiverCallback;

    private static UUID UUID_SERVER = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARREAD = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARWRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int MOBILE_ONLY = 1;
    public static final int WIFI_ONLY = 2;
    public static final int MOBILE_WIFI = 3;
    public static boolean isBlueToothLastState = false;
    public static boolean isSetByService = false;
    private String mBlueToothName = null;

    public static final int CONNECT_SUCCESSED = 2;
    public static final int CONNECT_DISCONNECT = 0;

    public synchronized static HelmetBluetoothManager getInstance() {
        if (mInstance == null)
            mInstance = new HelmetBluetoothManager();
        return mInstance;
    }

    public void init() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mAppContext.getSystemService(Context.BLUETOOTH_SERVICE);
        }
    }

    public void startAdvertising() {
        if (mBluetoothLeAdvertiser == null
                && mBluetoothManager != null) {
            BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
            Log.e(TAG, "isMultipleAdvertisementSupported = " + bluetoothAdapter.isMultipleAdvertisementSupported());
            if (bluetoothAdapter != null)
                mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        }
        Log.e(TAG, "[startAdvertising] mAdertiseCallback = " + (mAdertiseCallback == null));
        if (mAdertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            mAdertiseCallback = new MyAdvertiseCallback();
            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdertiseCallback);
            }
        }
    }

    public void stopAdvertising() {
        Log.e(TAG, "[stopAdvertising]");
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdertiseCallback);
            mAdertiseCallback = null;
        }
    }

    public void setOnReceiverCallback(OnReceiverCallback onReceiverCallback) {
        mOnReceiverCallback = onReceiverCallback;
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    private AdvertiseData buildAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true);
        return dataBuilder.build();
    }

    private void initServices(Context context) {
        mBluetoothGattServer = mBluetoothManager.openGattServer(context, mBluetoothGattServerCallback);
        BluetoothGattService service = new BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mCharacteristicRead = new BluetoothGattCharacteristic(UUID_CHARREAD,
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE);
        mCharacteristicRead.addDescriptor(descriptor);
        service.addCharacteristic(mCharacteristicRead);

        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHARWRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristicWrite);

        mBluetoothGattServer.addService(service);
        Log.d(TAG, "[initServices] success");
    }

    private class MyAdvertiseCallback extends AdvertiseCallback {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.e(TAG, "Advertising start success");
            initServices(mAppContext);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "Advertising start failure");
        }
    }

    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.e(TAG, "[onConnectionStateChange] status = " + status + " newState = " + newState);
            if (mOnReceiverCallback != null) {
                mOnReceiverCallback.onConnectionStateChange(newState);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.e(TAG, "[onServiceAdded] status = " + status);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.e(TAG, "[onCharacteristicReadRequest]");
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.e(TAG, "[onCharacteristicWriteRequest]");
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            if (mOnReceiverCallback != null) {
                mOnReceiverCallback.onCharacteristicWriteRequest(mCharacteristicRead, mBluetoothGattServer, device, value);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.e(TAG, "[onDescriptorReadRequest]");
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            Log.e(TAG, "[onDescriptorWriteRequest]");
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.e(TAG, "[onNotificationSent] status = " + status);
        }
    };


    public void openBluetooth(boolean value) {
        if (mBluetoothManager == null)
            return;
        if (value && !mBluetoothManager.getAdapter().isEnabled()) {
            mBluetoothManager.getAdapter().enable();
        } else {
            mBluetoothManager.getAdapter().disable();
        }
    }

    public void setHelmetName(String name) {
        //TODO set bluetooth name here
        if (name != null && !TextUtils.isEmpty(name.trim())) {
            bluetoothRenameByService(name);
        }
    }

    public boolean isBluetoothEnable() {
        if (mBluetoothManager != null)
            return mBluetoothManager.getAdapter().isEnabled();
        return false;
    }

    public void bluetoothRenameByService(String name) {
        Log.e(TAG, "bluetoothRenameByService: " + name);
        mBlueToothName = name;
        isSetByService = true;
        isBlueToothLastState = isBluetoothEnable();
        bluetoothRename();
    }

    public void bluetoothRename() {
        BluetoothAdapter bluetoothAdapter = null;
        if (mBluetoothManager != null) {
            bluetoothAdapter = mBluetoothManager.getAdapter();
        }
        if (bluetoothAdapter != null && mBlueToothName != null) {
            String curretName = bluetoothAdapter.getName();
            if (!TextUtils.isEmpty(mBlueToothName)) {
                if (!TextUtils.isEmpty(curretName)) {
                    if (!curretName.equals(mBlueToothName)) {
                        if (bluetoothAdapter.isEnabled()) {
                            bluetoothAdapter.setName(mBlueToothName);
                            if (!isBlueToothLastState)
                                openBluetooth(false);
                            else
                                isSetByService = false;
                        } else {
                            openBluetooth(true);
                        }
                    } else {
                        if (!isBlueToothLastState)
                            openBluetooth(false);
                        else
                            isSetByService = false;
                    }
                } else {
                    openBluetooth(true);
                }
            }
        }
    }
}
