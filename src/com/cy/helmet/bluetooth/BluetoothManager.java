package com.cy.helmet.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.R;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.location.LocationUtil;
import com.cy.helmet.storage.HelmetStorage;
import com.cy.helmet.storage.StorageBean;
import com.cy.helmet.util.AppUtil;
import com.cy.helmet.util.DeviceStatusUtil;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.voice.HelmetVoiceManager;

import org.json.JSONObject;

/**
 * Created by yaojiaqing on 2018-04-04.
 */

public class BluetoothManager {
    public static final String TAG = "YXL";

    private static final int UNKNOWN_COMMAND = -1;
    private static final int WIFI_SETTING = 1;
    private static final int SERVER_SETTING = 2;
    private static final int STORAGE_SETTING = 3;
    private static final int VOLUME_SETTING = 4;
    private static final int QUERY_SETTING = 5;
    private static final int NETWORK_SETTING = 6;

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    private HandlerThread mThread;
    private Handler mHandler;
    private AudioManager mAudioManager;
    private static BluetoothManager mInstance;

    //add by Jerry
    public static final int MOBILE_ONLY = 1;
    public static final int WIFI_ONLY = 2;
    public static final int MOBILE_WIFI = 3;
    public static boolean isBlueToothLastState = false;
    public static boolean isSetByService = false;
    private String mBlueToothName = null;

    public static synchronized BluetoothManager getInstance() {
        if (mInstance == null) {
            mInstance = new BluetoothManager();
        }
        return mInstance;
    }

