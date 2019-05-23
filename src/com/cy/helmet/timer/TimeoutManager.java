package com.cy.helmet.timer;

import android.os.Handler;
import android.os.SystemClock;

import com.cy.helmet.WorkThreadManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jiaqing on 2018/1/3.
 */

public class TimeoutManager {

    public static final String TASK_ID_REQUEST_TALK = "request_talk_task_id";

    private static TimeoutManager mInstance;

    private Set<String> mTaskTag = new HashSet<>();

    public static synchronized TimeoutManager getInstance() {
        if (mInstance == null) {
            mInstance = new TimeoutManager();
        }
        return mInstance;
    }

    private TimeoutManager() {
    }

    public synchronized boolean removeTimeoutTask(final String taskId) {
        return mTaskTag.remove(taskId);
    }

    private synchronized boolean addTimeoutTask(final String taskId) {
        if (!mTaskTag.contains(taskId)) {
            return mTaskTag.add(taskId);
        }
        return false;
    }

    public synchronized void startTimeoutTask(final String taskId, final int timeSec, final OnTimeoutListener listener) {
        final Handler handler = WorkThreadManager.getTimeoutHandler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (addTimeoutTask(taskId)) {
                    long destTime = SystemClock.uptimeMillis() + timeSec * 1000;
                    handler.postAtTime(new Runnable() {
                        @Override
                        public void run() {
                            if (removeTimeoutTask(taskId)) {
                                if (listener != null) {
                                    listener.onTimeout();
                                }
                            }
                        }
                    }, taskId, destTime);
                }
            }
        });
    }
}
