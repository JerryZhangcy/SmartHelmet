package com.cy.helmet.led;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhangchongyang on 18-3-7.
 */

public class Led {
    private static final String led_switch = "/sys/class/leds/aw9109_led/hwen";//echo 1 or 0
    private static final String led_open = "/sys/class/leds/aw9109_led/brightness";//echo 255
    private static final String led_blink = "/sys/class/leds/aw9109_led/blink";//echo 1
    private static final String led_reg = "/sys/class/leds/aw9109_led/reg";
    private static final String led_onoff_by_reg = "/sys/class/leds/aw9109_led/onoff_by_reg";

    private static final String LED_REGISTER = "0x50";

    private static int ONE_ALL = 0x1C;
    private static int TWO_ALL = 0xE0;
    private static int THREE_ALL = 0x700;

    /**
     * 全亮是0x7FC
     * 第一排全亮0x1C
     * 第二排全亮0xE0
     * 第三排全亮0x700
     * <p>
     * *-------------------------------------------------------
     * *
     * *
     * *
     * ---------------------------------------------------------
     */
    private static int[][] mLedValue =
            {
                    {
                            0x0004, 0x0008, 0x0010
                    },
                    {
                            0x0020, 0x0040, 0x0080
                    },
                    {
                            0x0100, 0x0200, 0x0400
                    }
            };

    private static Led mInstance;


    protected Thread mLedSetThread;
    protected ledSetRunnable mLedSetRunnable;
    private boolean mStop = false;
    protected LinkedBlockingQueue<LedConfig> mSendMessageQueue = new LinkedBlockingQueue<LedConfig>();


    /**
     * @return
     */
    public static synchronized Led getInstance() {
        if (mInstance == null) {
            synchronized (Led.class) {
                if (mInstance == null) {
                    mInstance = new Led();
                }
            }
        }
        return mInstance;
    }

    /**
     * @param config
     */
    public void sendMessage(LedConfig config) {
        if (config != null) {
            mSendMessageQueue.add(config);
        }
    }

    /**
     * @return
     */

    protected LedConfig pollMessage() {
        try {
            return mSendMessageQueue.take();
        } catch (InterruptedException e) {
            LedConfig.d("------->pollMessage mStop = " + mStop);
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @throws Exception
     */
    public synchronized void start() throws Exception {
        mStop = false;
        mLedSetRunnable = new ledSetRunnable();
        mLedSetThread = new Thread(mLedSetRunnable, "LED_SET_THREAD");
        mLedSetThread.setDaemon(true);
        synchronized (mLedSetThread) {
            mLedSetThread.start();
        }
    }

    public synchronized void stop() {
        LedConfig.d("------->stop");
        mStop = true;
        if (mLedSetThread != null) {
            try {
                mLedSetThread.interrupt();
            } catch (Exception e) {
            }
        }
    }


    /**
     * @param config
     * @throws Exception
     */

    private void openLed(LedConfig config) throws Exception {
        if(config == null)
            return;
        switchState(config.getmRow(), config.getmColumns());
    }


    /**
     * row = -1 || columns = -1 灯全灭
     * row = 100 && columns = 100 灯全亮
     *
     * @param row
     * @param columns
     * @throws IOException
     */

    private void switchState(int row, int columns) throws IOException {

        int currentState = 0;
        String transferValue = null;

        try {
            currentState = readLedState();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (row == -1 || columns == -1) {
            File file = new File(led_switch);
            FileOutputStream fout = new FileOutputStream(file);
            byte[] bytes = "1".getBytes();
            fout.write(bytes);
            fout.close();
            return;
        } else if (row == 100 && columns == 100) {
            File file = new File(led_switch);
            FileOutputStream fout = new FileOutputStream(file);
            byte[] bytes = "1".getBytes();
            fout.write(bytes);
            fout.close();

            File openFile = new File(led_open);
            FileOutputStream openfout = new FileOutputStream(openFile);
            byte[] openbytes = "255".getBytes();
            openfout.write(openbytes);
            openfout.close();
        }

        LedConfig.d("currentState = " + currentState + " row = " + row + " columns = " + columns);

        if (row == 0) {
            currentState = currentState & (TWO_ALL | THREE_ALL);
            if (columns == 0) {
                currentState = currentState | mLedValue[0][0];
            } else if (columns == 1) {
                currentState = currentState | mLedValue[0][1];
            } else if (columns == 2) {
                currentState = currentState | mLedValue[0][2];
            }
        } else if (row == 1) {
            currentState = currentState & (ONE_ALL | THREE_ALL);
            if (columns == 0) {
                currentState = currentState | mLedValue[1][0];
            } else if (columns == 1) {
                currentState = currentState | mLedValue[1][1];
            } else if (columns == 2) {
                currentState = currentState | mLedValue[1][2];
            }
        } else if (row == 2) {
            currentState = currentState & (ONE_ALL | TWO_ALL);
            if (columns == 0) {
                currentState = currentState | mLedValue[2][0];
            } else if (columns == 1) {
                currentState = currentState | mLedValue[2][1];
            } else if (columns == 2) {
                currentState = currentState | mLedValue[2][2];
            }
        }

        transferValue = Integer.toHexString(currentState);

        if (transferValue != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            File regFile = new File(led_onoff_by_reg);
            FileOutputStream regfout = new FileOutputStream(regFile);
            byte[] regbytes = transferValue.getBytes();
            regfout.write(regbytes);
            regfout.close();
        }
    }


    private int readLedState() throws IOException {
        File regFile = new File(led_reg);
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(regFile), 256);
        String str = null;
        while (reader.read() != -1) {
            str = reader.readLine();
            if (str.contains(LED_REGISTER)) {
                break;
            }
        }
        if (str != null && str.contains("=")) {
            str = str.substring(str.indexOf("=") + 3, str.length()).trim();//需要将0x移除后转
        }
        int ret = Integer.parseInt(str, 16);
        if(reader != null)
            reader.close();
        return ret;
    }

    class ledSetRunnable implements Runnable {

        @Override
        public void run() {
            while (!mStop) {
                try {
                    openLed(pollMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
