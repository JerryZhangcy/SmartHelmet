package com.cy.helmet.wifi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.location.LocationDataUtil;
import com.cy.helmet.location.LocationUtil;
import com.cy.helmet.networkstatus.NetWorkStatus;
import com.cy.helmet.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Context.ALARM_SERVICE;

public class HelmetWifiManager {
    private static HelmetWifiManager mInstance;

    private List<ScanResult> mWifiInfoList = new ArrayList<>();
    private Context mContext;
    private WifiManager mWifiManager;
    private Thread mWifiDataThread;
    private WifiDataRunnable mWifiDataRunnable;
    private Thread mWifiDataSendThread;
    private WifiDataSendRunnable mWifiDataSendRunnable;
    private boolean mStop = false;
    private boolean mStart = false;
    public static boolean mDataAlarm = false;
    public static boolean mSendAlarm = false;
    private Semaphore mSemaphore;

    public static synchronized HelmetWifiManager getInstance() {
        if (mInstance == null) {
            synchronized (HelmetWifiManager.class) {
                if (mInstance == null) {
                    mInstance = new HelmetWifiManager();
                }
            }
        }
        return mInstance;
    }

    public void initWifiManager(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mSemaphore = new Semaphore(1, true);
    }

    public void start() {
        if (mStart)
            return;
        mStop = false;
        mStart = true;
        mWifiDataRunnable = new WifiDataRunnable();
        mWifiDataThread = new Thread(mWifiDataRunnable, "WIFI_DATA");
        mWifiDataSendRunnable = new WifiDataSendRunnable();
        mWifiDataSendThread = new Thread(mWifiDataSendRunnable, "WIFI_DATA_SEND");

        synchronized (mWifiDataThread) {
            mWifiDataThread.start();
        }

        synchronized (mWifiDataSendThread) {
            mWifiDataSendThread.start();
        }
    }


