package com.cy.helmet;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.cy.helmet.conn.HelmetConnManager;
import com.cy.helmet.networkstatus.NetWorkStatus;
import com.cy.helmet.temperature.TemperatureDetect;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkUtil;
import com.cy.helmet.video.HelmetVideoManager;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class HelmetMainService extends Service implements NetWorkStatus.NetAvaileChangeListen {

    private static String NOTIFY_CHANNEL_ID = "CHANNEL_MAIN_SERVICE";

    private static HelmetVideoManager mVideoManager;

    private NotificationManager mManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mVideoManager = HelmetVideoManager.getInstance();
        mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //registerNetworkChangeReceiver();
        NetWorkStatus.getInstance().setListener(this);
    }

    private void startForegroundService() {
        //startForeground(10010, getNotification()); modify by Jerry
    }

    private void stopForegroundService() {
        //stopForeground(true); modify by Jerry
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String action = bundle.getString("ACTION");
                if ("SWITCH_DEVICE_STATE".equals(action)) {
                    int destState = bundle.getInt("STATE", -1);
                    if (destState == HelmetClient.DEVICE_STATE_WAKE_UP) {
                        Log.e("YJQ", "force start.........");
                        Toast.makeText(getApplicationContext(), "开始预览...", Toast.LENGTH_SHORT).show();
                        startForegroundService();
                        setTickAlarm();
                        HelmetConnManager.getInstance().forceStart();
                    } else if (destState == HelmetClient.DEVICE_STATE_PRE_SLEEP) {
                        Log.e("YJQ", "force sleep.........");
                        cancelTickAlarm();
                        HelmetVideoManager.getInstance().prepareSleep();
                        HelmetConnManager.getInstance().prepareSleep();
                    }
                } else if ("TAKE_PHOTO".equals(action)) {//take photo
                    if (TemperatureDetect.getmInstance().isTempWarning()) { //add by Jerry
                        LogUtil.e("takePhoto: temperature is warning");
                        return START_STICKY;
                    }
                    boolean playVoice = bundle.getBoolean("PLAY_VOICE", true);
                    boolean isCompress = bundle.getBoolean("NEED_COMPRESS", false);
                    if (mVideoManager != null) {
                        mVideoManager.takePhoto(false, playVoice, isCompress);
                    } else {
                        LogUtil.e("takePhoto: 'HelmetVideoManager' object not exist.");
                    }
                } else if ("START_RECORD_VIDEO".equals(action)) {
                    if (TemperatureDetect.getmInstance().isTempWarning()) { //add by Jerry
                        LogUtil.e("startRecord: temperature is warning");
                        return START_STICKY;
                    }
                    if (mVideoManager != null) {
                        boolean isManual = bundle.getBoolean("IS_MANUAL", true);
                        boolean playVoice = bundle.getBoolean("PLAY_VOICE", true);
                        mVideoManager.startRecording(isManual, playVoice);
                    } else {
                        LogUtil.e("startRecord: 'HelmetVideoManager' object not exist.");
                    }
                } else if ("STOP_RECORD_VIDEO".equals(action)) {
                    if (mVideoManager != null) {
                        boolean isManual = bundle.getBoolean("IS_MANUAL", true);
                        mVideoManager.stopRecording(isManual);
                    } else {
                        LogUtil.e("stopRecord: 'HelmetVideoManager' object not exist.");
                    }
                } else if ("START_LIVE_STREAM".equals(action)) {
                    if (TemperatureDetect.getmInstance().isTempWarning()) { //add by Jerry
                        LogUtil.e("startLive: temperature is warning");
                        return START_STICKY;
                    }
                    if (mVideoManager != null) {
                        String url = bundle.getString("URL");
                        int frameSize = bundle.getInt("FRAME_SIZE");
                        mVideoManager.startLiving(url,frameSize);
                    } else {
                        LogUtil.e("startLive: 'HelmetVideoManager' object not exist.");
                    }
                } else if ("STOP_LIVE_STREAM".equals(action)) {
                    if (mVideoManager != null) {
                        mVideoManager.stopLiving();
                        Toast.makeText(getApplicationContext(), "结束视频直播...", Toast.LENGTH_SHORT).show();
                    } else {
                        LogUtil.e("stopLive: 'HelmetVideoManager' object not exist.");
                    }
                } else if ("ALARM_TICK".equals(action)) {
                    LogUtil.e("receive tick alarm in main service...");
                    if (!HelmetClient.isSleepNow()) {
                        HelmetConnManager.getInstance().triggerHelmetConnect();
                        setTickAlarm();
                    }
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTickAlarm();
        stopForegroundService();
        if (mVideoManager != null) {
            mVideoManager.onDestroy();
        }
        //unRegisterNetworkChangeReceiver();

        NetWorkStatus.getInstance().removeListener(this);
    }

    protected void setTickAlarm() {
        Intent intent = new Intent(HelmetApplication.mAppContext, HelmetMainService.class);
        Bundle bundle = new Bundle();
        bundle.putString("ACTION", "ALARM_TICK");
        intent.putExtras(bundle);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent alarmPendingIntent = PendingIntent.getService(this, 0, intent, FLAG_UPDATE_CURRENT);

        // cancel exist alarm if necessary
        am.cancel(alarmPendingIntent);

        // set new alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + Constant.TIME_INTERVAL, alarmPendingIntent);
        } else {
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + Constant.TIME_INTERVAL, alarmPendingIntent);
        }
    }

    protected void cancelTickAlarm() {
        Intent intent = new Intent(HelmetApplication.mAppContext, HelmetMainService.class);
        Bundle bundle = new Bundle();
        bundle.putString("ACTION", "ALARM_TICK");
        intent.putExtras(bundle);

        PendingIntent alarmPendingIntent = PendingIntent.getService(this, 0, intent, FLAG_UPDATE_CURRENT);

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(alarmPendingIntent);
    }

    private Notification getNotification() {

        Notification.Builder builder;
        String title = getString(R.string.app_name);
        String summary = getString(R.string.foreground_service_is_running);

        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel();
            builder = new Notification.Builder(getApplicationContext(), NOTIFY_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(getApplicationContext());
        }

        Intent intent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.
                getActivity(this, 0, intent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(summary)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= 21) {
            builder.setSound(null, null);
        } else {
            builder.setSound(null);
        }

        return builder.build();
    }

    @TargetApi(26)
    public void createNotificationChannel() {
        if (mManager.getNotificationChannel(NOTIFY_CHANNEL_ID) == null) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NOTIFY_CHANNEL_ID, NOTIFY_CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
    }

    private BroadcastReceiver mNetworkChangeReceiver;

    private void registerNetworkChangeReceiver() {
        mNetworkChangeReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                boolean available = NetworkUtil.hasNetwork();
                HelmetConnManager.getInstance().onNetworkStateChanged(available);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkChangeReceiver, filter);
    }

    private void unRegisterNetworkChangeReceiver() {
        if (mNetworkChangeReceiver != null) {
            unregisterReceiver(mNetworkChangeReceiver);
        }
    }

    @Override
    public void netWorkDisconnect(boolean flag) {
        HelmetConnManager.getInstance().onNetworkStateChanged(!flag);
    }
}
