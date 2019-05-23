package com.cy.helmet.util;

import android.os.RemoteException;
import android.util.Log;

import com.android.internal.util.HexDump;

import java.util.ArrayList;

import vendor.mediatek.hardware.nvram.V1_0.INvram;

public class NvramFileUtil {
    public static final String TAG = "NvramFileUtil";
    public static final int STORAGE_SIZE = 1024;
    public static final int NVRAM_ID = 78;
    public static byte mDataInfo[] = new byte[STORAGE_SIZE];

    public static byte[] stringParseAscii(String str) {
        byte value[] = str.getBytes();
        return value;
    }

    public static String asciiParseString(byte[] mByte) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mByte.length; i++) {
            sb.append((char) (mByte[i]));
        }
        return sb.toString();
    }

    public static byte[] readData() {
        byte buff[] = new byte[STORAGE_SIZE];
        String buffString = null;
        INvram agent;

        try {
            agent = INvram.getService();
            Log.v(TAG, "ReadData, agent = " + agent);

            if (agent == null) {
                Log.e(TAG, "NvRAMAgent is null");
                return buff;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return buff;
        }

        try {
            buffString = agent.readFileById(NVRAM_ID, STORAGE_SIZE);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            Log.v(TAG,"read failed");
            e.printStackTrace();
            return buff;
        }

        Log.v(TAG, "read data:" + buffString);
        int len = buffString.length() - 1;
        Log.v(TAG, "read data len:" + len);

        mDataInfo = HexDump.hexStringToByteArray(
                buffString.substring(0, buffString.length() - 1));
        Log.v(TAG, "read buffArr len:" + mDataInfo.length);

        System.arraycopy(mDataInfo, 0, buff, 0, 100);
        Log.v(TAG, "read buffArr len:" + buff.length);

        return buff;
    }


    public static void writePhoneInfoData(byte[] mByte) {
        int len = mByte.length;
        INvram agent;
        try {
            agent = INvram.getService();
            Log.i(TAG,"WriteData, agent = "+agent);

            if (agent == null) {
                Log.e(TAG, "NvRAMAgent is null");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        ArrayList<Byte> dataArray = new ArrayList<Byte>(len);

        for (int i = 0; i < len; i++) {
            dataArray.add(i, new Byte(mByte[i]));
        }

        try {
            int flag = agent.writeFileById(NVRAM_ID, len, dataArray);
            Log.v(TAG,"write success flag = " + flag);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.v(TAG,"write failed" + e);
            return;
        }
    }
}