    public void stop() {
        mStop = true;
        if (mWifiDataThread != null) {
            try {
                mWifiDataThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mWifiDataSendThread != null) {
            try {
                mWifiDataSendThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mStart = false;
    }


    public void notifyDataSendThread() {
        if (mWifiDataSendThread != null) {
            synchronized (mWifiDataSendThread) {
                mWifiDataSendThread.notifyAll();
            }
        }
    }

    public void notifyDataCollectThread() {
        if (mWifiDataThread != null) {
            synchronized (mWifiDataThread) {
                mWifiDataThread.notifyAll();
            }
        }
    }

    class WifiDataRunnable implements Runnable {

        @Override
        public void run() {
            while (!mStop) {
                synchronized (mWifiDataThread) {
                    try {
                        mWifiDataThread.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (mWifiManager != null) {
                    mWifiManager.startScan();
                    mWifiInfoList = mWifiManager.getScanResults();

                    try {
                        mSemaphore.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (mWifiInfoList != null && mWifiInfoList.size() > 3) {
                        JSONArray array = new JSONArray();
                        for (ScanResult scanResult : mWifiInfoList) {
                            String mac = scanResult.BSSID.replace(":", "-");
                            int level = scanResult.level;
                            long time = System.currentTimeMillis();
                            JSONObject jsonObject = WifiMsgDef.getWifiInfo(mac, level, time);
                            array.put(jsonObject);
                        }
                        WifiManagerUtil.writeFile(array);
                    }
                    mSemaphore.release();
                    startDataCollectionAlarm(false);
                }
            }
        }
    }

    class WifiDataSendRunnable implements Runnable {

        @Override
        public void run() {
            while (!mStop) {
                synchronized (mWifiDataSendThread) {
                    try {
                        mWifiDataSendThread.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    mSemaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ArrayList<JSONArray> arrays = WifiManagerUtil.readFile();
                WifiManagerUtil.deleteCachingFile();
                mSemaphore.release();

                try {
                    String IMEI = LocationDataUtil.getInstance().getIMEI();
                    if (arrays != null && !arrays.isEmpty()) {
                        JSONObject sendJSONObject = WifiMsgDef.getWifiSendData(IMEI, arrays);
                        startHttpConnect(sendJSONObject);
                        Thread.sleep(1000);//睡眠1S确保数据发送完
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startDataSendNextAlarm(false);
            }
        }
    }


    public synchronized void startDataSendNextAlarm(boolean isFirst) {
        if (mSendAlarm)
            return;
        mSendAlarm = true;
        AlarmManager mAlarmManager = (AlarmManager) HelmetApplication.mAppContext.getSystemService(ALARM_SERVICE);
        Intent sendDataIntent = new Intent();
        sendDataIntent.setClass(HelmetApplication.mAppContext,
                WifiDataSendService.class);
        PendingIntent sendData = PendingIntent.getService(HelmetApplication.mAppContext,
                0, sendDataIntent, FLAG_UPDATE_CURRENT);
        int time = HelmetConfig.get().mwifiUploadRate;
        if (isFirst) {
            time = 0;
        }

        mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + time,
                sendData);
    }

    public synchronized void startDataCollectionAlarm(boolean isFirst) {
        if (mDataAlarm)
            return;
        mDataAlarm = true;
        AlarmManager mAlarmManager = (AlarmManager) HelmetApplication.mAppContext.getSystemService(ALARM_SERVICE);
        Intent sendDataIntent = new Intent();
        sendDataIntent.setClass(HelmetApplication.mAppContext,
                WifiDataCollectService.class);
        PendingIntent sendData = PendingIntent.getService(HelmetApplication.mAppContext,
                0, sendDataIntent, FLAG_UPDATE_CURRENT);
        int time = HelmetConfig.get().mWifiCollectRate;
        if (isFirst) {
            time = 0;
        }
        mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + time,
                sendData);
    }


    private void startHttpConnect(JSONObject jsonObject) {
        LogUtil.e("[startHttpConnect] " + jsonObject.toString());
        boolean uploadSuccess = false;
        OutputStream os = null;
        HttpURLConnection conn = null;
        BufferedReader br = null;
        BufferedWriter bw = null;
        String ip = null;
        int port = -1;
        String serverUrl = null;
        if (NetWorkStatus.getInstance().getCurrentConnected() == NetWorkStatus.MOBILE_CONNECTED) {
            ip = HelmetConfig.get().getCloudServerHost();
            port = HelmetConfig.get().getCloudServerPort();
        } else {
            ip = HelmetConfig.get().getGpsServerHost();
            port = HelmetConfig.get().getGpsServerPort();
        }

        if (ip != null && port != -1) {
            StringBuilder builder = new StringBuilder();
            builder.append("http://");
            builder.append(ip);
            builder.append(":");
            builder.append(port);
            builder.append(LocationUtil.WIFI_URL_SUFFIX);
            serverUrl = builder.toString();
        }

        LogUtil.e("serverUrl = " + serverUrl);
        if (serverUrl == null) {
            LogUtil.e("!!!!! serverUrl == null");
            return;
        }
        try {
            conn = (HttpURLConnection) new URL(serverUrl).openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setChunkedStreamingMode(0);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.connect();

            os = conn.getOutputStream();
            bw = new BufferedWriter(new OutputStreamWriter(os));
            bw.write(jsonObject.toString());
            bw.flush();

            LogUtil.e("conn.getResponseCode() = " + conn.getResponseCode());

            if (conn.getResponseCode() == 200) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuffer sb = new StringBuffer();
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                String result = sb.toString();
                LogUtil.e("result = " + result);
                try {
                    JSONObject resultJSON = new JSONObject(result);
                    uploadSuccess = (0 == resultJSON.getInt("code"));
                } catch (Exception e) {
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e("!!!! connect failed");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (conn != null) {
                conn.disconnect();
            }

            LogUtil.e("uploadSuccess = " + uploadSuccess);
        }
    }

}
