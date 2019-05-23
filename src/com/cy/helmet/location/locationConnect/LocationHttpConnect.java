package com.cy.helmet.location.locationConnect;

import com.cy.helmet.location.LocationDataUtil;
import com.cy.helmet.location.LocationMsgDef;
import com.cy.helmet.location.LocationUtil;
import com.cy.helmet.location.locationcaching.LocationCaching;
import com.cy.helmet.observer.GpsConnectChange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LocationHttpConnect {
    private static final String TAG = "LocationHttpConnect";
    private static LocationHttpConnect mInstance;
    protected LinkedBlockingQueue<LocationHttpSendMessage> mSendMessageQueue =
            new LinkedBlockingQueue<LocationHttpSendMessage>();
    protected Thread mLocationHttpSendThread;
    protected LocationHttpSendRunnable mLocationHttpSendRunnable;
    private boolean mStop = false;
    private boolean mStart = false;

    public static synchronized LocationHttpConnect getInstance() {
        if (mInstance == null) {
            synchronized (LocationHttpConnect.class) {
                if (mInstance == null) {
                    mInstance = new LocationHttpConnect();
                }
            }
        }
        return mInstance;
    }

    public synchronized void start() throws Exception {
        if (mStart)
            return;
        mStart = true;
        mStop = false;
        mLocationHttpSendRunnable = new LocationHttpSendRunnable();
        mLocationHttpSendThread = new Thread(mLocationHttpSendRunnable, "LOCATION_HTTP_THREAD");
        mLocationHttpSendThread.setDaemon(true);
        synchronized (mLocationHttpSendThread) {
            mLocationHttpSendThread.start();
        }
    }

    public synchronized void stop() {
        mStop = true;
        mStart = false;
        if (mLocationHttpSendThread != null) {
            try {
                mLocationHttpSendThread.interrupt();
            } catch (Exception e) {
            }
        }
    }

    public void sendMessage(LocationHttpSendMessage msg) {
        if (msg != null) {
            try {
                mSendMessageQueue.add(msg);
            } catch (Exception e) {
            }
        }
    }

    protected LocationHttpSendMessage takeMessage() {
        LocationHttpSendMessage message = null;
        try {
            message = mSendMessageQueue.take();
        } catch (Exception e) {

        }
        return message;
    }

    class LocationHttpSendRunnable implements Runnable {

        @Override
        public void run() {
            while (!mStop) {
                try {
                    LocationHttpSendMessage message = takeMessage();
                    startHttpConnect(message.message, message.mMsg_type);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {

                }
            }
        }
    }


    private void startHttpConnect(JSONObject jsonObject, int type) {
        boolean uploadSuccess = false;
        OutputStream os = null;
        HttpURLConnection conn = null;
        BufferedReader br = null;
        BufferedWriter bw = null;
        String serverUrl = LocationDataUtil.getInstance().getGpsUrl();
        Log.e(TAG, "serverUrl = " + serverUrl);
        if (serverUrl == null) {
            Log.e(TAG, "!!!!! serverUrl == null");
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

            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String result = sb.toString();
            Log.e(TAG, "result = " + result);
            try {
                JSONObject resultJSON = new JSONObject(result);
                uploadSuccess = (0 == resultJSON.getInt("code"));
            } catch (Exception e) {
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "!!!! connect failed");
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
            //发送失败时缓存数据
            Log.e(TAG, "uploadSuccess = " + uploadSuccess + " type = " + type);
            if (!uploadSuccess) {
                LocationCaching.saveGpsCachingBySendFailed(jsonObject, type);
            } else {
                if (type == LocationHttpSendMessage.TYPE_CACHING) {
                    LocationCaching.deleteCachingFile(LocationUtil.GPS_CACHING_FILE_NAME);
                } else if (type == LocationHttpSendMessage.TYPE_MUL) {
                    LocationCaching.deleteCachingFile(LocationUtil.GPS_NORMAL_FILE_NAME);
                }
            }
            GpsConnectChange.getInstance().onGpsConnectChange(uploadSuccess);
        }
    }
}
