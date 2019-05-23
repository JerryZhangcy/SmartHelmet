package com.cy.helmet.bluetooth.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.storage.HelmetStorage;
import com.cy.helmet.storage.StorageBean;
import com.cy.helmet.util.AppUtil;
import com.cy.helmet.util.DeviceStatusUtil;

import static com.cy.helmet.HelmetApplication.mAppContext;

import org.json.JSONObject;

public class HelmetBluetoothDataProcess {
    private static final String TAG = "HelmetBluetoothData";
    private static final int UNKNOWN_COMMAND = -1;
    private static final int WIFI_SETTING = 1;
    private static final int SERVER_SETTING = 2;
    private static final int STORAGE_SETTING = 3;
    private static final int VOLUME_SETTING = 4;
    private static final int QUERY_SETTING = 5;
    private static final int NETWORK_SETTING = 6;
    private static BluetoothGattCharacteristic mCharacteristicRead;
    private static BluetoothGattServer mBluetoothGattServer;
    private static BluetoothDevice mBluetoothDevice;

    private static final int BLE_BYTE_LENGTH = 18;

    public static void setBlueGatt(BluetoothGattCharacteristic characteristicRead,
                                   BluetoothGattServer bluetoothGattServer,
                                   BluetoothDevice device) {
        mCharacteristicRead = characteristicRead;
        mBluetoothGattServer = bluetoothGattServer;
        mBluetoothDevice = device;
    }


    public static void sendMsgToRemote(String msg) {
        if (mCharacteristicRead != null
                && mBluetoothGattServer != null
                && mBluetoothDevice != null) {
            byte[] send = msg.getBytes();
            int count = send.length / BLE_BYTE_LENGTH;
            int last_byte = send.length % BLE_BYTE_LENGTH;
            Log.e(TAG, "sendMsgToRemote  leng = " + send.length + " count = " + count
                                       + " last_byte = " + last_byte);
            for (int i = 0; i < count; i++) {
                byte[] rel = new byte[BLE_BYTE_LENGTH];
                System.arraycopy(send, i * BLE_BYTE_LENGTH, rel, 0, BLE_BYTE_LENGTH);
                mCharacteristicRead.setValue(rel);
                mBluetoothGattServer.notifyCharacteristicChanged(mBluetoothDevice, mCharacteristicRead, false);
            }

            if (last_byte > 0) {
                byte[] last = new byte[last_byte];
                System.arraycopy(send, send.length - last_byte, last, 0, last_byte);
                mCharacteristicRead.setValue(last);
                mBluetoothGattServer.notifyCharacteristicChanged(mBluetoothDevice, mCharacteristicRead, false);
            }

        }
    }


    public static void onReceiveMessage(String message) {
        if (message != null && !TextUtils.isEmpty(message.trim())) {
            try {
                JSONObject json = new JSONObject(message);
                int msgId = json.optInt("msg_id", UNKNOWN_COMMAND);
                switch (msgId) {
                    case WIFI_SETTING:
                        setWifi(json);
                        break;
                    case SERVER_SETTING:
                        setServer(json);
                        break;
                    case STORAGE_SETTING:
                        setStorage(json);
                        break;
                    case VOLUME_SETTING:
                        setVolume(json);
                        break;
                    case QUERY_SETTING:
                        querySetting();
                        break;
                    case NETWORK_SETTING:
                        setNetwork(json);
                        break;
                    default:
                        Log.e(TAG, "unrecognized command: " + message);
                }
            } catch (Exception var4) {
            }
        } else {
            Log.e(TAG, "receive empty message from bluetooth...");
        }
    }


