package com.cy.helmet.bluetooth.ble;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cy.helmet.voice.HelmetVoiceManager;

import java.nio.ByteBuffer;

import static com.cy.helmet.bluetooth.ble.HelmetBluetoothManager.CONNECT_SUCCESSED;
import static com.cy.helmet.bluetooth.ble.HelmetBluetoothManager.CONNECT_DISCONNECT;

public class HelmetBluetoothService extends Service implements OnReceiverCallback {
    public static boolean mServiceRunning = false;
    private HelmetBluetoothManager mHelmetBluetoothManager;
    private String mContent = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mServiceRunning = true;
        mHelmetBluetoothManager = HelmetBluetoothManager.getInstance();
        mHelmetBluetoothManager.startAdvertising();
        mHelmetBluetoothManager.setOnReceiverCallback(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mServiceRunning = false;
        if (mHelmetBluetoothManager != null)
            mHelmetBluetoothManager.stopAdvertising();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothGattCharacteristic characteristicRead,
                                             BluetoothGattServer bluetoothGattServer, BluetoothDevice device, byte[] value) {
        HelmetBluetoothDataProcess.setBlueGatt(characteristicRead, bluetoothGattServer, device);
        ByteBuffer byteBuffer = ByteBuffer.wrap(value);
        String context = new String(byteBuffer.array());
        mContent += context;
        Log.e(HelmetBluetoothManager.TAG, "[onCharacteristicWriteRequest] ---HelmetBluetoothService---" +
                mContent);
        if (mContent.contains("}")) {
            HelmetBluetoothDataProcess.onReceiveMessage(mContent);
            mContent = "";
        }
    }

    @Override
    public void onConnectionStateChange(int newState) {
        if (newState == CONNECT_SUCCESSED) {
            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.BLUETOOTH_PAIR_SUCCESS);
        } else if(newState == CONNECT_DISCONNECT) {
            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_BLUETOOTH_DISCONNECT);
        }
    }
}