    private BluetoothManager() {
        mContext = HelmetApplication.mAppContext;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            mThread = new HandlerThread("HELMET_BLUETOOTH");
            mThread.start();
            mHandler = new Handler(mThread.getLooper()) {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case Constants.MESSAGE_STATE_CHANGE:
                            switch (msg.arg1) {
                                case BluetoothChatService.STATE_CONNECTED:
                                    Log.e(TAG, "bluetooth connected...");
                                    HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.BLUETOOTH_PAIR_SUCCESS);
                                    break;

                                case BluetoothChatService.STATE_CONNECTING:
                                    Log.e(TAG, "bluetooth connecting...");
                                    break;

                                case BluetoothChatService.STATE_LISTEN:
                                case BluetoothChatService.STATE_NONE:
                                    Log.e(TAG, "bluetooth disconnected...");
                                    break;
                                default:
                                    return;
                            }
                            break;

                        case Constants.MESSAGE_READ:
                            byte[] readBuf = (byte[]) msg.obj;
                            String readMessage = new String(readBuf, 0, msg.arg1);
                            if (!TextUtils.isEmpty(readMessage)) {
                                Toast.makeText(mContext, "recvMessage: " + readMessage, Toast.LENGTH_SHORT).show();
                                onReceiveMessage(readMessage);
                            }
                            break;
                        case Constants.MESSAGE_DEVICE_NAME:
                            // save the connected device's name
                            String connectedName = msg.getData().getString(Constants.DEVICE_NAME);
                            Toast.makeText(mContext, "Connected to " + connectedName, Toast.LENGTH_SHORT).show();
                            break;

                        case Constants.MESSAGE_TOAST:
                            Toast.makeText(mContext, msg.getData().getString(Constants.TOAST),
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            };
            setupBluetoothIfNecessary();
        }
    }

    private boolean setupBluetoothIfNecessary() {
        Log.e(TAG, "setupBluetooth......");
        if (!isSupportBluetooth()) {
            Log.e(TAG, "bluetooth is not supported...");
            return false;
        } else if (!mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "bluetooth is disabled...");
            return false;
        } else {
            if (mChatService == null) {
                mChatService = new BluetoothChatService(mContext, mHandler);
            }

            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }

            return true;
        }
    }

    public void sendMessage(String message) {

        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(mContext, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.e(TAG, "sendMessage: " + message);

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    private boolean isSupportBluetooth() {
        return mBluetoothAdapter != null;
    }

    private void onReceiveMessage(String message) {
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
                        LogUtil.e("unrecognized command: " + message);
                }
            } catch (Exception var4) {
            }

        } else {
            LogUtil.e("receive empty message from bluetooth...");
        }
    }

    private void setWifi(JSONObject json) {
        LogUtil.e("bluetooth config wifi: " + json);

        if (json == null) {
            sendMessage("{\"msg_id\":11,\"code\":-1}");
        } else {
            String ssid_2_4g = "";
            String passwd_2_4g = "";
            String ssid_5g = "";
            String passwd_5g = "";
            ssid_2_4g = json.optString("wifi_2_4g_ssid");
            passwd_2_4g = json.optString("wifi_2_4g_pass");
            ssid_5g = json.optString("wifi_5g_ssid");
            passwd_5g = json.optString("wifi_5g_pass");

            //HelmetConfig.get().updateWifiConfig(this, ssid_2_4g, passwd_2_4g, ssid_5g, passwd_5g);
        }
    }

    private void setServer(JSONObject json) {
        LogUtil.e("bluetooth config server: " + json);
        if (json == null) {
            sendMessage("{\"msg_id\":21,\"code\":-1}");
        } else {
            String serverHost = json.optString("web_server_host", "");
            int serverPort = json.optInt("web_server_port", -1);
            String gpsServerHost = json.optString("gps_server_host", "");
            int gpsServerPort = json.optInt("gps_server_port", -1);
            //HelmetConfig.get().updateServerConfig(this, serverHost, serverPort, gpsServerHost, gpsServerPort);
        }
    }

    private void setStorage(JSONObject json) {
        LogUtil.e("bluetooth config storage: " + json);
        if (json == null) {
            sendMessage("{\"msg_id\":31,\"code\":-1}");
        } else {
            boolean isTfFirst = true;
            String storage = json.optString("mem_use_first", "");
            if ("nand".equals(storage)) {
                isTfFirst = false;
            } else {
                if (!"tf".equals(storage)) {
                    sendMessage("{\"msg_id\":31,\"code\":-1}");
                    return;
                }

                isTfFirst = true;
            }

            //HelmetConfig.get().updateStorageConfig(this, isTfFirst);
        }
    }

    private void setVolume(JSONObject json) {
        LogUtil.e("bluetooth config volume: " + json);

        if (json == null) {
            sendMessage("{\"msg_id\":41,\"code\":-1}");
        } else {
            int volume = json.optInt("voice", -1);
            if (volume >= 0 && volume <= 100) {
                int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int index = maxVolume * volume / 100;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, AudioManager.FLAG_PLAY_SOUND);
                sendMessage("{\"msg_id\":41,\"code\":0}");
            } else {
                sendMessage("{\"msg_id\":41,\"code\":-1}");
            }
        }
    }

    private void querySetting() {
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
            json.put("softwear_version", softVersion);
            json.put("net_type", DeviceStatusUtil.getNetType());
            json.put("net_status", DeviceStatusUtil.getNetStatus());
            int volume = mAudioManager.getStreamVolume(3);
            int maxVolume = mAudioManager.getStreamMaxVolume(3);
            Log.e(TAG, "volume:maxVolume>>>" + volume + ":" + maxVolume);
            json.put("voice", volume);
        } catch (Exception var6) {
            sendMessage("{\"msg_id\":51,\"code\":-1}");
            return;
        }

        sendMessage(json.toString());
    }

    private void setNetwork(JSONObject json) {
        if (json != null) {
            int networkType = json.optInt("force_net_type", MOBILE_WIFI);
            //HelmetConfig.get().updateNetworkConfig(this, networkType);
        }
    }

    public void setHelmetName(String name) {
        //TODO set bluetooth name here
        if (name != null && !TextUtils.isEmpty(name.trim())) {
            bluetoothRenameByService(name);
        }
    }


    //add by Jerry
    public void openBluetooth(boolean value) {
        Intent intent = new Intent("com.hy.helmet.ACTION_BLUETOOTH");
        intent.putExtra("bluetooth_state", value);
        HelmetApplication.mAppContext.sendBroadcast(intent);//systemui will receiver this

        if (!value) {
            BluetoothChatService.mIsInitiative = true;
            if (mChatService != null)
                mChatService.stop();
        } else {
            BluetoothChatService.mIsInitiative = false;
        }
    }

    public boolean isBluetoothEnable() {
        if (mBluetoothAdapter != null)
            return mBluetoothAdapter.isEnabled();
        return false;
    }

    public void startDiscovery() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.setDiscoverableTimeout(Integer.MAX_VALUE);
            mBluetoothAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
            setupBluetoothIfNecessary();
        }
    }

    public void bluetoothRenameByService(String name) {
        LogUtil.e("bluetoothRenameByService: " + name);
        mBlueToothName = name;
        isSetByService = true;
        isBlueToothLastState = isBluetoothEnable();
        bluetoothRename();
    }

    public void bluetoothRename() {
        if (mBluetoothAdapter != null && mBlueToothName != null) {
            String curretName = mBluetoothAdapter.getName();
            if (!TextUtils.isEmpty(mBlueToothName)) {
                if (!TextUtils.isEmpty(curretName)) {
                    if (!curretName.equals(mBlueToothName)) {
                        if (mBluetoothAdapter.isEnabled()) {
                            mBluetoothAdapter.setName(mBlueToothName);
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