    public static void setWifi(JSONObject json) {
        if (json == null) {
            String msg = "{\"msg_id\":11,\"code\":-1}";
            sendMsgToRemote(msg);
        } else {
            String ssid_2_4g = "";
            String passwd_2_4g = "";
            String ssid_5g = "";
            String passwd_5g = "";
            ssid_2_4g = json.optString("wifi_2_4g_ssid");
            passwd_2_4g = json.optString("wifi_2_4g_pass");
            ssid_5g = json.optString("wifi_5g_ssid");
            passwd_5g = json.optString("wifi_5g_pass");

            HelmetConfig.get().updateWifiConfig(
                    ssid_2_4g, passwd_2_4g, ssid_5g, passwd_5g);
        }
    }

    public static void setServer(JSONObject json) {
        if (json == null) {
            String msg = "{\"msg_id\":21,\"code\":-1}";
            sendMsgToRemote(msg);
        } else {
            String serverHost = json.optString("web_server_host", "");
            int serverPort = json.optInt("web_server_port", -1);
            String gpsServerHost = json.optString("gps_server_host", "");
            int gpsServerPort = json.optInt("gps_server_port", -1);
            HelmetConfig.get().updateServerConfig(
                    serverHost, serverPort, gpsServerHost, gpsServerPort);
        }
    }

    public static void setStorage(JSONObject json) {
        if (json == null) {
            String msg = "{\"msg_id\":31,\"code\":-1}";
            sendMsgToRemote(msg);
        } else {
            boolean isTfFirst = true;
            String storage = json.optString("mem_use_first", "");
            if ("nand".equals(storage)) {
                isTfFirst = false;
            } else {
                if (!"tf".equals(storage)) {
                    String msg = "{\"msg_id\":31,\"code\":-1}";
                    sendMsgToRemote(msg);
                    return;
                }

                isTfFirst = true;
            }

            HelmetConfig.get().updateStorageConfig(isTfFirst);
        }
    }

    public static void setVolume(JSONObject json) {
        if (json == null) {
            String msg = "{\"msg_id\":41,\"code\":-1}";
            sendMsgToRemote(msg);
        } else {
            int volume = json.optInt("voice", -1);
            if (volume >= 0 && volume <= 100) {
                AudioManager mAudioManager = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
                int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int index = maxVolume * volume / 100;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, AudioManager.FLAG_PLAY_SOUND);
                String msg = "{\"msg_id\":41,\"code\":0}";
                sendMsgToRemote(msg);
            } else {
                String msg = "{\"msg_id\":41,\"code\":-1}";
                sendMsgToRemote(msg);
            }
        }
    }

    public static void querySetting() {
        JSONObject json = new JSONObject();

        try {
            json.put("msg_id", 51);
            json.put("power", DeviceStatusUtil.getPowerPercent());
            StorageBean[] storageList = HelmetStorage.getInstance().getStorageArray();
            if (storageList[0] != null) {
                json.put("nand_left", storageList[0].getAvailableSizeMB());
            } else {
                json.put("nand_left", 0);
            }

            if (storageList[1] != null) {
                json.put("tf_card_left", storageList[1].getAvailableSizeMB());
            } else {
                json.put("tf_card_left", 0);
            }

            String softVersion = AppUtil.getAppVersionName(HelmetApplication.mAppContext);
            AudioManager mAudioManager = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            json.put("softwear_version", softVersion);
            json.put("net_type", DeviceStatusUtil.getNetType());
            json.put("net_status", DeviceStatusUtil.getNetStatus());
            int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            Log.e(TAG, "volume:maxVolume>>>" + volume + ":" + maxVolume);
            json.put("voice", volume);
        } catch (Exception var6) {
            String msg = "{\"msg_id\":51,\"code\":-1}";
            sendMsgToRemote(msg);
            return;
        }
        String msg = json.toString();
        sendMsgToRemote(msg);
    }

    public static void setNetwork(JSONObject json) {
        if (json != null) {
            int networkType = json.optInt("force_net_type", HelmetBluetoothManager.MOBILE_WIFI);
            HelmetConfig.get().updateNetworkConfig(networkType);
        }
    }
}
