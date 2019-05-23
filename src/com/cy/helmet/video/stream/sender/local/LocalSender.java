package com.cy.helmet.video.stream.sender.local;

import android.util.Log;

import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.storage.LocalFileManager;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.video.stream.sender.Sender;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LocalSender implements Sender {

    private long FILE_MAX_SIZE_BYTE = 8 * 1024 * 1024;
    private long mSendBytes = 0;

    private File mCurFile = null;
    private FileOutputStream mOutStream;
    private static BufferedOutputStream mBuffer;

    @Override
    public void start() {
        transferNewFile();
    }

    public boolean onTransferFile() {
        mCurFile = transferNewFile();
        return true;
    }

    @Override
    public void onData(byte[] data, int type) {
        if (mBuffer != null) {
            try {

                long startWriteFile = System.currentTimeMillis();
                mBuffer.write(data);
                mBuffer.flush();
                Log.e("YJQ", "writePartTime: " + (System.currentTimeMillis() - startWriteFile));

//                mSendBytes += data.length;
//                long needBytes = FILE_MAX_SIZE_BYTE - mSendBytes;
//                if (needBytes < data.length) {
//                    mSendBytes = 0;
//                    Log.e("YJQ", "finishFileName>>>" + mCurFile.getName());
//                    Log.e("YJQ", "finishFileSize>>>" + mCurFile.length());
//                    mCurFile = transferNewFile();
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        mCurFile = null;
        if (mBuffer != null) {
            try {
                mBuffer.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mBuffer = null;
        }

        if (mOutStream != null) {
            try {
                mOutStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mBuffer = null;
            mOutStream = null;
        }
    }

    private File transferNewFile() {
        FILE_MAX_SIZE_BYTE = HelmetConfig.get().getMaxRecordFileLength() * 1024 * 1024;
        Log.d("txhlog", "FILE_MAX_SIZE_BYTE=" + FILE_MAX_SIZE_BYTE);
        mCurFile = LocalFileManager.getInstance().getEmptyVideoFile();
        if (mCurFile.exists()) {
            mCurFile.delete();
        }

        LogUtil.e("new video file>>>" + mCurFile.getName());

        // close output stream
        try {
            if (mOutStream != null) {
                mOutStream.close();
            }

            if (mBuffer != null) {
                mBuffer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mOutStream = null;
            mBuffer = null;
        }

        try {
            mOutStream = new FileOutputStream(mCurFile);
            mBuffer = new BufferedOutputStream(mOutStream);
        } catch (Exception e) {
            mCurFile = null;
            LogUtil.e("transfer output stream failed: ");
            LogUtil.e(e);
            throw new RuntimeException(e);
        }

        return mCurFile;
    }

    public String getRecordingFileName() {
        if (mCurFile != null) {
            return mCurFile.getName();
        }
        return "";
    }
}
